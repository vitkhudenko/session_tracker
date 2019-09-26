package vit.khudenko.android.sessiontracker

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import vit.khudenko.android.sessiontracker.test_util.Session
import vit.khudenko.android.sessiontracker.test_util.Session.Event
import vit.khudenko.android.sessiontracker.test_util.Session.State
import java.util.Collections

class SessionTrackerStrictModeTest {

    @get:Rule
    val rule: MockitoRule = MockitoJUnit.rule()

    @get:Rule
    val expectedExceptionRule: ExpectedException = ExpectedException.none()

    private val mode = SessionTracker.Mode.STRICT

    @Test
    fun `consumeEvent() called with uninitialized sessionStore`() {
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage("SessionTracker must be initialized before calling its #consumeEvent method")

        val storage = mock<ISessionTrackerStorage<Session, State>>()
        val listener = mock<SessionTracker.Listener<Session, State>>()
        val sessionStateTransitionsSupplier = mock<ISessionStateTransitionsSupplier<Session, Event, State>>()

        val sessionStore = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        sessionStore.consumeEvent("session_id", Event.LOGIN)

        verifyZeroInteractions(storage, listener, sessionStateTransitionsSupplier)
    }

    @Test
    fun `trackSession() called with uninitialized sessionStore`() {
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage("SessionTracker must be initialized before calling its #trackSession method")

        val storage = mock<ISessionTrackerStorage<Session, State>>()
        val listener = mock<SessionTracker.Listener<Session, State>>()
        val sessionStateTransitionsSupplier = mock<ISessionStateTransitionsSupplier<Session, Event, State>>()

        val sessionStore = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        sessionStore.trackSession(mock(), State.ACTIVE)

        verifyZeroInteractions(storage, listener, sessionStateTransitionsSupplier)
    }

    @Test
    fun `untrackSession() called with uninitialized sessionStore`() {
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage("SessionTracker must be initialized before calling its #untrackSession method")

        val storage = mock<ISessionTrackerStorage<Session, State>>()
        val listener = mock<SessionTracker.Listener<Session, State>>()
        val sessionStateTransitionsSupplier = mock<ISessionStateTransitionsSupplier<Session, Event, State>>()

        val sessionStore = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        sessionStore.untrackSession("session_id")

        verifyZeroInteractions(storage, listener, sessionStateTransitionsSupplier)
    }

    @Test
    fun `untrackAllSessions() called with uninitialized sessionStore`() {
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage("SessionTracker must be initialized before calling its #untrackAllSessions method")

        val storage = mock<ISessionTrackerStorage<Session, State>>()
        val listener = mock<SessionTracker.Listener<Session, State>>()
        val sessionStateTransitionsSupplier = mock<ISessionStateTransitionsSupplier<Session, Event, State>>()

        val sessionStore = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        sessionStore.untrackAllSessions()

        verifyZeroInteractions(storage, listener, sessionStateTransitionsSupplier)
    }

    @Test
    fun `getSessions() called with uninitialized sessionStore`() {
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage("SessionTracker must be initialized before calling its #getSessions method")

        val storage = mock<ISessionTrackerStorage<Session, State>>()
        val listener = mock<SessionTracker.Listener<Session, State>>()
        val sessionStateTransitionsSupplier = mock<ISessionStateTransitionsSupplier<Session, Event, State>>()

        val sessionStore = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        sessionStore.getSessions()

        verifyZeroInteractions(storage, listener, sessionStateTransitionsSupplier)
    }

    @Test
    fun initialization() {
        val session = Session("session_id")
        val state = State.ACTIVE

        val storage = createStorageMock(listOf(Pair(session, state)))
        val listener = mock<SessionTracker.Listener<Session, State>>()

        val sessionStore = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        sessionStore.initialize()

        with(inOrder(storage, listener)) {
            verify(storage).loadSessionsData()
            verify(listener).onSessionTrackingStarted(session, state)
        }

        verifyNoMoreInteractions(listener, storage)

        assertEquals(listOf(Pair(session, state)), sessionStore.getSessions())
    }

    @Test
    fun `initialization with session in a auto-untrack state`() {
        val session = Session("session_id")

        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage(
            "Unable to initialize SessionTracker: session with ID '${session.sessionId}' is in auto-untrack state (${State.FORGOTTEN})"
        )

        val storage = createStorageMock(listOf(Pair(session, State.FORGOTTEN)))
        val listener = mock<SessionTracker.Listener<Session, State>>()

        val sessionStore = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = setOf(State.FORGOTTEN),
            mode = mode,
            logger = mock()
        )

        sessionStore.initialize()

        verify(storage).loadSessionsData()
        verifyNoMoreInteractions(storage)
        verifyZeroInteractions(listener)
    }

    @Test
    fun `initialization without state transitions fails on state machine's builder validation`() {
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage(
            "Unable to initialize SessionTracker: error creating StateMachine"
        )

        val storage = createStorageMock(listOf(Pair(Session("session_id"), State.ACTIVE)))

        val listener = mock<SessionTracker.Listener<Session, State>>()
        val sessionStore = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = object : ISessionStateTransitionsSupplier<Session, Event, State> {
                override fun getStateTransitions(session: Session): List<Transition<Event, State>> {
                    return emptyList() // incomplete config
                }
            },
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        sessionStore.initialize()

        verify(storage).loadSessionsData()
        verifyNoMoreInteractions(storage)
        verifyZeroInteractions(listener)
    }

    @Test
    fun trackSession() {
        val storage = createStorageMock(emptyList())
        val listener = mock<SessionTracker.Listener<Session, State>>()

        val sessionStore = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        sessionStore.initialize()
        assertTrue(sessionStore.getSessions().isEmpty())

        val session = Session("session_id")
        val state = State.ACTIVE

        sessionStore.trackSession(session, state)

        with(inOrder(storage, listener)) {
            verify(storage).loadSessionsData()
            verify(listener).onSessionTrackingStarted(session, state)
            verify(storage).saveSessionsData(listOf(Pair(session, state)))
        }

        verifyNoMoreInteractions(storage, listener)

        assertEquals(listOf(Pair(session, state)), sessionStore.getSessions())
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
        val listener = mock<SessionTracker.Listener<Session, State>>()

        val sessionStore = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = setOf(state),
            mode = mode,
            logger = mock()
        )

        with(sessionStore) {
            initialize()
            trackSession(session, state)
        }

        verify(storage).loadSessionsData()
        verifyNoMoreInteractions(storage)
        verifyZeroInteractions(listener)

        assertTrue(sessionStore.getSessions().isEmpty())
    }

    @Test
    fun `trackSession() with already tracked session`() {
        val session1 = Session("session_id")
        val state1 = State.ACTIVE

        val storage = createStorageMock(listOf(Pair(session1, state1)))
        val logger = mock<SessionTracker.Logger>()
        val listener = mock<SessionTracker.Listener<Session, State>>()

        val sessionStore = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )

        val session2 = Session(session1.sessionId) // same session ID

        with(sessionStore) {
            initialize()
            trackSession(session2, State.INACTIVE)
        }

        with(inOrder(storage, listener, logger)) {
            verify(storage).loadSessionsData()
            verify(listener).onSessionTrackingStarted(session1, state1)
            verify(logger).w(SessionTracker.TAG, "trackSession: session with ID '${session1.sessionId}' already exists")
        }

        verifyNoMoreInteractions(storage, listener)

        assertEquals(listOf(Pair(session1, state1)), sessionStore.getSessions())
    }

    @Test
    fun untrackSession() {
        val session = Session("session_id")
        val state = State.ACTIVE

        val storage = createStorageMock(listOf(Pair(session, state)))
        val listener = mock<SessionTracker.Listener<Session, State>>()

        val sessionStore = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        with(sessionStore) {
            initialize()
            untrackSession(session.sessionId)
        }

        with(inOrder(storage, listener)) {
            verify(storage).loadSessionsData()
            verify(listener).onSessionTrackingStarted(session, state)
            verify(listener).onSessionTrackingStopped(session, state)
            verify(storage).saveSessionsData(emptyList())
        }

        verifyNoMoreInteractions(storage, listener)

        assertTrue(sessionStore.getSessions().isEmpty())
    }

    @Test
    fun untrackAllSessions() {
        val session1 = Session("session_id_1")
        val state1 = State.ACTIVE

        val session2 = Session("session_id_2")
        val state2 = State.INACTIVE

        val storage = createStorageMock(listOf(Pair(session1, state1), Pair(session2, state2)))
        val listener = mock<SessionTracker.Listener<Session, State>>()

        val sessionStore = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        with(sessionStore) {
            initialize()
            untrackAllSessions()
        }

        with(inOrder(storage, listener)) {
            verify(storage).loadSessionsData()
            verify(listener).onSessionTrackingStarted(session1, state1)
            verify(listener).onSessionTrackingStarted(session2, state2)
            verify(listener).onSessionTrackingStopped(session1, state1)
            verify(listener).onSessionTrackingStopped(session2, state2)
            verify(storage).saveSessionsData(emptyList())
        }

        verifyNoMoreInteractions(storage, listener)

        assertTrue(sessionStore.getSessions().isEmpty())
    }

    @Test
    fun consumeEvent() {
        val session1 = Session("session_id_1")
        val state1 = State.ACTIVE

        val session2 = Session("session_id_2")
        val state2 = State.INACTIVE

        val storage = createStorageMock(listOf(Pair(session1, state1), Pair(session2, state2)))
        val listener = mock<SessionTracker.Listener<Session, State>>()

        val sessionStore = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        with(sessionStore) {
            initialize()
            assertTrue(consumeEvent(session1.sessionId, Event.LOGOUT))
        }

        with(inOrder(storage, listener)) {
            verify(storage).loadSessionsData()
            verify(listener).onSessionTrackingStarted(session1, state1)
            verify(listener).onSessionTrackingStarted(session2, state2)
            verify(listener).onSessionStateChanged(session1, state1, State.INACTIVE)
            verify(storage).saveSessionsData(listOf(Pair(session1, State.INACTIVE), Pair(session2, state2)))
        }

        verifyNoMoreInteractions(storage, listener)

        assertEquals(listOf(Pair(session1, State.INACTIVE), Pair(session2, state2)), sessionStore.getSessions())
    }

    @Test
    fun `if event is ignored then listeners should not be notified and sessions state should not be persisted`() {
        val session = Session("session_id_1")
        val state = State.ACTIVE

        val storage = createStorageMock(listOf(Pair(session, state)))
        val listener = mock<SessionTracker.Listener<Session, State>>()

        val sessionStore = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = mock()
        )

        with(sessionStore) {
            initialize()
            // LOGIN event will be ignored, since current state is ACTIVE
            assertFalse(consumeEvent(session.sessionId, Event.LOGIN))
        }

        with(inOrder(storage, listener)) {
            verify(storage).loadSessionsData()
            verify(listener).onSessionTrackingStarted(session, state)
        }

        verifyNoMoreInteractions(storage, listener)

        assertEquals(listOf(Pair(session, state)), sessionStore.getSessions())
    }

    @Test
    fun `consumeEvent() called and session appears in a auto-untrack state`() {
        val session1 = Session("session_id_1")
        val state1 = State.ACTIVE

        val session2 = Session("session_id_2")
        val state2 = State.INACTIVE

        val storage = createStorageMock(listOf(Pair(session1, state1), Pair(session2, state2)))
        val listener = mock<SessionTracker.Listener<Session, State>>()

        val sessionStore = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = createSessionStateTransitionsSupplier(),
            autoUntrackStates = setOf(State.FORGOTTEN),
            mode = mode,
            logger = mock()
        )

        with(sessionStore) {
            initialize()
            assertTrue(consumeEvent(session1.sessionId, Event.LOGOUT_AND_FORGET))
        }

        with(inOrder(listener, storage)) {
            verify(storage).loadSessionsData()
            verify(listener).onSessionStateChanged(session1, state1, State.FORGOTTEN)
            verify(storage).saveSessionsData(listOf(Pair(session2, state2)))
        }

        assertEquals(listOf(Pair(session2, state2)), sessionStore.getSessions())
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

    private fun createStorageMock(sessions: List<Pair<Session, State>>): ISessionTrackerStorage<Session, State> = mock {
        on { loadSessionsData() } doReturn Collections.unmodifiableList(sessions)
    }
}