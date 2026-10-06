# DMs Only

A small personal-use Android WebView wrapper for Instagram Direct Messages.

## Architecture

The app is deliberately small:

- Jetpack Compose provides only the native shell, loading/error UI, and session menu.
- Android WebView renders Instagram's real web DM interface.
- InstagramWebViewClient owns URL policy, external-link handling, errors, SSL rejection, and renderer-crash handling.
- InstagramRoutes owns the route allow/block rules.
- InstagramUiShield contains the maintainable DOM/CSS shield plus SPA history handling.
- InstagramWebChromeClient connects Instagram's HTML file chooser to Android's system document picker.
- There is no backend and no JavaScript-to-Android bridge.

## What it does

- Opens Instagram Direct at https://www.instagram.com/direct/inbox/.
- Uses Instagram's normal web authentication and WebView session cookies.
- Keeps Instagram's actual web DM interface instead of recreating it.
- Blocks navigation to Feed, Explore, and Reels.
- Re-applies the UI shield after DOM mutations and SPA history changes.
- Supports Android's system document picker for DM media uploads.
- Has no backend, analytics, credential storage, scraping, or private Instagram API.
- Provides an explicit Clear Instagram Session action.

## Requirements

- Android Studio with a JDK 17 runtime.
- Android SDK Platform 37.
- A current Android System WebView/Chrome provider on the device.

Current build choices are Android Gradle Plugin 9.4.0, Gradle 9.6.0, Kotlin 2.4.10, Compose BOM 2026.09.00, and AndroidX WebKit 1.17.1.

## Privacy model

The app never reads username/password fields and does not expose an Android JavaScript bridge to the page. Instagram credentials are entered into Instagram's own web UI. Session state is maintained by Android WebView's normal cookie/storage mechanisms.

The WebView stores normal website state such as cookies, local storage, cache, and history under Android's app-private WebView storage. The app does not copy those credentials/session values into SharedPreferences, a database, logs, analytics, or a server.

The Clear Instagram Session action explicitly clears this app's WebView cookies, cache, and history.

## Build and run

1. Open the repository in Android Studio.
2. Let Android Studio import the Gradle project.
3. Install Android SDK Platform 37 if prompted.
4. Use JDK 17 for Gradle.
5. Sync Project with Gradle Files.
6. Build > Make Project.
7. Connect your Android phone with USB debugging enabled, or start an emulator.
8. Run the app.
9. Instagram's normal login/challenge/2FA UI will appear if a session is not already present.
10. Verify that the Direct inbox loads.
11. Try the Instagram logo, /explore/, /reels/, and the root URL; they should return to the DM inbox.
12. Test sending a DM and selecting an image through the system picker.

Android Studio can generate the Gradle wrapper for the installed Gradle version if your checkout does not already contain wrapper scripts.

## ADB

Install a debug APK:

    adb install -r app/build/outputs/apk/debug/app-debug.apk

Clear the app's local data:

    adb shell pm clear com.example.dmsonly

Open the app:

    adb shell monkey -p com.example.dmsonly 1

Useful WebView/Chromium log filtering:

    adb logcat | grep -iE "chromium|WebView|dmsonly"

Windows PowerShell equivalent:

    adb logcat | Select-String -Pattern "chromium|WebView|dmsonly"

## Navigation policy

Only main-frame Instagram URLs are considered by the native route blocker. Instagram resources such as CSS, JavaScript, fonts, images, and media are not blocked by the route policy.

Blocked Instagram destinations are:

- /
- /explore
- /explore/*
- /reels
- /reels/*

Authentication/challenge routes such as /accounts/login/*, /challenge/*, /checkpoint/*, /oauth/*, and /consent/* are allowed so Instagram can complete its own authentication flow.

Non-Instagram HTTP(S) links are opened by the Android system browser/app. Non-HTTP(S) external schemes are not handed to the WebView.

This is navigation restriction, not an Instagram security feature.

## DOM/CSS shield maintenance

Open:

    app/src/main/java/com/example/dmsonly/web/InstagramUiShield.kt

The SELECTORS list is the maintenance point. It targets Feed, Explore, and Reels links using URL and semantic fallbacks. The MutationObserver re-applies the shield after dynamic DOM updates, but batches work through requestAnimationFrame so it does not run a continuous tight loop.

The script also observes pushState, replaceState, and popstate. If Instagram's SPA changes to a blocked route without a normal WebView navigation callback, the script redirects back to the DM inbox.

## Testing checklist

### Authentication

- [ ] First login
- [ ] Wrong password
- [ ] 2FA
- [ ] Login challenge
- [ ] Session persists after force-close/reopen
- [ ] Clear Instagram session logs the WebView out
- [ ] Reopening after logout shows Instagram login

### DM functionality

- [ ] Inbox
- [ ] Individual conversation
- [ ] Send message
- [ ] Receive message
- [ ] Group chat
- [ ] Message request
- [ ] Story reply where Instagram web supports it
- [ ] Image/video attachment where Instagram web supports it

### Navigation shielding

- [ ] Tap Instagram logo
- [ ] Navigate to /
- [ ] Navigate to /explore/
- [ ] Navigate to /reels/
- [ ] Android Back
- [ ] Direct blocked URL
- [ ] SPA navigation
- [ ] Refresh
- [ ] Dynamic DOM changes

### WebView stability

- [ ] Rotation
- [ ] Background/foreground
- [ ] Network disconnect/reconnect
- [ ] Slow connection
- [ ] WebView renderer process death/recreation
- [ ] External link opens outside the WebView
- [ ] SSL errors are rejected rather than bypassed

## Known limitations

- This is a WebView wrapper around Instagram's website, not an official Instagram Android client.
- Instagram can change its DOM, routes, login flow, or WebView compatibility at any time.
- CSS/JS selectors can therefore require maintenance.
- Instagram may detect or restrict unusual web sessions.
- Some features can behave differently from the native Instagram app.
- There is no guarantee that every Instagram feature will remain available in a WebView.
- WebView cookies/session data are protected by Android's app-private storage and normal WebView mechanisms, but the app cannot honestly promise that authentication is "100% safe".
- Navigation blocking and DOM shielding reduce distraction; they are not Instagram security controls.

## What the app intentionally does not do

- No Instagram private/internal API.
- No scraping.
- No credential inspection.
- No password storage.
- No custom Instagram login screen.
- No proxy server.
- No analytics.
- No unnecessary camera, microphone, contacts, storage, or location permissions.
