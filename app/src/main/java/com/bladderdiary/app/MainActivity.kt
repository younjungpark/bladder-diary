package com.bladderdiary.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.activity.viewModels
import com.bladderdiary.app.core.AppGraph
import com.bladderdiary.app.presentation.main.MainViewModel
import com.bladderdiary.app.presentation.navigation.AppNavGraph
import com.bladderdiary.app.ui.theme.BladderDiaryTheme
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        emitOAuthCallbackIfNeeded(intent)

        val viewModel: MainViewModel by viewModels {
            MainViewModel.factory(
                AppGraph.addVoidingEventUseCase,
                AppGraph.getDailyEventsUseCase,
                AppGraph.getDailyCountUseCase,
                AppGraph.deleteVoidingEventUseCase,
                AppGraph.updateVoidingEventMemoUseCase,
                AppGraph.voidingRepository
            )
        }

        setContent {
            BladderDiaryTheme {
                AppNavGraph()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        emitOAuthCallbackIfNeeded(intent)
    }

    private fun emitOAuthCallbackIfNeeded(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "bladderdiary" && data.host == "auth") {
            oauthCallbackChannel.trySend(data.toString())
        }
    }

    companion object {
        private val oauthCallbackChannel: Channel<String> = Channel(Channel.BUFFERED)
        val oauthCallbackFlow: Flow<String> = oauthCallbackChannel.receiveAsFlow()
    }
}
