package vit.khudenko.android.sessiontracker

import android.util.Log
import vit.khudenko.android.fsm.StateMachine
import java.util.*

/**
 * ## TL;DR
 *
 * SessionTracker is a general purpose framework to provide a foundation for session management in your app.
 *
 * Your app provides (a) session storage implementation and (b) session state machine configuration while SessionTracker
 * provides callbacks to create/release session resources.
 *
 * ## Contract description
 *
 * ### What is session?
 *
 * Session is a flexible entity - it could be a user that logs in/out or a bluetooth device with all its possible
 * states. In SessionTracker framework, sessions are represented by [`ISession`][ISession] interface with the only
 * requirement to have a unique session ID. Actual implementation of ISession is up to your app.
 *
 * ### Persistence
 *
 * SessionTracker framework supports automatic session initialization on application process restarts. Your
 * app must provide an implementation of [`ISessionTrackerStorage`][ISessionTrackerStorage], which is used by
 * SessionTracker to persist/read sessions data.
 *
 * ### Session states and events
 *
 * SessionTracker maintains a state machine per session. Your app must define a set of possible events and
 * states per session. Using events and states your app should provide state machine transitions, which are
 * used to configure session state machine. For example, your app may define the following session events and states:
 *
 * ```kotlin
 *     class Session(override val sessionId: String) : ISession {
 *         enum class State {
 *             INACTIVE, ACTIVE
 *         }
 *         enum class Event {
 *             LOGIN, LOGOUT
 *         }
 *     }
 * ```
 *
 * then a sample transitions config (an implementation of
 * [`ISessionStateTransitionsSupplier`][ISessionStateTransitionsSupplier]) could be as following:
 *
 * ```kotlin
 *     val sessionStateTransitionsSupplier =
 *             object : ISessionStateTransitionsSupplier<Session, Session.Event, Session.State> {
 *         override fun getStateTransitions(session: Session) = listOf(
 *             Transition(
 *                 event = Session.Event.LOGIN,
 *                 statePath = listOf(Session.State.INACTIVE, Session.State.ACTIVE)
 *             ),
 *             Transition(
 *                 event = Session.Event.LOGOUT,
 *                 statePath = listOf(Session.State.ACTIVE, Session.State.INACTIVE)
 *             )
 *         )
 *     }
 * ```
 *
 * Such config would mean there are two possible session states (`ACTIVE`/`INACTIVE`) and two possible events: `LOGIN`
 * (to move session from `INACTIVE` to `ACTIVE` state) and `LOGOUT` (to move session from `ACTIVE` to `INACTIVE` state).
 *
 * ### Session tracking
 *
 * In order to make SessionTracker ready to function it should be initialized first. The most appropriate place for
 * [`initialize()`][initialize] call is
 * [`Application.onCreate()`][android.app.Application.onCreate].
 *
 * Suppose your user hits "Login" button, your app authenticates user and creates a Session object. In order to make
 * use of SessionTracker the session should be "attached" to SessionTracker:
 *
 * ```kotlin
 *     sessionTracker.trackSession(session, Session.State.ACTIVE)
 * ```
 *
 * Now SessionTracker is tracking the session until your app calls [`untrackSession(sessionId)`][untrackSession].
 * Next time your app starts (and SessionTracker is initialized), the session will be automatically tracked by
 * SessionTracker with the same `ACTIVE` state.
 *
 * As long as session is tracked, its state changes are propagated to your app via
 * [`SessionTracker.Listener`][SessionTracker.Listener].
 *
 * Suppose eventually your user hits "Log Out" button, then your app is responsible to communicate this event
 * to SessionTracker by asking to consume `LOGOUT` event for the session:
 *
 * ```kotlin
 *     sessionTracker.consumeEvent(session.sessionId, Session.Event.LOGOUT)
 * ```
 *
 * Now SessionTracker updates session state to `INACTIVE`, propagates this state change via
 * [`SessionTracker.Listener`][SessionTracker.Listener] and persists session with its new state . Note, the session
 * is still tracked by SessionTracker, so next time your app starts, the session will be automatically picked up for
 * tracking by SessionTracker with the same `INACTIVE` state.
 *
 * ### Management of session resources
 *
 * [`SessionTracker.Listener`][SessionTracker.Listener] has useful for your app callbacks that allow to manage session
 * resources appropriately:
 *
 * - `onSessionTrackingStarted(sessionTracker: SessionTracker<S, Event, State>, session: S, initState: State)` -
 *     SessionTracker has added session to the list of tracked sessions.
 *     This happens as a result of calling [`SessionTracker.trackSession(session, state)`][trackSession] or
 *     [`SessionTracker.initialize()`][initialize].
 *     This callback is the right place to create any resources for the session (a DB connection, a DI scope, etc.)
 *     depending on the initState.
 *
 * - `onSessionStateChanged(sessionTracker: SessionTracker<S, Event, State>, session: S, oldState: State, newState: State)` -
 *     session state has changed from oldState to newState.
 *     This happens as a result of calling [`SessionTracker.consumeEvent(sessionId, event)`][consumeEvent].
 *     This callback is the right place to create or release any resources for the session (a DB connection,
 *     a DI scope, etc.).
 *
 * - `onSessionTrackingStopped(sessionTracker: SessionTracker<S, Event, State>, session: S, state: State)` -
 *     SessionTracker has removed session from the list of tracked sessions. This happens as a result
 *     of calling [`SessionTracker.untrackSession(sessionId)`][untrackSession].
 *     This may also happen as a result of calling [`SessionTracker.consumeEvent`][consumeEvent] if session
 *     appears in one of the [`autoUntrackStates`][autoUntrackStates].
 *     This callback is the right place to release any resources for the session (a DB connection, a DI scope, etc.).
 *
 * - `onAllSessionsTrackingStopped(sessionTracker: SessionTracker<S, Event, State>, sessionsData: List<Pair<S, State>>)` -
 *     SessionTracker has removed session from the list of tracked sessions. This happens as a result
 *     of calling [`SessionTracker.untrackAllSessions()`][untrackAllSessions].
 *     This callback is the right place to release any resources for the sessions (a DB connection, a DI scope, etc.).
 *
 * ## Threading
 *
 * SessionTracker is thread-safe. Public methods are declared as `synchronized`. Thread-safe compound actions are
 * possible by using synchronized statement on `SessionTracker` instance:
 *
 * ```kotlin
 *     synchronized(sessionTracker) {
 *         sessionTracker.consumeEvent(..) // step 1 of the compound action
 *         sessionTracker.consumeEvent(..) // step 2 of the compound action
 *     }
 * ```
 *
 * SessionTracker is a synchronous tool, meaning it neither creates threads nor uses thread-pools.
 *
 * ## Miscellaneous
 *
 * Typical SessionTracker usage implies being a singleton in your app.
 */
class SessionTracker<S : ISession, Event : Enum<Event>, State : Enum<State>>(
    private val sessionTrackerStorage: ISessionTrackerStorage<S, State>,
    private val sessionStateTransitionsSupplier: ISessionStateTransitionsSupplier<S, Event, State>,
    private val listener: Listener<S, Event, State>,
    /**
     * If a session appears in one of these states, then `SessionTracker` automatically untracks such session.
     * The effect of automatic untracking is similar to making an explicit [`untrackSession()`][untrackSession] call.
     *
     * @see [consumeEvent]
     * @see [untrackSession]
     * @see [untrackAllSessions]
     */
    private val autoUntrackStates: Set<State>,
    private val mode: Mode,
    private val logger: Logger = Logger.DefaultImpl()
) {

    companion object {
        internal val TAG = SessionTracker::class.java.simpleName
    }

    /**
     * Defines misuse/misconfiguration tolerance and amount of logging.
     *
     * @see [Mode.STRICT]
     * @see [Mode.STRICT_VERBOSE]
     * @see [Mode.RELAXED]
     * @see [Mode.RELAXED_VERBOSE]
     */
    enum class Mode(val strict: Boolean, val verbose: Boolean) {

        /**
         * In this mode SessionTracker does not tolerate most of the misuse/misconfiguration issues by crashing the app.
         *
         * @see [initialize]
         * @see [trackSession]
         * @see [untrackSession]
         * @see [consumeEvent]
         */
        STRICT(strict = true, verbose = false),

        /**
         * Same as [`STRICT`][STRICT], but with more logging.
         */
        STRICT_VERBOSE(strict = true, verbose = true),

        /**
         * In this mode SessionTracker tries to overstep misuse/misconfiguration issues (if possible) by just logging
         * the issue and turning an operation to 'no op'.
         *
         * @see [initialize]
         * @see [trackSession]
         * @see [untrackSession]
         * @see [consumeEvent]
         */
        RELAXED(strict = false, verbose = false),

        /**
         * Same as [`RELAXED`][RELAXED], but with more logging.
         */
        RELAXED_VERBOSE(strict = false, verbose = true)
    }

    /**
     * A listener, through which the [`ISession`][ISession]'s lifecycle and state changes are communicated.
     *
     * @see [onSessionTrackingStarted]
     * @see [onSessionTrackingStopped]
     * @see [onSessionStateChanged]
     */
    interface Listener<S : ISession, Event : Enum<Event>, State : Enum<State>> {

        /**
         * The `SessionTracker` has added `session` to the list of tracked sessions. This happens as a result
         * of calling [`SessionTracker.trackSession()`][trackSession] or [`SessionTracker.initialize()`][initialize].
         *
         * This callback is the right place to create any resources for the `session`
         * (a DB connection, a DI scope, etc.) depending on the `initState`.
         */
        fun onSessionTrackingStarted(
            sessionTracker: SessionTracker<S, Event, State>,
            session: S, initState: State
        )

        /**
         * The `session` state has changed from `oldState` to `newState`.
         * This happens as a result of calling [`SessionTracker.consumeEvent()`][consumeEvent].
         *
         * This callback is the right place to create or release any resources
         * for the `session` (a DB connection, a DI scope, etc.).
         */
        fun onSessionStateChanged(
            sessionTracker: SessionTracker<S, Event, State>,
            session: S,
            oldState: State,
            newState: State
        )

        /**
         * The `SessionTracker` has removed `session` from the list of tracked sessions. This happens as a result
         * of calling [`SessionTracker.untrackSession()`][untrackSession].
         *
         * This may also happen as a result of calling [`SessionTracker.consumeEvent()`][SessionTracker.consumeEvent]
         * if session appears in one of the [`autoUntrackStates`][autoUntrackStates].
         *
         * This callback is the right place to release any resources for
         * the `session` (a DB connection, a DI scope, etc.).
         */
        fun onSessionTrackingStopped(
            sessionTracker: SessionTracker<S, Event, State>,
            session: S,
            state: State
        )

        /**
         * The `SessionTracker` has removed all sessions from the list of tracked sessions. This happens as a result
         * of calling [`SessionTracker.untrackAllSessions()`][untrackAllSessions].
         *
         * This callback is the right place to release any resources for
         * the sessions (a DB connection, a DI scope, etc.).
         */
        fun onAllSessionsTrackingStopped(
            sessionTracker: SessionTracker<S, Event, State>,
            sessionsData: List<Pair<S, State>>
        )
    }

    interface Logger {
        fun d(tag: String, message: String)
        fun w(tag: String, message: String)
        fun e(tag: String, message: String)

        /**
         * Default implementation of [`Logger`][Logger] that uses [`android.util.Log`][android.util.Log].
         */
        class DefaultImpl : Logger {
            override fun d(tag: String, message: String) {
                Log.d(tag, message)
            }

            override fun w(tag: String, message: String) {
                Log.w(tag, message)
            }

            override fun e(tag: String, message: String) {
                Log.e(tag, message)
            }
        }
    }

    private var initialized: Boolean = false
    private val sessionsMap = LinkedHashMap<String, SessionInfo<S, Event, State>>()
    private var persisting = false

    /**
     * Must be called before calling any other methods.
     *
     * Subsequent calls are ignored.
     *
     * This method calls [`ISessionTrackerStorage.readAllSessionRecords()`][ISessionTrackerStorage.readAllSessionRecords],
     * starts tracking the obtained sessions and notifies sessions listener (see
     * [`Listener.onSessionTrackingStarted()`][Listener.onSessionTrackingStarted]).
     *
     * @throws [RuntimeException] for a strict [`mode`][mode], if [`autoUntrackStates`][autoUntrackStates] are defined
     * AND session is in one of such states. For a relaxed [`mode`][mode] it just logs an error message and skips such
     * session from tracking.
     * @throws [RuntimeException] for a strict [`mode`][mode], if
     * [`sessionStateTransitionsSupplier`][sessionStateTransitionsSupplier] returns transitions that cause validation
     * errors while creating session's state machine. For a relaxed [`mode`][mode] it just logs an error
     * message and skips such session from tracking.
     */
    @Synchronized
    fun initialize() {
        val startedAt = System.currentTimeMillis()

        if (initialized) {
            logger.w(TAG, "initialize: already initialized, skipping..")
            return
        }

        if (mode.verbose) {
            logger.d(TAG, "initialize: starting..")
        }

        val loadedSessionRecords = sessionTrackerStorage.readAllSessionRecords()

        loadedSessionRecords
            .filter { (_, state) ->
                state in autoUntrackStates
            }.forEach { (session, state) ->
                val explanation = "session with ID '${session.sessionId}' is in auto-untrack state (${state})"
                if (mode.strict) {
                    throw RuntimeException("Unable to initialize $TAG: $explanation")
                } else {
                    logger.e(TAG, "initialize: $explanation, rejecting this session")
                }
            }

        loadedSessionRecords
            .filterNot { (_, state) ->
                state in autoUntrackStates
            }
            .map { (session, state) ->
                val stateMachine = try {
                    setupSessionStateMachine(session, state)
                } catch (e: Exception) {
                    throw RuntimeException(
                        "Unable to initialize $TAG: error creating ${StateMachine::class.java.simpleName}", e
                    )
                }
                SessionInfo(session, stateMachine)
            }
            .forEach { sessionInfo ->
                val (session, stateMachine) = sessionInfo
                sessionsMap[session.sessionId] = sessionInfo
                listener.onSessionTrackingStarted(this@SessionTracker, session, stateMachine.getCurrentState())
            }

        initialized = true

        if (mode.verbose) {
            logger.d(TAG, "initialize: done, took ${System.currentTimeMillis() - startedAt} ms")
        }
    }

    /**
     * @return a list of the currently tracked sessions with corresponding states.
     *
     * @throws [RuntimeException] for a strict [`mode`][mode], if `SessionTracker` has not been initialized.
     * For a relaxed [`mode`][mode] it just logs an error message and returns an empty list.
     */
    @Synchronized
    fun getSessions(): List<SessionRecord<S, State>> {
        return if (ensureInitialized("getSessions")) {
            val sessions = sessionsMap.values
                .map { it.toSessionRecord() }
                .toMutableList()
            if (mode.verbose) {
                val dump = sessions.joinToString(
                    prefix = "[", postfix = "]"
                ) { (session, state) -> "{ '${session.sessionId}': $state }" }
                logger.d(TAG, "getSessions: $dump")
            }
            sessions
        } else {
            emptyList()
        }
    }

    /**
     * Starts tracking a session, notifies session listener (see
     * [`SessionTracker.Listener.onSessionTrackingStarted()`][Listener.onSessionTrackingStarted]) and
     * persists sessions via [`ISessionTrackerStorage`][ISessionTrackerStorage].
     *
     * If session with the same ID is already present, then the call does nothing.
     *
     * @param session [`ISession`][ISession] to track.
     * @param state initial [`State`][State] of the `session`.
     *
     * @throws [IllegalArgumentException] for a strict [`mode`][mode], if [`autoUntrackStates`][autoUntrackStates]
     * are defined AND session is in one of such states. For a relaxed [`mode`][mode] it just logs an error message
     * and does nothing.
     * @throws [RuntimeException] for a strict [`mode`][mode], if `SessionTracker` has not been initialized.
     * For a relaxed [`mode`][mode] it just logs an error message and does nothing.
     * @throws [RuntimeException] for a strict [`mode`][mode], if
     * [`sessionStateTransitionsSupplier`][sessionStateTransitionsSupplier] returns transitions that
     * cause validation errors while creating session's state machine. For a relaxed [`mode`][mode] it just logs
     * an error message and does nothing.
     * @throws [RuntimeException] for a strict [`mode`][mode], if this call is initiated from the
     * [`sessionTrackerStorage`][sessionTrackerStorage]. For a relaxed [`mode`][mode] it just logs an error message
     * and does nothing.
     */
    @Synchronized
    fun trackSession(session: S, state: State) {
        if (!ensureInitialized("trackSession")) {
            return
        }
        if (mode.verbose) {
            logger.d(TAG, "trackSession: sessionId = '${session.sessionId}', state = $state")
        }
        if (!ensureNotPersisting("trackSession")) {
            return
        }
        if (sessionsMap.contains(session.sessionId)) {
            logger.w(TAG, "trackSession: session with ID '${session.sessionId}' already exists")
        } else {
            if (state in autoUntrackStates) {
                val explanation = "session with ID '${session.sessionId}' is in auto-untrack state ($state)"
                if (mode.strict) {
                    throw IllegalArgumentException("Unable to track session: $explanation")
                }
                logger.e(TAG, "trackSession: $explanation, rejecting this session")
            } else {
                val stateMachine = try {
                    setupSessionStateMachine(session, state)
                } catch (e: Exception) {
                    throw RuntimeException(
                        "$TAG failed to track session: error creating ${StateMachine::class.java.simpleName}", e
                    )
                }
                val sessionInfo = SessionInfo(session, stateMachine)
                doPersistAction { sessionTrackerStorage.createSessionRecord(sessionInfo.toSessionRecord()) }
                sessionsMap[session.sessionId] = sessionInfo
                listener.onSessionTrackingStarted(this@SessionTracker, session, stateMachine.getCurrentState())
            }
        }
    }

    /**
     * Stops tracking a session with specified `sessionId`, notifies session listener
     * (see [`SessionTracker.Listener.onSessionTrackingStopped()`][Listener.onSessionTrackingStopped]) and removes
     * session from persistent storage (via [`ISessionTrackerStorage`][ISessionTrackerStorage] implementation).
     *
     * If `SessionTracker` does not track a session with specified `sessionId`, then this call does nothing.
     *
     * Note, this method does not modify session state.
     *
     * Note, it's possible to define [`autoUntrackStates`][autoUntrackStates] via `SessionTracker` constructor, so
     * sessions are untracked automatically at [`SessionTracker.consumeEvent()`][consumeEvent].
     *
     * @param sessionId [String] session ID.
     *
     * @throws [RuntimeException] for a strict [`mode`][mode], if `SessionTracker` has not been initialized.
     * For a relaxed [`mode`][mode] it just logs an error message and does nothing.
     * @throws [RuntimeException] for a strict [`mode`][mode], if this call is initiated from the
     * [`sessionTrackerStorage`][sessionTrackerStorage]. For a relaxed [`mode`][mode] it just logs an error message
     * and does nothing.
     */
    @Synchronized
    fun untrackSession(sessionId: String) {
        if (!ensureInitialized("untrackSession")) {
            return
        }
        if (mode.verbose) {
            logger.d(TAG, "untrackSession: sessionId = '${sessionId}'")
        }
        if (!ensureNotPersisting("untrackSession")) {
            return
        }
        val sessionInfo = sessionsMap[sessionId]
        if (sessionInfo == null) {
            logger.d(TAG, "untrackSession: no session with ID '$sessionId' found")
        } else {
            if (sessionInfo.isUntracking) {
                logger.w(TAG, "untrackSession: session with ID '$sessionId' is already untracking")
            } else {
                sessionsMap[sessionId] = sessionInfo.copy(isUntracking = true)
                doUntrackSession(sessionInfo)
            }
        }
    }

    /**
     * Stops tracking all the currently tracked sessions, notifies sessions listener for each session (see
     * [`Listener.onSessionTrackingStopped`][Listener.onSessionTrackingStopped]) and removes sessions from persistent
     * storage (via [`ISessionTrackerStorage`][ISessionTrackerStorage] implementation).
     *
     * If `SessionTracker` does not track any sessions, then this call does nothing.
     *
     * Note, this method does not modify session state.
     *
     * Note, it's possible to define [`autoUntrackStates`][autoUntrackStates] via `SessionTracker` constructor, so
     * sessions are untracked automatically at [`SessionTracker.consumeEvent()`][consumeEvent].
     *
     * @throws [RuntimeException] for a strict [`mode`][mode], if `SessionTracker` has not been initialized.
     * For a relaxed [`mode`][mode] it just logs an error message and does nothing.
     * @throws [RuntimeException] for a strict [`mode`][mode], if this call is initiated from the
     * [`sessionTrackerStorage`][sessionTrackerStorage]. For a relaxed [`mode`][mode] it just logs an error message
     * and does nothing.
     */
    @Synchronized
    fun untrackAllSessions() {
        if (!ensureInitialized("untrackAllSessions")) {
            return
        }
        if (!ensureNotPersisting("untrackAllSessions")) {
            return
        }
        if (sessionsMap.isEmpty()) {
            if (mode.verbose) {
                logger.d(TAG, "untrackAllSessions: no sessions found")
            }
        } else {
            if (mode.verbose) {
                logger.d(TAG, "untrackAllSessions")
            }

            doPersistAction { sessionTrackerStorage.deleteAllSessionRecords() }

            sessionsMap.values.forEach { (_, stateMachine) -> stateMachine.removeAllListeners() }

            val sessionsWithState = sessionsMap.values.map { (session, stateMachine) ->
                session to stateMachine.getCurrentState()
            }

            sessionsMap.clear()

            listener.onAllSessionsTrackingStopped(this@SessionTracker, sessionsWithState)
        }
    }

    /**
     * Attempts to apply the specified [`event`][event] to the specified session. Whether the event actually causes
     * session state change depends on the session state machine configuration and current session state. If session
     * state change occurs, then sessions listener is notified and sessions are persisted (via
     * [`ISessionTrackerStorage`][ISessionTrackerStorage]).
     *
     * If, as a result of the event consuming, the session appears in a one of the
     * [`autoUntrackStates`][autoUntrackStates] (assuming these were defined), then `SessionTracker` stops
     * tracking such session, notifies session listener and removes this session from persistent storage.
     *
     * If `SessionTracker` does not track a session with specified `sessionId`, then this call does nothing
     * and returns `false`.
     *
     * @param sessionId `String`.
     * @param event [`Event`][Event].
     *
     * @return flag whether the event was consumed (meaning moving to a new state) or ignored.
     *
     * @throws [RuntimeException] for a strict [`mode`][mode], if `SessionTracker` has not been initialized.
     * For a relaxed [`mode`][mode] it just logs an error message and returns false.
     * @throws [RuntimeException] for a strict [`mode`][mode], if this call is initiated from the
     * [`sessionTrackerStorage`][sessionTrackerStorage]. For a relaxed [`mode`][mode] it just logs an error message
     * and returns false.
     */
    @Synchronized
    fun consumeEvent(sessionId: String, event: Event): Boolean {
        if (!ensureInitialized("consumeEvent")) {
            return false
        }
        if (mode.verbose) {
            logger.d(TAG, "consumeEvent: sessionId = '$sessionId', event = '$event'")
        }
        if (!ensureNotPersisting("consumeEvent")) {
            return false
        }
        val sessionInfo = sessionsMap[sessionId]
        if (sessionInfo == null) {
            logger.w(TAG, "consumeEvent: no session with ID '$sessionId' found")
        } else {
            if (sessionInfo.isUntracking) {
                logger.w(TAG, "consumeEvent: event = '$event', session with ID '$sessionId' is already untracking")
            } else if (sessionInfo.stateMachine.consumeEvent(event)) {
                return true
            }
            if (mode.verbose) {
                logger.d(
                    TAG, "consumeEvent: event '$event' was ignored for session with ID '$sessionId' " +
                            "in state ${sessionInfo.stateMachine.getCurrentState()}, " +
                            "isUntracking = ${sessionInfo.isUntracking}"
                )
            }
        }
        return false
    }

    private fun doUntrackSession(sessionInfo: SessionInfo<S, Event, State>) {
        val (session, stateMachine) = sessionInfo
        stateMachine.removeAllListeners()
        doPersistAction { sessionTrackerStorage.deleteSessionRecord(session.sessionId) }
        sessionsMap.remove(session.sessionId)
        listener.onSessionTrackingStopped(this@SessionTracker, session, stateMachine.getCurrentState())
    }

    private fun ensureInitialized(method: String): Boolean {
        if (!initialized) {
            val explanation = "$TAG must be initialized before calling its #$method method"
            if (mode.strict) {
                throw RuntimeException(explanation)
            } else {
                logger.e(TAG, explanation)
            }
        }
        return initialized
    }

    private fun ensureNotPersisting(method: String): Boolean {
        if (persisting) {
            val explanation = "$method: misuse detected, accessing " +
                    "$TAG from ${ISessionTrackerStorage::class.java.simpleName} callbacks is not allowed"
            if (mode.strict) {
                throw RuntimeException(explanation)
            } else {
                logger.e(TAG, explanation)
            }
        }
        return !persisting
    }

    private fun setupSessionStateMachine(session: S, state: State): StateMachine<Event, State> {
        val builder = StateMachine.Builder<Event, State>().setInitialState(state)

        sessionStateTransitionsSupplier.getStateTransitions(session)
            .forEach { (event, statePath) ->
                builder.addTransition(event, statePath)
            }

        val stateMachine = builder.build()

        stateMachine.addListener(object : StateMachine.Listener<State> {
            override fun onStateChanged(oldState: State, newState: State) {
                val baseLogMessage = "onStateChanged: '$oldState' -> '$newState', sessionId = '${session.sessionId}'"
                val sessionInfo = sessionsMap[session.sessionId]
                if (sessionInfo != null) {
                    if (sessionInfo.isUntracking) {
                        logger.w(TAG, "$baseLogMessage, session is untracking, so ignoring state change")
                    } else {
                        if (mode.verbose) {
                            logger.d(TAG, baseLogMessage)
                        }
                        if (newState in autoUntrackStates) {
                            logger.d(TAG, "$baseLogMessage, going to auto-untrack session..")
                            val updatedSessionInfo = sessionInfo.copy(isUntracking = true)
                            sessionsMap[session.sessionId] = updatedSessionInfo
                            stateMachine.removeAllListeners()
                            listener.onSessionStateChanged(this@SessionTracker, session, oldState, newState)
                            if (sessionsMap.contains(session.sessionId)) {
                                doUntrackSession(updatedSessionInfo)
                            }
                        } else {
                            doPersistAction { sessionTrackerStorage.updateSessionRecord(sessionInfo.toSessionRecord()) }
                            listener.onSessionStateChanged(this@SessionTracker, session, oldState, newState)
                        }
                    }
                } else {
                    logger.d(TAG, "$baseLogMessage, session not found")
                }
            }
        })

        return stateMachine
    }

    private fun doPersistAction(action: () -> Unit) {
        persisting = true
        try {
            action.invoke()
        } finally {
            persisting = false
        }
    }

    private data class SessionInfo<S : ISession, Event : Enum<Event>, State : Enum<State>>(
        val session: S,
        val stateMachine: StateMachine<Event, State>,
        val isUntracking: Boolean = false
    ) {
        fun toSessionRecord() = SessionRecord(session, stateMachine.getCurrentState())
    }
}
