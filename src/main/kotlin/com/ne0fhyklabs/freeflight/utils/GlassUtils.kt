package com.ne0fhyklabs.freeflight.utils

import android.os.Build

/**
 * Created by fhuya on 3/11/14.
 */
object GlassUtils {

    /**
     * Determines if the current device is Google glass.
     */
    fun isGlassDevice(): Boolean {
        return Build.MODEL.contains("Glass");
    }
}