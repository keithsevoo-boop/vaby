VYBE FAST APK BUILDER

Double-click BUILD_APK.bat. First build needs internet to download Gradle 8.9, Android plugins and Compose dependencies. Subsequent builds reuse the installed Gradle distribution, the Gradle daemon, dependency caches, build cache and configuration cache.

For best speed, keep this folder in the same place and do not delete .gradle, .tools or app/build. Keep your JDK 17 and Android SDK Platform 35 installed.

Build command: assembleDebug. The script tries offline first and automatically retries online when the cache is incomplete. It does NOT guarantee faster first builds or a successful compilation. If it fails, share build-log.txt.

APK after success: app\build\outputs\apk\debug\app-debug.apk
