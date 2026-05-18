package com.xpotrack.app.data.prefs

import android.content.Context
import com.xpotrack.app.ui.theme.Dark
import com.xpotrack.app.ui.theme.Light
import com.xpotrack.app.ui.theme.XpTokens

// Single-key prefs for the light/dark toggle. Plain SharedPreferences instead
// of DataStore — one Boolean doesn't justify another dependency, and reading
// it synchronously at Application.onCreate avoids a dark-then-light flash on
// cold start when the user has picked light.
class ThemePrefs(context: Context) {

    private val sp = context.getSharedPreferences("xp_theme", Context.MODE_PRIVATE)

    var isDark: Boolean
        get() = sp.getBoolean(KEY_DARK, true)
        set(value) {
            sp.edit().putBoolean(KEY_DARK, value).apply()
            XpTokens.apply(if (value) Dark else Light)
        }

    fun applyCurrent() = XpTokens.apply(if (isDark) Dark else Light)

    private companion object { const val KEY_DARK = "is_dark" }
}
