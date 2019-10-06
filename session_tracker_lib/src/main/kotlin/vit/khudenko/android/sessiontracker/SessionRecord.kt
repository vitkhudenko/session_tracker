package vit.khudenko.android.sessiontracker

/**
 * A structure to transfer data between [`SessionTracker`][SessionTracker] and client persisting layer.
 *
 * @see ISessionTrackerStorage
 */
data class SessionRecord<State : Enum<State>>(
    val sessionId: SessionId,
    val state: State
)