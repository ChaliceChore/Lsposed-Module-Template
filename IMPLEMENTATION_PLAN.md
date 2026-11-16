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
