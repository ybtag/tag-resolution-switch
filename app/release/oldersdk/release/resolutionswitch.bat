adb start-server
adb wait-for-device
adb install -r ResolutionSwitch.apk
adb shell pm grant com.android.cts.resolutionswitchadb android.permission.WRITE_SECURE_SETTINGS
pause