# Installs FrameHome.apk on the TV. This TV's Wi-Fi adb drops large streams, so the APK goes over in
# 256 KB chunks (each retried after a reconnect), is reassembled and checksummed on the TV, then installed.
param([string]$Device = "192.168.1.5:5555")
# Continue, not Stop: adb writes transient errors to stderr, and those must lead to a retry, not abort.
$ErrorActionPreference = 'Continue'
$adb = (Get-Command adb -ErrorAction SilentlyContinue).Source
if (-not $adb) {
    # winget installs adb here but only adds it to PATH for new shells
    $adb = Get-ChildItem "$env:LOCALAPPDATA\Microsoft\WinGet\Packages\Google.PlatformTools*\platform-tools\adb.exe" -ErrorAction SilentlyContinue |
        Select-Object -First 1 -ExpandProperty FullName
}
if (-not $adb) { throw "adb not found: winget install Google.PlatformTools" }
$apk = "$PSScriptRoot\FrameHome.apk"
$parts = Join-Path ([IO.Path]::GetTempPath()) "framehome-chunks"

function Reconnect { & $adb disconnect $Device 2>&1 | Out-Null; Start-Sleep 3; & $adb connect $Device 2>&1 | Out-Null; Start-Sleep 2 }

if (Test-Path $parts) { Remove-Item -Recurse -Force $parts }
New-Item -ItemType Directory $parts | Out-Null
$bytes = [IO.File]::ReadAllBytes($apk)
$size = 256KB
$n = [Math]::Ceiling($bytes.Length / $size)
for ($i = 0; $i -lt $n; $i++) {
    $len = [Math]::Min($size, $bytes.Length - $i * $size)
    $buf = New-Object byte[] $len
    [Array]::Copy($bytes, $i * $size, $buf, 0, $len)
    [IO.File]::WriteAllBytes(("$parts\part{0:D3}" -f $i), $buf)
}

for ($t = 0; $t -lt 12; $t++) {
    & $adb -s $Device shell "rm -rf /data/local/tmp/fh; mkdir -p /data/local/tmp/fh" 2>&1 | Out-Null
    if ($LASTEXITCODE -eq 0) { break }
    Reconnect
}
if ($LASTEXITCODE -ne 0) { throw "TV not reachable at $Device" }
for ($i = 0; $i -lt $n; $i++) {
    $f = "part{0:D3}" -f $i
    $ok = $false
    for ($t = 0; $t -lt 6 -and -not $ok; $t++) {
        & $adb -s $Device push "$parts\$f" "/data/local/tmp/fh/$f" 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) { $ok = $true } else { Reconnect }
    }
    if (-not $ok) { throw "chunk $f would not transfer" }
}

$want = (Get-FileHash $apk -Algorithm MD5).Hash.ToLower()
$got = (& $adb -s $Device shell "cat /data/local/tmp/fh/part* > /data/local/tmp/FrameHome.apk && md5sum /data/local/tmp/FrameHome.apk").Split(' ')[0]
if ($got -ne $want) { throw "checksum mismatch ($got vs $want)" }
& $adb -s $Device shell "pm install -r /data/local/tmp/FrameHome.apk; rm -rf /data/local/tmp/fh /data/local/tmp/FrameHome.apk"
Remove-Item -Recurse -Force $parts
