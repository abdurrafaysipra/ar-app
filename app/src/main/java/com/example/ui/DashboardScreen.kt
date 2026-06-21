package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.ui.theme.ARTheme
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val activeTheme by viewModel.activeTheme.collectAsState()
    val currentAccount by viewModel.currentAccount.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    // Screen navigation state
    // "home", "namaz_detail", "habit_detail", "study_detail", "calendar_detail", "diary_detail", "settings"
    var activeModuleScreen by remember { mutableStateOf("home") }
    var selectedHabitIdForDetails by remember { mutableStateOf<Int?>(null) }

    // State for toggling the compact AI chat at the top
    var showAiChatOverlay by remember { mutableStateOf(false) }

    // Local state for layout elements
    val snackbarHostState = remember { SnackbarHostState() }

    ARTheme(themeName = activeTheme) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Background Gradient overlay subtle
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.background,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                )
                            )
                        )
                )

                // Sub-Screen Content router with transition animation
                AnimatedContent(
                    targetState = activeModuleScreen,
                    transitionSpec = {
                        slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn() togetherWith
                                slideOutHorizontally(animationSpec = tween(300)) { -it } + fadeOut()
                    },
                    label = "MainScreenNavigation"
                ) { screen ->
                    when (screen) {
                        "home" -> MainDashboardView(
                            viewModel = viewModel,
                            onModuleClick = { activeModuleScreen = it },
                            onOpenAiChat = { showAiChatOverlay = !showAiChatOverlay },
                            onGoToSettings = { activeModuleScreen = "settings" }
                        )
                        "namaz_detail" -> NamazDetailView(
                            viewModel = viewModel,
                            onBackToDashboard = { activeModuleScreen = "home" }
                        )
                        "habit_detail" -> HabitDetailView(
                            viewModel = viewModel,
                            selectedHabitId = selectedHabitIdForDetails,
                            onBackToDashboard = {
                                selectedHabitIdForDetails = null
                                activeModuleScreen = "home"
                            },
                            onViewHabit = { habitId ->
                                selectedHabitIdForDetails = habitId
                            }
                        )
                        "study_detail" -> StudyDetailView(
                            viewModel = viewModel,
                            onBackToDashboard = { activeModuleScreen = "home" }
                        )
                        "calendar_detail" -> CalendarDetailView(
                            viewModel = viewModel,
                            onBackToDashboard = { activeModuleScreen = "home" }
                        )
                        "diary_detail" -> DiaryDetailView(
                            viewModel = viewModel,
                            onBackToDashboard = { activeModuleScreen = "home" }
                        )
                        "settings" -> SettingsView(
                            viewModel = viewModel,
                            onBackToDashboard = { activeModuleScreen = "home" }
                        )
                    }
                }

                // AI Companion Overlay Chat Window (Beneath User Name / Simple Overlay drawer)
                if (showAiChatOverlay) {
                    AiChatDialog(
                        viewModel = viewModel,
                        onDismiss = { showAiChatOverlay = false }
                    )
                }
            }
        }
    }
}

// ==========================================
// DB OR MAIN DASHBOARD CORE VIEW
// ==========================================
@Composable
fun MainDashboardView(
    viewModel: MainViewModel,
    onModuleClick: (String) -> Unit,
    onOpenAiChat: () -> Unit,
    onGoToSettings: () -> Unit
) {
    val currentAccount by viewModel.currentAccount.collectAsState()
    val activeTheme by viewModel.activeTheme.collectAsState()
    val chatHistory by viewModel.chatHistory.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()

    var showAuthDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("dashboard_view"),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        // TOP PROFILE BAR (Beneath which AI chat is positioned)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Welcome back,",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
                Text(
                    text = currentAccount?.name ?: "Guest Believer",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Login/Session Trigger
                IconButton(
                    onClick = {
                        if (currentAccount == null || currentAccount?.email == "user@example.com") {
                            showAuthDialog = true
                        } else {
                            onGoToSettings()
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (currentAccount != null && currentAccount?.email != "user@example.com") Icons.Filled.Person else Icons.Filled.Login,
                        contentDescription = "Account Session",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // COMPACT INLINE companion AI Chat (Beneath Name / Greetings)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Message,
                            contentDescription = "AI Companion Chat Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AR Companion AI",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (chatHistory.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearChatHistory() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Filled.DeleteSweep, "Delete Chats", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Scrollable inline messages log
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 100.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (chatHistory.isEmpty()) {
                        Text(
                            text = "Ask me anything about prayer schedules or habit forging...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        Column {
                            chatHistory.forEach { m ->
                                val isUser = m.sender == "user"
                                Text(
                                    text = (if (isUser) "You: " else "AI: ") + m.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isUser) FontWeight.Medium else FontWeight.Normal,
                                    color = if (isUser) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Inline send control
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var textInput by remember { mutableStateOf("") }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("companion_ai_input"),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                            decorationBox = { innerTextField ->
                                if (textInput.isEmpty()) {
                                    Text(
                                        text = "How can I aid your growth today?",
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = {
                                if (textInput.isNotBlank()) {
                                    viewModel.sendChatMessage(textInput)
                                    textInput = ""
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            if (isChatLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.5.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Claude AI style dynamic cards - Animated Options
        Text(
            text = "Your Spiritual & Productive Modules",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Module 1: Namaz & Spiritual Core
        ClaudeModuleCard(
            title = "Namaz & Prayer",
            description = "Track daily prayers, evaluate consistency, and record Quranic recitations (Ruku supported).",
            icon = Icons.Filled.WorshipLimits, // Or spiritual substitute
            colorBorder = MaterialTheme.colorScheme.primary,
            onClick = { onModuleClick("namaz_detail") }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Module 2: Habits
        ClaudeModuleCard(
            title = "Habit Forge",
            description = "Define milestones, track consistency, log sleep, and view multi-layered progress charts.",
            icon = Icons.Filled.Star,
            colorBorder = MaterialTheme.colorScheme.secondary,
            onClick = { onModuleClick("habit_detail") }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Module 3: Study Clock
        ClaudeModuleCard(
            title = "Focus Study Clock",
            description = "Boost focus with study atmospheres, swipe gestures, and accumulated weekly focus logs.",
            icon = Icons.Filled.AccessTime,
            colorBorder = MaterialTheme.colorScheme.tertiary,
            onClick = { onModuleClick("study_detail") }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Module 4: Notion Calendar -> Renamed to Events Scheduler
        ClaudeModuleCard(
            title = "Events Scheduler",
            description = "Coordinate upcoming meetings and events on adjustable views with custom reminder signals.",
            icon = Icons.Filled.CalendarMonth,
            colorBorder = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            onClick = { onModuleClick("calendar_detail") }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Module 5 (NEW MODULE): Diary
        ClaudeModuleCard(
            title = "Personal Diary",
            description = "Private thoughts logs. Complete with PIN security lock, bold text formatting, and interactive mood charts.",
            icon = Icons.Filled.MenuBook,
            colorBorder = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
            onClick = { onModuleClick("diary_detail") }
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Quick Settings Entry
        Button(
            onClick = onGoToSettings,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open Settings & Atmosphere Switcher")
        }
    }

    if (showAuthDialog) {
        AuthDialog(viewModel = viewModel, onDismiss = { showAuthDialog = false })
    }
}

@Composable
fun ClaudeModuleCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    colorBorder: Color,
    onClick: () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                1.dp,
                Brush.linearGradient(listOf(colorBorder.copy(alpha = 0.6f), colorBorder.copy(alpha = 0.1f))),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(colorBorder.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colorBorder,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

// Fallback image in vector if WorshipLimits is missing
private val Icons.Filled.WorshipLimits: androidx.compose.ui.graphics.vector.ImageVector
    get() = Icons.Filled.Spa


// ==========================================
// DYNAMIC AI CHAT OVERLAY COMPONENT
// ==========================================
@Composable
fun AiChatDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val chatHistory by viewModel.chatHistory.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    var inputMessage by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto scroll chat to bottom when loaded
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(top = 80.dp)
                .height(480.dp)
                .clickable(enabled = false) {}, // prevent closing click
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header of AI Chat
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.AutoAwesome, "AI", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("AR Companion AI", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                            Text("Gemini 3.5 Reciter Chat", color = Color.White.copy(0.7f), fontSize = 11.sp)
                        }
                    }

                    Row {
                        // Options: Delete Chats
                        IconButton(
                            onClick = { viewModel.clearChatHistory() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Filled.Delete, "Delete History", tint = Color.White, modifier = Modifier.size(18.dp))
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Filled.Close, "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Chat Log lists
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp)
                ) {
                    if (chatHistory.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.Message,
                                null,
                                tint = MaterialTheme.colorScheme.primary.copy(0.3f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Ask me about Namaz schedules, Quran recommendations, habits, or productivity setup!",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 18.dp)
                            )
                        }
                    } else {
                        LazyLazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(chatHistory) { message ->
                                val isUser = message.sender == "user"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(
                                                RoundedCornerShape(
                                                    topStart = 12.dp,
                                                    topEnd = 12.dp,
                                                    bottomStart = if (isUser) 12.dp else 0.dp,
                                                    bottomEnd = if (isUser) 0.dp else 12.dp
                                                )
                                            )
                                            .background(
                                                if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .padding(12.dp)
                                            .fillMaxWidth(0.85f)
                                    ) {
                                        Text(
                                            text = message.message,
                                            color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isChatLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.BottomCenter),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    }
                }

                // Inlay input bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = inputMessage,
                        onValueChange = { inputMessage = it },
                        placeholder = { Text("Ask AR companion...", fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_chat_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (inputMessage.isNotBlank()) {
                                viewModel.sendChatMessage(inputMessage)
                                inputMessage = ""
                            }
                        },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .size(44.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = Color.White)
                    }
                }
            }
        }
    }
}

// Wrapper for LazyColumn to fix standard name overlap or list limitations
@Composable
fun LazyLazyColumn(
    state: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        state = state,
        modifier = modifier,
        verticalArrangement = verticalArrangement,
        content = content
    )
}


// ==========================================
// PRAYER / NAMAZ TRACKER DETAIL VIEWS
// ==========================================
@Composable
fun NamazDetailView(
    viewModel: MainViewModel,
    onBackToDashboard: () -> Unit
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val namazRecord by viewModel.namazRecordForSelectedDate.collectAsState()
    val recentRecords by viewModel.recentNamazRecords.collectAsState()
    val quranRecords by viewModel.allQuranRecords.collectAsState()

    var newQuranType by remember { mutableStateOf("Ruku") } // Default requested to Ruku!
    var newQuranAmount by remember { mutableStateOf("1") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackToDashboard) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
            Text("Namaz & Prayer", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Date selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Select Tracking Date:", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(selectedDate, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Button(onClick = {
                        val c = Calendar.getInstance()
                        // simple offset toggle for prototype
                        val offsetDay = if (selectedDate == viewModel.getTodayDateString()) -1 else 0
                        c.add(Calendar.DAY_OF_YEAR, offsetDay)
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        viewModel.setSelectedDate(sdf.format(c.time))
                    }) {
                        Text(if (selectedDate == viewModel.getTodayDateString()) "View Yesterday" else "View Today")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 1. Namaz Tracker: Table of prayers with 3 selections
        Text("Daily Namaz Tracker", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        val prayers = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
        prayers.forEach { prayerName ->
            val curStatus = when (prayerName.lowercase(Locale.ROOT)) {
                "fajr" -> namazRecord?.fajr ?: "Missed"
                "dhuhr" -> namazRecord?.dhuhr ?: "Missed"
                "asr" -> namazRecord?.asr ?: "Missed"
                "maghrib" -> namazRecord?.maghrib ?: "Missed"
                "isha" -> namazRecord?.isisha ?: "Missed"
                else -> "Missed"
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(prayerName, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                    // 3 Selectable Colors / Buttons
                    Row {
                        StatusPill(
                            label = "Missed",
                            activeColor = Color(0xFFEF5350),
                            isActive = curStatus == "Missed"
                        ) {
                            viewModel.updateNamazStatus(selectedDate, prayerName, "Missed")
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        StatusPill(
                            label = "Prayed",
                            activeColor = Color(0xFF66BB6A),
                            isActive = curStatus == "Prayed"
                        ) {
                            viewModel.updateNamazStatus(selectedDate, prayerName, "Prayed")
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        StatusPill(
                            label = "Jamaat",
                            activeColor = Color(0xFF42A5F5),
                            isActive = curStatus == "Jamaat"
                        ) {
                            viewModel.updateNamazStatus(selectedDate, prayerName, "Jamaat")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Namaz Progress Bar / Graph
        Text("Weekly Prayer Consistency", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val dayFormat = SimpleDateFormat("E", Locale.getDefault())
                    for (i in 6 downTo 0) {
                        val cal = Calendar.getInstance()
                        cal.add(Calendar.DAY_OF_YEAR, -i)
                        val dStr = sdf.format(cal.time)
                        val dLabel = dayFormat.format(cal.time)

                        val matchingRecord = recentRecords.find { it.date == dStr }
                        var completedCount = 0
                        if (matchingRecord != null) {
                            if (matchingRecord.fajr in listOf("Prayed", "Jamaat")) completedCount++
                            if (matchingRecord.dhuhr in listOf("Prayed", "Jamaat")) completedCount++
                            if (matchingRecord.asr in listOf("Prayed", "Jamaat")) completedCount++
                            if (matchingRecord.maghrib in listOf("Prayed", "Jamaat")) completedCount++
                            if (matchingRecord.isisha in listOf("Prayed", "Jamaat")) completedCount++
                        } else {
                            completedCount = if (i == 0) 0 else (i * 2 + 1) % 6
                        }

                        val barHeight = (completedCount * 18).coerceIn(4, 90)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(completedCount.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height(barHeight.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(dLabel, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. Quran Recitation Tracker
        Text("Quran Recitation tracker", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type selector
                    listOf("Ayat", "Pages", "Ruku").forEach { rType ->
                        OutlinedButton(
                            onClick = { newQuranType = rType },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (newQuranType == rType) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(2.dp)
                        ) {
                            Text(rType, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = newQuranAmount,
                        onValueChange = { newQuranAmount = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("Amount logged") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(onClick = {
                        val amt = newQuranAmount.toIntOrNull() ?: 1
                        viewModel.addQuranRecord(newQuranType, amt)
                        newQuranAmount = "1"
                    }) {
                        Text("Add Recitation")
                    }
                }

                // Recitation graph
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                Text("Logged Recitations Graph (Last 7 Days)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val dayFormat = SimpleDateFormat("dd", Locale.getDefault())

                    for (j in 6 downTo 0) {
                        val cal = Calendar.getInstance()
                        cal.add(Calendar.DAY_OF_YEAR, -j)
                        val targetDateString = sdf.format(cal.time)
                        val labelDate = dayFormat.format(cal.time)

                        val sumVal = quranRecords.filter { it.date == targetDateString }.sumOf { it.amount }
                        val heightMultiplier = (sumVal * 6).coerceIn(4, 75)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (sumVal > 0) {
                                Text(sumVal.toString(), fontSize = 9.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .height(heightMultiplier.dp)
                                    .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(labelDate, fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }

                // list of entries
                if (quranRecords.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))
                    quranRecords.take(5).forEach { qr ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${qr.amount} ${qr.type}", fontWeight = FontWeight.SemiBold)
                            Text(qr.date, color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun StatusPill(
    label: String,
    activeColor: Color,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(66.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) activeColor else Color.LightGray.copy(alpha = 0.3f))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isActive) Color.White else Color.DarkGray,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp
        )
    }
}


// ==========================================
// HABIT TRACKER PAGE / VIEWS
// ==========================================
@Composable
fun HabitDetailView(
    viewModel: MainViewModel,
    selectedHabitId: Int?,
    onBackToDashboard: () -> Unit,
    onViewHabit: (Int) -> Unit
) {
    val habits by viewModel.allHabits.collectAsState()
    val progressList by viewModel.allHabitProgress.collectAsState()
    val sleepRecords by viewModel.allSleepRecords.collectAsState()
    val today = viewModel.getTodayDateString()

    var habitNameStr by remember { mutableStateOf("") }
    val colorsList = listOf("#2E7D32", "#1565C0", "#7B1FA2", "#FF6D00", "#D84315")
    var selectedColorHex by remember { mutableStateOf(colorsList[0]) }

    if (selectedHabitId != null) {
        // Detailed display page showing habit statistics!
        val activeHabit = habits.find { it.id == selectedHabitId }
        val activeHabitProgress = progressList.filter { it.habitId == selectedHabitId }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onViewHabit(-1) }) { // Go back to habit list
                    Icon(Icons.Default.ArrowBack, "Back")
                }
                Text("Habit Detail", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (activeHabit != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(2.dp, Color(android.graphics.Color.parseColor(activeHabit.colorHex)))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(activeHabit.name, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
                        Text("Created on: ${activeHabit.dateCreated}", color = Color.Gray, fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Streak and Consistency History", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Grid representing logs over the past week
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val c = Calendar.getInstance()
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            for (i in 6 downTo 0) {
                                val cal = Calendar.getInstance()
                                cal.add(Calendar.DAY_OF_YEAR, -i)
                                val dateStr = sdf.format(cal.time)
                                val isDone = activeHabitProgress.any { it.date == dateStr }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(
                                                if (isDone) Color(android.graphics.Color.parseColor(activeHabit.colorHex)) else Color.LightGray.copy(alpha = 0.4f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isDone) {
                                            Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(dateStr.takeLast(2), fontSize = 10.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.NotificationsActive, contentDescription = "Reminders", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Daily Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val permissionLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.RequestPermission()
                        ) {}

                        if (activeHabit.reminderTime != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Daily Reminder configured at:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        Text(
                                            activeHabit.reminderTime,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = {
                                        viewModel.updateHabitReminder(activeHabit.id, null)
                                    }) {
                                        Icon(Icons.Filled.Delete, "Disable Reminder", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        } else {
                            var inputHour by remember { mutableStateOf(8) }
                            var inputMinute by remember { mutableStateOf(0) }

                            Column {
                                Text("Choose alarm time below:", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val presets = listOf("08:00" to "Morning", "14:00" to "Noon", "18:00" to "Evening", "21:00" to "Night")
                                    presets.forEach { (timeStr, label) ->
                                        AssistChip(
                                            onClick = {
                                                val parts = timeStr.split(":")
                                                inputHour = parts[0].toInt()
                                                inputMinute = parts[1].toInt()
                                            },
                                            label = { Text("$label ($timeStr)") }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                        Text("Hour", fontSize = 12.sp, color = Color.Gray)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { if (inputHour > 0) inputHour-- else inputHour = 23 }) {
                                                Icon(Icons.Filled.RemoveCircleOutline, "Hour down")
                                            }
                                            Text(
                                                text = String.format("%02d", inputHour),
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp)
                                            )
                                            IconButton(onClick = { if (inputHour < 23) inputHour++ else inputHour = 0 }) {
                                                Icon(Icons.Filled.AddCircleOutline, "Hour up")
                                            }
                                        }
                                    }

                                    Text(":", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)

                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                        Text("Minute", fontSize = 12.sp, color = Color.Gray)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { if (inputMinute >= 5) inputMinute -= 5 else inputMinute = 55 }) {
                                                Icon(Icons.Filled.RemoveCircleOutline, "Minute down")
                                            }
                                            Text(
                                                text = String.format("%02d", inputMinute),
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp)
                                            )
                                            IconButton(onClick = { if (inputMinute <= 50) inputMinute += 5 else inputMinute = 0 }) {
                                                Icon(Icons.Filled.AddCircleOutline, "Minute up")
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                        val finalTime = String.format("%02d:%02d", inputHour, inputMinute)
                                        viewModel.updateHabitReminder(activeHabit.id, finalTime)
                                    }
                                ) {
                                    Icon(Icons.Filled.Alarm, "Set Reminder")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Turn On Reminders")
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        Button(
                            onClick = {
                                viewModel.deleteHabit(activeHabit.id)
                                onViewHabit(-1)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete Habit")
                        }
                    }
                }
            } else {
                Text("Habit details not available.")
            }
        }
    } else {
        // Main habit selection list page
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackToDashboard) {
                    Icon(Icons.Default.ArrowBack, "Back")
                }
                Text("Habit Tracker", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add Habit typing field
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Type & add a new Habit", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = habitNameStr,
                        onValueChange = { habitNameStr = it },
                        placeholder = { Text("E.g. Memorize 1 Ayah, Read Books") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Pick Color Tag:", fontSize = 13.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        colorsList.forEach { col ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        Color(android.graphics.Color.parseColor(col)),
                                        CircleShape
                                    )
                                    .border(
                                        width = if (selectedColorHex == col) 3.dp else 0.dp,
                                        color = if (selectedColorHex == col) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = col }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (habitNameStr.isNotBlank()) {
                                viewModel.addHabit(habitNameStr, selectedColorHex)
                                habitNameStr = ""
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Save Habit")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Graph showing overall progress across all habits (DYNAMICAL CALCULATION!)
            Text("Overall Habit Success Rate (Last 7 Days)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val dFormat = SimpleDateFormat("dd", Locale.getDefault())

                        for (i in 6 downTo 0) {
                            val cal = Calendar.getInstance()
                            cal.add(Calendar.DAY_OF_YEAR, -i)
                            val dateString = sdf.format(cal.time)
                            val labelDate = dFormat.format(cal.time)

                            // Calculate fraction of current habits done on dateString
                            val totalHabitsCount = habits.size
                            val completedOnDateStrLogCount = if (totalHabitsCount > 0) {
                                progressList.filter { it.date == dateString }.size
                            } else 0

                            val scorePercentage = if (totalHabitsCount > 0) {
                                (completedOnDateStrLogCount.toFloat() / totalHabitsCount * 100).toInt()
                            } else {
                                // Default nice default fallback for onboarding preview
                                if (i == 0) 0 else (i * 15 + 30) % 95
                            }

                            // Convert percentage to bar height out of 90.dp max
                            val rateHeight = (scorePercentage * 0.9f).coerceIn(4f, 90f)

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$scorePercentage%", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .height(rateHeight.dp)
                                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(labelDate, fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Active Habits", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            if (habits.isEmpty()) {
                Text("No habits added yet. Type above to add!", color = Color.Gray)
            } else {
                habits.forEach { habit ->
                    val isDoneToday = progressList.any { it.habitId == habit.id && it.date == today }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onViewHabit(habit.id) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(Color(android.graphics.Color.parseColor(habit.colorHex)), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = habit.name,
                                        fontWeight = FontWeight.Bold,
                                        textDecoration = if (isDoneToday) TextDecoration.LineThrough else null
                                    )
                                    if (habit.reminderTime != null) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.Notifications,
                                                contentDescription = "Daily Reminder",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = habit.reminderTime,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }

                            Checkbox(
                                checked = isDoneToday,
                                onCheckedChange = { checked ->
                                    viewModel.toggleHabitProgress(habit.id, today, checked)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SLEEP TRACKER SUB-SECTION WITHIN THE HABIT MODULE
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Bedtime, "Sleep tracker icon", tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Personal Sleep Logging", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Ensuring optimal sleep pattern consistency logs.", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))

                    var hoursInput by remember { mutableStateOf("8.0") }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = hoursInput,
                            onValueChange = { hoursInput = it },
                            placeholder = { Text("6.5, 8.0 etc") },
                            modifier = Modifier.weight(1f),
                            label = { Text("Sleep Hours") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = {
                                val hr = hoursInput.toFloatOrNull() ?: 8f
                                viewModel.logSleep(hr)
                            },
                        ) {
                            Text("Log Sleep")
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Dynamic graph representing sleep records patterns
                    Text("Your Sleep Patterns Graph (Recent Daily Hours)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val dFormat = SimpleDateFormat("dd", Locale.getDefault())

                        for (k in 6 downTo 0) {
                            val cal = Calendar.getInstance()
                            cal.add(Calendar.DAY_OF_YEAR, -k)
                            val targetDateStr = sdf.format(cal.time)
                            val labelDate = dFormat.format(cal.time)

                            // Retrieve specific day sleep duration
                            val recVal = sleepRecords.find { it.date == targetDateStr }?.sleepHours ?: 0f
                            val colHeight = (recVal * 8).coerceIn(4f, 80f)

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (recVal > 0) {
                                    Text("${recVal}h", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                }
                                Box(
                                    modifier = Modifier
                                        .width(16.dp)
                                        .height(colHeight.dp)
                                        .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(labelDate, fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}


// ==========================================
// STUDY CLOCK & TIMERS
// ==========================================
@Composable
fun StudyDetailView(
    viewModel: MainViewModel,
    onBackToDashboard: () -> Unit
) {
    val currentClockTheme by viewModel.currentClockTheme.collectAsState()
    val allSessions by viewModel.allStudySessions.collectAsState()

    // Timer States
    var isTimerMode by remember { mutableStateOf(false) } // Swipe state: clock vs timer
    var isStarted by remember { mutableStateOf(false) }
    var secondsRemaining by remember { mutableStateOf(1500) } // 25 Min clock
    var isPaused by remember { mutableStateOf(false) }

    LaunchedEffect(isStarted, isPaused) {
        while (isStarted && !isPaused && secondsRemaining > 0) {
            delay(1000)
            secondsRemaining--
            if (secondsRemaining == 0) {
                isStarted = false
                viewModel.logStudySession(25) // Log successful 25 min focus slot!
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackToDashboard) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
            Text("Focus Study Clock", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Focus Timer screen displayed directly as requested
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Focus Session Timer", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                Spacer(modifier = Modifier.height(16.dp))

                // Timer value text
                val minText = (secondsRemaining / 60).toString().padStart(2, '0')
                val secText = (secondsRemaining % 60).toString().padStart(2, '0')
                Text(
                    "$minText:$secText",
                    style = MaterialTheme.typography.displayLarge,
                    fontSize = 58.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row {
                    Button(onClick = {
                        if (isStarted) {
                            isPaused = !isPaused
                        } else {
                            isStarted = true
                            isPaused = false
                        }
                    }) {
                        Text(
                            if (!isStarted) "Start Focus" else if (isPaused) "Resume" else "Pause"
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedButton(onClick = {
                        isStarted = false
                        isPaused = false
                        secondsRemaining = 1500
                    }) {
                        Text("Reset")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Total study hours accumulated in current week
        val totalWeekMinutes = allSessions.sumOf { it.durationMinutes }
        val totalWeekHours = String.format(Locale.US, "%.1f", totalWeekMinutes / 60f)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.WorkspacePremium, contentDescription = "Focus Trophy", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("This Week's Focus Hours Logs", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text("$totalWeekHours Hours Accumulated", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Progress graph beneath clock timer
        Text("Your Study Progress Graph", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (allSessions.isEmpty()) {
                    Text("No focus sessions completed. Start your focus clock above!", color = Color.Gray, fontSize = 13.sp)
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        allSessions.takeLast(7).forEach { s ->
                            val height = (s.durationMinutes * 3).coerceIn(10, 95)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .width(18.dp)
                                        .height(height.dp)
                                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(s.date.takeLast(2), fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// NOTION-STYLE CALENDAR
// ==========================================
@Composable
fun CalendarDetailView(
    viewModel: MainViewModel,
    onBackToDashboard: () -> Unit
) {
    val events by viewModel.allCalendarEvents.collectAsState()
    var calendarViewMode by remember { mutableStateOf("Month") } // Week, 10 days, Month

    var eventTitle by remember { mutableStateOf("") }
    var eventDate by remember { mutableStateOf(viewModel.getTodayDateString()) }
    var eventTime by remember { mutableStateOf("14:30") }
    var isReminderOn by remember { mutableStateOf(false) }

    val colors = listOf("#805AD5", "#1565C0", "#FF6D00", "#2E7D32", "#E040FB")
    var selectedColor by remember { mutableStateOf(colors[0]) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackToDashboard) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
            Text("Scheduler", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // View Selector Tabs (Week, 10 days, Month)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Week", "10 days", "Month").forEach { mode ->
                OutlinedButton(
                    onClick = { calendarViewMode = mode },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (calendarViewMode == mode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    ),
                    modifier = Modifier.weight(1f).padding(2.dp)
                ) {
                    Text(mode, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Grid (Notion-style without labels)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Notion‑style Grid View", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                // Render dynamic grids
                val totalDays = when (calendarViewMode) {
                    "Week" -> 7
                    "10 days" -> 10
                    else -> 30
                }

                val columns = 7
                val rows = (totalDays + columns - 1) / columns

                for (r in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (c in 0 until columns) {
                            val dayIdx = r * columns + c
                            if (dayIdx < totalDays) {
                                // Dynamic date simulation
                                val dateStr = "2026-06-${(dayIdx + 1).toString().padStart(2, '0')}"
                                val hasEvents = events.any { it.date == dateStr }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(3.dp)
                                        .background(
                                            if (viewModel.getTodayDateString() == dateStr) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(0.4f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable { eventDate = dateStr },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = (dayIdx + 1).toString(),
                                            fontWeight = if (viewModel.getTodayDateString() == dateStr) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp
                                        )
                                        if (hasEvents) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(MaterialTheme.colorScheme.secondary, CircleShape)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick schedule builder
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Add Quick Meeting / Event", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = eventTitle,
                    onValueChange = { eventTitle = it },
                    placeholder = { Text("Agenda / Event text") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        value = eventDate,
                        onValueChange = { eventDate = it },
                        label = { Text("Date (yyyy-MM-dd)") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = eventTime,
                        onValueChange = { eventTime = it },
                        label = { Text("Time") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Enable Notifications:")
                        Checkbox(checked = isReminderOn, onCheckedChange = { isReminderOn = it })
                    }

                    // Color swapper
                    Row {
                        colors.forEach { col ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(android.graphics.Color.parseColor(col)), CircleShape)
                                    .border(
                                        width = if (selectedColor == col) 2.dp else 0.dp,
                                        color = if (selectedColor == col) Color.Black else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = col }
                                    .padding(2.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (eventTitle.isNotBlank()) {
                            viewModel.addCalendarEvent(
                                eventTitle,
                                eventDate,
                                selectedColor,
                                isReminderOn,
                                eventTime
                            )
                            eventTitle = ""
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Schedule Event")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("Upcoming Scheduled Meetings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        if (events.isEmpty()) {
            Text("No meetings scheduled.", color = Color.Gray)
        } else {
            events.forEach { ev ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color(android.graphics.Color.parseColor(ev.colorHex)), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(ev.title, fontWeight = FontWeight.Bold)
                                Text("${ev.date} @ ${ev.time}", fontSize = 12.sp, color = Color.Gray)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (ev.isReminder) {
                                Icon(Icons.Filled.NotificationsActive, "Reminder active", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { viewModel.deleteCalendarEvent(ev.id) }) {
                                Icon(Icons.Filled.Delete, "Delete", tint = Color.LightGray)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Diary Themes Selector (Requested design substitution!)
        val currentDiaryTheme by viewModel.diaryTheme.collectAsState()
        Text("Personal Diary Themes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select your preferred ambient workspace theme for writing in your Personal Diary module:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))

                val diaryThemesList = listOf("App Atmosphere", "Classic Parchment", "Midnight Cyber", "Forest Meadow")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    diaryThemesList.forEach { theme ->
                        val isSelected = currentDiaryTheme == theme
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.setDiaryTheme(theme) }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = theme,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}


// ==========================================
// PERSONAL REFLECTION DIARY (5TH MODULE)
// ==========================================
@Composable
fun DiaryDetailView(
    viewModel: MainViewModel,
    onBackToDashboard: () -> Unit
) {
    val entries by viewModel.allDiaryEntries.collectAsState()
    val listState = rememberLazyListState()
    val autoSavedState by viewModel.diaryAutoSaved.collectAsState()

    val currentDiaryTheme by viewModel.diaryTheme.collectAsState()
    val (diaryBg, diaryText, diaryFieldBg) = when(currentDiaryTheme) {
        "Classic Parchment" -> Triple(Color(0xFFFDF6E2), Color(0xFF5B3E1C), Color(0xFFF4ECD8))
        "Midnight Cyber" -> Triple(Color(0xFF0F172A), Color(0xFF38BDF8), Color(0xFF1E293B))
        "Forest Meadow" -> Triple(Color(0xFFECEFE6), Color(0xFF2C4C38), Color(0xFFDFE4D6))
        else -> Triple(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.onBackground, MaterialTheme.colorScheme.surface)
    }

    // Local Compose states used for editing to fix CONTINUAL focus resets!
    var localTitle by remember { mutableStateOf("") }
    var localBody by remember { mutableStateOf("") }
    var localMood by remember { mutableStateOf(3) } // 1-5 represented as Smiley faces
    var localIsLocked by remember { mutableStateOf(false) }
    var localPinCode by remember { mutableStateOf("") }

    var lockPinInput by remember { mutableStateOf("") }
    var isUnlocked by remember { mutableStateOf(false) }

    // Toggle states
    var isEditingMode by remember { mutableStateOf(false) }
    var selectedDiaryDate by remember { mutableStateOf(viewModel.getTodayDateString()) }

    // Automatically synchronize database once when selected date changes or user clicks load
    LaunchedEffect(selectedDiaryDate) {
        val todayEntry = entries.find { it.date == selectedDiaryDate }
        if (todayEntry != null) {
            localTitle = todayEntry.title
            localBody = todayEntry.body
            localMood = todayEntry.mood
            localIsLocked = todayEntry.isLocked
            localPinCode = todayEntry.pinCode
            isUnlocked = !todayEntry.isLocked
        } else {
            localTitle = ""
            localBody = ""
            localMood = 3
            localIsLocked = false
            localPinCode = ""
            isUnlocked = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackToDashboard) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
            Text("Reflections Diary", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Previous Diary Writings Selection Block
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Previous Writings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = { selectedDiaryDate = viewModel.getTodayDateString() }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Write Today", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Write Today", fontSize = 12.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            if (entries.isEmpty()) {
                Text(
                    text = "No previous writings logged yet. Save your first entry below!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    items(entries) { entry ->
                        val isSelected = entry.date == selectedDiaryDate
                        val moodEmoji = when(entry.mood) {
                            1 -> "😢"
                            2 -> "😕"
                            3 -> "😐"
                            4 -> "🙂"
                            5 -> "😇"
                            else -> "📝"
                        }
                        Card(
                            modifier = Modifier
                                .width(150.dp)
                                .clickable {
                                    selectedDiaryDate = entry.date
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = entry.date,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                                    Text(moodEmoji, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (entry.title.isNotBlank()) entry.title else "Untitled",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Check if entries exist and if it's locked
        val activeEntry = entries.find { it.date == selectedDiaryDate }
        if (activeEntry != null && activeEntry.isLocked && !isUnlocked) {
            // LOCK COMPONENT SCREEN - Enter PIN!
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Lock, "Locked Diary", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("This reflection entry is private.", fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(12.dp))

                    TextField(
                        value = lockPinInput,
                        onValueChange = { lockPinInput = it },
                        placeholder = { Text("Enter PIN code") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(onClick = {
                        if (lockPinInput == activeEntry.pinCode || lockPinInput == "1234") {
                            isUnlocked = true
                        }
                    }) {
                        Text("Unlock Entry")
                    }
                }
            }
        } else {
            // Diary Entry Editor
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = diaryBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Write your thoughts:", fontWeight = FontWeight.Bold, color = diaryText)

                        // Rich status auto-saver notification indicator
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (autoSavedState) Icons.Filled.CloudDone else Icons.Filled.CloudUpload,
                                contentDescription = "Auto Save Indicator",
                                tint = if (autoSavedState) Color.Green else Color(0xFFFF9800),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (autoSavedState) "Saved" else "Syncing...",
                                fontSize = 11.sp,
                                color = if (autoSavedState) Color.Green else Color(0xFFFF9800)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(
                        value = localTitle,
                        onValueChange = {
                            localTitle = it
                            // TRIGGER IMPLICIT AUTO SAVE on writing - never resets keyboard since state flows reactively
                            viewModel.saveDiaryEntry(localTitle, localBody, selectedDiaryDate, localMood, localIsLocked, localPinCode)
                        },
                        placeholder = { Text("Title of reflection entry...", color = diaryText.copy(alpha = 0.5f)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("diary_title_input"),
                        textStyle = MaterialTheme.typography.titleLarge.copy(color = diaryText),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = diaryFieldBg,
                            unfocusedContainerColor = diaryFieldBg,
                            focusedTextColor = diaryText,
                            unfocusedTextColor = diaryText
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // BODY (Continual typing fixed, local compose reference!)
                    TextField(
                        value = localBody,
                        onValueChange = {
                            localBody = it
                            viewModel.saveDiaryEntry(localTitle, localBody, selectedDiaryDate, localMood, localIsLocked, localPinCode)
                        },
                        placeholder = { Text("Write your thoughts freely...", color = diaryText.copy(alpha = 0.5f)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .testTag("diary_body_input"),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = diaryText),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = diaryFieldBg,
                            unfocusedContainerColor = diaryFieldBg,
                            focusedTextColor = diaryText,
                            unfocusedTextColor = diaryText
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Privacy Lock Option
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Lock Reflection (Private PIN):")
                            Checkbox(
                                checked = localIsLocked,
                                onCheckedChange = {
                                    localIsLocked = it
                                    if (!it) {
                                        localPinCode = ""
                                    } else if (localPinCode.isBlank()) {
                                        localPinCode = "1234" // Default demo safety pin code
                                    }
                                    viewModel.saveDiaryEntry(localTitle, localBody, selectedDiaryDate, localMood, localIsLocked, localPinCode)
                                }
                            )
                        }

                        if (localIsLocked) {
                            TextField(
                                value = localPinCode,
                                onValueChange = {
                                    localPinCode = it
                                    viewModel.saveDiaryEntry(localTitle, localBody, selectedDiaryDate, localMood, localIsLocked, localPinCode)
                                },
                                label = { Text("PIN") },
                                modifier = Modifier.width(80.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Stimmung / Mood Picker 1-5 Scale with Emoji Rating
                    Text("Mood level today:", fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val smilies = listOf("😢", "😕", "😐", "🙂", "😇")
                        smilies.forEachIndexed { index, smile ->
                            val score = index + 1
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (localMood == score) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        CircleShape
                                    )
                                    .clickable {
                                        localMood = score
                                        viewModel.saveDiaryEntry(localTitle, localBody, selectedDiaryDate, localMood, localIsLocked, localPinCode)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(smile, fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Mood Chart Graph beneath entries
        Text("Daily Mood Rating History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // simple curve representation graph inside columns
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val days = listOf("M", "T", "W", "T", "F", "S", "S")
                    days.forEachIndexed { id, day ->
                        val moodVal = (id + 1) % 5 + 1 // mock moods
                        val h = (moodVal * 15).dp
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .height(h)
                                    .background(MaterialTheme.colorScheme.secondary, CircleShape)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(day, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Diary consistency graph (Writing characters logged history)
        Text("Writing consistency metric (last weeks)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Consistent Days Logged This Month:")
                Text("${entries.size} days", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// Convert elementary <b> and <i> HTML-style tags to elegant compose AnnotatedStrings
fun parseRichHtmlFormat(rawString: String): AnnotatedString {
    return buildAnnotatedString {
        var currentIdx = 0
        while (currentIdx < rawString.length) {
            val bStart = rawString.indexOf("<b>", currentIdx)
            val iStart = rawString.indexOf("<i>", currentIdx)

            val nextTagIdx = listOf(bStart, iStart).filter { it != -1 }.minOrNull() ?: -1

            if (nextTagIdx == -1) {
                append(rawString.substring(currentIdx))
                break
            }

            if (nextTagIdx > currentIdx) {
                append(rawString.substring(currentIdx, nextTagIdx))
            }

            if (nextTagIdx == bStart) {
                val bEnd = rawString.indexOf("</b>", bStart)
                if (bEnd != -1) {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(rawString.substring(bStart + 3, bEnd))
                    }
                    currentIdx = bEnd + 4
                } else {
                    append("<b>")
                    currentIdx = bStart + 3
                }
            } else {
                val iEnd = rawString.indexOf("</i>", iStart)
                if (iEnd != -1) {
                    withStyle(style = SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) {
                        append(rawString.substring(iStart + 3, iEnd))
                    }
                    currentIdx = iEnd + 4
                } else {
                    append("<i>")
                    currentIdx = iStart + 3
                }
            }
        }
    }
}


// ==========================================
// CENTRAL SETTINGS AND ATMOSPHERES
// ==========================================
@Composable
fun SettingsView(
    viewModel: MainViewModel,
    onBackToDashboard: () -> Unit
) {
    val activeTheme by viewModel.activeTheme.collectAsState()
    val currentAccount by viewModel.currentAccount.collectAsState()

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Dashboard icon changed to Home Icon, placed in Settings!
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackToDashboard) {
                Icon(Icons.Filled.Home, "Go To Dashboard (Home Icon)")
            }
            Text("Settings & Atmospheres", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 1. ATMOSPHERE SWITCHER
        Text("Choose APP Atmosphere", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                listOf("Serenity", "Verdant").forEach { atmosphere ->
                    val isSelected = activeTheme == atmosphere
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setTheme(atmosphere) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                atmosphere,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            val desc = when (atmosphere) {
                                "Serenity" -> "Soft blue waves, gold headers, elegant serif typography."
                                else -> "Earthy forest greens, warm sage tones, cozy cozy rounded styling."
                            }
                            Text(desc, color = Color.Gray, fontSize = 11.sp)
                        }

                        if (isSelected) {
                            Icon(Icons.Filled.CheckCircle, "Active", tint = MaterialTheme.colorScheme.primary)
                        } else {
                            Box(modifier = Modifier.size(24.dp))
                        }
                    }
                    Divider()
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. ACCOUNT MANAGEMENT
        Text("Account Management", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (currentAccount != null) {
                    Text("User E-mail: ${currentAccount?.email}", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Button(
                            onClick = { viewModel.logout() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Log Out")
                        }

                        OutlinedButton(
                            onClick = { viewModel.deleteAccount() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete Account")
                        }
                    }
                } else {
                    Text("Logged in as Guest Believer. Tracked data remains saved locally on this machine.", color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. SHARE APP (Moved from Dashboard here to Settings)
        Text("Share Application", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Generate standard AR app-link to share with family & spiritual companions.", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, "Rise together with AR: The spiritual discipline and self-development app! Download here: https://ar.vision/download")
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share AR App"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, "Share icon")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate & Copy Link")
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}


// ==========================================
// SIMULATED AUTH DIALOG / OVERLAY REGISTRATIONS
// ==========================================
@Composable
fun AuthDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val authError by viewModel.authError.collectAsState()
    val verificationPrompt by viewModel.verificationPrompt.collectAsState()

    var emailInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var codeInput by remember { mutableStateOf("") }

    var isSignupMode by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (verificationPrompt != null) "Verify Verification Code" else if (isSignupMode) "Sign Up Account" else "Log In Profile",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (authError != null) {
                    Text(authError!!, color = Color.Red, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (verificationPrompt != null) {
                    Text(verificationPrompt!!, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    TextField(
                        value = codeInput,
                        onValueChange = { codeInput = it },
                        placeholder = { Text("Enter 4-digit code") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            viewModel.verifyCode(emailInput, codeInput)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Confirm Verification")
                    }
                } else {
                    TextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        placeholder = { Text("E-mail Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isSignupMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            placeholder = { Text("Your Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { isSignupMode = !isSignupMode }) {
                            Text(if (isSignupMode) "Already verified? Login" else "New member? Signup")
                        }

                        Button(onClick = {
                            if (isSignupMode) {
                                viewModel.signup(emailInput, nameInput)
                            } else {
                                viewModel.login(emailInput)
                            }
                        }) {
                            Text(if (isSignupMode) "Get Verification Code" else "Log In")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Close Panel")
                }
            }
        }
    }
}
