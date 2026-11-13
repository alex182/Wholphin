# Wholphin Accessible — Visual Accessibility Roadmap

A fork of [damontecres/Wholphin](https://github.com/damontecres/Wholphin) (Android TV client for Jellyfin) focused on making the app usable for people with low vision, colour-vision deficiency, light/motion sensitivity, and screen-reader users.

This file breaks the roadmap into tickets. Each ticket is sized to be roughly one PR.

## Status

Updated as tickets land on the `accessible` branch. "Checked on emulator" means it was tried on the Android TV emulator container against a test Jellyfin server.

| Ticket | Status | Notes |
|---|---|---|
| A11Y-001 | Done | Stored in `WholphinAccessibility.proto`, field 1000 on `AppPreferences`. Every proto default is upstream behaviour, so no migration is needed. |
| A11Y-002 | Done | `LocalAccessibility` in `ui/theme/Accessibility.kt`. |
| A11Y-003 | Partly done | System font scale and animation scale are followed by default. TalkBack detection is still to do (needed by A11Y-106). |
| A11Y-004 | Done | Settings > Accessibility, at the top of Settings. Checked on emulator. |
| A11Y-101, 102 | Done | One app-wide overlay (`FocusRingOverlay`) outlines whatever has focus, including inside dialogs, instead of a modifier on every component. Thickness and colour settings. Skips anything covering half the screen or more (video, loading pages). Checked on emulator (home, settings, dialogs, details, player); not yet checked on every screen. |
| A11Y-201 | Done | Text size: follow system or 85-200%. Checked on emulator at 150%. Combined with 125% zoom, the details line under the title is cut short. |
| A11Y-202 | Done, differently | Implemented as **Interface zoom** (100/110/125/140%), which enlarges cards, buttons, spacing and text together. Page headers shrink when zoomed so a full row of cards fits. Checked on emulator at 125% (home, movie details, player). 110% and 140% not yet checked. |
| A11Y-208 (new) | Done | **Plain text titles**: every media title uses the one standard title font instead of logo artwork (headers, player, screensaver). **On by default** by the fork owner's decision; this is the one setting whose default differs from upstream. Checked on emulator. |
| A11Y-301 | Done, differently | A separate **High contrast** setting (white on black, yellow on black, black on white) that overrides the application theme, rather than new entries in the theme list. All three checked on emulator (home, settings, details, player). Known gap: sliders show no difference between the filled and unfilled part of the track. |
| A11Y-302 | Done | **Dim background image** slider (0-90%). Checked on emulator. |
| A11Y-303 | Already upstream | Backdrop style "None" exists; it is now also listed under Accessibility. |
| A11Y-401 | Done, main switch only | **Reduce motion** stops all animations app-wide. Per-animation switches are not done. Checked on emulator: focus zoom and backdrop change are immediate. |
| A11Y-402 | Partly done | **Keep background still** added. Theme music setting is repeated under Accessibility. Not yet checked on emulator. |
| A11Y-701 | Done | Subtitle **Preset**: Classic, High visibility, Low vision, Minimal. Not yet checked on emulator. |
| A11Y-1001 | Done | `TestThemeContrast`: high-contrast themes must meet AAA; regular themes are reported. |
| A11Y-901 | Done | **Accessibility setup** screen shows once on first launch, before server setup. Offers text size, zoom, high contrast, focus outline, plain titles and reduce motion; each applies immediately. "Show accessibility setup" under Settings > Accessibility brings it back. People upgrading from an earlier build of the fork also see it once. Checked on emulator. |
| A11Y-1004 | Mostly done | Docker dev and emulator containers are in `docker/`. App is now "Wholphin Accessible" with id `com.github.alex182.wholphin.accessible`, so it installs beside upstream. Fork workflow is described in CONTRIBUTING.md. A regular upstream merge has not been tried yet. |

Everything else is not started.

**Default check:** with every accessibility setting at its default, the home page on the emulator matches upstream (logo title, purple theme, same layout). This is a by-eye check plus unit tests; the automated screenshot comparison (A11Y-1002) is not built yet.

**How to try it:** `docker/dev.sh ./gradlew assembleDefaultDebug` builds the APK in the container. `docker/emu.sh` drives the emulator container (`install`, `login`, `start`, `a11y`, `key`, `shot`); screenshots go to `build/a11y-shots/`.

## Conventions

- **ID:** `A11Y-<epic><nn>` (e.g. `A11Y-102` = Epic 1, ticket 02)
- **Priority:** `P0` (milestone 1, must have) · `P1` (should have) · `P2` (nice to have)
- **Size:** `S` (≤ 1 day) · `M` (2–4 days) · `L` (1–2 weeks)
- **Paths** are relative to `app/src/main/java/com/github/damontecres/wholphin/` unless they start with `app/`.
- **Settings** are stored in `app/src/main/proto/WholphinDataStore.proto`. New settings go in a new `AccessibilityPreferences` message (A11Y-001), not scattered across existing messages, so upstream rebases stay painless.
- **Everything is configurable (project rule).** Every user-facing change in this fork must have a setting that turns it on or off (or picks a level) under **Settings → Accessibility**. This applies to every ticket and is part of its Definition of Done, whether or not the ticket lists it:
  - The setting is stored in `AccessibilityPreferences` and appears in the Accessibility settings section (A11Y-004).
  - Turning it off restores the upstream Wholphin behaviour exactly. With every accessibility setting off, the fork looks and behaves like upstream.
  - **One exception:** "Plain text titles" is on by default (fork owner's decision). Turning it off restores upstream's logo titles. The one-time setup screen is the other visible difference on first launch.
  - Changes apply immediately, without restarting the app.
  - Composite options (presets, "Simplified layout", "Reduce motion", the wizard) only set individual settings, which can each still be changed separately afterwards.
  - "Follow system" counts as a valid default where A11Y-003 applies.
  - **Exempt:** build and test infrastructure (Epic 10), plus screen-reader-only metadata (content descriptions, headings) that does nothing unless TalkBack is running. TalkBack's own on/off switch controls it. Anything that *speaks* or *announces* on its own still needs a setting.
- **Upstreamable:** tickets marked 🔼 are generic enough to offer back to upstream as PRs.

## Milestones

| Milestone | Goal | Tickets |
|---|---|---|
| **M0: Foundation** | Plumbing everything else depends on | A11Y-001 – 004 |
| **M1: Core visibility** | Biggest wins: see the focus, read the text, calm the screen | A11Y-101, 102, 103, 201, 202, 301, 302, 401, 402, 701, 901 |
| **M2: Screen reader** | Usable with TalkBack end to end | A11Y-501 – 507 |
| **M3: Polish** | Everything else | Remaining tickets |

---

## Epic 0 — Foundation

### A11Y-001 · Add `AccessibilityPreferences` proto message and plumbing
**Priority:** P0 · **Size:** M · **Depends on:** — · 🔼

Create one home for every new accessibility setting.

**Acceptance criteria**
- New `AccessibilityPreferences` message in `WholphinDataStore.proto`, referenced from `InterfacePreferences` (new field number, no renumbering).
- Defaults chosen so existing users see **no visual change** after upgrade.
- A migration in `preferences/AppPreferencesSerializer.kt` (or equivalent) fills defaults for existing installs.
- `UserPreferencesService` exposes the new prefs as a `Flow`.
- Unit test proving an old datastore file loads with the correct defaults.

**Files:** `app/src/main/proto/WholphinDataStore.proto`, `preferences/AppPreferencesSerializer.kt`, `preferences/UserPreferences.kt`, `services/UserPreferencesService.kt`

---

### A11Y-002 · `LocalAccessibility` CompositionLocal
**Priority:** P0 · **Size:** S · **Depends on:** A11Y-001

Give composables cheap access to accessibility settings without passing parameters through every layer.

**Acceptance criteria**
- `ui/theme/Accessibility.kt` defines an immutable `AccessibilitySettings` data class and a `LocalAccessibility` CompositionLocal.
- It is provided in `WholphinTheme` (`ui/theme/Theme.kt`) next to the existing theme-colour local.
- `ThemePreview.kt` supports previewing with non-default accessibility settings.

**Files:** `ui/theme/Theme.kt`, `ui/theme/ThemePreview.kt`, new `ui/theme/Accessibility.kt`

---

### A11Y-003 · Read system accessibility settings
**Priority:** P0 · **Size:** S · **Depends on:** A11Y-002 · 🔼

Respect what the user already set at the OS level.

**Acceptance criteria**
- Read system font scale (`Configuration.fontScale`), animator duration scale (`Settings.Global.ANIMATOR_DURATION_SCALE`), high-contrast text, and whether TalkBack is enabled (`AccessibilityManager`).
- Each in-app setting has a **"Follow system"** option, which is the default.
- Values update live when the system setting changes (no app restart).

**Files:** `ui/theme/Accessibility.kt`, `MainActivity.kt`

---

### A11Y-004 · "Accessibility" settings section
**Priority:** P0 · **Size:** M · **Depends on:** A11Y-001

A single place where users find every option in this roadmap.

**Acceptance criteria**
- New top-level "Accessibility" entry in Settings, near the top of the list.
- Subsections: Focus, Text & Layout, Colour & Contrast, Motion, Playback, Subtitles, Audio.
- Uses the existing preference composables (`SwitchPreference`, `ChoicePreference`, `SliderPreference`).
- All strings in `app/src/main/res/values/strings.xml` so they can be translated through the existing Codeberg Weblate setup.
- Later tickets add their settings here.

**Files:** `ui/preferences/PreferencesContent.kt`, `preferences/AppPreference.kt`, `app/src/main/res/values/strings.xml`

---

## Epic 1 — Focus visibility

### A11Y-101 · High-contrast focus ring modifier
**Priority:** P0 · **Size:** M · **Depends on:** A11Y-002 · 🔼

The most important change for low-vision D-pad users: make it obvious where focus is.

**Acceptance criteria**
- New `Modifier.a11yFocusIndicator()` in `ui/components/` that draws an outline with an inner + outer contrasting stroke (e.g. white inside black) so it shows up on any artwork.
- Thickness (2–8dp), colour (theme accent / white / yellow / custom) and shape follow the item's shape.
- The ring is drawn *outside* the content and never clipped by `LazyRow` / `LazyVerticalGrid` padding.
- Setting off = current upstream behaviour.

**Files:** new `ui/components/FocusIndicator.kt`, `ui/theme/Accessibility.kt`

---

### A11Y-102 · Apply the focus ring to every focusable surface
**Priority:** P0 · **Size:** L · **Depends on:** A11Y-101

**Acceptance criteria**
- Applied to all cards (`ui/cards/*`), buttons (`ui/components/Button.kt`, `PlayButtons.kt`), list items (`ListItemWrapper.kt`, `CollectionFolderList.kt`), tabs (`TabRow.kt`), nav drawer (`ui/nav/NavDrawer.kt`), preference rows, dialogs, playback controls (`ui/playback/overlay/*`), and the alphabet picker in `ui/detail/CardGrid.kt`.
- Places that set `focusedScale = 1f` (e.g. `PlaybackTrackInfo.kt`, `CollectionFolderList.kt`, `RemovedNextUpContent.kt`) still show a clear focus state when the ring is enabled.
- A manual checklist (in the PR) walks through every screen with a D-pad and confirms focus is always visible.

---

### A11Y-103 · Focus state must not depend on colour or scale alone
**Priority:** P0 · **Size:** M · **Depends on:** A11Y-102 · 🔼

**Acceptance criteria**
- Every focusable component has at least one non-colour, non-scale focus cue (outline, underline, or a brightness change ≥ 3:1 against unfocused).
- Text-only items (e.g. `TitleValueText.kt`, which only scales 1.1×) also get an underline or background fill.
- Setting "Extra focus cue": Off / Underline / Background fill / Brightness. Off = upstream behaviour.
- Verified with a greyscale screen filter: focus is still findable.

---

### A11Y-104 · Dim non-focused content (optional)
**Priority:** P2 · **Size:** S · **Depends on:** A11Y-002

**Acceptance criteria**
- Setting "Dim unfocused items" with strength 0–60%.
- Applies to cards inside rows and grids only (not text, headings or the nav drawer).
- Disabled automatically when reduce motion (A11Y-401) is on *if* it causes a transition; otherwise applied without animation.

---

### A11Y-105 · Larger focus targets and spacing
**Priority:** P1 · **Size:** M · **Depends on:** A11Y-202

**Acceptance criteria**
- Minimum focusable size of 48dp (at 1× scale) for all icon buttons.
- Setting "Item spacing": Compact / Normal / Spacious, which changes row and grid gaps.
- No overlapping focus rings at any spacing.

---

### A11Y-106 · Read focused item aloud without TalkBack
**Priority:** P2 · **Size:** M · **Depends on:** A11Y-501

For users with partial vision who don't want full TalkBack.

**Acceptance criteria**
- Setting "Speak focused item" uses Android `TextToSpeech` to read the item's title (and optionally a short description) after focus settles for ~400ms.
- Never active at the same time as TalkBack (A11Y-003 detects it).
- Speech rate and verbosity (title only / title + year / full) are configurable.
- Does not speak during playback.

---

## Epic 2 — Text and layout scaling

### A11Y-201 · Global UI text scale
**Priority:** P0 · **Size:** M · **Depends on:** A11Y-003 · 🔼

**Acceptance criteria**
- Setting "Text size": Follow system / 85% / 100% / 115% / 130% / 150% / 175% / 200%.
- Implemented by scaling the `Typography` in `ui/theme/Type.kt`, plus a `LocalDensity` `fontScale` override, so hard-coded `sp` values elsewhere scale too.
- Audit and remove any `TextUnit` sizes that bypass the theme where practical.
- No text is clipped at 200% on the Home, Details, Search, Settings and Playback screens (verified with A11Y-1002).

**Files:** `ui/theme/Type.kt`, `ui/theme/Theme.kt`

---

### A11Y-202 · Card size presets
**Priority:** P0 · **Size:** M · **Depends on:** A11Y-002

**Acceptance criteria**
- Setting "Card size": Small / Medium (current) / Large / Extra large.
- Applies to home rows, library grids, and search results; grid column counts shrink accordingly.
- Works for both poster and thumb image styles from the existing home settings.
- Card dimensions are centralised (e.g. `ui/UiConstants.kt` or a new `CardDimensions` object), not hard-coded per card.

**Files:** `ui/cards/*`, `ui/components/ItemGrid.kt`, `ui/components/CollectionFolderGrid.kt`, `ui/UiConstants.kt`

---

### A11Y-203 · Minimum font size floor
**Priority:** P1 · **Size:** S · **Depends on:** A11Y-201

**Acceptance criteria**
- Setting "Minimum text size" (off / 14sp / 16sp / 18sp / 20sp).
- Small text styles (`labelSmall`, `bodySmall`, etc.) are clamped to the floor after scaling.

---

### A11Y-204 · Always show titles under cards, with wrapping
**Priority:** P1 · **Size:** S · **Depends on:** —

**Acceptance criteria**
- Setting "Card titles": Hidden / One line (current) / Wrap up to 3 lines.
- Overrides the per-row show/hide title option when set to "Always".
- Wrapped titles keep cards in a row aligned (fixed title area height per row).

**Files:** `ui/cards/*`, `services/HomeSettingsService.kt`

---

### A11Y-205 · Simplified layout mode
**Priority:** P2 · **Size:** M · **Depends on:** A11Y-201, A11Y-202

**Acceptance criteria**
- One toggle "Simplified layout" that, at the same time: limits the home page to N rows (user-configurable, default 4), hides secondary metadata (studio, codec badges, ratings) on cards and the `QuickDetails` header, and uses larger titles.
- Each sub-option can still be changed on its own afterwards.

**Files:** `ui/components/QuickDetails.kt`, `ui/components/VideoStreamDetails.kt`, home page composables

---

### A11Y-206 · Hyperlegible / dyslexia-friendly font option
**Priority:** P1 · **Size:** M · **Depends on:** A11Y-002

**Acceptance criteria**
- Setting "App font": Default / Atkinson Hyperlegible / Lexend / OpenDyslexic.
- Fonts bundled under `app/src/main/res/font/` with licences (all three are OFL) added to the in-app licence screen (`ui/components/LicenseInfo.kt`).
- APK size increase noted in the PR; consider downloadable fonts if > 1 MB.

**Files:** `ui/theme/Type.kt`, `app/src/main/res/font/`

---

### A11Y-207 · Adjustable line height and letter spacing
**Priority:** P2 · **Size:** S · **Depends on:** A11Y-201

**Acceptance criteria**
- Settings for line height (1.0×–2.0×) and letter spacing (0–0.12em), applied to body text styles.
- Visible on the overview text in `ui/components/OverviewText.kt`.

---

## Epic 3 — Colour and contrast

### A11Y-208 · Plain text titles in one standard font
**Priority:** P0 · **Size:** S · **Depends on:** A11Y-002

Stylised logo artwork is hard to read for many low-vision users, and every title looks different.

**Acceptance criteria**
- Setting "Plain text titles" (default off). When on, every media title is shown as text in the one standard title style (`Title` in `ui/components/TitleOrLogo.kt`), never as logo artwork.
- Applies everywhere a logo can appear: page headers, detail pages, the list view, the player overlay and the screensaver.
- Overrides the existing upstream "prefer logos" setting when on.

---

### A11Y-301 · High-contrast themes
**Priority:** P0 · **Size:** M · **Depends on:** — · 🔼

**Acceptance criteria**
- Three new `AppThemeColors` values (appended, no renumbering): `HIGH_CONTRAST_DARK` (white on black), `HIGH_CONTRAST_YELLOW` (yellow on black), `HIGH_CONTRAST_LIGHT` (black on white).
- New files in `ui/theme/colors/` following the pattern of `OledThemeColors.kt`.
- Every text/background pair meets **WCAG AAA (7:1)**; non-text UI (borders, icons, focus) meets ≥ 3:1 (checked by A11Y-1001).
- Themes are listed in `getThemeColors()` in `ui/theme/Theme.kt` and in the theme picker.

---

### A11Y-302 · Text scrim behind content on backdrops
**Priority:** P0 · **Size:** S · **Depends on:** A11Y-002

**Acceptance criteria**
- Setting "Text background": None (current gradient) / Solid / Blurred.
- Solid uses the theme background at ≥ 85% opacity behind all text drawn over a backdrop or artwork (detail pages, `QuickDetails`, home header).
- With Solid enabled, all text over backdrops meets 4.5:1 regardless of the image.

**Files:** `ui/nav/Backdrop.kt`, `ui/components/QuickDetails.kt`, `ui/detail/*`

---

### A11Y-303 · "No backdrop images" option
**Priority:** P1 · **Size:** S · **Depends on:** —

**Acceptance criteria**
- Extends the existing `BackdropStyle` enum with `BACKDROP_NONE` (or confirm an existing equivalent and expose it under Accessibility).
- Background is a flat theme colour; no image loads are triggered (also saves bandwidth).

**Files:** `app/src/main/proto/WholphinDataStore.proto`, `ui/nav/Backdrop.kt`

---

### A11Y-304 · Colour-blind-safe themes
**Priority:** P1 · **Size:** M · **Depends on:** A11Y-301, A11Y-1001

**Acceptance criteria**
- At least two themes designed around a CVD-safe palette (e.g. blue/orange based). Document which deficiencies each targets.
- Status colours (watched, in progress, error, success, rating stars) stay distinguishable under simulated protanopia, deuteranopia and tritanopia; checked in A11Y-1001 with a simulation matrix.

---

### A11Y-305 · Custom accent colour with live contrast check
**Priority:** P2 · **Size:** M · **Depends on:** A11Y-301

**Acceptance criteria**
- Colour picker (preset swatches + hex entry via `StringInputDialog`) for the accent colour.
- Shows a live contrast ratio against the background with pass/fail badges (AA / AAA).
- Refuses to save, or warns, if the accent is below 3:1 against the background.

---

### A11Y-306 · Watched / progress indicators that don't rely on colour
**Priority:** P1 · **Size:** S · **Depends on:** — · 🔼

**Acceptance criteria**
- Setting "High-visibility status indicators" (on/off): the watched state uses a checkmark icon with a contrasting outline (review `ui/cards/WatchedIcon.kt`), progress bars get a contrasting track outline, and unwatched-episode count badges reach ≥ 4.5:1 contrast.
- Separate setting "Progress label": Off / Percent ("45%") / Time left ("32m left").

**Files:** `ui/cards/WatchedIcon.kt`, card progress bar composables

---

### A11Y-307 · Artwork dimming / desaturation
**Priority:** P2 · **Size:** S · **Depends on:** A11Y-002

**Acceptance criteria**
- Settings "Artwork brightness" (50–100%) and "Artwork saturation" (0–100%), applied via a `ColorFilter` to images in cards and backdrops.
- Not applied to video playback or the photo slideshow (`ui/slideshow/*`) unless the user opts in.

**Files:** `ui/cards/ItemCardImage.kt`, `ui/nav/Backdrop.kt`, `ui/CoilConfig.kt`

---

## Epic 4 — Motion and visual noise

### A11Y-401 · Reduce motion mode
**Priority:** P0 · **Size:** M · **Depends on:** A11Y-003 · 🔼

**Acceptance criteria**
- Setting "Reduce motion": Follow system / On / Off.
- When on: focus scale animations disabled (`focusedScale = 1f` everywhere via a shared helper), crossfades become instant cuts (`ui/CrossFadeFactory.kt`), row scrolling snaps instead of animating, and the nav drawer opens without sliding.
- Each of those four can also be toggled separately (Focus zoom / Crossfades / Smooth scrolling / Drawer animation). The main switch sets all four.
- Focus is still clearly visible (relies on A11Y-101).
- Following system: on when `ANIMATOR_DURATION_SCALE == 0`.

---

### A11Y-402 · Disable auto-playing media and changing backdrops
**Priority:** P0 · **Size:** S · **Depends on:** A11Y-001

**Acceptance criteria**
- Setting "Auto-play trailers / backdrop video" (if such auto-play exists; otherwise confirm and document that).
- Setting "Change backdrop as focus moves": On / Delayed (1.5s) / Off. With Off, the backdrop only changes on page navigation.
- Theme songs: point users to the existing `ThemeSongVolume` setting from the Accessibility section (no duplicate setting).

**Files:** `ui/nav/Backdrop.kt`, `services/ThemeSongPlayer.kt`

---

### A11Y-403 · Screensaver motion options
**Priority:** P2 · **Size:** S · **Depends on:** A11Y-401

**Acceptance criteria**
- With reduce motion on, the screensaver (`ui/components/AppScreensaver.kt`) uses a static or slow fade instead of panning or zooming.
- Option to replace the screensaver with a dark clock-only screen.

---

### A11Y-404 · Photosensitivity safeguards
**Priority:** P2 · **Size:** S · **Depends on:** —

**Acceptance criteria**
- Audit: no UI element flashes more than 3 times per second. Any element that does gets a "Reduce flashing" setting (default on) instead of being changed outright.
- Setting "Photosensitivity warning" (default off): if the server metadata includes a photosensitive-content tag or warning, show a dismissible warning before playback.

---

### A11Y-405 · Adjustable D-pad scroll and key-repeat speed
**Priority:** P2 · **Size:** M · **Depends on:** —

**Acceptance criteria**
- Setting "Navigation speed when holding a direction": Slow / Normal / Fast.
- Applies to rows, grids and lists. Seek acceleration in playback (`ui/playback/SeekAcceleration.kt`) has its own matching option.

---

## Epic 5 — Screen reader (TalkBack)

### A11Y-501 · Content descriptions audit
**Priority:** P1 · **Size:** L · **Depends on:** — · 🔼

About 50 places use `contentDescription = null`. Decide for each whether it's decorative (keep `null`) or meaningful (add a localised description).

**Acceptance criteria**
- Every image or icon that carries meaning has a localised description, at minimum in: `ui/cards/*` (GenreCard, StudioCard, BannerCard, ChapterCard, ItemCardImage, WatchedIcon), `ui/components/` (Rating, PlayButtons, FilterByButton, QuickDetails, Dialogs), `ui/nav/NavDrawer.kt`, `ui/playback/overlay/*` (PauseIndicator, SkipIndicator, PlaybackTrackInfo), `ui/playback/NextUpEpisode.kt`, `ui/detail/music/*`, `ui/detail/livetv/*`, `ui/slideshow/*`, `ui/preferences/*`.
- Decorative images remain `null` with a short code comment saying why.
- A lint rule or Detekt check flags new `contentDescription = null` without a comment (stretch goal).

---

### A11Y-502 · Merged semantics for cards
**Priority:** P1 · **Size:** M · **Depends on:** A11Y-501 · 🔼

**Acceptance criteria**
- Each card is a single TalkBack stop that reads e.g. *"The Matrix, 1999, movie, 2 hours 16 minutes, watched"* or *"Episode 3, The Bridge, 42 minutes, 60% watched"*.
- Implemented via `Modifier.semantics(mergeDescendants = true)` plus a built `contentDescription` helper in one place.
- Setting "Card announcement detail": Title only / Standard / Full (adds rating, genres, audio/subtitle info). Shares its value with A11Y-106's verbosity.

---

### A11Y-503 · Headings and traversal order
**Priority:** P1 · **Size:** M · **Depends on:** —

**Acceptance criteria**
- Row titles, page titles and settings section titles are marked `heading()` so TalkBack heading navigation works.
- Traversal order on detail pages is logical: title → metadata → play buttons → overview → seasons/episodes → cast.
- The nav drawer is announced as navigation and its open/closed state is announced.

---

### A11Y-504 · Accessible playback controls and state
**Priority:** P1 · **Size:** M · **Depends on:** A11Y-501

**Acceptance criteria**
- Seek bar (`ui/playback/overlay/SeekBar.kt`) exposes `progressBarRangeInfo` and a readable value ("12 minutes 30 seconds of 1 hour 42 minutes").
- Play/pause, skip-segment, next-up and track buttons have role and state ("Pause, button", "Subtitles: English SDH").
- Setting "Announce track changes" (default on when TalkBack is running): changing the audio or subtitle track announces the new choice.

---

### A11Y-505 · Live-region announcements for loading, errors and toasts
**Priority:** P1 · **Size:** S · **Depends on:** —

**Acceptance criteria**
- Setting "Status announcements": Off / Errors only / All (default All when TalkBack is running).
- Loading indicators (`CircularProgress.kt`, `LoadingRow.kt`) announce "Loading" once and "Loaded" or the result count when done.
- `ErrorMessage.kt` uses `liveRegion = Assertive`.
- Toasts/snackbars are announced politely.

---

### A11Y-506 · Accessible forms, dialogs and the server setup flow
**Priority:** P1 · **Size:** M · **Depends on:** A11Y-501

**Acceptance criteria**
- Text fields (`EditTextBox.kt`, `StringInputDialog.kt`) have labels and announce errors.
- Dialogs announce their title when opened and trap focus correctly.
- The first-run server/login flow (`ui/setup/*`, `QuickConnectDialog.kt`, `SetPinFlow.kt`) can be completed with TalkBack only.

---

### A11Y-507 · TalkBack end-to-end pass
**Priority:** P1 · **Size:** M · **Depends on:** A11Y-501 – 506

**Acceptance criteria**
- Documented manual test script: set up server → browse home → open a series → play an episode → change subtitles → search → change a setting, done fully with TalkBack.
- All blocking issues fixed; minor ones filed as follow-up tickets.

---

## Epic 6 — Playback overlay

### A11Y-601 · Larger, high-contrast playback overlay
**Priority:** P1 · **Size:** M · **Depends on:** A11Y-002

**Acceptance criteria**
- Setting "Player controls size": Normal / Large / Extra large, which scales buttons, the seek bar height and time labels.
- Seek bar track/progress/thumb contrast ≥ 3:1 against the overlay background.
- Uses theme colours so high-contrast themes (A11Y-301) apply in the player.

**Files:** `ui/playback/overlay/PlaybackControls.kt`, `PlaybackOverlay.kt`, `SeekBar.kt`, `ui/components/TimeDisplay.kt`

---

### A11Y-602 · Solid overlay background
**Priority:** P1 · **Size:** S · **Depends on:** A11Y-302

**Acceptance criteria**
- Setting "Player controls background": Gradient (current) / Solid. Solid uses ≥ 85% opacity behind the controls area.

---

### A11Y-603 · Large clock and time remaining
**Priority:** P2 · **Size:** S · **Depends on:** A11Y-601

**Acceptance criteria**
- Option to show "Ends at 10:42 PM" and time remaining in a large style in the overlay.
- Respects 12/24-hour system setting.

---

### A11Y-604 · Bigger trickplay preview with timestamp
**Priority:** P2 · **Size:** S · **Depends on:** A11Y-601

**Acceptance criteria**
- Trickplay/seek preview (`TrickplayPreview.kt`, `SeekPreviewImage.kt`) scales with the controls-size setting.
- Setting "Trickplay preview size": Normal / Match controls size / Large.
- Setting "Show timestamp under preview" (on/off), with a solid background.

---

### A11Y-605 · Adjustable overlay timeout
**Priority:** P1 · **Size:** S · **Depends on:** —

**Acceptance criteria**
- Setting "Hide player controls after": 3s / 5s (default) / 10s / 30s / Never (until Back is pressed).
- Separate setting "Skip button timeout" (same choices) for the skip-intro/credits button (`SkipSegmentButton.kt`), so it doesn't vanish before the user can find it.

---

## Epic 7 — Subtitles and captions

Subtitle styling already has a lot of options (`SubtitlePreferences` in the proto and `ui/preferences/subtitle/SubtitleStylePage.kt`). These tickets build on that.

### A11Y-701 · Subtitle presets
**Priority:** P0 · **Size:** S · **Depends on:** —  · 🔼

**Acceptance criteria**
- One-click presets at the top of the subtitle settings page: **High visibility** (large, yellow, black box), **Low vision** (extra large, white, bold, solid black box), **Classic** (current default), **Minimal** (white, shadow edge).
- Choosing a preset sets the existing `SubtitlePreferences` fields; the user can still tweak each one afterwards.
- Works for both SDR and the separate HDR subtitle settings.

**Files:** `ui/preferences/subtitle/SubtitleStylePage.kt`

---

### A11Y-702 · Live subtitle preview
**Priority:** P1 · **Size:** M · **Depends on:** —

**Acceptance criteria**
- The subtitle style page shows sample subtitle text over a representative frame (bright and dark sample images, switchable) and updates instantly as settings change.

---

### A11Y-703 · Subtitle font family
**Priority:** P1 · **Size:** M · **Depends on:** A11Y-206

**Acceptance criteria**
- New `font_family` field in `SubtitlePreferences`.
- Options: Default / Atkinson Hyperlegible / Lexend / OpenDyslexic / Monospace.
- Works with both ExoPlayer (Media3 `CaptionStyleCompat` / `util/Media3SubtitleOverride.kt`) and MPV (`sub-font` option); document any backend that can't support it.

---

### A11Y-704 · Larger subtitle size range and resolution-aware scaling
**Priority:** P1 · **Size:** S · **Depends on:** —

**Acceptance criteria**
- Raise the maximum font size (see `subtitleMaxSize`) so subtitles can fill up to ~15% of screen height.
- Size is relative to the screen, so 1080p and 4K look the same.

---

### A11Y-705 · Prefer SDH / CC tracks
**Priority:** P1 · **Size:** S · **Depends on:** — · 🔼

**Acceptance criteria**
- Setting "Prefer SDH / hearing-impaired subtitles", used during automatic track selection (`ui/playback/TrackSelectionUtils.kt`).
- Falls back to the normal language preference if no SDH track exists.

---

### A11Y-706 · Subtitle position control
**Priority:** P2 · **Size:** M · **Depends on:** —

**Acceptance criteria**
- Setting "Subtitle position": Bottom (current) / Top, alongside the existing `margin` field.
- Optional quick toggle in the playback overlay to flip position when subtitles cover burned-in text.

---

### A11Y-707 · Force user style over ASS/SSA styling
**Priority:** P2 · **Size:** M · **Depends on:** —

**Acceptance criteria**
- Extend `AssPlaybackMode` with an option that renders ASS/SSA text with the user's subtitle style (strips fonts/colours/positions, keeps text).
- MPV: map to `sub-ass-override=force`; ExoPlayer: equivalent override. Document per-backend limits.

---

### A11Y-708 · Emphasise sound-effect cues
**Priority:** P2 · **Size:** S · **Depends on:** —

**Acceptance criteria**
- Setting "Sound-effect cue style": Off / Italic / Colour (with colour picker). Applies to bracketed cues (`[door slams]`, `♪ music ♪`) — e.g. italic or a different colour — where the backend allows text processing.

---

## Epic 8 — Audio as a visual aid

### A11Y-801 · Prefer audio description tracks
**Priority:** P1 · **Size:** S · **Depends on:** — · 🔼

**Acceptance criteria**
- Setting "Prefer audio description tracks", used in automatic audio track selection.
- AD tracks are clearly labelled in the track picker (`PlaybackTrackInfo.kt`), e.g. "English — Audio Description".

---

### A11Y-802 · Large, high-contrast Now Playing screen for music
**Priority:** P2 · **Size:** M · **Depends on:** A11Y-201, A11Y-301

**Acceptance criteria**
- Setting "Large Now Playing screen" (on/off): `ui/detail/music/NowPlayingPage.kt` uses enlarged artwork, title and controls.
- It always follows the global text scale, theme and A11Y-302 scrim settings.
- Setting "Show lyrics" (on/off), with its own "Lyrics size" setting, when lyrics are available.

---

## Epic 9 — Setup and discoverability

### A11Y-901 · First-run accessibility wizard
**Priority:** P0 · **Size:** M · **Depends on:** A11Y-201, A11Y-301, A11Y-401, A11Y-101

**Acceptance criteria**
- Shown once on first launch (before or after server setup), skippable.
- Starts with large, high-contrast text by default so it's readable by the people who need it.
- Asks: larger text? high contrast? reduce motion? stronger focus outline? subtitle preset? Each choice previews immediately.
- Setting "Show accessibility setup on first launch" (default on), so device installers/admins can turn it off.
- The wizard only changes the individual settings from this roadmap; it adds no behaviour of its own.
- Can be re-run from Settings → Accessibility.

**Files:** `ui/setup/*`

---

### A11Y-902 · Quick accessibility toggle
**Priority:** P2 · **Size:** S · **Depends on:** A11Y-201, A11Y-301

**Acceptance criteria**
- Setting "Quick toggle shortcut": Off (default) / Long-press Menu / other key choices. When enabled, the shortcut opens a small quick-settings panel: text size, high contrast, reduce motion.
- Handled alongside existing key handling; must not conflict with playback keys in `PlaybackKeyHandler.kt`.

---

### A11Y-903 · Per-profile accessibility settings
**Priority:** P1 · **Size:** M · **Depends on:** A11Y-001

**Acceptance criteria**
- Accessibility settings are saved per user profile (`preferences/UserProfileSettings.kt`) so a shared TV works for different household members.
- Switching user applies their settings without restart.
- Optional "Apply to all profiles" button.

---

## Epic 10 — Engineering and quality

### A11Y-1001 · Automated contrast tests for themes
**Priority:** P0 · **Size:** S · **Depends on:** — · 🔼

**Acceptance criteria**
- Unit test in `app/src/test/` iterates every `ThemeColors` and asserts minimum contrast for key pairs (on-background/background, on-primary/primary, focus/background, etc.).
- AA for existing themes is reported (not failing) at first; AAA is required for the high-contrast themes.
- Includes a CVD simulation matrix used by A11Y-304.

---

### A11Y-1002 · Large-font and large-layout UI tests
**Priority:** P1 · **Size:** M · **Depends on:** A11Y-201, A11Y-202

**Acceptance criteria**
- Compose UI or screenshot tests (e.g. Paparazzi or Roborazzi) render key screens at 100% / 150% / 200% text scale and the largest card size.
- Fails on text truncation or overlap where detectable; screenshots are stored for review.
- **"All off" test:** with every accessibility setting off, screenshots of key screens match upstream Wholphin's, which enforces the project rule that everything is configurable.
- A unit test checks that every field in `AccessibilityPreferences` has a matching entry in the Accessibility settings screen.

---

### A11Y-1003 · Accessibility checks in CI
**Priority:** P1 · **Size:** M · **Depends on:** A11Y-1002

**Acceptance criteria**
- Enable Android accessibility test framework checks (`AccessibilityChecks.enable()` / the Compose equivalent) in instrumented tests.
- CI workflow (`.github/workflows/`) runs contrast tests, screenshot tests and accessibility checks on every PR.

---

### A11Y-1004 · Upstream sync strategy
**Priority:** P0 · **Size:** S · **Depends on:** —

**Acceptance criteria**
- `CONTRIBUTING.md` section describing: branch layout (`main` tracks upstream, `accessible` holds the fork), the rule that new prefs go only in `AccessibilityPreferences`, and the regular rebase/merge cadence.
- Fork's `applicationId` and app name changed so it can be installed beside upstream Wholphin.
- Seerr/flavour build config reviewed so the fork builds cleanly.

---

### A11Y-1005 · Offer upstreamable changes back
**Priority:** P2 · **Size:** S (ongoing) · **Depends on:** tickets marked 🔼

**Acceptance criteria**
- For each 🔼 ticket, open a discussion or issue upstream before the PR to check the maintainer is interested.
- Track status in a table in this README (proposed / accepted / declined / merged).

---

## Ticket index

| ID | Title | Pri | Size |
|---|---|---|---|
| A11Y-001 | `AccessibilityPreferences` proto + plumbing | P0 | M |
| A11Y-002 | `LocalAccessibility` CompositionLocal | P0 | S |
| A11Y-003 | Read system accessibility settings | P0 | S |
| A11Y-004 | Accessibility settings section | P0 | M |
| A11Y-101 | High-contrast focus ring modifier | P0 | M |
| A11Y-102 | Apply focus ring everywhere | P0 | L |
| A11Y-103 | Focus not by colour/scale alone | P0 | M |
| A11Y-104 | Dim unfocused content | P2 | S |
| A11Y-105 | Larger focus targets and spacing | P1 | M |
| A11Y-106 | Speak focused item (no TalkBack) | P2 | M |
| A11Y-201 | Global UI text scale | P0 | M |
| A11Y-202 | Card size presets | P0 | M |
| A11Y-203 | Minimum font size | P1 | S |
| A11Y-204 | Always show / wrap card titles | P1 | S |
| A11Y-205 | Simplified layout mode | P2 | M |
| A11Y-206 | Hyperlegible font option | P1 | M |
| A11Y-207 | Line height / letter spacing | P2 | S |
| A11Y-208 | Plain text titles | P0 | S |
| A11Y-301 | High-contrast themes | P0 | M |
| A11Y-302 | Text scrim on backdrops | P0 | S |
| A11Y-303 | No backdrop images | P1 | S |
| A11Y-304 | Colour-blind-safe themes | P1 | M |
| A11Y-305 | Custom accent + contrast check | P2 | M |
| A11Y-306 | Non-colour watched/progress indicators | P1 | S |
| A11Y-307 | Artwork dimming/desaturation | P2 | S |
| A11Y-401 | Reduce motion mode | P0 | M |
| A11Y-402 | Disable auto-play / changing backdrops | P0 | S |
| A11Y-403 | Screensaver motion options | P2 | S |
| A11Y-404 | Photosensitivity safeguards | P2 | S |
| A11Y-405 | D-pad scroll/repeat speed | P2 | M |
| A11Y-501 | Content descriptions audit | P1 | L |
| A11Y-502 | Merged card semantics | P1 | M |
| A11Y-503 | Headings and traversal order | P1 | M |
| A11Y-504 | Accessible playback controls | P1 | M |
| A11Y-505 | Live-region announcements | P1 | S |
| A11Y-506 | Accessible forms/dialogs/setup | P1 | M |
| A11Y-507 | TalkBack end-to-end pass | P1 | M |
| A11Y-601 | Larger, high-contrast player overlay | P1 | M |
| A11Y-602 | Solid overlay background | P1 | S |
| A11Y-603 | Large clock / time remaining | P2 | S |
| A11Y-604 | Bigger trickplay preview | P2 | S |
| A11Y-605 | Adjustable overlay timeout | P1 | S |
| A11Y-701 | Subtitle presets | P0 | S |
| A11Y-702 | Live subtitle preview | P1 | M |
| A11Y-703 | Subtitle font family | P1 | M |
| A11Y-704 | Larger subtitle size range | P1 | S |
| A11Y-705 | Prefer SDH/CC tracks | P1 | S |
| A11Y-706 | Subtitle position control | P2 | M |
| A11Y-707 | Force user style over ASS/SSA | P2 | M |
| A11Y-708 | Emphasise sound-effect cues | P2 | S |
| A11Y-801 | Prefer audio description tracks | P1 | S |
| A11Y-802 | Accessible Now Playing (music) | P2 | M |
| A11Y-901 | First-run accessibility wizard | P0 | M |
| A11Y-902 | Quick accessibility toggle | P2 | S |
| A11Y-903 | Per-profile accessibility settings | P1 | M |
| A11Y-1001 | Automated theme contrast tests | P0 | S |
| A11Y-1002 | Large-font UI/screenshot tests | P1 | M |
| A11Y-1003 | Accessibility checks in CI | P1 | M |
| A11Y-1004 | Upstream sync strategy | P0 | S |
| A11Y-1005 | Offer changes upstream | P2 | S |
