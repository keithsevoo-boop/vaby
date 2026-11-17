VYBE 2.0 — STAGED ANDROID PROTOTYPE
===================================
BUILD ON WINDOWS
1. Extract the ZIP to a writable folder, e.g. Desktop\VYBE_Android.
2. Install JDK 17 and Android Studio Android SDK Platform 35.
3. Double-click BUILD_APK.bat (first build needs internet).
4. Find the debug APK in app\build\outputs\apk\debug\app-debug.apk.
5. If it fails, send build-log.txt and the full terminal error.
The APK has not been compiled or device-tested in this environment.

WORKING IN SOURCE (LOCAL / SINGLE DEVICE)
- Five tabs: Home, Clips, Create, Chats, Profile
- Midnight V launcher icon
- Persistent local text posts, likes, bookmarks, text sharing, post search
- Draft autosave and local profile username
- Local chat notes demo (NOT actual messaging)
- Direct authorized HTTPS MP4 downloads via Android DownloadManager
- Offline video playback via installed player; download list and deletion
- Local light/dark theme preference and basic settings
- NEW: Feature Hub accessible from Home, Settings, Profile
- NEW: Camera Studio: take photo with camera intent or pick photo from gallery, save image locally, save caption
- NEW: 24-hour local text statuses, filtered after expiry
- NEW: Local community name creation (no network members)
- NEW: Planned post drafts (no scheduled online publishing)
- NEW: Local creator dashboard counts
- NEW: Data Saver and Wi-Fi-only future auto-download preferences (not yet enforced on video downloads)
- NEW: Roadmap screen for online features

NOT IMPLEMENTED YET (NEEDS CLOUD/INTEGRATIONS)
- Smart For You recommendations and full swipeable video feed
- Real voice/video calls, livestreaming, remote chats and group chats
- Instant translation, AI Studio, licensed music catalog
- VYBE Shop/payments, challenges/duets, real scheduled publishing
- Push notifications, server-side moderation and blocking
- Cloud backup, cross-device sync, floating mini-player
- Online communities, public stories, public profiles, cloud analytics
- Automatic Wi-Fi offline video playlists, actual streaming data saver
- TikTok/Instagram recommendation feed imports (not freely available)

SECURITY / CONTENT
Local privacy toggles are preferences only, not server-side enforcement.
Do not use local demo chat for private conversations.
Only download videos you have the right to save. No scraping or bypassing restrictions.

RELIABILITY
This is an incremental source update, not a finished social network.
Online capabilities require accounts, secure backend, moderation, licensing and testing.
