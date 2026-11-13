#!/usr/bin/env bash
# Helper for the emulator container.
#   docker/emu.sh install            install the latest x86_64 debug APK
#   docker/emu.sh shot <name>        save a screenshot to build/a11y-shots/<name>.png
#   docker/emu.sh key <KEY...>       send key events (e.g. DPAD_DOWN DPAD_CENTER)
#   docker/emu.sh login              clear app data and sign in to the test server
#   docker/emu.sh start              bring the app to the front
#   docker/emu.sh a11y               from home, open Settings > Accessibility
#   docker/emu.sh adb <args...>      run any adb command
set -euo pipefail
C=wholphin-a11y-emu
cmd="${1:-}"; shift || true
case "$cmd" in
  install)
    docker exec "$C" bash -c 'adb install -r -g $(ls -t /workspace/app/build/outputs/apk/default/debug/*x86_64.apk | head -1)' ;;
  shot)
    docker exec "$C" bash -c "mkdir -p /workspace/build/a11y-shots && adb exec-out screencap -p > /workspace/build/a11y-shots/$1.png && convert /workspace/build/a11y-shots/$1.png -resize 960x /workspace/build/a11y-shots/$1.png" ;;
  key)
    for k in "$@"; do docker exec "$C" adb shell input keyevent "KEYCODE_$k"; sleep 0.4; done ;;
  login)
    # Fresh install: add the test Jellyfin server and sign in as test/test
    JF_IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' wholphin-a11y-jellyfin)
    a() { docker exec "$C" adb shell "$@"; }
    a pm clear com.github.damontecres.wholphin.debug >/dev/null || true
    a am start -n com.github.damontecres.wholphin.debug/com.github.damontecres.wholphin.MainActivity >/dev/null 2>&1
    sleep 8; a input keyevent KEYCODE_DPAD_CENTER; sleep 2; a input keyevent KEYCODE_DPAD_CENTER; sleep 2
    a input text "http://$JF_IP:8096"; sleep 1; a input keyevent KEYCODE_ENTER; sleep 6
    a input keyevent KEYCODE_DPAD_CENTER; sleep 2; a input keyevent KEYCODE_DPAD_CENTER; sleep 2
    a input text test; sleep 1; a input keyevent KEYCODE_ENTER; sleep 1
    a input text test; sleep 1; a input keyevent KEYCODE_ENTER; sleep 8 ;;
  start)
    docker exec "$C" adb shell am start -n com.github.damontecres.wholphin.debug/com.github.damontecres.wholphin.MainActivity >/dev/null 2>&1; sleep 8 ;;
  a11y)
    # From the home page with a card focused, open Settings > Accessibility
    "$0" key DPAD_LEFT DPAD_DOWN DPAD_DOWN DPAD_DOWN DPAD_DOWN DPAD_DOWN DPAD_CENTER; sleep 2; "$0" key DPAD_CENTER; sleep 2 ;;
  adb)
    docker exec "$C" adb "$@" ;;
  *) echo "usage: $0 install|shot|key|adb"; exit 1 ;;
esac
