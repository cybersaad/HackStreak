package com.cybersaad.hackstreak.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cybersaad.hackstreak.HackStreakApp
import com.cybersaad.hackstreak.ui.theme.*
import com.cybersaad.hackstreak.formatNumber
import java.util.Calendar

@Composable
fun HackStreakMainScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as HackStreakApp
    val viewModel: MainViewModel = viewModel(
        factory = MainViewModel.Factory(app.container.repository)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Local state for editing the username
    var isEditing by remember { mutableStateOf(false) }

    // Auto-hide edit mode when sync is successful and we have a profile
    LaunchedEffect(state.profile) {
        if (state.profile != null) {
            isEditing = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // === Header ===
        TopHeader(
            onEditClick = { isEditing = !isEditing },
            isEditing = isEditing
        )

        // === Main Content Area ===
        Column(modifier = Modifier.padding(20.dp)) {
            
            // === Connection Box OR Input Box ===
            if (isEditing || !state.hasEverSynced || state.username.isBlank()) {
                UsernameInputSection(
                    usernameInput = state.usernameInput,
                    onUsernameChange = viewModel::onUsernameInputChanged,
                    onSync = {
                        viewModel.syncStreak()
                    },
                    isSyncing = state.isSyncing,
                    hasEverSynced = state.hasEverSynced,
                    username = state.username,
                    timeSinceLastSync = viewModel.getTimeSinceLastSync()
                )
                
                // Show errors if any during editing
                AnimatedVisibility(visible = state.errorMessage != null) {
                    state.errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = ErrorRed,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 8.dp, start = 8.dp)
                        )
                    }
                }
            } else {
                ConnectedBox(username = state.username)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // === TryHackMe Stats Dashboard ===
            AnimatedVisibility(
                visible = state.profile != null,
                enter = fadeIn() + slideInVertically { it / 2 }
            ) {
                state.profile?.let { profile ->
                    StatsDashboard(
                        username = profile.username,
                        streak = profile.streak,
                        rank = profile.rank,
                        points = profile.points,
                        rooms = profile.roomsCompleted,
                        badges = profile.badges,
                        weeklyActivity = profile.weeklyActivity.split(",").map { it.trim() == "true" }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // === HOW TO ADD WIDGET ===
            Text(
                text = "HOW TO ADD WIDGET",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Steps
            WidgetStep(
                number = "1",
                title = "Long Press",
                description = "Go to your home screen and long press on any empty space."
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            WidgetStep(
                number = "2",
                title = "Select Widgets",
                description = "Tap on the 'Widgets' button from the menu."
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            WidgetStep(
                number = "3",
                title = "Find HackStreak",
                description = "Drag the 3x2 widget to your home screen."
            )
        }
    }
}

// ================= TOP HEADER =================
@Composable
private fun TopHeader(
    onEditClick: () -> Unit,
    isEditing: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground)
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .padding(top = 24.dp), // For status bar
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "HackStreak",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        
        IconButton(
            onClick = onEditClick,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                if (isEditing) Icons.Filled.Close else Icons.Filled.Edit,
                contentDescription = if (isEditing) "Cancel Edit" else "Edit Username",
                tint = ThmGreen,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ================= CONNECTED BOX =================
@Composable
private fun ConnectedBox(username: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF2A3040), Color(0xFF2A3040))
            )
        )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Connected: ",
                    color = ThmGreen,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = username,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Ready to fire up your TryHackMe streak!",
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

// ================= STATS DASHBOARD =================
@Composable
private fun StatsDashboard(
    username: String,
    streak: Int,
    rank: String,
    points: Int,
    rooms: Int,
    badges: Int,
    weeklyActivity: List<Boolean>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF2A3040), Color(0xFF2A3040))
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // === Streak Circle: ring + fire icon + number ===
            StreakCircleDisplay(streak = streak)

            Spacer(modifier = Modifier.height(28.dp))

            // === Weekly activity circles ===
            WeeklyDaysRow(weeklyActivity)

            Spacer(modifier = Modifier.height(28.dp))

            // === "Your stats" section ===
            YourStatsSection(
                rank = rank,
                points = points,
                rooms = rooms,
                badges = badges
            )
        }
    }
}

// ================= STREAK CIRCLE (THM-style) =================
@Composable
private fun StreakCircleDisplay(streak: Int) {
    val animatedStreak = remember { Animatable(0f) }
    LaunchedEffect(streak) {
        animatedStreak.animateTo(
            targetValue = streak.toFloat(),
            animationSpec = tween(800, easing = EaseOutCubic)
        )
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Circle with fire icon inside
        Box(
            modifier = Modifier.size(110.dp),
            contentAlignment = Alignment.Center
        ) {
            // Ring border
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color(0xFF2A3040),
                    style = Stroke(width = 3.dp.toPx())
                )
                // Green progress overlay (subtle arc based on streak)
                if (streak > 0) {
                    val sweepAngle = (streak.coerceAtMost(30) / 30f) * 360f
                    drawArc(
                        color = Color(0xFF2ECC40),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // Fire icon
            Text(
                text = "🔥",
                fontSize = 40.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Streak number
        Text(
            text = animatedStreak.value.toInt().toString(),
            color = TextPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )

        // "day streak" label
        Text(
            text = "day streak",
            color = TextSecondary,
            fontSize = 14.sp
        )
    }
}

// ================= WEEKLY DAYS (THM circle style) =================
@Composable
private fun WeeklyDaysRow(weeklyActivity: List<Boolean>) {
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    val todayIndex = when (today) {
        Calendar.MONDAY -> 0; Calendar.TUESDAY -> 1; Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3; Calendar.FRIDAY -> 4; Calendar.SATURDAY -> 5
        Calendar.SUNDAY -> 6; else -> -1
    }

    val activity = if (weeklyActivity.size >= 7) weeklyActivity else List(7) { false }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        days.forEachIndexed { index, day ->
            val isActive = activity.getOrElse(index) { false }
            val isToday = index == todayIndex
            val isPast = index < todayIndex

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Circle indicator
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .then(
                            when {
                                isActive -> Modifier
                                    .clip(CircleShape)
                                    .background(ThmGreen.copy(alpha = 0.15f))
                                    .border(1.5.dp, ThmGreen, CircleShape)
                                isToday -> Modifier
                                    .border(1.5.dp, ThmGreen, CircleShape)
                                else -> Modifier
                                    .border(1.dp, Color(0xFF3A4050), CircleShape)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isActive -> Text("🔥", fontSize = 16.sp)
                        isPast && !isActive -> Text("✕", color = Color(0xFF4A5060), fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = day,
                    color = if (isToday) TextPrimary else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// ================= YOUR STATS =================
@Composable
private fun YourStatsSection(rank: String, points: Int, rooms: Int, badges: Int) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp, color = Color(0xFF2A3040))
            Text("  Your stats  ", color = TextMuted, fontSize = 12.sp)
            HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp, color = Color(0xFF2A3040))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            StatItem(value = rank, label = "Rank", modifier = Modifier.weight(1f))
            Box(modifier = Modifier.width(1.dp).height(50.dp).background(Color(0xFF2A3040)).align(Alignment.CenterVertically))
            StatItem(value = formatNumber(points), label = "Points", modifier = Modifier.weight(1f))
        }

        HorizontalDivider(thickness = 1.dp, color = Color(0xFF2A3040), modifier = Modifier.padding(vertical = 4.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            StatItem(value = rooms.toString(), label = "Rooms", modifier = Modifier.weight(1f))
            Box(modifier = Modifier.width(1.dp).height(50.dp).background(Color(0xFF2A3040)).align(Alignment.CenterVertically))
            StatItem(value = badges.toString(), label = "Badges", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, color = TextSecondary, fontSize = 11.sp)
    }
}

// ================= WIDGET STEP =================
@Composable
private fun WidgetStep(number: String, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ThmGreen),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = DarkBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

// ================= USERNAME INPUT =================
// Retained from original code to handle the input state
@Composable
private fun UsernameInputSection(
    usernameInput: String,
    onUsernameChange: (String) -> Unit,
    onSync: () -> Unit,
    isSyncing: Boolean,
    hasEverSynced: Boolean,
    username: String,
    timeSinceLastSync: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "TryHackMe username",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = usernameInput,
                onValueChange = onUsernameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ThmGreen,
                    unfocusedBorderColor = SurfaceElevated,
                    focusedContainerColor = SurfaceElevated,
                    unfocusedContainerColor = SurfaceElevated,
                    cursorColor = ThmGreen,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Person,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                },
                trailingIcon = {
                    if (hasEverSynced && username == usernameInput.trim()) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Verified",
                            tint = ThmGreen
                        )
                    }
                },
                placeholder = {
                    Text("Enter username", color = TextMuted)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onSync,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isSyncing,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = if (isSyncing) {
                                    listOf(ThmGreen.copy(alpha = 0.5f), ThmGreenBright.copy(alpha = 0.5f))
                                } else {
                                    listOf(ThmGreen, ThmGreenBright)
                                }
                            ),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSyncing) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = DarkBackground,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "SYNCING…",
                                color = DarkBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("🔥", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "SYNC STREAK",
                                color = DarkBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ================= HELPERS =================


