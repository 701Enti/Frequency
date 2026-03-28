package com.org701enti.frealicane.core.datastore

import android.content.Context
import android.content.res.Configuration
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

object DarkModeSetting {

    const val FOLLOW_SYSTEM = 0    // 跟随系统
    const val USE_LIGHT = 1        // 固定使用浅色模式
    const val USE_DARK = 2         // 固定使用深色模式

    private val Context.dataStore by preferencesDataStore("dark_mode_setting")
    private val modeKey = intPreferencesKey("dark_mode")

    /**
     * 保存深色模式设置
     * @param context 上下文
     * @param mode 模式：FOLLOW_SYSTEM / USE_LIGHT / USE_DARK
     */
    fun saveMode(context: Context, mode: Int) {
        val validMode = when (mode) {
            FOLLOW_SYSTEM, USE_LIGHT, USE_DARK -> mode
            else -> FOLLOW_SYSTEM
        }

        runBlocking(Dispatchers.IO) {
            context.dataStore.edit { pref ->
                pref[modeKey] = validMode
            }
        }
    }

    /**
     * 获取当前保存的深色模式设置
     * @return 当前设置：FOLLOW_SYSTEM / USE_LIGHT / USE_DARK
     */
    fun getMode(context: Context): Int {
        return runBlocking(Dispatchers.IO) {
            context.dataStore.data.first()[modeKey] ?: FOLLOW_SYSTEM
        }
    }

    /**
     * 判断当前是否应该使用深色主题
     * @return true 使用深色，false 使用浅色
     */
    fun shouldUseDark(context: Context): Boolean {
        return when (getMode(context)) {
            USE_LIGHT -> false
            USE_DARK -> true
            else -> isSystemInDarkMode(context)
        }
    }

    fun isSystemInDarkMode(context: Context): Boolean {
        val uiMode = context.resources.configuration.uiMode
        val nightMode = uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightMode == Configuration.UI_MODE_NIGHT_YES
    }
}
