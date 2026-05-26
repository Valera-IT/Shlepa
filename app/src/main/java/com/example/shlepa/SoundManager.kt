package com.example.shlepa

import android.content.Context
import android.media.MediaPlayer
import android.util.Log

/**
 * Менеджер для управления звуковыми эффектами.
 * Использует MediaPlayer для последовательного воспроизведения.
 * Реализовано ограничение очереди: максимум 1 звук в ожидании, чтобы избежать наложения.
 */
class SoundManager(private val context: Context) {
    private val soundResIds = intArrayOf(
        R.raw.hentai_moan,
        R.raw.hentai_yamete,
        R.raw.glitter_hentai,
        R.raw.arigato,
    )
    
    private var currentPlayer: MediaPlayer? = null
    private var queuedIndex: Int? = null
    private var queuedAction: (() -> Unit)? = null

    /**
     * Проигрывание звука шлепка.
     * @param onStart Коллбэк, который вызывается в момент фактического начала проигрывания.
     * @return true если звук запущен или успешно поставлен в очередь.
     */
    fun playSlap(isStrong: Boolean, onStart: (() -> Unit)? = null): Boolean {
        return playSound(if (isStrong) 2 else 0, onStart)
    }

    /**
     * Проигрывание звука по индексу (0-3).
     * Если звук уже играет, ставит один следующий в очередь.
     * Если очередь уже занята, игнорирует вызов.
     * @param onStart Коллбэк, который вызывается в момент фактического начала проигрывания.
     * @return true если звук запущен или успешно поставлен в очередь.
     */
    fun playSound(index: Int, onStart: (() -> Unit)? = null): Boolean {
        if (index !in soundResIds.indices) return false
        
        synchronized(this) {
            if (currentPlayer == null) {
                // Ничего не играет, запускаем сразу
                onStart?.invoke()
                playInternal(index)
                return true
            } else if (queuedIndex == null) {
                // Уже играет, но очередь пуста - ставим в очередь
                queuedIndex = index
                queuedAction = onStart
                Log.d("SoundManager", "Sound $index queued")
                return true
            } else {
                // И играет, и в очереди уже есть звук - игнорируем
                Log.d("SoundManager", "Sound $index ignored (queue full)")
                return false
            }
        }
    }

    /**
     * Внутренний метод для создания и запуска MediaPlayer.
     */
    private fun playInternal(index: Int) {
        val resId = soundResIds[index]
        try {
            val mediaPlayer = MediaPlayer.create(context, resId) ?: return
            currentPlayer = mediaPlayer
            
            mediaPlayer.setOnCompletionListener { mp ->
                mp.release()
                synchronized(this) {
                    currentPlayer = null
                    // После завершения проверяем, нет ли чего в очереди
                    val next = queuedIndex
                    val action = queuedAction
                    if (next != null) {
                        queuedIndex = null
                        queuedAction = null
                        action?.invoke() // Вызываем коллбэк для следующего звука
                        playInternal(next)
                    }
                }
            }
            
            mediaPlayer.start()
            
        } catch (e: Exception) {
            Log.e("SoundManager", "Error playing sound at index $index", e)
            synchronized(this) {
                currentPlayer = null
            }
        }
    }

    /**
     * Освобождение ресурсов при закрытии Activity.
     */
    fun release() {
        synchronized(this) {
            currentPlayer?.let {
                try {
                    if (it.isPlaying) it.stop()
                } catch (e: Exception) { /* ignore */ }
                it.release()
            }
            currentPlayer = null
            queuedIndex = null
            queuedAction = null
        }
    }
}
