# LSPosed Module Template

A shared base for LSPosed (libxposed API 102) modules that have a settings app. It includes a
Material 3 Expressive settings UI, a hook entry point, and the build setup.

The UI layout is adapted from [hxreborn/playstore-adblock](https://github.com/hxreborn/playstore-adblock)
(GPL-3.0). This template is GPL-3.0 for that reason.

## What's in it

| Part | Where |
|---|---|
| Module identity (id, name, target package, repo, version) | `gradle.properties` |
| Hook entry point | `app/src/main/kotlin/.../xposed/ModuleMain.kt` |
| Settings model shared by app and hook | `prefs/FeatureSettings.kt` |
| Labels and icons for settings | `ui/Catalog.kt` |
| Screens: Home, Category, Appearance, About, Licenses | `ui/screen/` |
| Shared components: rows, status card, apply sheet, morphing logo | `ui/component/` |
| Theme, spacing tokens, Google Sans Flex font | `ui/theme/Theme.kt` |

### Layout

- **Home**: a large collapsing title, a status card (framework connected or not, apply progress),
  then three sections: *Always on* (read-only rows), *Features* (one row per category) and *App*
  (Appearance, About).
- **Category**: switch rows, plus an optional single-choice row that opens a radio dialog.
- **Apply sheet**: a bottom sheet that peeks while changes aren't applied yet. Drag it up to see
  the list of changes. *Apply and restart* writes the settings to remote preferences and then
  restarts the target app. If something fails, the sheet expands.
- **Appearance**: System, Light or Dark theme, dynamic color, and a switch to hide the launcher icon.
- **About**: an identity card (morphing logo, version and license chips, two buttons), link rows
  and open source licenses.

## Start a new module from it

1. Copy the template. Edit `gradle.properties`: `module.*`, `target.*` and `version.*`.
2. In `settings.gradle.kts`, change `rootProject.name`. Rename the Kotlin package and
   `namespace` in `app/build.gradle.kts`. Update `java_init.list` and `proguard-rules.pro`.
3. Change `<queries>` in `AndroidManifest.xml` to the target package.
4. Replace the placeholder settings in `prefs/FeatureSettings.kt`, `ui/Catalog.kt` and
   `strings.xml`.
5. Write the hooks in `ModuleMain.onPackageReady`.
6. Pick a restart strategy in `ModuleApp.targetRestarter`. The default is a root `am force-stop`.
7. Replace the launcher icon in `res/drawable/ic_launcher_foreground.xml` and
   `values/ic_launcher_background.xml`.

Search for `TEMPLATE:` to find every place that needs a change.

## Build (Termux)

```bash
bash ./gradlew assembleDebug
```

`local.properties` (gitignored) sets `sdk.dir` and `buildRoot`. See `GITHUB_RELEASE.md` in the
modules folder for release signing and CI.
