package com.xpotrack.app.data.prefs

import android.content.Context

// Persisted zoom multiplier for the note editor body. Same single-key prefs
// pattern as ThemePrefs — one Float doesn't justify DataStore.
class EditorZoomPrefs(context: Context) {

    private val sp = context.getSharedPreferences("xp_editor", Context.MODE_PRIVATE)

    var zoom: Float
        get() = sp.getFloat(KEY_ZOOM, 1f).coerceIn(MIN, MAX)
        set(value) { sp.edit().putFloat(KEY_ZOOM, value.coerceIn(MIN, MAX)).apply() }

    companion object {
        const val MIN = 0.7f
        const val MAX = 2.0f
        private const val KEY_ZOOM = "body_zoom"
    }
}
