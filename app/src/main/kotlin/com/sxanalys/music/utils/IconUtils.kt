package com.sxanalys.music.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object IconUtils {
    val colors = listOf("blue", "teal", "green", "orange", "red", "pink", "purple", "graphite")

    private const val DEFAULT = "MainActivityAlias"
    private const val STATIC = "MainActivityStatic"

    private fun variantAlias(color: String) = "Icon${color.replaceFirstChar { it.uppercase() }}"

    private fun allAliases(): List<String> = listOf(DEFAULT, STATIC) + colors.map(::variantAlias)

    private fun apply(context: Context, active: String) {
        val pm = context.packageManager
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

    fun setIcon(context: Context, enabled: Boolean) = apply(context, if (enabled) DEFAULT else STATIC)

    fun setVariant(context: Context, color: String) = apply(context, variantAlias(color))

    fun resetToDefault(context: Context) = apply(context, DEFAULT)

    fun currentVariant(context: Context): String? {
        val pm = context.packageManager
        return colors.firstOrNull { c ->
            pm.getComponentEnabledSetting(ComponentName(context, "com.sxanalys.music.${variantAlias(c)}")) ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }
    }
}
