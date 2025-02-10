#!/bin/bash -eu
: "${1?"Usage: $0 package name"}"

# Initialize and create a backup using Google Backup Transport
adb shell bmgr enable true
adb shell bmgr transport com.google.android.gms/.backup.BackupTransportService | grep -q "Selected transport" || (echo "Error: error selecting Google backup transport"; exit 1)
adb shell bmgr init com.google.android.gms/.backup.BackupTransportService
adb shell bmgr backupnow "$1" | grep -F "Package $1 with result: Success" || (echo "Backup failed"; exit 1)

# Uninstall and reinstall the app to clear the data and trigger a restore
apk_path_list=$(adb shell pm path "$1")
OIFS=$IFS
IFS=$'\n'
apk_number=0
for apk_line in $apk_path_list
do
    (( ++apk_number ))
    apk_path=${apk_line:8:1000}
    adb pull "$apk_path" "myapk${apk_number}.apk"
done
IFS=$OIFS
adb shell pm uninstall --user 0 "$1"

# Install the APK(s) back onto the device
apks=$(seq -f 'myapk%.f.apk' 1 $apk_number)
adb install-multiple -t --user 0 $apks

# Clean up backup-related configurations
adb shell bmgr transport com.google.android.gms/.backup.BackupTransportService
rm $apks

echo "Backup and restore process completed successfully!"

