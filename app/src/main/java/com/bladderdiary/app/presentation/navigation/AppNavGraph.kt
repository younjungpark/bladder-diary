package com.bladderdiary.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bladderdiary.app.core.AppGraph
import com.bladderdiary.app.presentation.auth.AuthScreen
import com.bladderdiary.app.presentation.auth.AuthViewModel
import com.bladderdiary.app.presentation.main.MainScreen
import com.bladderdiary.app.presentation.main.MainViewModel

@Composable
fun AppNavGraph() {
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.factory(AppGraph.authRepository))
    val mainViewModel: MainViewModel = viewModel(
        factory = MainViewModel.factory(
            addVoidingEventUseCase = AppGraph.addVoidingEventUseCase,
            getDailyEventsUseCase = AppGraph.getDailyEventsUseCase,
            getDailyCountUseCase = AppGraph.getDailyCountUseCase,
            deleteVoidingEventUseCase = AppGraph.deleteVoidingEventUseCase,
            voidingRepository = AppGraph.voidingRepository
        )
    )
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    if (authState.isLoggedIn) {
        MainScreen(
            viewModel = mainViewModel,
            onSignOut = authViewModel::signOut
        )
    } else {
        AuthScreen(viewModel = authViewModel)
    }
}
