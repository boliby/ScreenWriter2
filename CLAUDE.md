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
- **Target SDK:** `targetSdk 36`.
- **Device testing:** a change that affects keyboard input isn't done until the
  user has tested it on real keyboards. Say so in the PR or summary.

## Environment

Cloud sessions have Java 21 and Gradle but no Android SDK, because
`dl.google.com` is blocked. Build and test `core-*` modules here. The `:app`
module and APKs are built in GitHub Actions.
