#!/usr/bin/env bash
set -euo pipefail
mkdir -p smoke-artifacts
apk="$(find app/build/outputs/apk/debug -name '*.apk' | head -1)"
if [ -z "$apk" ] || [ ! -s "$apk" ]; then
  echo "FAIL: debug APK not found"
  exit 1
fi
adb install -r "$apk"
adb shell pm clear com.ledinhtien.ttube
adb logcat -c
adb shell am start -W -n com.ledinhtien.ttube/com.carhud.aaproxy.MainActivity | tee smoke-artifacts/launch.txt
sleep 12
adb logcat -b crash -d > smoke-artifacts/crash.log
adb shell dumpsys activity activities > smoke-artifacts/activities.txt
adb exec-out screencap -p > smoke-artifacts/phone-screen.png || true
if grep -E 'FATAL EXCEPTION|ActivityNotFoundException|ClassNotFoundException|Unable to start activity' smoke-artifacts/crash.log; then
  echo "FAIL: fatal exception during phone startup"
  exit 1
fi
if ! grep -F 'com.carhud.aaproxy.ActivationActivity' smoke-artifacts/activities.txt; then
  echo "FAIL: ActivationActivity is not visible after first launch"
  exit 1
fi
echo "PASS: app launched without fatal exception; activation screen visible"
