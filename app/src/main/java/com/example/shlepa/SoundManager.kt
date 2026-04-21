package com.example.shlepa

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/**
 * Менеджер для управления звуковыми эффектами.
 */
class SoundManager(context: Context) {
    private val soundPool: SoundPool
    private var slapLowSoundId: Int = 0
    private var slapHighSoundId: Int = 0
    private var isLowReady = false
    private var isHighReady = false

    init {
        // Настройка атрибутов аудио (для системных звуков/эффектов)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        // Создание пула звуков
        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()

        // Слушатель окончания загрузки сэмплов
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                if (sampleId == slapLowSoundId) isLowReady = true
                if (sampleId == slapHighSoundId) isHighReady = true
            }
        }

        // Загрузка звуковых файлов из ресурсов raw
        slapLowSoundId = soundPool.load(context, R.raw.moan_low, 1)
        slapHighSoundId = soundPool.load(context, R.raw.moan_high, 1)
    }

    /**
     * Проигрывание звука шлепка.
     * @param isStrong Если true, проигрывается "сильный" звук, иначе "слабый".
     */
    fun playSlap(isStrong: Boolean) {
        val soundId = if (isStrong) slapHighSoundId else slapLowSoundId
        val ready = if (isStrong) isHighReady else isLowReady
        if (ready) {
            // Воспроизведение: id, громкость L/R, приоритет, цикл, скорость
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    /**
     * Освобождение ресурсов SoundPool.
     */
    fun release() {
        soundPool.release()
    }
}
