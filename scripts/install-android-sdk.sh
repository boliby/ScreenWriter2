#!/usr/bin/env bash
# Installs the Android SDK packages this project builds against and points
# local.properties at them. Cloud sessions don't ship an Android SDK; this
# needs dl.google.com to be reachable. Safe to re-run.
set -euo pipefail

SDK_DIR="${ANDROID_HOME:-$HOME/android-sdk}"
CMDLINE_TOOLS_ZIP="commandlinetools-linux-16111833_latest.zip"
# Keep in step with compileSdk in app/build.gradle.kts and the build-tools
# version the Android Gradle Plugin picks.
PACKAGES=("platforms;android-37.0" "build-tools;36.0.0")

if [[ ! -x "$SDK_DIR/cmdline-tools/latest/bin/android" ]]; then
  tmp="$(mktemp -d)"
  trap 'rm -rf "$tmp"' EXIT
  curl -sSfL -o "$tmp/tools.zip" "https://dl.google.com/android/repository/$CMDLINE_TOOLS_ZIP"
  unzip -q "$tmp/tools.zip" -d "$tmp"
  mkdir -p "$SDK_DIR/cmdline-tools"
  rm -rf "$SDK_DIR/cmdline-tools/latest"
  mv "$tmp/cmdline-tools" "$SDK_DIR/cmdline-tools/latest"
fi

"$SDK_DIR/cmdline-tools/latest/bin/android" --no-metrics --sdk="$SDK_DIR" sdk install "${PACKAGES[@]}"

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
echo "sdk.dir=$SDK_DIR" > "$repo_root/local.properties"
echo "Android SDK ready at $SDK_DIR"
