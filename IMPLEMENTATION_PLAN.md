# Implementation plan

This plan covers all the feedback collected for the Module UI Template up to 2026-11-16. Each
step below ends in one commit pushed to `ChaliceChore/Lsposed-Module-Template` (now public). Work
starts when you say **go**.

All decisions are settled.

## Principles

- **Foundations first.** The model, hook and theme come before the screens, so the screens are
  built once on the final data.
- **Every step builds.** Each step ends with `assembleDebug` passing. Every few steps there's a
  checkpoint where I install the app on your phone so you can look at it.
- **Optional parts are switched on in one place.** A new `ModuleConfig.kt`, marked with
  `TEMPLATE:` comments, holds the per-module switches: search, back up and restore, Try it, the
  translate URL and the home layout. In this template build every switch is **on**, so you can
  test everything at once.
- **Demos stay separate.** The optional components (#8–17) are shown on a separate demo category
  called *Components*. A module deletes that one category to remove all of them. Release builds
  (R8) strip any component a module doesn't use.
- **Strings settle before translation.** The 12 translations are written last, once the English
  text has stopped changing.

---

## Phase A: Foundations

### A1. Brand colour and theme
- The colour scheme is generated from the seed **Deep Cerulean Teal #0E7C93**, in light and dark.
  It replaces the Graphite scheme. Dynamic color still overrides it when switched on.
- **Pure black** setting: dark-mode surfaces become #000000. The setting is stored in the
  appearance prefs.
- The launcher icon background and glyph accent are updated to the teal.

### A2. Settings model: master toggle and restart tagging
- `FeatureSettings.enabled` (default **on**) is the master toggle.
- Each setting declares `applies = RESTART | LIVE`. This includes the switches, the choice, and
  the master toggle, which is RESTART.
- `LIVE` settings skip the apply sheet. They are written to remote prefs straight away, and the
  hook picks them up through a remote-prefs change listener.
- `UiState` splits pending changes into restart-pending and live. Only restart-pending changes
  show the sheet.

### A3. Hook side: on/off gate, load report and feature health
- `onPackageReady` reads the settings first. If the master toggle is off, it logs and installs
  nothing.
- A new `StatusProvider` content provider in the app (the pattern from DualDial and App Info
  Tweaks):
  - **Load report:** the hook calls `loaded` with a timestamp. The app uses this for "the
    running target hasn't loaded the module yet" (#18).
  - **Feature health:** the hook reports each feature as installed or failed, with a reason (#2).
  - **Security:** only calls from the target app's uid are accepted.
- Hook-side helper: `installFeature(FeatureSwitch) { … }`. It wraps each hook, catches
  failures, and reports the result.
- **Debug logging** (#6): the hook writes verbose logs only while the `debug_until` setting holds
  a time in the future.

### A4. Module config
- Add `ModuleConfig.kt` with the switches listed under Principles, plus the Try it action and
  its label (default "Try it").
- `gradle.properties` gets `module.translate=` (the Weblate URL). The Help translate row is
  hidden when it's empty.

**Checkpoint 1:** the app builds and looks the same, apart from the teal colours.

---

## Phase B: Home

### B1. Status card
- Split layout in the template's own styling: status on the left, a divider, and the M3 switch
  (master toggle) on the right.
- The status icon sits inside a morphing shape:

  | State | Shape | Colour | Icon |
  |---|---|---|---|
  | Active | Cookie | primary container | ✓ |
  | Changes pending | Clover | tertiary container | sync |
  | Off in the app | Pill | surface variant | ⏸ |
  | Not active in LSPosed | Burst | error container | ✕ |
  | Applying | slow spin | | |

  The shape morphs only when the state changes, and swaps instantly when reduced motion is on.
- **Feature health line:** "Partly active · N features need attention", with the card turning
  amber. Tapping it opens the Feature health screen (step E2).
- **Versions footer** (#7): one line under the card, `<Target> v15.2.1 · <Module> v1.4.0`.

### B2. Header and Try it
- The app icon sits beside the app name in the large top bar: about 40dp when expanded and about
  24dp when collapsed. The icon is drawn from the adaptive icon layers.
- **Try it** (optional) is a tonal button with ↗ in the top bar. It is disabled when the module
  isn't active, or when the module has been turned off and that change applied. If changes are
  pending, a dialog offers *Apply and try*, *Try anyway* or *Cancel*. If the target can't be
  opened, a snackbar says so.
- **Search** (#3, optional) adds an icon in the top bar. Details are in step E1.

### B3. Features section
- Turning the master toggle off dims and disables the Features rows. Their values are kept.
- **Home layout** (#17): the categories show as a list (default) or as a two-column grid of
  cards, with the cards in each row matching the tallest. This is chosen in Appearance.

**Checkpoint 2:** install on your phone and review the Home screen.

---

## Phase C: Apply sheet

### C1. Discard and Apply
- Two buttons: **Discard** on the left (smaller, outlined or tonal) and **Apply and restart
  <target>** on the right (filled, wider).
- Discard always asks for confirmation, even for one change.

### C2. Reverting one change
- Each change row in the expanded sheet gets an ✕ button, and rows can also be swiped sideways.
  Either one reverts only that change.
- No confirmation for these. Reverting the last change hides the sheet.

### C3. Restart to load
- If there are no pending changes but the running target hasn't loaded the module yet (it was
  enabled in LSPosed after the target started), the sheet peeks with "Restart <target> to load
  the module". This replaces the top-bar restart buttons the other modules use.

**Checkpoint 3:** install on your phone and review the apply sheet.

---

## Phase D: Appearance and About

### D1. Appearance
- Rows in this order:
  1. Theme
  2. Dynamic color
  3. **Pure black**: "Use black backgrounds in dark mode", disabled when Theme is Light
  4. **Language**
  5. **Home layout**: list or grid
  6. Hide launcher icon
- **Language:** a radio dialog with "System default" first, then each language written in its
  own script. It uses AppCompat `setApplicationLocales`, so it works on Android 12 too. AppCompat
  becomes a dependency and MainActivity switches to `AppCompatActivity`. The language list is
  generated from the `values-xx` folders (`generateLocaleConfig`).

### D2. About: identity card
- The morphing shape frames the real app icon. Tapping it pulses the shape.
- Buttons: **Star on GitHub** and **Ko-fi** (https://ko-fi.com/ChaliceChore, coffee icon), as a
  connected pair.
- The **Releases** button is dropped, because the Version row (D3) now opens the latest
  release.

### D3. About: rows
Rows in this order:

| Row | What it does |
|---|---|
| **Version** (#1) | Subtext reads "v1.2.0 · Tap to check for updates". After a tap it shows "You are on the latest version", "Update to v1.3.0" (tap to open the release) or "Could not check. Tap to retry." |
| What's new | |
| Changelog | |
| Source code | `< >` icon (Outlined.Code) |
| **Report a problem** (#5), in the Troubleshooting group (E2) | Shows a preview first, with "What's in the report" and "Never in the report", then opens a GitHub issue form filled in through the URL. A report longer than the URL allows is copied to the clipboard instead. |
| **Request a feature** (#5) | Opens the feature request form. No device info is included. |
| **Help translate** | Opens the Weblate page. Hidden when `module.translate` is empty. |
| **Author's profile** | Subtext github.com/ChaliceChore, GitHub icon |
| **More modules by ChaliceChore** | Opens the More modules screen (D4) |
| Licenses | |

### D4. More modules screen
- A single column of cards, each showing the module's icon, name, description, star count and a
  repo link.
- **Data:** a snapshot is built into the APK at build time. When online, the app refreshes it from
  `GET /users/ChaliceChore/repos` and keeps repos that have the topic `lsposed-module`, aren't
  forks, aren't archived, and aren't this module. The result is cached for one day.
- **Icons:** each repo's `icon.png`, with a generic icon if it's missing.
- Adds the `INTERNET` permission. The GitHub client is shared with the Version row.

**Checkpoint 4:** install on your phone and review Appearance and About.

---

## Phase E: Tools

### E1. Search settings (#3, optional)
- A search field opens from the Home top bar. It searches the titles and summaries of every
  setting, and tolerates typos.
- Tapping a result opens that category and briefly highlights the row.
- Search is built from the setting catalog, so it covers new settings with no extra work.

### E2. Feature health screen (#2)
- Lists each feature with its state: OK, failed (with the reason) or not checked yet.
- Has an "Alert when a feature breaks" switch. The switch only stays on if notification
  permission is granted.
- Affected rows and categories get a red "Doesn't work in this <target> version" chip. Tapping
  the chip shows the reason. The affected category also gets a warning banner.
- The screen opens from the status card line, and from a **Troubleshooting** group in About.
  That group holds Feature health, Debug logging and Report a problem.

### E3. Debug logging (#6)
- A Troubleshooting row whose subtext reads:
  - off: "Off. Turn it on to record details while you look into a problem."
  - on: "On until 18:30. If you leave it on, it turns itself off after 24 hours."
- A "What's recorded" explainer.
- The report from D3 includes these logs when debug logging is on.

### E4. Back up and restore (#4, optional)
- Saves the settings to a JSON file through the system file picker, and loads them back.
- Each section can be ticked, with Select all and Select none.
- A diff chip ("3 changes") lists exactly what will change.
- A restore marks the changes as pending, so they go through the apply sheet.
- The dialogs are a first version only. You'll polish the design later.

**Checkpoint 5:** install on your phone and review the tools.

---

## Phase F: Optional components (demo category "Components")

| # | Component | Demo |
|---|---|---|
| 8 | Live preview block | A preview card that changes as the demo settings change. It sticks to the top when it's inside an options sheet and the options scroll. |
| 9 | Split row with options sheet | Tapping the left side opens a sheet, and the switch on the right turns the group on or off. Inside the sheet, the group's switch sits in a sticky header instead of a separate row. |
| 10 | Expandable row | "Enabled for: A, C", which expands into one switch per item. |
| 11 | Colour picker | Has three modes: **Swatches**, **Wheel** and **Area**, the area being a saturation and brightness square with a hue slider. All three share the hex field and a preview. This is a redesign, not a copy of Undo Notification Swipe's picker. |
| 12 | Chip selector | A 4-option FilterChip row. |
| 13 | Duration row | "Default (4 s)" or a custom value from a slider dialog (2 to 15 s). |
| 14 | App picker | A polished dialog: a search field, app icons with labels, an "Installed" group and a "System" group, and the selected app pinned to the top. It is not modelled on Overboard's picker. |
| 16 | Try-out | A card with an input field and a live result. The demo reverses or uppercases text. |

Each component is in its own file in `ui/component/`.

**Checkpoint 6:** install on your phone and review the components.

---

## Phase G: Localization

### G0. String split (done)
- `res/values/strings_template.xml`: shared template wording every module keeps. This is the only
  file that gets translated and the only one Weblate tracks.
- `res/values/strings_module.xml`: each module's own wording (features, categories, options,
  demo). Replaced by each module, never translated by the template.
- The Language picker lists all 12 languages, but only those in `TranslatedLanguages` can be
  picked. The rest are greyed out with "Not translated yet".

### G1. Translations (deferred until layout and wording are final)
- `values-xx/strings_template.xml` only, for: es, pt-BR, fr, de, ru, zh-CN, zh-TW, ja, in, tr,
  vi and ar. Each finished language is added to `TranslatedLanguages`.
- URLs, names and other strings that must not be translated are marked `translatable="false"`.
- Arabic gets a right-to-left check: mirrored icons, slide transitions and the split card.

### G2. Weblate readiness
- The repo layout is set up to work with Weblate, with `strings_template.xml` as the one component.
- The README gets a section on how to connect Weblate: create the component, add the webhook,
  and push changes as pull requests.
- Translations are labelled as machine-written, with an invitation to correct them.

**Checkpoint 7:** switch between languages on your phone and review them.

---

## Phase H: Repo and docs

### H1. GitHub issue forms
- `.github/ISSUE_TEMPLATE/bug_report.yml` and `feature_request.yml`. Their field ids match the
  query parameters the app fills in.

### H2. Release conventions
- Update `../GITHUB_RELEASE.md`: add the topic `lsposed-module`
  (`gh repo edit … --add-topic lsposed-module`) and put an `icon.png` in each repo's root.
- This file is outside the template repo, so the change isn't part of the template commit.

### H3. README
- Update the README to describe the new layout, `ModuleConfig`, the RESTART/LIVE tagging and the
  feature-health helper.
- Update the "Start a new module" checklist.

### H4. Clean up
- `UI_FEATURES_REVIEW.md` moves to `docs/`. It is gitignored, so it stays on this device only.

**Final checkpoint:** full review on your phone. After that, the next task is the adoption Doc
for the module folders.

---

## Risks and notes

- **Live settings:** a LIVE setting only works if the hook actually reads it again when it
  changes. The adoption Doc will require every module to tag each setting and test it.
- **Feature health accuracy:** it only knows that a hook installed. It can't tell whether the hook
  does the right thing in a new target version.
- **Report URL size:** GitHub caps URLs at about 8 KB. Long reports fall back to the clipboard.
- **GitHub API limit:** 60 requests per hour without a token. The one-day cache keeps the app far
  below that.
- **AppCompat:** adds about 300 KB to release builds after R8.
- **Translation quality:** these are machine translations, so expect corrections through Weblate.

---

## Phase I: Testing feedback, batch 1 (2026-11-17, done)

Your feedback numbers are in brackets. One commit per step; build with the low-memory script
after each.

### I1. Home header and status card [1, 2, 3, 4, 5, 15]
- **Title icon [1]:** `TitleWithIcon` draws the bare foreground glyph (`ic_launcher_foreground`
  as a vector, no adaptive-icon circle), tinted `colorScheme.primary`. That is the wallpaper
  colour with Dynamic color on, otherwise the teal seed. Slightly smaller than the text height.
- **Not active badge [2]:** `Burst` → `MaterialShapes.Sunny` (short, rounded points). The cross
  becomes a heavier glyph (`Icons.Rounded.Close` drawn with a thicker stroke, or a custom 3 dp
  stroke vector). Light-mode colours go from `errorContainer` (pale on pale) to a stronger pair:
  `error` fill with `onError` cross, softened to about 85 % so it isn't harsh. Dark mode keeps
  the container pair, which already reads well. The other badges get the same contrast check.
- **Versions line [3]:** a small pill (`surfaceContainer`, full-round corners, 8 dp vertical
  padding) holding "Target · Module" with a small icon, centred horizontally under the card.
- **Always on [4]:** new `ModuleConfig.ALWAYS_ON` flag (default true in the template). When
  false the header and rows are skipped. The KDoc and README mark the section as optional.
- **Footnote [5]:** remove the "Changes take effect after … restarts." line and its string.
- **Debug status preview [15]:** debug builds only (`BuildConfig.DEBUG`). Long-press the status
  side of the card to step through Active → Pending → Warning → Off → Not active → Busy →
  live. A small "Preview" tag shows while overriding. Nothing is saved. Release builds
  compile it out.

### I2. Apply sheet [8]
- **Handle (i, ii):** a custom drag handle with no ripple (`indication = null`). Tapping it
  toggles between **peek** (summary and buttons) and **expanded** (full change list). If the
  sheet is already expanded, a tap returns it to peek.
- **Resists collapsing (iii):** likely cause: the inner `verticalScroll` takes the drag while
  the list sits at its top. Fix: put the list's scroll inside the sheet's nested-scroll
  connection, and only let the inner list take a drag when it can still scroll that way.
  Also re-check `confirmValueChange` so it never blocks PartiallyExpanded. (Can't reproduce it
  reliably, so this is a best-effort fix.)
- **Buttons (iv):** medium → small-medium height (`ButtonDefaults.MinHeight` + 8 dp) and
  tighter content padding.
- **Separator (v):** replace the full-width divider with an inset, rounded 1 dp line in
  `outlineVariant`, shown only while the list is expanded.
- **Background (vi):** sheet container goes to `surfaceContainerHigh` with tonal elevation and
  a soft top shadow, so it separates from the app's `surface`.
- **Gap (vii):** 8 dp → 16 dp between the value and the ✕ button.
- **Animation (viii):** rows leave with `animateItem` (fade and shrink) after the swipe ends.
  The sheet hides with the expressive motion spec instead of snapping.
- **Bug (ix):** the swipe state is remembered per key and survives while the row is
  gone (the sheet keeps the last content while hiding). So a redone change comes back already
  "dismissed", and only the background (two undo arrows) shows. Fix: key rows by key plus a
  revision counter, and reset the state to Settled when a row re-enters.

### I3. Options sheet and demo [6, 7, 12, 13, 14]
- **Sticky header border [6]:** no fixed divider. A hairline plus a slight tonal shift fades
  in only once the options are scrolled. The header has the same background as the sheet.
- **Glitchy scrolling [7]:** the options column gets `weight(1f, fill = false)` so the sheet
  height is bounded. The nested scroll is set up so the list scrolls before the sheet drags
  down. 20 filler rows instead of 6.
- **Show for dialog [12]:** a bigger value display ("4 s", with the default marked), a slider
  with tick labels, and −/+ buttons for 1-step nudges. "Default" sets the slider to the
  default and keeps the dialog open; only OK commits. Button order: Default (left), Cancel,
  OK.
- **Colour picker [13]:** remove the Area tab and `SatValueArea`. Two segments are left:
  Swatches and Wheel.
- **Choose apps [14]:** new `AppMultiPickerDialog(title, selected: Set<String>, onDone, onDismiss)`
  with checkboxes, a search field, and Done/Cancel buttons. The order is frozen when the
  dialog opens: checked apps first, then Installed, then System. Checking or unchecking only
  flips the box, and the list doesn't re-sort until the dialog is opened again. New demo row
  "Choose apps", with the summary "3 apps: A, B, +1".

### I4. Reset settings [11]
- **Per section:** a ⋮ menu in each category screen's top bar with "Reset to defaults"
  (disabled when nothing differs from the defaults).
- **Global:** a "Reset all settings" row in About → Data (see I5), next to Back up and Restore.
  It offers the same Settings/Appearance section checkboxes as Restore.
- **Both:** a confirm dialog that lists what changes (reusing the restore diff). Reset values
  become **pending** changes, like Restore, so the apply sheet can still undo them.
  View model: `resetCategory(category)` and `resetAll(sections)`.
- **Optional:** `ModuleConfig.RESET` flag (default on).

### I5. About restructure [10]
New order, with a header for each group:
1. **Identity card** (unchanged).
2. **Updates:** Version (update check), What's new, Changelog.
3. **Help:** Troubleshooting rows (Feature health, Debug logging, Report a problem).
4. **Data:** Back up, Restore, Reset all settings.
5. **Project:** Source code, Request a feature, Translate.
6. **Author:** GitHub profile, More modules.
7. **Legal:** Open-source licences.

Optional groups (Help and Data) drop out with their flags.

### I6. Language dialog [9]
- System default at the top, then a divider.
- **Available:** English, plus any other translated languages, sorted by their own names.
- **Not translated yet:** a subheader, then the greyed-out languages sorted by their own
  names. Each row shows the English name as a subtitle ("Deutsch · German"), so they're easy to
  find.

---

## Phase J: Testing feedback, batch 2 (2026-11-17)

Your feedback numbers are in brackets. One commit per step, built with the low-memory script.

### J1. Home header and status badges [1, 2, 3, 4]
- **Shape swap [1]:** Warning (Not loaded yet, Partly active) gets `Sunny`; Not active gets
  `SoftBurst`.
- **Turned off badge [2]:** its fill is `surfaceContainerHighest`, which sits on the card's
  `surfaceContainerHigh`, so the pill almost vanishes. New fill: `outlineVariant`, with an
  `onSurface` pause icon. That reads in light and dark, and stays neutral (not an error colour).
- **Pill gap [3]:** padding under the versions pill drops from 12 dp to 4 dp. The section
  header's own top padding gives the remaining space.
- **Blurry title icon [4]:** cause: the vector is drawn at the small size and then *scaled up*
  with `graphicsLayer`, which enlarges a bitmap. Fix: draw it at the full canvas size
  (size × 108/66) and clip it to the visible box, so it is rasterised sharp. Icon-to-name
  gap 10 → 6 dp.

### J2. Demo screen and options sheet [5, 6]
- **Preview row colour [5]:** `PreviewBlock` uses `surfaceContainer`, while rows use
  `surfaceContainerHigh`. Give it a `color` parameter that defaults to the row colour, so the
  preview matches the rows in the sheet and on the page.
- **Try it out behind Gboard [6]:** the demo column gets `imePadding()`. The card asks to be
  brought into view (`BringIntoViewRequester`) when its field gains focus or the text changes,
  so the result line stays above the keyboard. The scaffold's top bar collapse still works.

### J3. Apply sheet rows and motion [7, 8, 9, 10]
- **Swipe background [7, 8]:**
  - Under the removal point: `surfaceContainerHighest` darkened a step (`secondaryContainer`
    if that still blends), with an outlined bin in `onSurfaceVariant`.
  - Past the point: haptic tick, filled bin with a 1.0→1.2 pop, and the background fades to
    `errorContainer` with an `onErrorContainer` icon.
  - Dragging back reverses all of it.
- **Predictable swipe [8]:** positional threshold = 40 % of the row width. A flick only
  counts once the row has moved at least 25 % too, so short fast flicks no longer remove a
  row. The "slow long drag stays" case gets checked on the device: the sheet's own drag may be
  taking the gesture. If so, the row consumes horizontal drags first.
- **Slower motion [9]:** the row collapse after removal goes to about 350 ms (from the
  default spatial spring). The sheet's hide animation uses a slightly softer spring (lower
  stiffness) via a sheet-only motion scheme.
- **Separator [10]:** `onSurface` at 12 % alpha instead of `outlineVariant`, 1 dp and inset.

### J4. Data and Help become their own screens [11, 12]
- **Home's App group [11]:** Appearance, Data, Help, About.
  - **Data:** Back up, Restore, Reset all settings. Shown when BACKUP_RESTORE or RESET is
    on. Summary: "Back up, restore or reset".
  - **Help:** Feature health, Debug logging, Report a problem. Summary: "Feature health,
    logs, report a problem".
  - New destinations `Destination.Data` and `Destination.Help`, built on `DetailScaffold`.
    The groups lose their own section headers, since the screen title already says it. About
    keeps Updates, Project, Author and Legal.
- **What's new [12]:** the subtext becomes "Changes in v1.0.0" (`about_whats_new_summary` =
  "Changes in v%1$s").

### J5. Apply sheet only where settings change [13]
- Navigation passes the sheet content only when the top destination is Home, a Category,
  Search or Data. Anywhere else it passes null, so the sheet slides away and slides back on
  return.
- The Home status card still says Pending meanwhile.
