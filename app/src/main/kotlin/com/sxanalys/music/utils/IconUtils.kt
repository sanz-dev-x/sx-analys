package com.sxanalys.music.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Launcher icon switching through activity-alias components. Exactly one alias is enabled at a time,
 * so the home screen icon really changes (no preview-only state).
 */
object IconUtils {
    val designs = listOf("note", "bars", "vinyl", "phones")
    val colors = listOf("blue", "pink", "orange", "green", "purple", "graphite")

    private const val DEFAULT = "MainActivityAlias"
    private const val STATIC = "MainActivityStatic"

    private fun variantAlias(design: String, color: String) = "Icon${design.replaceFirstChar { it.uppercase() }}${color.replaceFirstChar { it.uppercase() }}"

    private fun allAliases(): List<String> =
        listOf(DEFAULT, STATIC) + designs.flatMap { d -> colors.map { c -> variantAlias(d, c) } }

    private fun apply(context: Context, active: String) {
        val pm = context.packageManager
        // Enable the target first so a launcher entry always exists.
        pm.setComponentEnabledSetting(
            ComponentName(context, "com.sxanalys.music.$active"),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
        allAliases().filter { it != active }.forEach { name ->
            val cn = ComponentName(context, "com.sxanalys.music.$name")
            if (pm.getComponentEnabledSetting(cn) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                pm.setComponentEnabledSetting(cn, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            }
        }
    }

    /** Legacy toggle: dynamic (default) vs static icon. */
    fun setIcon(context: Context, enabled: Boolean) = apply(context, if (enabled) DEFAULT else STATIC)

    fun setVariant(context: Context, design: String, color: String) = apply(context, variantAlias(design, color))

    fun resetToDefault(context: Context) = apply(context, DEFAULT)

    /** Returns (design, color) of the active variant, or null when the default/static icon is active. */
    fun currentVariant(context: Context): Pair<String, String>? {
        val pm = context.packageManager
        for (d in designs) for (c in colors) {
            val cn = ComponentName(context, "com.sxanalys.music.${variantAlias(d, c)}")
            if (pm.getComponentEnabledSetting(cn) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) return d to c
        }
        return null
    }
}
