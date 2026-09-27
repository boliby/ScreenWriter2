# Building an Offline, Subscription-Free Android Screenwriting App: A Full Development Plan (2026)

**Bottom line: this is buildable solo, but only if you treat the editor as the whole project. Use native Kotlin with Jetpack Compose. Model the script as a list of typed elements, not free text. Keep the parser, formatter, paginator and exporters in pure Kotlin so a desktop port is possible later. Plan on roughly 12–18 months of part-time work to reach a v1.0 that matches paid apps for typing and output quality.** The opening in the market is real. Fade In Mobile was removed from Google Play on October 3, 2025, according to AppBrain. Highland Pro and Final Draft have no Android apps. The Android apps that do exist are subscriptions or cloud-first.

## TL;DR
- **The gap is real.** Fade In Mobile was removed from Google Play on October 3, 2025, according to AppBrain. Final Draft and Highland Pro don't ship on Android. JotterPad ($29.99/yr), WriterDuet, Arc Studio ($69/yr) and Celtx are subscriptions. The open-source apps (Beat, Trelby, STARC) are GPL, so you can't reuse their code in a closed paid app. A polished, offline, one-time-purchase Android app with Final Draft-style Enter/Tab flow and SmartType-style autocomplete has no direct competitor.
- **Stack: Kotlin + Jetpack Compose, with a block-per-element editor.** Each screenplay element is its own text field in a lazily rendered list. A pure-Kotlin core holds the model, formatting state machine, Fountain/FDX I/O and paginator. PDF goes through PdfBox-Android (Apache 2.0). Avoid WebView editors: CodeMirror has a long record of Gboard composition bugs. Avoid GPL/AGPL code (Beat, Trelby, STARC, iText).
- **Sequence: learn, then spike, then MVP, then v1.0.** Learn Kotlin/Compose (about 3 months). Spend 1–2 months on prototype spikes for the three riskiest pieces: IME behavior, pagination versus Final Draft, and FDX round-trip. Then build an MVP (4–6 months) and v1.0 (4–6 months). To sell it, budget $25 for Play registration plus a 12-tester, 14-day closed test, and target API 36.

---

## Key Findings

1. **No paid leader has a real Android editor, and the market is moving to subscriptions.**
   - Final Draft 13 is US$199.99 one-time, discounted from US$249.99, according to finaldraft.com/pricing. The same page notes the license "Does not include Final Draft Cloud." Cloud and mobile are separate subscriptions.
   - Highland Pro launched in March 2025 at $59.99/yr or $9.99/mo and runs only on Apple platforms. Quote-Unquote Apps says Highland 2, previously a $49.99 one-time unlock, "is no longer available for sale."
   - Fade In Professional is still $79.95 one-time. Its Android app, last updated in March 2022, was removed from Google Play on October 3, 2025. [appbrain](https://www.appbrain.com/app/fade-in-mobile/com.generalcoffee.fadeinmobile)
2. **Top-tier smart formatting is well documented.** Final Draft publishes its Enter/Tab transition tables, SmartType lists and "Automatically Guess Next Character" option. [finaldraft](https://kb.finaldraft.com/hc/en-us/articles/27977488282644-What-keyboard-shortcuts-can-I-use-in-Final-Draft) [finaldraft](https://kb.finaldraft.com/hc/en-us/articles/27750003388948-What-is-SmartType-and-how-do-I-use-it) Fountain's spec defines plain-text inference exactly. [fountain](https://fountain.io/syntax/) Together they give a complete, testable rule set.
3. **The editor surface is the main technical risk.** WebView editors have recurring, sometimes unresolved Gboard bugs. [github](https://github.com/openchamber/openchamber/issues/3514) [github](https://github.com/codemirror/dev/issues/1145) Compose's state-based text fields give native IME handling. They include OutputTransformation, which has supported styling since Compose 1.9 (August 2025). [googleblog](https://android-developers.googleblog.com/2025/08/whats-new-in-jetpack-compose-august-25-release.html)
4. **Licensing and 2026 Play policy constrain choices.**
   - GPL/AGPL code (Beat, Trelby, STARC, iText) is out for a closed app. PdfBox-Android (Apache 2.0) and Courier Prime (OFL 1.1) are fine. [github +4](https://github.com/TomRoush/PdfBox-Android)
   - Play Console Help: "Starting August 31, 2026: New apps and app updates must target Android 16 (API level 36) or higher."
   - Play Console Help: personal accounts created after November 13, 2023 "must run a closed test for their app with a minimum of 12 testers who have been opted in continuously for at least 14 days."
   - The Android Developers Blog says developer verification protections "take effect on September 30, 2026, starting with users in Brazil, Indonesia, Singapore, and Thailand," with a global rollout in 2027.

---

## 1. Competitive Landscape and Feature Matrix

### 1.1 Paid leaders

| App | Pricing (2026) | Paywalled | Standout QoL | Complaints |
|---|---|---|---|---|
| **Final Draft 13** | $199.99 one-time, 2 computers. Upgrade $79.99 on sale. Suite $99.99/yr or $16.99/mo [finaldraft](https://www.finaldraft.com/products/final-draft-13) [finaldraft](https://www.finaldraft.com/pricing/) | Cloud and mobile separate. Paid major upgrades | Enter/Tab flow, SmartType, Beat Board, revisions, locked pages, reports | Price. Upgrade treadmill. FD10 activation ended June 30, 2025. [consumerrights](https://consumerrights.wiki/w/Final_Draft_software_activation) No Android |
| **Highland Pro** | $59.99/yr or $9.99/mo [quoteunquoteapps](https://quoteunquoteapps.com/highland-pro/) | Everything after 30-day trial | Plain-text Fountain, sprints, themes, distraction-free | Subscription switch. Apple-only [Substack](https://donnybroussard621349.substack.com/p/the-best-screenwriting-software) |
| **WriterDuet** | Free (3 scripts). Plus $7.99/mo. Pro $11.99/mo or $89/yr. Premium $15.99/mo [squibler](https://www.squibler.io/learn/software/writer-duet-review/) | Unlimited scripts, history, offline apps (Pro), reports | Collaboration, autocomplete, Android app | Best features in top tiers. Cloud-centric [studiobinder](https://www.studiobinder.com/blog/screenwriting-software/) |
| **Fade In** | $79.95 one-time, free updates [fadeinpro](https://www.fadeinpro.com/page.pl?content=purchase) | Nothing on desktop | Production features, Linux, cheap | Android app abandoned [google](https://play.google.com/store/apps/details?id=com.generalcoffee.fadeinmobile&hl=en_GB&gl=US) |
| **Arc Studio** | Free (2 scripts, watermark). Essentials $69/yr. Pro $99 first year, then $79 [screenweaver](https://www.screenweaver.ai/blog/arc-studio-review-2026) | Unlimited scripts, no watermark, outlining | Clean UI, board beside script | Annual-only. Cloud-first |
| **Celtx** | Free (1 project). Writer $14.99/mo. Writer Pro $24.99/mo [screenweaver](https://www.screenweaver.ai/blog/celtx-review-2026) | Drafts, headers/footers, collaboration | Production planning | Aggressive tiers |
| Movie Magic, Slugline, KIT Scenarist | Not verified here | — | — | Secondary benchmarks |

### 1.2 On Android today

- **JotterPad:** Fountain WYSIWYG with .pdf/.fdx/.docx export and typewriter scrolling. 5.4M downloads, rated 4.1. The Pro Cloud subscription is US$6.99/mo or US$29.99/yr. [appbrain +2](https://www.appbrain.com/app/jotterpad-writer-screenplay/com.jotterpad.x) It's a general writing app, and some reviews call the UI dated. [google](https://play.google.com/store/apps/details?id=com.jotterpad.x&hl=en_US)
- **WriterDuet:** an official Android app, [writerduet](https://www.writerduet.com/article/355-steps-to-update-writerduet-on-your-device) but offline use requires Pro. [prowritingaid](https://prowritingaid.com/best-screenwriting-software)
- **STARC:** GPL-3.0. Pro costs $4.17/mo billed annually or $150 lifetime. The Android app is at v0.1.7 (Dec 2025) and rated 3.5/5. [starc +2](https://starc.app/pricing/)
- **Fade In Mobile:** removed from Google Play. [appbrain](https://www.appbrain.com/app/fade-in-mobile/com.generalcoffee.fadeinmobile) Its last reviews called it "Abandonware". [google](https://play.google.com/store/apps/details?id=com.generalcoffee.fadeinmobile&hl=en_GB&gl=US)

### 1.3 Free/open source

Your only reuse candidate is Afterwriting (MIT, web; a good reference for Fountain-to-PDF and stats). [medevel](https://medevel.com/18-open-source-tools-for-writers/) The others you can only study:
- Beat (GPL v3, Mac/iOS, v2.2.7 August 2026) [screenweaver](https://www.screenweaver.ai/blog/free-screenwriting-software-comparison-2026)
- Trelby (GPL-2.0; imports and exports FDX, Celtx, Fountain, Adobe Story and Fade In) [alternativeto +2](https://alternativeto.net/software/writerduet)
- STARC (GPL-3.0) [alternativeto](https://www.alternativeto.net/software/story-architect-starc-/about/)

### 1.4 Feature matrix

Legend: ● full, ◐ partial or paywalled, ○ absent.

| Feature | Final Draft | Highland Pro | WriterDuet | Fade In | Arc Studio | JotterPad | **Your v1.0** |
|---|---|---|---|---|---|---|---|
| Enter/Tab element flow | ● | ○ | ● | ● | ● | ○ | **●** |
| Fountain inference | ◐ | ● | ◐ | ◐ | ◐ | ● | **● (import/paste)** |
| Autocomplete | ● | ◐ | ● | ● | ● | ◐ | **●** |
| Auto (CONT'D)/(MORE) | ● | ● | ● | ● | ● | ◐ | **●** |
| Accurate PDF | ● | ● | ● | ● | ◐ | ◐ | **●** |
| FDX import/export | ● | ● | ● | ● | ● | ● | **●** |
| Scene navigator | ● | ● | ● | ● | ● | ◐ | **●** |
| Index cards | ● | ◐ | ● | ● | ● | ○ | v1.x |
| Reports | ● | ◐ | ◐ | ● | ◐ | ◐ | **●** |
| Dual dialogue | ● | ● | ● | ● | ● | ● | **●** |
| Revisions/locked pages | ● | ◐ | ◐ | ● | ◐ | ○ | v2 |
| Sprints/stats | ◐ | ● | ◐ | ○ | ◐ | ◐ | **●** |
| Android hardware-keyboard shortcuts | n/a | n/a | ◐ | n/a | ◐ | ◐ | **●** |
| Fully offline, no account | ● | ● | ◐ | ● | ○ | ◐ | **●** |
| Price model | One-time + upgrades | Sub | Sub | One-time | Sub | Sub | **One-time** |

**Table stakes:** formatting and pagination, Enter/Tab, autocomplete, (MORE)/(CONT'D), PDF/FDX/Fountain, title page, navigator, find/replace, dark mode, autosave.

**Differentiators you can own:**
- Android-native editing on phones and Chromebooks
- No account and fully offline
- One-time price
- QoL features others paywall: reports, sprints, snapshots, unlimited scripts, no watermark

---

## 2. Predictive Auto-Formatting

### 2.1 Final Draft (reference behavior)

**Enter** creates the next paragraph:
- Scene Heading → Action
- Action → Action
- Character → Dialogue
- Dialogue → Action
- Parenthetical → Dialogue
- Transition → Scene Heading
- Shot → Action
- New Act → Scene Heading
- End of Act → New Act

Users can remap these (Format > Elements). A common change is Dialogue → Character. [finaldraft](https://kb.finaldraft.com/hc/en-us/articles/27977488282644-What-keyboard-shortcuts-can-I-use-in-Final-Draft)

**Tab:**
- Scene Heading → Action
- Action → Character
- Character → Transition (only when the character line is blank)
- Transition → Scene Heading
- Dialogue → Parenthetical
- Parenthetical → Dialogue

Tab also accepts the current SmartType selection. In a scene heading, Tab after INT/EXT adds ". " and moves to the location, then adds " - " and moves to time of day. [finaldraft](https://kb.finaldraft.com/hc/en-us/articles/27977488282644-What-keyboard-shortcuts-can-I-use-in-Final-Draft) [belalampert](https://www.belalampert.com/2021/05/final-draft-12-screenplay-formatting-elements/) By default, Tab after a character creates a parenthetical (an option makes it a character extension). [finaldraft](https://kb.finaldraft.com/hc/en-us/articles/27750003388948-What-is-SmartType-and-how-do-I-use-it) Enter on a blank line opens the Elements menu. [finaldraft](https://www.finaldraft.com/blog/how-to-use-final-draft-script-elements)

**SmartType** keeps lists of characters, extensions, scene intros, locations, times and transitions, and narrows as you type. Entries persist after they're removed from the pages. "Automatically Guess Next Character" alternates two speakers in a conversation. [finaldraft](https://kb.finaldraft.com/hc/en-us/articles/27750003388948-What-is-SmartType-and-how-do-I-use-it) Typing "IN" or "EX" prompts INT./EXT. [finaldraft](https://www.finaldraft.com/learn/screenplay-formatting-elements/) Parentheticals get both parentheses automatically with the cursor between them. [finaldraft](https://kb.finaldraft.com/hc/en-us/articles/27646947570196-What-are-script-elements)

### 2.2 Highland/Fountain inference

- **Scene heading:** starts with INT, EXT, EST, INT./EXT, INT/EXT or I/E (case-insensitive), then a dot or space, with blank lines around it. A single leading "." forces a heading; "..." doesn't. Scene numbers are written `#1A#`. [fountain](https://fountain.io/syntax/)
- **Character:** an all-uppercase line with a blank line before and none after. It needs at least one letter ("R2D2" works, "23" doesn't). "@" forces a character (@McCLANE). Extensions can be any case. [fountain](https://fountain.io/syntax/)
- **Dialogue** follows a Character or Parenthetical. A **Parenthetical** is a line in parentheses. [fountain](https://fountain.io/syntax/)
- **Transition:** uppercase, blank lines around it, ends in `TO:`. ">" forces one. **Centered:** `>THE END<`. [fountain](https://fountain.io/syntax/)
- **Dual dialogue:** `^` after the second character. [fountain](https://fountain.io/syntax/)
- **Action** is the fallback. "!" forces it, which fixes all-caps lines like "SCANNING THE AISLES…" being misread as characters. [fountain](https://fountain.io/syntax/)

Highland makes writers learn this syntax. This plan uses structured elements plus Final Draft keys as the primary interaction, and keeps Fountain inference for paste, import and optional smart detection.

### 2.3 WriterDuet, Fade In, Arc Studio

All three follow the Final Draft paradigm: typed paragraphs, Enter/Tab transitions, and name/location autocomplete. Fade In Mobile advertised "tools for quickly selecting character and location names". [google](https://play.google.com/store/apps/details?id=com.generalcoffee.fadeinmobile&hl=en_GB&gl=US) Their exact per-key tables weren't verified here, so use Final Draft's documented tables as the baseline and allow remapping.

### 2.4 Element state machine (your spec)

Elements: `SCENE_HEADING, ACTION, CHARACTER, PARENTHETICAL, DIALOGUE, TRANSITION, SHOT, CENTERED, LYRIC, SECTION, SYNOPSIS, NOTE, PAGE_BREAK`. Dual dialogue is a flag on a CHARACTER block.

| Current | Enter (has text) | Enter (empty) | Tab (empty) | Tab (has text) |
|---|---|---|---|---|
| Scene Heading | → Action | → Action | → Action | Accept suggestion / next heading part |
| Action | → Action | → Character | → Character | no-op |
| Character | → Dialogue | → Action | → Transition | → Parenthetical (option: extension) |
| Parenthetical | → Dialogue | → Dialogue | → Dialogue | Skip ")" → Dialogue |
| Dialogue | → Action (option: Character) | → Action | → Parenthetical | → Parenthetical |
| Transition | → Scene Heading | → Scene Heading | → Scene Heading | Accept suggestion |

**Editing rules:**
- **Double-Enter** converts the empty block instead of adding another, so "Enter twice" returns to Action.
- **Shift+Tab** cycles backward.
- **Backspace in an empty block** deletes it.
- **Backspace at the start of a non-empty block** merges it with the previous block if the types match, and converts it otherwise.

**Hardware shortcuts:**
- Ctrl+1–8: Scene Heading, Action, Character, Parenthetical, Dialogue, Transition, Shot, Note. Final Draft documents Ctrl/Cmd+5 for Dialogue. [finaldraft](https://www.finaldraft.com/learn/screenplay-formatting-elements/)
- Ctrl+D: dual dialogue.
- Ctrl+F: find.
- Ctrl+Z / Ctrl+Shift+Z: undo/redo.

### 2.5 Autocomplete sources and ranking

| Context | Source | Ranking |
|---|---|---|
| Heading intro | INT., EXT., INT./EXT., I/E, EST. | Frequency |
| Location | Previously used locations | Prefix, then frequency, then recency |
| Time | DAY, NIGHT, CONTINUOUS, LATER, MOMENTS LATER, MORNING, EVENING, SAME | Frequency |
| Character | Cue names without extensions | Score below |
| Extension | V.O., O.S., O.C., CONT'D, PRE-LAP | Frequency |
| Transition | CUT TO:, SMASH CUT TO:, DISSOLVE TO:, MATCH CUT TO:, FADE OUT., FADE IN: | Frequency |

**Character score (a tunable heuristic):**
`score = 3·(spoke in current scene) + 2·(speaker two cues ago) + 1·log(1+cues) + recencyDecay`

The "two cues ago" term reproduces Final Draft's speaker alternation. When the leader is decisive, show it as ghost text: Tab or Enter accepts, typing overrides. Persist the lists in the document and offer "Clean up lists".

### 2.6 Automatic behaviors

- **Uppercase:** store raw text and render Scene Heading, Character and Transition in uppercase via `OutputTransformation`. Export in uppercase. Also set `KeyboardCapitalization.Characters` on those blocks. Never rewrite text mid-composition.
- **Auto-parentheses:** a new Parenthetical gets "()" with the caret inside. Typing "(" at the start of a Dialogue block converts it.
- **(CONT'D):** computed at render time when the same speaker resumes after only Action. It's a setting.
- **(MORE)/(CONT'D) at page breaks:** paginator only (3.3).
- **Dual dialogue:** a toggle on the second cue. It maps to Fountain `^` and FDX `DualDialogue`.
- **Scene numbers:** computed at export when enabled. Locking and A-numbers come in v2.
- **Smart detect:** in Action, "int." or "ext." plus a space converts the block to Scene Heading. An all-caps Action line prompts "Make character?" in the suggestion strip rather than converting silently.

### 2.7 Phones vs. hardware keyboards

**Phones:**
- An **element bar** sits above the keyboard (`imePadding()`). It holds the current element chip (tap for a menu), Tab, the top 3 suggestions, "()", "^" and undo/redo.
- Swipe a block right or left to cycle its element.
- The soft keyboard's Enter keeps its Enter meaning.

**Tablets/Chromebooks:**
- Intercept Tab, Shift+Tab, Enter and Ctrl-shortcuts with `Modifier.onPreviewKeyEvent`.
- Hide the bar when a physical keyboard is attached, and show a "Tab → Character / Enter → Action" hint.
- Test resizable windows. API 36 makes resizability the baseline on large screens. [ecorpit](https://ecorpit.com/android-target-api-36-play-store-deadline-migration-2026/)

### 2.8 Soft-keyboard pitfalls

| Pitfall | Evidence | Response |
|---|---|---|
| Gboard re-composes a tapped word, causing caret jumps and reinserted fragments | auto-mobile #7495; openchamber #3514; CodeMirror #1504 [github](https://github.com/kaeawc/auto-mobile/issues/7495) | Use platform text fields. Never mutate text during composition. Keep transforms visual |
| Enter mid-word with the suggestion strip on desyncs the editor | CodeMirror #1145 (Replit) [github](https://github.com/codemirror/dev/issues/1145) | Catch "\n" in the InputTransformation, revert it, and split the block in the model |
| Duplicated characters in WebView editors | CodeMirror #96, #1028 [github](https://github.com/codemirror/dev/issues/96) [github](https://github.com/codemirror/dev/issues/1028) | Avoid WebView editors |
| Autocorrect mangles names and sluglines | Common | Per-block `autoCorrectEnabled = false` for Character, Heading and Transition |
| Samsung predictive text gets stuck | CodeMirror #1504 [github](https://github.com/openchamber/openchamber/issues/3514) | Test on a real Galaxy. Commit composition (focus change) before block operations |

### 2.9 Engine design

```kotlin
enum class ElementType { SCENE_HEADING, ACTION, CHARACTER, PARENTHETICAL, DIALOGUE,
  TRANSITION, SHOT, CENTERED, LYRIC, SECTION, SYNOPSIS, NOTE, PAGE_BREAK }
data class Span(val start: Int, val end: Int, val style: Style)
data class Block(val id: BlockId, val type: ElementType, val text: String,
  val spans: List<Span> = emptyList(), val dual: Boolean = false,
  val sceneNumber: String? = null, val extras: Map<String, String> = emptyMap())
data class Script(val titlePage: TitlePage, val blocks: List<Block>,
  val settings: ScriptSettings, val smartLists: SmartLists)

data class Transition(val onEnter: ElementType, val onTab: ElementType,
  val onEnterEmpty: ElementType, val onTabEmpty: ElementType)
val DEFAULT_RULES = mapOf(
  SCENE_HEADING to Transition(ACTION, ACTION, ACTION, ACTION),
  ACTION        to Transition(ACTION, CHARACTER, CHARACTER, CHARACTER),
  CHARACTER     to Transition(DIALOGUE, PARENTHETICAL, ACTION, TRANSITION),
  PARENTHETICAL to Transition(DIALOGUE, DIALOGUE, DIALOGUE, DIALOGUE),
  DIALOGUE      to Transition(ACTION, PARENTHETICAL, ACTION, PARENTHETICAL),
  TRANSITION    to Transition(SCENE_HEADING, SCENE_HEADING, SCENE_HEADING, SCENE_HEADING))

sealed interface Command {
  data class InsertText(val id: BlockId, val at: Int, val text: String): Command
  data class DeleteRange(val id: BlockId, val start: Int, val end: Int): Command
  data class SplitBlock(val id: BlockId, val at: Int, val newType: ElementType): Command
  data class MergeWithPrevious(val id: BlockId): Command
  data class SetType(val id: BlockId, val type: ElementType): Command
  data class ToggleDual(val id: BlockId): Command
}
```

```
onEnter(block, cursor):
  if block.text.isBlank(): SetType(block, rules[type].onEnterEmpty); return
  newType = rules[type].onEnter
  SplitBlock(block, cursor, newType)
  if newType == PARENTHETICAL: insert "()", caret at 1
  if newType == CHARACTER: suggestions = rankCharacters(scene)
onTab(block):
  if suggestionVisible: accept(); advanceHeadingPart(); return
  if block.text.isBlank(): SetType(block, rules[type].onTabEmpty)
  else SplitBlock(block, end, rules[type].onTab)
```

Every edit is a command, so undo comes for free. Build in this order, each step unit-tested:
1. Commands
2. Rules table
3. Uppercase rendering
4. Auto-parentheses
5. Character suggestions
6. Heading parts
7. CONT'D
8. Smart detect
9. Dual dialogue
10. Scene numbers

---

## 3. Industry-Standard Formatting Spec

### 3.1 US Letter geometry

Courier 12 pt is **10 characters per inch and 6 lines per inch**, so every measurement becomes an exact character or line count.

| Element | Left edge | Right edge | Max chars | Case | Blank lines before |
|---|---|---|---|---|---|
| Scene heading | 1.5" | 7.5" | 60 | UPPER | 1 (some use 2) | [skrib](https://skrib.com/blog/screenplay-format-template-and-example)
| Action | 1.5" | 7.5" | 60 | Sentence | 1 | [skrib](https://skrib.com/blog/screenplay-format-template-and-example)
| Character | 3.7" | 7.5" | 38 | UPPER | 1 | [skrib](https://skrib.com/blog/screenplay-format-template-and-example)
| Parenthetical | 3.1" | ~5.6" | ~25 | lower | 0 | [skrib](https://skrib.com/blog/screenplay-format-template-and-example)
| Dialogue | 2.5" | ~6.0" | ~35 | Sentence | 0 | [skrib](https://skrib.com/blog/screenplay-format-template-and-example)
| Transition | 6.0" or right-aligned to 7.5" | 7.5" | ~15 | UPPER | 1 | [skrib](https://skrib.com/blog/screenplay-format-template-and-example)
| Page number | Flush right, 0.5" from top, "12." | — | — | — | From page 2 | [skrib](https://skrib.com/blog/screenplay-format-template-and-example)

**Page setup:** margins are 1.5" left, 1" right, 1" top and bottom, giving about 55 lines per page (54 body lines plus header). [skrib](https://skrib.com/blog/screenplay-format-template-and-example)

**Where sources disagree:** Final Draft's guide gives Character about 3.7", Parenthetical about 3.1" (right margin about 2.9") and Dialogue about 2.5" (right margin about 2.5"). [finaldraft](https://www.finaldraft.com/blog/what-are-the-margins-for-a-screenplay) Other guides differ by a few tenths of an inch. [nycmidnight](https://www.nycmidnight.com/howtowriteascreenplay) [scriptserious](https://scriptserious.com/wp-content/uploads/2025/10/Screenplay-Format-Guide.pdf) Make every value a template setting and calibrate against real Final Draft PDFs.

**A4:** keep the same left positions and column widths so wrapping matches US scripts, and allow about 58 lines. Offer an "A4, US-compatible pagination" option at 54 lines.

**Font:** bundle Courier Prime (SIL OFL 1.1), [github](https://github.com/quoteunquoteapps/CourierPrime) with base-14 Courier as a fallback.

### 3.2 Title page

Title, "Written by", author and source are centered. Contact and draft date go at the bottom left, as the Fountain spec recommends. [fountain](https://fountain.io/syntax/) No page number, and the title page isn't counted.

### 3.3 Page-break rules

These are conventions implemented by Final Draft and Fade In and codified in *The Hollywood Standard* (Christopher Riley). Buy the book as your tiebreaker; it wasn't reviewed directly here.
1. A scene heading needs at least 2 lines of the following element beneath it, or it moves to the next page.
2. Action splits only between sentences, with at least 2 lines on each side.
3. A character cue is never alone at the bottom of a page.
4. A parenthetical is never the last line on a page.
5. Dialogue splits at a sentence boundary, with at least 1–2 lines each side. Add **(MORE)** at the cue indent, then **CHARACTER (CONT'D)** at the top of the next page, keeping any extension.
6. A transition is never at the top of a page.
7. Dual dialogue is never split.
8. `===` and act breaks force a new page.
9. **Target:** within ±1 page of Final Draft on a 120-page script, with at least 95% of page starts matching. Wrap rules (spaces, "--", overlong words) matter most.

### 3.4 Feature vs. TV

- **Single-camera:** feature layout plus teaser/cold open and act headers. Each act starts a new page and ends with "END OF ACT ONE".
- **Multi-camera sitcom:** uppercase action, double-spaced dialogue, scene letters, underlined entrances and exits, and a new page per scene. [scriptserious](https://scriptserious.com/wp-content/uploads/2025/10/Screenplay-Format-Guide.pdf) Ship it as a v1.x template (mostly paginator parameters).

### 3.5 Production features

Revision marks, colored pages, locked/A-pages and OMITTED scenes go in **v2**. Reserve `revisionId` on spans and `sceneNumber` on blocks now, so FDX round-trip preserves them.

---

## 4. File Formats and Interoperability

### 4.1 Fountain

Support everything in 2.2, plus:
- **Title page:** `Key: value`. Multi-line values indent 3+ spaces or a tab. [fountain](https://fountain.io/syntax/)
- **Emphasis:** `*italic*`, `**bold**`, `***bold italic***`, `_underline_`, with backslash escapes. Emphasis never crosses line breaks. [fountain](https://fountain.io/syntax/)
- **Notes** `[[…]]` and **boneyard** `/* … */`. The boneyard can span lines. [fountain](https://fountain.io/syntax/)
- **Sections and synopses:** `#`, `##` for sections and `=` for synopses. Neither prints; both feed the navigator. [fountain](https://fountain.io/syntax/)
- **Page breaks:** `===`. **Lyrics:** `~`. **Centered:** `>…<`. [fountain](https://fountain.io/syntax/)
- **Whitespace:** tabs become 4 spaces in Action. Blank lines inside dialogue need a two-space line. [fountain](https://fountain.io/syntax/)
- **Error handling:** fall back to Action. Never look past a double line break for closing syntax (except the boneyard). [fountain](https://fountain.io/syntax/)
- **Edge cases:**
  - "...where…" (not a forced heading)
  - "CUT TO: " with a trailing space (Action)
  - all-caps action lines
  - `#I-1-A#` scene numbers
  - `STEEL (O.S.) ^`

**Parser:** write your own (roughly 500–800 lines of Kotlin). Beat, Trelby and STARC are GPL; use MIT Afterwriting only as a reference. Test against the official samples (Big Fish, Brick & Steel, The Last Birthday Card), which come with matching .pdf and .fdx files. [fountain](https://fountain.io/syntax/)

### 4.2 Final Draft .fdx

FDX is XML, introduced with Final Draft 8. The root is `<FinalDraft DocumentType="Script" Template="No" Version="…">`. [archiveteam](http://justsolve.archiveteam.org/wiki/Final_Draft) [laper](https://laper.ai/blog/fountain-format-vs-fdx-screenplay-files/)
- `<Content>` holds a sequence of `<Paragraph Type="Scene Heading|Action|Character|Parenthetical|Dialogue|Transition|Shot|General|New Act|End of Act">` elements. Each contains `<Text>` runs with `Style="Bold+Underline"`, `Font`, `Size` and `RevisionID`. [go](https://pkg.go.dev/github.com/lapingvino/lexington/fdx) [libertyseeds](https://libertyseeds.ca/2015/07/21/Investigating-Final-Draft-s-XML-document-format-with-Ruby/)
- Scene headings carry `Number` and `<SceneProperties Length Page Title>`. [filext](https://filext.com/file-extension/FDX)
- Dual dialogue is a `<DualDialogue>` wrapper around both speeches. Verify it against the Brick & Steel sample.
- `<TitlePage>` holds the title-page paragraphs. Document settings include `ElementSettings`/`ParagraphSpec`, `HeaderAndFooter`, `SmartType`, `MoresAndContinueds`, `Revisions`, `LockedPages`, `SceneNumberOptions` and `PageLayout`. [go](https://pkg.go.dev/github.com/rsdoiel/fdx)

**Round-trip strategy:**
- Map known paragraph types to your elements.
- Keep unknown attributes in `Block.extras`.
- Keep unknown top-level nodes verbatim in a `ForeignData` bag and write them back unchanged.
- Don't regenerate `ElementSettings` unless the user changes formatting.

### 4.3 PDF export

Requirements: exact paginator geometry, embedded subset font, scene/section bookmarks, metadata, and selectable text.

| Option | License | Fonts | Bookmarks | Verdict |
|---|---|---|---|---|
| `android.graphics.pdf.PdfDocument` | Platform | Canvas via Skia; embedding undocumented | No documented outline API | Spike only |
| **PdfBox-Android** | **Apache 2.0** | `PDType0Font.load` embeds a TTF subset [tabnine](https://www.tabnine.com/code/java/classes/com.tom_roush.pdfbox.pdmodel.font.PDType0Font) | `PDDocumentOutline`/`PDOutlineItem` [appdoc](https://appdoc.app/artifact/com.tom-roush/pdfbox-android/2.0.20.0/com/tom_roush/pdfbox/pdmodel/PDDocumentCatalog.html) | **Recommended.** Last release v2.0.27.0 (Jan 2023): [mvnrepository](https://mvnrepository.com/artifact/com.tom-roush/pdfbox-android) pin it and wrap it behind an interface |
| OpenPDF | MPL-2.0 or LGPL-2.1+ [github](https://github.com/LibrePDF/OpenPDF/blob/master/LICENSE.md) | Yes | Yes | Current releases target Java 21+, so Android fit is doubtful [github](https://github.com/LibrePDF/OpenPDF/blob/master/README.md) |
| iText Core | **AGPLv3 or commercial** [itextpdf](https://itextpdf.com/how-buy/AGPLv3-license) | Yes | Yes | **Avoid** |
| WebView print | Platform | Browser | Limited | The browser controls pagination, so fidelity breaks. Avoid |

The desktop port can use upstream Apache PDFBox behind the same interface.

### 4.4 Native format

**Save as Fountain plus a JSON sidecar, and edit a block model in memory.**
- **Why Fountain:** it's readable, diffable, and opens in Highland, Beat and JotterPad, a strong trust signal for an own-your-files app.
- **What goes in the sidecar:** locked numbers, SmartType lists, revision IDs, FDX foreign data, template and cursor position. Store it as `.fountain` + `.meta.json`, or zip both into a `.scriptpkg`.
- **How to write Fountain:** always use explicit forcing (`.`, `@`, `!`, `>`) where inference could be ambiguous, so your saved files re-parse into identical blocks.

### 4.5 Other formats

- **MVP:** Fountain, PDF.
- **v1.0:** FDX, .txt.
- **v1.x:** .highland (a zip containing Fountain), .docx.
- **v2:** .fadein, Celtx.
- **Probably never:** PDF import.

---

## 5. Technology Stack

### 5.1 Comparison

| Criterion | (a) Kotlin + Compose | (b) EditText + Spannables | (c) KMP + Compose MP | (d) WebView editor + Capacitor | (e) Tauri 2 | (f) Flutter | (g) React Native |
|---|---|---|---|---|---|---|---|
| Structured editor fit | **Good** (block list of BasicTextFields) | Good for one document; per-paragraph right margins awkward | Same as (a) | Excellent frameworks | Same as (d) | Custom editor needed | Weak |
| Soft IME / Gboard | **Native** | Native, most proven | Native | **Recurring Gboard/Samsung bugs** | Same as (d) | Own IME bridge, less proven (judgment) | Depends on wrapped view |
| 120+ pages | LazyColumn virtualizes | Huge spanned EditText slows | Same as (a) | Good | Good | Good | Varies |
| Accessibility | Good | Best | Good | Fair–good | Fair | Fair–good | Fair |
| SAF / offline | Native | Native | Native | Plugin | Plugin | Plugin | Library |
| Desktop path | Via (c) | None | **Built in** | Built in | Built in | Built in | Weak |
| Beginner curve | Moderate | Moderate | Moderate+ | **Easiest for web devs** | Hard (Rust) | Moderate (Dart) | Moderate |
| AI-assistant support | Very high | High | Medium–high | Very high | Medium | High | High |

### 5.2 Primary: (a) Kotlin + Compose, KMP-ready core

**Why:**
- Correct IME behavior is the hardest problem, and native text fields solve it.
- Block-per-element editing turns every formatting rule into split/merge/setType instead of span surgery.
- Each block gets its own `KeyboardOptions` (caps and autocorrect per element).
- Compose 1.9's `OutputTransformation.addStyle` renders uppercase and emphasis without touching stored text. [medium](https://medium.com/@hiren6997/whats-new-in-jetpack-compose-1-9-from-performance-boosts-to-new-ui-apis-f5cddd4a2708)
- With `core` free of Android imports (later KMP `commonMain`), a Compose Multiplatform desktop port reuses most of the logic.

**Limitations and mitigations:**
- **Cross-block selection:** offer block multi-select plus "Select/Copy scene".
- **Find/replace:** runs on the model.
- **Arrow and Backspace across blocks:** implement as tested commands.

### 5.3 Fallback: single-field Fountain source mode

If the block-editor spike fails, do what Highland, Beat and JotterPad do: one text field containing Fountain, with live indents and styling and incremental re-parsing. The editor loses exact dialogue right margins (Page View shows them). The core, paginator and exporters are unchanged.

**Not recommended:**
- **(d)/(e):** CodeMirror's documented Gboard issues (#96, #1028, #1145, #1504), which even Replit hit, target your core value directly. [github +3](https://github.com/openchamber/openchamber/issues/3514)
- **Sora Editor:** LGPL-2.1 and code-oriented. [github](https://github.com/Rosemoe/sora-editor) [context7](https://context7.com/rosemoe/sora-editor)
- **(g):** adds a bridge between you and the text system.

---

## 6. Architecture

```
:core-model    Script, Block, Span, TitlePage, Settings (pure Kotlin)
:core-format   Rules, command processor, SmartLists, CONT'D logic
:core-fountain Parser + forcing writer
:core-fdx      FDX reader/writer with foreign-data preservation
:core-layout   Line wrapper, paginator, PageModel
:core-export   Renderer interface; PdfRenderer (PdfBox), TxtRenderer
:core-reports  Character/scene/location stats, sprints
:data          Room (library, snapshots), SAF I/O, autosave
:app           Compose UI: library, editor, navigator, page preview, settings
```

**Data flow:** keystroke → InputTransformation → `Command` → ViewModel applies it to an immutable `Script` and pushes the inverse onto the undo stack → UI diffs by BlockId. In parallel: debounced autosave (2 s), incremental layout (re-wrap the dirty block, repaginate until page starts re-converge), and navigator/SmartList updates.

- **Undo:** command stack, with typing coalesced into about 1-second steps and a cap of about 500.
- **Crash-safe saves:** temp file, then fsync, then atomic rename. SAF targets are written via `openOutputStream(uri, "wt")` from the verified temp copy. Keep a `.bak`.
- **Snapshots:** on every manual save and hourly while editing, kept in private storage (last 50). This replaces the "version history" that subscription apps charge for.
- **Library:** Room indexes URIs, titles, page counts and snapshots. Scripts stay as user-owned files, opened and created via SAF with persisted permissions. Users can save into folders a sync app mirrors, without the app doing any cloud work itself.
- **Search:** regex, case and scope options (for example, one character's dialogue). Replace is one undoable batch.
- **Navigator:** built from headings, sections and synopses, with page numbers, drag-reorder and color tags.

---

## 7. Phased Roadmap (~10 hours/week)

| Phase | Duration | Scope | Definition of done |
|---|---|---|---|
| **0. Foundations** | 10–12 wks | Kotlin, Compose, Git, testing | Two small apps built (Room notes app, SAF file editor) |
| **1. Risk spikes** | 4–6 wks | Block editor on Gboard, Samsung, SwiftKey and hardware keys. Paginator vs Final Draft Big Fish PDF. FDX read/write of Brick & Steel | 5 pages typed per keyboard with no lost or duplicated text. Big Fish within ±2 pages. FDX opens cleanly in Final Draft trial or Fade In demo. **Go/no-go on block vs single-field** |
| **2. MVP** | 16–24 wks | All elements, Enter/Tab, element bar, autocomplete, uppercase, auto-parens, CONT'D, Fountain, PDF with (MORE)/(CONT'D), autosave, snapshots, library, dark mode | A 10-page short written on phone and Chromebook without touching formatting. PDF passes golden tests on 3 scripts |
| **3. v1.0** | 16–24 wks | FDX, navigator reorder, title page, find/replace, dual dialogue, scene numbers, reports, page view, focus mode, spell check (Action/Dialogue), sprints, TV template, A4, accessibility | 120 pages at 60 fps on a mid-range phone. ±1 page vs Final Draft on 5 scripts. No data loss in a 2-week beta. Closed test passed |
| **4. v1.x** | ongoing | Index cards, notes panel, multi-cam, .highland, .docx, themes, keymaps | Feedback-driven |
| **5. v2** | 6–12 months | Revisions, locked pages/A-pages, locked scene numbers, OMITTED; desktop | Production FDX round-trips with revisions intact |

**Cut order if scope grows:** index cards → sprints → docx → reports → dual dialogue → A4. Never cut Enter/Tab, autocomplete, pagination accuracy, Fountain/PDF or autosave.

**Total:** about 12–18 months to v1.0 at 10 hours/week, or 7–10 months at 20 hours/week.

---

## 8. Learning Path and Workflow

1. **Kotlin** (2–3 weeks): official docs and Kotlin Koans. Focus on data classes, sealed interfaces, collections and coroutines.
2. **Android Basics with Compose** (Google's free course, 4–6 weeks): state, ViewModel, navigation, Room.
3. **Compose text input** (1 week): "Configure text fields" and "Migrate to state-based text fields" (TextFieldState, InputTransformation, OutputTransformation), plus `onPreviewKeyEvent`. [android](https://developer.android.com/develop/ui/compose/text/user-input) [android](https://developer.android.com/develop/ui/compose/text/migrate-state-based)
4. **Testing** (1 week): JUnit for `core` and Compose UI testing.
5. **Git/GitHub** (ongoing): tag each milestone. **Later:** Kotlin Multiplatform "Get started".

**Tools:**
- Android Studio with phone, tablet and resizable-desktop emulator profiles
- A real Samsung phone
- A Chromebook or tablet with a Bluetooth keyboard
- Gboard and SwiftKey installed
- GitHub Actions running `core` tests

**AI assistants:**
- **Use them for:** the parser (paste the spec plus tests), test fixtures, FDX mappers and code review.
- **Rules:**
  1. Keep `core` pure Kotlin so tests run without an emulator.
  2. Make one small change at a time, with tests.
  3. Paste current docs. Assistants often suggest the older value/onValueChange TextField API. [medium](https://medium.com/@sivavishnu0705/compose-just-fixed-text-fields-for-good-heres-the-mental-model-shift-7f1b4fb50bb7)
  4. Device-test any IME-related code.
  5. License-check "recalled" code, which may be GPL from Beat or Trelby.
- **Watch for:** invented FDX tags, wrong pagination math, and IME cases the assistant can't observe.

---

## 9. Testing and Quality

- **Unit:**
  - every element × {Enter, Tab} × {empty, non-empty}
  - ranking
  - CONT'D
  - all Fountain spec examples and edge cases
  - FDX type/style/dual/scene number
- **Property:** `parse(write(s)) == s`, and `fdxRead(fdxWrite(s))` preserves known fields and foreign data.
- **Golden pagination:**
  - Use Final Draft PDFs of 5 scripts: Big Fish from fountain.io, plus trial-generated TV and dual-dialogue cases.
  - Extract page-start text with PDFBox.
  - Track page-count delta and % matching page starts. Fail CI on regression.
- **Visual:** render pages to PNG and diff against baselines.
- **IME matrix** (before each release):
  - Keyboards: Gboard, Samsung, SwiftKey and one open-source IME.
  - Scenarios: tap-correct mid-word, accept suggestion, swipe typing, Enter mid-word, Backspace-merge, voice input, paste, hardware keys.
  - Any caret jump or duplication blocks the release.
- **Performance:**
  - Scripts: a 150-page stress script and Big Fish.
  - Targets: keystroke under 16 ms, open under 1.5 s, full repagination under 300 ms, incremental under 30 ms.
  - Tools: Macrobenchmark.
- **Data safety:** kill the app mid-save 100 times with zero corruption; low-storage test; SAF permission revocation.
- **Samples:** Big Fish, Brick & Steel, The Last Birthday Card, plus generated files. Don't bundle copyrighted scripts.

---

## 10. Release and Monetization Checklist

**Google Play (2026):**
- [ ] **Registration:** US$25 one-time, with government ID and credit card. Prepaid cards aren't accepted. [google](https://support.google.com/googleplay/android-developer/answer/6112435?hl=en)
- [ ] **Closed test:** 12+ testers for 14 continuous days (personal accounts created after November 13, 2023). [google](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en) Recruit 15–20. Organization accounts (D-U-N-S) are exempt. [github +2](https://12-testers-for-14-days.github.io/)
- [ ] **Target API:** Play Console Help: "Starting August 31, 2026: New apps and app updates must target Android 16 (API level 36) or higher." Extensions run to November 1, 2026. Expect API 37 around August 2027.
- [ ] **Packaging:** AAB with Play App Signing.
- [ ] **Data safety form and privacy policy:** both required even with no data collection. Google says they can simply state that none is collected or shared. [google](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en) Host the policy on GitHub Pages.
- [ ] **Payments and fees:** set up a payments profile and taxes. For new installs from June 30, 2026 in the EEA, UK and US, Google's "Understanding Google Play's lower service fees" page lists the initial purchase of a paid app at 20% plus a billing fee as standard, or 15% under specific programs. The first $1M of annual earnings is charged "10% + billing fee", and the billing fee is "set at 5%" in these markets. Other markets pay 15% on the first $1M with tier enrollment. Recheck at launch.
- [ ] **Registration for verification:** Play apps must be registered by September 30, 2026. The Android Developers Blog says "99% of apps on Play have been registered automatically."
- [ ] **Refunds:** Play's windows apply. Promise "all 1.x updates included."

**Pricing model:**

| | Paid upfront ($9.99–$19.99) | Free + one-time unlock |
|---|---|---|
| Pros | No billing code; clear promise | Try before buy; more installs and reviews |
| Cons | Fewer installs; no trial | Billing Library upkeep; risk of recreating a paywall |
| Pick | — | **Recommended.** Writing and clean PDF free; ≈$14.99 unlock for FDX, reports and templates |

**Alternative distribution:**
- **Personal use:** Android Studio or ADB always works ("Unregistered apps can still be installed using ADB or the advanced flow"). [helpnetsecurity](https://www.helpnetsecurity.com/2026/03/31/android-developer-verification-requirement/)
- **Direct APK/itch.io:** from September 30, 2026 in Brazil, Indonesia, Singapore and Thailand (globally in 2027), apps from unverified developers need an advanced flow with a 24-hour wait. Register in the Android Developer Console ($25, ID) to avoid it. [google +2](https://support.google.com/android-developer-console/answer/16604405?hl=en) The free limited-distribution account (August 2026) covers up to 20 devices without ID, which suits beta testers. [androidauthority](https://www.androidauthority.com/android-sideloading-changes-timeline-3679204/)
- **Samsung Galaxy Store:** a viable second store. [mobilemasr](https://mobilemasr.com/en/blogs/android-and-sideloading)
- **F-Droid:** needs open source and has no payments, so it doesn't fit.

**Legal:**
- **Dependencies:** Apache, MIT, BSD, MPL and OFL are fine. Avoid GPL and AGPL. Treat LGPL with care.
- **Courier Prime (OFL 1.1):** can be bundled and sold with software if you include the notice and license text, don't sell the font alone, and rename any modified version. [spdx](https://spdx.org/licenses/OFL-1.1.html)
- **Trademarks:** "Imports/exports Final Draft® (.fdx) files" is nominative use. Keep competitor names out of your app name and icon, and credit the trademark in the listing.

**Cost estimate:** Play $25, test devices $0–$400, optional Final Draft license $0–$199.99, *The Hollywood Standard* about $25, AI assistant $0–$20/month. **Total to v1.0: about $50–$900.**

---

## 11. Risks and Mitigations

| Risk | Likelihood / impact | Mitigation |
|---|---|---|
| IME misbehavior | High / critical | Native fields, visual-only transforms, model-level splits. Phase 1 spike on 3 keyboards. Single-field fallback ready |
| Pagination mismatch | High / high | Character-grid layout, configurable template, golden tests from Phase 1. Publish "±1 page" honestly |
| FDX corruption | Medium / high | Preserve unknown XML, property tests, real-file tests. Label FDX "beta" |
| Long-script performance | Medium / medium | Virtualization, per-block layout cache, incremental pagination, CI benchmark |
| Data loss | Low / critical | Atomic writes, .bak, snapshots, kill tests |
| PdfBox-Android stagnation | Medium / medium | Interface wrapper, base-14 fallback, Canvas emergency path |
| Scope creep / burnout | High / high | Cut list. Ship for personal use first. Time-box phases |
| Play policy churn | Certain / low–medium | Annual targetSdk bump. Re-read policy each release |
| GPL contamination | Medium / high | Own parser, Gradle license checks, review AI output |

---

## Caveats

- **Pricing sources:** Final Draft, Highland, Fade In, STARC and Google figures come from official pages. Arc Studio, Celtx and WriterDuet tier details are partly from third-party reviews, and WriterDuet Plus is quoted at $7.99–$9.
- **Layout measurements** vary by a few tenths of an inch between authorities. Final Draft's published values are the defaults, and golden files are the final arbiter.
- **FDX** has no public spec. The structure here comes from real files and community parsers.
- **PdfDocument's missing outline API** is inferred from its documented methods. [android](https://developer.android.com/reference/android/graphics/pdf/PdfDocument)
- **Fees:** Google's June 2026 fee tables separate new installs from existing installs (existing installs pay 25% as standard). Confirm which rows apply to your app at launch.
- **Flutter, Tauri and React Native IME assessments** are engineering judgment.
- **Time estimates** assume about 10 focused hours per week.