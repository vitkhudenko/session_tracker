package vit.khudenko.android.sessiontracker

/**
 * A structure to transfer data between [`SessionTracker`][SessionTracker] and client persisting layer.
 *
 * @see ISessionTrackerStorage
 */
data class SessionRecord<S : ISession, State : Enum<State>>(
    val session: S,
    val state: State
)