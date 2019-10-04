package vit.khudenko.android.sessiontracker

import com.nhaarman.mockitokotlin2.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import vit.khudenko.android.sessiontracker.test_util.Session
import vit.khudenko.android.sessiontracker.test_util.Session.Event
import vit.khudenko.android.sessiontracker.test_util.Session.State
import java.util.*
import java.util.concurrent.atomic.AtomicReference

class SessionTrackerStrictModeTest {

    @get:Rule
    val rule: MockitoRule = MockitoJUnit.rule()

    @get:Rule
    val expectedExceptionRule: ExpectedException = ExpectedException.none()

    private val mode = SessionTracker.Mode.STRICT

    @Test
    fun `consumeEvent() called with uninitialized sessionTracker`() {
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage("SessionTracker must be initialized before calling its #consumeEvent method")

        val storage = mock<ISessionTrackerStorage<Session, State>>()
        val listener = mock<SessionTracker.Listener<Session, Event, State>>()
        val sessionStateTransitionsSupplier = mock<ISessionStateTransitionsSupplier<Session, Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        try {
            sessionTracker.consumeEvent("session_id", Event.LOGIN)
        } catch (e: Exception) {
            verifyZeroInteractions(storage, listener, sessionStateTransitionsSupplier)
            throw e
        }
    }

    @Test
    fun `trackSession() called with uninitialized sessionTracker`() {
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage("SessionTracker must be initialized before calling its #trackSession method")

        val storage = mock<ISessionTrackerStorage<Session, State>>()
        val listener = mock<SessionTracker.Listener<Session, Event, State>>()
        val sessionStateTransitionsSupplier = mock<ISessionStateTransitionsSupplier<Session, Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        try {
            sessionTracker.trackSession(mock(), State.ACTIVE)
        } catch (e: Exception) {
            verifyZeroInteractions(storage, listener, sessionStateTransitionsSupplier)
            throw e
        }
    }

    @Test
    fun `untrackSession() called with uninitialized sessionTracker`() {
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage("SessionTracker must be initialized before calling its #untrackSession method")

        val storage = mock<ISessionTrackerStorage<Session, State>>()
        val listener = mock<SessionTracker.Listener<Session, Event, State>>()
        val sessionStateTransitionsSupplier = mock<ISessionStateTransitionsSupplier<Session, Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        try {
            sessionTracker.untrackSession("session_id")
        } catch (e: Exception) {
            verifyZeroInteractions(storage, listener, sessionStateTransitionsSupplier)
            throw e
        }
    }

    @Test
    fun `untrackAllSessions() called with uninitialized sessionTracker`() {
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage("SessionTracker must be initialized before calling its #untrackAllSessions method")

        val storage = mock<ISessionTrackerStorage<Session, State>>()
        val listener = mock<SessionTracker.Listener<Session, Event, State>>()
        val sessionStateTransitionsSupplier = mock<ISessionStateTransitionsSupplier<Session, Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        try {
            sessionTracker.untrackAllSessions()
        } catch (e: Exception) {
            verifyZeroInteractions(storage, listener, sessionStateTransitionsSupplier)
            throw e
        }
    }

    @Test
    fun `getSessions() called with uninitialized sessionTracker`() {
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage("SessionTracker must be initialized before calling its #getSessions method")

        val storage = mock<ISessionTrackerStorage<Session, State>>()
        val listener = mock<SessionTracker.Listener<Session, Event, State>>()
        val sessionStateTransitionsSupplier = mock<ISessionStateTransitionsSupplier<Session, Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        try {
            sessionTracker.getSessions()
        } catch (e: Exception) {
            verifyZeroInteractions(storage, listener, sessionStateTransitionsSupplier)
            throw e
        }
    }

    @Test
    fun initialization() {
        val session = Session("session_id")
        val state = State.ACTIVE

        val storage = createStorageMock(listOf(SessionRecord(session, state)))
        val listener = mock<SessionTracker.Listener<Session, Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        sessionTracker.initialize()

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, session, state)
        }

        verifyNoMoreInteractions(listener, storage)

        assertEquals(listOf(SessionRecord(session, state)), sessionTracker.getSessions())
    }

    @Test
    fun `initialization with session in a auto-untrack state`() {
        val session = Session("session_id")

        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage(
            "Unable to initialize SessionTracker: session with ID '${session.sessionId}' is in auto-untrack state (${State.FORGOTTEN})"
        )

        val storage = createStorageMock(listOf(SessionRecord(session, State.FORGOTTEN)))
        val listener = mock<SessionTracker.Listener<Session, Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = setOf(State.FORGOTTEN),
            mode = mode,
            logger = mock()
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

        val storage = createStorageMock(listOf(SessionRecord(Session("session_id"), State.ACTIVE)))

        val listener = mock<SessionTracker.Listener<Session, Event, State>>()
        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = mock {
                on { getStateTransitions(any()) } doReturn emptyList() // incomplete config
            },
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
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
        val session = Session("session_id")
        val state = State.ACTIVE

        val storage = createStorageMock(listOf(SessionRecord(session, state)))
        val listener = mock<SessionTracker.Listener<Session, Event, State>>()
        val logger = mock<SessionTracker.Logger>()

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
            verify(listener).onSessionTrackingStarted(sessionTracker, session, state)
        }
        verifyNoMoreInteractions(listener, storage)
        verifyZeroInteractions(logger)

        assertEquals(listOf(SessionRecord(session, state)), sessionTracker.getSessions())

        reset(storage, listener, logger)

        sessionTracker.initialize()

        verify(logger).w(SessionTracker.TAG, "initialize: already initialized, skipping..")
        verifyZeroInteractions(listener, storage)

        assertEquals(listOf(SessionRecord(session, state)), sessionTracker.getSessions())
    }

    @Test
    fun trackSession() {
        val storage = createStorageMock(emptyList())
        val listener = mock<SessionTracker.Listener<Session, Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        sessionTracker.initialize()
        assertTrue(sessionTracker.getSessions().isEmpty())

        val session = Session("session_id")
        val state = State.ACTIVE

        sessionTracker.trackSession(session, state)

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(storage).createSessionRecord(SessionRecord(session, state))
            verify(listener).onSessionTrackingStarted(sessionTracker, session, state)
        }

        verifyNoMoreInteractions(storage, listener)

        assertEquals(listOf(SessionRecord(session, state)), sessionTracker.getSessions())
    }

    @Test
    fun `trackSession() with session in a auto-untrack state`() {
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage(
            "Unable to track session: session with ID 'session_id' is in auto-untrack state (FORGOTTEN)"
        )

        val session = Session("session_id")
        val state = State.FORGOTTEN

        val storage = createStorageMock(emptyList())
        val listener = mock<SessionTracker.Listener<Session, Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = setOf(state),
            mode = mode,
            logger = mock()
        )

        sessionTracker.initialize()

        try {
            sessionTracker.trackSession(session, state)
        } catch (e: Exception) {
            verify(storage).readAllSessionRecords()
            verifyNoMoreInteractions(storage)
            verifyZeroInteractions(listener)

            assertTrue(sessionTracker.getSessions().isEmpty())

            throw e
        }
    }

    @Test
    fun `trackSession() with already tracked session`() {
        val session1 = Session("session_id")
        val state1 = State.ACTIVE

        val storage = createStorageMock(listOf(SessionRecord(session1, state1)))
        val logger = mock<SessionTracker.Logger>()
        val listener = mock<SessionTracker.Listener<Session, Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        val session2 = Session(session1.sessionId) // same session ID

        with(sessionTracker) {
            initialize()
            trackSession(session2, State.INACTIVE)
        }

        with(inOrder(storage, listener, logger)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, session1, state1)
            verify(logger).w(SessionTracker.TAG, "trackSession: session with ID '${session1.sessionId}' already exists")
        }

        verifyNoMoreInteractions(storage, listener)

        assertEquals(listOf(SessionRecord(session1, state1)), sessionTracker.getSessions())
    }

    @Test
    fun `trackSession() without state transitions fails on state machine's builder validation`() {
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage("SessionTracker failed to track session: error creating StateMachine")

        val storage = createStorageMock(emptyList())

        val listener = mock<SessionTracker.Listener<Session, Event, State>>()
        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = mock {
                on { getStateTransitions(any()) } doReturn emptyList() // incomplete config
            },
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        sessionTracker.initialize()
        assertTrue(sessionTracker.getSessions().isEmpty())

        val session = Session("session_id")
        val state = State.ACTIVE

        try {
            sessionTracker.trackSession(session, state)
        } catch (e: Exception) {
            verify(storage).readAllSessionRecords()
            verifyNoMoreInteractions(storage)
            verifyZeroInteractions(listener)
            assertTrue(sessionTracker.getSessions().isEmpty())

            throw e
        }
    }

    @Test
    fun untrackSession() {
        val session = Session("session_id")
        val state = State.ACTIVE

        val storage = createStorageMock(listOf(SessionRecord(session, state)))
        val listener = mock<SessionTracker.Listener<Session, Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        with(sessionTracker) {
            initialize()
            untrackSession(session.sessionId)
        }

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, session, state)
            verify(storage).deleteSessionRecord(session.sessionId)
            verify(listener).onSessionTrackingStopped(sessionTracker, session, state)
        }

        verifyNoMoreInteractions(storage, listener)

        assertTrue(sessionTracker.getSessions().isEmpty())
    }

    @Test
    fun `untrackSession() for an unknown session should be ignored`() {
        val session = Session("session_id")
        val state = State.ACTIVE

        val storage = createStorageMock(listOf(SessionRecord(session, state)))
        val listener = mock<SessionTracker.Listener<Session, Event, State>>()
        val logger = mock<SessionTracker.Logger>()

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

        assertEquals(listOf(SessionRecord(session, state)), sessionTracker.getSessions())
    }

    @Test
    fun untrackAllSessions() {
        val session1 = Session("session_id_1")
        val state1 = State.ACTIVE

        val session2 = Session("session_id_2")
        val state2 = State.INACTIVE

        val storage = createStorageMock(listOf(SessionRecord(session1, state1), SessionRecord(session2, state2)))
        val listener = mock<SessionTracker.Listener<Session, Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        with(sessionTracker) {
            initialize()
            untrackAllSessions()
        }

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, session1, state1)
            verify(listener).onSessionTrackingStarted(sessionTracker, session2, state2)
            verify(storage).deleteAllSessionRecords()
            verify(listener).onAllSessionsTrackingStopped(
                sessionTracker,
                listOf(session1 to state1, session2 to state2)
            )
        }

        verifyNoMoreInteractions(storage, listener)

        assertTrue(sessionTracker.getSessions().isEmpty())
    }

    @Test
    fun consumeEvent() {
        val session1 = Session("session_id_1")
        val state1 = State.ACTIVE

        val session2 = Session("session_id_2")
        val state2 = State.INACTIVE

        val storage = createStorageMock(listOf(SessionRecord(session1, state1), SessionRecord(session2, state2)))
        val listener = mock<SessionTracker.Listener<Session, Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        with(sessionTracker) {
            initialize()
            assertTrue(consumeEvent(session1.sessionId, Event.LOGOUT))
        }

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, session1, state1)
            verify(listener).onSessionTrackingStarted(sessionTracker, session2, state2)
            verify(storage).updateSessionRecord(SessionRecord(session1, State.INACTIVE))
            verify(listener).onSessionStateChanged(sessionTracker, session1, state1, State.INACTIVE)
        }

        verifyNoMoreInteractions(storage, listener)

        assertEquals(
            listOf(
                SessionRecord(session1, State.INACTIVE),
                SessionRecord(session2, state2)
            ),
            sessionTracker.getSessions()
        )
    }

    @Test
    fun `if event is ignored then listeners should not be notified and sessions state should not be persisted`() {
        val session = Session("session_id_1")
        val state = State.ACTIVE

        val storage = createStorageMock(listOf(SessionRecord(session, state)))
        val listener = mock<SessionTracker.Listener<Session, Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        with(sessionTracker) {
            initialize()
            // LOGIN event will be ignored, since current state is ACTIVE
            assertFalse(consumeEvent(session.sessionId, Event.LOGIN))
        }

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, session, state)
        }

        verifyNoMoreInteractions(storage, listener)

        assertEquals(listOf(SessionRecord(session, state)), sessionTracker.getSessions())
    }

    @Test
    fun `consumeEvent() called and session appears in a auto-untrack state`() {
        val session1 = Session("session_id_1")
        val state1 = State.ACTIVE

        val session2 = Session("session_id_2")
        val state2 = State.INACTIVE

        val storage = createStorageMock(listOf(SessionRecord(session1, state1), SessionRecord(session2, state2)))
        val listener = mock<SessionTracker.Listener<Session, Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = setOf(State.FORGOTTEN),
            mode = mode,
            logger = mock()
        )

        with(sessionTracker) {
            initialize()
            assertTrue(consumeEvent(session1.sessionId, Event.LOGOUT_AND_FORGET))
        }

        with(inOrder(listener, storage)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, session1, state1)
            verify(listener).onSessionTrackingStarted(sessionTracker, session2, state2)
            verify(listener).onSessionStateChanged(sessionTracker, session1, state1, State.FORGOTTEN)
            verify(storage).deleteSessionRecord(session1.sessionId)
            verify(listener).onSessionTrackingStopped(sessionTracker, session1, State.FORGOTTEN)
        }

        verifyNoMoreInteractions(storage, listener)

        assertEquals(listOf(SessionRecord(session2, state2)), sessionTracker.getSessions())
    }

    @Test
    fun `consumeEvent() called and session appears in a auto-untrack state being an intermediate state in transition`() {
        val session1 = Session("session_id_1")
        val state1 = State.ACTIVE

        val session2 = Session("session_id_2")
        val state2 = State.INACTIVE

        val storage = createStorageMock(listOf(SessionRecord(session1, state1), SessionRecord(session2, state2)))
        val listener = mock<SessionTracker.Listener<Session, Event, State>>()

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
            logger = mock()
        )

        with(sessionTracker) {
            initialize()
            assertTrue(consumeEvent(session1.sessionId, Event.LOGOUT))
        }

        with(inOrder(listener, storage)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, session1, state1)
            verify(listener).onSessionTrackingStarted(sessionTracker, session2, state2)
            verify(listener).onSessionStateChanged(sessionTracker, session1, state1, State.FORGOTTEN)
            verify(storage).deleteSessionRecord(session1.sessionId)
            verify(listener).onSessionTrackingStopped(sessionTracker, session1, State.FORGOTTEN)
        }

        verifyNoMoreInteractions(storage, listener)

        assertEquals(listOf(SessionRecord(session2, state2)), sessionTracker.getSessions())
    }

    @Test
    fun `consumeEvent() called and session appears in a auto-untrack state being an intermediate state in transition, and listener calls untrackSession()`() {
        val session1 = Session("session_id_1")
        val state1 = State.ACTIVE

        val session2 = Session("session_id_2")
        val state2 = State.INACTIVE

        val storage = createStorageMock(listOf(SessionRecord(session1, state1), SessionRecord(session2, state2)))
        val listener = mock<SessionTracker.Listener<Session, Event, State>> {
            on { onSessionStateChanged(any(), eq(session1), eq(state1), eq(State.FORGOTTEN)) } doAnswer {
                val sessionTracker = it.getArgument<SessionTracker<Session, Event, State>>(0)
                sessionTracker.untrackSession(session1.sessionId)
                Unit
            }
        }

        val logger = mock<SessionTracker.Logger>()
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
            assertTrue(consumeEvent(session1.sessionId, Event.LOGOUT))
        }

        with(inOrder(listener, storage)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, session1, state1)
            verify(listener).onSessionTrackingStarted(sessionTracker, session2, state2)
            verify(listener).onSessionStateChanged(sessionTracker, session1, state1, State.FORGOTTEN)
            verify(storage).deleteSessionRecord(session1.sessionId)
            verify(listener).onSessionTrackingStopped(sessionTracker, session1, State.FORGOTTEN)
        }

        verifyNoMoreInteractions(storage, listener)

        verify(logger).w(
            SessionTracker.TAG,
            "untrackSession: session with ID '${session1.sessionId}' is already untracking"
        )

        assertEquals(listOf(SessionRecord(session2, state2)), sessionTracker.getSessions())
    }

    @Test
    fun `consumeEvent() called and session appears in a auto-untrack state being an intermediate state in transition, and listener calls untrackAllSessions()()`() {
        val session1 = Session("session_id_1")
        val state1 = State.ACTIVE

        val session2 = Session("session_id_2")
        val state2 = State.INACTIVE

        val storage = createStorageMock(listOf(SessionRecord(session1, state1), SessionRecord(session2, state2)))
        val listener = mock<SessionTracker.Listener<Session, Event, State>> {
            on { onSessionStateChanged(any(), eq(session1), eq(state1), eq(State.FORGOTTEN)) } doAnswer {
                val sessionTracker = it.getArgument<SessionTracker<Session, Event, State>>(0)
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
            logger = mock()
        )

        with(sessionTracker) {
            initialize()
            assertTrue(consumeEvent(session1.sessionId, Event.LOGOUT))
        }

        with(inOrder(listener, storage)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, session1, state1)
            verify(listener).onSessionTrackingStarted(sessionTracker, session2, state2)
            verify(listener).onSessionStateChanged(sessionTracker, session1, state1, State.FORGOTTEN)
            verify(storage).deleteAllSessionRecords()
            verify(listener).onAllSessionsTrackingStopped(
                sessionTracker,
                listOf(
                    session1 to State.FORGOTTEN,
                    session2 to state2
                )
            )
        }

        verifyNoMoreInteractions(storage, listener)

        assertTrue(sessionTracker.getSessions().isEmpty())
    }

    @Test
    fun `consumeEvent() for an unknown session should be ignored`() {
        val session = Session("session_id")
        val state = State.ACTIVE

        val storage = createStorageMock(listOf(SessionRecord(session, state)))
        val listener = mock<SessionTracker.Listener<Session, Event, State>>()
        val logger = mock<SessionTracker.Logger>()

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

        assertEquals(listOf(SessionRecord(session, state)), sessionTracker.getSessions())
    }

    @Test
    fun `consumeEvent() for the session being auto-untracked should be ignored`() {
        val session = Session("session_id")
        val state = State.ACTIVE

        val storage = createStorageMock(listOf(SessionRecord(session, state)))
        val listener = mock<SessionTracker.Listener<Session, Event, State>> {
            on { onSessionStateChanged(any(), eq(session), eq(state), eq(State.INACTIVE)) } doAnswer {
                val sessionTracker = it.getArgument<SessionTracker<Session, Event, State>>(0)
                assertFalse(sessionTracker.consumeEvent(session.sessionId, Event.LOGIN))
                Unit
            }
        }
        val logger = mock<SessionTracker.Logger>()

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
        assertEquals(listOf(SessionRecord(session, state)), sessionTracker.getSessions())

        reset(storage, logger)

        assertTrue(sessionTracker.consumeEvent(session.sessionId, Event.LOGOUT))

        with(inOrder(listener, storage, logger)) {
            verify(listener).onSessionTrackingStarted(sessionTracker, session, state)
            verify(logger).d(
                SessionTracker.TAG,
                "onStateChanged: '$state' -> '${State.INACTIVE}', sessionId = '${session.sessionId}', going to auto-untrack session.."
            )
            verify(listener).onSessionStateChanged(sessionTracker, session, state, State.INACTIVE)
            verify(logger).w(
                SessionTracker.TAG,
                "consumeEvent: event = '${Event.LOGIN}', session with ID '${session.sessionId}' is already untracking"
            )
            verify(storage).deleteSessionRecord(session.sessionId)
            verify(listener).onSessionTrackingStopped(sessionTracker, session, State.INACTIVE)
        }

        verifyNoMoreInteractions(storage, listener, logger)

        assertTrue(sessionTracker.getSessions().isEmpty())
    }

    //////////////////////// ---------- detecting misuse at ISessionTrackerStorage ----------- ////////////////////////

    @Test
    fun `storage implementation attempts to call SessionTracker#consumeEvent() from consumeEvent()`() {
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage(
            "consumeEvent: misuse detected, " +
                    "accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        val session1 = Session("session_id_1")
        val state1 = State.ACTIVE

        val session2 = Session("session_id_2")
        val state2 = State.INACTIVE

        val sessionTrackerRef = AtomicReference<SessionTracker<Session, Event, State>>()

        val storage = mock<ISessionTrackerStorage<Session, State>> {
            on { readAllSessionRecords() } doReturn Collections.unmodifiableList(
                listOf(
                    SessionRecord(session1, state1),
                    SessionRecord(session2, state2)
                )
            )
            on { updateSessionRecord(SessionRecord(session1, State.INACTIVE)) } doAnswer {
                sessionTrackerRef.get().consumeEvent(session2.sessionId, Event.LOGIN)
                Unit
            }
        }

        val listener = mock<SessionTracker.Listener<Session, Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()

        try {
            sessionTracker.consumeEvent(session1.sessionId, Event.LOGOUT)
        } catch (e: Exception) {
            with(inOrder(storage, listener)) {
                verify(storage).readAllSessionRecords()
                verify(listener).onSessionTrackingStarted(sessionTracker, session1, state1)
                verify(listener).onSessionTrackingStarted(sessionTracker, session2, state2)
                verify(storage).updateSessionRecord(SessionRecord(session1, State.INACTIVE))
            }

            verifyNoMoreInteractions(storage, listener)

            assertEquals(
                listOf(
                    SessionRecord(session1, State.INACTIVE),
                    SessionRecord(session2, state2)
                ),
                sessionTracker.getSessions()
            )

            throw e
        }
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#trackSession() from consumeEvent()`() {
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage(
            "trackSession: misuse detected, " +
                    "accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        val session1 = Session("session_id_1")
        val state1 = State.ACTIVE

        val sessionTrackerRef = AtomicReference<SessionTracker<Session, Event, State>>()

        val storage = mock<ISessionTrackerStorage<Session, State>> {
            on { readAllSessionRecords() } doReturn Collections.unmodifiableList(
                listOf(SessionRecord(session1, state1))
            )
            on { updateSessionRecord(SessionRecord(session1, State.INACTIVE)) } doAnswer {
                sessionTrackerRef.get().trackSession(Session("session_id_2"), State.INACTIVE)
                Unit
            }
        }

        val listener = mock<SessionTracker.Listener<Session, Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()

        try {
            sessionTracker.consumeEvent(session1.sessionId, Event.LOGOUT)
        } catch (e: Exception) {
            with(inOrder(storage, listener)) {
                verify(storage).readAllSessionRecords()
                verify(listener).onSessionTrackingStarted(sessionTracker, session1, state1)
                verify(storage).updateSessionRecord(SessionRecord(session1, State.INACTIVE))
            }

            verifyNoMoreInteractions(storage, listener)

            assertEquals(listOf(SessionRecord(session1, State.INACTIVE)), sessionTracker.getSessions())

            throw e
        }
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackSession() from consumeEvent()`() {
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage(
            "untrackSession: misuse detected, " +
                    "accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        val session1 = Session("session_id_1")
        val state1 = State.ACTIVE

        val sessionTrackerRef = AtomicReference<SessionTracker<Session, Event, State>>()

        val storage = mock<ISessionTrackerStorage<Session, State>> {
            on { readAllSessionRecords() } doReturn Collections.unmodifiableList(
                listOf(SessionRecord(session1, state1))
            )
            on { updateSessionRecord(SessionRecord(session1, State.INACTIVE)) } doAnswer {
                sessionTrackerRef.get().untrackSession(session1.sessionId)
                Unit
            }
        }

        val listener = mock<SessionTracker.Listener<Session, Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()

        try {
            sessionTracker.consumeEvent(session1.sessionId, Event.LOGOUT)
        } catch (e: Exception) {
            with(inOrder(storage, listener)) {
                verify(storage).readAllSessionRecords()
                verify(listener).onSessionTrackingStarted(sessionTracker, session1, state1)
                verify(storage).updateSessionRecord(SessionRecord(session1, State.INACTIVE))
            }

            verifyNoMoreInteractions(storage, listener)

            assertEquals(listOf(SessionRecord(session1, State.INACTIVE)), sessionTracker.getSessions())

            throw e
        }
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackAllSessions() from consumeEvent()`() {
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage(
            "untrackAllSessions: misuse detected, " +
                    "accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        val session1 = Session("session_id_1")
        val state1 = State.ACTIVE

        val session2 = Session("session_id_2")
        val state2 = State.INACTIVE

        val sessionTrackerRef = AtomicReference<SessionTracker<Session, Event, State>>()

        val storage = mock<ISessionTrackerStorage<Session, State>> {
            on { readAllSessionRecords() } doReturn Collections.unmodifiableList(
                listOf(
                    SessionRecord(session1, state1),
                    SessionRecord(session2, state2)
                )
            )
            on { updateSessionRecord(SessionRecord(session1, State.INACTIVE)) } doAnswer {
                sessionTrackerRef.get().untrackAllSessions()
                Unit
            }
        }

        val listener = mock<SessionTracker.Listener<Session, Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()

        try {
            sessionTracker.consumeEvent(session1.sessionId, Event.LOGOUT)
        } catch (e: Exception) {
            with(inOrder(storage, listener)) {
                verify(storage).readAllSessionRecords()
                verify(listener).onSessionTrackingStarted(sessionTracker, session1, state1)
                verify(listener).onSessionTrackingStarted(sessionTracker, session2, state2)
                verify(storage).updateSessionRecord(SessionRecord(session1, State.INACTIVE))
            }

            verifyNoMoreInteractions(storage, listener)

            assertEquals(
                listOf(
                    SessionRecord(session1, State.INACTIVE),
                    SessionRecord(session2, state2)
                ),
                sessionTracker.getSessions()
            )

            throw  e
        }
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#consumeEvent() from trackSession()`() {
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage(
            "consumeEvent: misuse detected, " +
                    "accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        val session = Session("session_id")
        val state = State.ACTIVE

        val sessionTrackerRef = AtomicReference<SessionTracker<Session, Event, State>>()

        val storage = mock<ISessionTrackerStorage<Session, State>> {
            on { readAllSessionRecords() } doReturn emptyList()
            on { createSessionRecord(SessionRecord(session, state)) } doAnswer {
                sessionTrackerRef.get().consumeEvent(session.sessionId, Event.LOGOUT)
                Unit
            }
        }
        val listener = mock<SessionTracker.Listener<Session, Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()
        assertTrue(sessionTracker.getSessions().isEmpty())

        try {
            sessionTracker.trackSession(session, state)
        } catch (e: Exception) {
            with(inOrder(storage, listener)) {
                verify(storage).readAllSessionRecords()
                verify(storage).createSessionRecord(SessionRecord(session, state))
            }

            verifyNoMoreInteractions(storage, listener)

            assertTrue(sessionTracker.getSessions().isEmpty())

            throw e
        }
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#trackSession() from trackSession()`() {
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage(
            "trackSession: misuse detected, " +
                    "accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        val session = Session("session_id")
        val state = State.ACTIVE

        val sessionTrackerRef = AtomicReference<SessionTracker<Session, Event, State>>()

        val storage = mock<ISessionTrackerStorage<Session, State>> {
            on { readAllSessionRecords() } doReturn emptyList()
            on { createSessionRecord(SessionRecord(session, state)) } doAnswer {
                sessionTrackerRef.get().trackSession(Session("session_id_2"), State.INACTIVE)
                Unit
            }
        }
        val listener = mock<SessionTracker.Listener<Session, Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()
        assertTrue(sessionTracker.getSessions().isEmpty())

        try {
            sessionTracker.trackSession(session, state)
        } catch (e: Exception) {
            with(inOrder(storage, listener)) {
                verify(storage).readAllSessionRecords()
                verify(storage).createSessionRecord(SessionRecord(session, state))
            }

            verifyNoMoreInteractions(storage, listener)

            assertTrue(sessionTracker.getSessions().isEmpty())

            throw e
        }
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackSession() from trackSession()`() {
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage(
            "untrackSession: misuse detected, " +
                    "accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        val session = Session("session_id")
        val state = State.ACTIVE

        val sessionTrackerRef = AtomicReference<SessionTracker<Session, Event, State>>()

        val storage = mock<ISessionTrackerStorage<Session, State>> {
            on { readAllSessionRecords() } doReturn emptyList()
            on { createSessionRecord(SessionRecord(session, state)) } doAnswer {
                sessionTrackerRef.get().untrackSession(session.sessionId)
                Unit
            }
        }
        val listener = mock<SessionTracker.Listener<Session, Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()
        assertTrue(sessionTracker.getSessions().isEmpty())

        try {
            sessionTracker.trackSession(session, state)
        } catch (e: Exception) {
            with(inOrder(storage, listener)) {
                verify(storage).readAllSessionRecords()
                verify(storage).createSessionRecord(SessionRecord(session, state))
            }

            verifyNoMoreInteractions(storage, listener)

            assertTrue(sessionTracker.getSessions().isEmpty())

            throw e
        }
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackAllSessions() from trackSession()`() {
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage(
            "untrackAllSessions: misuse detected, " +
                    "accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        val session = Session("session_id")
        val state = State.ACTIVE

        val sessionTrackerRef = AtomicReference<SessionTracker<Session, Event, State>>()

        val storage = mock<ISessionTrackerStorage<Session, State>> {
            on { readAllSessionRecords() } doReturn emptyList()
            on { createSessionRecord(SessionRecord(session, state)) } doAnswer {
                sessionTrackerRef.get().untrackAllSessions()
                Unit
            }
        }
        val listener = mock<SessionTracker.Listener<Session, Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()
        assertTrue(sessionTracker.getSessions().isEmpty())

        try {
            sessionTracker.trackSession(session, state)
        } catch (e: Exception) {
            with(inOrder(storage, listener)) {
                verify(storage).readAllSessionRecords()
                verify(storage).createSessionRecord(SessionRecord(session, state))
            }

            verifyNoMoreInteractions(storage, listener)

            assertTrue(sessionTracker.getSessions().isEmpty())

            throw e
        }
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#consumeEvent() from untrackSession()`() {
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage(
            "consumeEvent: misuse detected, " +
                    "accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        val session1 = Session("session_id_1")
        val state1 = State.ACTIVE

        val session2 = Session("session_id_2")
        val state2 = State.INACTIVE

        val sessionTrackerRef = AtomicReference<SessionTracker<Session, Event, State>>()

        val storage = mock<ISessionTrackerStorage<Session, State>> {
            on { readAllSessionRecords() } doReturn Collections.unmodifiableList(
                listOf(
                    SessionRecord(session1, state1),
                    SessionRecord(session2, state2)
                )
            )
            on { deleteSessionRecord(session1.sessionId) } doAnswer {
                sessionTrackerRef.get().consumeEvent(session2.sessionId, Event.LOGIN)
                Unit
            }
        }
        val listener = mock<SessionTracker.Listener<Session, Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()

        try {
            sessionTracker.untrackSession(session1.sessionId)
        } catch (e: Exception) {
            with(inOrder(storage, listener)) {
                verify(storage).readAllSessionRecords()
                verify(listener).onSessionTrackingStarted(sessionTracker, session1, state1)
                verify(listener).onSessionTrackingStarted(sessionTracker, session2, state2)
                verify(storage).deleteSessionRecord(session1.sessionId)
            }

            verifyNoMoreInteractions(storage, listener)

            assertEquals(
                listOf(
                    SessionRecord(session1, state1),
                    SessionRecord(session2, state2)
                ),
                sessionTracker.getSessions()
            )

            throw e
        }
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#trackSession() from untrackSession()`() {
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage(
            "trackSession: misuse detected, " +
                    "accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        val session1 = Session("session_id_1")
        val state1 = State.ACTIVE

        val sessionTrackerRef = AtomicReference<SessionTracker<Session, Event, State>>()

        val storage = mock<ISessionTrackerStorage<Session, State>> {
            on { readAllSessionRecords() } doReturn Collections.unmodifiableList(
                listOf(SessionRecord(session1, state1))
            )
            on { deleteSessionRecord(session1.sessionId) } doAnswer {
                sessionTrackerRef.get().trackSession(Session("session_id_2"), State.INACTIVE)
                Unit
            }
        }
        val listener = mock<SessionTracker.Listener<Session, Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()

        try {
            sessionTracker.untrackSession(session1.sessionId)
        } catch (e: Exception) {
            with(inOrder(storage, listener)) {
                verify(storage).readAllSessionRecords()
                verify(listener).onSessionTrackingStarted(sessionTracker, session1, state1)
                verify(storage).deleteSessionRecord(session1.sessionId)
            }

            verifyNoMoreInteractions(storage, listener)

            assertEquals(listOf(SessionRecord(session1, state1)), sessionTracker.getSessions())

            throw e
        }
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackSession() from untrackSession()`() {
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage(
            "untrackSession: misuse detected, " +
                    "accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        val session1 = Session("session_id_1")
        val state1 = State.ACTIVE

        val session2 = Session("session_id_2")
        val state2 = State.INACTIVE

        val sessionTrackerRef = AtomicReference<SessionTracker<Session, Event, State>>()

        val storage = mock<ISessionTrackerStorage<Session, State>> {
            on { readAllSessionRecords() } doReturn Collections.unmodifiableList(
                listOf(
                    SessionRecord(session1, state1),
                    SessionRecord(session2, state2)
                )
            )
            on { deleteSessionRecord(session1.sessionId) } doAnswer {
                sessionTrackerRef.get().untrackSession(session2.sessionId)
                Unit
            }
        }
        val listener = mock<SessionTracker.Listener<Session, Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()

        try {
            sessionTracker.untrackSession(session1.sessionId)
        } catch (e: Exception) {
            with(inOrder(storage, listener)) {
                verify(storage).readAllSessionRecords()
                verify(listener).onSessionTrackingStarted(sessionTracker, session1, state1)
                verify(listener).onSessionTrackingStarted(sessionTracker, session2, state2)
                verify(storage).deleteSessionRecord(session1.sessionId)
            }

            verifyNoMoreInteractions(storage, listener)

            assertEquals(
                listOf(
                    SessionRecord(session1, state1),
                    SessionRecord(session2, state2)
                ),
                sessionTracker.getSessions()
            )

            throw e
        }
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackAllSessions() from untrackSession()`() {
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage(
            "untrackAllSessions: misuse detected, " +
                    "accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        val session1 = Session("session_id_1")
        val state1 = State.ACTIVE

        val session2 = Session("session_id_2")
        val state2 = State.INACTIVE

        val sessionTrackerRef = AtomicReference<SessionTracker<Session, Event, State>>()

        val storage = mock<ISessionTrackerStorage<Session, State>> {
            on { readAllSessionRecords() } doReturn Collections.unmodifiableList(
                listOf(
                    SessionRecord(session1, state1),
                    SessionRecord(session2, state2)
                )
            )
            on { deleteSessionRecord(session1.sessionId) } doAnswer {
                sessionTrackerRef.get().untrackAllSessions()
                Unit
            }
        }
        val listener = mock<SessionTracker.Listener<Session, Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()

        try {
            sessionTracker.untrackSession(session1.sessionId)
        } catch (e: Exception) {
            with(inOrder(storage, listener)) {
                verify(storage).readAllSessionRecords()
                verify(listener).onSessionTrackingStarted(sessionTracker, session1, state1)
                verify(listener).onSessionTrackingStarted(sessionTracker, session2, state2)
                verify(storage).deleteSessionRecord(session1.sessionId)
            }

            verifyNoMoreInteractions(storage, listener)

            assertEquals(
                listOf(
                    SessionRecord(session1, state1),
                    SessionRecord(session2, state2)
                ),
                sessionTracker.getSessions()
            )

            throw e
        }
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#consumeEvent() from untrackAllSessions()`() {
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage(
            "consumeEvent: misuse detected, " +
                    "accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        val session1 = Session("session_id_1")
        val state1 = State.ACTIVE

        val session2 = Session("session_id_2")
        val state2 = State.INACTIVE

        val sessionTrackerRef = AtomicReference<SessionTracker<Session, Event, State>>()

        val storage = mock<ISessionTrackerStorage<Session, State>> {
            on { readAllSessionRecords() } doReturn Collections.unmodifiableList(
                listOf(
                    SessionRecord(session1, state1),
                    SessionRecord(session2, state2)
                )
            )
            on { deleteAllSessionRecords() } doAnswer {
                sessionTrackerRef.get().consumeEvent(session1.sessionId, Event.LOGOUT)
                Unit
            }
        }

        val listener = mock<SessionTracker.Listener<Session, Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()

        try {
            sessionTracker.untrackAllSessions()
        } catch (e: Exception) {
            with(inOrder(storage, listener)) {
                verify(storage).readAllSessionRecords()
                verify(listener).onSessionTrackingStarted(sessionTracker, session1, state1)
                verify(listener).onSessionTrackingStarted(sessionTracker, session2, state2)
                verify(storage).deleteAllSessionRecords()
            }

            verifyNoMoreInteractions(storage, listener)

            assertEquals(
                listOf(
                    SessionRecord(session1, state1),
                    SessionRecord(session2, state2)
                ),
                sessionTracker.getSessions()
            )

            throw e
        }
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#trackSession() from untrackAllSessions()`() {
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage(
            "trackSession: misuse detected, " +
                    "accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        val session1 = Session("session_id_1")
        val state1 = State.ACTIVE

        val session2 = Session("session_id_2")
        val state2 = State.INACTIVE

        val sessionTrackerRef = AtomicReference<SessionTracker<Session, Event, State>>()

        val storage = mock<ISessionTrackerStorage<Session, State>> {
            on { readAllSessionRecords() } doReturn Collections.unmodifiableList(
                listOf(
                    SessionRecord(session1, state1),
                    SessionRecord(session2, state2)
                )
            )
            on { deleteAllSessionRecords() } doAnswer {
                sessionTrackerRef.get().trackSession(Session("session_id_3"), State.ACTIVE)
                Unit
            }
        }

        val listener = mock<SessionTracker.Listener<Session, Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()

        try {
            sessionTracker.untrackAllSessions()
        } catch (e: Exception) {
            with(inOrder(storage, listener)) {
                verify(storage).readAllSessionRecords()
                verify(listener).onSessionTrackingStarted(sessionTracker, session1, state1)
                verify(listener).onSessionTrackingStarted(sessionTracker, session2, state2)
                verify(storage).deleteAllSessionRecords()
            }

            verifyNoMoreInteractions(storage, listener)

            assertEquals(
                listOf(
                    SessionRecord(session1, state1),
                    SessionRecord(session2, state2)
                ),
                sessionTracker.getSessions()
            )

            throw e
        }
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackSession() from untrackAllSessions()`() {
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage(
            "untrackSession: misuse detected, " +
                    "accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        val session1 = Session("session_id_1")
        val state1 = State.ACTIVE

        val session2 = Session("session_id_2")
        val state2 = State.INACTIVE

        val sessionTrackerRef = AtomicReference<SessionTracker<Session, Event, State>>()

        val storage = mock<ISessionTrackerStorage<Session, State>> {
            on { readAllSessionRecords() } doReturn Collections.unmodifiableList(
                listOf(
                    SessionRecord(session1, state1),
                    SessionRecord(session2, state2)
                )
            )
            on { deleteAllSessionRecords() } doAnswer {
                sessionTrackerRef.get().untrackSession(session2.sessionId)
                Unit
            }
        }

        val listener = mock<SessionTracker.Listener<Session, Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()

        try {
            sessionTracker.untrackAllSessions()
        } catch (e: Exception) {
            with(inOrder(storage, listener)) {
                verify(storage).readAllSessionRecords()
                verify(listener).onSessionTrackingStarted(sessionTracker, session1, state1)
                verify(listener).onSessionTrackingStarted(sessionTracker, session2, state2)
                verify(storage).deleteAllSessionRecords()
            }

            verifyNoMoreInteractions(storage, listener)

            assertEquals(
                listOf(
                    SessionRecord(session1, state1),
                    SessionRecord(session2, state2)
                ),
                sessionTracker.getSessions()
            )

            throw e
        }
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackAllSessions() from untrackAllSessions()`() {
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage(
            "untrackAllSessions: misuse detected, " +
                    "accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        val session1 = Session("session_id_1")
        val state1 = State.ACTIVE

        val session2 = Session("session_id_2")
        val state2 = State.INACTIVE

        val sessionTrackerRef = AtomicReference<SessionTracker<Session, Event, State>>()

        val storage = mock<ISessionTrackerStorage<Session, State>> {
            on { readAllSessionRecords() } doReturn Collections.unmodifiableList(
                listOf(
                    SessionRecord(session1, state1),
                    SessionRecord(session2, state2)
                )
            )
            on { deleteAllSessionRecords() } doAnswer {
                sessionTrackerRef.get().untrackAllSessions()
                Unit
            }
        }

        val listener = mock<SessionTracker.Listener<Session, Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()

        try {
            sessionTracker.untrackAllSessions()
        } catch (e: Exception) {
            with(inOrder(storage, listener)) {
                verify(storage).readAllSessionRecords()
                verify(listener).onSessionTrackingStarted(sessionTracker, session1, state1)
                verify(listener).onSessionTrackingStarted(sessionTracker, session2, state2)
                verify(storage).deleteAllSessionRecords()
            }

            verifyNoMoreInteractions(storage, listener)

            assertEquals(
                listOf(
                    SessionRecord(session1, state1),
                    SessionRecord(session2, state2)
                ),
                sessionTracker.getSessions()
            )

            throw e
        }
    }

    private fun createSessionStateTransitionsSupplier() =
        mock<ISessionStateTransitionsSupplier<Session, Event, State>> {
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

    private fun createStorageMock(
        sessions: List<SessionRecord<Session, State>>
    ): ISessionTrackerStorage<Session, State> = mock {
        on { readAllSessionRecords() } doReturn Collections.unmodifiableList(sessions)
    }
}