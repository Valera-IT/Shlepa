package com.example.shlepa

import android.media.AudioManager
import android.os.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shlepa.ui.theme.ShlepaTheme
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    // Менеджеры для звука, вибрации и датчика
    private var soundManager: SoundManager? = null
    private var vibrationManager: VibrationManager? = null
    private var slapDetector: SlapDetector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Включение режима "от края до края"

        // Привязка кнопок громкости к медиа-потоку
        volumeControlStream = AudioManager.STREAM_MUSIC
        
        // Инициализация менеджеров
        soundManager = SoundManager(this)
        vibrationManager = VibrationManager(this)

        setContent {
            val viewModel: SlapViewModel = viewModel()
            
            // Инициализация детектора шлепков один раз при запуске
            LaunchedEffect(viewModel) {
                slapDetector = SlapDetector(
                    context = this@MainActivity,
                    onSlap = { force -> onSlapDetected(viewModel, force) }
                ) { force -> viewModel.impactForceState.floatValue = force }.apply {
                    threshold = viewModel.thresholdState.floatValue
                    start()
                }
            }

            // Синхронизация порога чувствительности между ViewModel и детектором
            LaunchedEffect(viewModel.thresholdState.floatValue) {
                slapDetector?.threshold = viewModel.thresholdState.floatValue
            }

            ShlepaTheme {
                MainScreen(viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Освобождение ресурсов при закрытии приложения
        slapDetector?.stop()
        soundManager?.release()
    }

    /**
     * Обработка зафиксированного шлепка.
     */
    private fun onSlapDetected(viewModel: SlapViewModel, force: Float) {
        // Определяем, "сильный" удар или нет (в 2 раза больше порога)
        val isStrong = force > (viewModel.thresholdState.floatValue * 2f)
        
        soundManager?.playSlap(isStrong)
        vibrationManager?.vibrate(isStrong)
        
        viewModel.addSlap(force)
        viewModel.isSlappingState.value = true // Триггер для анимации
    }

    /**
     * Эмуляция шлепка при нажатии на тестовые кнопки.
     */
    fun playManualSound(viewModel: SlapViewModel, index: Int) {
        val baseForce = viewModel.thresholdState.floatValue
        val fakeForce = baseForce * (1.1f + index * 0.3f)
        
        viewModel.impactForceState.floatValue = fakeForce
        
        soundManager?.playSound(index)
        vibrationManager?.vibrate(index >= 2)
        
        viewModel.addSlap(fakeForce)
        viewModel.isSlappingState.value = true // Триггер для анимации
    }
}

/**
 * Основной экран приложения.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: SlapViewModel) {
    var showMenu by remember { mutableStateOf(value = false) }
    var showAboutDialog by remember { mutableStateOf(value = false) }
    val activity = (LocalContext.current as? MainActivity)

    // Диалог "О приложении"
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(stringResource(R.string.about_title)) },
            text = { Text(stringResource(R.string.about_text)) },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    // Сброс состояния анимации шлепка через 150мс
    LaunchedEffect(viewModel.isSlappingState.value) {
        if (viewModel.isSlappingState.value) {
            delay(150.milliseconds)
            viewModel.isSlappingState.value = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                actions = {
                    // Кнопка сброса статистики
                    IconButton(onClick = { viewModel.reset() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.reset))
                    }
                    // Кнопка меню
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.about_menu_item)) },
                            onClick = { 
                                showMenu = false 
                                showAboutDialog = true
                            },
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        SlapScreen(
            impactForce = viewModel.impactForceState.floatValue,
            maxForce = viewModel.maxForceState.floatValue,
            threshold = viewModel.thresholdState.floatValue,
            isSlapping = viewModel.isSlappingState.value,
            slapCount = viewModel.slapCountState.intValue,
            history = viewModel.slapHistory,
            onThresholdChange = { viewModel.thresholdState.floatValue = it },
            onPlaySound = { index -> activity?.playManualSound(viewModel, index) },
            modifier = Modifier.padding(innerPadding)
        )
    }
}

/**
 * Карточка для отображения статистики (счетчик, рекорд).
 */
@Composable
fun StatCard(label: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall)
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Компоновка контента экрана шлепков.
 */
@Composable
fun SlapScreen(
    impactForce: Float,
    maxForce: Float,
    threshold: Float,
    isSlapping: Boolean,
    slapCount: Int,
    history: List<Float>,
    onThresholdChange: (Float) -> Unit,
    onPlaySound: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Анимация цвета текста при шлепке
    val textColor by animateColorAsState(
        targetValue = if (isSlapping) Color.Red else MaterialTheme.colorScheme.onBackground,
        animationSpec = tween(durationMillis = 100), label = "textColor"
    )

    // Анимация масштаба при шлепке
    val scale by animateFloatAsState(
        targetValue = if (isSlapping) 1.5f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessLow), label = "scale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Верхняя панель статистики
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCard(stringResource(R.string.stat_slaps), slapCount.toString())
            StatCard(stringResource(R.string.stat_record), String.format(Locale.getDefault(), "%.1f", maxForce))
        }

        // Центральный блок с текущей силой удара
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = stringResource(R.string.last_impact_force), style = MaterialTheme.typography.titleMedium)
            Text(
                text = String.format(Locale.getDefault(), "%.2f", impactForce),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor,
                    fontSize = 80.sp
                ),
                modifier = Modifier.scale(scale)
            )
            
            // Надпись "ШЛЁП!"
            if (isSlapping) {
                Text(
                    text = stringResource(R.string.slap_exclamation),
                    color = Color.Red,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.scale(scale)
                )
            }
        }

        // Список истории последних ударов
        if (history.isNotEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = stringResource(R.string.history_label), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    history.forEach { force ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f", force),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        // Нижний блок: кнопки тестирования и настройка порога
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = { onPlaySound(0) }, modifier = Modifier.weight(1f)) { Text("Кнопка 1") }
                    Button(onClick = { onPlaySound(1) }, modifier = Modifier.weight(1f)) { Text("Кнопка 2") }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = { onPlaySound(2) }, modifier = Modifier.weight(1f)) { Text("Кнопка 3") }
                    Button(onClick = { onPlaySound(3) }, modifier = Modifier.weight(1f)) { Text("Кнопка 4") }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.sensitivity_threshold, threshold),
                style = MaterialTheme.typography.bodyLarge
            )
            Slider(
                value = threshold,
                onValueChange = onThresholdChange,
                valueRange = 5f..50f,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
