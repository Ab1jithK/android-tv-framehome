# Builds FrameHome.apk with the Android SDK command-line tools (no Gradle).
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }
$bt = "$sdk\build-tools\36.0.0"
$jar = "$sdk\platforms\android-35\android.jar"
if (-not $env:JAVA_HOME) { $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr" }
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$out = "$root\build"

function Check($what) { if ($LASTEXITCODE -ne 0) { throw "$what failed ($LASTEXITCODE)" } }

if (Test-Path $out) { Remove-Item -Recurse -Force $out }
New-Item -ItemType Directory -Force "$out\gen", "$out\classes", "$out\dex" | Out-Null

& "$bt\aapt2.exe" compile --dir "$root\res" -o "$out\res.zip"; Check 'aapt2 compile'
& "$bt\aapt2.exe" link -I $jar --manifest "$root\AndroidManifest.xml" -A "$root\assets" --java "$out\gen" `
    --min-sdk-version 26 --target-sdk-version 29 -o "$out\unsigned.apk" "$out\res.zip"; Check 'aapt2 link'

$src = Get-ChildItem "$root\src", "$out\gen" -Recurse -Filter *.java | ForEach-Object FullName
& javac --release 8 -classpath $jar -encoding UTF-8 -Xlint:-options -d "$out\classes" $src; Check 'javac'

$classes = Get-ChildItem "$out\classes" -Recurse -Filter *.class | ForEach-Object FullName
& "$bt\d8.bat" --release --min-api 26 --lib $jar --output "$out\dex" $classes; Check 'd8'

& jar --update --no-manifest --file "$out\unsigned.apk" -C "$out\dex" classes.dex; Check 'jar'
& "$bt\zipalign.exe" -f -p 4 "$out\unsigned.apk" "$out\aligned.apk"; Check 'zipalign'

$ks = "$root\framehome.jks"
if (-not (Test-Path $ks)) {
    & keytool -genkeypair -keystore $ks -alias framehome -keyalg RSA -keysize 2048 -validity 10000 `
        -storepass framehome -keypass framehome -dname "CN=Frame Home"; Check 'keytool'
}
& "$bt\apksigner.bat" sign --ks $ks --ks-pass pass:framehome --out "$root\FrameHome.apk" "$out\aligned.apk"; Check 'apksigner'
Get-Item "$root\FrameHome.apk" | Select-Object Name, Length
