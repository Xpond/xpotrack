# Expense regression checks

Use a test installation with separate data. Run the build checks with:

```sh
./gradlew testDebugUnitTest assembleDebug assembleRelease lintDebug
```

## Keyboard and saving

1. Open Expense and enter `Lunch 260`, then twenty `Item 10` lines.
   The total must be ₹460, and the caret must stay above the keyboard.
2. Move the caret upward to the first line. It must stay below the pinned header.
3. Repeat on an older date below today's row; this checks the nested field position.
4. Switch days and months while editing. The newly focused field must continue
   following the caret after its stored text loads.
5. Collapse/reopen and restart the app. Text and totals must survive.
6. Clear the older day's text and collapse it. Its row and calendar dot must disappear.

## Date state

- Switch between months containing different entries. The old month's rows and
  total must disappear as soon as the header changes, before the next Room result.
- `ExpenseDatesTest` reproduces that delayed-query case.
  `ExpenseViewModelTest` covers next-day/year rollover, preserving an active draft on a midnight tick, and keeping deliberate historical selections.
- `ExpenseMidnightTest` checks the next local midnight on ordinary days and
  both daylight-saving transitions, including a sub-millisecond boundary.
- Re-entering Expense on the same day preserves the user's collapsed state.
- Day blocks are keyed by epoch day; switching dates must not reuse another
  editor's remembered focus or layout state.

## Collapsed previews

- `Paid by Ravi` must appear in the collapsed preview with a zero total.
- `260` must have a nonempty preview and a ₹260 total.
- A mixed day must show both expense labels and note text.

## Calendar

- Check portrait and landscape, including a six-week month such as November 2026.
- All dates must fit, or be reachable by scrolling in a short window.
- Select dates in the first and last weeks and navigate across month boundaries.
- Repeat from Tasks, which uses the same calendar component.

## Shared editor regression

The caret effect is also used by Notes, Quick Notes, and Vault. Check long text
and upward cursor movement in those editors after changing caret behavior.
For the PR's vault fix, opening an older note and returning without edits must
preserve its list position; editing it should move it to the top.
