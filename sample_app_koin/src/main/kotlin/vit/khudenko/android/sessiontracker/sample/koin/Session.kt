package vit.khudenko.android.sessiontracker.sample.koin

class Session {

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