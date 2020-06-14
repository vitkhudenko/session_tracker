package vit.khudenko.android.sessiontracker

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import vit.khudenko.android.sessiontracker.test_util.Event
import vit.khudenko.android.sessiontracker.test_util.State
import vit.khudenko.android.sessiontracker.test_util.assertThrows
import vit.khudenko.android.sessiontracker.test_util.createSessionStateTransitionsSupplierMock
import vit.khudenko.android.sessiontracker.test_util.createStorageMock
import vit.khudenko.android.sessiontracker.test_util.verifyInitialization

class SessionTrackerRelaxedModeTest {

    private val mode = SessionTracker.Mode.RELAXED
    private val modeVerbose = SessionTracker.Mode.RELAXED_VERBOSE

    private lateinit var logger: SessionTracker.Logger
    private lateinit var storage: ISessionTrackerStorage<State>
    private lateinit var listener: SessionTracker.Listener<Event, State>
    private lateinit var sessionStateTransitionsSupplier: ISessionStateTransitionsSupplier<Event, State>

    @Before
    fun setUp() {
        logger = mock()
        listener = mock()
        storage = createStorageMock(emptyList())
        sessionStateTransitionsSupplier = createSessionStateTransitionsSupplierMock()
    }

    @Test
    fun initialization() {
        val sessionRecords = listOf(SessionRecord("session_id", State.ACTIVE))

        storage = createStorageMock(sessionRecords)

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        verifyInitialization(sessionTracker, sessionRecords, logger, storage, listener, mode)
    }

    @Test
    fun `initialization in verbose mode`() {
        val sessionRecords = listOf(SessionRecord("session_id", State.ACTIVE))

        storage = createStorageMock(sessionRecords)

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = modeVerbose,
            logger = logger
        )

        verifyInitialization(sessionTracker, sessionRecords, logger, storage, listener, modeVerbose)
    }

    @Test
    fun `initialization with session in a auto-untrack state`() {
        val sessionId = "session_id"

        storage = createStorageMock(listOf(SessionRecord(sessionId, State.FORGOTTEN)))

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = setOf(State.FORGOTTEN),
            mode = mode,
            logger = logger
        )

        sessionTracker.initialize()

        verify(storage).readAllSessionRecords()
        verify(logger).e(
            SessionTracker.TAG,
            "initialize: session with ID '${sessionId}' is in auto-untrack state (${State.FORGOTTEN})" +
                    ", rejecting this session"
        )
        verify(listener).onSessionTrackerInitialized(sessionTracker, emptyList())
        verifyNoMoreInteractions(storage, listener)
        verifyZeroInteractions(sessionStateTransitionsSupplier)
    }

    @Test
    fun `initialization without state transitions fails on state machine's builder validation`() {
        storage = createStorageMock(listOf(SessionRecord("session_id", State.ACTIVE)))

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = mock {
                on { getStateTransitions(any()) } doReturn emptyList() // incomplete config
            },
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        assertThrows(RuntimeException::class.java, "Unable to initialize SessionTracker: error creating StateMachine") {
            sessionTracker.initialize()
        }

        verify(storage).readAllSessionRecords()
        verifyNoMoreInteractions(storage)
        verifyZeroInteractions(listener)
    }

    @Test
    fun `initialization happens only once, subsequent calls are ignored`() {
        val sessionRecords = listOf(SessionRecord("session_id", State.ACTIVE))
        storage = createStorageMock(sessionRecords)

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        verifyInitialization(sessionTracker, sessionRecords, logger, storage, listener, mode)

        sessionTracker.initialize()

        verify(logger).w(SessionTracker.TAG, "initialize: already initialized, skipping..")
        verifyZeroInteractions(listener, storage)

        assertEquals(sessionRecords, sessionTracker.getSessionRecords())
    }

    @Test
    fun trackSession() {
        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        verifyInitialization(sessionTracker, emptyList(), logger, storage, listener, mode)

        val sessionRecord = SessionRecord("session_id", State.ACTIVE)

        sessionTracker.trackSession(sessionRecord.sessionId, sessionRecord.state)

        with(inOrder(storage, listener)) {
            verify(storage).createSessionRecord(sessionRecord)
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionRecord)
        }

        verifyNoMoreInteractions(storage, listener)

        assertEquals(listOf(sessionRecord), sessionTracker.getSessionRecords())
    }

    @Test
    fun `trackSession() in verbose mode`() {
        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = modeVerbose,
            logger = logger
        )

        verifyInitialization(sessionTracker, emptyList(), logger, storage, listener, modeVerbose)

        val sessionRecord = SessionRecord("session_id", State.ACTIVE)

        sessionTracker.trackSession(sessionRecord.sessionId, sessionRecord.state)

        with(inOrder(storage, listener, logger)) {
            verify(logger).d(
                SessionTracker.TAG,
                "trackSession: sessionId = '${sessionRecord.sessionId}', state = ${sessionRecord.state}"
            )
            verify(storage).createSessionRecord(sessionRecord)
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionRecord)
        }

        verifyNoMoreInteractions(storage, listener, logger)

        assertEquals(listOf(sessionRecord), sessionTracker.getSessionRecords())
    }

    @Test
    fun `trackSession() with session in an auto-untrack state`() {
        val sessionId = "session_id"
        val state = State.FORGOTTEN

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = setOf(state),
            mode = mode,
            logger = logger
        )

        verifyInitialization(sessionTracker, emptyList(), logger, storage, listener, mode)

        sessionTracker.trackSession(sessionId, state)

        verify(logger).e(
            SessionTracker.TAG,
            "trackSession: session with ID '${sessionId}' is in auto-untrack state (${State.FORGOTTEN}), " +
                    "rejecting this session"
        )
        verifyZeroInteractions(storage, sessionStateTransitionsSupplier, listener)

        assertTrue(sessionTracker.getSessionRecords().isEmpty())
    }

    @Test
    fun `trackSession() with already tracked session`() {
        val sessionRecord = SessionRecord("session_id", State.ACTIVE)

        storage = createStorageMock(listOf(sessionRecord))

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        verifyInitialization(sessionTracker, listOf(sessionRecord), logger, storage, listener, mode)

        sessionTracker.trackSession(sessionRecord.sessionId, State.INACTIVE)

        verify(logger).w(
            SessionTracker.TAG,
            "trackSession: session with ID '${sessionRecord.sessionId}' already exists"
        )

        verifyZeroInteractions(storage, listener)

        assertEquals(listOf(sessionRecord), sessionTracker.getSessionRecords())
    }

    @Test
    fun `trackSession() without state transitions fails on state machine's builder validation`() {
        sessionStateTransitionsSupplier = mock {
            on { getStateTransitions(any()) } doReturn emptyList() // incomplete config
        }

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        verifyInitialization(sessionTracker, emptyList(), logger, storage, listener, mode)

        assertThrows(RuntimeException::class.java, "SessionTracker failed to track session: error creating StateMachine") {
            sessionTracker.trackSession("session_id", State.ACTIVE)
        }

        verifyZeroInteractions(storage, listener)
        assertTrue(sessionTracker.getSessionRecords().isEmpty())
    }

    @Test
    fun untrackSession() {
        val sessionRecord = SessionRecord("session_id", State.ACTIVE)

        storage = createStorageMock(listOf(sessionRecord))

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        verifyInitialization(sessionTracker, listOf(sessionRecord), logger, storage, listener, mode)

        sessionTracker.untrackSession(sessionRecord.sessionId)

        with(inOrder(storage, listener)) {
            verify(storage).deleteSessionRecord(sessionRecord.sessionId)
            verify(listener).onSessionTrackingStopped(sessionTracker, sessionRecord)
        }

        verifyNoMoreInteractions(storage, listener)

        assertTrue(sessionTracker.getSessionRecords().isEmpty())
    }

    @Test
    fun `untrackSession() in verbose mode`() {
        val sessionRecord = SessionRecord("session_id", State.ACTIVE)

        storage = createStorageMock(listOf(sessionRecord))

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = modeVerbose,
            logger = logger
        )

        verifyInitialization(sessionTracker, listOf(sessionRecord), logger, storage, listener, modeVerbose)

        sessionTracker.untrackSession(sessionRecord.sessionId)

        with(inOrder(storage, listener, logger)) {
            verify(logger).d(SessionTracker.TAG, "untrackSession: sessionId = '${sessionRecord.sessionId}'")
            verify(storage).deleteSessionRecord(sessionRecord.sessionId)
            verify(listener).onSessionTrackingStopped(sessionTracker, sessionRecord)
        }

        verifyNoMoreInteractions(storage, listener, logger)

        assertTrue(sessionTracker.getSessionRecords().isEmpty())
    }

    @Test
    fun `untrackSession() for an unknown session should be ignored`() {
        val sessionRecord = SessionRecord("session_id", State.ACTIVE)

        storage = createStorageMock(listOf(sessionRecord))

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        verifyInitialization(sessionTracker, listOf(sessionRecord), logger, storage, listener, mode)

        val unknownSessionId = "unknown_session_id"

        sessionTracker.untrackSession(unknownSessionId)

        verifyZeroInteractions(storage, listener)
        verify(logger).d(SessionTracker.TAG, "untrackSession: no session with ID '$unknownSessionId' found")
        verifyNoMoreInteractions(logger)

        assertEquals(listOf(sessionRecord), sessionTracker.getSessionRecords())
    }

    @Test
    fun untrackAllSessions() {
        val sessionRecord1 = SessionRecord("session_id_1", State.ACTIVE)
        val sessionRecord2 = SessionRecord("session_id_2", State.INACTIVE)
        val sessionRecords = listOf(sessionRecord1, sessionRecord2)

        storage = createStorageMock(sessionRecords)

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        verifyInitialization(sessionTracker, sessionRecords, logger, storage, listener, mode)

        sessionTracker.untrackAllSessions()

        with(inOrder(storage, listener)) {
            verify(storage).deleteAllSessionRecords()
            verify(listener).onAllSessionsTrackingStopped(sessionTracker, sessionRecords)
        }

        verifyNoMoreInteractions(storage, listener)

        assertTrue(sessionTracker.getSessionRecords().isEmpty())
    }

    @Test
    fun `untrackAllSessions() in verbose mode`() {
        val sessionRecords = listOf(
            SessionRecord("session_id_1", State.ACTIVE),
            SessionRecord("session_id_2", State.INACTIVE)
        )

        storage = createStorageMock(sessionRecords)

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = modeVerbose,
            logger = logger
        )

        verifyInitialization(sessionTracker, sessionRecords, logger, storage, listener, modeVerbose)

        sessionTracker.untrackAllSessions()

        with(inOrder(storage, listener, logger)) {
            verify(logger).d(SessionTracker.TAG, "untrackAllSessions")
            verify(storage).deleteAllSessionRecords()
            verify(listener).onAllSessionsTrackingStopped(sessionTracker, sessionRecords)
        }

        verifyNoMoreInteractions(storage, listener, logger)

        assertTrue(sessionTracker.getSessionRecords().isEmpty())
    }

    @Test
    fun `untrackAllSessions() if there are no sessions at all`() {
        storage = createStorageMock(emptyList())

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        verifyInitialization(sessionTracker, emptyList(), logger, storage, listener, mode)

        sessionTracker.untrackAllSessions()

        verifyZeroInteractions(storage, listener, logger)

        assertTrue(sessionTracker.getSessionRecords().isEmpty())
    }

    @Test
    fun `untrackAllSessions() if there are no sessions at all, in verbose mode`() {
        storage = createStorageMock(emptyList())

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = modeVerbose,
            logger = logger
        )

        verifyInitialization(sessionTracker, emptyList(), logger, storage, listener, modeVerbose)

        sessionTracker.untrackAllSessions()

        verify(logger).d(SessionTracker.TAG, "untrackAllSessions: no sessions found")

        verifyZeroInteractions(storage, listener)
        verifyNoMoreInteractions(logger)

        assertTrue(sessionTracker.getSessionRecords().isEmpty())
    }

    @Test
    fun consumeEvent() {
        val sessionRecord1 = SessionRecord("session_id_1", State.ACTIVE)
        val sessionRecord2 = SessionRecord("session_id_2", State.INACTIVE)
        val sessionRecords = listOf(sessionRecord1, sessionRecord2)

        storage = createStorageMock(sessionRecords)

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        verifyInitialization(sessionTracker, sessionRecords, logger, storage, listener, mode)

        assertTrue(sessionTracker.consumeEvent(sessionRecord1.sessionId, Event.LOGOUT))

        val updatedSessionRecord1 = sessionRecord1.copy(state = State.INACTIVE)

        with(inOrder(storage, listener)) {
            verify(storage).updateSessionRecord(updatedSessionRecord1)
            verify(listener).onSessionStateChanged(sessionTracker, updatedSessionRecord1, sessionRecord1.state)
        }

        verifyNoMoreInteractions(storage, listener)

        assertEquals(listOf(updatedSessionRecord1, sessionRecord2), sessionTracker.getSessionRecords())
    }

    @Test
    fun `consumeEvent() in verbose mode`() {
        val sessionRecord1 = SessionRecord("session_id_1", State.ACTIVE)
        val sessionRecord2 = SessionRecord("session_id_2", State.INACTIVE)
        val sessionRecords = listOf(sessionRecord1, sessionRecord2)

        storage = createStorageMock(sessionRecords)

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = modeVerbose,
            logger = logger
        )

        verifyInitialization(sessionTracker, sessionRecords, logger, storage, listener, modeVerbose)

        assertTrue(sessionTracker.consumeEvent(sessionRecord1.sessionId, Event.LOGOUT))

        val updatedSessionRecord1 = sessionRecord1.copy(state = State.INACTIVE)

        with(inOrder(storage, listener, logger)) {
            verify(logger).d(
                SessionTracker.TAG,
                "consumeEvent: sessionId = '${sessionRecord1.sessionId}', event = '${Event.LOGOUT}'"
            )
            verify(logger).d(
                SessionTracker.TAG,
                "onStateChanged: '${sessionRecord1.state}' -> '${updatedSessionRecord1.state}', sessionId = '${sessionRecord1.sessionId}'"
            )
            verify(storage).updateSessionRecord(updatedSessionRecord1)
            verify(listener).onSessionStateChanged(sessionTracker, updatedSessionRecord1, sessionRecord1.state)
        }

        verifyNoMoreInteractions(storage, listener, logger)

        assertEquals(listOf(updatedSessionRecord1, sessionRecord2), sessionTracker.getSessionRecords())
    }

    @Test
    fun `if event is ignored, then listeners should not be notified and sessions state should not be persisted`() {
        val sessionRecord = SessionRecord("session_id", State.ACTIVE)

        storage = createStorageMock(listOf(sessionRecord))

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        verifyInitialization(sessionTracker, listOf(sessionRecord), logger, storage, listener, mode)

        // LOGIN event will be ignored, since current state is ACTIVE
        assertFalse(sessionTracker.consumeEvent(sessionRecord.sessionId, Event.LOGIN))

        verifyZeroInteractions(storage, listener)

        assertEquals(listOf(sessionRecord), sessionTracker.getSessionRecords())
    }

    @Test
    fun `if event is ignored, then listeners should not be notified and sessions state should not be persisted, in verbose mode`() {
        val sessionRecord = SessionRecord("session_id", State.ACTIVE)

        storage = createStorageMock(listOf(sessionRecord))

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = modeVerbose,
            logger = logger
        )

        verifyInitialization(sessionTracker, listOf(sessionRecord), logger, storage, listener, modeVerbose)

        // LOGIN event will be ignored, since current state is ACTIVE
        assertFalse(sessionTracker.consumeEvent(sessionRecord.sessionId, Event.LOGIN))

        with(inOrder(logger)) {
            verify(logger).d(
                SessionTracker.TAG,
                "consumeEvent: sessionId = '${sessionRecord.sessionId}', event = '${Event.LOGIN}'"
            )
            verify(logger).d(
                SessionTracker.TAG,
                "consumeEvent: event '${Event.LOGIN}' was ignored for session with ID '${sessionRecord.sessionId}' " +
                        "in state ${sessionRecord.state}, isUntracking = false"
            )
        }

        verifyZeroInteractions(storage, listener)
        verifyNoMoreInteractions(logger)

        assertEquals(listOf(sessionRecord), sessionTracker.getSessionRecords())
    }

    @Test
    fun `consumeEvent() called and session appears in a auto-untrack state`() {
        val sessionRecord1 = SessionRecord("session_id_1", State.ACTIVE)
        val sessionRecord2 = SessionRecord("session_id_2", State.INACTIVE)
        val sessionRecords = listOf(sessionRecord1, sessionRecord2)

        storage = createStorageMock(sessionRecords)

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = setOf(State.FORGOTTEN),
            mode = mode,
            logger = logger
        )

        verifyInitialization(sessionTracker, sessionRecords, logger, storage, listener, mode)

        assertTrue(sessionTracker.consumeEvent(sessionRecord1.sessionId, Event.LOGOUT_AND_FORGET))

        val updatedSessionRecord1 = sessionRecord1.copy(state = State.FORGOTTEN)

        with(inOrder(listener, storage)) {
            verify(listener).onSessionStateChanged(sessionTracker, updatedSessionRecord1, sessionRecord1.state)
            verify(storage).deleteSessionRecord(sessionRecord1.sessionId)
            verify(listener).onSessionTrackingStopped(sessionTracker, updatedSessionRecord1)
        }

        verifyNoMoreInteractions(storage, listener)

        assertEquals(listOf(sessionRecord2), sessionTracker.getSessionRecords())
    }

    @Test
    fun `consumeEvent() called and session appears in a auto-untrack state being an intermediate state in transition`() {
        val sessionRecord1 = SessionRecord("session_id_1", State.ACTIVE)
        val sessionRecord2 = SessionRecord("session_id_2", State.INACTIVE)
        val sessionRecords = listOf(sessionRecord1, sessionRecord2)

        storage = createStorageMock(sessionRecords)
        sessionStateTransitionsSupplier = mock {
            on { getStateTransitions(any()) } doReturn listOf(
                Transition(Event.LOGOUT, listOf(State.ACTIVE, State.FORGOTTEN, State.INACTIVE)),
                Transition(Event.LOGIN, listOf(State.INACTIVE, State.ACTIVE))
            )
        }
        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = setOf(State.FORGOTTEN),
            mode = mode,
            logger = logger
        )

        verifyInitialization(sessionTracker, sessionRecords, logger, storage, listener, mode)

        assertTrue(sessionTracker.consumeEvent(sessionRecord1.sessionId, Event.LOGOUT))

        val updatedSessionRecord1 = sessionRecord1.copy(state = State.FORGOTTEN)

        with(inOrder(listener, storage)) {
            verify(listener).onSessionStateChanged(sessionTracker, updatedSessionRecord1, sessionRecord1.state)
            verify(storage).deleteSessionRecord(sessionRecord1.sessionId)
            verify(listener).onSessionTrackingStopped(sessionTracker, updatedSessionRecord1)
        }

        verifyNoMoreInteractions(storage, listener)

        assertEquals(listOf(sessionRecord2), sessionTracker.getSessionRecords())
    }

    @Test
    fun `consumeEvent() called and session appears in a auto-untrack state being an intermediate state in transition, and listener calls untrackSession()`() {
        val sessionRecord1 = SessionRecord("session_id_1", State.ACTIVE)
        val sessionRecord2 = SessionRecord("session_id_2", State.INACTIVE)

        val updatedSessionRecord1 = sessionRecord1.copy(state = State.FORGOTTEN)

        storage = createStorageMock(listOf(sessionRecord1, sessionRecord2))
        listener = mock {
            on { onSessionStateChanged(any(), eq(updatedSessionRecord1), eq(sessionRecord1.state)) } doAnswer {
                val sessionTracker = it.getArgument<SessionTracker<Event, State>>(0)
                sessionTracker.untrackSession(sessionRecord1.sessionId)
                Unit
            }
        }
        sessionStateTransitionsSupplier = mock {
            on { getStateTransitions(any()) } doReturn listOf(
                Transition(Event.LOGOUT, listOf(State.ACTIVE, State.FORGOTTEN, State.INACTIVE)),
                Transition(Event.LOGIN, listOf(State.INACTIVE, State.ACTIVE))
            )
        }

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = setOf(State.FORGOTTEN),
            mode = mode,
            logger = logger
        )

        with(sessionTracker) {
            initialize()
            assertTrue(consumeEvent(sessionRecord1.sessionId, Event.LOGOUT))
        }

        with(inOrder(listener, storage)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackerInitialized(sessionTracker, listOf(sessionRecord1, sessionRecord2))
            verify(listener).onSessionStateChanged(sessionTracker, updatedSessionRecord1, sessionRecord1.state)
            verify(storage).deleteSessionRecord(sessionRecord1.sessionId)
            verify(listener).onSessionTrackingStopped(sessionTracker, updatedSessionRecord1)
        }

        verifyNoMoreInteractions(storage, listener)

        verify(logger).w(
            SessionTracker.TAG,
            "untrackSession: session with ID '${sessionRecord1.sessionId}' is already untracking"
        )

        assertEquals(listOf(sessionRecord2), sessionTracker.getSessionRecords())
    }

    @Test
    fun `consumeEvent() called and session appears in a auto-untrack state being an intermediate state in transition, and listener calls untrackAllSessions()()`() {
        val sessionRecord1 = SessionRecord("session_id_1", State.ACTIVE)
        val sessionRecord2 = SessionRecord("session_id_2", State.INACTIVE)

        val updatedSessionRecord1 = sessionRecord1.copy(state = State.FORGOTTEN)

        storage = createStorageMock(listOf(sessionRecord1, sessionRecord2))
        listener = mock {
            on { onSessionStateChanged(any(), eq(updatedSessionRecord1), eq(sessionRecord1.state)) } doAnswer {
                val sessionTracker = it.getArgument<SessionTracker<Event, State>>(0)
                sessionTracker.untrackAllSessions()
                Unit
            }
        }
        sessionStateTransitionsSupplier = mock {
            on { getStateTransitions(any()) } doReturn listOf(
                Transition(Event.LOGOUT, listOf(State.ACTIVE, State.FORGOTTEN, State.INACTIVE)),
                Transition(Event.LOGIN, listOf(State.INACTIVE, State.ACTIVE))
            )
        }

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = setOf(State.FORGOTTEN),
            mode = mode,
            logger = logger
        )

        with(sessionTracker) {
            initialize()
            assertTrue(consumeEvent(sessionRecord1.sessionId, Event.LOGOUT))
        }

        with(inOrder(listener, storage)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackerInitialized(sessionTracker, listOf(sessionRecord1, sessionRecord2))
            verify(listener).onSessionStateChanged(sessionTracker, updatedSessionRecord1, sessionRecord1.state)
            verify(storage).deleteAllSessionRecords()
            verify(listener).onAllSessionsTrackingStopped(
                sessionTracker,
                listOf(
                    updatedSessionRecord1,
                    sessionRecord2
                )
            )
        }

        verifyNoMoreInteractions(storage, listener)

        assertTrue(sessionTracker.getSessionRecords().isEmpty())
    }

    @Test
    fun `consumeEvent() for an unknown session should be ignored`() {
        val sessionRecord = SessionRecord("session_id", State.ACTIVE)
        val sessionRecords = listOf(sessionRecord)

        storage = createStorageMock(sessionRecords)

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        verifyInitialization(sessionTracker, sessionRecords, logger, storage, listener, mode)

        val unknownSessionId = "unknown_session_id"

        assertFalse(sessionTracker.consumeEvent(unknownSessionId, Event.LOGIN))

        verifyZeroInteractions(storage, listener)
        verify(logger).w(SessionTracker.TAG, "consumeEvent: no session with ID '$unknownSessionId' found")
        verifyNoMoreInteractions(logger)

        assertEquals(sessionRecords, sessionTracker.getSessionRecords())
    }

    @Test
    fun `consumeEvent() for the session being auto-untracked should be ignored`() {
        val sessionRecord = SessionRecord("sessionId", State.ACTIVE)

        val updatedSessionRecord = sessionRecord.copy(state = State.INACTIVE)

        storage = createStorageMock(listOf(sessionRecord))
        listener = mock {
            on { onSessionStateChanged(any(), eq(updatedSessionRecord), eq(sessionRecord.state)) } doAnswer {
                val sessionTracker = it.getArgument<SessionTracker<Event, State>>(0)
                assertFalse(sessionTracker.consumeEvent(sessionRecord.sessionId, Event.LOGIN))
                Unit
            }
        }
        sessionStateTransitionsSupplier = mock {
            on { getStateTransitions(any()) } doReturn listOf(
                Transition(
                    Event.LOGOUT,
                    listOf(State.ACTIVE, State.INACTIVE, State.FORGOTTEN)
                ),
                Transition(
                    Event.LOGIN,
                    listOf(State.ACTIVE, State.FORGOTTEN)
                )
            )
        }

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = setOf(State.INACTIVE),
            mode = mode,
            logger = logger
        )

        sessionTracker.initialize()
        assertEquals(listOf(sessionRecord), sessionTracker.getSessionRecords())

        reset(storage, logger)

        assertTrue(sessionTracker.consumeEvent(sessionRecord.sessionId, Event.LOGOUT))

        with(inOrder(listener, storage, logger)) {
            verify(listener).onSessionTrackerInitialized(sessionTracker, listOf(sessionRecord))
            verify(logger).d(
                SessionTracker.TAG,
                "onStateChanged: '${sessionRecord.state}' -> '${State.INACTIVE}', sessionId = '${sessionRecord.sessionId}', going to auto-untrack session.."
            )
            verify(listener).onSessionStateChanged(sessionTracker, updatedSessionRecord, sessionRecord.state)
            verify(logger).w(
                SessionTracker.TAG,
                "consumeEvent: event = '${Event.LOGIN}', session with ID '${sessionRecord.sessionId}' is already untracking"
            )
            verify(storage).deleteSessionRecord(sessionRecord.sessionId)
            verify(listener).onSessionTrackingStopped(sessionTracker, updatedSessionRecord)
        }

        verifyNoMoreInteractions(storage, listener, logger)

        assertTrue(sessionTracker.getSessionRecords().isEmpty())
    }

}