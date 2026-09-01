package com.scizor.feature.custom

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * An icon a host app contributes to a [DeveloperOption].
 *
 * [Resource] exists so a host can supply an icon without depending on
 * `material-icons-extended`, whose baked-in iconset pattern Google has
 * deprecated.
 */
sealed interface ScizorIcon {

    /** A Compose [ImageVector], e.g. `Icons.Filled.Refresh`. */
    data class Vector(val image: ImageVector) : ScizorIcon

    /** A drawable resource id, e.g. `R.drawable.ic_refresh`. */
    data class Resource(@DrawableRes val id: Int) : ScizorIcon
}
