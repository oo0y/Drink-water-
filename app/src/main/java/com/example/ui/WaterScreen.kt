package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.database.WaterLog
import com.example.data.database.WaterSettings
import com.example.ui.viewmodel.WaterViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WaterMainScreen(
    viewModel: WaterViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Collect Reactive Database State
    val settings by viewModel.settingsState.collectAsState()
    val todayLogs by viewModel.todayLogsState.collectAsState()
    val totalIntake by viewModel.totalIntakeMlState.collectAsState()
    val progress by viewModel.intakeProgressState.collectAsState()

    // Trigger Scheduler helper if isNotificationsEnabled is true on first launch
    LaunchedEffect(key1 = true) {
        viewModel.startSchedulerIfNeeded()
    }

    // Interactive tabs: Dashboard vs History & Tips
    var activeTab by remember { mutableStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            // Header
            WaterAppHeader()

            // Tab bar
            WaterTabBar(
                selectedTab = activeTab,
                onTabSelected = { activeTab = it }
            )

            // Content based on tab selection
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) with fadeOut(animationSpec = tween(220))
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { tab ->
                when (tab) {
                    0 -> DashboardTab(
                        progress = progress,
                        totalIntake = totalIntake,
                        settings = settings,
                        todayLogs = todayLogs,
                        viewModel = viewModel
                    )
                    1 -> SettingsAndTipsTab(
                        settings = settings,
                        viewModel = viewModel
                    )
                }
            }
        }

        // ==========================================
        // MANDATORY IMMERSIVE POPUP/OVERLAY SCREEN
        // "ولا تذهب إلا عند ضغط لقد شربت الماء أو تأكيد للغرض"
        // ==========================================
        if (settings.isReminderPending) {
            Dialog(
                onDismissRequest = {
                    // Do nothing - prevent dismissal by clicking outside or back button!
                    // This forces the user to interact with the overlay as requested!
                },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    usePlatformDefaultWidth = false // Make it full screen
                )
            ) {
                WaterUrgentReminderPopup(
                    settings = settings,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun WaterAppHeader() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.End // Beautiful RTL alignment
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "رَوَاء 💧",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Right
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "مستشارك الذكي للوقاية من الجفاف وشرب الماء بانتظام",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun WaterTabBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val tabs = listOf(
            Pair("الرئيسية والمتابعة  ", Icons.Rounded.WaterDrop),
            Pair("الإعدادات والنصائح ⚙️", Icons.Rounded.Settings)
        )

        tabs.forEachIndexed { index, pair ->
            val isSelected = selectedTab == index
            val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
            val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(backgroundColor)
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = pair.first,
                        color = contentColor,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = pair.second,
                        contentDescription = pair.first,
                        tint = contentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardTab(
    progress: Float,
    totalIntake: Int,
    settings: WaterSettings,
    todayLogs: List<WaterLog>,
    viewModel: WaterViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Water Visual Glass and Status
        item {
            WaterGlassProgressCard(
                progress = progress,
                totalIntake = totalIntake,
                target = settings.dailyTargetMl
            )
        }

        // Simple Countdown Alert / General Status Guide
        item {
            CountdownTimerCard(settings = settings)
        }

        // 2. Add Cups Shortcuts Panel
        item {
            WaterLogShortcutsCard(onAddWater = { viewModel.addIntake(it) })
        }

        // 3. Daily Log History Row Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (todayLogs.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.clearAllIntakes() },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red.copy(alpha = 0.8f))
                    ) {
                        Text("مسح السجل  ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Rounded.Delete, contentDescription = "Clear all", modifier = Modifier.size(14.dp))
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                Text(
                    text = "سجل شرب الماء اليوم",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // 4. Log List
        if (todayLogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = "Empty Log",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "لم تسجل أي كوب ماء اليوم بعد!\nاضغط على الأكواب أعلاه للبدء بالترطيب 💧",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        } else {
            items(todayLogs) { log ->
                WaterLogItem(log = log, onDelete = { viewModel.deleteLog(log.id) })
            }
            
            // Undo last insert
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = { viewModel.undoLastIntake() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("تراجع عن عملية الإدخال الأخيرة ↩️", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun WaterGlassProgressCard(
    progress: Float,
    totalIntake: Int,
    target: Int
) {
    // Elegant pulsing animation for the background
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, shape = RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "معدّل الارتواء اليومي",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Beautiful custom canvas circular progress representation resembling liquid filling
            Box(
                modifier = Modifier
                    .size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background water wave simulation circle with custom stroke
                val animatedProgress by animateFloatAsState(
                    targetValue = progress.coerceIn(0f, 1f),
                    animationSpec = tween(1000, easing = FastOutSlowInEasing)
                )

                val primaryColor = MaterialTheme.colorScheme.primary
                val secondaryColor = MaterialTheme.colorScheme.secondary

                Canvas(modifier = Modifier.size(160.dp)) {
                    // Draw outer background orbit
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.12f),
                        radius = size.width / 2,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw colored arc progress ring
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(primaryColor, secondaryColor, primaryColor)
                        ),
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Inside metadata
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Water,
                        contentDescription = "Water status",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(32.dp)
                            .graphicsLayer { scaleY = pulseAlpha; scaleX = pulseAlpha }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$totalIntake / $target مل",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Empathetic interactive feedback based on progress
            val feedbackText = when {
                progress >= 1.0f -> "رائع! مرتوٍ بالكامل، غسلت كليتيك وحميتها من الجفاف اليوم! 🎉👏"
                progress >= 0.7f -> "أنت في نطاق رائع! كوبين آخرين وتصل لهدفك المثالي 🌊"
                progress >= 0.4f -> "تقدّم رائع، استمر بشرب الكؤوس بانتظام لمقاومة الخمول 💪"
                totalIntake > 0 -> "بداية طيبة! لا تنتظر العطش الشديد حتى تشرب، العطش كذبة أولية للجفاف."
                else -> "⚠️ تعاني من خمول؟ ربما تكون جافاً! يُرجى شرب كوب ماء فوراً لحماية خلاياك."
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = feedbackText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun CountdownTimerCard(settings: WaterSettings) {
    // Estimating next timer
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val nextReminderTime = settings.lastReminderTimestamp + (settings.reminderIntervalMinutes * 60 * 1000L)
    val formattedTime = sdf.format(Date(nextReminderTime))

    val intervalTextAr = when (settings.reminderIntervalMinutes) {
        30 -> "نصف ساعة"
        60 -> "ساعة كاملة"
        90 -> "ساعة ونصف"
        120 -> "ساعتين"
        180 -> "3 ساعات"
        else -> "${settings.reminderIntervalMinutes} دقيقة"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, shape = RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Rounded.NotificationsActive,
                contentDescription = "Alert logo",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp)
            )

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(1f).padding(end = 6.dp)
            ) {
                Text(
                    text = "التذكير القادم: الساعة $formattedTime تقريباً ⏰",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "تنبيه إجباري منبثق من التطبيق كل $intervalTextAr",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun WaterLogShortcutsCard(onAddWater: (Int) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, shape = RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "سجل شربك السريع بكبسة زر واحدة 🥛",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Short choices of cups
                val cupOptions = listOf(
                    Triple("كوب صغير", 150, "🥛"),
                    Triple("كوب قياسي", 250, "💧"),
                    Triple("كأس كبير", 350, "🍵"),
                    Triple("زجاجة ماء", 500, "🍾")
                )

                cupOptions.forEach { (name, amount, emoji) ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .clickable { onAddWater(amount) }
                            .testTag("add_water_${amount}_ml"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(emoji, fontSize = 24.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "$amount مل",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WaterLogItem(
    log: WaterLog,
    onDelete: () -> Unit
) {
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(log.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, shape = RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Delete",
                    tint = Color.Red.copy(alpha = 0.6f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "كوب ماء (${log.amountMl} مل)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "تم التناول في: $formattedTime",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.Rounded.LocalCafe,
                    contentDescription = "Glass icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsAndTipsTab(
    settings: WaterSettings,
    viewModel: WaterViewModel
) {
    var targetInput by remember { mutableStateOf(settings.dailyTargetMl.toString()) }
    var expandedInterval by remember { mutableStateOf(false) }
    var notificationPermissionState by remember { mutableStateOf(false) }

    val intervalLabel = when (settings.reminderIntervalMinutes) {
        30 -> "كل 30 دقيقة (نصف ساعة)"
        60 -> "كل 60 دقيقة (ساعة كاملة)"
        90 -> "كل 90 دقيقة (ساعة ونصف)"
        120 -> "كل 120 دقيقة (ساعتين)"
        180 -> "كل 180 دقيقة (3 ساعات)"
        else -> "كل ${settings.reminderIntervalMinutes} دقيقة"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Settings Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(3.dp, shape = RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.End // RTL
                ) {
                    Text(
                        text = "تعديل إعدادات التذكير والهدف اليومي ⚙️",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. Daily Target Input
                    Text(
                        text = "الهدف اليومي لشرب الماء (بالمليلتر)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = targetInput,
                        onValueChange = { input ->
                            targetInput = input.filter { it.isDigit() }
                            val num = targetInput.toIntOrNull() ?: 0
                            if (num in 500..10000) {
                                viewModel.updateSettings(
                                    targetMl = num,
                                    intervalMinutes = settings.reminderIntervalMinutes,
                                    isNotificationsEnabled = settings.isNotificationsEnabled
                                )
                            }
                        },
                        trailingIcon = { Text("مل  ", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("target_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "مثال: 2000 مل للبالغين بوزن متوسط.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Interval Selection
                    Text(
                        text = "فترة تكرار المنبه (ساعة ونصف هي الفترة الطبيعية الموصى بها)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { expandedInterval = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                    RoundedCornerShape(12.dp)
                                )
                                .testTag("interval_selector_btn")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Dropdown")
                                Text(intervalLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        DropdownMenu(
                            expanded = expandedInterval,
                            onDismissRequest = { expandedInterval = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            val intervalOptions = listOf(
                                Pair(30, "كل 30 دقيقة (نصف ساعة)"),
                                Pair(60, "كل 60 دقيقة (ساعة كاملة)"),
                                Pair(90, "كل 90 دقيقة (ساعة ونصف) ⭐️ الموصى به"),
                                Pair(120, "كل 120 دقيقة (ساعتين)"),
                                Pair(180, "كل 180 دقيقة (3 ساعات)")
                            )

                            intervalOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.second, fontWeight = FontWeight.Bold, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                                    onClick = {
                                        viewModel.updateSettings(
                                            targetMl = settings.dailyTargetMl,
                                            intervalMinutes = option.first,
                                            isNotificationsEnabled = settings.isNotificationsEnabled
                                        )
                                        expandedInterval = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Notifications Toggle Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = settings.isNotificationsEnabled,
                            onCheckedChange = { isEnabled ->
                                viewModel.updateSettings(
                                    targetMl = settings.dailyTargetMl,
                                    intervalMinutes = settings.reminderIntervalMinutes,
                                    isNotificationsEnabled = isEnabled
                                )
                            },
                            modifier = Modifier.testTag("notification_toggle")
                        )

                        Text(
                            text = "تشغيل المنبهات التلقائية المكررة 🔔",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Extremely critical functional helper: "Test Alarm Now"
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, shape = RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "أداة التجربة السريعة والمحاكاة 🧪",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "لا تريد الانتظار ساعة ونصف؟ اضغط على زر التجربة أدناه، ثم أغلق التطبيق أو ابقَ فيه. بعد 5 ثوانٍ، ستنبثق لك شاشة التنبيه القهرية وتصلك الإشعارات للتأكد من أنها تعمل بكفاءة!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp,
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.triggerTestAlarmImmediate() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("simulate_alarm_btn")
                    ) {
                        Text("محاكاة المنبه فوراً (تنبيه بعد 5 ثوانٍ) 🚀", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Educational Section countering dehydration
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, shape = RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "مخاطر الجفاف وفوائد شرب الماء بانتظام 🩺",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val tips = listOf(
                        "العطش الشديد إشارة متأخرة جداً على الجفاف، يبدأ جسمك بفقدان قدراته الذهنية بنسبة 10% قبل أن تشعر بالعطش العادي.",
                        "يؤدي الجفاف الخفيف والمزمن إلى تكون الرواسب والحصى في الكلى والشعور الدائم بالصداع والخمول الدائم بمستوى طاقة الجسم.",
                        "شرب الماء بانتظام (مثال كل ساعة ونصف) يزيد من حرق السعرات الحرارية، يحسن نضارة بشرتك، ويقوي الدورة الدموية والمناعة.",
                        "شرب كأساً من الماء فور الاستيقاظ ينشط خلايا الدماغ والأعضاء الداخلية فوراً ويعزز طرد السموم المتراكمة."
                    )

                    tips.forEachIndexed { i, tip ->
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = tip,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.weight(1f).padding(end = 6.dp)
                            )
                            Text("💧", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// URGENT REMINDER POPUP (MANDATORY WINDOW)
// Emphasizing dehydration prevention & forcing interaction
// ==========================================
@Composable
fun WaterUrgentReminderPopup(
    settings: WaterSettings,
    viewModel: WaterViewModel
) {
    val infiniteTransition = rememberInfiniteTransition()
    val waterPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF001F3F).copy(alpha = 0.95f), // Rich space navy
                        Color(0xFF0D94E6).copy(alpha = 0.98f)  // Clear glass blue
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Emphasized giant floating blue water drop representing pure liquid importance
            Icon(
                imageVector = Icons.Rounded.WaterDrop,
                contentDescription = "Thirst alarm drop",
                tint = Color.White,
                modifier = Modifier
                    .size(100.dp)
                    .graphicsLayer {
                        scaleX = waterPulseScale
                        scaleY = waterPulseScale
                    }
                    .shadow(16.dp, CircleShape)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "جَسدُك يُناديك: حان وقت شرب الماء! 💧⚠️",
                fontSize = 22.sp,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Warning message targeting user's dehydration issue
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "«تذكير لإنقاذ صحتك وحمايتها من الجفاف»",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE0F7FA),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "لقد مرت ساعة ونصف! عدم شرب الماء بشكل دوري يعرضك للجفاف الخفيف، الخمول الدائم، والصداع الحاد. التطبيق لن يغلق هذه الشاشة حتى تطمئننا وتؤكد شربك للجرعة الآن!",
                        fontSize = 12.sp,
                        color = Color.White,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "كمية الكوب المقترح شربها الآن:",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Choice block standard cup
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(bottom = 20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "250 ملليلتر (كوب قياسي)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0D94E6)
                        )
                        Text("💧", fontSize = 20.sp)
                    }
                }
            }

            // PRIMARY INTERACTIVE CONFIRM BUTTONS - "لقد شربت الماء وتأكيد"
            Button(
                onClick = { viewModel.dismissReminderConfirmDrank(250) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF001F3F)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(8.dp, shape = RoundedCornerShape(16.dp))
                    .testTag("pop_confirm_drank_250")
            ) {
                Text(
                    text = "لقد شربت كوب ماء قياسي! (تأكيد شرب 250 مل) 👍",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // SECONDARY CHOICE: DRANK DIFFERENT VOLUME
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.dismissReminderConfirmOnly() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.15f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .testTag("pop_dismiss_only")
                ) {
                    Text(
                        text = "لقد شربت مسبقاً (تأجيل فقط) ⏰",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = { viewModel.dismissReminderConfirmDrank(500) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.15f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .testTag("pop_confirm_drank_500")
                ) {
                    Text(
                        text = "شربت زجاجة كاملة (500 مل) 🍾",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
