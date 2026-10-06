# DMs Only

A small personal-use Android WebView wrapper for Instagram Direct Messages.

## What it does

- Opens Instagram Direct at https://www.instagram.com/direct/inbox/.
- Uses Instagram's normal web authentication and WebView session cookies.
- Keeps Instagram's actual web DM interface instead of recreating it.
- Blocks navigation to Feed, Explore, and Reels.
- Adds a lightweight DOM/CSS shield for distracting navigation links.
- Supports Android's system document picker for DM media uploads.
- Has no backend, analytics, credential storage, scraping, or private Instagram API.
- Provides an explicit Clear Instagram Session action.

## Requirements

- Android Studio with a JDK 17 runtime.
- Android SDK Platform 37.
- An Android device/emulator with a current Android System WebView/Chrome provider.

The project currently uses Android Gradle Plugin 9.4.0, Gradle 9.6.0, Kotlin 2.4.10, Compose BOM 2026.09.00, and AndroidX WebKit 1.17.1.

## Privacy model

The app never reads username/password fields and does not expose an Android JavaScript bridge to the page. Instagram credentials are entered into Instagram's own web UI. Session state is maintained by Android WebView's normal cookie/storage mechanisms.

Clearing the session is deliberately an explicit native action and removes WebView cookies/cache/history for this app.

## Important limitation

This is a WebView wrapper around Instagram's website, not an official Instagram Android client. Instagram can change routes and DOM structure at any time, so the navigation rules and UI shield may need maintenance.

See app/src/main/java/com/example/dmsonly/web/InstagramUiShield.kt for the clearly marked selector/routing section.
