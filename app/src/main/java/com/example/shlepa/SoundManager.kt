package com.example.shlepa

import android.content.Context
import android.media.MediaPlayer
import android.util.Log

/**
 * Менеджер для управления звуковыми эффектами.
 * Использует MediaPlayer для надежного воспроизведения MP3 файлов.
 */
class SoundManager(private val context: Context) {
    private val soundResIds = intArrayOf(
        R.raw.hentai_moan,
        R.raw.hentai_yamete,
        R.raw.glitter_hentai,
        R.raw.arigato
    )
    
    private val activePlayers = mutableListOf<MediaPlayer>()

    /**
     * Проигрывание звука шлепка.
     */
    fun playSlap(isStrong: Boolean) {
        playSound(if (isStrong) 2 else 0)
    }

    /**
     * Проигрывание звука по индексу (0-3).
     */
    fun playSound(index: Int) {
        if (index !in soundResIds.indices) return
        
        val resId = soundResIds[index]
        try {
            val mediaPlayer = MediaPlayer.create(context, resId) ?: return
            
            synchronized(activePlayers) {
                activePlayers.add(mediaPlayer)
            }
            
            mediaPlayer.setOnCompletionListener { mp ->
                mp.release()
                synchronized(activePlayers) {
                    activePlayers.remove(mp)
                }
            }
            
            mediaPlayer.start()
            
        } catch (e: Exception) {
            Log.e("SoundManager", "Error playing sound at index $index", e)
        }
    }

    /**
     * Освобождение всех активных ресурсов.
     */
    fun release() {
        synchronized(activePlayers) {
            activePlayers.forEach { 
                try {
                    if (it.isPlaying) it.stop()
                    it.release()
                } catch (e: Exception) { /* ignore */ }
            }
            activePlayers.clear()
        }
    }
}
