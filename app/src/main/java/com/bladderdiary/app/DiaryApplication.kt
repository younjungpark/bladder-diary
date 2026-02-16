package com.bladderdiary.app

import android.app.Application
import com.bladderdiary.app.core.AppGraph
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

class DiaryApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
        AppGraph.requestSync()

        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    AppGraph.requestSync()
                }

                override fun onStop(owner: LifecycleOwner) {
                    AppGraph.lockRepository.clearRuntimeUnlock()
                }
            }
        )
    }
}
