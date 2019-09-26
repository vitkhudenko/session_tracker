package vit.khudenko.android.sessiontracker

/**
 * Your application must assume [`loadSessionsData()`][loadSessionsData] and [`saveSessionsData()`][saveSessionsData]
 * methods may be called by [`SessionTracker`][SessionTracker] while processing the calls made by your application
 * to the `SessionTracker`.
 *
 * `SessionTracker` calls `ISessionTrackerStorage` synchronously from the threads your application calls
 * `SessionTracker` from.
 *
 * `SessionTracker` implementation guarantees that [`loadSessionsData()`][loadSessionsData]
 * and [`saveSessionsData()`][saveSessionsData] methods are never called concurrently.
 */
interface ISessionTrackerStorage<S : ISession, State : Enum<State>> {

    /**
     * This method may be called by `SessionTracker` from within the
     * [`SessionTracker.trackSession()`][SessionTracker.trackSession],
     * [`SessionTracker.consumeEvent()`][SessionTracker.consumeEvent],
     * [`SessionTracker.untrackSession()`][SessionTracker.untrackSession] or
     * [`SessionTracker.untrackAllSessions()`][SessionTracker.untrackAllSessions] calls.
     *
     * The implementation must not defer actual persisting for future.
     *
     * @param sessionsData list of [`ISession`][ISession] instances with corresponding states.
     */
    fun saveSessionsData(sessionsData: List<Pair<S, State>>)

    /**
     * This is called by `SessionTracker` from within the
     * [`SessionTracker.initialize()`][SessionTracker.initialize] call.
     *
     * The implementation should read and create previously persisted (if any) list of [`ISession`][ISession]
     * instances with corresponding states.
     */
    fun loadSessionsData(): List<Pair<S, State>>
}
