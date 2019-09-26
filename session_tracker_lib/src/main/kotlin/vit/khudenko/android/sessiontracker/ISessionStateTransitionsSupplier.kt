package vit.khudenko.android.sessiontracker

/**
 * During [`SessionTracker.initialize()`][SessionTracker.initialize] and
 * [`SessionTracker.trackSession()`][SessionTracker.trackSession] calls, the state transitions returned by this
 * supplier will be used to configure session state machine.
 *
 * A sample (assuming your app has `Session` class, that defines specific to your app events and states
 * (`Session.Event` and `Session.State` enums) could be as this:
 *
 * ```
 *     listOf(
 *         Transition(
 *             event = Session.Event.LOGIN,
 *             statePath = listOf(Session.State.INACTIVE, Session.State.ACTIVE)
 *         ),
 *         Transition(
 *             event = Session.Event.LOGOUT,
 *             statePath = listOf(Session.State.ACTIVE, Session.State.INACTIVE)
 *         ),
 *         Transition(
 *             event = Session.Event.LOGOUT_AND_FORGET,
 *             statePath = listOf(Session.State.ACTIVE, Session.State.FORGOTTEN)
 *         )
 *     )
 * ```
 *
 * @param [S] [`ISession`][ISession] implementation.
 * @param [Event] event parameter of enum type.
 * @param [State] state parameter of enum type.
 */
interface ISessionStateTransitionsSupplier<S : ISession, Event : Enum<Event>, State : Enum<State>> {

    /**
     * @return a list of transitions to configure `session`'s state
     * machine with.
     *
     * Transition consists of event and state path. State path is a list of states.
     *
     * Each transition defines its identity as a combination of the event and the starting state (the first item
     * in the state path).
     *
     * Session state machine configuration will fail (throwing a validation exception) in the following cases:
     * - if state path is empty or has a single state
     * - if statePath does not consist of unique states
     * - if a duplicate transition identified (by a combination of event and starting state)
     *
     * @param session [`ISession`][ISession] implementation.
     */
    fun getStateTransitions(session: S): List<Transition<Event, State>>
}