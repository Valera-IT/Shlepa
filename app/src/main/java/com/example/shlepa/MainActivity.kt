package com.example.shlepa

import android.content.Context
import android.content.res.Configuration
import android.media.AudioManager
import android.os.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shlepa.ui.theme.ShlepaTheme
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    // Менеджеры для звука, вибрации и датчика
    private var soundManager: SoundManager? = null
    private var vibrationManager: VibrationManager? = null
    private var flashlightManager: FlashlightManager? = null
    private var slapDetector: SlapDetector? = null

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("shlepa_prefs", MODE_PRIVATE)
        val langCode = prefs.getString("language", null)
        val context = if (langCode != null) {
            val locale = Locale.forLanguageTag(langCode)
            Locale.setDefault(locale)
            val config = Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            config.setLocales(LocaleList(locale))
            // Для корректной работы ресурсов на всех версиях Android
            newBase.createConfigurationContext(config)
        } else {
            newBase
        }
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Включение режима "от края до края"

        // Привязка кнопок громкости к медиа-потоку
        volumeControlStream = AudioManager.STREAM_MUSIC
        
        // Инициализация менеджеров
        soundManager = SoundManager(this)
        vibrationManager = VibrationManager(this)
        flashlightManager = FlashlightManager(this)

        setContent {
            val viewModel: SlapViewModel = viewModel()
            val currentLanguage = viewModel.currentLanguageState.value
            
            // Обертка для поддержки динамической смены языка
            LocalizationWrapper(currentLanguage) {
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

                ShlepaTheme(appTheme = viewModel.currentThemeState.value) {
                    MainScreen(viewModel, soundManager, vibrationManager, flashlightManager)
                }
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
        val threshold = viewModel.thresholdState.floatValue
        // Определяем, "сильный" удар или нет (в 2 раза больше порога)
        val isStrong = force > (threshold * 2f)
        // Очень сильный удар для вспышки (в 3 раза больше порога)
        val isVeryStrong = force > (threshold * 3f)
        
        // Пытаемся запустить или поставить звук в очередь
        val accepted = soundManager?.playSlap(isStrong) {
            // Эти действия произойдут в момент фактического начала звука (сразу или после очереди)
            val vibMode = if (isVeryStrong) 2 else if (isStrong) 1 else 0
            vibrationManager?.vibrate(vibMode)
            
            if (isVeryStrong) {
                flashlightManager?.flash()
            }
            viewModel.isSlappingState.value = true // Триггер для анимации
        } == true
        
        // Если звук принят (играет или в очереди), обновляем статистику
        if (accepted) {
            viewModel.addSlap(force)
        }
    }

    /**
     * Эмуляция шлепка при нажатии на тестовые кнопки.
     */
    fun playManualSound(viewModel: SlapViewModel, index: Int) {
        val baseForce = viewModel.thresholdState.floatValue
        val variation = Random.nextFloat() * 0.2f - 0.1f // разброс +- 10%
        val fakeForce = baseForce * (1.1f + index * 0.3f) * (1f + variation)
        
        viewModel.impactForceState.floatValue = fakeForce
        
        val accepted = soundManager?.playSound(index) {
            val vibMode = if (index >= 3) 2 else if (index >= 2) 1 else 0
            vibrationManager?.vibrate(vibMode)
            
            if (index >= 3) { // Для самых сильных тестовых звуков мигаем вспышкой
                flashlightManager?.flash()
            }
            viewModel.isSlappingState.value = true
        } == true
        
        if (accepted) {
            viewModel.addSlap(fakeForce)
        }
    }
}

/**
 * Обертка для динамической смены языка без перезагрузки Activity.
 */
@Composable
fun LocalizationWrapper(
    language: AppLanguage,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    
    val localizedContext = remember(language, configuration) {
        val locale = Locale.forLanguageTag(language.code)
        Locale.setDefault(locale)
        val newConfig = Configuration(configuration)
        newConfig.setLocale(locale)
        newConfig.setLocales(LocaleList(locale))
        newConfig.setLayoutDirection(locale)
        context.createConfigurationContext(newConfig)
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration
    ) {
        // Мы используем key(language), чтобы принудительно пересоздать всё дерево контента
        // при смене языка. Это гарантирует обновление всех stringResource.
        key(language) {
            content()
        }
    }
}

/**
 * Компонент меню настроек, который правильно обрабатывает смену языка.
 */
@Composable
fun SettingsMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    viewModel: SlapViewModel,
    currentLanguage: AppLanguage
) {
    // Считываем строки в начале функции, чтобы они обновлялись при смене языка
    val themeLabel = stringResource(R.string.settings_theme)
    val languageLabel = stringResource(R.string.settings_language)
    val sensitivityLabel = stringResource(R.string.settings_sensitivity)
    val aboutLabel = stringResource(R.string.about_menu_item)
    val okLabel = stringResource(R.string.ok)
    val aboutTitle = stringResource(R.string.about_title)
    val aboutText = stringResource(R.string.about_text)

    // Текст для чувствительности (обновляется при движении слайдера и смене языка)
    val sensitivityThresholdText = stringResource(
        R.string.sensitivity_threshold,
        viewModel.thresholdState.floatValue
    )

    // Заранее готовим локализованные названия тем
    val themeNames = AppTheme.entries.associateWith { theme ->
        stringResource(
            when (theme) {
                AppTheme.SYSTEM -> R.string.theme_auto
                AppTheme.LIGHT -> R.string.theme_light
                AppTheme.DARK -> R.string.theme_dark
                AppTheme.PINK -> R.string.theme_pink
                AppTheme.YELLOW -> R.string.theme_yellow
            }
        )
    }

    // Диалог "О приложении"
    var showAboutDialog by remember { mutableStateOf(false) }
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(aboutTitle) },
            text = { Text(aboutText) },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(okLabel)
                }
            }
        )
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        // Пункт выбора темы
        var showThemeMenu by remember { mutableStateOf(false) }
        DropdownMenuItem(
            text = { Text(themeLabel) },
            onClick = { showThemeMenu = true },
            trailingIcon = { Text(">", fontSize = 12.sp) }
        )

        // Пункт выбора языка
        var showLanguageMenu by remember { mutableStateOf(false) }
        DropdownMenuItem(
            text = { Text(languageLabel) },
            onClick = { showLanguageMenu = true },
            trailingIcon = { Text(">", fontSize = 12.sp) }
        )

        // Пункт настройки чувствительности
        var showSensitivityMenu by remember { mutableStateOf(false) }
        DropdownMenuItem(
            text = { Text(sensitivityLabel) },
            onClick = { showSensitivityMenu = true },
            trailingIcon = { Text(">", fontSize = 12.sp) }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Пункт "О приложении"
        DropdownMenuItem(
            text = { Text(aboutLabel) },
            onClick = {
                onDismissRequest()
                showAboutDialog = true
            },
        )

        // Подменю темы
        if (showThemeMenu) {
            AlertDialog(
                onDismissRequest = { showThemeMenu = false },
                title = { Text(themeLabel) },
                text = {
                    Column {
                        AppTheme.entries.forEach { theme ->
                            val label = themeNames[theme] ?: theme.name
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = viewModel.currentThemeState.value == theme,
                                    onClick = { viewModel.setTheme(theme) }
                                )
                                Text(text = label, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showThemeMenu = false
                        onDismissRequest()
                    }) {
                        Text(okLabel)
                    }
                }
            )
        }

        // Подменю языка
        if (showLanguageMenu) {
            AlertDialog(
                onDismissRequest = { showLanguageMenu = false },
                title = { Text(languageLabel) },
                text = {
                    Column {
                        AppLanguage.entries.forEach { language ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = viewModel.currentLanguageState.value == language,
                                    onClick = { 
                                        viewModel.setLanguage(language)
                                    }
                                )
                                Text(text = language.label, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showLanguageMenu = false
                        onDismissRequest()
                    }) {
                        Text(okLabel)
                    }
                }
            )
        }

        // Подменю чувствительности
        if (showSensitivityMenu) {
            AlertDialog(
                onDismissRequest = { showSensitivityMenu = false },
                title = { Text(sensitivityLabel) },
                text = {
                    Column {
                        Text(
                            text = sensitivityThresholdText,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Slider(
                            value = viewModel.thresholdState.floatValue,
                            onValueChange = { viewModel.setThreshold(it) },
                            valueRange = 5f..50f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showSensitivityMenu = false
                        onDismissRequest()
                    }) {
                        Text(okLabel)
                    }
                }
            )
        }
    }
}

/**
 * Основной экран приложения.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: SlapViewModel,
    soundManager: SoundManager?,
    vibrationManager: VibrationManager?,
    flashlightManager: FlashlightManager?
) {
    var showMenu by remember { mutableStateOf(value = false) }
    
    // Лямбда для безопасного вызова звука
    val onPlayManualSound: (Int) -> Unit = { index ->
        android.util.Log.d("MainScreen", "Button $index clicked, playing sound...")
        
        val baseForce = viewModel.thresholdState.floatValue
        val variation = Random.nextFloat() * 0.2f - 0.1f // разброс +- 10%
        val fakeForce = baseForce * (1.1f + index * 0.3f) * (1f + variation)
        
        viewModel.impactForceState.floatValue = fakeForce
        
        val accepted = soundManager?.playSound(index) {
            // Эффекты синхронизированы с началом звука
            val vibMode = if (index >= 3) 2 else if (index >= 2) 1 else 0
            vibrationManager?.vibrate(vibMode)
            
            if (index >= 3) {
                flashlightManager?.flash()
            }
            viewModel.isSlappingState.value = true
        } == true
        
        if (accepted) {
            viewModel.addSlap(fakeForce)
        }
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
                title = { 
                    Text(
                        stringResource(R.string.app_name), 
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                actions = {
                    // Кнопка сброса статистики
                    IconButton(onClick = { viewModel.reset() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.reset))
                    }
                    // Кнопка меню (Настройки)
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu))
                    }
                    
                    SettingsMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        viewModel = viewModel,
                        currentLanguage = viewModel.currentLanguageState.value
                    )
                }
            )
        }
    ) { innerPadding ->
        SlapScreen(
            impactForce = viewModel.impactForceState.floatValue,
            maxForce = viewModel.maxForceState.floatValue,
            isSlapping = viewModel.isSlappingState.value,
            slapCount = viewModel.slapCountState.intValue,
            history = viewModel.slapHistory,
            onPlaySound = onPlayManualSound,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

/**
 * Кнопка с анимацией нажатия (масштабирование).
 */
@Composable
fun AnimatedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "buttonScale"
    )

    Button(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        interactionSource = interactionSource,
        colors = if (containerColor != null) {
            ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = Color.White)
        } else {
            ButtonDefaults.buttonColors()
        },
        content = content
    )
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
    isSlapping: Boolean,
    slapCount: Int,
    history: List<Float>,
    onPlaySound: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Рассчитываем интенсивность удара для визуальных эффектов (0..1)
    val intensity = (impactForce / 50f).coerceIn(0f, 1f)

    // Анимация цвета текста при шлепке (золотой для рекорда)
    val textColor by animateColorAsState(
        targetValue = when {
            !isSlapping -> MaterialTheme.colorScheme.onBackground
            impactForce >= maxForce && maxForce > 0 -> Color(0xFFFFD700) // Золотой для рекорда
            impactForce > 30f -> MaterialTheme.colorScheme.primary // Цвет темы для сильного шлепка
            else -> MaterialTheme.colorScheme.secondary // Цвет темы для обычного шлепка
        },
        animationSpec = if (isSlapping) tween(durationMillis = 50) else tween(durationMillis = 300),
        label = "textColor"
    )

    // Анимация масштаба при шлепке (масштаб зависит от силы)
    val scale by animateFloatAsState(
        targetValue = if (isSlapping) 1.2f + (intensity * 0.8f) else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    // Эффект тряски (вращение)
    val rotation = remember(isSlapping) { if (isSlapping) (Random.nextFloat() * 20f - 10f) * intensity else 0f }
    val animatedRotation by animateFloatAsState(
        targetValue = rotation,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "rotation"
    )

    // Эффект тряски (смещение)
    val shakeOffset = remember(isSlapping) {
        if (isSlapping) Offset(
            (Random.nextFloat() * 50f - 25f) * intensity,
            (Random.nextFloat() * 50f - 25f) * intensity
        ) else Offset.Zero
    }
    val animatedShakeX by animateFloatAsState(
        targetValue = shakeOffset.x,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "shakeX"
    )
    val animatedShakeY by animateFloatAsState(
        targetValue = shakeOffset.y,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "shakeY"
    )

    // Вспышка фона при сильном ударе
    val flashColor by animateColorAsState(
        targetValue = if (isSlapping) textColor.copy(alpha = 0.15f) else Color.Transparent,
        animationSpec = tween(50),
        label = "flashColor"
    )

    Box(modifier = modifier.fillMaxSize().background(flashColor)) {
        // Эффект частиц (используем slapCount как ключ, чтобы эффект не прерывался)
        ParticleEffect(slapId = slapCount, color = textColor, particleCount = (15 + intensity * 30).toInt())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 16.dp)
                .graphicsLayer {
                    translationX = animatedShakeX
                    translationY = animatedShakeY
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Верхняя панель статистики
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard(stringResource(R.string.stat_slaps), slapCount.toString())
                StatCard(stringResource(R.string.stat_record), String.format(Locale.getDefault(), "%.1f", maxForce))
            }

            // Центральный блок с текущей силой удара
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    rotationZ = animatedRotation
                    scaleX = scale
                    scaleY = scale
                }
            ) {
                Text(
                    text = stringResource(R.string.last_impact_force),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = String.format(Locale.getDefault(), "%.1f", impactForce),
                    style = MaterialTheme.typography.displayLarge.copy(
                        color = textColor,
                        shadow = if (isSlapping) Shadow(
                            color = textColor.copy(alpha = 0.5f),
                            blurRadius = 20f,
                            offset = Offset(2f, 2f)
                        ) else null
                    )
                )
                
                // Надпись "ШЛЁП!"
                if (isSlapping) {
                    Text(
                        text = stringResource(R.string.slap_exclamation),
                        color = textColor,
                        style = MaterialTheme.typography.headlineLarge,
                    )
                }
            }

            // Список истории последних ударов
            if (history.isNotEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = stringResource(R.string.history_label), style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        modifier = Modifier.fillMaxWidth()
                    ) {
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

            // Нижний блок: кнопки тестирования
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnimatedButton(
                            onClick = { onPlaySound(0) },
                            modifier = Modifier.weight(1f),
                            containerColor = Color(0xFF4CAF50)
                        ) { Text(stringResource(R.string.btn_test_1)) }

                        AnimatedButton(
                            onClick = { onPlaySound(1) },
                            modifier = Modifier.weight(1f),
                            containerColor = Color(0xFF2196F3)
                        ) { Text(stringResource(R.string.btn_test_2)) }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnimatedButton(
                            onClick = { onPlaySound(2) },
                            modifier = Modifier.weight(1f),
                            containerColor = Color(0xFFFF9800)
                        ) { Text(stringResource(R.string.btn_test_3)) }

                        AnimatedButton(
                            onClick = { onPlaySound(3) },
                            modifier = Modifier.weight(1f),
                            containerColor = Color(0xFFF44336)
                        ) { Text(stringResource(R.string.btn_test_4)) }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Эффект разбитого экрана при очень сильном ударе
        BrokenScreenOverlay(isVisible = isSlapping && intensity > 0.8f, intensity = intensity)
    }
}
