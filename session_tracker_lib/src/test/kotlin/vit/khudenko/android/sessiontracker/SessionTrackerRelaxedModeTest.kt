package vit.khudenko.android.sessiontracker

import com.nhaarman.mockitokotlin2.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import vit.khudenko.android.sessiontracker.test_util.Event
import vit.khudenko.android.sessiontracker.test_util.State
import java.util.*
import java.util.concurrent.atomic.AtomicReference

class SessionTrackerRelaxedModeTest {

    @get:Rule
    val rule: MockitoRule = MockitoJUnit.rule()

    @get:Rule
    val expectedExceptionRule: ExpectedException = ExpectedException.none()

    private val mode = SessionTracker.Mode.RELAXED
    private lateinit var logger: SessionTracker.Logger

    @Before
    fun setUp() {
        logger = mock()
    }

    @Test
    fun `consumeEvent() called with uninitialized sessionTracker`() {
        val storage = mock<ISessionTrackerStorage<State>>()
        val listener = mock<SessionTracker.Listener<Event, State>>()
        val sessionStateTransitionsSupplier = mock<ISessionStateTransitionsSupplier<Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        assertFalse(sessionTracker.consumeEvent("session_id", Event.LOGIN))

        verify(logger).e(
            SessionTracker.TAG,
            "SessionTracker must be initialized before calling its #consumeEvent method"
        )
        verifyZeroInteractions(storage, listener, sessionStateTransitionsSupplier)
    }

    @Test
    fun `trackSession() called with uninitialized sessionTracker`() {
        val storage = mock<ISessionTrackerStorage<State>>()
        val listener = mock<SessionTracker.Listener<Event, State>>()
        val sessionStateTransitionsSupplier = mock<ISessionStateTransitionsSupplier<Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        sessionTracker.trackSession("session_id", State.ACTIVE)

        verify(logger).e(
            SessionTracker.TAG,
            "SessionTracker must be initialized before calling its #trackSession method"
        )
        verifyZeroInteractions(storage, listener, sessionStateTransitionsSupplier)
    }

    @Test
    fun `untrackSession() called with uninitialized sessionTracker`() {
        val storage = mock<ISessionTrackerStorage<State>>()
        val listener = mock<SessionTracker.Listener<Event, State>>()
        val sessionStateTransitionsSupplier = mock<ISessionStateTransitionsSupplier<Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        sessionTracker.untrackSession("session_id")

        verify(logger).e(
            SessionTracker.TAG,
            "SessionTracker must be initialized before calling its #untrackSession method"
        )
        verifyZeroInteractions(storage, listener, sessionStateTransitionsSupplier)
    }

    @Test
    fun `untrackAllSessions() called with uninitialized sessionTracker`() {
        val storage = mock<ISessionTrackerStorage<State>>()
        val listener = mock<SessionTracker.Listener<Event, State>>()
        val sessionStateTransitionsSupplier = mock<ISessionStateTransitionsSupplier<Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )
        sessionTracker.untrackAllSessions()

        verify(logger).e(
            SessionTracker.TAG,
            "SessionTracker must be initialized before calling its #untrackAllSessions method"
        )
        verifyZeroInteractions(storage, listener, sessionStateTransitionsSupplier)
    }

    @Test
    fun `getSessionRecords() called with uninitialized sessionTracker`() {
        val storage = mock<ISessionTrackerStorage<State>>()
        val listener = mock<SessionTracker.Listener<Event, State>>()
        val sessionStateTransitionsSupplier = mock<ISessionStateTransitionsSupplier<Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        val sessions = sessionTracker.getSessionRecords()
        assertTrue(sessions.isEmpty())

        verify(logger).e(
            SessionTracker.TAG,
            "SessionTracker must be initialized before calling its #getSessionRecords method"
        )
        verifyZeroInteractions(storage, listener, sessionStateTransitionsSupplier)
    }

    @Test
    fun initialization() {
        val sessionId = "session_id"
        val state = State.ACTIVE

        val storage = createStorageMock(listOf(SessionRecord(sessionId, state)))
        val listener = mock<SessionTracker.Listener<Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        sessionTracker.initialize()

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId, state)
        }

        verifyNoMoreInteractions(listener, storage)

        assertEquals(listOf(SessionRecord(sessionId, state)), sessionTracker.getSessionRecords())
    }

    @Test
    fun `initialization with session in a auto-untrack state`() {
        val sessionId = "session_id"

        val storage = createStorageMock(listOf(SessionRecord(sessionId, State.FORGOTTEN)))
        val listener = mock<SessionTracker.Listener<Event, State>>()
        val sessionStateTransitionsSupplier = mock<ISessionStateTransitionsSupplier<Event, State>>()

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
        verifyNoMoreInteractions(storage)
        verifyZeroInteractions(sessionStateTransitionsSupplier, listener)
    }

    @Test
    fun `initialization without state transitions fails on state machine's builder validation`() {
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage("Unable to initialize SessionTracker: error creating StateMachine")

        val storage = createStorageMock(listOf(SessionRecord("session_id", State.ACTIVE)))

        val listener = mock<SessionTracker.Listener<Event, State>>()
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
        val sessionId = "session_id"
        val state = State.ACTIVE

        val storage = createStorageMock(listOf(SessionRecord(sessionId, state)))
        val listener = mock<SessionTracker.Listener<Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        sessionTracker.initialize()

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId, state)
        }
        verifyNoMoreInteractions(listener, storage)
        verifyZeroInteractions(logger)

        assertEquals(listOf(SessionRecord(sessionId, state)), sessionTracker.getSessionRecords())

        reset(storage, listener, logger)

        sessionTracker.initialize()

        verify(logger).w(SessionTracker.TAG, "initialize: already initialized, skipping..")
        verifyZeroInteractions(listener, storage)

        assertEquals(listOf(SessionRecord(sessionId, state)), sessionTracker.getSessionRecords())
    }

    @Test
    fun trackSession() {
        val storage = createStorageMock(emptyList())
        val listener = mock<SessionTracker.Listener<Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        sessionTracker.initialize()
        assertTrue(sessionTracker.getSessionRecords().isEmpty())

        val sessionId = "session_id"
        val state = State.ACTIVE

        sessionTracker.trackSession(sessionId, state)

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(storage).createSessionRecord(SessionRecord(sessionId, state))
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId, state)
        }

        verifyNoMoreInteractions(storage, listener)

        assertEquals(listOf(SessionRecord(sessionId, state)), sessionTracker.getSessionRecords())
    }

    @Test
    fun `trackSession() with session in an auto-untrack state`() {
        val sessionId = "session_id"
        val state = State.FORGOTTEN

        val storage = createStorageMock(emptyList())
        val listener = mock<SessionTracker.Listener<Event, State>>()
        val sessionStateTransitionsSupplier = mock<ISessionStateTransitionsSupplier<Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = setOf(state),
            mode = mode,
            logger = logger
        )

        with(sessionTracker) {
            initialize()
            trackSession(sessionId, state)
        }

        verify(storage).readAllSessionRecords()
        verify(logger).e(
            SessionTracker.TAG,
            "trackSession: session with ID '${sessionId}' is in auto-untrack state (${State.FORGOTTEN}), " +
                    "rejecting this session"
        )
        verifyNoMoreInteractions(storage)
        verifyZeroInteractions(sessionStateTransitionsSupplier, listener)

        assertTrue(sessionTracker.getSessionRecords().isEmpty())
    }

    @Test
    fun `trackSession() with already tracked session`() {
        val sessionId = "session_id"
        val state = State.ACTIVE

        val storage = createStorageMock(listOf(SessionRecord(sessionId, state)))
        val logger = mock<SessionTracker.Logger>()
        val listener = mock<SessionTracker.Listener<Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        with(sessionTracker) {
            initialize()
            trackSession(sessionId, State.INACTIVE)
        }

        with(inOrder(storage, listener, logger)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId, state)
            verify(logger).w(SessionTracker.TAG, "trackSession: session with ID '${sessionId}' already exists")
        }

        verifyNoMoreInteractions(storage, listener)

        assertEquals(listOf(SessionRecord(sessionId, state)), sessionTracker.getSessionRecords())
    }

    @Test
    fun `trackSession() without state transitions fails on state machine's builder validation`() {
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage("SessionTracker failed to track session: error creating StateMachine")

        val storage = createStorageMock(emptyList())

        val listener = mock<SessionTracker.Listener<Event, State>>()
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

        sessionTracker.initialize()
        assertTrue(sessionTracker.getSessionRecords().isEmpty())

        val sessionId = "session_id"
        val state = State.ACTIVE

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
    fun untrackSession() {
        val sessionId = "session_id"
        val state = State.ACTIVE

        val storage = createStorageMock(listOf(SessionRecord(sessionId, state)))
        val listener = mock<SessionTracker.Listener<Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        with(sessionTracker) {
            initialize()
            untrackSession(sessionId)
        }

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId, state)
            verify(storage).deleteSessionRecord(sessionId)
            verify(listener).onSessionTrackingStopped(sessionTracker, sessionId, state)
        }

        verifyNoMoreInteractions(storage, listener)

        assertTrue(sessionTracker.getSessionRecords().isEmpty())
    }

    @Test
    fun `untrackSession() for an unknown session should be ignored`() {
        val sessionId = "session_id"
        val state = State.ACTIVE

        val storage = createStorageMock(listOf(SessionRecord(sessionId, state)))
        val listener = mock<SessionTracker.Listener<Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        val unknownSessionId = "unknown_session_id"

        sessionTracker.initialize()

        reset(storage, listener, logger)

        sessionTracker.untrackSession(unknownSessionId)

        verifyZeroInteractions(storage, listener)
        verify(logger).d(SessionTracker.TAG, "untrackSession: no session with ID '$unknownSessionId' found")
        verifyNoMoreInteractions(logger)

        assertEquals(listOf(SessionRecord(sessionId, state)), sessionTracker.getSessionRecords())
    }

    @Test
    fun untrackAllSessions() {
        val sessionId1 = "session_id_1"
        val state1 = State.ACTIVE

        val sessionId2 = "session_id_2"
        val state2 = State.INACTIVE

        val storage = createStorageMock(listOf(SessionRecord(sessionId1, state1), SessionRecord(sessionId2, state2)))
        val listener = mock<SessionTracker.Listener<Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        with(sessionTracker) {
            initialize()
            untrackAllSessions()
        }

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId1, state1)
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId2, state2)
            verify(storage).deleteAllSessionRecords()
            verify(listener).onAllSessionsTrackingStopped(
                sessionTracker,
                listOf(sessionId1 to state1, sessionId2 to state2)
            )
        }

        verifyNoMoreInteractions(storage, listener)

        assertTrue(sessionTracker.getSessionRecords().isEmpty())
    }

    @Test
    fun consumeEvent() {
        val sessionId1 = "session_id_1"
        val state1 = State.ACTIVE

        val sessionId2 = "session_id_2"
        val state2 = State.INACTIVE

        val storage = createStorageMock(listOf(SessionRecord(sessionId1, state1), SessionRecord(sessionId2, state2)))
        val listener = mock<SessionTracker.Listener<Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        with(sessionTracker) {
            initialize()
            assertTrue(consumeEvent(sessionId1, Event.LOGOUT))
        }

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId1, state1)
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId2, state2)
            verify(storage).updateSessionRecord(SessionRecord(sessionId1, State.INACTIVE))
            verify(listener).onSessionStateChanged(sessionTracker, sessionId1, state1, State.INACTIVE)
        }

        verifyNoMoreInteractions(storage, listener)

        assertEquals(
            listOf(
                SessionRecord(sessionId1, State.INACTIVE),
                SessionRecord(sessionId2, state2)
            ),
            sessionTracker.getSessionRecords()
        )
    }

    @Test
    fun `if event is ignored then listeners should not be notified and sessions state should not be persisted`() {
        val sessionId = "session_id"
        val state = State.ACTIVE

        val storage = createStorageMock(listOf(SessionRecord(sessionId, state)))
        val listener = mock<SessionTracker.Listener<Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        with(sessionTracker) {
            initialize()
            // LOGIN event will be ignored, since current state is ACTIVE
            assertFalse(consumeEvent(sessionId, Event.LOGIN))
        }

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId, state)
        }

        verifyNoMoreInteractions(storage, listener)

        assertEquals(listOf(SessionRecord(sessionId, state)), sessionTracker.getSessionRecords())
    }

    @Test
    fun `consumeEvent() called and session appears in a auto-untrack state`() {
        val sessionId1 = "session_id_1"
        val state1 = State.ACTIVE

        val sessionId2 = "session_id_2"
        val state2 = State.INACTIVE

        val storage = createStorageMock(listOf(SessionRecord(sessionId1, state1), SessionRecord(sessionId2, state2)))
        val listener = mock<SessionTracker.Listener<Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = setOf(State.FORGOTTEN),
            mode = mode,
            logger = logger
        )

        with(sessionTracker) {
            initialize()
            assertTrue(consumeEvent(sessionId1, Event.LOGOUT_AND_FORGET))
        }

        with(inOrder(listener, storage)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId1, state1)
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId2, state2)
            verify(listener).onSessionStateChanged(sessionTracker, sessionId1, state1, State.FORGOTTEN)
            verify(storage).deleteSessionRecord(sessionId1)
            verify(listener).onSessionTrackingStopped(sessionTracker, sessionId1, State.FORGOTTEN)
        }

        verifyNoMoreInteractions(storage, listener)

        assertEquals(listOf(SessionRecord(sessionId2, state2)), sessionTracker.getSessionRecords())
    }

    @Test
    fun `consumeEvent() called and session appears in a auto-untrack state being an intermediate state in transition`() {
        val sessionId1 = "session_id_1"
        val state1 = State.ACTIVE

        val sessionId2 = "session_id_2"
        val state2 = State.INACTIVE

        val storage = createStorageMock(listOf(SessionRecord(sessionId1, state1), SessionRecord(sessionId2, state2)))
        val listener = mock<SessionTracker.Listener<Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = mock {
                on { getStateTransitions(any()) } doReturn listOf(
                    Transition(Event.LOGOUT, listOf(State.ACTIVE, State.FORGOTTEN, State.INACTIVE))
                )
            },
            autoUntrackStates = setOf(State.FORGOTTEN),
            mode = mode,
            logger = logger
        )

        with(sessionTracker) {
            initialize()
            assertTrue(consumeEvent(sessionId1, Event.LOGOUT))
        }

        with(inOrder(listener, storage)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId1, state1)
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId2, state2)
            verify(listener).onSessionStateChanged(sessionTracker, sessionId1, state1, State.FORGOTTEN)
            verify(storage).deleteSessionRecord(sessionId1)
            verify(listener).onSessionTrackingStopped(sessionTracker, sessionId1, State.FORGOTTEN)
        }

        verifyNoMoreInteractions(storage, listener)

        assertEquals(listOf(SessionRecord(sessionId2, state2)), sessionTracker.getSessionRecords())
    }

    @Test
    fun `consumeEvent() called and session appears in a auto-untrack state being an intermediate state in transition, and listener calls untrackSession()`() {
        val sessionId1 = "session_id_1"
        val state1 = State.ACTIVE

        val sessionId2 = "session_id_2"
        val state2 = State.INACTIVE

        val storage = createStorageMock(listOf(SessionRecord(sessionId1, state1), SessionRecord(sessionId2, state2)))
        val listener = mock<SessionTracker.Listener<Event, State>> {
            on { onSessionStateChanged(any(), eq(sessionId1), eq(state1), eq(State.FORGOTTEN)) } doAnswer {
                val sessionTracker = it.getArgument<SessionTracker<Event, State>>(0)
                sessionTracker.untrackSession(sessionId1)
                Unit
            }
        }

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = mock {
                on { getStateTransitions(any()) } doReturn listOf(
                    Transition(Event.LOGOUT, listOf(State.ACTIVE, State.FORGOTTEN, State.INACTIVE))
                )
            },
            autoUntrackStates = setOf(State.FORGOTTEN),
            mode = mode,
            logger = logger
        )

        with(sessionTracker) {
            initialize()
            assertTrue(consumeEvent(sessionId1, Event.LOGOUT))
        }

        with(inOrder(listener, storage)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId1, state1)
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId2, state2)
            verify(listener).onSessionStateChanged(sessionTracker, sessionId1, state1, State.FORGOTTEN)
            verify(storage).deleteSessionRecord(sessionId1)
            verify(listener).onSessionTrackingStopped(sessionTracker, sessionId1, State.FORGOTTEN)
        }

        verifyNoMoreInteractions(storage, listener)

        verify(logger).w(
            SessionTracker.TAG,
            "untrackSession: session with ID '${sessionId1}' is already untracking"
        )

        assertEquals(listOf(SessionRecord(sessionId2, state2)), sessionTracker.getSessionRecords())
    }

    @Test
    fun `consumeEvent() called and session appears in a auto-untrack state being an intermediate state in transition, and listener calls untrackAllSessions()()`() {
        val sessionId1 = "session_id_1"
        val state1 = State.ACTIVE

        val sessionId2 = "session_id_2"
        val state2 = State.INACTIVE

        val storage = createStorageMock(listOf(SessionRecord(sessionId1, state1), SessionRecord(sessionId2, state2)))
        val listener = mock<SessionTracker.Listener<Event, State>> {
            on { onSessionStateChanged(any(), eq(sessionId1), eq(state1), eq(State.FORGOTTEN)) } doAnswer {
                val sessionTracker = it.getArgument<SessionTracker<Event, State>>(0)
                sessionTracker.untrackAllSessions()
                Unit
            }
        }

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = mock {
                on { getStateTransitions(any()) } doReturn listOf(
                    Transition(Event.LOGOUT, listOf(State.ACTIVE, State.FORGOTTEN, State.INACTIVE))
                )
            },
            autoUntrackStates = setOf(State.FORGOTTEN),
            mode = mode,
            logger = logger
        )

        with(sessionTracker) {
            initialize()
            assertTrue(consumeEvent(sessionId1, Event.LOGOUT))
        }

        with(inOrder(listener, storage)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId1, state1)
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId2, state2)
            verify(listener).onSessionStateChanged(sessionTracker, sessionId1, state1, State.FORGOTTEN)
            verify(storage).deleteAllSessionRecords()
            verify(listener).onAllSessionsTrackingStopped(
                sessionTracker,
                listOf(
                    sessionId1 to State.FORGOTTEN,
                    sessionId2 to state2
                )
            )
        }

        verifyNoMoreInteractions(storage, listener)

        assertTrue(sessionTracker.getSessionRecords().isEmpty())
    }

    @Test
    fun `consumeEvent() for an unknown session should be ignored`() {
        val sessionId = "session_id"
        val state = State.ACTIVE

        val storage = createStorageMock(listOf(SessionRecord(sessionId, state)))
        val listener = mock<SessionTracker.Listener<Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        val unknownSessionId = "unknown_session_id"

        sessionTracker.initialize()

        reset(storage, listener, logger)

        assertFalse(sessionTracker.consumeEvent(unknownSessionId, Event.LOGIN))

        verifyZeroInteractions(storage, listener)
        verify(logger).w(SessionTracker.TAG, "consumeEvent: no session with ID '$unknownSessionId' found")
        verifyNoMoreInteractions(logger)

        assertEquals(listOf(SessionRecord(sessionId, state)), sessionTracker.getSessionRecords())
    }

    @Test
    fun `consumeEvent() for the session being auto-untracked should be ignored`() {
        val sessionId = "session_id"
        val state = State.ACTIVE

        val storage = createStorageMock(listOf(SessionRecord(sessionId, state)))
        val listener = mock<SessionTracker.Listener<Event, State>> {
            on { onSessionStateChanged(any(), eq(sessionId), eq(state), eq(State.INACTIVE)) } doAnswer {
                val sessionTracker = it.getArgument<SessionTracker<Event, State>>(0)
                assertFalse(sessionTracker.consumeEvent(sessionId, Event.LOGIN))
                Unit
            }
        }

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
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
            },
            autoUntrackStates = setOf(State.INACTIVE),
            mode = mode,
            logger = logger
        )

        sessionTracker.initialize()
        assertEquals(listOf(SessionRecord(sessionId, state)), sessionTracker.getSessionRecords())

        reset(storage, logger)

        assertTrue(sessionTracker.consumeEvent(sessionId, Event.LOGOUT))

        with(inOrder(listener, storage, logger)) {
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId, state)
            verify(logger).d(
                SessionTracker.TAG,
                "onStateChanged: '$state' -> '${State.INACTIVE}', sessionId = '${sessionId}', going to auto-untrack session.."
            )
            verify(listener).onSessionStateChanged(sessionTracker, sessionId, state, State.INACTIVE)
            verify(logger).w(
                SessionTracker.TAG,
                "consumeEvent: event = '${Event.LOGIN}', session with ID '${sessionId}' is already untracking"
            )
            verify(storage).deleteSessionRecord(sessionId)
            verify(listener).onSessionTrackingStopped(sessionTracker, sessionId, State.INACTIVE)
        }

        verifyNoMoreInteractions(storage, listener, logger)

        assertTrue(sessionTracker.getSessionRecords().isEmpty())
    }

    //////////////////////// ---------- detecting misuse at ISessionTrackerStorage ----------- ////////////////////////

    @Test
    fun `storage implementation attempts to call SessionTracker#consumeEvent() from consumeEvent()`() {
        val sessionId1 = "session_id_1"
        val state1 = State.ACTIVE

        val sessionId2 = "session_id_2"
        val state2 = State.INACTIVE

        val sessionTrackerRef = AtomicReference<SessionTracker<Event, State>>()

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn Collections.unmodifiableList(
                listOf(
                    SessionRecord(sessionId1, state1),
                    SessionRecord(sessionId2, state2)
                )
            )
            on { updateSessionRecord(SessionRecord(sessionId1, State.INACTIVE)) } doAnswer {
                assertFalse(sessionTrackerRef.get().consumeEvent(sessionId2, Event.LOGIN))
                Unit
            }
        }

        val listener = mock<SessionTracker.Listener<Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()
        assertTrue(sessionTracker.consumeEvent(sessionId1, Event.LOGOUT))

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId1, state1)
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId2, state2)
            verify(storage).updateSessionRecord(SessionRecord(sessionId1, State.INACTIVE))
            verify(listener).onSessionStateChanged(sessionTracker, sessionId1, state1, State.INACTIVE)
        }

        verifyNoMoreInteractions(storage, listener)

        verify(logger).e(
            SessionTracker.TAG,
            "consumeEvent: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        assertEquals(
            listOf(
                SessionRecord(sessionId1, State.INACTIVE),
                SessionRecord(sessionId2, state2)
            ),
            sessionTracker.getSessionRecords()
        )
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#trackSession() from consumeEvent()`() {
        val sessionId = "session_id_1"
        val state = State.ACTIVE

        val sessionTrackerRef = AtomicReference<SessionTracker<Event, State>>()

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn Collections.unmodifiableList(
                listOf(SessionRecord(sessionId, state))
            )
            on { updateSessionRecord(SessionRecord(sessionId, State.INACTIVE)) } doAnswer {
                sessionTrackerRef.get().trackSession("session_id_2", State.INACTIVE)
                Unit
            }
        }

        val listener = mock<SessionTracker.Listener<Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()

        assertTrue(sessionTracker.consumeEvent(sessionId, Event.LOGOUT))

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId, state)
            verify(storage).updateSessionRecord(SessionRecord(sessionId, State.INACTIVE))
            verify(listener).onSessionStateChanged(sessionTracker, sessionId, state, State.INACTIVE)
        }

        verifyNoMoreInteractions(storage, listener)

        verify(logger).e(
            SessionTracker.TAG,
            "trackSession: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        assertEquals(listOf(SessionRecord(sessionId, State.INACTIVE)), sessionTracker.getSessionRecords())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackSession() from consumeEvent()`() {
        val sessionId = "session_id"
        val state = State.ACTIVE

        val sessionTrackerRef = AtomicReference<SessionTracker<Event, State>>()

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn Collections.unmodifiableList(
                listOf(SessionRecord(sessionId, state))
            )
            on { updateSessionRecord(SessionRecord(sessionId, State.INACTIVE)) } doAnswer {
                sessionTrackerRef.get().untrackSession(sessionId)
                Unit
            }
        }

        val listener = mock<SessionTracker.Listener<Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()

        assertTrue(sessionTracker.consumeEvent(sessionId, Event.LOGOUT))

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId, state)
            verify(storage).updateSessionRecord(SessionRecord(sessionId, State.INACTIVE))
            verify(listener).onSessionStateChanged(sessionTracker, sessionId, state, State.INACTIVE)
        }

        verifyNoMoreInteractions(storage, listener)

        verify(logger).e(
            SessionTracker.TAG,
            "untrackSession: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        assertEquals(listOf(SessionRecord(sessionId, State.INACTIVE)), sessionTracker.getSessionRecords())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackAllSessions() from consumeEvent()`() {
        val sessionId1 = "session_id_1"
        val state1 = State.ACTIVE

        val sessionId2 = "session_id_2"
        val state2 = State.INACTIVE

        val sessionTrackerRef = AtomicReference<SessionTracker<Event, State>>()

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn Collections.unmodifiableList(
                listOf(
                    SessionRecord(sessionId1, state1),
                    SessionRecord(sessionId2, state2)
                )
            )
            on { updateSessionRecord(SessionRecord(sessionId1, State.INACTIVE)) } doAnswer {
                sessionTrackerRef.get().untrackAllSessions()
                Unit
            }
        }

        val listener = mock<SessionTracker.Listener<Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()

        assertTrue(sessionTracker.consumeEvent(sessionId1, Event.LOGOUT))

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId1, state1)
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId2, state2)
            verify(storage).updateSessionRecord(SessionRecord(sessionId1, State.INACTIVE))
            verify(listener).onSessionStateChanged(sessionTracker, sessionId1, state1, State.INACTIVE)
        }

        verifyNoMoreInteractions(storage, listener)

        verify(logger).e(
            SessionTracker.TAG,
            "untrackAllSessions: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        assertEquals(
            listOf(
                SessionRecord(sessionId1, State.INACTIVE),
                SessionRecord(sessionId2, state2)
            ),
            sessionTracker.getSessionRecords()
        )
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#consumeEvent() from trackSession()`() {
        val sessionId = "session_id"
        val state = State.ACTIVE

        val sessionTrackerRef = AtomicReference<SessionTracker<Event, State>>()

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn emptyList()
            on { createSessionRecord(SessionRecord(sessionId, state)) } doAnswer {
                assertFalse(sessionTrackerRef.get().consumeEvent(sessionId, Event.LOGOUT))
                Unit
            }
        }
        val listener = mock<SessionTracker.Listener<Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()
        assertTrue(sessionTracker.getSessionRecords().isEmpty())

        sessionTracker.trackSession(sessionId, state)

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(storage).createSessionRecord(SessionRecord(sessionId, state))
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId, State.ACTIVE)
        }

        verifyNoMoreInteractions(storage, listener)

        verify(logger).e(
            SessionTracker.TAG,
            "consumeEvent: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        assertEquals(listOf(SessionRecord(sessionId, State.ACTIVE)), sessionTracker.getSessionRecords())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#trackSession() from trackSession()`() {
        val sessionId = "session_id"
        val state = State.ACTIVE

        val sessionTrackerRef = AtomicReference<SessionTracker<Event, State>>()

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn emptyList()
            on { createSessionRecord(SessionRecord(sessionId, state)) } doAnswer {
                sessionTrackerRef.get().trackSession("session_id_2", State.INACTIVE)
                Unit
            }
        }
        val listener = mock<SessionTracker.Listener<Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()
        assertTrue(sessionTracker.getSessionRecords().isEmpty())

        sessionTracker.trackSession(sessionId, state)

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(storage).createSessionRecord(SessionRecord(sessionId, state))
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId, State.ACTIVE)
        }

        verifyNoMoreInteractions(storage, listener)

        verify(logger).e(
            SessionTracker.TAG,
            "trackSession: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        assertEquals(listOf(SessionRecord(sessionId, State.ACTIVE)), sessionTracker.getSessionRecords())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackSession() from trackSession()`() {
        val sessionId = "session_id"
        val state = State.ACTIVE

        val sessionTrackerRef = AtomicReference<SessionTracker<Event, State>>()

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn emptyList()
            on { createSessionRecord(SessionRecord(sessionId, state)) } doAnswer {
                sessionTrackerRef.get().untrackSession(sessionId)
                Unit
            }
        }
        val listener = mock<SessionTracker.Listener<Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()
        assertTrue(sessionTracker.getSessionRecords().isEmpty())

        sessionTracker.trackSession(sessionId, state)

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(storage).createSessionRecord(SessionRecord(sessionId, state))
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId, State.ACTIVE)
        }

        verifyNoMoreInteractions(storage, listener)

        verify(logger).e(
            SessionTracker.TAG,
            "untrackSession: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        assertEquals(listOf(SessionRecord(sessionId, State.ACTIVE)), sessionTracker.getSessionRecords())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackAllSessions() from trackSession()`() {
        val sessionId = "session_id"
        val state = State.ACTIVE

        val sessionTrackerRef = AtomicReference<SessionTracker<Event, State>>()

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn emptyList()
            on { createSessionRecord(SessionRecord(sessionId, state)) } doAnswer {
                sessionTrackerRef.get().untrackAllSessions()
                Unit
            }
        }
        val listener = mock<SessionTracker.Listener<Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()
        assertTrue(sessionTracker.getSessionRecords().isEmpty())

        sessionTracker.trackSession(sessionId, state)

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(storage).createSessionRecord(SessionRecord(sessionId, state))
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId, State.ACTIVE)
        }

        verifyNoMoreInteractions(storage, listener)

        verify(logger).e(
            SessionTracker.TAG,
            "untrackAllSessions: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        assertEquals(listOf(SessionRecord(sessionId, State.ACTIVE)), sessionTracker.getSessionRecords())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#consumeEvent() from untrackSession()`() {
        val sessionId1 = "session_id_1"
        val state1 = State.ACTIVE

        val sessionId2 = "session_id_2"
        val state2 = State.INACTIVE

        val sessionTrackerRef = AtomicReference<SessionTracker<Event, State>>()

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn Collections.unmodifiableList(
                listOf(
                    SessionRecord(sessionId1, state1),
                    SessionRecord(sessionId2, state2)
                )
            )
            on { deleteSessionRecord(sessionId1) } doAnswer {
                assertFalse(sessionTrackerRef.get().consumeEvent(sessionId2, Event.LOGIN))
                Unit
            }
        }
        val listener = mock<SessionTracker.Listener<Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()

        sessionTracker.untrackSession(sessionId1)

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId1, state1)
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId2, state2)
            verify(storage).deleteSessionRecord(sessionId1)
            verify(listener).onSessionTrackingStopped(sessionTracker, sessionId1, State.ACTIVE)
        }

        verifyNoMoreInteractions(storage, listener)

        verify(logger).e(
            SessionTracker.TAG,
            "consumeEvent: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        assertEquals(listOf(SessionRecord(sessionId2, state2)), sessionTracker.getSessionRecords())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#trackSession() from untrackSession()`() {
        val sessionId1 = "session_id_1"
        val state1 = State.ACTIVE

        val sessionTrackerRef = AtomicReference<SessionTracker<Event, State>>()

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn Collections.unmodifiableList(
                listOf(SessionRecord(sessionId1, state1))
            )
            on { deleteSessionRecord(sessionId1) } doAnswer {
                sessionTrackerRef.get().trackSession("session_id_2", State.INACTIVE)
                Unit
            }
        }
        val listener = mock<SessionTracker.Listener<Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()

        sessionTracker.untrackSession(sessionId1)

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId1, state1)
            verify(storage).deleteSessionRecord(sessionId1)
            verify(listener).onSessionTrackingStopped(sessionTracker, sessionId1, State.ACTIVE)
        }

        verifyNoMoreInteractions(storage, listener)

        verify(logger).e(
            SessionTracker.TAG,
            "trackSession: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        assertTrue(sessionTracker.getSessionRecords().isEmpty())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackSession() from untrackSession()`() {
        val sessionId1 = "session_id_1"
        val state1 = State.ACTIVE

        val sessionId2 = "session_id_2"
        val state2 = State.INACTIVE

        val sessionTrackerRef = AtomicReference<SessionTracker<Event, State>>()

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn Collections.unmodifiableList(
                listOf(
                    SessionRecord(sessionId1, state1),
                    SessionRecord(sessionId2, state2)
                )
            )
            on { deleteSessionRecord(sessionId1) } doAnswer {
                sessionTrackerRef.get().untrackSession(sessionId2)
                Unit
            }
        }
        val listener = mock<SessionTracker.Listener<Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()

        sessionTracker.untrackSession(sessionId1)

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId1, state1)
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId2, state2)
            verify(storage).deleteSessionRecord(sessionId1)
            verify(listener).onSessionTrackingStopped(sessionTracker, sessionId1, State.ACTIVE)
        }

        verifyNoMoreInteractions(storage, listener)

        verify(logger).e(
            SessionTracker.TAG,
            "untrackSession: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        assertEquals(listOf(SessionRecord(sessionId2, state2)), sessionTracker.getSessionRecords())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackAllSessions() from untrackSession()`() {
        val sessionId1 = "session_id_1"
        val state1 = State.ACTIVE

        val sessionId2 = "session_id_2"
        val state2 = State.INACTIVE

        val sessionTrackerRef = AtomicReference<SessionTracker<Event, State>>()

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn Collections.unmodifiableList(
                listOf(
                    SessionRecord(sessionId1, state1),
                    SessionRecord(sessionId2, state2)
                )
            )
            on { deleteSessionRecord(sessionId1) } doAnswer {
                sessionTrackerRef.get().untrackAllSessions()
                Unit
            }
        }
        val listener = mock<SessionTracker.Listener<Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()

        sessionTracker.untrackSession(sessionId1)

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId1, state1)
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId2, state2)
            verify(storage).deleteSessionRecord(sessionId1)
            verify(listener).onSessionTrackingStopped(sessionTracker, sessionId1, State.ACTIVE)

        }

        verifyNoMoreInteractions(storage, listener)

        verify(logger).e(
            SessionTracker.TAG,
            "untrackAllSessions: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        assertEquals(listOf(SessionRecord(sessionId2, state2)), sessionTracker.getSessionRecords())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#consumeEvent() from untrackAllSessions()`() {
        val sessionId1 = "session_id_1"
        val state1 = State.ACTIVE

        val sessionId2 = "session_id_2"
        val state2 = State.INACTIVE

        val sessionTrackerRef = AtomicReference<SessionTracker<Event, State>>()

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn Collections.unmodifiableList(
                listOf(
                    SessionRecord(sessionId1, state1),
                    SessionRecord(sessionId2, state2)
                )
            )
            on { deleteAllSessionRecords() } doAnswer {
                assertFalse(sessionTrackerRef.get().consumeEvent(sessionId1, Event.LOGOUT))
                Unit
            }
        }

        val listener = mock<SessionTracker.Listener<Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()

        sessionTracker.untrackAllSessions()

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId1, state1)
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId2, state2)
            verify(storage).deleteAllSessionRecords()
            verify(listener).onAllSessionsTrackingStopped(
                sessionTracker,
                listOf(sessionId1 to state1, sessionId2 to state2)
            )
        }

        verifyNoMoreInteractions(storage, listener)

        verify(logger).e(
            SessionTracker.TAG,
            "consumeEvent: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        assertTrue(sessionTracker.getSessionRecords().isEmpty())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#trackSession() from untrackAllSessions()`() {
        val sessionId1 = "session_id_1"
        val state1 = State.ACTIVE

        val sessionId2 = "session_id_2"
        val state2 = State.INACTIVE

        val sessionTrackerRef = AtomicReference<SessionTracker<Event, State>>()

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn Collections.unmodifiableList(
                listOf(
                    SessionRecord(sessionId1, state1),
                    SessionRecord(sessionId2, state2)
                )
            )
            on { deleteAllSessionRecords() } doAnswer {
                sessionTrackerRef.get().trackSession("session_id_3", State.ACTIVE)
                Unit
            }
        }

        val listener = mock<SessionTracker.Listener<Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()

        sessionTracker.untrackAllSessions()

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId1, state1)
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId2, state2)
            verify(storage).deleteAllSessionRecords()
            verify(listener).onAllSessionsTrackingStopped(
                sessionTracker,
                listOf(sessionId1 to state1, sessionId2 to state2)
            )
        }

        verifyNoMoreInteractions(storage, listener)

        verify(logger).e(
            SessionTracker.TAG,
            "trackSession: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        assertTrue(sessionTracker.getSessionRecords().isEmpty())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackSession() from untrackAllSessions()`() {
        val sessionId1 = "session_id_1"
        val state1 = State.ACTIVE

        val sessionId2 = "session_id_2"
        val state2 = State.INACTIVE

        val sessionTrackerRef = AtomicReference<SessionTracker<Event, State>>()

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn Collections.unmodifiableList(
                listOf(
                    SessionRecord(sessionId1, state1),
                    SessionRecord(sessionId2, state2)
                )
            )
            on { deleteAllSessionRecords() } doAnswer {
                sessionTrackerRef.get().untrackSession(sessionId2)
                Unit
            }
        }

        val listener = mock<SessionTracker.Listener<Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()

        sessionTracker.untrackAllSessions()

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId1, state1)
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId2, state2)
            verify(storage).deleteAllSessionRecords()
            verify(listener).onAllSessionsTrackingStopped(
                sessionTracker,
                listOf(sessionId1 to state1, sessionId2 to state2)
            )
        }

        verifyNoMoreInteractions(storage, listener)

        verify(logger).e(
            SessionTracker.TAG,
            "untrackSession: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        assertTrue(sessionTracker.getSessionRecords().isEmpty())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackAllSessions() from untrackAllSessions()`() {
        val sessionId1 = "session_id_1"
        val state1 = State.ACTIVE

        val sessionId2 = "session_id_2"
        val state2 = State.INACTIVE

        val sessionTrackerRef = AtomicReference<SessionTracker<Event, State>>()

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn Collections.unmodifiableList(
                listOf(
                    SessionRecord(sessionId1, state1),
                    SessionRecord(sessionId2, state2)
                )
            )
            on { deleteAllSessionRecords() } doAnswer {
                sessionTrackerRef.get().untrackAllSessions()
                Unit
            }
        }

        val listener = mock<SessionTracker.Listener<Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()

        sessionTracker.untrackAllSessions()

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId1, state1)
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionId2, state2)
            verify(storage).deleteAllSessionRecords()
            verify(listener).onAllSessionsTrackingStopped(
                sessionTracker,
                listOf(sessionId1 to state1, sessionId2 to state2)
            )
        }

        verifyNoMoreInteractions(storage, listener)

        verify(logger).e(
            SessionTracker.TAG,
            "untrackAllSessions: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        assertTrue(sessionTracker.getSessionRecords().isEmpty())
    }

    private fun createSessionStateTransitionsSupplier() = mock<ISessionStateTransitionsSupplier<Event, State>> {
        on { getStateTransitions(any()) } doReturn listOf(
            Transition(
                Event.LOGIN,
                listOf(State.INACTIVE, State.ACTIVE)
            ),
            Transition(
                Event.LOGOUT,
                listOf(State.ACTIVE, State.INACTIVE)
            ),
            Transition(
                Event.LOGOUT_AND_FORGET,
                listOf(State.ACTIVE, State.FORGOTTEN)
            )
        )
    }

    private fun createStorageMock(sessions: List<SessionRecord<State>>) = mock<ISessionTrackerStorage<State>> {
        on { readAllSessionRecords() } doReturn Collections.unmodifiableList(sessions)
    }
}