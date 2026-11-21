# Rizumu

Native Android music player (Kotlin + Jetpack Compose) that streams music sourced from
YouTube / YouTube Music. No WebView, no backend server.

> Personal use. Downloading or streaming YouTube content outside the official player may
> violate YouTube's Terms of Service.

## Status

R3 is implemented: songs can be downloaded for offline playback through WorkManager,
the player supports 0.75x–1.5x speed, and user data can be exported/imported as JSON via
the system file picker. Crossfade stays hidden until a stable dual-player implementation
is available. v0.11.1 unifies every bottom sheet behind a shared dark glass sheet, hides
the dock while a sheet is open, and reworks Equalizer, Timer tidur, Info, and Antrian.
v0.11.2 fixes the Info sheet crash and removes the pink swipe background in Antrian.
v0.12.0 adds swipeable Home/Cari/Library pages, dock auto-return timing, and refreshes
trending whenever the app opens. v0.12.1 fixes the dock auto-return timer and the Home
Info sheet inside the pager; v0.12.2 also auto-returns after a manual dock drag to nav;
v0.12.3 points the update checker at the public repository. v0.13.0 refreshes the
launcher icon, removes search filter chips in favor of mixed results, neutralizes brand
words in the UI, and adds playback retry/recovery.
The package id and signing key intentionally remain unchanged so existing installs keep
their Disukai, Riwayat, Playlist, and download data.

- R1: Rizumu identity, launcher icon, update checker, Equalizer empty state — done
- R2: playlist mosaic/reorder, sleep timer, MediaStyle/lock screen — done
- R3: offline downloads, playback speed, JSON backup/restore — done
- R4: listening stats, gestures, full lyrics, Glance widgets — next

## Stack

- Kotlin 2.0.21, Jetpack Compose (Material 3), AGP 8.5.2, Gradle 8.7
- minSdk 26, compileSdk/targetSdk 34, Java 17 + core library desugaring
- Media3/ExoPlayer 1.4.1, OkHttp 4.12.0, Coil 2.7.0, Navigation Compose 2.8.2, NewPipeExtractor 0.27.1
- Room (playlists, lyrics, downloads), WorkManager, DataStore (update preferences)

## Build

Requires JDK 17 and Android SDK 34. Build a signed release with:

```bash
./gradlew assembleRelease
# output: app/build/outputs/apk/release/app-release.apk
```

Release signing uses a local, gitignored `keystore.properties`. Install the published
APK from the [latest GitHub release](https://github.com/fatkhur20/Rizumu/releases/latest).

## Update source

The update checker reads the latest GitHub release from:

```text
https://api.github.com/repos/<owner>/<repo>/releases/latest
```

Set the repository once in `gradle.properties` (or `~/.gradle/gradle.properties`):

```properties
RIZUMU_GITHUB_OWNER=your-github-user
RIZUMU_GITHUB_REPO=Rizumu
```

Defaults are `fatkhur20/Rizumu`; change them before publishing so devices do not check the
wrong repository. Builds up to 0.12.2 still check `kyrix/Rizumu`, so install 0.12.3 manually
once before relying on in-app updates. Release assets must end in `.apk`. If the release notes contain a
`SHA-256:` line, the downloaded APK is verified before the installer opens.

## Offline and backup

Downloads are stored in app-private storage and played before the network source. The
download queue survives restarts and uses a connected-network constraint. Backup JSON
contains favorites, history, searches, and playlists; it intentionally excludes audio
files and download records. Import merges data instead of wiping the current library.

## Signing

`keystore.properties` and `*.jks` are gitignored. The release keystore must be kept safe;
without it, future updates cannot be installed over the current app. Do not change the
application id (`com.kyrix.ftxmusic`) or the signing key after release.
