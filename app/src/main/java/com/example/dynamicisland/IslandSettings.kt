package com.example.dynamicisland

import android.content.Context

object IslandSettings {
    private const val PREFS = "island_settings"
    private const val WIDTH = "width"
    private const val HEIGHT = "height"
    private const val TOP = "top"
    private const val ENABLED = "enabled"

    fun width(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(WIDTH, 230)

    fun height(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(HEIGHT, 38)

    fun top(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(TOP, 10)

    fun enabled(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(ENABLED, true)

    fun save(context: Context, width: Int, height: Int, top: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(WIDTH, width.coerceIn(120, 340))
            .putInt(HEIGHT, height.coerceIn(28, 70))
            .putInt(TOP, top.coerceIn(0, 50))
            .apply()
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(ENABLED, enabled)
            .apply()
    }
}
