#!/bin/bash
# Manual build for Sacred Verses — no Gradle daemon needed.
# Uses aapt2 + javac + d8 + apksigner straight from the local Android SDK.
set -e

PROJ=/home/hatch/workspace/sacred-verses-app
SDK=$PROJ/android-sdk
BT=$SDK/build-tools/34.0.0
JDK=$PROJ/_tools/jdk-17.0.20.1+1
OUT=$PROJ/manual-out
export PATH="$JDK/bin:$PATH"

rm -rf "$OUT"
mkdir -p "$OUT/compiled_res" "$OUT/gen" "$OUT/classes" "$OUT/dex"

echo "[1/7] compiling resources..."
"$BT/aapt2" compile --dir "$PROJ/app/src/main/res" -o "$OUT/compiled_res.zip"

echo "[2/7] linking..."
"$BT/aapt2" link -o "$OUT/app-unsigned.apk" \
  -I "$SDK/platforms/android-34/android.jar" \
  --manifest "$PROJ/app/src/main/AndroidManifest.xml" \
  --min-sdk-version 26 --target-sdk-version 34 \
  -A "$PROJ/app/src/main/assets" \
  --java "$OUT/gen" \
  "$OUT/compiled_res.zip"

echo "[3/7] compiling java..."
find "$PROJ/app/src/main/java" "$OUT/gen" -name "*.java" > "$OUT/sources.txt"
"$JDK/bin/javac" -source 8 -target 8 -nowarn -encoding UTF-8 \
  -cp "$SDK/platforms/android-34/android.jar" \
  -d "$OUT/classes" @"$OUT/sources.txt"

echo "[4/7] dexing..."
"$BT/d8" --lib "$SDK/platforms/android-34/android.jar" \
  --min-api 26 --output "$OUT/dex" $(find "$OUT/classes" -name "*.class")

echo "[5/7] adding classes.dex to apk..."
cd "$OUT/dex" && zip -X -q ../app-unsigned.apk classes.dex && cd ..

echo "[6/7] debug keystore..."
KS="$OUT/debug.keystore"
if [ ! -f "$KS" ]; then
  "$JDK/bin/keytool" -genkeypair -keystore "$KS" -alias androiddebugkey \
    -storepass android -keypass android -keyalg RSA -keysize 2048 \
    -validity 10950 -dname "CN=Android Debug,O=Android,C=US" 2>/dev/null
fi

echo "[7/7] zipalign + sign..."
"$BT/zipalign" -f 4 "$OUT/app-unsigned.apk" "$OUT/app-aligned.apk"
"$BT/apksigner" sign --ks "$KS" --ks-pass pass:android --key-pass pass:android \
  --out "$OUT/SacredVerses-v1.0.apk" "$OUT/app-aligned.apk"

echo "--- verify ---"
"$BT/apksigner" verify "$OUT/SacredVerses-v1.0.apk" && echo "SIGNATURE OK"
"$BT/aapt2" dump badging "$OUT/SacredVerses-v1.0.apk" | head -3
ls -la "$OUT/SacredVerses-v1.0.apk"
echo "BUILD SUCCESS"
