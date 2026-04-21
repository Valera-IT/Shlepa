package com.example.shlepa

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Класс для обнаружения "шлепков" с использованием акселерометра.
 * @param onSlap Коллбэк при обнаружении удара выше порога.
 * @param onForceUpdate Коллбэк для обновления текущих показаний силы.
 */
class SlapDetector(
    context: Context,
    private val onSlap: (Float) -> Unit,
    private val onForceUpdate: (Float) -> Unit,
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    
    // Порог срабатывания
    var threshold = 15f
    // Время последнего шлепка для предотвращения дребезга
    private var lastSlapTime = 0L
    // Минимальный интервал между шлепками в мс
    private val cooldownMs = 250L

    /**
     * Регистрация слушателя сенсора.
     */
    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    /**
     * Отмена регистрации слушателя.
     */
    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if ((event == null) || (event.sensor.type != Sensor.TYPE_ACCELEROMETER)) return
        
        // Вычисление результирующего ускорения
        val totalForce = sqrt(
            (event.values[0] * event.values[0]) +
                    (event.values[1] * event.values[1]) +
                    (event.values[2] * event.values[2]),
        )
        
        // Вычитаем гравитацию Земли, чтобы получить чистую силу удара
        val currentImpact = totalForce - SensorManager.GRAVITY_EARTH
        
        // Игнорируем мелкие колебания
        if (currentImpact > 0.5f || currentImpact < -0.5f) {
            onForceUpdate(currentImpact)
        }

        // Проверка на превышение порога и кулдаун
        val currentTime = System.currentTimeMillis()
        if (currentImpact > threshold && (currentTime - lastSlapTime) > cooldownMs) {
            onSlap(currentImpact)
            lastSlapTime = currentTime
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
