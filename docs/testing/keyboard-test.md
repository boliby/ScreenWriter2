# Keyboard test (Phase 1A)

This build is a rough editor for one question: does typing work reliably on
real keyboards? Watch for text that is **lost, doubled, or lands in the wrong
place**, the **cursor jumping**, and the **keyboard closing by itself**. Those
are the problems that sink screenwriting apps on Android.

Run the whole list once with **Gboard**, once with **SwiftKey**, and the
hardware section with the **Bluetooth keyboard**. To switch soft keyboards on
the Pixel, tap the keyboard icon at the bottom right while typing, or go to
Settings > System > Keyboard.

## Not in this build (not bugs)

- Nothing is saved. Closing the app loses your text. Rotating the phone keeps it.
- No autocomplete, automatic parentheses, or (CONT'D).
- The layout is rough. Only the typing matters here.

## How it works

Each line of the script is its own element: Scene heading, Action, Character,
Parenthetical, Dialogue, Transition, or Shot. The bar above the keyboard shows
which element you're in. Tap a chip there to change it, or tap **Tab**. Empty
lines show the element name in grey.

- **Enter** starts the next element: Scene heading → Action, Character →
  Dialogue, Dialogue → Action, Transition → Scene heading.
- **Enter on an empty line** changes its element instead: an empty Action
  becomes Character, and an empty Character or Dialogue becomes Action.
- **Tab on an empty line** changes its element: Action → Character → Transition,
  and Dialogue ↔ Parenthetical.
- **Backspace at the start of a line** joins it to the line above when they're
  the same element. Otherwise the first press changes it to the element above
  and a second press joins them. On an empty line, Backspace removes the line.

## Soft keyboard checklist

Start each keyboard with **Blank** in the top bar.

1. **Basic flow.** Type `int. kitchen - day` and press Enter. The heading shows
   in capitals and the new line is Action. Type a sentence and press Enter
   twice; the second press turns the empty line into Character. Type a name and
   press Enter; you're in Dialogue. Type a line, then press Enter.
   *Pass:* each line is the element you expect and nothing is lost.
2. **Capitals switch.** On a Character or Scene heading line, the keyboard
   should type in capitals on its own. Check again after changing elements with
   Tab or the chips.
3. **Fix a word by tapping it.** In Action, type `teh cat`, tap `teh`, and pick
   the correction.
   *Pass:* the word is fixed, the cursor doesn't jump, and no letters repeat.
4. **Suggestions.** While typing a word, tap a suggestion in the strip above
   the keys.
5. **Swipe typing.** Swipe a full sentence into Action and Dialogue.
6. **Voice typing.** Use the keyboard's mic to dictate a sentence into Action.
7. **Enter in the middle of a word.** Put the cursor inside a word in Action,
   such as `cof|fee`, and press Enter.
   *Pass:* two lines, `cof` and `fee`, with nothing lost or doubled. Press
   Backspace at the start of `fee`; it joins back into `coffee`.
8. **Backspace between elements.** Put the cursor at the start of a Dialogue
   line and press Backspace. It becomes Character. Press it again, and it joins
   the name line. On an empty line, Backspace removes the line and puts the
   cursor at the end of the line above.
9. **The bar keeps the keyboard up.** Tap Tab and several chips while typing.
   *Pass:* the keyboard stays open and you keep typing on the same line.
10. **Paste.** Copy a few lines of text from another app and paste them into
    Action. Each line becomes its own element.
11. **Endurance.** Write about 5 pages (roughly 100 lines) of mixed elements.
    Then scroll up and edit lines in the middle and near the top.
    *Pass:* no slowdown, no jumping, and new lines near the bottom of the screen
    stay visible above the keyboard.

## Bluetooth keyboard checklist

Connect the keyboard to the Pixel and tap into a line.

- **Enter** and **Backspace** behave as above.
- **Tab** follows the Tab rules. **Shift+Tab** steps back one element.
- **Ctrl+1** to **Ctrl+7** set Scene heading, Action, Character, Parenthetical,
  Dialogue, Transition, and Shot.
- **Arrow keys** move between lines. Up from the first row of a line goes to
  the line above; Down from the last row goes to the line below.
- Type fast: press Enter and immediately type the next line. The first letters
  should land on the new line, not the old one.

## Undo and redo (build 0.2.0)

**Undo** and **Redo** are at the left of the element bar. On the Bluetooth
keyboard they're **Ctrl+Z** and **Ctrl+Shift+Z** (or **Ctrl+Y**). Typing
without a pause of about a second counts as one step.

1. **Typing.** Type a sentence, pause, and type another. Undo removes the
   second sentence, then the first. Redo brings them back.
2. **Enter.** Press Enter in the middle of a line, then Undo.
   *Pass:* the two halves join back into one line with the cursor where you
   pressed Enter.
3. **Backspace join.** Join two lines with Backspace, then Undo. They split
   again.
4. **Element changes.** Change a line's element with Tab or a chip, then Undo.
   It goes back to the element it was.
5. **Paste.** Paste several lines, then Undo. The whole paste disappears in
   one step.
6. **Mixed.** Do a few of the above, then Undo all the way back and Redo all
   the way forward.
   *Pass:* every step comes back exactly, no text is lost or doubled, and
   the keyboard stays up throughout.
7. **With each keyboard.** Repeat 1 and 2 on Gboard, SwiftKey, and the
   Bluetooth keyboard. Also try Undo while a word is still underlined as you
   type it.

## Reporting a problem

For each problem, send the keyboard, what you did, what you expected, and what
happened. A screen recording is ideal: pull down Quick Settings and tap
**Screen record**.
