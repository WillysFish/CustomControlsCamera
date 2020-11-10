package studio.zewei.willy.customcontrolscameraview

import android.app.Activity

/**
 * Current window brightness
 * Range is 0~1.0, 1.0 is the brightest, -1 is the system default setting
 */
var Activity.windowBrightness
    get() = window.attributes.screenBrightness
    set(brightness) {
        // Less than 0 or greater than 1.0 defaults to system brightness
        window.attributes = window.attributes.apply {
            screenBrightness = if (brightness > 1.0 || brightness < 0) -1.0F else brightness
        }
    }