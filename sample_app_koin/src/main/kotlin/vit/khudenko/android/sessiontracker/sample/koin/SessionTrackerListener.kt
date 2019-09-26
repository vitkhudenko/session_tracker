package vit.khudenko.android.sessiontracker.sample.koin

import android.app.Application
import android.util.Log
import org.koin.android.ext.android.getKoin
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import vit.khudenko.android.sessiontracker.SessionTracker
import vit.khudenko.android.sessiontracker.sample.koin.Session.State
import vit.khudenko.android.sessiontracker.sample.koin.di.SCOPE_ID_USER_SESSION

class SessionTrackerListener(private val app: Application) : SessionTracker.Listener<Session, State> {

    companion object {
        private val TAG = SessionTrackerListener::class.java.simpleName
    }

    private var scope: Scope? = null

    override fun onSessionTrackingStarted(session: Session, initState: State) {
        Log.d(TAG, "onSessionTrackingStarted: session ID = ${session.sessionId}, initState = $initState")
        if (initState == State.ACTIVE) {
            createKoinScope(session.sessionId)
        }
    }

    override fun onSessionTrackingStopped(session: Session, state: State) {
        Log.d(TAG, "onSessionTrackingStopped: session ID = ${session.sessionId}, state = $state")
        closeKoinScope(session.sessionId)
    }

    override fun onSessionStateChanged(session: Session, oldState: State, newState: State) {
        Log.d(TAG, "onSessionStateChanged: session ID = ${session.sessionId}, states = ($oldState -> $newState)")
        when (newState) {
            State.INACTIVE -> closeKoinScope(session.sessionId)
            State.ACTIVE -> createKoinScope(session.sessionId)
            State.FORGOTTEN -> closeKoinScope(session.sessionId)
        }
    }

    private fun closeKoinScope(sessionId: String) {
        if (scope != null) {
            Log.d(TAG, "closeKoinScope: session ID = '$sessionId'")
            scope!!.close()
            scope = null
        } else {
            Log.d(TAG, "closeKoinScope: no scope to close for session with ID '$sessionId'")
        }
    }

    private fun createKoinScope(sessionId: String) {
        if (scope != null) {
            Log.w(TAG, "createKoinScope: scope already exists for session with ID '$sessionId'")
        } else {
            Log.d(TAG, "createKoinScope: session ID = '$sessionId'")
            scope = app.getKoin().createScope(
                qualifier = named(SCOPE_ID_USER_SESSION), // scopeName
                scopeId = sessionId
            )
        }
    }
}