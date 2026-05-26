package com.example.shlepa

import android.content.Context
import android.hardware.camera2.CameraManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Менеджер для кратковременного мигания вспышкой.
 */
class FlashlightManager(context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraId: String? = null

    init {
        try {
            // Пытаемся найти камеру со вспышкой
            cameraId = cameraManager.cameraIdList.firstOrNull()
        } catch (e: Exception) {
            // Камера может быть недоступна
        }
    }

    /**
     * Кратковременная вспышка.
     */
    fun flash() {
        val id = cameraId ?: return
        CoroutineScope(Dispatchers.Main).launch {
            try {
                cameraManager.setTorchMode(id, true)
                delay(50) // Вспышка на 50мс
                cameraManager.setTorchMode(id, false)
            } catch (e: Exception) {
                // Игнорируем ошибки (например, если вспышка уже используется)
            }
        }
    }
}
