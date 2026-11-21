#!/usr/bin/env bash
# 在 Android 设备或 Linux 上离线构建《墨塔》。
# 用法：bash build.sh [gradle 任务...]        默认 :app:assembleStandardRelease
# 环境：JAVA_HOME（JDK 17）、ANDROID_HOME、GRADLE_USER_HOME、可选 AAPT2_BIN
set -u

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-arm64}"
export PATH="$JAVA_HOME/bin:$PATH"

: "${ANDROID_HOME:=${ANDROID_SDK_ROOT:-$HOME/android-sdk}}"
export ANDROID_HOME
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
GRADLE_BIN="${GRADLE_BIN:-gradle}"

ARGS=()
# arm64 Android 上跑不了官方 aapt2，需要指向本机编译的二进制
if [ -n "${AAPT2_BIN:-}" ]; then
  ARGS+=("-Pandroid.aapt2FromMavenOverride=$AAPT2_BIN")
fi

if [ $# -eq 0 ]; then set -- :app:assembleStandardRelease; fi
cd "$PROJ_DIR" || exit 1
"$GRADLE_BIN" "$@" --no-daemon ${ARGS[@]+"${ARGS[@]}"}
