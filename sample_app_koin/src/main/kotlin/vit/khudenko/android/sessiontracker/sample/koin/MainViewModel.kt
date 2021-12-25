package vit.khudenko.android.sessiontracker.sample.koin

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vit.khudenko.android.sessiontracker.SessionId
import vit.khudenko.android.sessiontracker.SessionTracker
import vit.khudenko.android.sessiontracker.sample.koin.util.BaseViewModel

class MainViewModel(
    private val sessionTracker: SessionTracker<Session.Event, Session.State>
) : BaseViewModel() {

    private val state = MutableStateFlow<State>(State.Idle)

    fun onLogOutButtonClicked(sessionId: SessionId) {
        state.value = State.Progress
        viewModelScope.launch {
            sessionTracker.consumeEvent(sessionId, Session.Event.LOGOUT)
            delay(2000)
            state.value = State.Success
        }
    }

    fun onLogOutAndForgetButtonClicked(sessionId: SessionId) {
        state.value = State.Progress
        viewModelScope.launch {
            sessionTracker.consumeEvent(sessionId, Session.Event.LOGOUT_AND_FORGET)
            delay(2000)
            state.value = State.Success
        }
    }

    fun stateFlow(): Flow<State> = state.asStateFlow()

    sealed class State {
        object Idle : State()
        object Progress : State()
        object Success : State()
    }
}