package com.scizor

/**
 * Gesture used to invoke the Scizor debug menu.
 *
 * Mirrors Scyther's `invocationGesture` on iOS.
 */
enum class ScizorGesture {
    /** Shake the device to open the menu. Default. */
    SHAKE,

    /** Show a draggable floating button that opens the menu. */
    FLOATING_BUTTON,

    /** No automatic gesture; open the menu only via [Scizor.show]. */
    NONE,
}
