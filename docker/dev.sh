#!/usr/bin/env bash
# Run a command inside the dev container, e.g.:
#   docker/dev.sh ./gradlew assembleDefaultDebug
#   docker/dev.sh            (interactive shell)
set -euo pipefail
REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
IMAGE=wholphin-a11y-dev
if ! docker image inspect "$IMAGE" >/dev/null 2>&1; then
  docker build --build-arg USER_ID="$(id -u)" --build-arg GROUP_ID="$(id -g)" -t "$IMAGE" "$REPO_DIR/docker"
fi
TTY=(); [ -t 0 ] && TTY=(-it)
exec docker run --rm "${TTY[@]}" \
  -v "$REPO_DIR":/workspace \
  -v wholphin-a11y-gradle:/home/dev/.gradle \
  -e GRADLE_OPTS="-Dorg.gradle.jvmargs='-Xmx8g -XX:MaxMetaspaceSize=1024m'" \
  "$IMAGE" "${@:-bash}"
