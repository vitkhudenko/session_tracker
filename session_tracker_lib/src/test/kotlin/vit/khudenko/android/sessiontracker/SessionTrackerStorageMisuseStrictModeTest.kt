package vit.khudenko.android.sessiontracker

import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import vit.khudenko.android.sessiontracker.test_util.Event
import vit.khudenko.android.sessiontracker.test_util.State
import vit.khudenko.android.sessiontracker.test_util.assertThrows
import vit.khudenko.android.sessiontracker.test_util.createSessionStateTransitionsSupplierMock
import java.util.concurrent.atomic.AtomicReference

class SessionTrackerStorageMisuseStrictModeTest {

    private val mode = SessionTracker.Mode.STRICT

    private lateinit var logger: SessionTracker.Logger
    private lateinit var listener: SessionTracker.Listener<Event, State>
    private lateinit var sessionStateTransitionsSupplier: ISessionStateTransitionsSupplier<Event, State>
    private lateinit var sessionTrackerRef: AtomicReference<SessionTracker<Event, State>>
    private lateinit var sessionTracker: SessionTracker<Event, State>

    @Before
    fun setUp() {
        logger = mock()
        listener = mock()
        sessionStateTransitionsSupplier = createSessionStateTransitionsSupplierMock()
        sessionTrackerRef = AtomicReference()
    }

    private fun initSessionTracker(storage: ISessionTrackerStorage<State>) {
        sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )
        sessionTrackerRef.set(sessionTracker)
        sessionTracker.initialize(listener)
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#consumeEvent() from consumeEvent()`() {
        val sessionRecord1 = SessionRecord(SessionId("session_id_1"), State.ACTIVE)
        val sessionRecord2 = SessionRecord(SessionId("session_id_2"), State.INACTIVE)

        val updatedSessionRecord1 = sessionRecord1.copy(state = State.INACTIVE)

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn listOf(sessionRecord1, sessionRecord2)
            on { updateSessionRecord(updatedSessionRecord1) } doAnswer {
                sessionTrackerRef.get().consumeEvent(sessionRecord2.sessionId, Event.LOGIN)
                Unit
            }
        }

        initSessionTracker(storage)

        assertThrows(
            RuntimeException::class.java,
            "consumeEvent: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        ) {
            sessionTracker.consumeEvent(sessionRecord1.sessionId, Event.LOGOUT)
        }

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackerInitialized(sessionTracker, listOf(sessionRecord1, sessionRecord2))
            verify(storage).updateSessionRecord(updatedSessionRecord1)
        }

        verifyNoMoreInteractions(storage, listener)

        assertEquals(listOf(updatedSessionRecord1, sessionRecord2), sessionTracker.getSessionRecords())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#trackSession() from consumeEvent()`() {
        val sessionRecord = SessionRecord(SessionId("session_id_1"), State.ACTIVE)
        val updatedSessionRecord = sessionRecord.copy(state = State.INACTIVE)

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn listOf(sessionRecord)
            on { updateSessionRecord(updatedSessionRecord) } doAnswer {
                sessionTrackerRef.get().trackSession(SessionId("session_id_2"), State.INACTIVE)
                Unit
            }
        }

        initSessionTracker(storage)

        assertThrows(
            RuntimeException::class.java,
            "trackSession: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        ) {
            sessionTracker.consumeEvent(sessionRecord.sessionId, Event.LOGOUT)
        }

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackerInitialized(sessionTracker, listOf(sessionRecord))
            verify(storage).updateSessionRecord(updatedSessionRecord)
        }

        verifyNoMoreInteractions(storage, listener)

        assertEquals(listOf(updatedSessionRecord), sessionTracker.getSessionRecords())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackSession() from consumeEvent()`() {
        val sessionRecord = SessionRecord(SessionId("session_id"), State.ACTIVE)
        val updatedSessionRecord = sessionRecord.copy(state = State.INACTIVE)

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn listOf(sessionRecord)
            on { updateSessionRecord(updatedSessionRecord) } doAnswer {
                sessionTrackerRef.get().untrackSession(sessionRecord.sessionId)
                Unit
            }
        }

        initSessionTracker(storage)

        assertThrows(
            RuntimeException::class.java,
            "untrackSession: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        ) {
            sessionTracker.consumeEvent(sessionRecord.sessionId, Event.LOGOUT)
        }

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackerInitialized(sessionTracker, listOf(sessionRecord))
            verify(storage).updateSessionRecord(updatedSessionRecord)
        }

        verifyNoMoreInteractions(storage, listener)

        assertEquals(listOf(updatedSessionRecord), sessionTracker.getSessionRecords())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackAllSessions() from consumeEvent()`() {
        val sessionRecord1 = SessionRecord(SessionId("session_id_1"), State.ACTIVE)
        val sessionRecord2 = SessionRecord(SessionId("session_id_2"), State.INACTIVE)
        val updatedSessionRecord1 = sessionRecord1.copy(state = State.INACTIVE)

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn listOf(sessionRecord1, sessionRecord2)
            on { updateSessionRecord(updatedSessionRecord1) } doAnswer {
                sessionTrackerRef.get().untrackAllSessions()
                Unit
            }
        }

        initSessionTracker(storage)

        assertThrows(
            RuntimeException::class.java,
            "untrackAllSessions: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        ) {
            sessionTracker.consumeEvent(sessionRecord1.sessionId, Event.LOGOUT)
        }

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackerInitialized(sessionTracker, listOf(sessionRecord1, sessionRecord2))
            verify(storage).updateSessionRecord(updatedSessionRecord1)
        }

        verifyNoMoreInteractions(storage, listener)

        assertEquals(listOf(updatedSessionRecord1, sessionRecord2), sessionTracker.getSessionRecords())

    }

    @Test
    fun `storage implementation attempts to call SessionTracker#consumeEvent() from trackSession()`() {
        val sessionId = SessionId("session_id")
        val state = State.ACTIVE

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn emptyList()
            on { createSessionRecord(SessionRecord(sessionId, state)) } doAnswer {
                sessionTrackerRef.get().consumeEvent(sessionId, Event.LOGOUT)
                Unit
            }
        }

        initSessionTracker(storage)
        assertTrue(sessionTracker.getSessionRecords().isEmpty())

        assertThrows(
            RuntimeException::class.java,
            "consumeEvent: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        ) {
            sessionTracker.trackSession(sessionId, state)
        }

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackerInitialized(sessionTracker, emptyList())
            verify(storage).createSessionRecord(SessionRecord(sessionId, state))
        }

        verifyNoMoreInteractions(storage, listener)

        assertTrue(sessionTracker.getSessionRecords().isEmpty())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#trackSession() from trackSession()`() {
        val sessionId = SessionId("session_id")
        val state = State.ACTIVE

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn emptyList()
            on { createSessionRecord(SessionRecord(sessionId, state)) } doAnswer {
                sessionTrackerRef.get().trackSession(SessionId("session_id_2"), State.INACTIVE)
                Unit
            }
        }

        initSessionTracker(storage)
        assertTrue(sessionTracker.getSessionRecords().isEmpty())

        assertThrows(
            RuntimeException::class.java,
            "trackSession: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        ) {
            sessionTracker.trackSession(sessionId, state)
        }

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackerInitialized(sessionTracker, emptyList())
            verify(storage).createSessionRecord(SessionRecord(sessionId, state))
        }

        verifyNoMoreInteractions(storage, listener)

        assertTrue(sessionTracker.getSessionRecords().isEmpty())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackSession() from trackSession()`() {
        val sessionId = SessionId("session_id")
        val state = State.ACTIVE

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn emptyList()
            on { createSessionRecord(SessionRecord(sessionId, state)) } doAnswer {
                sessionTrackerRef.get().untrackSession(sessionId)
                Unit
            }
        }

        initSessionTracker(storage)
        assertTrue(sessionTracker.getSessionRecords().isEmpty())

        assertThrows(
            RuntimeException::class.java,
            "untrackSession: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        ) {
            sessionTracker.trackSession(sessionId, state)
        }

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackerInitialized(sessionTracker, emptyList())
            verify(storage).createSessionRecord(SessionRecord(sessionId, state))
        }

        verifyNoMoreInteractions(storage, listener)

        assertTrue(sessionTracker.getSessionRecords().isEmpty())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackAllSessions() from trackSession()`() {
        val sessionId = SessionId("session_id")
        val state = State.ACTIVE

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn emptyList()
            on { createSessionRecord(SessionRecord(sessionId, state)) } doAnswer {
                sessionTrackerRef.get().untrackAllSessions()
                Unit
            }
        }

        initSessionTracker(storage)
        assertTrue(sessionTracker.getSessionRecords().isEmpty())

        assertThrows(
            RuntimeException::class.java,
            "untrackAllSessions: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        ) {
            sessionTracker.trackSession(sessionId, state)
        }

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackerInitialized(sessionTracker, emptyList())
            verify(storage).createSessionRecord(SessionRecord(sessionId, state))
        }

        verifyNoMoreInteractions(storage, listener)

        assertTrue(sessionTracker.getSessionRecords().isEmpty())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#consumeEvent() from untrackSession()`() {
        val sessionRecord1 = SessionRecord(SessionId("session_id_1"), State.ACTIVE)
        val sessionRecord2 = SessionRecord(SessionId("session_id_2"), State.INACTIVE)

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn listOf(sessionRecord1, sessionRecord2)
            on { deleteSessionRecord(sessionRecord1.sessionId) } doAnswer {
                sessionTrackerRef.get().consumeEvent(sessionRecord2.sessionId, Event.LOGIN)
                Unit
            }
        }

        initSessionTracker(storage)

        assertThrows(
            RuntimeException::class.java,
            "consumeEvent: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        ) {
            sessionTracker.untrackSession(sessionRecord1.sessionId)
        }

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackerInitialized(sessionTracker, listOf(sessionRecord1, sessionRecord2))
            verify(storage).deleteSessionRecord(sessionRecord1.sessionId)
        }

        verifyNoMoreInteractions(storage, listener)

        assertEquals(listOf(sessionRecord1, sessionRecord2), sessionTracker.getSessionRecords())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#trackSession() from untrackSession()`() {
        val sessionRecord = SessionRecord(SessionId("session_id_1"), State.ACTIVE)

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn listOf(sessionRecord)
            on { deleteSessionRecord(sessionRecord.sessionId) } doAnswer {
                sessionTrackerRef.get().trackSession(SessionId("session_id_2"), State.INACTIVE)
                Unit
            }
        }

        initSessionTracker(storage)

        assertThrows(
            RuntimeException::class.java,
            "trackSession: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        ) {
            sessionTracker.untrackSession(sessionRecord.sessionId)
        }

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackerInitialized(sessionTracker, listOf(sessionRecord))
            verify(storage).deleteSessionRecord(sessionRecord.sessionId)
        }

        verifyNoMoreInteractions(storage, listener)

        assertEquals(listOf(sessionRecord), sessionTracker.getSessionRecords())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackSession() from untrackSession()`() {
        val sessionRecord1 = SessionRecord(SessionId("session_id_1"), State.ACTIVE)
        val sessionRecord2 = SessionRecord(SessionId("session_id_2"), State.INACTIVE)

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn listOf(sessionRecord1, sessionRecord2)
            on { deleteSessionRecord(sessionRecord1.sessionId) } doAnswer {
                sessionTrackerRef.get().untrackSession(sessionRecord2.sessionId)
                Unit
            }
        }

        initSessionTracker(storage)

        assertThrows(
            RuntimeException::class.java,
            "untrackSession: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        ) {
            sessionTracker.untrackSession(sessionRecord1.sessionId)
        }

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackerInitialized(sessionTracker, listOf(sessionRecord1, sessionRecord2))
            verify(storage).deleteSessionRecord(sessionRecord1.sessionId)
        }

        verifyNoMoreInteractions(storage, listener)

        assertEquals(listOf(sessionRecord1, sessionRecord2), sessionTracker.getSessionRecords())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackAllSessions() from untrackSession()`() {
        val sessionRecord1 = SessionRecord(SessionId("session_id_1"), State.ACTIVE)
        val sessionRecord2 = SessionRecord(SessionId("session_id_2"), State.INACTIVE)

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn listOf(sessionRecord1, sessionRecord2)
            on { deleteSessionRecord(sessionRecord1.sessionId) } doAnswer {
                sessionTrackerRef.get().untrackAllSessions()
                Unit
            }
        }

        initSessionTracker(storage)

        assertThrows(
            RuntimeException::class.java,
            "untrackAllSessions: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        ) {
            sessionTracker.untrackSession(sessionRecord1.sessionId)
        }

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackerInitialized(sessionTracker, listOf(sessionRecord1, sessionRecord2))
            verify(storage).deleteSessionRecord(sessionRecord1.sessionId)
        }

        verifyNoMoreInteractions(storage, listener)

        assertEquals(listOf(sessionRecord1, sessionRecord2), sessionTracker.getSessionRecords())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#consumeEvent() from untrackAllSessions()`() {
        val sessionRecord1 = SessionRecord(SessionId("session_id_1"), State.ACTIVE)
        val sessionRecord2 = SessionRecord(SessionId("session_id_2"), State.INACTIVE)

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn listOf(sessionRecord1, sessionRecord2)
            on { deleteAllSessionRecords() } doAnswer {
                sessionTrackerRef.get().consumeEvent(sessionRecord1.sessionId, Event.LOGOUT)
                Unit
            }
        }

        initSessionTracker(storage)

        assertThrows(
            RuntimeException::class.java,
            "consumeEvent: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        ) {
            sessionTracker.untrackAllSessions()
        }

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackerInitialized(sessionTracker, listOf(sessionRecord1, sessionRecord2))
            verify(storage).deleteAllSessionRecords()
        }

        verifyNoMoreInteractions(storage, listener)

        assertEquals(listOf(sessionRecord1, sessionRecord2), sessionTracker.getSessionRecords())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#trackSession() from untrackAllSessions()`() {
        val sessionRecord1 = SessionRecord(SessionId("session_id_1"), State.ACTIVE)
        val sessionRecord2 = SessionRecord(SessionId("session_id_2"), State.INACTIVE)

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn listOf(sessionRecord1, sessionRecord2)
            on { deleteAllSessionRecords() } doAnswer {
                sessionTrackerRef.get().trackSession(SessionId("session_id_3"), State.ACTIVE)
                Unit
            }
        }

        initSessionTracker(storage)

        assertThrows(
            RuntimeException::class.java,
            "trackSession: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        ) {
            sessionTracker.untrackAllSessions()
        }

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackerInitialized(sessionTracker, listOf(sessionRecord1, sessionRecord2))
            verify(storage).deleteAllSessionRecords()
        }

        verifyNoMoreInteractions(storage, listener)

        assertEquals(listOf(sessionRecord1, sessionRecord2), sessionTracker.getSessionRecords())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackSession() from untrackAllSessions()`() {
        val sessionRecord1 = SessionRecord(SessionId("session_id_1"), State.ACTIVE)
        val sessionRecord2 = SessionRecord(SessionId("session_id_2"), State.INACTIVE)

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn listOf(sessionRecord1, sessionRecord2)
            on { deleteAllSessionRecords() } doAnswer {
                sessionTrackerRef.get().untrackSession(sessionRecord2.sessionId)
                Unit
            }
        }

        initSessionTracker(storage)

        assertThrows(
            RuntimeException::class.java,
            "untrackSession: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        ) {
            sessionTracker.untrackAllSessions()
        }

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackerInitialized(sessionTracker, listOf(sessionRecord1, sessionRecord2))
            verify(storage).deleteAllSessionRecords()
        }

        verifyNoMoreInteractions(storage, listener)

        assertEquals(listOf(sessionRecord1, sessionRecord2), sessionTracker.getSessionRecords())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackAllSessions() from untrackAllSessions()`() {
        val sessionRecord1 = SessionRecord(SessionId("session_id_1"), State.ACTIVE)
        val sessionRecord2 = SessionRecord(SessionId("session_id_2"), State.INACTIVE)

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn listOf(sessionRecord1, sessionRecord2)
            on { deleteAllSessionRecords() } doAnswer {
                sessionTrackerRef.get().untrackAllSessions()
                Unit
            }
        }

        initSessionTracker(storage)

        assertThrows(
            RuntimeException::class.java,
            "untrackAllSessions: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        ) {
            sessionTracker.untrackAllSessions()
        }

        with(inOrder(storage, listener)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackerInitialized(sessionTracker, listOf(sessionRecord1, sessionRecord2))
            verify(storage).deleteAllSessionRecords()
        }

        verifyNoMoreInteractions(storage, listener)

        assertEquals(listOf(sessionRecord1, sessionRecord2), sessionTracker.getSessionRecords())
    }
}