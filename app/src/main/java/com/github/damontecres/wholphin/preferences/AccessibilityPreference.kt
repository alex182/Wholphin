package com.github.damontecres.wholphin.preferences

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.ui.nav.Destination
import com.github.damontecres.wholphin.ui.preferences.ConditionalPreferences
import com.github.damontecres.wholphin.ui.preferences.PreferenceGroup
import com.github.damontecres.wholphin.ui.preferences.PreferenceScreenOption

private fun a11ySwitch(
    @StringRes title: Int,
    @StringRes summary: Int? = null,
    defaultValue: Boolean = false,
    get: (AccessibilityPreferences) -> Boolean,
    set: AccessibilityPreferences.Builder.(Boolean) -> Unit,
) = AppSwitchPreference<AppPreferences>(
    title = title,
    defaultValue = defaultValue,
    getter = { get(it.accessibilityPreferences) },
    setter = { prefs, value -> prefs.updateAccessibilityPreferences { set(value) } },
    summary = summary,
)

/** A choice between fixed values. The first value is the default. */
private fun <T> a11yChoice(
    @StringRes title: Int,
    @ArrayRes displayValues: Int,
    values: List<T>,
    get: (AccessibilityPreferences) -> T,
    set: AccessibilityPreferences.Builder.(T) -> Unit,
) = AppChoicePreference<AppPreferences, T>(
    title = title,
    defaultValue = values.first(),
    displayValues = displayValues,
    getter = { get(it.accessibilityPreferences).takeIf { v -> v in values } ?: values.first() },
    setter = { prefs, value -> prefs.updateAccessibilityPreferences { set(value) } },
    indexToValue = { values.getOrElse(it) { values.first() } },
    valueToIndex = { values.indexOf(it).coerceAtLeast(0) },
)

private fun a11yPercentSlider(
    @StringRes title: Int,
    max: Long,
    interval: Int = 10,
    get: (AccessibilityPreferences) -> Int,
    set: AccessibilityPreferences.Builder.(Int) -> Unit,
) = AppSliderPreference<AppPreferences>(
    title = title,
    defaultValue = 0,
    min = 0,
    max = max,
    interval = interval,
    getter = { get(it.accessibilityPreferences).toLong() },
    setter = { prefs, value -> prefs.updateAccessibilityPreferences { set(value.toInt()) } },
    summarizer = { value -> value?.let { "$it%" } },
)

/**
 * Settings added by the accessibility fork. All of them live in [AccessibilityPreferences].
 *
 * Every setting's default must match upstream behaviour (see WholphinAccessibility.proto).
 */
object AccessibilityPreference {
    val Settings =
        AppDestinationPreference<AppPreferences>(
            title = R.string.a11y_settings,
            summary = R.string.a11y_settings_summary,
            destination = Destination.Settings(PreferenceScreenOption.ACCESSIBILITY),
        )

    // region Setup

    /** Turning this on shows the setup screen again straight away */
    val ShowSetupWizard =
        a11ySwitch(
            title = R.string.a11y_wizard_show,
            summary = R.string.a11y_wizard_show_summary,
            defaultValue = true,
            get = { !it.setupWizardDone },
            set = { setupWizardDone = !it },
        )

    val QuickShortcutPref =
        a11yChoice(
            title = R.string.a11y_quick_shortcut,
            displayValues = R.array.a11y_quick_shortcuts,
            values =
                listOf(
                    QuickShortcut.QUICK_SHORTCUT_OFF,
                    QuickShortcut.QUICK_SHORTCUT_MENU,
                    QuickShortcut.QUICK_SHORTCUT_INFO,
                    QuickShortcut.QUICK_SHORTCUT_PLAY_PAUSE,
                ),
            get = { it.quickShortcut },
            set = { quickShortcut = it },
        )

    /** Copies the current settings to every profile; handled in PreferencesContent */
    val ApplyToAllProfiles =
        AppClickablePreference<AppPreferences>(
            title = R.string.a11y_apply_all_profiles,
            summary = R.string.a11y_apply_all_profiles_summary,
        )

    val PerProfileSettings =
        a11ySwitch(
            title = R.string.a11y_per_profile,
            summary = R.string.a11y_per_profile_summary,
            get = { it.perProfileSettings },
            set = { perProfileSettings = it },
        )

    // endregion

    // region Focus

    val FocusRingEnabled =
        a11ySwitch(
            title = R.string.a11y_focus_ring,
            summary = R.string.a11y_focus_ring_summary,
            get = { it.focusRingEnabled },
            set = { focusRingEnabled = it },
        )

    const val DEFAULT_FOCUS_RING_THICKNESS = 4
    val FocusRingThickness =
        AppSliderPreference<AppPreferences>(
            title = R.string.a11y_focus_ring_thickness,
            defaultValue = DEFAULT_FOCUS_RING_THICKNESS.toLong(),
            min = 2,
            max = 8,
            interval = 1,
            getter = {
                it.accessibilityPreferences.focusRingThicknessDp
                    .takeIf { v -> v > 0 }
                    ?.toLong() ?: DEFAULT_FOCUS_RING_THICKNESS.toLong()
            },
            setter = { prefs, value ->
                prefs.updateAccessibilityPreferences { focusRingThicknessDp = value.toInt() }
            },
            summarizer = { value -> value?.let { "${it}dp" } },
        )

    val FocusRingColorPref =
        a11yChoice(
            title = R.string.a11y_focus_ring_color,
            displayValues = R.array.a11y_focus_ring_colors,
            values =
                listOf(
                    FocusRingColor.FOCUS_RING_WHITE,
                    FocusRingColor.FOCUS_RING_YELLOW,
                    FocusRingColor.FOCUS_RING_CYAN,
                    FocusRingColor.FOCUS_RING_MAGENTA,
                    FocusRingColor.FOCUS_RING_THEME,
                ),
            get = { it.focusRingColor },
            set = { focusRingColor = it },
        )

    val FocusCuePref =
        a11yChoice(
            title = R.string.a11y_focus_cue,
            displayValues = R.array.a11y_focus_cues,
            values = listOf(FocusCue.FOCUS_CUE_NONE, FocusCue.FOCUS_CUE_FILL, FocusCue.FOCUS_CUE_UNDERLINE),
            get = { it.focusCue },
            set = { focusCue = it },
        )

    val DimUnfocused =
        a11yPercentSlider(
            title = R.string.a11y_dim_unfocused,
            max = 60,
            get = { it.dimUnfocusedPercent },
            set = { dimUnfocusedPercent = it },
        )

    // endregion

    // region Text & layout

    /** Text scale choices in percent. 0 means follow the system font scale. */
    val textScaleValues = listOf(0, 85, 100, 115, 130, 150, 175, 200)
    val TextScale =
        a11yChoice(
            title = R.string.a11y_text_size,
            displayValues = R.array.a11y_text_sizes,
            values = textScaleValues,
            get = { it.textScalePercent },
            set = { textScalePercent = it },
        )

    /** Interface zoom choices in percent. Stored as 0 for 100% so the proto default is upstream. */
    val uiZoomValues = listOf(100, 110, 125, 140)
    val UiZoom =
        a11yChoice(
            title = R.string.a11y_ui_zoom,
            displayValues = R.array.a11y_ui_zooms,
            values = uiZoomValues,
            get = { it.uiZoomPercent.takeIf { v -> v > 0 } ?: 100 },
            set = { uiZoomPercent = if (it == 100) 0 else it },
        )

    val minTextSizeValues = listOf(0, 14, 16, 18, 20)
    val MinTextSize =
        a11yChoice(
            title = R.string.a11y_min_text_size,
            displayValues = R.array.a11y_min_text_sizes,
            values = minTextSizeValues,
            get = { it.minTextSizeSp },
            set = { minTextSizeSp = it },
        )

    val AppFont =
        a11yChoice(
            title = R.string.a11y_font,
            displayValues = R.array.a11y_fonts,
            values =
                listOf(
                    A11yFont.A11Y_FONT_DEFAULT,
                    A11yFont.A11Y_FONT_ATKINSON,
                    A11yFont.A11Y_FONT_LEXEND,
                    A11yFont.A11Y_FONT_OPEN_DYSLEXIC,
                ),
            get = { it.appFont },
            set = { appFont = it },
        )

    val lineHeightValues = listOf(0, 15, 30, 50)
    val LineHeight =
        a11yChoice(
            title = R.string.a11y_line_height,
            displayValues = R.array.a11y_line_heights,
            values = lineHeightValues,
            get = { it.lineHeightPercent },
            set = { lineHeightPercent = it },
        )

    val letterSpacingValues = listOf(0, 30, 60, 120)
    val LetterSpacing =
        a11yChoice(
            title = R.string.a11y_letter_spacing,
            displayValues = R.array.a11y_letter_spacings,
            values = letterSpacingValues,
            get = { it.letterSpacingPermille },
            set = { letterSpacingPermille = it },
        )

    val PlainTextTitles =
        a11ySwitch(
            title = R.string.a11y_plain_titles,
            summary = R.string.a11y_plain_titles_summary,
            // On by default: the one deliberate difference from upstream
            defaultValue = true,
            get = { !it.allowLogoTitles },
            set = { allowLogoTitles = !it },
        )

    val AlwaysShowCardTitles =
        a11ySwitch(
            title = R.string.a11y_always_titles,
            summary = R.string.a11y_always_titles_summary,
            get = { it.alwaysShowCardTitles },
            set = { alwaysShowCardTitles = it },
        )

    val NoScrollingText =
        a11ySwitch(
            title = R.string.a11y_no_scrolling_text,
            summary = R.string.a11y_no_scrolling_text_summary,
            get = { it.noScrollingText },
            set = { noScrollingText = it },
        )

    val ArtworkShapePref =
        a11yChoice(
            title = R.string.a11y_artwork_shape,
            displayValues = R.array.a11y_artwork_shapes,
            values =
                listOf(
                    ArtworkShape.ARTWORK_STRETCH,
                    ArtworkShape.ARTWORK_CROP,
                    ArtworkShape.ARTWORK_FIT,
                ),
            get = { it.artworkShape },
            set = { artworkShape = it },
        )

    val WrapCardTitles =
        a11ySwitch(
            title = R.string.a11y_wrap_titles,
            summary = R.string.a11y_wrap_titles_summary,
            get = { it.wrapCardTitles },
            set = { wrapCardTitles = it },
        )

    // endregion

    // region Simplified layout

    val maxHomeRowValues = listOf(0, 2, 3, 4, 6)
    val MaxHomeRows =
        a11yChoice(
            title = R.string.a11y_max_home_rows,
            displayValues = R.array.a11y_max_home_rows_values,
            values = maxHomeRowValues,
            get = { it.maxHomeRows },
            set = { maxHomeRows = it },
        )

    val HideTechnicalDetails =
        a11ySwitch(
            title = R.string.a11y_hide_tech,
            summary = R.string.a11y_hide_tech_summary,
            get = { it.hideTechnicalDetails },
            set = { hideTechnicalDetails = it },
        )

    // endregion

    // region Colour & contrast

    val HighContrast =
        a11yChoice(
            title = R.string.a11y_high_contrast,
            displayValues = R.array.a11y_high_contrast_modes,
            values =
                listOf(
                    HighContrastMode.HIGH_CONTRAST_OFF,
                    HighContrastMode.HIGH_CONTRAST_WHITE_ON_BLACK,
                    HighContrastMode.HIGH_CONTRAST_YELLOW_ON_BLACK,
                    HighContrastMode.HIGH_CONTRAST_BLACK_ON_WHITE,
                    HighContrastMode.HIGH_CONTRAST_COLOR_BLIND,
                ),
            get = { it.highContrast },
            set = { highContrast = it },
        )

    val Accent =
        a11yChoice(
            title = R.string.a11y_accent,
            displayValues = R.array.a11y_accents,
            values =
                listOf(
                    AccentColor.ACCENT_DEFAULT,
                    AccentColor.ACCENT_ORANGE,
                    AccentColor.ACCENT_CYAN,
                    AccentColor.ACCENT_LIME,
                    AccentColor.ACCENT_PINK,
                    AccentColor.ACCENT_WHITE,
                ),
            get = { it.accentColor },
            set = { accentColor = it },
        )

    val Highlight =
        a11yChoice(
            title = R.string.a11y_highlight,
            displayValues = R.array.a11y_highlights,
            values =
                listOf(
                    HighlightColor.HIGHLIGHT_DEFAULT,
                    HighlightColor.HIGHLIGHT_YELLOW,
                    HighlightColor.HIGHLIGHT_ORANGE,
                    HighlightColor.HIGHLIGHT_CYAN,
                    HighlightColor.HIGHLIGHT_LIGHT_BLUE,
                    HighlightColor.HIGHLIGHT_GREEN,
                    HighlightColor.HIGHLIGHT_LIME,
                    HighlightColor.HIGHLIGHT_PINK,
                    HighlightColor.HIGHLIGHT_DARK_GREY,
                ),
            get = { it.highlightColor },
            set = { highlightColor = it },
        )

    val BackdropDim =
        a11yPercentSlider(
            title = R.string.a11y_backdrop_dim,
            max = 90,
            get = { it.backdropDimPercent },
            set = { backdropDimPercent = it },
        )

    val ArtworkDim =
        a11yPercentSlider(
            title = R.string.a11y_artwork_dim,
            max = 50,
            get = { it.artworkDimPercent },
            set = { artworkDimPercent = it },
        )

    val ArtworkDesaturate =
        a11yPercentSlider(
            title = R.string.a11y_artwork_desaturate,
            max = 100,
            interval = 25,
            get = { it.artworkDesaturatePercent },
            set = { artworkDesaturatePercent = it },
        )

    val HighVisibilityStatus =
        a11ySwitch(
            title = R.string.a11y_status_indicators,
            summary = R.string.a11y_status_indicators_summary,
            get = { it.highVisibilityStatus },
            set = { highVisibilityStatus = it },
        )

    val ShowProgressPercent =
        a11ySwitch(
            title = R.string.a11y_progress_percent,
            summary = R.string.a11y_progress_percent_summary,
            get = { it.showProgressPercent },
            set = { showProgressPercent = it },
        )

    // endregion

    // region Motion

    val ReduceMotion =
        a11ySwitch(
            title = R.string.a11y_reduce_motion,
            summary = R.string.a11y_reduce_motion_summary,
            get = { it.reduceMotion },
            set = { reduceMotion = it },
        )

    val StaticBackdrop =
        a11ySwitch(
            title = R.string.a11y_static_backdrop,
            summary = R.string.a11y_static_backdrop_summary,
            get = { it.staticBackdrop },
            set = { staticBackdrop = it },
        )

    val NavRepeat =
        a11yChoice(
            title = R.string.a11y_nav_repeat,
            displayValues = R.array.a11y_nav_repeats,
            values =
                listOf(
                    NavRepeatSpeed.NAV_REPEAT_NORMAL,
                    NavRepeatSpeed.NAV_REPEAT_SLOW,
                    NavRepeatSpeed.NAV_REPEAT_VERY_SLOW,
                ),
            get = { it.navRepeatSpeed },
            set = { navRepeatSpeed = it },
        )

    // endregion

    val PhotosensitivityWarning =
        a11ySwitch(
            title = R.string.a11y_photosensitivity,
            summary = R.string.a11y_photosensitivity_summary,
            get = { it.photosensitivityWarning },
            set = { photosensitivityWarning = it },
        )

    // endregion

    // region Player

    val playerScaleValues = listOf(100, 125, 150)
    val PlayerControlsScale =
        a11yChoice(
            title = R.string.a11y_player_scale,
            displayValues = R.array.a11y_player_scales,
            values = playerScaleValues,
            get = { it.playerControlsScalePercent.takeIf { v -> v > 0 } ?: 100 },
            set = { playerControlsScalePercent = if (it == 100) 0 else it },
        )

    val SolidPlayerBackground =
        a11ySwitch(
            title = R.string.a11y_player_solid,
            summary = R.string.a11y_player_solid_summary,
            get = { it.solidPlayerBackground },
            set = { solidPlayerBackground = it },
        )

    /** Seconds. 0 means use the regular playback setting; 3600 stands for "until dismissed". */
    val controllerTimeoutValues = listOf(0, 10, 30, 60, 3600)
    val ControllerTimeout =
        a11yChoice(
            title = R.string.a11y_player_timeout,
            displayValues = R.array.a11y_player_timeouts,
            values = controllerTimeoutValues,
            get = { it.controllerTimeoutS },
            set = { controllerTimeoutS = it },
        )

    /** Seconds. Stored as 0 for the default of 10. */
    val skipButtonTimeoutValues = listOf(10, 20, 30, 60)
    val SkipButtonTimeout =
        a11yChoice(
            title = R.string.a11y_skip_timeout,
            displayValues = R.array.a11y_skip_timeouts,
            values = skipButtonTimeoutValues,
            get = { it.skipButtonTimeoutS.takeIf { v -> v > 0 } ?: 10 },
            set = { skipButtonTimeoutS = if (it == 10) 0 else it },
        )

    // endregion

    // region Subtitles and audio

    val PreferSdh =
        a11ySwitch(
            title = R.string.a11y_prefer_sdh,
            summary = R.string.a11y_prefer_sdh_summary,
            get = { it.preferSdhSubtitles },
            set = { preferSdhSubtitles = it },
        )

    val PreferAudioDescription =
        a11ySwitch(
            title = R.string.a11y_prefer_ad,
            summary = R.string.a11y_prefer_ad_summary,
            get = { it.preferAudioDescription },
            set = { preferAudioDescription = it },
        )

    val SubtitlesAtTop =
        a11ySwitch(
            title = R.string.a11y_subtitles_top,
            summary = R.string.a11y_subtitles_top_summary,
            get = { it.subtitlesAtTop },
            set = { subtitlesAtTop = it },
        )

    val SubtitleFont =
        a11yChoice(
            title = R.string.a11y_subtitle_font,
            displayValues = R.array.a11y_fonts,
            values =
                listOf(
                    A11yFont.A11Y_FONT_DEFAULT,
                    A11yFont.A11Y_FONT_ATKINSON,
                    A11yFont.A11Y_FONT_LEXEND,
                    A11yFont.A11Y_FONT_OPEN_DYSLEXIC,
                ),
            get = { it.subtitleFont },
            set = { subtitleFont = it },
        )

    val SoundCues =
        a11yChoice(
            title = R.string.a11y_sound_cues,
            displayValues = R.array.a11y_sound_cue_styles,
            values =
                listOf(
                    SoundCueStyle.SOUND_CUE_NONE,
                    SoundCueStyle.SOUND_CUE_ITALIC,
                    SoundCueStyle.SOUND_CUE_COLOR,
                ),
            get = { it.soundCueStyle },
            set = { soundCueStyle = it },
        )

    val ForceSubtitleStyle =
        a11ySwitch(
            title = R.string.a11y_force_sub_style,
            summary = R.string.a11y_force_sub_style_summary,
            get = { it.forceSubtitleStyle },
            set = { forceSubtitleStyle = it },
        )

    // endregion

    // region Screen reader

    val SpeakFocusedItem =
        a11ySwitch(
            title = R.string.a11y_speak_focus,
            summary = R.string.a11y_speak_focus_summary,
            get = { it.speakFocusedItem },
            set = { speakFocusedItem = it },
        )

    val BuiltInKeyboardPref =
        a11yChoice(
            title = R.string.a11y_keyboard,
            displayValues = R.array.a11y_keyboard_modes,
            values =
                listOf(
                    BuiltInKeyboard.BUILT_IN_KEYBOARD_AUTO,
                    BuiltInKeyboard.BUILT_IN_KEYBOARD_ALWAYS,
                    BuiltInKeyboard.BUILT_IN_KEYBOARD_NEVER,
                ),
            get = { it.builtInKeyboard },
            set = { builtInKeyboard = it },
        )

    val QuietTrackAnnouncements =
        a11ySwitch(
            title = R.string.a11y_quiet_tracks,
            summary = R.string.a11y_quiet_tracks_summary,
            get = { it.quietTrackAnnouncements },
            set = { quietTrackAnnouncements = it },
        )

    val CardAnnouncementsTitleOnly =
        a11ySwitch(
            title = R.string.a11y_card_title_only,
            summary = R.string.a11y_card_title_only_summary,
            get = { it.cardAnnouncementsTitleOnly },
            set = { cardAnnouncementsTitleOnly = it },
        )

    // endregion
}

/**
 * Lazy because this list and the upstream preference lists refer to each other, which otherwise
 * leaves some entries null depending on which file is loaded first.
 */
val accessibilityPreferences: List<PreferenceGroup<AppPreferences>> by lazy {
    listOf(
        PreferenceGroup(
            title = R.string.a11y_group_setup,
            preferences =
                listOf(
                    AccessibilityPreference.ShowSetupWizard,
                    AccessibilityPreference.QuickShortcutPref,
                    AccessibilityPreference.PerProfileSettings,
                ),
            conditionalPreferences =
                listOf(
                    ConditionalPreferences(
                        condition = { it.accessibilityPreferences.perProfileSettings },
                        preferences = listOf(AccessibilityPreference.ApplyToAllProfiles),
                    ),
                ),
        ),
        PreferenceGroup(
            title = R.string.a11y_group_focus,
            preferences =
                listOf(
                    AccessibilityPreference.FocusRingEnabled,
                ),
            conditionalPreferences =
                listOf(
                    ConditionalPreferences(
                        condition = { it.accessibilityPreferences.focusRingEnabled },
                        preferences =
                            listOf(
                                AccessibilityPreference.FocusRingThickness,
                                AccessibilityPreference.FocusRingColorPref,
                            ),
                    ),
                    ConditionalPreferences(
                        condition = { true },
                        preferences =
                            listOf(
                                AccessibilityPreference.FocusCuePref,
                                AccessibilityPreference.DimUnfocused,
                            ),
                    ),
                ),
        ),
        PreferenceGroup(
            title = R.string.a11y_group_text_layout,
            preferences =
                listOf(
                    AccessibilityPreference.TextScale,
                    AccessibilityPreference.UiZoom,
                    AccessibilityPreference.MinTextSize,
                    AccessibilityPreference.AppFont,
                    AccessibilityPreference.LineHeight,
                    AccessibilityPreference.LetterSpacing,
                    AccessibilityPreference.PlainTextTitles,
                    AccessibilityPreference.AlwaysShowCardTitles,
                    AccessibilityPreference.WrapCardTitles,
                    AccessibilityPreference.NoScrollingText,
                    AccessibilityPreference.ArtworkShapePref,
                ),
        ),
        PreferenceGroup(
            title = R.string.a11y_group_simplified,
            preferences =
                listOf(
                    AccessibilityPreference.MaxHomeRows,
                    AccessibilityPreference.HideTechnicalDetails,
                ),
        ),
        PreferenceGroup(
            title = R.string.a11y_group_contrast,
            preferences =
                listOf(
                    AccessibilityPreference.HighContrast,
                    AccessibilityPreference.Accent,
                    AccessibilityPreference.Highlight,
                    // Existing upstream setting, repeated here so it is easy to find
                    AppPreference.BackdropStylePref,
                    AccessibilityPreference.BackdropDim,
                    AccessibilityPreference.ArtworkDim,
                    AccessibilityPreference.ArtworkDesaturate,
                    AccessibilityPreference.HighVisibilityStatus,
                    AccessibilityPreference.ShowProgressPercent,
                ),
        ),
        PreferenceGroup(
            title = R.string.a11y_group_player,
            preferences =
                listOf(
                    AccessibilityPreference.PlayerControlsScale,
                    AccessibilityPreference.SolidPlayerBackground,
                    AccessibilityPreference.ControllerTimeout,
                    AccessibilityPreference.SkipButtonTimeout,
                ),
        ),
        PreferenceGroup(
            title = R.string.a11y_group_subtitles_audio,
            preferences =
                listOf(
                    // Existing upstream page, linked here so it is easy to find
                    AppPreference.SubtitleStyle,
                    AccessibilityPreference.SubtitleFont,
                    AccessibilityPreference.SubtitlesAtTop,
                    AccessibilityPreference.SoundCues,
                    AccessibilityPreference.ForceSubtitleStyle,
                    AccessibilityPreference.PreferSdh,
                    AccessibilityPreference.PreferAudioDescription,
                ),
        ),
        PreferenceGroup(
            title = R.string.a11y_group_screen_reader,
            preferences =
                listOf(
                    AccessibilityPreference.SpeakFocusedItem,
                    AccessibilityPreference.CardAnnouncementsTitleOnly,
                    AccessibilityPreference.QuietTrackAnnouncements,
                    AccessibilityPreference.BuiltInKeyboardPref,
                ),
        ),
        PreferenceGroup(
            title = R.string.a11y_group_motion,
            preferences =
                listOf(
                    AccessibilityPreference.ReduceMotion,
                    AccessibilityPreference.StaticBackdrop,
                    AccessibilityPreference.NavRepeat,
                    AccessibilityPreference.PhotosensitivityWarning,
                    // Existing upstream setting, repeated here so it is easy to find
                    AppPreference.PlayThemeMusic,
                ),
        ),
    )
}
