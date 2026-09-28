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

**Build loop:** each step goes on its own branch. Claude builds and tests it,
then pushes it, and GitHub Actions builds a debug APK on every push. Claude
can also send you the APK directly in chat. You install it on your Pixel, try
it, and report back. See the README for install steps. You don't need Android
Studio.

**Test hardware:** a Pixel (Gboard, plus SwiftKey from Play), a Bluetooth
keyboard, and a Chromebook. There's no Samsung device, so Samsung Keyboard
coverage comes from beta testers (Phase 4). The cloud environment has no
emulator, so every on-screen and keyboard check happens on your devices.

---

## Phase 0: Setup

- [x] **You:** choose the app name and permanent application ID: ScreenWriter,
      `com.boliby.screenwriter`. Debug builds use `com.boliby.screenwriter.debug`
      so they can sit next to the Play version.
- [x] **You:** list your test hardware: a Pixel, a Chromebook, and a Bluetooth
      keyboard.
- [x] **Claude:** set up a Gradle project (§6) with `:app` and `:core-model`,
      a version catalog, `compileSdk 37`, `targetSdk 37`, `minSdk 26` (Android
      8+), and a placeholder Compose screen. Compose 1.12 requires compileSdk 37.
      Play requires targetSdk 36 or higher.
- [x] **Claude:** add CI that runs tests, lint, and a dependency license check
      that allows only Apache, MIT, BSD, and MPL, and builds a debug APK on
      every push. The debug signing key is committed so each build installs
      over the previous one.
- [x] **You:** install the placeholder APK on your Pixel. The first time,
      Android asks you to allow installs from the app you opened it with.

**Done when:** CI is green and the placeholder opens on your Pixel.

## Phase 1: Risk spikes (go/no-go)

These three spikes answer "can this be built well?" before anything else is
built on top of them. 1B and 1C are pure Kotlin, so Claude works on them while
you test 1A.

### 1A. Block editor on real keyboards (§2.8, §5.2)
- [x] **Claude:** build a throwaway editor. It shows a `LazyColumn` of
      `BasicTextField(TextFieldState)`. Enter splits a block through
      `InputTransformation`, Backspace merges blocks, headings display in
      uppercase through `OutputTransformation`, and it handles Tab and
      Enter from a hardware keyboard.
- [x] **Claude:** write the IME test script
      ([`docs/testing/keyboard-test.md`](docs/testing/keyboard-test.md)): tap-correct mid-word, accept a
      suggestion, swipe typing, Enter mid-word, Backspace-merge, voice input,
      paste, and hardware keys. The Enter/Tab/Backspace rules (§2.4) live in
      `core-format` with tests, ready for Phase 2.1.
- [ ] **You:** type about 5 pages with each keyboard on your Pixel, following
      the script: Gboard, SwiftKey, and the Bluetooth keyboard. *First pass:
      a scene using every element, with no problems.*

**Done when:** no text is lost or duplicated on any keyboard. **Go:** keep the
block editor. **No-go:** switch to the single-field Fountain editor (§5.3). The
core, paginator, and exporters stay the same either way.

### 1B. Pagination against a reference PDF (§3.1–3.3)
- [x] **Claude:** fetch the fountain.io samples (*Big Fish*, *Brick & Steel*,
      *The Last Birthday Card*) with `scripts/fetch-samples.sh`. They're
      copyrighted and this repository is public, so they're checksummed but
      not committed.
- [x] **Claude:** write the Fountain parser (`core-fountain`), a
      character-grid line wrapper and paginator (`core-layout`), and golden
      tests (`golden-tests`) that compare page starts with Final Draft's PDFs.
- [ ] **You (optional):** with a Final Draft trial or the Fade In demo, export
      a couple more reference PDFs, especially TV and dual dialogue.

**Done when:** *Big Fish* paginates within ±2 pages of the reference PDF.
**Result:** done, and past the v1.0 target. Laid out from their `.fdx` files
with each script's own formatting:

| Script | Pages vs Final Draft | Page starts on the same page |
|---|---|---|
| *Big Fish* | 120 vs 120 | 116 of 120 (97%) |
| *Brick & Steel* | 4 vs 4 | 4 of 4 |
| *The Last Birthday Card* | 20 vs 20 | 20 of 20 |

The four *Big Fish* misses are one song set in a 10-point proportional font;
the layout grid assumes Courier. What the calibration found: a 6" column holds
61 characters, a speech split across pages puts (MORE) and the (CONT'D) cue
in the margins, a split falls between sentences and re-wraps each half,
a parenthetical may end a page above (MORE), and a scene heading keeps all of
a paragraph that can't split.

### 1C. FDX round-trip (§4.2)
- [x] **Claude:** write an FDX reader and writer (`core-fdx`) that keep
      unknown XML intact. All three samples round-trip unchanged, edited
      paragraphs keep their other attributes, and a Fountain script converts
      to FDX and back. Imported files also keep their page formatting
      (`FdxTemplate`).
- [ ] **You:** open `fdx-check.fdx`, a short test script using every element,
      in Final Draft, Fade In, or a web app that imports FDX, such as
      WriterDuet or Arc Studio. Check that every element has the right type
      and styles.

**Done when:** the exported `.fdx` opens cleanly.

**Checkpoint:** record the block-editor or single-field decision in `CLAUDE.md`.

**Optional now, required by Phase 4:** register the Play Console account
($25 and a government ID). Identity checks can take days. Once it's set up,
Play's internal testing track is the easiest way to get builds onto your
Chromebook, which can't just open an APK file the way a phone can.

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

- [ ] **You:** register a Play Console account ($25 and a government ID), if
      you didn't during Phase 1.
- [ ] **You:** recruit 15–20 testers. Play requires 12 opted-in testers for 14
      continuous days. Include several people with Samsung Galaxy phones: they
      supply the Samsung Keyboard coverage you can't test yourself. Claude gives
      them the IME test script.
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
