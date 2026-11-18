#!/bin/bash
set -euo pipefail
cd -- "$(dirname -- "$0")"

tools="${PIXEL_WINDOW_ANDROID_TOOLS:-/tmp/codex-pet-android-tools}"
build="$(mktemp -d "${TMPDIR:-/tmp}/pixel-window-build.XXXXXX")"
trap 'rm -rf -- "$build"' EXIT
sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
build_tools="$sdk/build-tools/${ANDROID_BUILD_TOOLS_VERSION:-34.0.0}"
java_home="${JAVA_HOME:-$(/usr/libexec/java_home -F -v 21 2>/dev/null || true)}"
if [[ -z "$java_home" ]]; then
  java_home='/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home'
fi
d8_jar="${PIXEL_WINDOW_D8_JAR:-$build_tools/lib/d8.jar}"
if [[ ! -s "$d8_jar" && -z "${PIXEL_WINDOW_D8_JAR:-}" ]]; then
  d8_jar=/opt/homebrew/share/android-commandlinetools/cmdline-tools/latest/lib/r8.jar
fi
if [[ ! -x "$java_home/bin/javac" || ! -s "$d8_jar" ]]; then
  echo "需要 JDK 21 和 Android SDK Build-Tools 34.0.0。可用 JAVA_HOME、ANDROID_HOME 和 PIXEL_WINDOW_D8_JAR 指定位置。"
  exit 1
fi
signing_dir="$HOME/Library/Application Support/Codex Display"
artifact="${PIXEL_WINDOW_APK:-$PWD/像素窗景.apk}"

mkdir -p "$tools" "$build/classes" "$build/dex"
platform_jar="$sdk/platforms/android-34/android.jar"
if [[ ! -s "$platform_jar" ]]; then
  platform_jar="$tools/android.jar"
fi
if [[ ! -s "$platform_jar" ]]; then
  curl -fLsS --retry 2 'https://android.googlesource.com/platform/prebuilts/sdk/+/refs/heads/main/34/public/android.jar?format=TEXT' -o "$build/platform.txt"
  base64 -D -i "$build/platform.txt" -o "$build/android.jar"
  mv "$build/android.jar" "$tools/android.jar"
fi
aapt2="$build_tools/aapt2"
if [[ ! -x "$aapt2" ]]; then
  aapt2="$tools/aapt2"
fi
if [[ ! -x "$aapt2" ]]; then
  curl -fLsS --retry 2 'https://dl.google.com/dl/android/maven2/com/android/tools/build/aapt2/8.7.3-12006047/aapt2-8.7.3-12006047-osx.jar' -o "$build/aapt2.jar"
  unzip -qo "$build/aapt2.jar" -d "$tools"
  chmod +x "$tools/aapt2"
fi
if [[ ! -s "$tools/apksig.jar" ]]; then
  curl -fLsS --retry 2 'https://dl.google.com/dl/android/maven2/com/android/tools/build/apksig/8.7.3/apksig-8.7.3.jar' -o "$build/apksig.jar"
  mv "$build/apksig.jar" "$tools/apksig.jar"
fi

"$aapt2" compile --dir android/res -o "$build/resources.zip"
"$aapt2" link -I "$platform_jar" --manifest android/AndroidManifest.xml \
  --java "$build/generated" -o "$build/unsigned.apk" "$build/resources.zip"
"$java_home/bin/javac" --release 11 -encoding UTF-8 -classpath "$platform_jar" -d "$build/classes" \
  "$build/generated/local/pixel/window/R.java" android/src/local/pixel/window/*.java
(cd "$build/classes" && zip -qr "$build/classes.jar" .)
"$java_home/bin/java" -cp "$d8_jar" com.android.tools.r8.D8 \
  --lib "$platform_jar" --min-api 30 --output "$build/dex" "$build/classes.jar"
zip -qj "$build/unsigned.apk" "$build/dex/classes.dex"

umask 077
signing_key="${PIXEL_WINDOW_SIGNING_KEY:-$signing_dir/signing.p12}"
if [[ ! -f "$signing_key" ]]; then
  mkdir -p -- "$(dirname -- "$signing_key")"
  "$java_home/bin/keytool" -genkeypair -storetype PKCS12 -keystore "$signing_key" \
    -storepass android -alias dashboard -keyalg RSA -keysize 2048 -validity 3650 -dname 'CN=Local Codex Display'
fi
"$java_home/bin/java" --class-path "$tools/apksig.jar" android/SignApk.java \
  "$signing_key" "$build/unsigned.apk" "$build/像素窗景.apk"
mv "$build/像素窗景.apk" "$artifact"
chmod 644 "$artifact"
echo "$artifact"
