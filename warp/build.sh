#!/bin/sh
# Cross-compiles the WARP helper into the APK's native lib folders (static Linux binaries run fine on Android).
set -e
cd "$(dirname "$0")"
CGO_ENABLED=0 GOOS=linux GOARCH=arm64 go build -trimpath -ldflags '-s -w' -o ../jni/arm64-v8a/libwarp.so .
CGO_ENABLED=0 GOOS=linux GOARCH=arm GOARM=7 go build -trimpath -ldflags '-s -w' -o ../jni/armeabi-v7a/libwarp.so .
