package code.name.monkey.retromusic.extensions

import android.content.Context
import android.content.res.Configuration
import android.os.PowerManager
import androidx.annotation.IntegerRes
import androidx.fragment.app.Fragment
import code.name.monkey.retromusic.util.PreferenceUtilKT

fun Fragment.getIntRes(@IntegerRes int: Int): Int {
    return resources.getInteger(int)
}

fun Context.getIntRes(@IntegerRes int: Int): Int {
    return resources.getInteger(int)
}

val Context.generalThemeValue
    get() = PreferenceUtilKT.getGeneralThemeValue(isSystemDarkModeEnabled())


fun Context.isSystemDarkModeEnabled(): Boolean {
    val isBatterySaverEnabled =
        (getSystemService(Context.POWER_SERVICE) as PowerManager?)?.isPowerSaveMode ?: false
    val isDarkModeEnabled =
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    return isBatterySaverEnabled or isDarkModeEnabled
}