#!/usr/bin/env bash
set -euo pipefail

chmod +x gradlew
./gradlew assembleDebug --stacktrace

apk="$(find app/build/outputs/apk/debug -name '*.apk' | head -1)"
test -n "$apk"
echo "Using APK: $apk"
adb install -r "$apk"

adb shell wm size 1280x720
adb shell wm density 160
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1
# Suppress Android's first-time immersive/fullscreen education popup.
adb shell settings put secure immersive_mode_confirmations confirmed || true
sleep 2

mkdir -p hud-render-artifacts

adb shell am force-stop com.ledinhtien.ttube
adb shell am start -W   -n com.ledinhtien.ttube/com.carhud.aaproxy.HudStyle7SmokeActivity   --ei scale 100
sleep 2
# Fallback: dismiss the fullscreen education popup if the setting above was ignored.
adb shell input tap 880 190 || true
sleep 1
adb exec-out screencap -p > hud-render-artifacts/style7-100-real.png

adb shell am force-stop com.ledinhtien.ttube
adb shell am start -W   -n com.ledinhtien.ttube/com.carhud.aaproxy.HudStyle7SmokeActivity   --ei scale 150
sleep 2
adb exec-out screencap -p > hud-render-artifacts/style7-150-real.png

adb shell dumpsys window windows > hud-render-artifacts/window.txt || true
file hud-render-artifacts/*.png || true
