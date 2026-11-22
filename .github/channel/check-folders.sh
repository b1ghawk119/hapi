#!/usr/bin/env bash
# Drive instrumentation directly so the APK and its screenshots are still on
# the device when we collect them (Gradle normally uninstalls after the test).
set -euo pipefail
adb install android/app/build/outputs/apk/debug/app-debug.apk
adb install android/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -r \
    -e class 'app.hapi.companion.feature.home.HomeScreenTest#manualFolderCollapseHidesOnlyItsOwnRowsAndSurvivesARefresh' \
    -e screenshots internal run.hapi.companion.test/androidx.test.runner.AndroidJUnitRunner | tee folder-test.log
grep -Fq 'OK (1 test)' folder-test.log
for name in projects-expanded project-folded; do
    adb exec-out run-as run.hapi.companion cat "files/toolbar-$name.png" > "$name.png"
    file "$name.png" | grep -q "PNG image data"
done
