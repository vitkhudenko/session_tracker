package vit.khudenko.android.sessiontracker

import com.nhaarman.mockitokotlin2.*
import org.junit.Assert.*
import org.junit.Before
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
        val storage = mock<ISessionTrackerStorage<Session, State>>()
        val listener = mock<SessionTracker.Listener<Session, Event, State>>()
        val sessionStateTransitionsSupplier = mock<ISessionStateTransitionsSupplier<Session, Event, State>>()

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
        val storage = mock<ISessionTrackerStorage<Session, State>>()
        val listener = mock<SessionTracker.Listener<Session, Event, State>>()
        val sessionStateTransitionsSupplier = mock<ISessionStateTransitionsSupplier<Session, Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        sessionTracker.trackSession(mock(), State.ACTIVE)

        verify(logger).e(
            SessionTracker.TAG,
            "SessionTracker must be initialized before calling its #trackSession method"
        )
        verifyZeroInteractions(storage, listener, sessionStateTransitionsSupplier)
    }

    @Test
    fun `untrackSession() called with uninitialized sessionTracker`() {
        val storage = mock<ISessionTrackerStorage<Session, State>>()
        val listener = mock<SessionTracker.Listener<Session, Event, State>>()
        val sessionStateTransitionsSupplier = mock<ISessionStateTransitionsSupplier<Session, Event, State>>()

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
        val storage = mock<ISessionTrackerStorage<Session, State>>()
        val listener = mock<SessionTracker.Listener<Session, Event, State>>()
        val sessionStateTransitionsSupplier = mock<ISessionStateTransitionsSupplier<Session, Event, State>>()

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
    fun `getSessions() called with uninitialized sessionTracker`() {
        val storage = mock<ISessionTrackerStorage<Session, State>>()
        val listener = mock<SessionTracker.Listener<Session, Event, State>>()
        val sessionStateTransitionsSupplier = mock<ISessionStateTransitionsSupplier<Session, Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        val sessions = sessionTracker.getSessions()
        assertTrue(sessions.isEmpty())

        verify(logger).e(
            SessionTracker.TAG,
            "SessionTracker must be initialized before calling its #getSessions method"
        )
        verifyZeroInteractions(storage, listener, sessionStateTransitionsSupplier)
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
            logger = logger
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

        val storage = createStorageMock(listOf(SessionRecord(session, State.FORGOTTEN)))
        val listener = mock<SessionTracker.Listener<Session, Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = setOf(State.FORGOTTEN),
            mode = mode,
            logger = logger
        )

        sessionTracker.initialize()

        verify(storage).readAllSessionRecords()
        verify(logger).e(
            SessionTracker.TAG,
            "initialize: session with ID '${session.sessionId}' is in auto-untrack state (${State.FORGOTTEN})" +
                    ", rejecting this session"
        )
        verifyNoMoreInteractions(storage)
        verifyZeroInteractions(listener)
    }

    @Test
    fun `initialization without state transitions fails on state machine's builder validation`() {
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage("Unable to initialize SessionTracker: error creating StateMachine")

        val storage = createStorageMock(listOf(SessionRecord(Session("session_id"), State.ACTIVE)))

        val listener = mock<SessionTracker.Listener<Session, Event, State>>()
        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = object : ISessionStateTransitionsSupplier<Session, Event, State> {
                override fun getStateTransitions(session: Session): List<Transition<Event, State>> {
                    return emptyList() // incomplete config
                }
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
    fun trackSession() {
        val storage = createStorageMock(emptyList())
        val listener = mock<SessionTracker.Listener<Session, Event, State>>()

        val sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
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
            logger = logger
        )

        with(sessionTracker) {
            initialize()
            trackSession(session, state)
        }

        verify(storage).readAllSessionRecords()
        verify(logger).e(
            SessionTracker.TAG,
            "trackSession: session with ID '${session.sessionId}' is in auto-untrack state (${State.FORGOTTEN}), " +
                    "rejecting this session"
        )
        verifyNoMoreInteractions(storage)
        verifyZeroInteractions(listener)

        assertTrue(sessionTracker.getSessions().isEmpty())
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
            logger = logger
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
            logger = logger
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
            verify(listener).onAllSessionsTrackingStopped(sessionTracker, listOf(session1 to state1, session2 to state2))
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
            logger = logger
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
            logger = logger
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
            logger = logger
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

    //////////////////////// ---------- detecting misuse at ISessionTrackerStorage ----------- ////////////////////////

    @Test
    fun `storage implementation attempts to call SessionTracker#consumeEvent() from consumeEvent()`() {
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
                assertFalse(sessionTrackerRef.get().consumeEvent(session2.sessionId, Event.LOGIN))
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
            logger = logger
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()
        assertTrue(sessionTracker.consumeEvent(session1.sessionId, Event.LOGOUT))

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, session1, state1)
            verify(listener).onSessionTrackingStarted(sessionTracker, session2, state2)
            verify(storage).updateSessionRecord(SessionRecord(session1, State.INACTIVE))
            verify(listener).onSessionStateChanged(sessionTracker, session1, state1, State.INACTIVE)
        }

        verifyNoMoreInteractions(storage, listener)

        verify(logger).e(
            SessionTracker.TAG,
            "consumeEvent: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        assertEquals(
            listOf(
                SessionRecord(session1, State.INACTIVE),
                SessionRecord(session2, state2)
            ),
            sessionTracker.getSessions()
        )
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#trackSession() from consumeEvent()`() {
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
            logger = logger
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()

        assertTrue(sessionTracker.consumeEvent(session1.sessionId, Event.LOGOUT))

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, session1, state1)
            verify(storage).updateSessionRecord(SessionRecord(session1, State.INACTIVE))
            verify(listener).onSessionStateChanged(sessionTracker, session1, state1, State.INACTIVE)
        }

        verifyNoMoreInteractions(storage, listener)

        verify(logger).e(
            SessionTracker.TAG,
            "trackSession: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        assertEquals(listOf(SessionRecord(session1, State.INACTIVE)), sessionTracker.getSessions())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackSession() from consumeEvent()`() {
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
            logger = logger
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()

        assertTrue(sessionTracker.consumeEvent(session1.sessionId, Event.LOGOUT))

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, session1, state1)
            verify(storage).updateSessionRecord(SessionRecord(session1, State.INACTIVE))
            verify(listener).onSessionStateChanged(sessionTracker, session1, state1, State.INACTIVE)
        }

        verifyNoMoreInteractions(storage, listener)

        verify(logger).e(
            SessionTracker.TAG,
            "untrackSession: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        assertEquals(listOf(SessionRecord(session1, State.INACTIVE)), sessionTracker.getSessions())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackAllSessions() from consumeEvent()`() {
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
            logger = logger
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()

        assertTrue(sessionTracker.consumeEvent(session1.sessionId, Event.LOGOUT))

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, session1, state1)
            verify(listener).onSessionTrackingStarted(sessionTracker, session2, state2)
            verify(storage).updateSessionRecord(SessionRecord(session1, State.INACTIVE))
            verify(listener).onSessionStateChanged(sessionTracker, session1, state1, State.INACTIVE)
        }

        verifyNoMoreInteractions(storage, listener)

        verify(logger).e(
            SessionTracker.TAG,
            "untrackAllSessions: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        assertEquals(
            listOf(
                SessionRecord(session1, State.INACTIVE),
                SessionRecord(session2, state2)
            ),
            sessionTracker.getSessions()
        )
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#consumeEvent() from trackSession()`() {
        val session = Session("session_id")
        val state = State.ACTIVE

        val sessionTrackerRef = AtomicReference<SessionTracker<Session, Event, State>>()

        val storage = mock<ISessionTrackerStorage<Session, State>> {
            on { readAllSessionRecords() } doReturn emptyList()
            on { createSessionRecord(SessionRecord(session, state)) } doAnswer {
                assertFalse(sessionTrackerRef.get().consumeEvent(session.sessionId, Event.LOGOUT))
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
            logger = logger
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()
        assertTrue(sessionTracker.getSessions().isEmpty())

        sessionTracker.trackSession(session, state)

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(storage).createSessionRecord(SessionRecord(session, state))
            verify(listener).onSessionTrackingStarted(sessionTracker, session, State.ACTIVE)
        }

        verifyNoMoreInteractions(storage, listener)

        verify(logger).e(
            SessionTracker.TAG,
            "consumeEvent: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        assertEquals(listOf(SessionRecord(session, State.ACTIVE)), sessionTracker.getSessions())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#trackSession() from trackSession()`() {
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
            logger = logger
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()
        assertTrue(sessionTracker.getSessions().isEmpty())

        sessionTracker.trackSession(session, state)

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(storage).createSessionRecord(SessionRecord(session, state))
            verify(listener).onSessionTrackingStarted(sessionTracker, session, State.ACTIVE)
        }

        verifyNoMoreInteractions(storage, listener)

        verify(logger).e(
            SessionTracker.TAG,
            "trackSession: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        assertEquals(listOf(SessionRecord(session, State.ACTIVE)), sessionTracker.getSessions())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackSession() from trackSession()`() {
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
            logger = logger
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()
        assertTrue(sessionTracker.getSessions().isEmpty())

        sessionTracker.trackSession(session, state)

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(storage).createSessionRecord(SessionRecord(session, state))
            verify(listener).onSessionTrackingStarted(sessionTracker, session, State.ACTIVE)
        }

        verifyNoMoreInteractions(storage, listener)

        verify(logger).e(
            SessionTracker.TAG,
            "untrackSession: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        assertEquals(listOf(SessionRecord(session, State.ACTIVE)), sessionTracker.getSessions())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackAllSessions() from trackSession()`() {
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
            logger = logger
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()
        assertTrue(sessionTracker.getSessions().isEmpty())

        sessionTracker.trackSession(session, state)

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(storage).createSessionRecord(SessionRecord(session, state))
            verify(listener).onSessionTrackingStarted(sessionTracker, session, State.ACTIVE)
        }

        verifyNoMoreInteractions(storage, listener)

        verify(logger).e(
            SessionTracker.TAG,
            "untrackAllSessions: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        assertEquals(listOf(SessionRecord(session, State.ACTIVE)), sessionTracker.getSessions())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#consumeEvent() from untrackSession()`() {
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
                assertFalse(sessionTrackerRef.get().consumeEvent(session2.sessionId, Event.LOGIN))
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
            logger = logger
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()

        sessionTracker.untrackSession(session1.sessionId)

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, session1, state1)
            verify(listener).onSessionTrackingStarted(sessionTracker, session2, state2)
            verify(storage).deleteSessionRecord(session1.sessionId)
            verify(listener).onSessionTrackingStopped(sessionTracker, session1, State.ACTIVE)
        }

        verifyNoMoreInteractions(storage, listener)

        verify(logger).e(
            SessionTracker.TAG,
            "consumeEvent: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        assertEquals(listOf(SessionRecord(session2, state2)), sessionTracker.getSessions())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#trackSession() from untrackSession()`() {
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
            logger = logger
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()

        sessionTracker.untrackSession(session1.sessionId)

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, session1, state1)
            verify(storage).deleteSessionRecord(session1.sessionId)
            verify(listener).onSessionTrackingStopped(sessionTracker, session1, State.ACTIVE)
        }

        verifyNoMoreInteractions(storage, listener)

        verify(logger).e(
            SessionTracker.TAG,
            "trackSession: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        assertTrue(sessionTracker.getSessions().isEmpty())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackSession() from untrackSession()`() {
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
            logger = logger
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()

        sessionTracker.untrackSession(session1.sessionId)

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, session1, state1)
            verify(listener).onSessionTrackingStarted(sessionTracker, session2, state2)
            verify(storage).deleteSessionRecord(session1.sessionId)
            verify(listener).onSessionTrackingStopped(sessionTracker, session1, State.ACTIVE)
        }

        verifyNoMoreInteractions(storage, listener)

        verify(logger).e(
            SessionTracker.TAG,
            "untrackSession: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        assertEquals(listOf(SessionRecord(session2, state2)), sessionTracker.getSessions())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackAllSessions() from untrackSession()`() {
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
            logger = logger
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()

        sessionTracker.untrackSession(session1.sessionId)

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, session1, state1)
            verify(listener).onSessionTrackingStarted(sessionTracker, session2, state2)
            verify(storage).deleteSessionRecord(session1.sessionId)
            verify(listener).onSessionTrackingStopped(sessionTracker, session1, State.ACTIVE)

        }

        verifyNoMoreInteractions(storage, listener)

        verify(logger).e(
            SessionTracker.TAG,
            "untrackAllSessions: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        assertEquals(listOf(SessionRecord(session2, state2)), sessionTracker.getSessions())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#consumeEvent() from untrackAllSessions()`() {
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
                assertFalse(sessionTrackerRef.get().consumeEvent(session1.sessionId, Event.LOGOUT))
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
            logger = logger
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()

        sessionTracker.untrackAllSessions()

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, session1, state1)
            verify(listener).onSessionTrackingStarted(sessionTracker, session2, state2)
            verify(storage).deleteAllSessionRecords()
            verify(listener).onAllSessionsTrackingStopped(sessionTracker, listOf(session1 to state1, session2 to state2))
        }

        verifyNoMoreInteractions(storage, listener)

        verify(logger).e(
            SessionTracker.TAG,
            "consumeEvent: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        assertTrue(sessionTracker.getSessions().isEmpty())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#trackSession() from untrackAllSessions()`() {
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
            logger = logger
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()

        sessionTracker.untrackAllSessions()

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, session1, state1)
            verify(listener).onSessionTrackingStarted(sessionTracker, session2, state2)
            verify(storage).deleteAllSessionRecords()
            verify(listener).onAllSessionsTrackingStopped(sessionTracker, listOf(session1 to state1, session2 to state2))
        }

        verifyNoMoreInteractions(storage, listener)

        verify(logger).e(
            SessionTracker.TAG,
            "trackSession: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        assertTrue(sessionTracker.getSessions().isEmpty())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackSession() from untrackAllSessions()`() {
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
            logger = logger
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()

        sessionTracker.untrackAllSessions()

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, session1, state1)
            verify(listener).onSessionTrackingStarted(sessionTracker, session2, state2)
            verify(storage).deleteAllSessionRecords()
            verify(listener).onAllSessionsTrackingStopped(sessionTracker, listOf(session1 to state1, session2 to state2))
        }

        verifyNoMoreInteractions(storage, listener)

        verify(logger).e(
            SessionTracker.TAG,
            "untrackSession: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        assertTrue(sessionTracker.getSessions().isEmpty())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackAllSessions() from untrackAllSessions()`() {
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
            logger = logger
        )

        sessionTrackerRef.set(sessionTracker)

        sessionTracker.initialize()

        sessionTracker.untrackAllSessions()

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackingStarted(sessionTracker, session1, state1)
            verify(listener).onSessionTrackingStarted(sessionTracker, session2, state2)
            verify(storage).deleteAllSessionRecords()
            verify(listener).onAllSessionsTrackingStopped(sessionTracker, listOf(session1 to state1, session2 to state2))
        }

        verifyNoMoreInteractions(storage, listener)

        verify(logger).e(
            SessionTracker.TAG,
            "untrackAllSessions: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        assertTrue(sessionTracker.getSessions().isEmpty())
    }

    private fun createSessionStateTransitionsSupplier() =
        object : ISessionStateTransitionsSupplier<Session, Event, State> {
            override fun getStateTransitions(session: Session): List<Transition<Event, State>> {
                return listOf(
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
        }

    private fun createStorageMock(
        sessions: List<SessionRecord<Session, State>>
    ): ISessionTrackerStorage<Session, State> = mock {
        on { readAllSessionRecords() } doReturn Collections.unmodifiableList(sessions)
    }
}