package vit.khudenko.android.sessiontracker

import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.InOrder
import vit.khudenko.android.sessiontracker.test_util.Event
import vit.khudenko.android.sessiontracker.test_util.State
import vit.khudenko.android.sessiontracker.test_util.createSessionStateTransitionsSupplierMock
import java.util.concurrent.atomic.AtomicReference

class SessionTrackerStorageMisuseRelaxedModeTest {

    private val mode = SessionTracker.Mode.RELAXED
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
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )
        sessionTrackerRef.set(sessionTracker)
        sessionTracker.initialize()
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#consumeEvent() from consumeEvent()`() {
        val sessionRecord1 = SessionRecord("session_id_1", State.ACTIVE)
        val sessionRecord2 = SessionRecord("session_id_2", State.INACTIVE)
        val updatedSessionRecord1 = sessionRecord1.copy(state = State.INACTIVE)
        val sessionRecords = listOf(sessionRecord1, sessionRecord2)

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn sessionRecords
            on { updateSessionRecord(updatedSessionRecord1) } doAnswer {
                assertFalse(sessionTrackerRef.get().consumeEvent(sessionRecord2.sessionId, Event.LOGIN))
                Unit
            }
        }

        initSessionTracker(storage)

        assertTrue(sessionTracker.consumeEvent(sessionRecord1.sessionId, Event.LOGOUT))

        with(inOrder(storage, listener, logger)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackerInitialized(sessionTracker, listOf(sessionRecord1, sessionRecord2))
            verify(storage).updateSessionRecord(updatedSessionRecord1)
            verifyStorageMisuseErrorLogged(logger, "consumeEvent")
            verify(listener).onSessionStateChanged(sessionTracker, updatedSessionRecord1, sessionRecord1.state)
        }

        verifyNoMoreInteractions(storage, listener, logger)

        assertEquals(listOf(updatedSessionRecord1, sessionRecord2), sessionTracker.getSessionRecords())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#trackSession() from consumeEvent()`() {
        val sessionRecord = SessionRecord("session_id_1", State.ACTIVE)

        val updatedSessionRecord = sessionRecord.copy(state = State.INACTIVE)

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn listOf(sessionRecord)
            on { updateSessionRecord(updatedSessionRecord) } doAnswer {
                sessionTrackerRef.get().trackSession("session_id_2", State.INACTIVE)
                Unit
            }
        }

        initSessionTracker(storage)

        assertTrue(sessionTracker.consumeEvent(sessionRecord.sessionId, Event.LOGOUT))

        with(inOrder(storage, listener, logger)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackerInitialized(sessionTracker, listOf(sessionRecord))
            verify(storage).updateSessionRecord(updatedSessionRecord)
            verifyStorageMisuseErrorLogged(logger, "trackSession")
            verify(listener).onSessionStateChanged(sessionTracker, updatedSessionRecord, sessionRecord.state)
        }

        verifyNoMoreInteractions(storage, listener, logger)

        assertEquals(listOf(updatedSessionRecord), sessionTracker.getSessionRecords())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackSession() from consumeEvent()`() {
        val sessionRecord = SessionRecord("session_id", State.ACTIVE)
        val updatedSessionRecord = sessionRecord.copy(state = State.INACTIVE)

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn listOf(sessionRecord)
            on { updateSessionRecord(updatedSessionRecord) } doAnswer {
                sessionTrackerRef.get().untrackSession(sessionRecord.sessionId)
                Unit
            }
        }

        initSessionTracker(storage)

        assertTrue(sessionTracker.consumeEvent(sessionRecord.sessionId, Event.LOGOUT))

        with(inOrder(storage, listener, logger)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackerInitialized(sessionTracker, listOf(sessionRecord))
            verify(storage).updateSessionRecord(updatedSessionRecord)
            verifyStorageMisuseErrorLogged(logger, "untrackSession")
            verify(listener).onSessionStateChanged(sessionTracker, updatedSessionRecord, sessionRecord.state)
        }

        verifyNoMoreInteractions(storage, listener, logger)

        assertEquals(listOf(updatedSessionRecord), sessionTracker.getSessionRecords())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackAllSessions() from consumeEvent()`() {
        val sessionRecord1 = SessionRecord("session_id_1", State.ACTIVE)
        val sessionRecord2 = SessionRecord("session_id_2", State.INACTIVE)

        val updatedSessionRecord1 = sessionRecord1.copy(state = State.INACTIVE)

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn listOf(sessionRecord1, sessionRecord2)
            on { updateSessionRecord(updatedSessionRecord1) } doAnswer {
                sessionTrackerRef.get().untrackAllSessions()
                Unit
            }
        }

        initSessionTracker(storage)

        assertTrue(sessionTracker.consumeEvent(sessionRecord1.sessionId, Event.LOGOUT))

        with(inOrder(storage, listener, logger)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackerInitialized(sessionTracker, listOf(sessionRecord1, sessionRecord2))
            verify(storage).updateSessionRecord(updatedSessionRecord1)
            verifyStorageMisuseErrorLogged(logger, "untrackAllSessions")
            verify(listener).onSessionStateChanged(sessionTracker, updatedSessionRecord1, sessionRecord1.state)
        }

        verifyNoMoreInteractions(storage, listener, logger)

        assertEquals(listOf(updatedSessionRecord1, sessionRecord2), sessionTracker.getSessionRecords())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#consumeEvent() from trackSession()`() {
        val sessionRecord = SessionRecord("session_id", State.ACTIVE)

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn emptyList()
            on { createSessionRecord(sessionRecord) } doAnswer {
                assertFalse(sessionTrackerRef.get().consumeEvent(sessionRecord.sessionId, Event.LOGOUT))
                Unit
            }
        }

        initSessionTracker(storage)
        assertTrue(sessionTracker.getSessionRecords().isEmpty())

        sessionTracker.trackSession(sessionRecord.sessionId, sessionRecord.state)

        with(inOrder(storage, listener, logger)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackerInitialized(sessionTracker, emptyList())
            verify(storage).createSessionRecord(sessionRecord)
            verifyStorageMisuseErrorLogged(logger, "consumeEvent")
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionRecord)
        }

        verifyNoMoreInteractions(storage, listener, logger)

        assertEquals(listOf(sessionRecord), sessionTracker.getSessionRecords())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#trackSession() from trackSession()`() {
        val sessionRecord = SessionRecord("session_id", State.ACTIVE)

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn emptyList()
            on { createSessionRecord(sessionRecord) } doAnswer {
                sessionTrackerRef.get().trackSession("session_id_2", State.INACTIVE)
                Unit
            }
        }

        initSessionTracker(storage)
        assertTrue(sessionTracker.getSessionRecords().isEmpty())

        sessionTracker.trackSession(sessionRecord.sessionId, sessionRecord.state)

        with(inOrder(storage, listener, logger)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackerInitialized(sessionTracker, emptyList())
            verify(storage).createSessionRecord(sessionRecord)
            verifyStorageMisuseErrorLogged(logger, "trackSession")
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionRecord)
        }

        verifyNoMoreInteractions(storage, listener, logger)

        assertEquals(listOf(sessionRecord), sessionTracker.getSessionRecords())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackSession() from trackSession()`() {
        val sessionRecord = SessionRecord("session_id", State.ACTIVE)

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn emptyList()
            on { createSessionRecord(sessionRecord) } doAnswer {
                sessionTrackerRef.get().untrackSession(sessionRecord.sessionId)
                Unit
            }
        }

        initSessionTracker(storage)
        assertTrue(sessionTracker.getSessionRecords().isEmpty())

        sessionTracker.trackSession(sessionRecord.sessionId, sessionRecord.state)

        with(inOrder(storage, listener, logger)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackerInitialized(sessionTracker, emptyList())
            verify(storage).createSessionRecord(sessionRecord)
            verifyStorageMisuseErrorLogged(logger, "untrackSession")
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionRecord)
        }

        verifyNoMoreInteractions(storage, listener, logger)

        assertEquals(listOf(sessionRecord), sessionTracker.getSessionRecords())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackAllSessions() from trackSession()`() {
        val sessionRecord = SessionRecord("session_id", State.ACTIVE)

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn emptyList()
            on { createSessionRecord(sessionRecord) } doAnswer {
                sessionTrackerRef.get().untrackAllSessions()
                Unit
            }
        }

        initSessionTracker(storage)
        assertTrue(sessionTracker.getSessionRecords().isEmpty())

        sessionTracker.trackSession(sessionRecord.sessionId, sessionRecord.state)

        with(inOrder(storage, listener, logger)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackerInitialized(sessionTracker, emptyList())
            verify(storage).createSessionRecord(sessionRecord)
            verifyStorageMisuseErrorLogged(logger, "untrackAllSessions")
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionRecord)
        }

        verifyNoMoreInteractions(storage, listener, logger)

        assertEquals(listOf(sessionRecord), sessionTracker.getSessionRecords())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#consumeEvent() from untrackSession()`() {
        val sessionRecord1 = SessionRecord("session_id_1", State.ACTIVE)
        val sessionRecord2 = SessionRecord("session_id_2", State.INACTIVE)

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn listOf(sessionRecord1, sessionRecord2)
            on { deleteSessionRecord(sessionRecord1.sessionId) } doAnswer {
                assertFalse(sessionTrackerRef.get().consumeEvent(sessionRecord2.sessionId, Event.LOGIN))
                Unit
            }
        }

        initSessionTracker(storage)

        sessionTracker.untrackSession(sessionRecord1.sessionId)

        with(inOrder(storage, listener, logger)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackerInitialized(sessionTracker, listOf(sessionRecord1, sessionRecord2))
            verify(storage).deleteSessionRecord(sessionRecord1.sessionId)
            verifyStorageMisuseErrorLogged(logger, "consumeEvent")
            verify(listener).onSessionTrackingStopped(sessionTracker, sessionRecord1)
        }

        verifyNoMoreInteractions(storage, listener, logger)

        assertEquals(listOf(sessionRecord2), sessionTracker.getSessionRecords())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#trackSession() from untrackSession()`() {
        val sessionRecord = SessionRecord("session_id_1", State.ACTIVE)

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn listOf(sessionRecord)
            on { deleteSessionRecord(sessionRecord.sessionId) } doAnswer {
                sessionTrackerRef.get().trackSession("session_id_2", State.INACTIVE)
                Unit
            }
        }

        initSessionTracker(storage)

        sessionTracker.untrackSession(sessionRecord.sessionId)

        with(inOrder(storage, listener, logger)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackerInitialized(sessionTracker, listOf(sessionRecord))
            verify(storage).deleteSessionRecord(sessionRecord.sessionId)
            verifyStorageMisuseErrorLogged(logger, "trackSession")
            verify(listener).onSessionTrackingStopped(sessionTracker, sessionRecord)
        }

        verifyNoMoreInteractions(storage, listener, logger)

        assertTrue(sessionTracker.getSessionRecords().isEmpty())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackSession() from untrackSession()`() {
        val sessionRecord1 = SessionRecord("session_id_1", State.ACTIVE)
        val sessionRecord2 = SessionRecord("session_id_2", State.INACTIVE)

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn listOf(sessionRecord1, sessionRecord2)
            on { deleteSessionRecord(sessionRecord1.sessionId) } doAnswer {
                sessionTrackerRef.get().untrackSession(sessionRecord2.sessionId)
                Unit
            }
        }

        initSessionTracker(storage)

        sessionTracker.untrackSession(sessionRecord1.sessionId)

        with(inOrder(storage, listener, logger)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackerInitialized(sessionTracker, listOf(sessionRecord1, sessionRecord2))
            verify(storage).deleteSessionRecord(sessionRecord1.sessionId)
            verifyStorageMisuseErrorLogged(logger, "untrackSession")
            verify(listener).onSessionTrackingStopped(sessionTracker, sessionRecord1)
        }

        verifyNoMoreInteractions(storage, listener, logger)

        assertEquals(listOf(sessionRecord2), sessionTracker.getSessionRecords())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackAllSessions() from untrackSession()`() {
        val sessionRecord1 = SessionRecord("session_id_1", State.ACTIVE)
        val sessionRecord2 = SessionRecord("session_id_2", State.INACTIVE)

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn listOf(sessionRecord1, sessionRecord2)
            on { deleteSessionRecord(sessionRecord1.sessionId) } doAnswer {
                sessionTrackerRef.get().untrackAllSessions()
                Unit
            }
        }

        initSessionTracker(storage)

        sessionTracker.untrackSession(sessionRecord1.sessionId)

        with(inOrder(storage, listener, logger)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackerInitialized(sessionTracker, listOf(sessionRecord1, sessionRecord2))
            verify(storage).deleteSessionRecord(sessionRecord1.sessionId)
            verifyStorageMisuseErrorLogged(logger, "untrackAllSessions")
            verify(listener).onSessionTrackingStopped(sessionTracker, sessionRecord1)
        }

        verifyNoMoreInteractions(storage, listener, logger)

        assertEquals(listOf(sessionRecord2), sessionTracker.getSessionRecords())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#consumeEvent() from untrackAllSessions()`() {
        val sessionRecord1 = SessionRecord("session_id_1", State.ACTIVE)
        val sessionRecord2 = SessionRecord("session_id_2", State.INACTIVE)

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn listOf(sessionRecord1, sessionRecord2)
            on { deleteAllSessionRecords() } doAnswer {
                assertFalse(sessionTrackerRef.get().consumeEvent(sessionRecord1.sessionId, Event.LOGOUT))
                Unit
            }
        }

        initSessionTracker(storage)

        sessionTracker.untrackAllSessions()

        with(inOrder(storage, listener, logger)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackerInitialized(sessionTracker, listOf(sessionRecord1, sessionRecord2))
            verify(storage).deleteAllSessionRecords()
            verifyStorageMisuseErrorLogged(logger, "consumeEvent")
            verify(listener).onAllSessionsTrackingStopped(sessionTracker, listOf(sessionRecord1, sessionRecord2))
        }

        verifyNoMoreInteractions(storage, listener, logger)

        assertTrue(sessionTracker.getSessionRecords().isEmpty())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#trackSession() from untrackAllSessions()`() {
        val sessionRecord1 = SessionRecord("session_id_1", State.ACTIVE)
        val sessionRecord2 = SessionRecord("session_id_2", State.INACTIVE)

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn listOf(sessionRecord1, sessionRecord2)
            on { deleteAllSessionRecords() } doAnswer {
                sessionTrackerRef.get().trackSession("session_id_3", State.ACTIVE)
                Unit
            }
        }

        initSessionTracker(storage)

        sessionTracker.untrackAllSessions()

        with(inOrder(storage, listener, logger)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackerInitialized(sessionTracker, listOf(sessionRecord1, sessionRecord2))
            verify(storage).deleteAllSessionRecords()
            verifyStorageMisuseErrorLogged(logger, "trackSession")
            verify(listener).onAllSessionsTrackingStopped(sessionTracker, listOf(sessionRecord1, sessionRecord2))
        }

        verifyNoMoreInteractions(storage, listener, logger)

        assertTrue(sessionTracker.getSessionRecords().isEmpty())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackSession() from untrackAllSessions()`() {
        val sessionRecord1 = SessionRecord("session_id_1", State.ACTIVE)
        val sessionRecord2 = SessionRecord("session_id_2", State.INACTIVE)

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn listOf(sessionRecord1, sessionRecord2)
            on { deleteAllSessionRecords() } doAnswer {
                sessionTrackerRef.get().untrackSession(sessionRecord2.sessionId)
                Unit
            }
        }

        initSessionTracker(storage)

        sessionTracker.untrackAllSessions()

        with(inOrder(storage, listener, logger)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackerInitialized(sessionTracker, listOf(sessionRecord1, sessionRecord2))
            verify(storage).deleteAllSessionRecords()
            verifyStorageMisuseErrorLogged(logger, "untrackSession")
            verify(listener).onAllSessionsTrackingStopped(sessionTracker, listOf(sessionRecord1, sessionRecord2))
        }

        verifyNoMoreInteractions(storage, listener, logger)

        assertTrue(sessionTracker.getSessionRecords().isEmpty())
    }

    @Test
    fun `storage implementation attempts to call SessionTracker#untrackAllSessions() from untrackAllSessions()`() {
        val sessionRecord1 = SessionRecord("session_id_1", State.ACTIVE)
        val sessionRecord2 = SessionRecord("session_id_2", State.INACTIVE)

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn listOf(sessionRecord1, sessionRecord2)
            on { deleteAllSessionRecords() } doAnswer {
                sessionTrackerRef.get().untrackAllSessions()
                Unit
            }
        }

        initSessionTracker(storage)

        sessionTracker.untrackAllSessions()

        with(inOrder(storage, listener, logger)) {
            verify(storage).readAllSessionRecords()
            verify(listener).onSessionTrackerInitialized(sessionTracker, listOf(sessionRecord1, sessionRecord2))
            verify(storage).deleteAllSessionRecords()
            verifyStorageMisuseErrorLogged(logger, "untrackAllSessions")
            verify(listener).onAllSessionsTrackingStopped(sessionTracker, listOf(sessionRecord1, sessionRecord2))
        }

        verifyNoMoreInteractions(storage, listener, logger)

        assertTrue(sessionTracker.getSessionRecords().isEmpty())
    }

    private fun InOrder.verifyStorageMisuseErrorLogged(logger: SessionTracker.Logger, methodName: String) {
        verify(logger).e(
            SessionTracker.TAG,
            "$methodName: misuse detected, accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )
    }
}