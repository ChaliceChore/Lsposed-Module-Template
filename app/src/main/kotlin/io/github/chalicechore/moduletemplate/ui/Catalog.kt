package io.github.chalicechore.moduletemplate.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.chalicechore.moduletemplate.R
import io.github.chalicechore.moduletemplate.prefs.Choice
import io.github.chalicechore.moduletemplate.prefs.FeatureCategory
import io.github.chalicechore.moduletemplate.prefs.FeatureSwitch

// TEMPLATE: labels and icons for the placeholder settings in prefs/FeatureSettings.kt.

val FeatureCategory.icon: ImageVector
    get() =
        when (this) {
            FeatureCategory.GENERAL -> Icons.Outlined.Tune
            FeatureCategory.INTERFACE -> Icons.Outlined.Brush
            FeatureCategory.ADVANCED -> Icons.Outlined.Science
        }

@get:StringRes
val FeatureCategory.titleRes: Int
    get() =
        when (this) {
            FeatureCategory.GENERAL -> R.string.category_general
            FeatureCategory.INTERFACE -> R.string.category_interface
            FeatureCategory.ADVANCED -> R.string.category_advanced
        }

val FeatureSwitch.icon: ImageVector
    get() =
        when (this) {
            FeatureSwitch.FEATURE_ONE -> Icons.Outlined.Extension
            FeatureSwitch.FEATURE_TWO -> Icons.Outlined.Bolt
            FeatureSwitch.FEATURE_THREE -> Icons.Outlined.Visibility
            FeatureSwitch.FEATURE_FOUR -> Icons.Outlined.Layers
            FeatureSwitch.FEATURE_FIVE -> Icons.Outlined.Speed
            FeatureSwitch.FEATURE_SIX -> Icons.Outlined.AutoAwesome
        }

@get:StringRes
val FeatureSwitch.titleRes: Int
    get() =
        when (this) {
            FeatureSwitch.FEATURE_ONE -> R.string.feature_one
            FeatureSwitch.FEATURE_TWO -> R.string.feature_two
            FeatureSwitch.FEATURE_THREE -> R.string.feature_three
            FeatureSwitch.FEATURE_FOUR -> R.string.feature_four
            FeatureSwitch.FEATURE_FIVE -> R.string.feature_five
            FeatureSwitch.FEATURE_SIX -> R.string.feature_six
        }

@get:StringRes
val FeatureSwitch.summaryRes: Int
    get() =
        when (this) {
            FeatureSwitch.FEATURE_ONE -> R.string.feature_one_summary
            FeatureSwitch.FEATURE_TWO -> R.string.feature_two_summary
            FeatureSwitch.FEATURE_THREE -> R.string.feature_three_summary
            FeatureSwitch.FEATURE_FOUR -> R.string.feature_four_summary
            FeatureSwitch.FEATURE_FIVE -> R.string.feature_five_summary
            FeatureSwitch.FEATURE_SIX -> R.string.feature_six_summary
        }

@get:StringRes
val Choice.labelRes: Int
    get() =
        when (this) {
            Choice.A -> R.string.option_choice_a
            Choice.B -> R.string.option_choice_b
            Choice.C -> R.string.option_choice_c
        }

/** Category that holds the single-choice option. */
val ChoiceCategory = FeatureCategory.GENERAL
