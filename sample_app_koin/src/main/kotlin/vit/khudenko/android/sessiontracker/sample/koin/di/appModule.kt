package vit.khudenko.android.sessiontracker.sample.koin.di

import android.content.Context
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import vit.khudenko.android.sessiontracker.ISessionStateTransitionsSupplier
import vit.khudenko.android.sessiontracker.SessionId
import vit.khudenko.android.sessiontracker.SessionTracker
import vit.khudenko.android.sessiontracker.Transition
import vit.khudenko.android.sessiontracker.sample.koin.Session.Event
import vit.khudenko.android.sessiontracker.sample.koin.Session.State
import vit.khudenko.android.sessiontracker.sample.koin.SessionTrackerListener
import vit.khudenko.android.sessiontracker.sample.koin.SessionTrackerStorage

val appModule = module {

    single(named(DI_NAME_SESSION_SHARED_PREFERENCES)) {
        androidContext().getSharedPreferences("session", Context.MODE_PRIVATE)
    }

    single {
        SessionTracker(
            sessionTrackerStorage = SessionTrackerStorage(androidApplication()),
            sessionStateTransitionsSupplier = object : ISessionStateTransitionsSupplier<Event, State> {
                override fun getStateTransitions(sessionId: SessionId) = listOf(
                    Transition(event = Event.LOGIN, statePath = listOf(State.INACTIVE, State.ACTIVE)),
                    Transition(event = Event.LOGOUT, statePath = listOf(State.ACTIVE, State.INACTIVE)),
                    Transition(event = Event.LOGOUT_AND_FORGET, statePath = listOf(State.ACTIVE, State.FORGOTTEN))
                )
            },
            listener = SessionTrackerListener(androidApplication()),
            autoUntrackStates = setOf(State.FORGOTTEN),
            mode = SessionTracker.Mode.STRICT_VERBOSE
        )
    }

}