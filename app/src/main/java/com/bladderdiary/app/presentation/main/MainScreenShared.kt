package com.bladderdiary.app.presentation.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

@Composable
internal fun ProvideFixedFontScale(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val fixedDensity = remember(density.density) {
        Density(density = density.density, fontScale = 1f)
    }

    CompositionLocalProvider(LocalDensity provides fixedDensity) {
        content()
    }
}

@Composable
internal fun MemoIndicatorButton(
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Description,
            contentDescription = "메모 보기",
            tint = tint,
            modifier = Modifier.size(15.dp)
        )
    }
}

@Composable
internal fun QuickActionBar(
    palette: HomePalette,
    isAdding: Boolean,
    isE2eeChecking: Boolean,
    onAddNow: () -> Unit,
    onAddAtTime: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = palette.bottomBarBackground,
        contentColor = palette.titleText,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, palette.bottomBarBorder),
        shadowElevation = 14.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GradientActionButton(
                modifier = Modifier.weight(1.05f),
                text = if (isAdding) "저장 중" else "지금 기록",
                icon = Icons.Default.Add,
                background = Brush.verticalGradient(
                    listOf(palette.primaryButtonStart, palette.primaryButtonEnd)
                ),
                topGlow = palette.primaryButtonGlow,
                contentColor = palette.primaryButtonText,
                enabled = !isAdding && !isE2eeChecking,
                onClick = onAddNow
            )
            GradientActionButton(
                modifier = Modifier.weight(1f),
                text = "시간 지정",
                icon = Icons.Default.Edit,
                background = Brush.verticalGradient(
                    listOf(palette.secondaryButtonStart, palette.secondaryButtonEnd)
                ),
                topGlow = palette.secondaryButtonGlow,
                contentColor = palette.secondaryButtonText,
                borderColor = palette.secondaryButtonBorder,
                enabled = !isAdding && !isE2eeChecking,
                onClick = onAddAtTime
            )
        }
    }
}

@Composable
private fun GradientActionButton(
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector,
    background: Brush,
    topGlow: Color,
    contentColor: Color,
    borderColor: Color = Color.Transparent,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .height(52.dp)
            .shadow(elevation = 10.dp, shape = shape)
            .clip(shape)
            .background(background)
            .drawBehind {
                drawCircle(
                    color = topGlow,
                    radius = size.width * 0.45f,
                    center = androidx.compose.ui.geometry.Offset(
                        x = size.width * 0.5f,
                        y = 0f
                    )
                )
            }
            .border(1.dp, borderColor, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor.copy(alpha = if (enabled) 1f else 0.45f),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                color = contentColor.copy(alpha = if (enabled) 1f else 0.45f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun GlassIconButton(
    palette: HomePalette,
    icon: ImageVector,
    contentDescription: String,
    buttonSize: Dp = 44.dp,
    iconSize: Dp = 18.dp,
    cornerRadius: Dp = 16.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(buttonSize)
            .clip(RoundedCornerShape(cornerRadius))
            .background(palette.iconButtonBackground)
            .border(1.dp, palette.iconButtonBorder, RoundedCornerShape(cornerRadius))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = palette.iconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
internal fun TinyActionButton(
    palette: HomePalette,
    icon: ImageVector,
    contentDescription: String,
    buttonSize: Dp = 24.dp,
    iconSize: Dp = 12.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(buttonSize)
            .clip(RoundedCornerShape(9.dp))
            .background(palette.miniButtonBackground)
            .border(1.dp, palette.miniButtonBorder, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = palette.actionIconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
internal fun HomeBackground(
    palette: HomePalette,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    listOf(palette.backgroundTop, palette.backgroundBottom)
                )
            )
            .drawBehind {
                drawCircle(
                    color = palette.backgroundGlowPrimary,
                    radius = size.minDimension * 0.26f,
                    center = androidx.compose.ui.geometry.Offset(
                        x = size.width * 0.15f,
                        y = size.height * 0.18f
                    )
                )
                drawCircle(
                    color = palette.backgroundGlowSecondary,
                    radius = size.minDimension * 0.2f,
                    center = androidx.compose.ui.geometry.Offset(
                        x = size.width * 0.82f,
                        y = size.height * 0.12f
                    )
                )
                drawCircle(
                    color = palette.backgroundGlowTertiary,
                    radius = size.minDimension * 0.28f,
                    center = androidx.compose.ui.geometry.Offset(
                        x = size.width * 0.5f,
                        y = size.height
                    )
                )
            }
    )
}

internal fun Long?.toMetricValue(): String {
    if (this == null) return "-"
    val totalMinutes = this / (1000 * 60)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "${hours}:${minutes.toString().padStart(2, '0')}"
}

internal fun LocalDate.toHeroDateText(): String {
    return "${monthNumber}월 ${dayOfMonth}일 ${dayOfWeek.toKoreanLabel()}"
}

private fun DayOfWeek.toKoreanLabel(): String {
    return when (value) {
        1 -> "월요일"
        2 -> "화요일"
        3 -> "수요일"
        4 -> "목요일"
        5 -> "금요일"
        6 -> "토요일"
        else -> "일요일"
    }
}

internal fun LocalDate.toKoreanShortDate(): String {
    return "${year}년 ${monthNumber}월 ${dayOfMonth}일"
}

internal fun LocalDate.plusDays(days: Int): LocalDate {
    return plus(DatePeriod(days = days))
}

internal fun Long.toTimeDisplay(): Pair<String, String> {
    val localTime = Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()

    val hour = localTime.hour
    val minute = localTime.minute
    val meridiem = if (hour < 12) "오전" else "오후"
    val displayHour = when (val normalized = hour % 12) {
        0 -> 12
        else -> normalized
    }

    return displayHour.toString().padStart(2, '0') + ":" + minute.toString().padStart(2, '0') to meridiem
}

internal fun Long.toIntervalText(): String {
    val minutes = this / (1000 * 60)
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return if (hours > 0) {
        if (remainingMinutes == 0L) {
            "${hours}시간"
        } else {
            "${hours}시간 ${remainingMinutes}분"
        }
    } else {
        "${remainingMinutes}분"
    }
}

internal fun String.toUiErrorText(maxLen: Int = 120): String {
    val normalized = replace('\n', ' ').replace('\r', ' ').trim()
    return if (normalized.length <= maxLen) normalized else normalized.take(maxLen) + "..."
}

internal fun String.isLikelyOfflineSyncError(): Boolean {
    val normalized = lowercase()
    return OFFLINE_SYNC_ERROR_PATTERNS.any(normalized::contains)
}

private val OFFLINE_SYNC_ERROR_PATTERNS = listOf(
    "unable to resolve host",
    "failed to connect",
    "network is unreachable",
    "no address associated with hostname",
    "no route to host",
    "software caused connection abort",
    "connection reset"
)

internal fun String.toVolumeMlOrNull(): Int? {
    if (isBlank()) return null
    return toIntOrNull()?.takeIf { it > 0 }
}

internal fun Int.toVolumeLabel(): String {
    return "$this mL"
}

@Composable
internal fun rememberHomePalette(): HomePalette {
    return if (isSystemInDarkTheme()) {
        DarkHomePalette
    } else {
        LightHomePalette
    }
}

internal data class HomePalette(
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val backgroundGlowPrimary: Color,
    val backgroundGlowSecondary: Color,
    val backgroundGlowTertiary: Color,
    val glassPanel: Color,
    val glassBorder: Color,
    val titleText: Color,
    val bodyText: Color,
    val mutedText: Color,
    val accentText: Color,
    val badgeText: Color,
    val volumeText: Color,
    val rowDivider: Color,
    val iconButtonBackground: Color,
    val iconButtonBorder: Color,
    val iconTint: Color,
    val datePillStart: Color,
    val datePillEnd: Color,
    val datePillBorder: Color,
    val summaryStart: Color,
    val summaryEnd: Color,
    val summaryBorder: Color,
    val metricPanel: Color,
    val metricBorder: Color,
    val metricLabelText: Color,
    val metricValueText: Color,
    val metricSubText: Color,
    val recordsBadgeBackground: Color,
    val recordsBadgeBorder: Color,
    val tableBackground: Color,
    val tableBorder: Color,
    val tableHeaderBackground: Color,
    val tableHeaderText: Color,
    val miniButtonBackground: Color,
    val miniButtonBorder: Color,
    val actionIconTint: Color,
    val bottomBarBackground: Color,
    val bottomBarBorder: Color,
    val primaryButtonStart: Color,
    val primaryButtonEnd: Color,
    val primaryButtonGlow: Color,
    val primaryButtonText: Color,
    val secondaryButtonStart: Color,
    val secondaryButtonEnd: Color,
    val secondaryButtonGlow: Color,
    val secondaryButtonBorder: Color,
    val secondaryButtonText: Color
)

private val DarkHomePalette = HomePalette(
    backgroundTop = Color(0xFF0C1719),
    backgroundBottom = Color(0xFF061012),
    backgroundGlowPrimary = Color(0x3854E3C0),
    backgroundGlowSecondary = Color(0x423E98C4),
    backgroundGlowTertiary = Color(0x33175D56),
    glassPanel = Color(0xD6121E22),
    glassBorder = Color(0x1FA7D0CB),
    titleText = Color(0xFFEEF9F7),
    bodyText = Color(0xFFE7F4F2),
    mutedText = Color(0xFF9AB7B2),
    accentText = Color(0xFFBCE6DF),
    badgeText = Color(0xFFD9FFF1),
    volumeText = Color(0xFFD7FFF3),
    rowDivider = Color(0x0DFFFFFF),
    iconButtonBackground = Color(0x0DFFFFFF),
    iconButtonBorder = Color(0x1FA7D0CB),
    iconTint = Color(0xFFD6F9F1),
    datePillStart = Color(0x24B5F2E8),
    datePillEnd = Color(0x1091D0C5),
    datePillBorder = Color(0x26C6FBF2),
    summaryStart = Color(0xFF07797C),
    summaryEnd = Color(0xFF07535A),
    summaryBorder = Color(0x2E9EFFE8),
    metricPanel = Color(0x6B03151B),
    metricBorder = Color(0x14FFFFFF),
    metricLabelText = Color(0xC2CCF0EC),
    metricValueText = Color(0xFFEEF9F7),
    metricSubText = Color(0xADDEF4F0),
    recordsBadgeBackground = Color(0x1F53E3C0),
    recordsBadgeBorder = Color(0x3853E3C0),
    tableBackground = Color(0x08FFFFFF),
    tableBorder = Color(0x0FFFFFFF),
    tableHeaderBackground = Color(0x12C9F5EC),
    tableHeaderText = Color(0xFFB1D7D0),
    miniButtonBackground = Color(0xDB182D31),
    miniButtonBorder = Color(0x2971B7AA),
    actionIconTint = Color(0xFF79F1CB),
    bottomBarBackground = Color(0xC21D2B2F),
    bottomBarBorder = Color(0x19C0ECE7),
    primaryButtonStart = Color(0xFF8AF7CB),
    primaryButtonEnd = Color(0xFF4CE1BB),
    primaryButtonGlow = Color(0x33E3FFF1),
    primaryButtonText = Color(0xFF07352E),
    secondaryButtonStart = Color(0xFF0CA4A3),
    secondaryButtonEnd = Color(0xFF076F7D),
    secondaryButtonGlow = Color(0x1FB6FFF1),
    secondaryButtonBorder = Color(0x24B6FFF1),
    secondaryButtonText = Color(0xFFECFFFB)
)

private val LightHomePalette = HomePalette(
    backgroundTop = Color(0xFFF4FBFB),
    backgroundBottom = Color(0xFFE8F5F4),
    backgroundGlowPrimary = Color(0x426CE0C3),
    backgroundGlowSecondary = Color(0x2E4DAED7),
    backgroundGlowTertiary = Color(0x3D8DDFCB),
    glassPanel = Color(0xB8FFFFFF),
    glassBorder = Color(0x1A185A62),
    titleText = Color(0xFF18363D),
    bodyText = Color(0xFF32575D),
    mutedText = Color(0xFF6D8B90),
    accentText = Color(0xFF3F7F84),
    badgeText = Color(0xFF28656D),
    volumeText = Color(0xFF114E5B),
    rowDivider = Color(0x0F1F5F65),
    iconButtonBackground = Color(0xDBFFFFFF),
    iconButtonBorder = Color(0x1A185A62),
    iconTint = Color(0xFF2A6F78),
    datePillStart = Color(0xFAE4F7F3),
    datePillEnd = Color(0xF4D0EEEA),
    datePillBorder = Color(0x1F26767A),
    summaryStart = Color(0xF0AFEBE4),
    summaryEnd = Color(0xF07FD6CE),
    summaryBorder = Color(0x2933878B),
    metricPanel = Color(0xD1FFFFFF),
    metricBorder = Color(0x1423696D),
    metricLabelText = Color(0xFF4E7F84),
    metricValueText = Color(0xFF153B42),
    metricSubText = Color(0xFF5C8589),
    recordsBadgeBackground = Color(0x244AB9AC),
    recordsBadgeBorder = Color(0x384AB9AC),
    tableBackground = Color(0xC2FFFFFF),
    tableBorder = Color(0x141F5F65),
    tableHeaderBackground = Color(0xB2C4EBE5),
    tableHeaderText = Color(0xFF5A8085),
    miniButtonBackground = Color(0xFFF1FCFA),
    miniButtonBorder = Color(0x2948A49A),
    actionIconTint = Color(0xFF1C8D89),
    bottomBarBackground = Color(0xC7FFFFFF),
    bottomBarBorder = Color(0x17185A62),
    primaryButtonStart = Color(0xFF86EDC6),
    primaryButtonEnd = Color(0xFF57D9B8),
    primaryButtonGlow = Color(0x6BFFFFFF),
    primaryButtonText = Color(0xFF0D4D45),
    secondaryButtonStart = Color(0xFF22C0B7),
    secondaryButtonEnd = Color(0xFF118F9D),
    secondaryButtonGlow = Color(0x26E8FFFB),
    secondaryButtonBorder = Color(0x29229398),
    secondaryButtonText = Color(0xFFF5FFFD)
)
