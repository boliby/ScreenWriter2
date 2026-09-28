# ScreenWriter2

An offline, subscription-free Android screenwriting app built with Kotlin and
Jetpack Compose.

- Spec and research: `docs/research-plan.md`. Section numbers (§) in issues and
  commits refer to it.
- Plan and progress: `ROADMAP.md`. Tick checkboxes as steps finish.

## Rules

- **Editor design (decided in Phase 1):** one text field per screenplay
  element, in a lazy list. It passed the keyboard tests on Gboard, SwiftKey,
  and a Bluetooth keyboard, so the single-field fallback (§5.3) isn't needed.
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

## Modules

- `core-model`: script, blocks, spans (§2.9)
- `core-format`: Enter/Tab/Backspace rules (§2.4)
- `core-fountain`: Fountain parser (§4.1)
- `core-layout`: line wrapping, page templates, pagination (§3)
- `core-fdx`: Final Draft reader, writer, and page template (§4.2)
- `golden-tests`: page breaks compared with Final Draft PDFs (§9)
- `testing`: test helpers; `app`: the Android app
- `build-logic`: shared Gradle conventions, including the license allowlist

## Building

Cloud sessions have Java 21 but no Android SDK and no emulator.

```sh
scripts/install-android-sdk.sh   # once per session; writes local.properties
scripts/fetch-samples.sh         # fountain.io samples for the golden tests
./gradlew check assembleDebug    # tests, lint, license check, debug APK
```

The samples are copyrighted and the repository is public, so they stay out of
git (`samples/` is ignored) and are checked against `scripts/samples.sha256`.
Tests that need them pass quietly without them, except in CI, which sets
`REQUIRE_SAMPLES`. Golden-test reports, with both layouts row by row, are in
`golden-tests/build/reports/pagination/`.

`app` has Robolectric smoke tests (`AppSmokeTest`) that launch the real app on
simulated Android and drive the editor. They catch crashes and broken wiring
that the JVM-only core tests can't, so extend them whenever the app changes.
They can't judge keyboard behavior; that still needs the user's devices.
Watch for Android-only rules the core can't see, such as `LazyColumn` keys
having to be Bundle-saveable (a raw `Long`, never `BlockId`).

The APK lands in `app/build/outputs/apk/debug/`. Maven Central sometimes
returns HTTP 429. Retry the build if that happens. CI
(`.github/workflows/ci.yml`) runs the same command on every push.
