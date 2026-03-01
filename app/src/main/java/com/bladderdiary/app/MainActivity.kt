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
        if (!data.isCustomSchemeOAuthCallback()) return
        val callback = data.toString()
        if (isDuplicateCallback(callback)) return
        oauthCallbackChannel.trySend(callback)
    }

    companion object {
        private val oauthCallbackChannel: Channel<String> = Channel(Channel.BUFFERED)
        val oauthCallbackFlow: Flow<String> = oauthCallbackChannel.receiveAsFlow()
        private const val OAUTH_CALLBACK_DEDUPE_WINDOW_MS = 2_500L
        private var lastCallbackUrl: String? = null
        private var lastCallbackHandledAtMs: Long = 0L

        @Synchronized
        private fun isDuplicateCallback(url: String): Boolean {
            val now = System.currentTimeMillis()
            val isDuplicate = lastCallbackUrl == url && (now - lastCallbackHandledAtMs) <= OAUTH_CALLBACK_DEDUPE_WINDOW_MS
            lastCallbackUrl = url
            lastCallbackHandledAtMs = now
            return isDuplicate
        }
    }
}

private fun android.net.Uri.isCustomSchemeOAuthCallback(): Boolean {
    return scheme.equals("bladderdiary", ignoreCase = true) &&
        host.equals("auth", ignoreCase = true) &&
        normalizedPathStartsWith("/callback")
}

private fun android.net.Uri.normalizedPathStartsWith(prefix: String): Boolean {
    val normalizedPath = (path ?: "").lowercase()
    val normalizedPrefix = prefix.lowercase()
    return normalizedPath.startsWith(normalizedPrefix)
}
