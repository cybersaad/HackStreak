package com.cybersaad.hackstreak.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.cornerRadius
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.cybersaad.hackstreak.MainActivity
import com.cybersaad.hackstreak.HackStreakApp
import java.util.Calendar

class HackStreakWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(180.dp, 80.dp),   // Small
            DpSize(250.dp, 120.dp),  // Medium
            DpSize(320.dp, 180.dp),  // Large
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as HackStreakApp
        val repo = app.container.repository
        val username = try { repo.getSavedUsername() } catch (_: Exception) { "" }
        val profile = try {
            app.container.database.userProfileDao().getProfileSync(username)
        } catch (_: Exception) { null }

        val streak = profile?.streak ?: 0
        val rank = profile?.rank ?: "N/A"
        val badges = profile?.badges ?: 0
        val weeklyActivity = profile?.weeklyActivity?.split(",")?.map { it.trim() == "true" } ?: List(7) { false }

        provideContent {
            WidgetContent(
                username = username,
                streak = streak,
                rank = rank,
                badges = badges,
                weeklyActivity = weeklyActivity,
                size = LocalSize.current
            )
        }
    }
}

/**
 * Callback that syncs the profile and refreshes the widget when the refresh button is tapped.
 */
class RefreshActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val app = context.applicationContext as HackStreakApp
        val repo = app.container.repository
        val username = try { repo.getSavedUsername() } catch (_: Exception) { "" }

        if (username.isNotBlank()) {
            // Sync profile in background
            repo.syncProfile(username)
        }

        // Refresh the widget UI with new data
        HackStreakWidget().update(context, glanceId)
    }
}

// ============ Colors ============
private val DarkBg = Color(0xFF161B22)
private val Green = Color(0xFF2ECC40)
private val GreenSubtle = Color(0xFF1B5E20)
private val GrayCircle = Color(0xFF3A4050)
private val GrayMissed = Color(0xFF4A5060)
private val RefreshBg = Color(0xFF21262D)
private val TextWhite = Color.White
private val TextGray = Color(0xFFB0B0B0)
private val TextDim = Color(0xFF6E7681)

@Composable
private fun WidgetContent(
    username: String,
    streak: Int,
    rank: String,
    badges: Int,
    weeklyActivity: List<Boolean>,
    size: DpSize
) {
    // If width is small OR height is small, we scale down text and spacing to fit.
    val scaleDown = size.width.value < 220f || size.height.value < 140f
    // Only show the bottom stats bar if we have enough vertical room.
    val showStats = size.height.value >= 140f

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(20.dp)
            .background(DarkBg)
            .clickable(actionStartActivity<MainActivity>())
            .padding(if (scaleDown) 10.dp else 14.dp)
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // === HEADER: "Streaks" title + refresh button ===
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Streaks",
                    style = TextStyle(
                        color = ColorProvider(TextWhite),
                        fontWeight = FontWeight.Bold,
                        fontSize = if (scaleDown) 14.sp else 18.sp
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                if (!scaleDown && username.isNotBlank()) {
                    Text(
                        "@$username",
                        style = TextStyle(
                            color = ColorProvider(TextDim),
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.width(8.dp))
                }
                // Refresh button
                Box(
                    modifier = GlanceModifier
                        .size(if (scaleDown) 28.dp else 32.dp)
                        .cornerRadius(if (scaleDown) 14.dp else 16.dp)
                        .background(RefreshBg)
                        .clickable(actionRunCallback<RefreshActionCallback>()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "↻",
                        style = TextStyle(
                            color = ColorProvider(Green),
                            fontSize = if (scaleDown) 16.sp else 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            // === CENTER: Fire icon + streak count + "day streak" ===
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "🔥",
                    style = TextStyle(fontSize = if (scaleDown) 24.sp else 36.sp)
                )

                Text(
                    "$streak",
                    style = TextStyle(
                        color = ColorProvider(TextWhite),
                        fontWeight = FontWeight.Bold,
                        fontSize = if (scaleDown) 28.sp else 40.sp,
                        textAlign = TextAlign.Center
                    )
                )

                Text(
                    "day streak",
                    style = TextStyle(
                        color = ColorProvider(TextGray),
                        fontSize = if (scaleDown) 10.sp else 13.sp,
                        textAlign = TextAlign.Center
                    )
                )
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            // === BOTTOM: Weekly activity circles (M T W T F S S) ===
            WeeklyActivityRow(
                weeklyActivity = weeklyActivity,
                isSmall = scaleDown
            )

            // === Stats bar (only if we have vertical room) ===
            if (showStats) {
                Spacer(modifier = GlanceModifier.height(8.dp))
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = GlanceModifier.defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            rank,
                            style = TextStyle(
                                color = ColorProvider(TextWhite),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                        Text(
                            "Rank",
                            style = TextStyle(
                                color = ColorProvider(TextDim),
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                    Column(
                        modifier = GlanceModifier.defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "$badges",
                            style = TextStyle(
                                color = ColorProvider(TextWhite),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                        Text(
                            "Badges",
                            style = TextStyle(
                                color = ColorProvider(TextDim),
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyActivityRow(
    weeklyActivity: List<Boolean>,
    isSmall: Boolean
) {
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
    val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    val todayIndex = when (today) {
        Calendar.MONDAY -> 0; Calendar.TUESDAY -> 1; Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3; Calendar.FRIDAY -> 4; Calendar.SATURDAY -> 5
        Calendar.SUNDAY -> 6; else -> -1
    }
    val activity = if (weeklyActivity.size >= 7) weeklyActivity else List(7) { false }

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        dayLabels.forEachIndexed { index, label ->
            val isActive = activity.getOrElse(index) { false }
            val isToday = index == todayIndex
            val isPast = index < todayIndex

            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = GlanceModifier
                        .size(if (isSmall) 22.dp else 28.dp)
                        .cornerRadius(if (isSmall) 11.dp else 14.dp)
                        .background(
                            when {
                                isActive -> GreenSubtle
                                else -> Color.Transparent
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        when {
                            isActive -> "🔥"
                            isPast -> "✕"
                            else -> "○"
                        },
                        style = TextStyle(
                            color = ColorProvider(
                                when {
                                    isActive -> Green
                                    isPast -> GrayMissed
                                    isToday -> Green
                                    else -> GrayCircle
                                }
                            ),
                            fontSize = if (isSmall) 10.sp else 13.sp,
                            fontWeight = if (isActive || isToday) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(2.dp))

                Text(
                    label,
                    style = TextStyle(
                        color = ColorProvider(if (isToday) TextWhite else TextDim),
                        fontSize = if (isSmall) 8.sp else 10.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}

class HackStreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HackStreakWidget()
}
