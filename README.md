# ScreenWriter

An offline, subscription-free screenwriting app for Android phones, tablets,
and Chromebooks. See [`ROADMAP.md`](ROADMAP.md) for the plan and progress, and
[`docs/research-plan.md`](docs/research-plan.md) for the full spec.

## Installing a development build

Every push builds a debug APK named `ScreenWriter-dev-<commit>.apk`. It
installs as **ScreenWriter Dev**, separately from any future Play Store version.
Each build installs over the previous one, and your data is kept.

1. On your phone, open the repository's **Actions** tab on GitHub and choose
   the latest green run for the branch.
2. Under **Artifacts**, tap the `.apk` file to download it.
3. Open the download. The first time, Android asks you to allow your browser
   to install apps. Allow it, then tap **Install**.

## Building

Requires JDK 17 or newer and the Android SDK.

```sh
scripts/install-android-sdk.sh   # only if you have no Android SDK
./gradlew check assembleDebug    # tests, lint, license check, debug APK
```

`core-*` modules are plain Kotlin, so `./gradlew :core-model:test` runs
without a device.
