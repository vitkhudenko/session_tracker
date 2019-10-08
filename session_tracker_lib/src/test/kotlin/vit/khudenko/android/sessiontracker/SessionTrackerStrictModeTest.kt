package vit.khudenko.android.sessiontracker

import com.nhaarman.mockitokotlin2.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import vit.khudenko.android.sessiontracker.test_util.*

class SessionTrackerStrictModeTest {

    @get:Rule
    val expectedExceptionRule: ExpectedException = ExpectedException.none()

    private val mode = SessionTracker.Mode.STRICT
    private val modeVerbose = SessionTracker.Mode.STRICT_VERBOSE

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

        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage(
            "Unable to initialize SessionTracker: session with ID '${sessionId}' is in auto-untrack state (${State.FORGOTTEN})"
        )

        storage = createStorageMock(listOf(SessionRecord(sessionId, State.FORGOTTEN)))

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = setOf(State.FORGOTTEN),
            mode = mode,
            logger = logger
        )

        try {
            sessionTracker.initialize()
        } catch (e: Exception) {
            verify(storage).readAllSessionRecords()
            verifyNoMoreInteractions(storage)
            verifyZeroInteractions(listener)
            throw e
        }
    }

    @Test
    fun `initialization without state transitions fails on state machine's builder validation`() {
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage(
            "Unable to initialize SessionTracker: error creating StateMachine"
        )

        storage = createStorageMock(listOf(SessionRecord("session_id", State.ACTIVE)))
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

        try {
            sessionTracker.initialize()
        } catch (e: Exception) {
            verify(storage).readAllSessionRecords()
            verifyNoMoreInteractions(storage)
            verifyZeroInteractions(listener)
            throw e
        }
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
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage(
            "Unable to track session: session with ID 'session_id' is in auto-untrack state (FORGOTTEN)"
        )

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

        sessionTracker.initialize()

        try {
            sessionTracker.trackSession(sessionId, state)
        } catch (e: Exception) {
            verify(storage).readAllSessionRecords()
            verifyNoMoreInteractions(storage)
            verifyZeroInteractions(listener)

            assertTrue(sessionTracker.getSessionRecords().isEmpty())

            throw e
        }
    }

    @Test
    fun `trackSession() with already tracked session`() {
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

        sessionTracker.trackSession(sessionRecord.sessionId, State.INACTIVE)

        verify(logger).w(
            SessionTracker.TAG,
            "trackSession: session with ID '${sessionRecord.sessionId}' already exists"
        )

        verifyZeroInteractions(storage, listener)

        assertEquals(sessionRecords, sessionTracker.getSessionRecords())
    }

    @Test
    fun `trackSession() without state transitions fails on state machine's builder validation`() {
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage("SessionTracker failed to track session: error creating StateMachine")

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

        try {
            sessionTracker.trackSession("session_id", State.ACTIVE)
        } catch (e: Exception) {
            verifyZeroInteractions(storage, listener)
            assertTrue(sessionTracker.getSessionRecords().isEmpty())

            throw e
        }
    }

    @Test
    fun untrackSession() {
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
        val sessionRecords = listOf(sessionRecord)

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

        sessionTracker.untrackSession(unknownSessionId)

        verify(logger).d(SessionTracker.TAG, "untrackSession: no session with ID '$unknownSessionId' found")
        verifyNoMoreInteractions(logger)
        verifyZeroInteractions(storage, listener)

        assertEquals(sessionRecords, sessionTracker.getSessionRecords())
    }

    @Test
    fun untrackAllSessions() {
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
    fun `if event is ignored then listeners should not be notified and sessions state should not be persisted`() {
        val sessionRecord = SessionRecord("session_id_1", State.ACTIVE)
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

        // LOGIN event will be ignored, since current state is ACTIVE
        assertFalse(sessionTracker.consumeEvent(sessionRecord.sessionId, Event.LOGIN))

        verifyZeroInteractions(storage, listener)

        assertEquals(sessionRecords, sessionTracker.getSessionRecords())
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

        assertEquals(sessionRecords - sessionRecord1, sessionTracker.getSessionRecords())
    }

    @Test
    fun `consumeEvent() called and session appears in a auto-untrack state being an intermediate state in transition`() {
        val sessionRecord1 = SessionRecord("session_id_1", State.ACTIVE)
        val sessionRecord2 = SessionRecord("session_id_2", State.INACTIVE)
        val sessionRecords = listOf(sessionRecord1, sessionRecord2)

        storage = createStorageMock(sessionRecords)
        sessionStateTransitionsSupplier = mock {
            on { getStateTransitions(any()) } doReturn listOf(
                Transition(Event.LOGOUT, listOf(State.ACTIVE, State.FORGOTTEN, State.INACTIVE))
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

        assertEquals(sessionRecords - sessionRecord1, sessionTracker.getSessionRecords())
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
                Transition(Event.LOGOUT, listOf(State.ACTIVE, State.FORGOTTEN, State.INACTIVE))
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
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionRecord1)
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionRecord2)
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
                Transition(Event.LOGOUT, listOf(State.ACTIVE, State.FORGOTTEN, State.INACTIVE))
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
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionRecord1)
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionRecord2)
            verify(listener).onSessionStateChanged(sessionTracker, updatedSessionRecord1, sessionRecord1.state)
            verify(storage).deleteAllSessionRecords()
            verify(listener).onAllSessionsTrackingStopped(
                sessionTracker,
                listOf(updatedSessionRecord1, sessionRecord2)
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
        val sessionRecord = SessionRecord("session_id", State.ACTIVE)
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
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionRecord)
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