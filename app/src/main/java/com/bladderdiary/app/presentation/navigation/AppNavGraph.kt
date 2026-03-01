package com.bladderdiary.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bladderdiary.app.core.AppGraph
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
    
    var showCalendar by remember { mutableStateOf(false) }
    var showPinSetup by remember { mutableStateOf(false) }

    // 로그인 화면으로 돌아가거나 재로그인 직후에는 PIN 설정 화면 자동 진입 상태를 초기화합니다.
    LaunchedEffect(authState.isLoggedIn) {
        if (!authState.isLoggedIn) {
            showCalendar = false
            showPinSetup = false
        }
    }

    // PIN 설정이 완료되면 showPinSetup을 false로 돌려줌
    LaunchedEffect(pinState.isPinSet) {
        if (pinState.isPinSet && showPinSetup) {
            showPinSetup = false
        }
    }

    if (!authState.isLoggedIn) {
        AuthScreen(viewModel = authViewModel)
    } else if (pinState.isPinSet && !pinState.isUnlocked) {
        // 이미 PIN이 설정되어 있고 잠긴 상태 (앱 시작/백그라운드 복귀 등)
        PinScreen(viewModel = pinViewModel)
    } else if (showPinSetup) {
        // 사용자가 PIN 설정을 원할 때 진입
        PinScreen(
            viewModel = pinViewModel,
            onCancel = { showPinSetup = false }
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
            onShowCalendar = { showCalendar = true },
            onTogglePin = {
                if (pinState.isPinSet) {
                    pinViewModel.removePin()
                } else {
                    showPinSetup = true
                }
            },
            onSignOut = {
                pinViewModel.clearRuntimeUnlock()
                authViewModel.signOut()
            }
        )
    }
}
