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
)