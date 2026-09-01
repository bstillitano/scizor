package com.scizor.feature.custom

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.vector.ImageVector

/** No-op mirror of the real [ScizorIcon]. */
sealed interface ScizorIcon {

    data class Vector(val image: ImageVector) : ScizorIcon

    data class Resource(@DrawableRes val id: Int) : ScizorIcon
}
