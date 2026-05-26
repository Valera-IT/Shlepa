package com.example.shlepa

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Эффект разлетающихся частиц.
 */
@Composable
fun ParticleEffect(
    slapId: Int,
    color: Color,
    particleCount: Int = 20
) {
    // Используем список, чтобы поддерживать частицы от нескольких шлепков одновременно
    val particles = remember { mutableStateListOf<ParticleData>() }
    
    // При каждом изменении slapId добавляем новую порцию частиц
    // При каждом изменении slapId добавляем новую порцию частиц
    LaunchedEffect(slapId) {
        if (slapId > 0) {
            val newParticles = List(particleCount) {
                val angle = Random.nextFloat() * 2 * Math.PI
                // Начальный импульс уменьшен в 2 раза для меньшего разлета
                val speed = (Random.nextFloat() * 2.5f + 1.5f) * 0.5f
                ParticleData(
                    id = Random.nextLong(),
                    x = 0.5f,
                    y = 0.5f,
                    vx = Math.cos(angle).toFloat() * speed,
                    vy = Math.sin(angle).toFloat() * speed,
                    size = Random.nextFloat() * 7f + 3f,
                    life = 1f
                )
            }
            particles.addAll(newParticles)
        }
    }

    // Отдельный цикл для обсчета физики, который не прерывается
    LaunchedEffect(Unit) {
        val frameTime = 16L
        
        while (true) {
            if (particles.isNotEmpty()) {
                val iterator = particles.listIterator()
                while (iterator.hasNext()) {
                    val p = iterator.next()
                    val age = 1f - p.life
                    
                    // Разделяем поведение на две фазы:
                    // 1. Всплеск (первые ~300мс)
                    // 2. Стекание (остальное время)
                    val isBurst = age < 0.1f
                    
                    val friction = if (isBurst) 0.96f else 0.82f // После всплеска "тормозим" горизонтально
                    val gravityVal = if (isBurst) 2.0f else 0.5f // Стекаем медленнее, чем разлетаемся
                    
                    val nvx = p.vx * friction
                    val nvy = (p.vy * friction) + (gravityVal * (frameTime / 1000f))
                    
                    // Уменьшаем скорость убывания жизни, чтобы капли стекали дольше
                    val newLife = p.life - 0.003f
                    val newY = p.y + nvy * (frameTime / 1000f)
                    
                    if (newLife <= 0 || newY > 1.3f) {
                        iterator.remove()
                    } else {
                        iterator.set(p.copy(
                            x = p.x + nvx * (frameTime / 1000f),
                            y = newY,
                            vx = nvx,
                            vy = nvy,
                            life = newLife
                        ))
                    }
                }
            }
            delay(frameTime)
        }
    }

    if (particles.isNotEmpty()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                val velocity = Math.sqrt((p.vx * p.vx + p.vy * p.vy).toDouble()).toFloat()
                val stretch = (1f + velocity * 0.8f).coerceIn(1f, 6f)
                val angle = Math.toDegrees(Math.atan2(p.vy.toDouble(), p.vx.toDouble())).toFloat()
                val center = Offset(p.x * size.width, p.y * size.height)
                
                rotate(angle, center) {
                    drawOval(
                        color = color.copy(alpha = p.life),
                        topLeft = Offset(center.x - p.size * stretch, center.y - p.size),
                        size = Size(p.size * 2 * stretch, p.size * 2)
                    )
                }
            }
        }
    }
}

data class ParticleData(
    val id: Long,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val size: Float,
    val life: Float
)

/**
 * Эффект "разбитого экрана".
 */
@Composable
fun BrokenScreenOverlay(
    isVisible: Boolean,
    intensity: Float // 0..1
) {
    if (isVisible) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val count = (5 + (intensity * 10)).toInt()
            val centerX = size.width / 2
            val centerY = size.height / 2
            
            for (i in 0 until count) {
                val path = Path()
                path.moveTo(centerX, centerY)
                
                var currX = centerX
                var currY = centerY
                val angle = (i.toFloat() / count) * 2 * Math.PI + (Random.nextFloat() * 0.5)
                
                val segments = 5
                val length = size.width * (0.3f + Random.nextFloat() * 0.4f)
                
                for (j in 1..segments) {
                    val segmentLength = length / segments
                    currX += (Math.cos(angle).toFloat() * segmentLength) + (Random.nextFloat() * 40 - 20)
                    currY += (Math.sin(angle).toFloat() * segmentLength) + (Random.nextFloat() * 40 - 20)
                    path.lineTo(currX, currY)
                }
                
                drawPath(
                    path = path,
                    color = Color.White.copy(alpha = 0.6f),
                    style = Stroke(width = 3f)
                )
            }
        }
    }
}
