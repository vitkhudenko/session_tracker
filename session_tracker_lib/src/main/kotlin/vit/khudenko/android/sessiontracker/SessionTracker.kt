package vit.khudenko.android.sessiontracker

import android.util.Log
import vit.khudenko.android.fsm.StateMachine
import java.util.*

/**
 * ## TL;DR
 *
 * SessionTracker is a general purpose framework to provide a foundation for session management in your app.
 *
 * Your app provides (a) session tracking storage implementation and (b) session tracking state machine configuration,
 * while SessionTracker provides callbacks to create/update/release session resources.
 *
 * ## Contract description
 *
 * ### What is session?
 *
 * Session is a flexible entity - it could be a user session with user signing in/out or a bluetooth device session
 * with all its possible states.
 *
 * ### Session tracking state VS. session state
 *
 * In SessionTracker framework, sessions are represented by session tracking records - instances of
 * [`SessionRecord`][SessionRecord]. It is an immutable data structure, that has just 2 fields - session ID and
 * session tracking state.
 *
 * Note, there are two (partially intersecting) types of session state:
 * 1. The session state that is tracked by SessionTracker, which is always an instance of enum by the contract.
 * 2. The session state that is specific to your app, which can be as diverse as your app's business logic requires
 * and which can not be represented by the [`SessionRecord`][SessionRecord].
 *
 * Please don't mess one with another. Actual implementation of session, including its persistence, is up to your app
 * and is out of SessionTracker framework responsibility. It is correct say that session tracking state (the one
 * tracked by SessionTracker) is a subset of a full session state in your app.
 *
 * ### Persistence
 *
 * SessionTracker framework supports session tracking auto-restoration on application process restarts. Your
 * app must provide an implementation of [`ISessionTrackerStorage`][ISessionTrackerStorage], which is used by
 * SessionTracker to make CRUD operations on session tracking records.
 *
 * ### Session tracking state machine
 *
 * SessionTracker maintains a state machine per session. Your app must define a set of possible events and
 * states per session. Using events and states, your app should provide state machine transitions, which are
 * used to configure session state machine. For example, your app may define the following session tracking events
 * and states:
 *
 * ```kotlin
 *     enum class State {
 *         INACTIVE, ACTIVE
 *     }
 *     enum class Event {
 *         LOGIN, LOGOUT
 *     }
 * ```
 *
 * then a sample transitions config (an implementation of
 * [`ISessionStateTransitionsSupplier`][ISessionStateTransitionsSupplier]) could be as following:
 *
 * ```kotlin
 *     val sessionStateTransitionsSupplier = object : ISessionStateTransitionsSupplier<Event, State> {
 *         override fun getStateTransitions(sessionId: SessionId) = listOf(
 *             Transition(
 *                 event = Event.LOGIN,
 *                 statePath = listOf(State.INACTIVE, State.ACTIVE)
 *             ),
 *             Transition(
 *                 event = Event.LOGOUT,
 *                 statePath = listOf(State.ACTIVE, State.INACTIVE)
 *             )
 *         )
 *     }
 * ```
 *
 * Such config would mean there are two possible session tracking states (`ACTIVE`/`INACTIVE`) and two possible session
 * tracking events: `LOGIN` (to move session from `INACTIVE` to `ACTIVE` state) and `LOGOUT` (to move session from
 * `ACTIVE` to `INACTIVE` state).
 *
 * ### Session tracking
 *
 * In order to make SessionTracker ready to function it should be initialized first. The most appropriate place for
 * [`initialize()`][initialize] call is
 * [`Application.onCreate()`][android.app.Application.onCreate].
 *
 * Suppose your user hits "Login" button, your app authenticates user and creates a session. In order to make
 * use of SessionTracker the session should be "attached" to SessionTracker:
 *
 * ```kotlin
 *     sessionTracker.trackSession(sessionId, State.ACTIVE)
 * ```
 *
 * Now SessionTracker is tracking the session until your app calls [`untrackSession(sessionId)`][untrackSession].
 * Next time your app starts (and SessionTracker is initialized), the session tracking will be automatically restored
 * by SessionTracker with the same `ACTIVE` state.
 *
 * As long as session is tracked, its session tracking state changes are propagated to your app via
 * [`SessionTracker.Listener`][SessionTracker.Listener].
 *
 * Suppose eventually your user hits "Log Out" button, then your app is responsible to communicate this event
 * to SessionTracker by asking to consume `LOGOUT` event for the session:
 *
 * ```kotlin
 *     sessionTracker.consumeEvent(sessionId, Event.LOGOUT)
 * ```
 *
 * Now SessionTracker updates session tracking state to `INACTIVE`, persists session record with the new state and
 * propagates this state change via [`SessionTracker.Listener`][SessionTracker.Listener]. Note, the session
 * is still tracked by SessionTracker, so next time your app starts, the session tracking will be automatically restored
 * by SessionTracker with the same `INACTIVE` state.
 *
 * ### Management of session resources
 *
 * [`SessionTracker.Listener`][SessionTracker.Listener] has useful for your app callbacks that allow to manage session
 * resources appropriately:
 *
 * - `onSessionTrackingStarted(sessionTracker: SessionTracker<Event, State>, sessionRecord: SessionRecord<State>)` -
 *     SessionTracker has added session to the list of tracked sessions.
 *     This happens as a result of calling [`SessionTracker.trackSession(sessionId, state)`][trackSession] or
 *     [`SessionTracker.initialize()`][initialize].
 *     This callback is the right place to create any resources for the session (a DB connection, a DI scope, etc.)
 *     depending on the initState.
 *
 * - `onSessionStateChanged(sessionTracker: SessionTracker<Event, State>, sessionRecord: SessionRecord<State>, oldState: State)` -
 *     session tracking state has changed.
 *     This happens as a result of calling [`SessionTracker.consumeEvent(sessionId, event)`][consumeEvent].
 *     This callback is the right place to create or release any resources for the session (a DB connection,
 *     a DI scope, etc.).
 *
 * - `onSessionTrackingStopped(sessionTracker: SessionTracker<Event, State>, sessionRecord: SessionRecord<State>)` -
 *     SessionTracker has removed session from the list of tracked sessions. This happens as a result
 *     of calling [`SessionTracker.untrackSession(sessionId)`][untrackSession].
 *     This may also happen as a result of calling [`SessionTracker.consumeEvent`][consumeEvent] if session
 *     appears in one of the [`autoUntrackStates`][autoUntrackStates].
 *     This callback is the right place to release any resources for the session (a DB connection, a DI scope, etc.).
 *
 * - `onAllSessionsTrackingStopped(sessionTracker: SessionTracker<Event, State>, sessionRecords: List<SessionRecord<State>>)` -
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
class SessionTracker<Event : Enum<Event>, State : Enum<State>>(
    private val sessionTrackerStorage: ISessionTrackerStorage<State>,
    private val sessionStateTransitionsSupplier: ISessionStateTransitionsSupplier<Event, State>,
    private val listener: Listener<Event, State>,
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
     * A listener, through which the session tracking lifecycle and state changes are communicated.
     *
     * @see [onSessionTrackingStarted]
     * @see [onSessionTrackingStopped]
     * @see [onSessionStateChanged]
     * @see [onAllSessionsTrackingStopped]
     */
    interface Listener<Event : Enum<Event>, State : Enum<State>> {

        /**
         * The `SessionTracker` has added session to the list of tracked sessions. This happens as a result
         * of calling [`SessionTracker.trackSession()`][trackSession] or [`SessionTracker.initialize()`][initialize].
         *
         * This callback is the right place to create any resources for the session
         * (a DB connection, a DI scope, etc.).
         */
        fun onSessionTrackingStarted(
            sessionTracker: SessionTracker<Event, State>,
            sessionRecord: SessionRecord<State>
        )

        /**
         * The session tracking state has changed from `oldState` to `newState`.
         * This happens as a result of calling [`SessionTracker.consumeEvent()`][consumeEvent].
         *
         * This callback is the right place to create or release any resources
         * for the session (a DB connection, a DI scope, etc.).
         */
        fun onSessionStateChanged(
            sessionTracker: SessionTracker<Event, State>,
            sessionRecord: SessionRecord<State>,
            oldState: State
        )

        /**
         * The `SessionTracker` has removed session from the list of tracked sessions. This happens as a result
         * of calling [`SessionTracker.untrackSession()`][untrackSession].
         *
         * This may also happen as a result of calling [`SessionTracker.consumeEvent()`][SessionTracker.consumeEvent]
         * if session appears in one of the [`autoUntrackStates`][autoUntrackStates].
         *
         * This callback is the right place to release any resources for
         * the session (a DB connection, a DI scope, etc.).
         */
        fun onSessionTrackingStopped(
            sessionTracker: SessionTracker<Event, State>,
            sessionRecord: SessionRecord<State>
        )

        /**
         * The `SessionTracker` has removed all sessions from the list of tracked sessions. This happens as a result
         * of calling [`SessionTracker.untrackAllSessions()`][untrackAllSessions].
         *
         * This callback is the right place to release any resources for
         * the sessions (a DB connection, a DI scope, etc.).
         */
        fun onAllSessionsTrackingStopped(
            sessionTracker: SessionTracker<Event, State>,
            sessionRecords: List<SessionRecord<State>>
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
    private val sessionsMap = LinkedHashMap<SessionId, SessionInfo<Event, State>>()
    private var persisting = false

    /**
     * Must be called before calling any other methods.
     *
     * Subsequent calls are ignored.
     *
     * This method calls [`ISessionTrackerStorage.readAllSessionRecords()`][ISessionTrackerStorage.readAllSessionRecords],
     * starts tracking the obtained session records and notifies session tracker listener (see
     * [`Listener.onSessionTrackingStarted()`][Listener.onSessionTrackingStarted]).
     *
     * @throws [RuntimeException] for a strict [`mode`][mode], if [`autoUntrackStates`][autoUntrackStates] are defined
     * AND session is in one of such states. For a relaxed [`mode`][mode] it just logs an error message and skips such
     * session from tracking.
     * @throws [RuntimeException] for a strict [`mode`][mode], if
     * [`sessionStateTransitionsSupplier`][sessionStateTransitionsSupplier] returns transitions that cause validation
     * errors while creating session tracking state machine. For a relaxed [`mode`][mode] it just logs an error
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
            .filter { sessionRecord ->
                sessionRecord.state in autoUntrackStates
            }.forEach { (sessionId, state) ->
                val explanation = "session with ID '${sessionId}' is in auto-untrack state (${state})"
                if (mode.strict) {
                    throw RuntimeException("Unable to initialize $TAG: $explanation")
                } else {
                    logger.e(TAG, "initialize: $explanation, rejecting this session")
                }
            }

        loadedSessionRecords
            .filterNot { sessionRecord ->
                sessionRecord.state in autoUntrackStates
            }
            .map { sessionRecord ->
                val stateMachine = try {
                    setupSessionStateMachine(sessionRecord)
                } catch (e: Exception) {
                    throw RuntimeException(
                        "Unable to initialize $TAG: error creating ${StateMachine::class.java.simpleName}", e
                    )
                }
                sessionRecord to stateMachine
            }
            .forEach { (sessionRecord, stateMachine) ->
                sessionsMap[sessionRecord.sessionId] = SessionInfo(stateMachine)
                listener.onSessionTrackingStarted(this@SessionTracker, sessionRecord)
            }

        initialized = true

        if (mode.verbose) {
            logger.d(TAG, "initialize: done, took ${System.currentTimeMillis() - startedAt} ms")
        }
    }

    /**
     * @return a list of the currently tracked session records.
     *
     * @throws [RuntimeException] for a strict [`mode`][mode], if `SessionTracker` has not been initialized.
     * For a relaxed [`mode`][mode] it just logs an error message and returns an empty list.
     */
    @Synchronized
    fun getSessionRecords(): List<SessionRecord<State>> {
        return if (ensureInitialized("getSessionRecords")) {
            val sessionRecords = sessionsMap.entries.map {
                SessionRecord(sessionId = it.key, state = it.value.stateMachine.getCurrentState())
            }.toMutableList()
            if (mode.verbose) {
                val dump = sessionRecords.joinToString(
                    prefix = "[", postfix = "]"
                ) { (sessionId, state) -> "{ '${sessionId}': $state }" }
                logger.d(TAG, "getSessionRecords: $dump")
            }
            sessionRecords
        } else {
            emptyList()
        }
    }

    /**
     * Starts tracking a session for the sessionId, persists a new session record via
     * [`ISessionTrackerStorage`][ISessionTrackerStorage] and notifies session tracker listener
     * (see [`SessionTracker.Listener.onSessionTrackingStarted()`][Listener.onSessionTrackingStarted]).
     *
     * If session with the same sessionId is already tracked, then the call does nothing.
     *
     * @param sessionId [`SessionId`][SessionId] - ID of the session to track.
     * @param state [`State`][State] - initial session tracking state.
     *
     * @throws [IllegalArgumentException] for a strict [`mode`][mode], if [`autoUntrackStates`][autoUntrackStates]
     * are defined AND sessionId is in one of such states. For a relaxed [`mode`][mode] it just logs an error message
     * and does nothing.
     * @throws [RuntimeException] for a strict [`mode`][mode], if `SessionTracker` has not been initialized.
     * For a relaxed [`mode`][mode] it just logs an error message and does nothing.
     * @throws [RuntimeException] for a strict [`mode`][mode], if
     * [`sessionStateTransitionsSupplier`][sessionStateTransitionsSupplier] returns transitions that
     * cause validation errors while creating session tracking state machine. For a relaxed [`mode`][mode] it just logs
     * an error message and does nothing.
     * @throws [RuntimeException] for a strict [`mode`][mode], if this call is initiated from the
     * [`sessionTrackerStorage`][sessionTrackerStorage]. For a relaxed [`mode`][mode] it just logs an error message
     * and does nothing.
     */
    @Synchronized
    fun trackSession(sessionId: SessionId, state: State) {
        if (!ensureInitialized("trackSession")) {
            return
        }
        if (mode.verbose) {
            logger.d(TAG, "trackSession: sessionId = '${sessionId}', state = $state")
        }
        if (!ensureNotPersisting("trackSession")) {
            return
        }
        if (sessionsMap.contains(sessionId)) {
            logger.w(TAG, "trackSession: session with ID '${sessionId}' already exists")
        } else {
            if (state in autoUntrackStates) {
                val explanation = "session with ID '${sessionId}' is in auto-untrack state ($state)"
                require(mode.strict.not()) { "Unable to track session: $explanation" }
                logger.e(TAG, "trackSession: $explanation, rejecting this session")
            } else {
                val sessionRecord = SessionRecord(sessionId, state)
                val stateMachine = try {
                    setupSessionStateMachine(sessionRecord)
                } catch (e: Exception) {
                    throw RuntimeException(
                        "$TAG failed to track session: error creating ${StateMachine::class.java.simpleName}", e
                    )
                }
                doPersistAction { sessionTrackerStorage.createSessionRecord(sessionRecord) }
                sessionsMap[sessionId] = SessionInfo(stateMachine)
                listener.onSessionTrackingStarted(this@SessionTracker, sessionRecord)
            }
        }
    }

    /**
     * Stops tracking a session with specified `sessionId`, removes corresponding session record from persistent storage
     * (via [`ISessionTrackerStorage`][ISessionTrackerStorage] implementation) and notifies session tracker listener
     * (see [`SessionTracker.Listener.onSessionTrackingStopped()`][Listener.onSessionTrackingStopped]).
     *
     * If `SessionTracker` does not track a session with specified `sessionId`, then this call does nothing.
     *
     * Note, this method does not modify session state.
     *
     * Note, it's possible to define [`autoUntrackStates`][autoUntrackStates] via `SessionTracker` constructor, so
     * sessions are untracked automatically at [`SessionTracker.consumeEvent()`][consumeEvent].
     *
     * @param sessionId [`SessionId`][SessionId].
     *
     * @throws [RuntimeException] for a strict [`mode`][mode], if `SessionTracker` has not been initialized.
     * For a relaxed [`mode`][mode] it just logs an error message and does nothing.
     * @throws [RuntimeException] for a strict [`mode`][mode], if this call is initiated from the
     * [`sessionTrackerStorage`][sessionTrackerStorage]. For a relaxed [`mode`][mode] it just logs an error message
     * and does nothing.
     */
    @Synchronized
    fun untrackSession(sessionId: SessionId) {
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
                doUntrackSession(sessionId, sessionInfo.stateMachine)
            }
        }
    }

    /**
     * Stops tracking all currently tracked sessions, removes session records from persistent storage (via
     * [`ISessionTrackerStorage`][ISessionTrackerStorage] implementation) and notifies session tracker listener
     * (see [`Listener.onAllSessionsTrackingStopped()`][Listener.onAllSessionsTrackingStopped]).
     *
     * If `SessionTracker` does not track any sessions, then this call does nothing.
     *
     * Note, this method does not modify session tracking state of the session records.
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

            sessionsMap.values.forEach { it.stateMachine.removeAllListeners() }

            val sessionRecords = sessionsMap.entries.map { (sessionId, sessionInfo) ->
                SessionRecord(sessionId, sessionInfo.stateMachine.getCurrentState())
            }

            sessionsMap.clear()

            listener.onAllSessionsTrackingStopped(this@SessionTracker, sessionRecords)
        }
    }

    /**
     * Attempts to apply the specified [`event`][event] to the specified session. Whether the event actually causes
     * session tracking state change depends on the session state machine configuration and current session tracking
     * state. If session tracking state change occurs, then updated session record is persisted
     * (via [`ISessionTrackerStorage`][ISessionTrackerStorage]) and session tracking listener is notified.
     *
     * If, as a result of the event consuming, the session appears in a one of the
     * [`autoUntrackStates`][autoUntrackStates] (assuming these were defined), then `SessionTracker` also stops
     * tracking the session, removes corresponding session record from the persistent storage and notifies session
     * tracking listener.
     *
     * If `SessionTracker` does not track a session with specified `sessionId`, then this call does nothing
     * and returns `false`.
     *
     * @param sessionId [`SessionId`][SessionId].
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
    fun consumeEvent(sessionId: SessionId, event: Event): Boolean {
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

    private fun doUntrackSession(sessionId: SessionId, stateMachine: StateMachine<Event, State>) {
        stateMachine.removeAllListeners()
        doPersistAction { sessionTrackerStorage.deleteSessionRecord(sessionId) }
        sessionsMap.remove(sessionId)
        listener.onSessionTrackingStopped(this@SessionTracker, SessionRecord(sessionId, stateMachine.getCurrentState()))
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

    private fun setupSessionStateMachine(sessionRecord: SessionRecord<State>): StateMachine<Event, State> {
        val (sessionId, state) = sessionRecord

        val builder = StateMachine.Builder<Event, State>().setInitialState(state)

        sessionStateTransitionsSupplier.getStateTransitions(sessionId)
            .forEach { (event, statePath) ->
                builder.addTransition(event, statePath)
            }

        val stateMachine = builder.build()

        stateMachine.addListener(object : StateMachine.Listener<State> {
            override fun onStateChanged(oldState: State, newState: State) {
                val baseLogMessage = "onStateChanged: '$oldState' -> '$newState', sessionId = '${sessionId}'"

                val sessionInfo = sessionsMap[sessionId]

                checkNotNull(sessionInfo) { "$baseLogMessage - session not found" }
                check(sessionInfo.isUntracking.not()) { "$baseLogMessage - session is untracking" }

                if (mode.verbose) {
                    logger.d(TAG, baseLogMessage)
                }

                val updatedSessionRecord = SessionRecord(sessionId, newState)

                if (newState in autoUntrackStates) {
                    logger.d(TAG, "$baseLogMessage, going to auto-untrack session..")
                    val updatedSessionInfo = sessionInfo.copy(isUntracking = true)
                    sessionsMap[sessionId] = updatedSessionInfo
                    stateMachine.removeAllListeners()
                    listener.onSessionStateChanged(this@SessionTracker, updatedSessionRecord, oldState)
                    if (sessionsMap.containsKey(sessionId)) {
                        doUntrackSession(sessionId, updatedSessionInfo.stateMachine)
                    }
                } else {
                    doPersistAction { sessionTrackerStorage.updateSessionRecord(updatedSessionRecord) }
                    listener.onSessionStateChanged(this@SessionTracker, updatedSessionRecord, oldState)
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

    private data class SessionInfo<Event : Enum<Event>, State : Enum<State>>(
        val stateMachine: StateMachine<Event, State>,
        val isUntracking: Boolean = false
    )
}

typealias SessionId = String
