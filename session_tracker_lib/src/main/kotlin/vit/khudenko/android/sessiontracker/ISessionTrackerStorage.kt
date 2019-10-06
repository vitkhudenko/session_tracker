package vit.khudenko.android.sessiontracker

/**
 * Your app must assume `ISessionTrackerStorage` methods may be called by [`SessionTracker`][SessionTracker]
 * while processing the calls made by your app to the `SessionTracker`.
 *
 * `SessionTracker` calls `ISessionTrackerStorage` synchronously from the threads your application calls
 * `SessionTracker` from.
 *
 * `SessionTracker` implementation guarantees that `ISessionTrackerStorage` methods are never called concurrently.
 */
interface ISessionTrackerStorage<State : Enum<State>> {

    /**
     * This method is called by `SessionTracker` from within the
     * [`SessionTracker.trackSession()`][SessionTracker.trackSession] call.
     *
     * The implementation must not defer actual persisting for future.
     *
     * @param sessionRecord [`SessionRecord`][SessionRecord]
     */
    fun createSessionRecord(sessionRecord: SessionRecord<State>)

    /**
     * This is called by `SessionTracker` from within the
     * [`SessionTracker.initialize()`][SessionTracker.initialize] call.
     *
     * The implementation should read and create previously persisted (if any) list of [`SessionRecord`][SessionRecord]
     * instances with corresponding states. If storage is empty, then an empty list should be returned.
     */
    fun readAllSessionRecords() : List<SessionRecord<State>>

    /**
     * This method is called by `SessionTracker` from within the
     * [`SessionTracker.consumeEvent()`][SessionTracker.consumeEvent] call.
     *
     * The implementation must not defer actual persisting for future.
     *
     * @param sessionRecord [`SessionRecord`][SessionRecord]
     */
    fun updateSessionRecord(sessionRecord: SessionRecord<State>)

    /**
     * This method is called by `SessionTracker` from within the
     * [`SessionTracker.untrackSession()`][SessionTracker.untrackSession] call.
     *
     * The implementation must not defer actual persisting for future.
     *
     * @param sessionId [`SessionId`][SessionId]
     */
    fun deleteSessionRecord(sessionId: SessionId)

    /**
     * This method is called by `SessionTracker` from within the
     * [`SessionTracker.untrackAllSessions()`][SessionTracker.untrackAllSessions] call.
     *
     * The implementation must not defer actual persisting for future.
     */
    fun deleteAllSessionRecords()
}
