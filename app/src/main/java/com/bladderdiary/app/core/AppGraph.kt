package com.bladderdiary.app.core

import android.content.Context
import com.bladderdiary.app.data.local.AppDatabase
import com.bladderdiary.app.data.remote.PinStore
import com.bladderdiary.app.data.remote.SessionStore
import com.bladderdiary.app.data.remote.SupabaseApi
import com.bladderdiary.app.data.remote.SupabaseAuthClient
import com.bladderdiary.app.data.repository.AuthRepositoryImpl
import com.bladderdiary.app.data.repository.LockRepositoryImpl
import com.bladderdiary.app.data.repository.VoidingRepositoryImpl
import com.bladderdiary.app.domain.model.AuthRepository
import com.bladderdiary.app.domain.model.LockRepository
import com.bladderdiary.app.domain.model.VoidingRepository
import com.bladderdiary.app.domain.usecase.AddVoidingEventUseCase
import com.bladderdiary.app.domain.usecase.DeleteVoidingEventUseCase
import com.bladderdiary.app.domain.usecase.GetDailyCountUseCase
import com.bladderdiary.app.domain.usecase.GetDailyEventsUseCase
import com.bladderdiary.app.domain.usecase.SyncEventsUseCase
import com.bladderdiary.app.worker.SyncScheduler

object AppGraph {
    private lateinit var db: AppDatabase
    private lateinit var sessionStore: SessionStore
    private lateinit var pinStore: PinStore
    private lateinit var supabaseApi: SupabaseApi
    private lateinit var supabaseAuthClient: SupabaseAuthClient
    private lateinit var syncScheduler: SyncScheduler

    lateinit var authRepository: AuthRepository
        private set
    lateinit var voidingRepository: VoidingRepository
        private set
    lateinit var lockRepository: LockRepository
        private set

    lateinit var addVoidingEventUseCase: AddVoidingEventUseCase
        private set
    lateinit var getDailyEventsUseCase: GetDailyEventsUseCase
        private set
    lateinit var getDailyCountUseCase: GetDailyCountUseCase
        private set
    lateinit var deleteVoidingEventUseCase: DeleteVoidingEventUseCase
        private set
    lateinit var syncEventsUseCase: SyncEventsUseCase
        private set

    fun init(context: Context) {
        db = AppDatabase.create(context)
        sessionStore = SessionStore(context)
        pinStore = PinStore(context)
        supabaseApi = SupabaseApi()
        supabaseAuthClient = SupabaseAuthClient()
        syncScheduler = SyncScheduler(context)

        authRepository = AuthRepositoryImpl(
            appContext = context.applicationContext,
            api = supabaseApi,
            authClient = supabaseAuthClient,
            sessionStore = sessionStore
        )
        voidingRepository = VoidingRepositoryImpl(
            db = db,
            authRepository = authRepository,
            api = supabaseApi,
            syncScheduler = syncScheduler
        )
        lockRepository = LockRepositoryImpl(
            authRepository = authRepository,
            pinStore = pinStore
        )

        addVoidingEventUseCase = AddVoidingEventUseCase(voidingRepository)
        getDailyEventsUseCase = GetDailyEventsUseCase(voidingRepository)
        getDailyCountUseCase = GetDailyCountUseCase(voidingRepository)
        deleteVoidingEventUseCase = DeleteVoidingEventUseCase(voidingRepository)
        syncEventsUseCase = SyncEventsUseCase(voidingRepository)
    }
}
