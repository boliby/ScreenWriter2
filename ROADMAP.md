# ScreenWriter2 Roadmap

An offline, one-time-purchase Android screenwriting app with Final Draft-style
Enter/Tab flow, autocomplete, and accurate PDF output. The full research and
spec are in [`docs/research-plan.md`](docs/research-plan.md). Section numbers
below (§) point into that document.

This roadmap updates the research plan's schedule for one change: Claude writes
most of the code. That removes most of the 3-month "learn Kotlin first" phase.
It doesn't remove the work only you can do: testing on real keyboards and
devices, supplying reference PDFs, making product decisions, and handling the
Play Store account.

## How we work

| Claude | You |
|---|---|
| Writes the code and unit tests, and keeps CI green | Decides names, pricing, and scope trade-offs |
| Keeps `core-*` pure Kotlin so it can be tested without a phone | Installs builds on real devices and types on Gboard, Samsung Keyboard, SwiftKey, and a hardware keyboard |
| Writes a test checklist for each device test | Reports caret jumps, lost or duplicated text, or anything that feels wrong |
| Drafts the privacy policy, store listing, and billing code | Owns the Play Console account, testers, payments, and taxes |

**Build loop:** each step goes on its own branch. Claude pushes it, and GitHub
Actions runs the tests and builds a debug APK. You download the APK from the
Actions run, install it on your phone, try it, and report back. You don't need
Android Studio for this loop. It's still useful for emulators and faster local
builds.

> This cloud environment can't reach `dl.google.com` (the Android SDK) or
> `fountain.io` (sample scripts). Claude can build and test the pure-Kotlin core
> here, but APKs are built in CI. To let Claude build APKs in-session, add both
> hosts to the environment's allowed domains.

---

## Phase 0: Setup

- [ ] **You:** choose a working app name and a permanent application ID (for
      example `com.yourname.screenwriter`). The ID can't change once the app is
      on Play.
- [ ] **You:** on your phone, enable Developer options and allow installing
      apps from your browser or file manager.
- [ ] **You:** list your test hardware. Ideally that's a Samsung phone plus a
      tablet or Chromebook with a Bluetooth keyboard, with Gboard and SwiftKey
      installed.
- [ ] **Claude:** set up a Gradle multi-module project (§6) with a version
      catalog, `targetSdk 36`, `minSdk 26`, and an empty Compose app.
- [ ] **Claude:** add CI that runs the `core` tests, builds a debug APK as a
      downloadable artifact, and runs a dependency license check that fails on
      GPL, AGPL, or LGPL.

**Done when:** CI is green and you've installed the placeholder APK from a CI
run on your phone.

## Phase 1: Risk spikes (go/no-go)

These three spikes answer "can this be built well?" before anything else is
built on top of them. 1B and 1C are pure Kotlin, so Claude works on them while
you test 1A.

### 1A. Block editor on real keyboards (§2.8, §5.2)
- [ ] **Claude:** build a throwaway editor. It shows a `LazyColumn` of
      `BasicTextField(TextFieldState)`. Enter splits a block through
      `InputTransformation`, Backspace merges blocks, headings display in
      uppercase through `OutputTransformation`, and it handles Tab and
      Enter from a hardware keyboard.
- [ ] **Claude:** write the IME test script: tap-correct mid-word, accept a
      suggestion, swipe typing, Enter mid-word, Backspace-merge, voice input,
      paste, and hardware keys.
- [ ] **You:** type about 5 pages with each keyboard, following the script.

**Done when:** no text is lost or duplicated on any keyboard. **Go:** keep the
block editor. **No-go:** switch to the single-field Fountain editor (§5.3). The
core, paginator, and exporters stay the same either way.

### 1B. Pagination against a reference PDF (§3.1–3.3)
- [ ] **You:** get the *Big Fish* `.fountain` and `.pdf` samples from
      fountain.io and commit them to `samples/`, or let Claude fetch them.
      If you can get a Final Draft trial or the Fade In demo, export a
      couple more reference PDFs.
- [ ] **Claude:** write a minimal Fountain parser, a character-grid line
      wrapper, and a paginator, plus a golden test that compares page-start
      text with the reference PDF.

**Done when:** *Big Fish* paginates within ±2 pages of the reference PDF.

### 1C. FDX round-trip (§4.2)
- [ ] **Claude:** write an FDX reader and writer that keep unknown XML intact,
      with round-trip tests on *Brick & Steel*.
- [ ] **You:** open the exported `.fdx` in the Fade In demo or a Final Draft
      trial.

**Done when:** the exported `.fdx` opens cleanly.

**Checkpoint:** record the block-editor or single-field decision in `CLAUDE.md`.

## Phase 2: MVP (good enough for your own writing)

- [ ] **2.1 Core engine (§2.4, §2.9):** script model, `Command` processor,
      element rules table, and undo/redo with typing coalescing. Tests cover
      every element × {Enter, Tab} × {empty, non-empty}.
- [ ] **2.2 Editor UI (§2.7):** element bar above the soft keyboard, block
      swipe to change element, hardware shortcuts (Tab, Shift+Tab,
      Ctrl+1–8, Ctrl+Z/Shift+Z), and per-block caps and autocorrect settings.
- [ ] **2.3 Smart formatting (§2.5, §2.6):** uppercase rendering,
      auto-parentheses, character and location lists with ranking, ghost-text
      suggestions, heading parts (INT. → location → time), CONT'D, and smart
      detect.
- [ ] **2.4 Files and safety (§4.1, §4.4, §6):** full Fountain parser and a
      writer that forces ambiguous elements, with a property test that
      `parse(write(s)) == s`. Opening and creating files uses the Storage
      Access Framework (SAF) with persisted permissions. Saves are atomic and
      keep a `.bak` copy. Autosave runs every 2 s, snapshots are kept, and a
      library screen is backed by Room.
- [ ] **2.5 PDF export (§4.3):** PdfBox-Android behind a renderer interface,
      embedded Courier Prime, title page, page numbers, (MORE)/(CONT'D), and
      scene bookmarks. Golden tests cover 3 scripts.
- [ ] **2.6 Polish:** dark mode and a settings screen.

**Done when:** you write a 10-page short on your phone and on a tablet or
Chromebook without fighting the formatting, and the PDF passes the golden tests.
**From here on, write real scripts in it.** That's the best source of bug
reports.

## Phase 3: v1.0

These are ordered by value. If time runs short, cut from the bottom of the cut
list.

- [ ] FDX import and export (labeled "beta")
- [ ] Title page editor
- [ ] Scene navigator with drag-to-reorder and color tags
- [ ] Find and replace (regex, case, and scope options, applied as one undo step)
- [ ] Dual dialogue
- [ ] Scene numbers
- [ ] Page view
- [ ] Reports (characters, scenes, locations)
- [ ] Focus mode
- [ ] Spell check in Action and Dialogue
- [ ] Sprints and stats
- [ ] TV single-camera template
- [ ] A4 paper
- [ ] Accessibility pass
- [ ] Performance: 120 pages at 60 fps on a mid-range phone, measured with Macrobenchmark
- [ ] Data-safety tests: kill the app mid-save, low storage, and revoked SAF permission

**Cut order:** index cards → sprints → docx → reports → dual dialogue → A4.
**Never cut:** Enter/Tab, autocomplete, pagination accuracy, Fountain/PDF, or
autosave.

**Done when:** pagination is within ±1 page of Final Draft on 5 scripts, 120
pages scroll smoothly, and a 2-week beta has no data loss.

## Phase 4: Release (§10)

Start this during Phase 3. The closed test alone takes at least 14 days.

- [ ] **You:** register a Play Console account ($25 and a government ID).
      Register early, because identity verification can take time.
- [ ] **You:** recruit 15–20 testers. Play requires 12 opted-in testers for 14
      continuous days.
- [ ] **Claude:** host the privacy policy on GitHub Pages, draft the data
      safety answers and store listing, add Play Billing for the one-time
      unlock, and set up release signing and AAB builds in CI.
- [ ] **You:** set up the payments profile and taxes, review the screenshots,
      and submit.

## Phase 5: v1.x and v2 (§7)

- **v1.x:** index cards, notes panel, multi-camera template, `.highland` and
  `.docx`, themes, custom keymaps.
- **v2:** revisions, locked pages and A-pages, locked scene numbers, OMITTED
  scenes, and a desktop app built with Compose Multiplatform from the same core.
