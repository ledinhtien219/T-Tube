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
adb shell settings put secure immersive_mode_confirmations confirmed || true
sleep 2

mkdir -p hud-render-artifacts

capture() {
  local scale="$1"
  local alerts="$2"
  local output="$3"

  adb shell am force-stop com.ledinhtien.ttube
  adb shell am start -W     -n com.ledinhtien.ttube/com.carhud.aaproxy.HudStyle7SmokeActivity     --ei scale "$scale"     --ez alerts "$alerts"
  sleep 2
  adb shell input tap 880 190 || true
  sleep 1
  adb exec-out screencap -p > "hud-render-artifacts/$output"
}

capture 100 false style7-100-no-alert-real.png
capture 100 true  style7-100-alert-real.png
capture 150 true  style7-150-alert-real.png

adb shell dumpsys window windows > hud-render-artifacts/window.txt || true
file hud-render-artifacts/*.png || true
