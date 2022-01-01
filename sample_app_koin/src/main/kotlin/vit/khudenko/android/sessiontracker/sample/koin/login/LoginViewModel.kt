package vit.khudenko.android.sessiontracker.sample.koin.login

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vit.khudenko.android.sessiontracker.SessionId
import vit.khudenko.android.sessiontracker.SessionTracker
import vit.khudenko.android.sessiontracker.sample.koin.Session
import vit.khudenko.android.sessiontracker.sample.koin.util.BaseViewModel

class LoginViewModel(
    private val sessionTracker: SessionTracker<Session.Event, Session.State>
) : BaseViewModel() {

    private val state = MutableStateFlow<State>(State.Idle)

    fun onLoginButtonClicked(sessionId: SessionId) {
        state.value = State.Progress

        viewModelScope.launch {
            val targetSessionRecord = sessionTracker.getSessionRecords()
                .firstOrNull { record -> record.sessionId == sessionId }
            if (targetSessionRecord == null) {
                sessionTracker.trackSession(sessionId, Session.State.ACTIVE)
            } else {
                sessionTracker.consumeEvent(sessionId, Session.Event.LOGIN)
            }
            delay(2000)
            state.value = State.Success(sessionId)
        }
    }

    fun stateFlow(): Flow<State> = state.asStateFlow()

    sealed class State {
        object Idle : State()
        object Progress : State()
        class Success(val sessionId: SessionId) : State()
    }
}
