# DMs Only

**DMs Only** is a small, personal-use Android app that opens Instagram Direct in an Android WebView and hides/blocks navigation to the Feed, Explore, and Reels.

It uses Instagram's real website and normal Instagram login flow. It is not an official Instagram app.

## Contents

- [Features](#features)
- [How it works](#how-it-works)
- [Requirements](#requirements)
- [Installation](#installation)
  - [Option A: Android Studio](#option-a-build-and-run-with-android-studio)
  - [Option B: Command line](#option-b-build-from-the-command-line)
- [Install the APK on a phone](#install-the-apk-on-a-phone)
- [First launch and sign-in](#first-launch-and-sign-in)
- [Build variants and useful commands](#build-variants-and-useful-commands)
- [Project structure](#project-structure)
- [Privacy and security notes](#privacy-and-security-notes)
- [Troubleshooting](#troubleshooting)
- [Testing checklist](#testing-checklist)
- [Known limitations](#known-limitations)

## Features

- Opens Instagram Direct at [instagram.com/direct/inbox](https://www.instagram.com/direct/inbox/).
- Uses Instagram's own website login, including its supported verification and challenge screens.
- Keeps WebView cookies and website storage so the session can persist between launches.
- Blocks top-level navigation to Instagram Feed, Explore, and Reels.
- Adds a DOM/JavaScript UI shield to re-hide those destinations when Instagram dynamically updates its page.
- Uses Android's system document picker for file attachments supported by Instagram Web.
- Provides Refresh and Clear Instagram Session actions.
- Handles loading states, retryable page errors, external links, SSL errors, and WebView renderer termination.
- Includes unit tests for URL-routing rules and a GitHub Actions build workflow.

## How it works

The app is a native Kotlin Android shell around Instagram's website:

- **Jetpack Compose + Material 3** provide the app bar, loading indicator, error UI, and menu.
- **Android WebView** renders Instagram's actual web interface.
- `InstagramWebViewClient` handles route policy, external links, page errors, SSL errors, and renderer-process errors.
- `InstagramRoutes` contains the route allow/block rules.
- `InstagramUiShield` hides Feed/Explore/Reels links and responds to dynamic page updates and SPA history changes.
- `InstagramWebChromeClient` connects web file selection to Android's system document picker.

There is no app backend and no JavaScript-to-Android bridge.

## Requirements

### Development computer

- Windows, macOS, or Linux computer.
- [Android Studio](https://developer.android.com/studio).
- **JDK 17** for Gradle builds.
- Android SDK Platform **37** and a compatible Android SDK Build-Tools version installed through SDK Manager.
- **Gradle 9.6.0** if building from the command line with the repository as currently checked in.
- Internet access during the initial dependency download and Gradle sync.

### Android device or emulator

- Android **7.0 (API 24)** or later (the project's `minSdk` is 24).
- A current Android System WebView or Chrome provider.
- Internet access.
- An Instagram account if you want to sign in and use your DMs.

### Versions configured in this repository

| Component | Configured version |
| --- | --- |
| Android Gradle Plugin | 9.4.0 |
| Gradle | 9.6.0 |
| Kotlin | 2.4.10 |
| Java source/target | 17 |
| Compile SDK | 37 |
| Target SDK | 37 |
| Minimum SDK | 24 |
| Compose BOM | 2026.09.00 |
| AndroidX WebKit | 1.17.1 |

The versions above reflect the current project files. If Android Studio reports that a component is unavailable in your installation channel, update Android Studio/SDK Manager or use the versions configured in the project rather than changing versions at random.

## Installation

### Option A: Build and run with Android Studio

#### 1. Install Android Studio

Download and install [Android Studio](https://developer.android.com/studio). Open it once and complete its setup wizard so it can install the Android SDK tools.

#### 2. Install the required SDK

In Android Studio:

1. Open **Tools → SDK Manager** (or use **More Actions → SDK Manager** from the welcome screen).
2. In **SDK Platforms**, enable **Android API 37** / **Android SDK Platform 37**.
3. In **SDK Tools**, ensure **Android SDK Build-Tools**, **Android SDK Platform-Tools**, and **Android SDK Command-line Tools** are installed.
4. Click **Apply** and wait for the installation to finish.

If API 37 is not shown, check that Android Studio and its SDK package lists are up to date. Do not change `compileSdk = 37` just to silence a missing-SDK warning unless you intend to change and validate the project configuration.

#### 3. Install/select JDK 17

Android Studio includes a runtime for the IDE, but Gradle must use a compatible JDK. In Android Studio, open **Settings/Preferences → Build, Execution, Deployment → Build Tools → Gradle** and set **Gradle JDK** to JDK 17. The exact label can differ slightly by Android Studio version.

You can verify a terminal JDK with:

```bash
java -version
```

The output should report version 17. If multiple JDKs are installed, make sure Android Studio's Gradle JDK and your terminal's `JAVA_HOME` point to JDK 17.

#### 4. Get the source code

Either download the ZIP from GitHub, or clone the repository:

```bash
git clone https://github.com/Syed-Haseeb97/quit_addiction.git
cd quit_addiction
```

If you downloaded a ZIP, extract it and open the extracted project folder. The folder you open should contain `settings.gradle.kts`, `build.gradle.kts`, and the `app/` directory.

#### 5. Configure Gradle for this checkout

**Important:** this repository currently does not include Gradle Wrapper scripts (`gradlew`, `gradlew.bat`) or the wrapper JAR.

Choose one of these approaches:

- **Recommended for repeatable local builds:** install Gradle 9.6.0, open a terminal in the project root, and run `gradle wrapper --gradle-version 9.6.0`. This generates wrapper files for your local checkout. Keep the generated wrapper files if you want to commit them in your own branch.
- **Use an installed Gradle distribution:** install Gradle 9.6.0 and configure Android Studio to use that local Gradle installation in the Gradle settings. This option depends on the Android Studio version you have installed.

See the [official Gradle installation guide](https://gradle.org/install/) for installing Gradle. Once the wrapper exists, use `./gradlew` on macOS/Linux or `gradlew.bat` on Windows for the commands below.

#### 6. Open and sync the project

1. In Android Studio, select **Open** and choose the repository root directory.
2. Accept the trust/import prompts if shown.
3. Allow Gradle to sync and download dependencies.
4. If Android Studio asks to install missing SDK components, install the versions required by the project.
5. Wait for sync to finish and check the **Build** tool window for errors.

Do not open only the `app/` subfolder; open the project root containing `settings.gradle.kts`.

#### 7. Choose a device

**Physical Android phone**

1. Enable Developer options on the phone by tapping **Build number** repeatedly in Settings → About phone.
2. In Developer options, enable **USB debugging**.
3. Connect the phone by USB and approve the debugging prompt on the phone.
4. Select the phone from Android Studio's device selector.

**Android emulator**

1. Open **Tools → Device Manager**.
2. Create a virtual device with an Android system image compatible with the project.
3. Start the emulator and select it as the run target.

#### 8. Build and run

Select the **app** run configuration and click **Run** (the green triangle). Android Studio builds and installs the debug app, then launches it.

You can also use **Build → Make Project** to compile without launching.

### Option B: Build from the command line

These commands assume JDK 17, Android SDK Platform 37, Android SDK command-line/build tools, and Gradle 9.6.0 are installed. Run commands from the repository root.

Check Java and Gradle:

```bash
java -version
gradle --version
```

Generate a wrapper once if this checkout does not already have one:

```bash
gradle wrapper --gradle-version 9.6.0
```

Then run the build using the generated wrapper.

**macOS/Linux**

```bash
./gradlew --version
./gradlew test
./gradlew assembleDebug
```

**Windows PowerShell**

```powershell
.\gradlew.bat --version
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

If you prefer to use the installed Gradle executable directly, the equivalent build commands are:

```bash
gradle test
gradle assembleDebug
```

The debug APK should be generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

The first build may take longer because Gradle needs to download the Android Gradle Plugin, Kotlin plugin, AndroidX dependencies, and other build artifacts.

## Install the APK on a phone

After `assembleDebug` succeeds, enable USB debugging and connect your phone. From the project root:

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

If Android reports that the device is unauthorized, unlock the phone and accept its USB debugging prompt. If more than one device is connected, use `adb -s DEVICE_SERIAL install -r app/build/outputs/apk/debug/app-debug.apk`.

Launch the app from the phone's app drawer, or run:

```bash
adb shell monkey -p com.example.dmsonly 1
```

To uninstall it:

```bash
adb uninstall com.example.dmsonly
```

Uninstalling normally removes this app's private WebView session data.

## First launch and sign-in

1. Launch **DMs Only**.
2. Wait for Instagram Direct to load.
3. If Instagram asks you to sign in, enter your details into Instagram's own page inside the WebView.
4. Complete any verification, two-factor authentication, or login challenge Instagram presents.
5. Open a conversation and test reading and sending a message.
6. Test attachment selection if you need to send supported media.

The app does not provide a custom login form and does not read or store your password. Instagram may change its login flow or may restrict some WebView sessions.

### App menu

Tap the three-dot menu in the top bar:

- **Refresh** reloads the current WebView page.
- **Clear Instagram session** asks for confirmation, then clears this app's WebView cookies, cache, and history before loading the DM inbox again. You will normally need to sign in again.

## Build variants and useful commands

| Task | Command |
| --- | --- |
| Run local unit tests | `./gradlew test` |
| Assemble debug APK | `./gradlew assembleDebug` |
| Assemble release APK | `./gradlew assembleRelease` |
| List available tasks | `./gradlew tasks` |
| Install debug APK using ADB | `adb install -r app/build/outputs/apk/debug/app-debug.apk` |
| Clear installed app data | `adb shell pm clear com.example.dmsonly` |

On Windows, replace `./gradlew` with `gradlew.bat`. The release build is not currently configured with release signing credentials; configure signing before distributing a release APK.

Useful WebView logs:

**macOS/Linux**

```bash
adb logcat | grep -iE "chromium|WebView|dmsonly"
```

**Windows PowerShell**

```powershell
adb logcat | Select-String -Pattern "chromium|WebView|dmsonly"
```

## Project structure

```text
.
├── .github/
│   └── workflows/
│       └── android.yml
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/dmsonly/
│       │   │   ├── MainActivity.kt
│       │   │   ├── ui/theme/
│       │   │   │   └── Theme.kt
│       │   │   └── web/
│       │   │       ├── InstagramRoutes.kt
│       │   │       ├── InstagramUiShield.kt
│       │   │       ├── InstagramWebChromeClient.kt
│       │   │       ├── InstagramWebView.kt
│       │   │       └── InstagramWebViewClient.kt
│       │   └── res/values/
│       │       ├── strings.xml
│       │       └── themes.xml
│       └── test/
│           └── java/com/example/dmsonly/web/
│               └── InstagramRoutesTest.kt
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
└── README.md
```

### Where to make changes

- **App shell, top bar, menu, back handling, error overlay:** `MainActivity.kt`
- **WebView settings, cookies, upload picker, lifecycle:** `InstagramWebView.kt`
- **URL blocking and allowed authentication routes:** `InstagramRoutes.kt`
- **Navigation callbacks and WebView error handling:** `InstagramWebViewClient.kt`
- **Feed/Explore/Reels DOM shield selectors:** `InstagramUiShield.kt`
- **File upload callback:** `InstagramWebChromeClient.kt`
- **Build dependencies and SDK versions:** `app/build.gradle.kts` and root `build.gradle.kts`

## Privacy and security notes

- The app declares only the `INTERNET` permission.
- Instagram login happens in Instagram's own website UI.
- The app does not intentionally inspect login fields, store passwords, or send account data to an app backend.
- WebView cookies and website storage are maintained by Android's WebView in this app's private storage.
- No app backend, analytics, scraping, private Instagram API, proxy, or JavaScript-to-Android bridge is included.
- The WebView does not allow local file access, and cleartext HTTP traffic is disabled.
- SSL certificate errors are rejected; the app does not offer a “proceed anyway” bypass.
- **Clear Instagram session** clears this app's WebView cookies, cache, and history.

Only sign in on a device you trust. This project is not an official Instagram product, and no app can guarantee that Instagram's web login/session behavior will remain unchanged.

## Troubleshooting

### Gradle sync says the wrapper is missing

This checkout does not currently contain `gradlew`, `gradlew.bat`, or the Gradle wrapper JAR. Install Gradle 9.6.0 and run:

```bash
gradle wrapper --gradle-version 9.6.0
```

Then retry sync/build with the generated wrapper, or configure Android Studio to use your local Gradle installation.

### “Android SDK Platform 37 not found”

Open SDK Manager and install Android SDK Platform 37. Confirm that Android Studio is using the SDK location where the platform was installed.

### Gradle uses the wrong Java version

Run `java -version` in your terminal and check **Gradle JDK** in Android Studio's Gradle settings. Set both to JDK 17. If necessary, set `JAVA_HOME` to your JDK 17 installation and restart Android Studio/your terminal.

### The app opens but Instagram is blank or cannot sign in

- Check that the phone has internet access.
- Update Android System WebView and Chrome from the Play Store where available.
- Confirm that date and time are set automatically on the phone.
- Use Refresh from the app menu.
- Try signing in through a normal browser to check whether Instagram is asking for account verification.
- Instagram can restrict or change WebView behavior; the app cannot override those restrictions.

### Upload picker does not appear or media cannot be attached

Check whether the particular Instagram Web composer supports the file type and upload flow. The app delegates selection to Android's system document picker and does not request broad storage permissions.

### Feed, Explore, or Reels becomes visible after an Instagram update

Instagram changes its website markup and routes over time. Review `InstagramUiShield.kt` and `InstagramRoutes.kt`, update selectors/rules as necessary, then retest authentication and DM navigation. These UI rules are best-effort and cannot guarantee that every new Instagram interface variant is blocked.

### Check logs

Connect the device with ADB and use the log commands in [Build variants and useful commands](#build-variants-and-useful-commands). Avoid sharing logs publicly without checking them for personal or account-related information.

## Testing checklist

Use this checklist after a build or after changing WebView behavior.

### Authentication and session

- [ ] Fresh install opens the Direct inbox/login flow.
- [ ] Login, two-factor authentication, and challenge flows work when Instagram supports them.
- [ ] Session persists after closing and reopening the app.
- [ ] Clear Instagram session logs the WebView out.
- [ ] Refresh recovers from a transient load error.

### Direct messages

- [ ] Inbox and individual conversations load.
- [ ] Sending and receiving messages works.
- [ ] Group conversations and message requests behave as expected.
- [ ] File picker opens for supported attachments.
- [ ] Story replies work where Instagram Web supports them.

### Navigation and lifecycle

- [ ] Feed/root, Explore, and Reels destinations redirect to the DM inbox.
- [ ] Direct blocked URLs and single-page-app route changes are handled.
- [ ] Android Back navigates browser history appropriately.
- [ ] External HTTP(S) links open outside the app WebView.
- [ ] Rotation, background/foreground, and network loss/recovery are handled.
- [ ] SSL errors are rejected.
- [ ] The unit test suite passes with `./gradlew test`.

## Known limitations

- This is a WebView wrapper around Instagram's website, not an official Instagram client.
- Instagram can change its website DOM, routes, login process, and WebView compatibility without notice.
- Feed/Explore/Reels shielding is best-effort and may need maintenance after website updates.
- Some features available in Instagram's native app may not be supported in Instagram Web.
- Instagram may require extra verification or restrict unusual sessions.
- The repository's CI workflow runs unit tests and assembles a debug APK, but a successful CI result should be confirmed in the repository's **Actions** tab after a push.
- This project has not been verified on every Android device or WebView version.

## What this app intentionally does not do

- No Instagram private/internal API.
- No scraping or automated account activity.
- No credential inspection or password storage.
- No custom Instagram login screen.
- No backend, proxy, or analytics.
- No unnecessary camera, microphone, contacts, storage, or location permissions.

## Disclaimer

DMs Only is an independent personal project and is not affiliated with, endorsed by, or sponsored by Instagram or Meta. Instagram and related marks belong to their respective owners.
