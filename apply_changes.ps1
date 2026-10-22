# Applies approved tweaks to the Toshiba Android TV, logging each change and building undo_all.ps1.
$adb  = "C:\Users\abiji\AppData\Local\Microsoft\WinGet\Packages\Google.PlatformTools_Microsoft.Winget.Source_8wekyb3d8bbwe\platform-tools\adb.exe"
$dev  = "192.168.1.5:5555"
$log  = "D:\Dev\TV\tv_changes_log.txt"
$undo = "D:\Dev\TV\undo_all.ps1"

function Log($msg) { $line = "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')  $msg"; Add-Content -Encoding utf8 $log $line; Write-Output $line }
function Undo($cmd) { Add-Content -Encoding utf8 $undo "& `$adb -s `$dev shell $cmd" }

Set-Content -Encoding utf8 $log "Android TV changes - $dev (Toshiba, Android 11)"
Set-Content -Encoding utf8 $undo @"
# Reverts every change made by apply_changes.ps1. Run:  powershell -ExecutionPolicy Bypass -File D:\Dev\TV\undo_all.ps1
`$adb = "$adb"
`$dev = "$dev"
& `$adb connect `$dev
"@

# FLauncher was installed just before this script ran (me.efesser.flauncher 0.18.0 from gitlab.com/flauncher)
Log "INSTALLED  me.efesser.flauncher 0.18.0 (SHA256 376FC528C880CDF7C7C04B2D3708C69F911144498218BABB6300808B772AF32E)"

# Animation scales (originals 1.0 / 1.0 / 0.6)
$orig = @{ window_animation_scale = '1.0'; transition_animation_scale = '1.0'; animator_duration_scale = '0.6' }
foreach ($k in $orig.Keys) {
    & $adb -s $dev shell settings put global $k 0
    Log "SETTING    global $k : $($orig[$k]) -> 0"
    Undo "settings put global $k $($orig[$k])"
}

# Home screen: make FLauncher default, then disable stock launcher
& $adb -s $dev shell cmd package set-home-activity me.efesser.flauncher/.MainActivity | Out-Null
Log "HOME       default home -> me.efesser.flauncher/.MainActivity (was com.google.android.tvlauncher/.MainActivity)"

$packages = @(
    'com.google.android.tvlauncher',               # stock home screen (replaced by FLauncher)
    'com.google.android.tvrecommendations',        # recommendation/ad rows
    'android.autoinstalls.config.hisense.hayward', # Play Auto Installs (OEM bloat pusher)
    'com.google.android.play.games',
    'com.google.android.feedback',
    'com.google.android.syncadapters.calendar',
    'com.android.printspooler',
    'com.android.htmlviewer',
    'fusion.android.tv.demo',                      # store demo mode
    'com.cvte.tv.fac.aging',                       # factory burn-in test
    'com.jio.media.stb.ondemand',
    'com.google.android.youtube.tvmusic',
    'com.sand.aircasttv',
    'com.google.android.videos',
    'com.google.android.tv',                       # Live Channels
    'com.mediatek.wwtv.mediaplayer',               # USB media player
    'com.google.android.apps.mediashell'           # Chromecast built-in
)
foreach ($p in $packages) {
    $r = (& $adb -s $dev shell pm disable-user --user 0 $p) -join ' '
    Log "DISABLE    $p  => $r"
    if ($r -match 'disabled-user') { Undo "pm enable $p" }
}

# FLauncher removal goes last in undo so the stock launcher is back first
Add-Content -Encoding utf8 $undo "& `$adb -s `$dev shell cmd package set-home-activity com.google.android.tvlauncher/.MainActivity"
Add-Content -Encoding utf8 $undo "& `$adb -s `$dev uninstall me.efesser.flauncher"
Add-Content -Encoding utf8 $undo "& `$adb -s `$dev reboot"
Add-Content -Encoding utf8 $log "`nUNDO EVERYTHING:  powershell -ExecutionPolicy Bypass -File D:\Dev\TV\undo_all.ps1"
Log "DONE"
