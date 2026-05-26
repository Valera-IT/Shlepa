package com.example.shlepa

import android.app.Application
import android.content.Context
import android.content.res.Resources
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel

/**
 * Варианты тем приложения.
 */
enum class AppTheme {
    SYSTEM, LIGHT, DARK, PINK, YELLOW
}

/**
 * Поддерживаемые языки.
 */
enum class AppLanguage(val code: String, val label: String) {
    RUSSIAN("ru", "Русский"),
    ENGLISH("en", "English"),
    ITALIAN("it", "Italiano"),
    FRENCH("fr", "Français"),
    GERMAN("de", "Deutsch"),
    JAPANESE("ja", "日本語"),
    CHINESE("zh", "中文")
}

/**
 * ViewModel для управления состоянием приложения "Шлепа".
 * Хранит данные о силе ударов, рекордах и истории.
 */
class SlapViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("shlepa_prefs", Context.MODE_PRIVATE)

    // Текущая тема приложения (с сохранением)
    val currentThemeState = mutableStateOf(
        run {
            val saved = prefs.getString("theme", AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
            try {
                if (saved == "PINK_BLACK") AppTheme.PINK else AppTheme.valueOf(saved)
            } catch (e: Exception) {
                AppTheme.SYSTEM
            }
        }
    )

    // Текущий язык приложения (с сохранением или по системе)
    val currentLanguageState = mutableStateOf(
        prefs.getString("language", null)?.let { savedCode ->
            AppLanguage.entries.find { it.code == savedCode }
        } ?: getSystemLanguage()
    )

    private fun getSystemLanguage(): AppLanguage {
        val systemLocale = Resources.getSystem().configuration.locales[0]
        return AppLanguage.entries.find { it.code == systemLocale.language } ?: AppLanguage.ENGLISH
    }

    // Текущая сила удара
    val impactForceState = mutableFloatStateOf(0f)
    // Максимальная зафиксированная сила (рекорд) (с сохранением)
    val maxForceState = mutableFloatStateOf(prefs.getFloat("max_force", 0f))
    // Порог чувствительности акселерометра (с сохранением)
    val thresholdState = mutableFloatStateOf(prefs.getFloat("threshold", 15f))
    // Состояние процесса "шлепка" для визуальных эффектов
    val isSlappingState = mutableStateOf(value = false)
    // Общее количество шлепков (с сохранением)
    val slapCountState = mutableIntStateOf(prefs.getInt("slap_count", 0))
    // История последних 14 шлепков (с сохранением)
    val slapHistory = mutableStateListOf<Float>().apply {
        val savedHistory = prefs.getString("slap_history", "") ?: ""
        if (savedHistory.isNotEmpty()) {
            addAll(savedHistory.split(",").mapNotNull { it.toFloatOrNull() })
        }
    }

    /**
     * Смена темы с сохранением.
     */
    fun setTheme(theme: AppTheme) {
        currentThemeState.value = theme
        prefs.edit().putString("theme", theme.name).apply()
    }

    /**
     * Смена языка с сохранением.
     */
    fun setLanguage(language: AppLanguage) {
        currentLanguageState.value = language
        prefs.edit().putString("language", language.code).commit()
    }

    /**
     * Смена порога чувствительности с сохранением.
     */
    fun setThreshold(value: Float) {
        thresholdState.floatValue = value
        prefs.edit().putFloat("threshold", value).apply()
    }

    /**
     * Сброс статистики.
     */
    fun reset() {
        slapCountState.intValue = 0
        maxForceState.floatValue = 0f
        slapHistory.clear()
        prefs.edit()
            .putInt("slap_count", 0)
            .putFloat("max_force", 0f)
            .putString("slap_history", "")
            .apply()
    }

    /**
     * Добавление нового шлепка в статистику.
     * @param force Сила зафиксированного удара.
     */
    fun addSlap(force: Float) {
        slapCountState.intValue++
        // Обновление рекорда
        if (force > maxForceState.floatValue) {
            maxForceState.floatValue = force
        }
        // Добавление в конец истории
        slapHistory.add(force)
        // Ограничение размера истории до 14 элементов (удаляем старые из начала)
        if (slapHistory.size > 14) {
            slapHistory.removeAt(0)
        }
        
        // Сохранение статистики
        prefs.edit()
            .putInt("slap_count", slapCountState.intValue)
            .putFloat("max_force", maxForceState.floatValue)
            .putString("slap_history", slapHistory.joinToString(","))
            .apply()
    }
}
