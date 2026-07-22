#!/bin/bash
# Deploy the latest release APK to the web server's static file directory

set -e

APK_PATH="app/build/outputs/apk/release/app-release.apk"
WEB_DIST="/opt/isaacs-hub-storage/web/dist"
TARGET_NAME="isaacs-hub-release.apk"

if [ ! -f "$APK_PATH" ]; then
    echo "Error: APK not found at $APK_PATH"
    echo "Run './gradlew assembleRelease' first"
    exit 1
fi

if [ ! -d "$WEB_DIST" ]; then
    echo "Error: Web dist directory not found at $WEB_DIST"
    exit 1
fi

echo "Copying APK to web server..."
sudo cp -v "$APK_PATH" "$WEB_DIST/$TARGET_NAME"

echo "Verifying..."
curl -I "http://isaacs-hub.playit.plus/$TARGET_NAME" 2>&1 | grep -E "HTTP|Content-Length"

echo ""
echo "✓ APK deployed successfully"
echo "  URL: http://isaacs-hub.playit.plus/$TARGET_NAME"
echo "  Size: $(ls -lh "$WEB_DIST/$TARGET_NAME" | awk '{print $5}')"
