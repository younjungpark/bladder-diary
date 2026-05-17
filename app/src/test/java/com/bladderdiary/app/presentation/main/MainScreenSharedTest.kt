package com.bladderdiary.app.presentation.main

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MainScreenSharedTest {
    @Test
    fun `클라우드 동기화 꺼짐 상태는 상단 아이콘을 숨긴다`() {
        val status = MainUiState(isCloudSyncEnabled = false).toHomeSyncStatus(TestHomePalette)

        assertNull(status)
    }

    @Test
    fun `클라우드 동기화 켜짐 idle 상태는 완료 아이콘을 표시한다`() {
        val status = MainUiState(isCloudSyncEnabled = true).toHomeSyncStatus(TestHomePalette)

        assertNotNull(status)
        assertEquals("동기화 완료", status?.label)
        assertEquals(TestHomePalette.primary, status?.tint)
    }

    @Test
    fun `클라우드 동기화 중 상태는 상단 아이콘을 표시한다`() {
        val status = MainUiState(
            isCloudSyncEnabled = true,
            isSyncing = true
        ).toHomeSyncStatus(TestHomePalette)

        assertNotNull(status)
        assertEquals("동기화 중", status?.label)
        assertEquals(TestHomePalette.syncPendingTint, status?.tint)
    }

    @Test
    fun `복원 기록 클라우드 업로드 중 상태는 별도 안내 문구를 표시한다`() {
        val status = MainUiState(
            isCloudSyncEnabled = true,
            isSyncing = true,
            pendingSyncCount = 2,
            shouldShowRestoreCloudUploadNotice = true
        ).toHomeSyncStatus(TestHomePalette)

        assertNotNull(status)
        assertEquals("복원 기록 동기화 중", status?.label)
        assertTrue(status?.message.orEmpty().contains("클라우드용으로 암호화해 업로드 중"))
        assertTrue(status?.message.orEmpty().contains("처음 한 번 오래 걸릴 수 있습니다"))
        assertEquals(TestHomePalette.syncPendingTint, status?.tint)
    }

    @Test
    fun `클라우드 동기화 대기 상태는 상단 아이콘을 표시한다`() {
        val status = MainUiState(
            isCloudSyncEnabled = true,
            pendingSyncCount = 2
        ).toHomeSyncStatus(TestHomePalette)

        assertNotNull(status)
        assertEquals("동기화 대기 2건", status?.label)
        assertEquals(TestHomePalette.syncPendingTint, status?.tint)
    }

    @Test
    fun `Drive 백업이 없으면 자동 백업 아이콘을 숨긴다`() {
        val status = MainUiState().toHomeBackupStatus(TestHomePalette)

        assertNull(status)
    }

    @Test
    fun `Drive 백업 연결 후 자동 백업 꺼짐 상태는 상단 아이콘을 숨긴다`() {
        val status = MainUiState(
            isDriveBackupConnected = true,
            isAutoBackupEnabled = false
        ).toHomeBackupStatus(TestHomePalette)

        assertNull(status)
    }

    @Test
    fun `자동 백업 켜짐 상태는 마지막 성공 시각을 안내한다`() {
        val status = MainUiState(
            isDriveBackupConnected = true,
            isAutoBackupEnabled = true,
            lastBackupSuccessEpochMs = 1_000L
        ).toHomeBackupStatus(TestHomePalette)

        assertNotNull(status)
        assertEquals("Google Drive 자동 백업 켜짐", status?.label)
        assertTrue(status?.message.orEmpty().contains("마지막 성공"))
        assertEquals(TestHomePalette.syncReadyTint, status?.tint)
    }

    @Test
    fun `백업 실패 상태는 확인 필요 문구를 표시한다`() {
        val status = MainUiState(
            isDriveBackupConnected = true,
            isAutoBackupEnabled = true,
            lastBackupFailureEpochMs = 1_000L,
            lastBackupErrorMessage = "네트워크 연결을 확인해 주세요."
        ).toHomeBackupStatus(TestHomePalette)

        assertNotNull(status)
        assertEquals("Google Drive 백업 확인 필요", status?.label)
        assertTrue(status?.message.orEmpty().contains("네트워크 연결"))
        assertEquals(TestHomePalette.syncErrorTint, status?.tint)
    }

    @Test
    fun `자동 백업 꺼짐 상태의 백업 실패는 상단 아이콘을 숨긴다`() {
        val status = MainUiState(
            isDriveBackupConnected = true,
            isAutoBackupEnabled = false,
            lastBackupFailureEpochMs = 1_000L,
            lastBackupErrorMessage = "네트워크 연결을 확인해 주세요."
        ).toHomeBackupStatus(TestHomePalette)

        assertNull(status)
    }
}

private val TestHomePalette = HomePalette(
    backgroundTop = Color.White,
    backgroundBottom = Color.White,
    backgroundGlowPrimary = Color.White,
    backgroundGlowSecondary = Color.White,
    backgroundGlowTertiary = Color.White,
    surfaceGlass = Color.White,
    surfaceStrong = Color.White,
    surfaceMuted = Color.White,
    surfaceTint = Color.White,
    noteSurface = Color.White,
    borderSoft = Color.Gray,
    borderStrong = Color.Gray,
    lineColor = Color.Gray,
    trackMuted = Color.Gray,
    titleText = Color.Black,
    bodyText = Color.Black,
    mutedText = Color.DarkGray,
    subtleText = Color.DarkGray,
    primary = Color.Blue,
    primaryStrong = Color.Blue,
    iconTint = Color(0xFF255A5A),
    primaryButtonStart = Color.Blue,
    primaryButtonEnd = Color.Blue,
    primaryButtonText = Color.White,
    secondaryButtonBackground = Color.White,
    secondaryButtonBorder = Color.Gray,
    secondaryButtonText = Color.Black,
    warningBackground = Color.Yellow,
    warningText = Color.Black,
    syncReadyTint = Color(0xFF1E8B6A),
    syncPendingTint = Color(0xFFC4801F),
    syncErrorTint = Color(0xFFB2453B),
    dangerTint = Color.Red,
    urgencyTones = listOf(
        UrgencyTone(
            container = Color.White,
            content = Color.Black,
            border = Color.Gray
        )
    )
)
