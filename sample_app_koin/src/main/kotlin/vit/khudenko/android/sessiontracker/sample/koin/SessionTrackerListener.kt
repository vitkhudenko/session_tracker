package vit.khudenko.android.sessiontracker.sample.koin

import android.app.Application
import android.util.Log
import org.koin.android.ext.android.getKoin
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import vit.khudenko.android.sessiontracker.SessionId
import vit.khudenko.android.sessiontracker.SessionRecord
import vit.khudenko.android.sessiontracker.SessionTracker
import vit.khudenko.android.sessiontracker.sample.koin.Session.Event
import vit.khudenko.android.sessiontracker.sample.koin.Session.State
import vit.khudenko.android.sessiontracker.sample.koin.di.SCOPE_ID_USER_SESSION

class SessionTrackerListener(private val app: Application) : SessionTracker.Listener<Event, State> {

    companion object {
        private val TAG = SessionTrackerListener::class.java.simpleName
    }

    private var scope: Scope? = null

    override fun onSessionTrackerInitialized(
        sessionTracker: SessionTracker<Event, State>,
        sessionRecords: List<SessionRecord<State>>
    ) {
        Log.d(TAG, "onSessionTrackerInitialized")
        check(sessionRecords.count { (_, state) -> state == State.ACTIVE } <= 1) {
            "One active session is allowed at most"
        }
        sessionRecords.forEach { (sessionId, state) ->
            if (state == State.ACTIVE) {
                createKoinScope(sessionId)
            }
        }
    }

    override fun onSessionTrackingStarted(
        sessionTracker: SessionTracker<Event, State>,
        sessionRecord: SessionRecord<State>
    ) {
        val (sessionId, initState) = sessionRecord
        Log.d(TAG, "onSessionTrackingStarted: session ID = ${sessionId}, initState = $initState")
        if (initState == State.ACTIVE) {
            createKoinScope(sessionId)
        }
    }

    override fun onSessionTrackingStopped(
        sessionTracker: SessionTracker<Event, State>,
        sessionRecord: SessionRecord<State>
    ) {
        val (sessionId, state) = sessionRecord
        Log.d(TAG, "onSessionTrackingStopped: session ID = ${sessionId}, state = $state")
        closeKoinScope(sessionId)
    }

    override fun onSessionStateChanged(
        sessionTracker: SessionTracker<Event, State>,
        sessionRecord: SessionRecord<State>,
        oldState: State
    ) {
        val (sessionId, newState) = sessionRecord
        Log.d(TAG, "onSessionStateChanged: session ID = ${sessionId}, states = ($oldState -> $newState)")
        when (newState) {
            State.INACTIVE -> closeKoinScope(sessionId)
            State.ACTIVE -> createKoinScope(sessionId)
            State.FORGOTTEN -> closeKoinScope(sessionId)
        }
    }

    override fun onAllSessionsTrackingStopped(
        sessionTracker: SessionTracker<Event, State>,
        sessionRecords: List<SessionRecord<State>>
    ) {
        Log.d(TAG, "onAllSessionsTrackingStopped")
        sessionRecords.forEach { (sessionId, _) -> closeKoinScope(sessionId) }
    }

    private fun closeKoinScope(sessionId: SessionId) {
        if (scope != null) {
            Log.d(TAG, "closeKoinScope: session ID = '$sessionId'")
            scope!!.close()
            scope = null
        } else {
            Log.d(TAG, "closeKoinScope: no scope to close for session with ID '$sessionId'")
        }
    }

    private fun createKoinScope(sessionId: SessionId) {
        if (scope != null) {
            Log.w(TAG, "createKoinScope: scope already exists for session with ID '$sessionId'")
        } else {
            Log.d(TAG, "createKoinScope: session ID = '$sessionId'")
            scope = app.getKoin().createScope(
                qualifier = named(SCOPE_ID_USER_SESSION),
                scopeId = sessionId
            )
        }
    }
}