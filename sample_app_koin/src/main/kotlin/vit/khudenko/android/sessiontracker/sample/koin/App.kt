package vit.khudenko.android.sessiontracker.sample.koin;

import android.app.Application
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import vit.khudenko.android.sessiontracker.SessionTracker
import vit.khudenko.android.sessiontracker.sample.koin.di.appModule
import vit.khudenko.android.sessiontracker.sample.koin.di.viewModelModule

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@App)
            androidLogger()
            modules(
                listOf(
                    appModule,
                    viewModelModule
                    // ... other modules
                )
            )
        }

        val sessionStore = getKoin().get<SessionTracker<Session, Session.Event, Session.State>>()
        sessionStore.initialize()
    }
}