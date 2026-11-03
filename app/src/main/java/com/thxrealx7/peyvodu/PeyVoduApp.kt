package com.thxrealx7.peyvodu

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thxrealx7.peyvodu.data.AppState
import com.thxrealx7.peyvodu.data.DayTotal
import com.thxrealx7.peyvodu.data.SettingsRepository
import com.thxrealx7.peyvodu.ui.theme.accentFor
import kotlinx.coroutines.delay
import java.time.LocalDate
import kotlin.math.roundToInt

enum class MainTab { Today, Facts, Voices, Settings }

private data class Fact(val title: String, val body: String, val plus: Boolean = false)

private val freeFacts = listOf(
    Fact("Почему не стоит пить всё сразу", "Удобнее распределять жидкость в течение дня небольшими порциями, а не пытаться закрыть всю дневную цель за один раз."),
    Fact("Вода и жаркая погода", "В жаркую погоду потребность в жидкости может меняться. При медицинских ограничениях ориентируйтесь на рекомендации врача."),
    Fact("После физической активности", "После нагрузки удобнее постепенно восполнять жидкость и ориентироваться на самочувствие."),
    Fact("Жидкость поступает не только из воды", "Часть жидкости человек получает из пищи и других напитков, поэтому цель в приложении является ориентировочной."),
    Fact("Когда цель нужно менять", "Если врач рекомендовал ограничить объём жидкости, цель приложения нужно настроить в соответствии с этой рекомендацией.")
)

private val plusFacts = listOf(
    Fact("Как распределить цель на день", "Можно разбить дневной ориентир на несколько небольших приёмов и привязать их к привычному распорядку.", true),
    Fact("Почему напоминания лучше делать редкими", "Слишком частые уведомления быстро начинают игнорироваться. Интервал должен оставаться удобным для пользователя.", true),
    Fact("Стаканы вместо миллилитров", "Для части пользователей привычный стакан понятнее цифр в миллилитрах. Приложение может считать объём автоматически.", true),
    Fact("Как менять дневную цель", "Дневная цель остаётся редактируемой и не должна восприниматься как медицинское назначение.", true),
    Fact("Напоминания в поездках", "В дороге можно увеличить интервал и оставить только самые важные напоминания.", true),
    Fact("Зачем смотреть историю", "Недельная история помогает увидеть привычку без превращения приложения в медицинский прибор.", true)
)

@Composable
fun PeyVoduApp(vm: PeyVoduViewModel, state: AppState) {
    val context = LocalContext.current
    val cooldown by vm.cooldownSeconds.collectAsState()
    var tab by rememberSaveable { mutableStateOf(MainTab.Today) }
    var showWelcome by rememberSaveable { mutableStateOf(false) }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(state.onboardingDone) {
        if (state.onboardingDone) {
            if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            showWelcome = true
            delay(1450)
            showWelcome = false
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (!state.onboardingDone) {
            Onboarding(vm)
        } else {
            MainShell(
                state = state,
                cooldown = cooldown,
                tab = tab,
                onTab = { tab = it },
                vm = vm
            )
        }

        AnimatedVisibility(visible = showWelcome && state.onboardingDone) {
            WelcomeOverlay(name = state.name, progress = state.progress)
        }
    }
}

@Composable
private fun Onboarding(vm: PeyVoduViewModel) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("Дмитрий") }
    var age by rememberSaveable { mutableStateOf("35") }
    var gender by rememberSaveable { mutableStateOf("m") }
    var height by rememberSaveable { mutableStateOf("175") }
    var weight by rememberSaveable { mutableStateOf("70") }
    var mode by rememberSaveable { mutableStateOf("simple") }
    var glass by rememberSaveable { mutableStateOf("250") }
    var voice by rememberSaveable { mutableStateOf("calm") }
    var photoChosen by rememberSaveable { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        photoChosen = uri != null
        if (uri != null) glass = "250"
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 52.dp)
    ) {
        BrandHeader()
        Spacer(Modifier.height(22.dp))
        StepBar(step)
        Spacer(Modifier.height(26.dp))

        when (step) {
            0 -> {
                SectionTitle("Расскажите о себе", "Данные нужны для обращения и ориентировочной дневной цели.")
                AppTextField(name, { name = it }, "Имя")
                AppTextField(age, { age = it.filter(Char::isDigit) }, "Возраст")
                Text("Пол", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 7.dp, bottom = 14.dp)) {
                    ChoiceChip("Мужской", gender == "m", Modifier.weight(1f)) { gender = "m" }
                    ChoiceChip("Женский", gender == "f", Modifier.weight(1f)) { gender = "f" }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) { AppTextField(height, { height = it.filter(Char::isDigit) }, "Рост, см") }
                    Box(Modifier.weight(1f)) { AppTextField(weight, { weight = it.filter(Char::isDigit) }, "Вес, кг") }
                }
                PrimaryButton("Продолжить") { step = 1 }
            }
            1 -> {
                SectionTitle("Выберите интерфейс", "Его можно поменять позже в настройках.")
                ModeCard("Упрощённый", "Крупные элементы, стаканы вместо миллилитров и минимум лишних действий.", mode == "simple") { mode = "simple" }
                Spacer(Modifier.height(10.dp))
                ModeCard("Полный", "Точная статистика, объёмы в миллилитрах и настройка цветовой темы.", mode == "full") { mode = "full" }
                Spacer(Modifier.height(18.dp))
                PrimaryButton("Продолжить") { step = 2 }
            }
            2 -> {
                SectionTitle("Ваш обычный стакан", "Укажите объём вручную или выберите фотографию стакана.")
                GlassPhotoCard(photoChosen) {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                AppTextField(glass, { glass = it.filter(Char::isDigit) }, "Объём стакана, мл")
                Text(
                    "Оценка объёма по фотографии в этой версии демонстрационная. Значение всегда можно исправить вручную.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                PrimaryButton("Продолжить") { step = 3 }
            }
            else -> {
                SectionTitle("Выберите голос", "Первый голос уже встроен в приложение и работает без интернета.")
                VoiceCard("Спокойный", "Записанный голос. Полностью офлайн.", voice == "calm", false) { voice = "calm" }
                Spacer(Modifier.height(10.dp))
                VoiceCard("Энергичный", "Временная более быстрая версия того же офлайн-голоса.", voice == "active", false) { voice = "active" }
                Spacer(Modifier.height(18.dp))
                PrimaryButton("Начать") {
                    vm.completeOnboarding(
                        name = name.ifBlank { "Пользователь" },
                        age = age.toIntOrNull() ?: 35,
                        gender = gender,
                        height = height.toIntOrNull() ?: 170,
                        weight = weight.toIntOrNull() ?: 70,
                        mode = mode,
                        glassMl = glass.toIntOrNull()?.coerceIn(50, 1500) ?: 250,
                        voice = voice
                    )
                }
            }
        }
    }
}

@Composable
private fun MainShell(state: AppState, cooldown: Int, tab: MainTab, onTab: (MainTab) -> Unit, vm: PeyVoduViewModel) {
    Box(Modifier.fillMaxSize()) {
        when (tab) {
            MainTab.Today -> TodayScreen(state, cooldown, vm)
            MainTab.Facts -> FactsScreen(state)
            MainTab.Voices -> VoicesScreen(state, vm)
            MainTab.Settings -> SettingsScreen(state, vm)
        }
        GlassBottomBar(tab, onTab, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 18.dp, end = 18.dp, top = 52.dp, bottom = 118.dp),
        content = content
    )
}

@Composable
private fun TodayScreen(state: AppState, cooldown: Int, vm: PeyVoduViewModel) {
    var customDialog by remember { mutableStateOf(false) }
    ScreenColumn {
        PageHeader("Сегодня", state.name, Icons.Outlined.Person)
        Spacer(Modifier.height(18.dp))

        if (state.isSimple) {
            GlassCard {
                StatusPill("Напоминания ${if (state.remindersEnabled) "активны" else "выключены"}")
                Spacer(Modifier.height(14.dp))
                AnimatedWaterGlass(progress = state.progress, modifier = Modifier.align(Alignment.CenterHorizontally).size(190.dp))
                Text(
                    "${state.glassesDone} из ${state.glassesGoal} стаканов",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "Один стакан: ${state.glassMl} мл",
                    modifier = Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 18.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { vm.addWater(state.glassMl) },
                    enabled = cooldown == 0,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(if (state.gender == "f") "Я выпила стакан" else "Я выпил стакан", fontWeight = FontWeight.Bold)
                }
                if (cooldown > 0) {
                    Text("Повторно можно отметить через $cooldown сек.", Modifier.fillMaxWidth().padding(top = 9.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            GlassCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Column {
                        Text("Выпито сегодня", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                        Text("${state.waterMl} мл", fontSize = 39.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Ориентир", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                        Text("${state.goalMl} мл", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(18.dp))
                CircularWaterProgress(state.progress, Modifier.align(Alignment.CenterHorizontally).size(190.dp))
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button({ vm.addWater(250) }, enabled = cooldown == 0, modifier = Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(18.dp)) { Text("+ 250 мл") }
                    OutlinedButton({ customDialog = true }, enabled = cooldown == 0, modifier = Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(18.dp)) { Text("Другой объём") }
                }
                if (cooldown > 0) Text("Повторно можно отметить через $cooldown сек.", Modifier.fillMaxWidth().padding(top = 9.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (state.plusEnabled) {
                Spacer(Modifier.height(14.dp))
                WeeklyStats(state.history, state.waterMl, state.goalMl)
            }
        }

        Spacer(Modifier.height(18.dp))
        Text("Полезно знать", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        GlassCard {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                IconTile(Icons.Outlined.Info)
                Column {
                    Text("Защита от случайных нажатий", fontWeight = FontWeight.Bold)
                    Text("После отметки воды кнопка блокируется на 10 секунд, чтобы дневной прогресс нельзя было случайно накрутить.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    if (customDialog) {
        var amount by remember { mutableStateOf("300") }
        AlertDialog(
            onDismissRequest = { customDialog = false },
            title = { Text("Сколько выпито?") },
            text = { AppTextField(amount, { amount = it.filter(Char::isDigit) }, "Объём, мл") },
            confirmButton = {
                Button(onClick = {
                    amount.toIntOrNull()?.takeIf { it > 0 }?.let(vm::addWater)
                    customDialog = false
                }) { Text("Добавить") }
            },
            dismissButton = { TextButton({ customDialog = false }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun FactsScreen(state: AppState) {
    val facts = if (state.plusEnabled) freeFacts + plusFacts else freeFacts
    ScreenColumn {
        PageHeader("Библиотека", "Факты о воде", Icons.Outlined.Info)
        Spacer(Modifier.height(14.dp))
        Text(
            if (state.plusEnabled) "PLUS активен: доступна расширенная офлайн-библиотека." else "В бесплатной версии доступны пять материалов без рекламы внутри раздела.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        facts.forEach { fact ->
            ExpandableFact(fact)
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun VoicesScreen(state: AppState, vm: PeyVoduViewModel) {
    ScreenColumn {
        PageHeader("Напоминания", "Голоса", Icons.Outlined.VolumeUp)
        Spacer(Modifier.height(18.dp))
        Text("Бесплатные", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        VoiceCard("Спокойный", "Записанный офлайн-голос.", state.selectedVoice == "calm", false) { vm.setVoice("calm") }
        Spacer(Modifier.height(10.dp))
        VoiceCard("Энергичный", "Более быстрый вариант текущей записи.", state.selectedVoice == "active", false) { vm.setVoice("active") }
        Spacer(Modifier.height(10.dp))
        OutlinedButton({ vm.previewVoice() }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Icon(Icons.Outlined.VolumeUp, null)
            Spacer(Modifier.width(8.dp))
            Text("Прослушать пример")
        }

        Spacer(Modifier.height(24.dp))
        Text("PLUS", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        PremiumVoicePlaceholder("Мама", state.plusEnabled)
        Spacer(Modifier.height(10.dp))
        PremiumVoicePlaceholder("Механический", state.plusEnabled)
        Spacer(Modifier.height(10.dp))
        PremiumVoicePlaceholder("Строгий", state.plusEnabled)
        Spacer(Modifier.height(10.dp))
        PremiumVoicePlaceholder("Мягкий", state.plusEnabled)
    }
}

@Composable
private fun SettingsScreen(state: AppState, vm: PeyVoduViewModel) {
    var profileDialog by remember { mutableStateOf(false) }
    var glassDialog by remember { mutableStateOf(false) }
    var plusDialog by remember { mutableStateOf(false) }
    var resetDialog by remember { mutableStateOf(false) }

    ScreenColumn {
        PageHeader("Параметры", "Настройки", Icons.Outlined.Settings)
        Spacer(Modifier.height(16.dp))

        GlassCard {
            Text("Интерфейс", fontSize = 19.sp, fontWeight = FontWeight.Bold)
            SettingRow("Полный дизайн", "В полном режиме доступно больше настроек") {
                Switch(checked = !state.isSimple, onCheckedChange = { vm.setMode(if (it) "full" else "simple") })
            }
            if (!state.isSimple) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .55f))
                Column(Modifier.padding(top = 12.dp)) {
                    Text("Цвет темы", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(9.dp))
                    ThemeDots(state.theme) { vm.setTheme(it) }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        GlassCard {
            Text("Профиль", fontSize = 19.sp, fontWeight = FontWeight.Bold)
            SettingRow(state.name, "${state.age} лет · ${state.heightCm} см · ${state.weightKg} кг · цель ${state.goalMl} мл") {
                SmallAction("Изменить") { profileDialog = true }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .55f))
            SettingRow("Обычный стакан", "${state.glassMl} мл") {
                SmallAction("Настроить") { glassDialog = true }
            }
        }

        Spacer(Modifier.height(12.dp))
        GlassCard {
            Text("Напоминания", fontSize = 19.sp, fontWeight = FontWeight.Bold)
            SettingRow("Напоминания", "Локально, без интернета") {
                Switch(
                    checked = state.remindersEnabled,
                    onCheckedChange = { checked -> vm.setReminders(checked) }
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .55f))
            SettingRow("Голосовые фразы", "Отдельно от звука системного уведомления") {
                Switch(
                    checked = state.voiceRemindersEnabled,
                    onCheckedChange = { checked -> vm.setVoiceReminders(checked) }
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .55f))
            Text("Интервал", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(60, 90, 120, 180).forEach { min ->
                    FilterChip(
                        selected = state.reminderIntervalMin == min,
                        onClick = { vm.setInterval(min) },
                        label = { Text(if (min < 120) "$min мин" else "${min / 60} ч") }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(vm::testNotification, Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Icon(Icons.Outlined.Notifications, null)
                Spacer(Modifier.width(8.dp))
                Text("Проверить тихое уведомление")
            }
        }

        Spacer(Modifier.height(12.dp))
        PlusCard(state.plusEnabled) { plusDialog = true }

        Spacer(Modifier.height(12.dp))
        TextButton(onClick = { resetDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Сбросить тестовые данные", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (profileDialog) ProfileDialog(state, onDismiss = { profileDialog = false }, onSave = { n,a,g,h,w,goal ->
        vm.updateProfile(n,a,g,h,w,goal); profileDialog = false
    })
    if (glassDialog) GlassVolumeDialog(state.glassMl, { glassDialog = false }) { vm.setGlass(it); glassDialog = false }
    if (plusDialog) PlusDialog(state.plusEnabled, { plusDialog = false }) { plan -> vm.activatePlus(plan); plusDialog = false }
    if (resetDialog) AlertDialog(
        onDismissRequest = { resetDialog = false },
        title = { Text("Сбросить приложение?") },
        text = { Text("Профиль, прогресс и тестовый PLUS будут удалены. Это кнопка только для первой тестовой версии.") },
        confirmButton = { Button({ vm.resetDemo(); resetDialog = false }) { Text("Сбросить") } },
        dismissButton = { TextButton({ resetDialog = false }) { Text("Отмена") } }
    )
}

@Composable
private fun WelcomeOverlay(name: String, progress: Float) {
    val fill by animateFloatAsState(targetValue = .78f.coerceAtLeast(progress), animationSpec = tween(1100), label = "welcomeFill")
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, Color(0xFF55AFFF)))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedWaterGlass(progress = fill, modifier = Modifier.size(145.dp), light = true)
            Spacer(Modifier.height(20.dp))
            Text("Здравствуйте, $name", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Text("Пусть вода будет простой привычкой", color = Color.White.copy(alpha = .82f), modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun BrandHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, Color(0xFF57B4FF)))),
            contentAlignment = Alignment.Center
        ) { MiniGlassLogo() }
        Spacer(Modifier.width(11.dp))
        Column {
            Text("Пей воду", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Text("Ваш ритм. Ваш стакан.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}

@Composable
private fun MiniGlassLogo() {
    Canvas(Modifier.size(24.dp)) {
        val stroke = 2.dp.toPx()
        drawRoundRect(Color.White, topLeft = Offset(size.width*.22f,size.height*.14f), size = Size(size.width*.56f,size.height*.7f), cornerRadius = CornerRadius(4.dp.toPx()), style = Stroke(stroke))
        drawRoundRect(Color.White.copy(alpha=.92f), topLeft = Offset(size.width*.27f,size.height*.52f), size = Size(size.width*.46f,size.height*.27f), cornerRadius = CornerRadius(3.dp.toPx()))
    }
}

@Composable
private fun StepBar(step: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(4) { i ->
            Box(Modifier.weight(1f).height(5.dp).clip(CircleShape).background(if (i <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha=.5f)))
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Text(title, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 33.sp)
    Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))
}

@Composable
private fun AppTextField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(17.dp)
    )
}

@Composable
private fun ChoiceChip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha=.10f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
    ) { Text(text, Modifier.padding(14.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun ModeCard(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha=.90f),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top=4.dp))
            }
            if (selected) Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun GlassPhotoCard(photoChosen: Boolean, onPick: () -> Unit) {
    GlassCard {
        Icon(Icons.Outlined.PhotoCamera, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
        Spacer(Modifier.height(10.dp))
        Text(if (photoChosen) "Фотография выбрана" else "Добавить фото стакана", fontWeight = FontWeight.Bold)
        Text(if (photoChosen) "Демо-оценка: около 250 мл. Исправьте объём ниже при необходимости." else "Фотография нужна только для проверки будущего сценария определения объёма.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top=4.dp,bottom=12.dp))
        OutlinedButton(onPick, Modifier.fillMaxWidth(), shape = RoundedCornerShape(17.dp)) { Text(if (photoChosen) "Выбрать другое фото" else "Выбрать фото") }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Button(onClick, Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(19.dp)) { Text(text, fontWeight = FontWeight.Bold) }
}

@Composable
private fun PageHeader(kicker: String, title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(kicker, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Text(title, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)
        }
        Surface(shape = RoundedCornerShape(17.dp), color = Color.White.copy(alpha=.76f), shadowElevation = 6.dp) {
            Icon(icon, null, Modifier.padding(12.dp).size(22.dp))
        }
    }
}

@Composable
private fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha=.78f),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha=.95f)),
        shadowElevation = 10.dp
    ) { Column(Modifier.padding(18.dp), content = content) }
}

@Composable
private fun StatusPill(text: String) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.onSurface.copy(alpha=.055f)) {
        Row(Modifier.padding(horizontal=11.dp, vertical=7.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF35B978)))
            Spacer(Modifier.width(7.dp))
            Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AnimatedWaterGlass(progress: Float, modifier: Modifier = Modifier, light: Boolean = false) {
    val p by animateFloatAsState(progress.coerceIn(0f,1f), animationSpec = tween(750), label = "glassFill")
    val outline = if (light) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha=.78f)
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val left = w*.25f
        val right = w*.75f
        val top = h*.09f
        val bottom = h*.90f
        val cupW = right-left
        val cupH = bottom-top
        val stroke = 4.dp.toPx()

        drawRoundRect(
            color = outline,
            topLeft = Offset(left,top),
            size = Size(cupW,cupH),
            cornerRadius = CornerRadius(14.dp.toPx(),24.dp.toPx()),
            style = Stroke(stroke)
        )

        val fillHeight = (cupH - stroke*2) * p.coerceAtLeast(.06f)
        val waterTop = bottom - stroke - fillHeight
        val waterColor = if (light) Color.White.copy(alpha=.88f) else Color(0xFF57B2FF)
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(waterColor.copy(alpha=.72f), waterColor), startY=waterTop, endY=bottom),
            topLeft = Offset(left+stroke,waterTop),
            size = Size(cupW-stroke*2, fillHeight),
            cornerRadius = CornerRadius(8.dp.toPx(),16.dp.toPx())
        )
        drawLine(
            color = Color.White.copy(alpha=.45f),
            start = Offset(left+stroke+8.dp.toPx(), top+14.dp.toPx()),
            end = Offset(left+stroke+8.dp.toPx(), bottom-20.dp.toPx()),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun CircularWaterProgress(progress: Float, modifier: Modifier = Modifier) {
    val p by animateFloatAsState(progress.coerceIn(0f,1f), animationSpec = tween(650), label = "circle")
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha=.35f)
    val primaryColor = MaterialTheme.colorScheme.primary
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            drawArc(outlineColor, -90f, 360f, false, style=Stroke(stroke, cap=StrokeCap.Round))
            drawArc(primaryColor, -90f, 360f*p, false, style=Stroke(stroke, cap=StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedWaterGlass(progress=p, modifier=Modifier.size(70.dp))
            Text("${(p*100).roundToInt()}%", fontSize=25.sp, fontWeight=FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun WeeklyStats(history: List<DayTotal>, todayMl: Int, goal: Int) {
    val rows = (history.takeLast(6) + DayTotal(LocalDate.now().toString(), todayMl)).takeLast(7)
    GlassCard {
        Text("Статистика PLUS", fontSize=19.sp, fontWeight=FontWeight.Bold)
        Text("Последние дни", color=MaterialTheme.colorScheme.onSurfaceVariant, style=MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().height(105.dp), horizontalArrangement=Arrangement.spacedBy(7.dp), verticalAlignment=Alignment.Bottom) {
            val padded = List((7-rows.size).coerceAtLeast(0)) { DayTotal("",0) } + rows
            padded.forEach { day ->
                val ratio=(day.ml.toFloat()/goal.coerceAtLeast(1)).coerceIn(0f,1f)
                Column(Modifier.weight(1f), horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.Bottom) {
                    Box(Modifier.width(19.dp).height((18+70*ratio).dp).clip(RoundedCornerShape(8.dp)).background(if(day.ml>0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha=.3f)))
                    Spacer(Modifier.height(5.dp))
                    Text(if(day.date.isBlank()) "" else day.date.takeLast(2), fontSize=9.sp, color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun IconTile(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(shape=RoundedCornerShape(15.dp), color=MaterialTheme.colorScheme.primary.copy(alpha=.09f)) {
        Icon(icon,null,Modifier.padding(11.dp).size(22.dp), tint=MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ExpandableFact(fact: Fact) {
    var open by remember { mutableStateOf(false) }
    GlassCard {
        Row(Modifier.fillMaxWidth().clickable { open=!open }, verticalAlignment=Alignment.CenterVertically) {
            IconTile(Icons.Outlined.Info)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(fact.title, fontWeight=FontWeight.Bold)
                Text(if(fact.plus) "PLUS-материал" else "Офлайн-материал", fontSize=11.sp, color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(if(open) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null)
        }
        AnimatedVisibility(open) {
            Text(fact.body, color=MaterialTheme.colorScheme.onSurfaceVariant, modifier=Modifier.padding(top=12.dp))
        }
    }
}

@Composable
private fun VoiceCard(title: String, subtitle: String, selected: Boolean, locked: Boolean, onClick: () -> Unit) {
    Surface(
        modifier=Modifier.fillMaxWidth().clickable(enabled=!locked,onClick=onClick),
        shape=RoundedCornerShape(23.dp),
        color=Color.White.copy(alpha=.78f),
        border=BorderStroke(if(selected) 2.dp else 1.dp, if(selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
        shadowElevation=5.dp
    ) {
        Row(Modifier.padding(17.dp), verticalAlignment=Alignment.CenterVertically) {
            IconTile(if(locked) Icons.Outlined.Lock else Icons.Outlined.VolumeUp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title,fontWeight=FontWeight.Bold)
                Text(subtitle,color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)
            }
            if(selected) Icon(Icons.Outlined.CheckCircle,null,tint=MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun PremiumVoicePlaceholder(title: String, plusEnabled: Boolean) {
    VoiceCard(title, if(plusEnabled) "PLUS активен, но аудиофайл этого голоса ещё не добавлен." else "Доступно по подписке PLUS.", false, true) {}
}

@Composable
private fun SettingRow(title: String, subtitle: String, action: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical=13.dp), verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title,fontWeight=FontWeight.SemiBold)
            Text(subtitle,color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.width(10.dp))
        action()
    }
}

@Composable
private fun SmallAction(text: String, onClick: () -> Unit) {
    OutlinedButton(onClick, contentPadding=PaddingValues(horizontal=12.dp,vertical=7.dp), shape=RoundedCornerShape(14.dp)) { Text(text,fontSize=12.sp) }
}

@Composable
private fun ThemeDots(selected: String, onSelect: (String) -> Unit) {
    val colors= listOf("purple" to Color(0xFF6D5DFC),"blue" to Color(0xFF268EF5),"amber" to Color(0xFFE4A528),"green" to Color(0xFF2EAE76))
    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        colors.forEach { (name,color) ->
            Box(
                Modifier.size(31.dp).clip(CircleShape).background(color).border(if(selected==name) 3.dp else 0.dp, if(selected==name) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape).clickable { onSelect(name) }
            )
        }
    }
}

@Composable
private fun PlusCard(enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier=Modifier.fillMaxWidth(),
        shape=RoundedCornerShape(28.dp),
        color=Color(0xFF1A1E29),
        shadowElevation=10.dp
    ) {
        Column(Modifier.padding(19.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text("Пей воду PLUS",color=Color.White,fontSize=20.sp,fontWeight=FontWeight.ExtraBold,modifier=Modifier.weight(1f))
                if(enabled) Surface(shape=CircleShape,color=Color.White.copy(alpha=.12f)){Text("АКТИВЕН",color=Color.White,fontSize=10.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=9.dp,vertical=5.dp))}
            }
            Text(if(enabled) "В тестовой версии PLUS уже включён." else "Расширенная библиотека, статистика и будущие дополнительные голоса.",color=Color.White.copy(alpha=.72f),modifier=Modifier.padding(top=7.dp,bottom=14.dp))
            Button(onClick,Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Color.White,contentColor=Color(0xFF1A1E29)),shape=RoundedCornerShape(18.dp)) { Text(if(enabled) "Настроить PLUS" else "Обновить",fontWeight=FontWeight.Bold) }
        }
    }
}

@Composable
private fun ProfileDialog(state: AppState, onDismiss: () -> Unit, onSave: (String,Int,String,Int,Int,Int)->Unit) {
    var name by remember { mutableStateOf(state.name) }
    var age by remember { mutableStateOf(state.age.toString()) }
    var gender by remember { mutableStateOf(state.gender) }
    var height by remember { mutableStateOf(state.heightCm.toString()) }
    var weight by remember { mutableStateOf(state.weightKg.toString()) }
    var goal by remember { mutableStateOf(state.goalMl.toString()) }
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text("Профиль")},
        text={Column(Modifier.verticalScroll(rememberScrollState())){
            AppTextField(name,{name=it},"Имя")
            AppTextField(age,{age=it.filter(Char::isDigit)},"Возраст")
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                ChoiceChip("Мужской",gender=="m",Modifier.weight(1f)){gender="m"}
                ChoiceChip("Женский",gender=="f",Modifier.weight(1f)){gender="f"}
            }
            Spacer(Modifier.height(10.dp))
            AppTextField(height,{height=it.filter(Char::isDigit)},"Рост, см")
            AppTextField(weight,{weight=it.filter(Char::isDigit)},"Вес, кг")
            AppTextField(goal,{goal=it.filter(Char::isDigit)},"Дневной ориентир, мл")
            Text("Цель является ориентиром и может быть изменена вручную.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }},
        confirmButton={Button({onSave(name.ifBlank{"Пользователь"},age.toIntOrNull()?:35,gender,height.toIntOrNull()?:170,weight.toIntOrNull()?:70,goal.toIntOrNull()?:SettingsRepository.defaultGoal(weight.toIntOrNull()?:70))}){Text("Сохранить")}},
        dismissButton={TextButton(onDismiss){Text("Отмена")}}
    )
}

@Composable
private fun GlassVolumeDialog(current: Int, onDismiss: () -> Unit, onSave: (Int)->Unit) {
    var text by remember { mutableStateOf(current.toString()) }
    AlertDialog(onDismissRequest=onDismiss,title={Text("Обычный стакан")},text={AppTextField(text,{text=it.filter(Char::isDigit)},"Объём, мл")},confirmButton={Button({onSave((text.toIntOrNull()?:250).coerceIn(50,1500))}){Text("Сохранить")}},dismissButton={TextButton(onDismiss){Text("Отмена")}})
}

@Composable
private fun PlusDialog(alreadyEnabled: Boolean, onDismiss: () -> Unit, onActivate: (String)->Unit) {
    var plan by remember { mutableStateOf("1m") }
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text("Пей воду PLUS")},
        text={Column{
            Text(if(alreadyEnabled) "PLUS уже активен в тестовом режиме. Реальных платежей в первой версии нет." else "Для проверки приложения PLUS можно включить бесплатно. Реальная оплата появится позже.",color=MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            listOf("1m" to "1 месяц","6m" to "6 месяцев","1y" to "1 год").forEach { (id,title) ->
                ChoiceChip(title,plan==id,Modifier.fillMaxWidth().padding(bottom=7.dp)){plan=id}
            }
        }},
        confirmButton={Button({onActivate(plan)}){Text(if(alreadyEnabled)"Сохранить" else "Включить PLUS")}},
        dismissButton={TextButton(onDismiss){Text("Отмена")}}
    )
}

@Composable
private fun GlassBottomBar(selected: MainTab, onSelect: (MainTab)->Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier=modifier.padding(start=15.dp,end=15.dp,bottom=14.dp).fillMaxWidth().height(78.dp),
        color=Color.White.copy(alpha=.76f),
        shape=RoundedCornerShape(29.dp),
        border=BorderStroke(1.dp,Color.White.copy(alpha=.95f)),
        shadowElevation=18.dp
    ) {
        Row(Modifier.fillMaxSize().padding(7.dp),horizontalArrangement=Arrangement.SpaceAround,verticalAlignment=Alignment.CenterVertically) {
            BottomItem(MainTab.Today,"Сегодня",Icons.Outlined.Home,selected,onSelect)
            BottomItem(MainTab.Facts,"Факты",Icons.Outlined.Info,selected,onSelect)
            BottomItem(MainTab.Voices,"Голоса",Icons.Outlined.VolumeUp,selected,onSelect)
            BottomItem(MainTab.Settings,"Настройки",Icons.Outlined.Settings,selected,onSelect)
        }
    }
}

@Composable
private fun RowScope.BottomItem(tab: MainTab, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: MainTab, onSelect: (MainTab)->Unit) {
    val active=tab==selected
    Surface(
        modifier=Modifier.weight(1f).fillMaxHeight().clickable{onSelect(tab)},
        color=if(active) Color.White.copy(alpha=.88f) else Color.Transparent,
        shape=RoundedCornerShape(20.dp)
    ) {
        Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
            Icon(icon,null,Modifier.size(21.dp),tint=if(active)MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(3.dp))
            Text(label,fontSize=10.sp,fontWeight=if(active)FontWeight.Bold else FontWeight.Medium,color=if(active)MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
