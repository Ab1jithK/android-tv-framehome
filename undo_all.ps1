# Reverts every change made by apply_changes.ps1. Run:  powershell -ExecutionPolicy Bypass -File D:\Dev\TV\undo_all.ps1
$adb = "C:\Users\abiji\AppData\Local\Microsoft\WinGet\Packages\Google.PlatformTools_Microsoft.Winget.Source_8wekyb3d8bbwe\platform-tools\adb.exe"
$dev = "192.168.1.5:5555"
& $adb connect $dev
& $adb -s $dev shell settings put global transition_animation_scale 1.0
& $adb -s $dev shell settings put global animator_duration_scale 0.6
& $adb -s $dev shell settings put global window_animation_scale 1.0
& $adb -s $dev shell pm enable com.google.android.tvlauncher
& $adb -s $dev shell pm enable com.google.android.tvrecommendations
& $adb -s $dev shell pm enable android.autoinstalls.config.hisense.hayward
& $adb -s $dev shell pm enable com.google.android.play.games
& $adb -s $dev shell pm enable com.google.android.feedback
& $adb -s $dev shell pm enable com.google.android.syncadapters.calendar
& $adb -s $dev shell pm enable com.android.printspooler
& $adb -s $dev shell pm enable com.android.htmlviewer
& $adb -s $dev shell pm enable fusion.android.tv.demo
& $adb -s $dev shell pm enable com.cvte.tv.fac.aging
& $adb -s $dev shell pm enable com.jio.media.stb.ondemand
& $adb -s $dev shell pm enable com.google.android.youtube.tvmusic
& $adb -s $dev shell pm enable com.sand.aircasttv
& $adb -s $dev shell pm enable com.google.android.videos
& $adb -s $dev shell pm enable com.google.android.tv
& $adb -s $dev shell pm enable com.mediatek.wwtv.mediaplayer
& $adb -s $dev shell pm enable com.google.android.apps.mediashell
& $adb -s $dev shell cmd package set-home-activity com.google.android.tvlauncher/.MainActivity
& $adb -s $dev uninstall local.framehome
& $adb -s $dev reboot
