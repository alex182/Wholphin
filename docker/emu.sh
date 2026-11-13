#!/usr/bin/env bash
# Helper for the emulator container.
#   docker/emu.sh up                 start the test server and a fresh emulator
#   docker/emu.sh down               stop them
#   docker/emu.sh install            install the latest x86_64 debug APK
#   docker/emu.sh shot <name>        save a screenshot to build/a11y-shots/<name>.png
#   docker/emu.sh key <KEY...>       send key events (e.g. DPAD_DOWN DPAD_CENTER)
#   docker/emu.sh login              clear app data and sign in to the test server
#   docker/emu.sh start              bring the app to the front
#   docker/emu.sh a11y               from home, open Settings > Accessibility
#   docker/emu.sh prefs <n=v...>     set accessibility settings by proto field number
#   docker/emu.sh adb <args...>      run any adb command
set -euo pipefail
C=wholphin-a11y-emu
cmd="${1:-}"; shift || true
case "$cmd" in
  up)
    # Start the test Jellyfin server (kept between runs) and a fresh emulator, then wait for boot
    REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
    docker network create wholphin-a11y >/dev/null 2>&1 || true
    docker start wholphin-a11y-jellyfin >/dev/null 2>&1 || {
      echo "No test Jellyfin server found; see ACCESSIBILITY_ROADMAP.md for how it was set up" >&2; exit 1; }
    docker rm -f "$C" >/dev/null 2>&1 || true
    docker run -d --name "$C" --network wholphin-a11y --device /dev/kvm \
      --group-add "$(stat -c %g /dev/kvm)" -v "$REPO_DIR":/workspace wholphin-a11y-emulator \
      bash -c 'adb start-server >/dev/null 2>&1; emulator -avd tv -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -no-snapshot -memory 3072 > /tmp/emu.log 2>&1' >/dev/null
    docker exec "$C" bash -c 'adb wait-for-device; for i in $(seq 1 90); do [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d "\r")" = 1 ] && exit 0; sleep 5; done; exit 1'
    echo "Emulator ready" ;;
  down)
    docker rm -f "$C" >/dev/null 2>&1 || true
    docker stop wholphin-a11y-jellyfin >/dev/null 2>&1 || true ;;
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
    a pm clear com.github.alex182.wholphin.accessible.debug >/dev/null || true
    a am start -n com.github.alex182.wholphin.accessible.debug/com.github.damontecres.wholphin.MainActivity >/dev/null 2>&1
    sleep 8
    if [ "${WIZARD:-skip}" = "stop" ]; then exit 0; fi   # WIZARD=stop leaves the setup screen open
    a input keyevent KEYCODE_DPAD_CENTER; sleep 3        # "Continue" on the accessibility setup screen
    a input keyevent KEYCODE_DPAD_CENTER; sleep 2; a input keyevent KEYCODE_DPAD_CENTER; sleep 2
    a input text "http://$JF_IP:8096"; sleep 1; a input keyevent KEYCODE_ENTER; sleep 6
    a input keyevent KEYCODE_DPAD_CENTER; sleep 2; a input keyevent KEYCODE_DPAD_CENTER; sleep 2
    a input text test; sleep 1; a input keyevent KEYCODE_ENTER; sleep 1
    a input text test; sleep 1; a input keyevent KEYCODE_ENTER; sleep 8 ;;
  start)
    # Bring the app to the front and wait until it really is the top activity
    for _ in 1 2 3 4 5; do
      docker exec "$C" adb shell am start -n com.github.alex182.wholphin.accessible.debug/com.github.damontecres.wholphin.MainActivity >/dev/null 2>&1 || true
      sleep 4
      top=$(docker exec "$C" adb shell dumpsys activity activities 2>/dev/null | grep -m1 "topResumedActivity=" || true)
      if [[ "$top" == *wholphin* ]]; then sleep 5; exit 0; fi
    done
    echo "App did not come to the front" >&2; exit 1 ;;
  a11y)
    # From the home page with a card focused, open Settings > Accessibility
    # Settings is the last item in the side menu, so pressing down repeatedly always reaches it
    "$0" key DPAD_LEFT DPAD_LEFT DPAD_DOWN DPAD_DOWN DPAD_DOWN DPAD_DOWN DPAD_DOWN DPAD_DOWN DPAD_DOWN DPAD_DOWN DPAD_CENTER; sleep 2; "$0" key DPAD_CENTER; sleep 2 ;;
  prefs)
    # Set accessibility settings directly, e.g.: docker/emu.sh prefs 1=1 11=125 30=1
    # (field numbers from WholphinAccessibility.proto). Restarts the app.
    PKG=com.github.alex182.wholphin.accessible.debug
    HERE="$(cd "$(dirname "$0")" && pwd)"
    HEX=$(python3 -I "$HERE/a11y_prefs.py" "$@" | od -An -v -tx1 | tr -d ' \n' | sed 's/../\\x&/g')
    docker exec "$C" adb shell am force-stop "$PKG"
    docker exec "$C" adb shell "run-as $PKG sh -c 'printf \"$HEX\" >> files/datastore/app_preferences.pb'"
    "$0" start ;;
  adb)
    docker exec "$C" adb "$@" ;;
  *) echo "usage: $0 install|shot|key|adb"; exit 1 ;;
esac
