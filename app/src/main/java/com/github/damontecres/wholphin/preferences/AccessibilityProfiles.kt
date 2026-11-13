package com.github.damontecres.wholphin.preferences

/**
 * Accessibility: "Separate settings for each profile".
 *
 * When on, each user profile keeps its own accessibility settings, so a shared TV can suit
 * different people. The active settings always live in [AppPreferences.getAccessibilityPreferences];
 * this swaps them when the signed-in user changes.
 *
 * Returns the same object when nothing needs to change.
 */
fun AppPreferences.switchAccessibilityProfile(userId: String?): AppPreferences {
    val current = accessibilityPreferences
    if (!current.perProfileSettings || userId.isNullOrBlank()) return this
    val previousUser = activeAccessibilityUser
    if (previousUser == userId) return this
    return toBuilder()
        .apply {
            // Keep what the previous person had set
            if (previousUser.isNotBlank()) {
                putAccessibilityByUser(previousUser, current)
            }
            // Load this person's settings, or start them from the current ones
            val saved = accessibilityByUserMap[userId]
            if (saved != null) {
                accessibilityPreferences =
                    saved
                        .toBuilder()
                        // These two are about the device, not the person
                        .setPerProfileSettings(true)
                        .setSetupWizardDone(current.setupWizardDone)
                        .build()
            }
            activeAccessibilityUser = userId
        }.build()
}

/** Copies the current accessibility settings to every profile that has saved settings */
fun AppPreferences.applyAccessibilityToAllProfiles(): AppPreferences =
    toBuilder()
        .apply {
            accessibilityByUserMap.keys.toList().forEach { putAccessibilityByUser(it, accessibilityPreferences) }
        }.build()
