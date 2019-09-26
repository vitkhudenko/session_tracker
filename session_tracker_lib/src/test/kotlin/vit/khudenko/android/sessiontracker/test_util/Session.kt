package vit.khudenko.android.sessiontracker.test_util

import vit.khudenko.android.sessiontracker.ISession

open class Session(override val sessionId: String) : ISession {

    enum class Event {
        LOGIN, LOGOUT, LOGOUT_AND_FORGET
    }

    enum class State {
        ACTIVE, INACTIVE, FORGOTTEN
    }
}