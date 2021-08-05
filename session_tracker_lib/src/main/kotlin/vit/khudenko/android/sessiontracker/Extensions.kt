package vit.khudenko.android.sessiontracker

/**
 * A helper function to reduce code footprint when specifying [`ISessionStateTransitionsSupplier`][ISessionStateTransitionsSupplier]
 * for [`SessionTracker`][SessionTracker].
 *
 * @param transitions one or more instances of [`Pair`][Pair]<[`Event`][Event], [`List`][List]<[`State`][State]>> each corresponding
 *                    to a [`Transition`][Transition].
 * @return [`ISessionStateTransitionsSupplier`][ISessionStateTransitionsSupplier] instance
 *
 * @see [Transition]
 *
 * @param [Event] event parameter of enum type.
 * @param [State] state parameter of enum type.
 */
fun <Event : Enum<Event>, State : Enum<State>> sessionStateTransitionsSupplier(
    vararg transitions: Pair<Event, List<State>>
): ISessionStateTransitionsSupplier<Event, State> {
    return transitions
        .map { (event, statePath) ->
            Transition(event, statePath.toList())
        }
        .let { transitionList ->
            ISessionStateTransitionsSupplier { transitionList }
        }
}

