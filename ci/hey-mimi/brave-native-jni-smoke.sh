#!/usr/bin/env bash
set -euo pipefail

# The emulator runner executes "script:" lines as distinct shells, so keep
# both finding the APK and invoking instrumentation in the SAME shell.
bash ci/hey-mimi/phone-startup-smoke.sh

test_apk="$(find app/build/outputs/apk/androidTest -type f -name '*.apk' | head -1)"
if [ -z "$test_apk" ] || [ ! -s "$test_apk" ]; then
  echo "FAIL: Android instrumentation APK was not generated"
  find app/build/outputs/apk -maxdepth 6 -type f || true
  exit 1
fi

adb install -r -t "$test_apk"

# Do not pipe through tee before checking test verdict. Android instrument
# may exit zero even when tests fail, so assert both native + PiP WebView tests passed.
result="$(adb shell am instrument -w -r \
  -e class com.carhud.aaproxy.BraveShieldsNativeTest,com.carhud.aaproxy.YouTubeFeedPipRuntimeTest \
  com.ledinhtien.ttube.test/androidx.test.runner.AndroidJUnitRunner)"
printf '%s\n' "$result" | tee smoke-artifacts/brave-native-jni.txt
grep -Eq 'OK \(2 tests\)|OK \(2 test\)' smoke-artifacts/brave-native-jni.txt
if grep -Eq 'FAILURES|INSTRUMENTATION_FAILED|Process crashed' smoke-artifacts/brave-native-jni.txt; then
  exit 1
fi
echo "PASS: Brave Rust JNI and real Android WebView feed-ad / 4-column PiP grid"
