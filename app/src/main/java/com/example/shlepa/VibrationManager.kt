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
     * @param isStrong Если true, вибрация будет дольше и сильнее.
     */
    fun vibrate(isStrong: Boolean) {
        val duration = if (isStrong) 150L else 70L
        val amplitude = if (isStrong) 255 else 180 // Максимальное значение 255
        
        // Создание одиночного вибро-эффекта
        vibrator?.vibrate(VibrationEffect.createOneShot(duration, amplitude))
    }
}
