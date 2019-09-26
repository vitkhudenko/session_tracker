package vit.khudenko.android.sessiontracker

data class Transition<Event : Enum<Event>, State : Enum<State>>(
    val event: Event,
    val statePath: List<State>
)