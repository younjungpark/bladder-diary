package com.bladderdiary.app.presentation.navigation

import androidx.compose.runtime.Composable
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
import androidx.compose.runtime.saveable.rememberSaveable
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
    
    var showCalendar by rememberSaveable { mutableStateOf(false) }

    if (!authState.isLoggedIn) {
        AuthScreen(viewModel = authViewModel)
    } else if (!pinState.isPinSet || !pinState.isUnlocked) {
        PinScreen(viewModel = pinViewModel)
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
            onShowCalendar = { showCalendar = true },
            onSignOut = {
                pinViewModel.clearRuntimeUnlock()
                authViewModel.signOut()
            }
        )
    }
}
