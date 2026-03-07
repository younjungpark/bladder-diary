package com.bladderdiary.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bladderdiary.app.core.AppGraph
import com.bladderdiary.app.presentation.e2ee.E2eePassphraseScreen
import com.bladderdiary.app.presentation.e2ee.E2eeEntryMode
import com.bladderdiary.app.presentation.e2ee.E2eePassphraseViewModel
import com.bladderdiary.app.presentation.auth.AuthScreen
import com.bladderdiary.app.presentation.auth.AuthViewModel
import com.bladderdiary.app.presentation.main.CalendarScreen
import com.bladderdiary.app.presentation.main.CalendarViewModel
import com.bladderdiary.app.presentation.main.MainScreen
import com.bladderdiary.app.presentation.main.MainViewModel
import com.bladderdiary.app.presentation.pin.PinScreen
import com.bladderdiary.app.presentation.pin.PinViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

@Composable
fun AppNavGraph() {
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.factory(
            AppGraph.authRepository,
            AppGraph.voidingRepository
        )
    )
    val calendarViewModel: CalendarViewModel = viewModel(
        factory = CalendarViewModel.factory(
            AppGraph.getMonthlyCountsUseCase
        )
    )
    val pinViewModel: PinViewModel = viewModel(
        factory = PinViewModel.factory(
            authRepository = AppGraph.authRepository,
            lockRepository = AppGraph.lockRepository
        )
    )
    val e2eeViewModel: E2eePassphraseViewModel = viewModel(
        factory = E2eePassphraseViewModel.factory(
            e2eeRepository = AppGraph.e2eeRepository,
            voidingRepository = AppGraph.voidingRepository
        )
    )
    val mainViewModel: MainViewModel = viewModel(
        factory = MainViewModel.factory(
            addVoidingEventUseCase = AppGraph.addVoidingEventUseCase,
            getDailyEventsUseCase = AppGraph.getDailyEventsUseCase,
            getDailyCountUseCase = AppGraph.getDailyCountUseCase,
            deleteVoidingEventUseCase = AppGraph.deleteVoidingEventUseCase,
            updateVoidingEventMemoUseCase = AppGraph.updateVoidingEventMemoUseCase,
            voidingRepository = AppGraph.voidingRepository
        )
    )
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val pinState by pinViewModel.uiState.collectAsStateWithLifecycle()
    val e2eeState by e2eeViewModel.uiState.collectAsStateWithLifecycle()

    var showCalendar by remember { mutableStateOf(false) }
    var showPinSetup by remember { mutableStateOf(false) }
    var showE2eeSettings by remember { mutableStateOf(false) }
    var e2eeSettingsOpenedForSetup by remember { mutableStateOf(false) }
    var mainE2eeNotice by remember { mutableStateOf<String?>(null) }
    var hasShownInitialE2eeNotice by remember { mutableStateOf(false) }

    // 로그인 화면으로 돌아가거나 재로그인 직후에는 PIN 설정 화면 자동 진입 상태를 초기화합니다.
    LaunchedEffect(authState.isLoggedIn) {
        if (!authState.isLoggedIn) {
            showCalendar = false
            showPinSetup = false
            showE2eeSettings = false
            e2eeSettingsOpenedForSetup = false
            mainE2eeNotice = null
            hasShownInitialE2eeNotice = false
        }
    }

    // PIN 설정이 완료되면 showPinSetup을 false로 돌려줌
    LaunchedEffect(pinState.isPinSet) {
        if (pinState.isPinSet && showPinSetup) {
            showPinSetup = false
        }
    }

    LaunchedEffect(e2eeState.isEnabled, showE2eeSettings, e2eeSettingsOpenedForSetup) {
        if (e2eeState.isEnabled && showE2eeSettings && e2eeSettingsOpenedForSetup) {
            showE2eeSettings = false
            e2eeSettingsOpenedForSetup = false
        }
    }

    LaunchedEffect(e2eeState.isEnabled) {
        if (!e2eeState.isEnabled) {
            mainE2eeNotice = null
            hasShownInitialE2eeNotice = false
        }
    }

    val isShowingMainScreen = authState.isLoggedIn &&
        !showCalendar &&
        !showPinSetup &&
        !showE2eeSettings &&
        (!pinState.isPinSet || pinState.isUnlocked) &&
        !e2eeState.isCheckingRemoteState &&
        (!e2eeState.isEnabled || e2eeState.isUnlocked)

    LaunchedEffect(isShowingMainScreen, e2eeState.isEnabled, e2eeState.isUnlocked, hasShownInitialE2eeNotice) {
        if (isShowingMainScreen && e2eeState.isEnabled && e2eeState.isUnlocked && !hasShownInitialE2eeNotice) {
            mainE2eeNotice = "메모 종단간 암호화가 활성화되어 있습니다. 열쇠 버튼에서 비밀문구를 변경할 수 있습니다."
            hasShownInitialE2eeNotice = true
        }
    }

    if (!authState.isLoggedIn) {
        AuthScreen(viewModel = authViewModel)
    } else if (pinState.isPinSet && !pinState.isUnlocked) {
        // 이미 PIN이 설정되어 있고 잠긴 상태 (앱 시작/백그라운드 복귀 등)
        PinScreen(viewModel = pinViewModel)
    } else if (e2eeState.isCheckingRemoteState) {
        SecurityLoadingScreen(message = "암호화 메모 상태를 확인하고 있습니다.")
    } else if (e2eeState.isEnabled && !e2eeState.isUnlocked) {
        E2eePassphraseScreen(
            viewModel = e2eeViewModel,
            entryMode = E2eeEntryMode.AUTO,
            onSignOut = {
                pinViewModel.clearRuntimeUnlock()
                e2eeViewModel.clearRuntimeUnlock()
                authViewModel.signOut()
            }
        )
    } else if (showPinSetup) {
        // 사용자가 PIN 설정을 원할 때 진입
        PinScreen(
            viewModel = pinViewModel,
            onCancel = { showPinSetup = false }
        )
    } else if (showE2eeSettings) {
        E2eePassphraseScreen(
            viewModel = e2eeViewModel,
            entryMode = E2eeEntryMode.MANAGE,
            onCancel = {
                showE2eeSettings = false
                e2eeSettingsOpenedForSetup = false
            },
            onPassphraseChanged = { message ->
                mainE2eeNotice = message
                hasShownInitialE2eeNotice = true
                showE2eeSettings = false
                e2eeSettingsOpenedForSetup = false
            }
        )
    } else if (showCalendar) {
        CalendarScreen(
            viewModel = calendarViewModel,
            onDateSelected = { date ->
                mainViewModel.setDate(date)
                showCalendar = false
            },
            onBack = { showCalendar = false }
        )
    } else {
        MainScreen(
            viewModel = mainViewModel,
            isPinSet = pinState.isPinSet,
            isE2eeEnabled = e2eeState.isEnabled,
            e2eeNoticeMessage = mainE2eeNotice,
            onShowCalendar = { showCalendar = true },
            onTogglePin = {
                if (pinState.isPinSet) {
                    pinViewModel.removePin()
                } else {
                    showPinSetup = true
                }
            },
            onOpenE2eeSettings = {
                showE2eeSettings = true
                e2eeSettingsOpenedForSetup = !e2eeState.isEnabled
            },
            onConsumeE2eeNotice = { mainE2eeNotice = null },
            onSignOut = {
                pinViewModel.clearRuntimeUnlock()
                e2eeViewModel.clearRuntimeUnlock()
                authViewModel.signOut()
            }
        )
    }
}

@Composable
private fun SecurityLoadingScreen(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
