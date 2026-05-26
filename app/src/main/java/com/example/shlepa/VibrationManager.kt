package com.example.shlepa

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Менеджер для управления вибрацией устройства.
 */
class VibrationManager(context: Context) {
    // Получение сервиса вибрации с учетом версии Android
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    /**
     * Запуск вибрации.
     * @param mode Тип вибрации: 0 - обычная, 1 - сильная, 2 - критическая.
     */
    fun vibrate(mode: Int) {
        when (mode) {
            2 -> { // Критическая (двойной удар)
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 100, 50, 200),
                        intArrayOf(0, 255, 0, 255),
                        -1
                    )
                )
            }
            1 -> { // Сильная
                vibrator?.vibrate(VibrationEffect.createOneShot(150, 255))
            }
            else -> { // Обычная
                vibrator?.vibrate(VibrationEffect.createOneShot(70, 180))
            }
        }
    }
}
