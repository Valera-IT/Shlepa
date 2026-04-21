package com.example.shlepa

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

/**
 * ViewModel для управления состоянием приложения "Шлепа".
 * Хранит данные о силе ударов, рекордах и истории.
 */
class SlapViewModel : ViewModel() {
    // Текущая сила удара
    val impactForceState = mutableFloatStateOf(0f)
    // Максимальная зафиксированная сила (рекорд)
    val maxForceState = mutableFloatStateOf(0f)
    // Порог чувствительности акселерометра
    val thresholdState = mutableFloatStateOf(15f)
    // Состояние процесса "шлепка" для визуальных эффектов
    val isSlappingState = mutableStateOf(value = false)
    // Общее количество шлепков
    val slapCountState = mutableIntStateOf(0)
    // История последних 5 шлепков
    val slapHistory = mutableStateListOf<Float>()

    /**
     * Сброс статистики.
     */
    fun reset() {
        slapCountState.intValue = 0
        maxForceState.floatValue = 0f
        slapHistory.clear()
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
        // Добавление в начало истории
        slapHistory.add(0, force)
        // Ограничение размера истории до 5 элементов
        if (slapHistory.size > 5) {
            slapHistory.removeAt(slapHistory.size - 1)
        }
    }
}
