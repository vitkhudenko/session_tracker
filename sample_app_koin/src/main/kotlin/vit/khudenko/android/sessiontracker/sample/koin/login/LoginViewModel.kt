package vit.khudenko.android.sessiontracker.sample.koin.login

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import vit.khudenko.android.sessiontracker.SessionTracker
import vit.khudenko.android.sessiontracker.sample.koin.Session
import vit.khudenko.android.sessiontracker.sample.koin.util.BaseViewModel

class LoginViewModel(
    private val sessionTracker: SessionTracker<Session.Event, Session.State>
) : BaseViewModel() {

    private val state = MutableStateFlow<State>(State.Idle)

    fun onLoginButtonClicked(userId: String) {
        state.value = State.Progress

        viewModelScope.launch {
            val targetSessionRecord = sessionTracker.getSessionRecords()
                .firstOrNull { record -> record.sessionId == userId }
            if (targetSessionRecord == null) {
                sessionTracker.trackSession(userId, Session.State.ACTIVE)
            } else {
                sessionTracker.consumeEvent(userId, Session.Event.LOGIN)
            }
            delay(2000)
            state.value = State.Success(userId)
        }
    }

    fun stateFlow(): Flow<State> = state

    sealed class State {
        object Idle : State()
        object Progress : State()
        class Success(val userId: String) : State()
    }
}
