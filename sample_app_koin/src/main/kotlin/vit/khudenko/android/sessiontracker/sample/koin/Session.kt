package vit.khudenko.android.sessiontracker.sample.koin

import vit.khudenko.android.sessiontracker.ISession

class Session(override val sessionId: String) : ISession {

    enum class State {
        INACTIVE,
        ACTIVE,
        FORGOTTEN
    }

    enum class Event {
        LOGIN,
        LOGOUT,
        LOGOUT_AND_FORGET
    }
}