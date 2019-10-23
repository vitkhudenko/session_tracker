package vit.khudenko.android.sessiontracker

import vit.khudenko.android.fsm.StateMachine

/**
 * A transition defines its identity as a pair of the [`event`][event] and the starting state
 * (the first item in the [`statePath`][statePath]). `StateMachine` allows unique transitions
 * only (each transition must have a unique identity).
 *
 * @param event [`Event`][Event] - triggering event for this transition.
 * @param statePath a list of states for this transition.
 *                  First item is a starting state for the transition.
 *                  Must have at least two items, and all items must be unique.
 *
 * @throws [IllegalArgumentException] if statePath is empty or has a single item
 * @throws [IllegalArgumentException] if statePath does not consist of unique items
 *
 * @param [Event] event parameter of enum type.
 * @param [State] state parameter of enum type.
 */
class Transition<Event : Enum<Event>, State : Enum<State>>(
    val event: Event,
    val statePath: List<State>
) {
    init {
        // trigger validation
        StateMachine.Transition(event, statePath)
    }
}