#!/usr/bin/env bash
set -euo pipefail
mkdir -p smoke-artifacts
adb install -r built/TTube_v1.1.10_debug.apk
adb shell pm clear com.ledinhtien.ttube
adb logcat -c
adb shell am start -W -n com.ledinhtien.ttube/com.carhud.aaproxy.MainActivity | tee smoke-artifacts/launch.txt
sleep 12
adb logcat -b crash -d > smoke-artifacts/crash.log
adb shell dumpsys activity activities > smoke-artifacts/activity.txt
adb exec-out screencap -p > smoke-artifacts/screen.png || true
if grep -E 'FATAL EXCEPTION|ActivityNotFoundException|Unable to start activity' smoke-artifacts/crash.log; then
  echo "FAIL: app process crashed at startup"
  exit 1
fi
if ! grep -F 'com.carhud.aaproxy.ActivationActivity' smoke-artifacts/activity.txt; then
  echo "FAIL: expected first-run activation screen did not appear"
  exit 1
fi
echo "PASS: v7 opens on Android 15 without a fatal startup exception"
