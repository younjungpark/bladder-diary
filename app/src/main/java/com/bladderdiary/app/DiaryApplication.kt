package com.bladderdiary.app

import android.app.Application
import com.bladderdiary.app.core.AppGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DiaryApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)

        CoroutineScope(Dispatchers.IO).launch {
            AppGraph.syncEventsUseCase()
        }
    }
}
