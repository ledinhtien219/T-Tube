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
# may exit zero even when tests fail, so assert an actual single test passed.
result="$(adb shell am instrument -w -r \
  -e class com.carhud.aaproxy.BraveShieldsNativeTest \
  com.ledinhtien.ttube.test/androidx.test.runner.AndroidJUnitRunner)"
printf '%s\n' "$result" | tee smoke-artifacts/brave-native-jni.txt
grep -Eq 'OK \(1 test\)|OK \(1 tests\)' smoke-artifacts/brave-native-jni.txt
if grep -Eq 'FAILURES|INSTRUMENTATION_FAILED|Process crashed' smoke-artifacts/brave-native-jni.txt; then
  exit 1
fi
echo "PASS: Brave native Rust adblock engine loaded and filtered a test ad request"
