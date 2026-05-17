package com.bladderdiary.app.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bladderdiary.app.R
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import java.time.Instant
import java.time.ZoneId

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
internal fun MainTopBar(
    palette: HomePalette,
    syncStatus: HomeSyncStatus?,
    backupStatus: HomeBackupStatus?,
    currentAccountLabel: String?,
    isPinSet: Boolean,
    isE2eeEnabled: Boolean,
    isE2eeChecking: Boolean,
    isCloudSyncEnabled: Boolean,
    isCloudSyncChanging: Boolean,
    menuExpanded: Boolean,
    onShowSyncStatus: () -> Unit,
    onShowBackupStatus: () -> Unit,
    onOpenMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onTogglePin: () -> Unit,
    onOpenCloudDataNotice: () -> Unit,
    onOpenCloudSyncSettings: () -> Unit,
    onOpenE2eeSettings: () -> Unit,
    onOpenPdfExport: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    isExportingPdf: Boolean,
    isDeletingAccount: Boolean,
    onOpenAccountDeletion: () -> Unit,
    onSignOut: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(palette.surfaceStrong)
                    .border(1.dp, palette.borderSoft, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                color = palette.titleText,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            backupStatus?.let { status ->
                StatusIconButton(
                    palette = palette,
                    icon = status.icon,
                    contentDescription = status.label,
                    tint = status.tint,
                    onClick = onShowBackupStatus
                )
            }

            syncStatus?.let { status ->
                StatusIconButton(
                    palette = palette,
                    icon = status.icon,
                    contentDescription = status.label,
                    tint = status.tint,
                    onClick = onShowSyncStatus
                )
            }

            MainOverflowMenu(
                palette = palette,
                currentAccountLabel = currentAccountLabel,
                isPinSet = isPinSet,
                isE2eeEnabled = isE2eeEnabled,
                isE2eeChecking = isE2eeChecking,
                isCloudSyncEnabled = isCloudSyncEnabled,
                isCloudSyncChanging = isCloudSyncChanging,
                menuExpanded = menuExpanded,
                onOpenMenu = onOpenMenu,
                onDismissMenu = onDismissMenu,
                onTogglePin = onTogglePin,
                onOpenCloudDataNotice = onOpenCloudDataNotice,
                onOpenCloudSyncSettings = onOpenCloudSyncSettings,
                onOpenE2eeSettings = onOpenE2eeSettings,
                onOpenPdfExport = onOpenPdfExport,
                onOpenBackupRestore = onOpenBackupRestore,
                isExportingPdf = isExportingPdf,
                isDeletingAccount = isDeletingAccount,
                onOpenAccountDeletion = onOpenAccountDeletion,
                onSignOut = onSignOut
            )
        }
    }
}

@Composable
private fun StatusIconButton(
    palette: HomePalette,
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(palette.surfaceStrong)
            .border(1.dp, palette.borderSoft, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun MainOverflowMenu(
    palette: HomePalette,
    currentAccountLabel: String?,
    isPinSet: Boolean,
    isE2eeEnabled: Boolean,
    isE2eeChecking: Boolean,
    isCloudSyncEnabled: Boolean,
    isCloudSyncChanging: Boolean,
    menuExpanded: Boolean,
    onOpenMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onTogglePin: () -> Unit,
    onOpenCloudDataNotice: () -> Unit,
    onOpenCloudSyncSettings: () -> Unit,
    onOpenE2eeSettings: () -> Unit,
    onOpenPdfExport: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    isExportingPdf: Boolean,
    isDeletingAccount: Boolean,
    onOpenAccountDeletion: () -> Unit,
    onSignOut: () -> Unit
) {
    Box {
        GlassIconButton(
            palette = palette,
            icon = Icons.Default.Settings,
            contentDescription = "전체 설정",
            onClick = onOpenMenu
        )

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = onDismissMenu
        ) {
            if (!currentAccountLabel.isNullOrBlank()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = currentAccountLabel,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null
                        )
                    },
                    enabled = false,
                    onClick = {}
                )
                HorizontalDivider()
            }
            DropdownMenuItem(
                text = { Text("개인정보/클라우드 저장 안내") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null
                    )
                },
                onClick = onOpenCloudDataNotice
            )
            DropdownMenuItem(
                text = {
                    Text(
                        when {
                            isCloudSyncChanging -> "동기화 설정 변경 중"
                            isCloudSyncEnabled -> "클라우드 동기화 끄기"
                            else -> "클라우드 동기화 켜기"
                        }
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (isCloudSyncEnabled) {
                            Icons.Default.CloudDone
                        } else {
                            Icons.Default.CloudOff
                        },
                        contentDescription = null
                    )
                },
                onClick = onOpenCloudSyncSettings,
                enabled = !isCloudSyncChanging
            )
            DropdownMenuItem(
                text = { Text(if (isE2eeEnabled) "기록 암호화 관리" else "기록 암호화 설정") },
                leadingIcon = {
                    Icon(
                        imageVector = if (isE2eeEnabled) {
                            Icons.Filled.VpnKey
                        } else {
                            Icons.Outlined.VpnKey
                        },
                        contentDescription = null
                    )
                },
                onClick = onOpenE2eeSettings,
                enabled = !isE2eeChecking
            )
            DropdownMenuItem(
                text = { Text(if (isExportingPdf) "PDF 생성 중" else "PDF 내보내기") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null
                    )
                },
                onClick = onOpenPdfExport,
                enabled = !isExportingPdf
            )
            DropdownMenuItem(
                text = { Text("백업 및 복원") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.SettingsBackupRestore,
                        contentDescription = null
                    )
                },
                onClick = onOpenBackupRestore
            )
            DropdownMenuItem(
                text = { Text(if (isPinSet) "PIN 해제" else "PIN 설정") },
                leadingIcon = {
                    Icon(
                        imageVector = if (isPinSet) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = null
                    )
                },
                onClick = onTogglePin
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(if (isDeletingAccount) "회원탈퇴 처리 중" else "회원탈퇴") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null
                    )
                },
                onClick = onOpenAccountDeletion,
                enabled = !isDeletingAccount
            )
            DropdownMenuItem(
                text = { Text("로그아웃") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null
                    )
                },
                onClick = onSignOut
            )
        }
    }
}

@Composable
internal fun GlassIconButton(
    palette: HomePalette,
    icon: ImageVector,
    contentDescription: String,
    buttonSize: Dp = 42.dp,
    iconSize: Dp = 20.dp,
    cornerRadius: Dp = 18.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(buttonSize)
            .clip(RoundedCornerShape(cornerRadius))
            .background(palette.surfaceStrong)
            .border(1.dp, palette.borderSoft, RoundedCornerShape(cornerRadius))
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
internal fun HomeBackground(palette: HomePalette, modifier: Modifier = Modifier) {
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
                    radius = size.minDimension * 0.24f,
                    center = Offset(size.width * 0.12f, size.height * 0.2f)
                )
                drawCircle(
                    color = palette.backgroundGlowSecondary,
                    radius = size.minDimension * 0.2f,
                    center = Offset(size.width * 0.9f, size.height * 0.16f)
                )
                drawCircle(
                    color = palette.backgroundGlowTertiary,
                    radius = size.minDimension * 0.3f,
                    center = Offset(size.width * 0.56f, size.height * 1.02f)
                )
            }
    )
}

internal fun Long?.toMetricValue(): String {
    if (this == null) return "-"
    val totalMinutes = this / (1000 * 60)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "$hours:${minutes.toString().padStart(2, '0')}"
}

internal fun LocalDate.toHeroDateText(): String =
    "${monthNumber}월 ${dayOfMonth}일 ${dayOfWeek.toKoreanLabel()}"

internal fun LocalDate.toCompactHeroDateText(): String =
    "${monthNumber}월 ${dayOfMonth}일 ${dayOfWeek.toKoreanShortLabel()}"

internal fun LocalDate.toHeroCaption(today: LocalDate): String =
    if (this == today) "오늘" else "${year}년 ${monthNumber}월 ${dayOfMonth}일"

private fun DayOfWeek.toKoreanLabel(): String = when (value) {
    1 -> "월요일"
    2 -> "화요일"
    3 -> "수요일"
    4 -> "목요일"
    5 -> "금요일"
    6 -> "토요일"
    else -> "일요일"
}

private fun DayOfWeek.toKoreanShortLabel(): String = when (value) {
    1 -> "월"
    2 -> "화"
    3 -> "수"
    4 -> "목"
    5 -> "금"
    6 -> "토"
    else -> "일"
}

internal fun LocalDate.toKoreanShortDate(): String = "${year}년 ${monthNumber}월 ${dayOfMonth}일"

internal fun LocalDate.plusDays(days: Int): LocalDate = plus(DatePeriod(days = days))

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

    val timeText = displayHour.toString().padStart(2, '0') +
        ":" +
        minute.toString().padStart(2, '0')
    return timeText to meridiem
}

private fun Long.toBackupStatusTimeText(): String {
    val (timeText, periodText) = toTimeDisplay()
    return "$periodText $timeText"
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

internal fun Int.toVolumeLabel(): String = "$this mL"

internal fun Int.toUrgencyLabel(): String = toString()

internal fun Int.toUrgencyDescription(): String = when (this) {
    1 -> "절박감 없음"
    2 -> "절박감 약함"
    3 -> "절박감 보통"
    4 -> "절박감 강함"
    else -> "절박감 매우 강함"
}

@Composable
internal fun rememberHomePalette(): HomePalette = if (isSystemInDarkTheme()) {
    DarkHomePalette
} else {
    LightHomePalette
}

internal data class HomeSyncStatus(
    val icon: ImageVector,
    val label: String,
    val message: String,
    val tint: Color
)

internal data class HomeBackupStatus(
    val icon: ImageVector,
    val label: String,
    val message: String,
    val tint: Color
)

internal data class UrgencyTone(val container: Color, val content: Color, val border: Color)

internal fun MainUiState.toHomeSyncStatus(palette: HomePalette): HomeSyncStatus? = when {
    isCloudSyncChanging -> HomeSyncStatus(
        icon = Icons.Default.CloudUpload,
        label = "동기화 설정 변경 중",
        message = "클라우드 동기화 설정을 변경하는 중입니다.",
        tint = palette.syncPendingTint
    )

    !isCloudSyncEnabled -> null

    shouldShowRestoreCloudUploadNotice && isSyncing -> HomeSyncStatus(
        icon = Icons.Default.CloudUpload,
        label = "복원 기록 동기화 중",
        message = restoreCloudUploadStatusMessage(pendingSyncCount, isUploading = true),
        tint = palette.syncPendingTint
    )

    isSyncing -> HomeSyncStatus(
        icon = Icons.Default.CloudUpload,
        label = "동기화 중",
        message = "클라우드에 기록을 동기화하는 중입니다.",
        tint = palette.syncPendingTint
    )

    pendingSyncError != null && pendingSyncCount > 0 -> HomeSyncStatus(
        icon = Icons.Default.CloudOff,
        label = "동기화 오류",
        message = "동기화에 문제가 있어 ${pendingSyncCount}건을 기기에 안전하게 보관 중입니다.",
        tint = palette.syncErrorTint
    )

    shouldShowRestoreCloudUploadNotice && pendingSyncCount > 0 -> HomeSyncStatus(
        icon = Icons.Default.CloudUpload,
        label = "복원 기록 업로드 대기",
        message = restoreCloudUploadStatusMessage(pendingSyncCount, isUploading = false),
        tint = palette.syncPendingTint
    )

    pendingSyncCount > 0 -> HomeSyncStatus(
        icon = Icons.Default.CloudUpload,
        label = "동기화 대기 ${pendingSyncCount}건",
        message = "${pendingSyncCount}건의 기록이 동기화를 기다리고 있습니다.",
        tint = palette.syncPendingTint
    )

    else -> HomeSyncStatus(
        icon = Icons.Default.CloudDone,
        label = "동기화 완료",
        message = "클라우드 동기화가 완료되었습니다.",
        tint = palette.primary
    )
}

private fun restoreCloudUploadStatusMessage(pendingSyncCount: Int, isUploading: Boolean): String {
    val actionText = if (isUploading) {
        "업로드 중입니다"
    } else {
        "업로드할 예정입니다"
    }
    val countText = if (pendingSyncCount > 0) {
        " ${pendingSyncCount}건을"
    } else {
        "을"
    }
    return "Google Drive 또는 백업 파일에서 복원한 기록$countText " +
        "클라우드용으로 암호화해 $actionText. 처음 한 번 오래 걸릴 수 있습니다."
}

internal fun MainUiState.toHomeBackupStatus(palette: HomePalette): HomeBackupStatus? = when {
    isBackupRunning -> HomeBackupStatus(
        icon = Icons.Default.SettingsBackupRestore,
        label = "Google Drive 백업 중",
        message = "Google Drive에 백업을 저장하는 중입니다.",
        tint = palette.syncPendingTint
    )

    isAutoBackupEnabled && (lastBackupFailureEpochMs != null || lastBackupErrorMessage != null) -> {
        val errorText = lastBackupErrorMessage?.toUiErrorText(maxLen = 80)
        HomeBackupStatus(
            icon = Icons.Default.SettingsBackupRestore,
            label = "Google Drive 백업 확인 필요",
            message = if (errorText == null) {
                "Google Drive 백업을 완료하지 못했습니다. 백업 및 복원에서 확인해 주세요."
            } else {
                "Google Drive 백업을 완료하지 못했습니다. $errorText"
            },
            tint = palette.syncErrorTint
        )
    }

    isAutoBackupEnabled -> HomeBackupStatus(
        icon = Icons.Default.SettingsBackupRestore,
        label = "Google Drive 자동 백업 켜짐",
        message = lastBackupSuccessEpochMs?.let { completedAt ->
            "Google Drive 자동 백업이 켜져 있습니다. 마지막 성공 ${completedAt.toBackupStatusTimeText()}"
        } ?: "Google Drive 자동 백업이 켜져 있습니다. 기록 변경 후 약 30분 뒤 백업됩니다.",
        tint = palette.syncReadyTint
    )

    else -> null
}

internal data class HomePalette(
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val backgroundGlowPrimary: Color,
    val backgroundGlowSecondary: Color,
    val backgroundGlowTertiary: Color,
    val surfaceGlass: Color,
    val surfaceStrong: Color,
    val surfaceMuted: Color,
    val surfaceTint: Color,
    val noteSurface: Color,
    val borderSoft: Color,
    val borderStrong: Color,
    val lineColor: Color,
    val trackMuted: Color,
    val titleText: Color,
    val bodyText: Color,
    val mutedText: Color,
    val subtleText: Color,
    val primary: Color,
    val primaryStrong: Color,
    val iconTint: Color,
    val primaryButtonStart: Color,
    val primaryButtonEnd: Color,
    val primaryButtonText: Color,
    val secondaryButtonBackground: Color,
    val secondaryButtonBorder: Color,
    val secondaryButtonText: Color,
    val warningBackground: Color,
    val warningText: Color,
    val syncReadyTint: Color,
    val syncPendingTint: Color,
    val syncErrorTint: Color,
    val dangerTint: Color,
    val urgencyTones: List<UrgencyTone>
)

private val DarkHomePalette = HomePalette(
    backgroundTop = Color(0xFF111818),
    backgroundBottom = Color(0xFF0B1212),
    backgroundGlowPrimary = Color(0x2247BEB5),
    backgroundGlowSecondary = Color(0x1C3A8A84),
    backgroundGlowTertiary = Color(0x24386D68),
    surfaceGlass = Color(0xE01A2424),
    surfaceStrong = Color(0xFF1D2727),
    surfaceMuted = Color(0xFF152020),
    surfaceTint = Color(0xFF244040),
    noteSurface = Color(0xFF203030),
    borderSoft = Color(0x1FBDD0CF),
    borderStrong = Color(0x297ED5D4),
    lineColor = Color(0x263C5A5A),
    trackMuted = Color(0xFF5A7272),
    titleText = Color(0xFFE8F0F0),
    bodyText = Color(0xFFD4E2E2),
    mutedText = Color(0xFF9CB4B3),
    subtleText = Color(0xFF7F9797),
    primary = Color(0xFF80D6D5),
    primaryStrong = Color(0xFF5DC4C3),
    iconTint = Color(0xFFE2F3F3),
    primaryButtonStart = Color(0xFF0E8585),
    primaryButtonEnd = Color(0xFF0A6767),
    primaryButtonText = Color(0xFFF7FFFF),
    secondaryButtonBackground = Color(0xFF223131),
    secondaryButtonBorder = Color(0x295B6E6E),
    secondaryButtonText = Color(0xFFD7EEEE),
    warningBackground = Color(0xFF3F2A18),
    warningText = Color(0xFFFFD7B1),
    syncReadyTint = Color(0xFF8BE2BD),
    syncPendingTint = Color(0xFFFFD083),
    syncErrorTint = Color(0xFFFFA299),
    dangerTint = Color(0xFFFFB4AB),
    urgencyTones = listOf(
        UrgencyTone(
            container = Color(0xFF20372F),
            content = Color(0xFF9DE9CD),
            border = Color(0xFF57C49A)
        ),
        UrgencyTone(
            container = Color(0xFF3A331A),
            content = Color(0xFFFFE09A),
            border = Color(0xFFD9B351)
        ),
        UrgencyTone(
            container = Color(0xFF3D2B1A),
            content = Color(0xFFFFC48B),
            border = Color(0xFFE08F43)
        ),
        UrgencyTone(
            container = Color(0xFF43261D),
            content = Color(0xFFFFB29C),
            border = Color(0xFFF07C57)
        ),
        UrgencyTone(
            container = Color(0xFF4A2322),
            content = Color(0xFFFFB7AF),
            border = Color(0xFFFF7E71)
        )
    )
)

private val LightHomePalette = HomePalette(
    backgroundTop = Color(0xFFF7FBFB),
    backgroundBottom = Color(0xFFEAF1F1),
    backgroundGlowPrimary = Color(0x2B98D8D8),
    backgroundGlowSecondary = Color(0x1FBAE4D9),
    backgroundGlowTertiary = Color(0x2695D6CC),
    surfaceGlass = Color(0xE8FFFFFF),
    surfaceStrong = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFF3F7F7),
    surfaceTint = Color(0xFFE7F0F0),
    noteSurface = Color(0xFFEEF3F3),
    borderSoft = Color(0x22A5B6B6),
    borderStrong = Color(0x3397B5B5),
    lineColor = Color(0x1D95B7B7),
    trackMuted = Color(0xFF9AB4B4),
    titleText = Color(0xFF171D1D),
    bodyText = Color(0xFF2A3535),
    mutedText = Color(0xFF617171),
    subtleText = Color(0xFF839292),
    primary = Color(0xFF006767),
    primaryStrong = Color(0xFF1E8181),
    iconTint = Color(0xFF255A5A),
    primaryButtonStart = Color(0xFF006767),
    primaryButtonEnd = Color(0xFF1E8181),
    primaryButtonText = Color(0xFFFFFFFF),
    secondaryButtonBackground = Color(0xFFFDFEFE),
    secondaryButtonBorder = Color(0x1FB2C1C1),
    secondaryButtonText = Color(0xFF255A5A),
    warningBackground = Color(0xFFFFEBDC),
    warningText = Color(0xFF8A4A0B),
    syncReadyTint = Color(0xFF1E8B6A),
    syncPendingTint = Color(0xFFC4801F),
    syncErrorTint = Color(0xFFB2453B),
    dangerTint = Color(0xFFB2453B),
    urgencyTones = listOf(
        UrgencyTone(
            container = Color(0xFFE7F4F0),
            content = Color(0xFF2D6B5B),
            border = Color(0xFF7CB6A6)
        ),
        UrgencyTone(
            container = Color(0xFFFFF4D4),
            content = Color(0xFF8A6C1C),
            border = Color(0xFFE0C66B)
        ),
        UrgencyTone(
            container = Color(0xFFFFEFE0),
            content = Color(0xFFB85D00),
            border = Color(0xFFF0A14F)
        ),
        UrgencyTone(
            container = Color(0xFFFFE6D6),
            content = Color(0xFFD14321),
            border = Color(0xFFF08A6F)
        ),
        UrgencyTone(
            container = Color(0xFFFFDED6),
            content = Color(0xFFBF2613),
            border = Color(0xFFE47162)
        )
    )
)

internal fun HomePalette.urgencyTone(level: Int): UrgencyTone {
    val index = level.coerceIn(1, urgencyTones.size) - 1
    return urgencyTones[index]
}
