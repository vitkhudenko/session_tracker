package vit.khudenko.android.sessiontracker

/**
 * A representation of a session tracked by [`SessionTracker`][SessionTracker].
 *
 * @see SessionTracker.Listener
 * @see ISessionTrackerStorage
 */
data class SessionRecord<State : Enum<State>>(
    val sessionId: SessionId,
    val state: State
) {
    /**
     * A workaround for accessing [sessionId] from a Java codebase (since SessionId is a Kotlin value class).
     */
    @JvmName("sessionId")
    fun sessionId(): SessionId = sessionId
}
