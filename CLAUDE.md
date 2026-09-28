# ScreenWriter2

An offline, subscription-free Android screenwriting app built with Kotlin and
Jetpack Compose.

- Spec and research: `docs/research-plan.md`. Section numbers (§) in issues and
  commits refer to it.
- Plan and progress: `ROADMAP.md`. Tick checkboxes as steps finish.

## Rules

- **Licenses:** no GPL, AGPL, or LGPL dependencies or code. That rules out
  Beat, Trelby, STARC, iText, and Sora Editor. Don't reproduce code from those
  projects from memory. Apache, MIT, BSD, MPL, and OFL are fine.
- **Pure core:** `core-*` modules are pure Kotlin with no `android.*` imports,
  so every rule is unit-tested on the JVM.
- **Text fields:** the editor uses Compose state-based text fields
  (`TextFieldState`, `InputTransformation`, `OutputTransformation`). Don't use
  the older `value`/`onValueChange` API, and never use a WebView editor.
- **IME safety:** never change stored text during IME composition. Uppercase
  and emphasis are display-only, rendered with `OutputTransformation`. Catch
  `"\n"` in `InputTransformation` and split the block in the model.
- **Commands:** every edit is a `Command`, and undo applies inverse commands.
- **Saves:** write a temp file, fsync it, then rename it atomically. Keep a
  `.bak` copy. Never write partially over a user's file.
- **Fountain:** the writer always forces elements whose type could be inferred
  wrongly (`.`, `@`, `!`, `>`), so saved files re-parse identically.
- **FDX:** keep unknown attributes and nodes, and write them back unchanged.
- **SDK levels:** `compileSdk 37`, `targetSdk 37`, `minSdk 26`. Play requires
  targetSdk 36 or higher.
- **Device testing:** a change that affects keyboard input isn't done until the
  user has tested it on real keyboards. Say so in the PR or summary.

## Working with the user

The user tests and decides but doesn't read code. Report in plain language:
what changed, what to try on the device, and what a pass looks like. After
building an APK, send it with SendUserFile so the user can install it without
going through GitHub.

Test hardware: a Pixel (Gboard, SwiftKey), a Bluetooth keyboard, and a
Chromebook. There's no Samsung device.

## Building

Cloud sessions have Java 21 but no Android SDK and no emulator.

```sh
scripts/install-android-sdk.sh   # once per session; writes local.properties
./gradlew check assembleDebug    # tests, lint, license check, debug APK
```

The APK lands in `app/build/outputs/apk/debug/`. Maven Central sometimes
returns HTTP 429. Retry the build if that happens. CI
(`.github/workflows/ci.yml`) runs the same command on every push.
