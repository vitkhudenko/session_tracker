package vit.khudenko.android.sessiontracker

import com.nhaarman.mockitokotlin2.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import vit.khudenko.android.sessiontracker.test_util.Event
import vit.khudenko.android.sessiontracker.test_util.State
import vit.khudenko.android.sessiontracker.test_util.createSessionStateTransitionsSupplierMock
import java.util.concurrent.atomic.AtomicReference

class SessionTrackerStorageMisuseStrictModeTest {

    @get:Rule
    val expectedExceptionRule: ExpectedException = ExpectedException.none()

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
        expectedExceptionRule.expect(RuntimeException::class.java)
        expectedExceptionRule.expectMessage(
            "consumeEvent: misuse detected, " +
                    "accessing SessionTracker from ISessionTrackerStorage callbacks is not allowed"
        )

        val sessionRecord1 = SessionRecord("session_id_1", State.ACTIVE)
        val sessionRecord2 = SessionRecord("session_id_2", State.INACTIVE)

        val updatedSessionRecord1 = sessionRecord1.copy(state = State.INACTIVE)

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn listOf(sessionRecord1, sessionRecord2)
            on { updateSessionRecord(updatedSessionRecord1) } doAnswer {
                sessionTrackerRef.get().consumeEvent(sessionRecord2.sessionId, Event.LOGIN)
                Unit
            }
        }

        initSessionTracker(storage)

        try {
            sessionTracker.consumeEvent(sessionRecord1.sessionId, Event.LOGOUT)
        } catch (e: Exception) {
            with(inOrder(storage, listener)) {
                verify(storage).readAllSessionRecords()
                verify(listener).onSessionTrackingStarted(sessionTracker, sessionRecord1)
                verify(listener).onSessionTrackingStarted(sessionTracker, sessionRecord2)
                verify(storage).updateSessionRecord(updatedSessionRecord1)
            }

            verifyNoMoreInteractions(storage, listener)

            assertEquals(listOf(updatedSessionRecord1, sessionRecord2), sessionTracker.getSessionRecords())

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

        try {
            sessionTracker.consumeEvent(sessionRecord.sessionId, Event.LOGOUT)
        } catch (e: Exception) {
            with(inOrder(storage, listener)) {
                verify(storage).readAllSessionRecords()
                verify(listener).onSessionTrackingStarted(sessionTracker, sessionRecord)
                verify(storage).updateSessionRecord(updatedSessionRecord)
            }

            verifyNoMoreInteractions(storage, listener)

            assertEquals(listOf(updatedSessionRecord), sessionTracker.getSessionRecords())

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

        try {
            sessionTracker.consumeEvent(sessionRecord.sessionId, Event.LOGOUT)
        } catch (e: Exception) {
            with(inOrder(storage, listener)) {
                verify(storage).readAllSessionRecords()
                verify(listener).onSessionTrackingStarted(sessionTracker, sessionRecord)
                verify(storage).updateSessionRecord(updatedSessionRecord)
            }

            verifyNoMoreInteractions(storage, listener)

            assertEquals(listOf(updatedSessionRecord), sessionTracker.getSessionRecords())

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

        try {
            sessionTracker.consumeEvent(sessionRecord1.sessionId, Event.LOGOUT)
        } catch (e: Exception) {
            with(inOrder(storage, listener)) {
                verify(storage).readAllSessionRecords()
                verify(listener).onSessionTrackingStarted(sessionTracker, sessionRecord1)
                verify(listener).onSessionTrackingStarted(sessionTracker, sessionRecord2)
                verify(storage).updateSessionRecord(updatedSessionRecord1)
            }

            verifyNoMoreInteractions(storage, listener)

            assertEquals(listOf(updatedSessionRecord1, sessionRecord2), sessionTracker.getSessionRecords())

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

        val sessionId = "session_id"
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

        try {
            sessionTracker.trackSession(sessionId, state)
        } catch (e: Exception) {
            with(inOrder(storage, listener)) {
                verify(storage).readAllSessionRecords()
                verify(storage).createSessionRecord(SessionRecord(sessionId, state))
            }

            verifyNoMoreInteractions(storage, listener)

            assertTrue(sessionTracker.getSessionRecords().isEmpty())

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

        val sessionId = "session_id"
        val state = State.ACTIVE

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn emptyList()
            on { createSessionRecord(SessionRecord(sessionId, state)) } doAnswer {
                sessionTrackerRef.get().trackSession("session_id_2", State.INACTIVE)
                Unit
            }
        }

        initSessionTracker(storage)
        assertTrue(sessionTracker.getSessionRecords().isEmpty())

        try {
            sessionTracker.trackSession(sessionId, state)
        } catch (e: Exception) {
            with(inOrder(storage, listener)) {
                verify(storage).readAllSessionRecords()
                verify(storage).createSessionRecord(SessionRecord(sessionId, state))
            }

            verifyNoMoreInteractions(storage, listener)

            assertTrue(sessionTracker.getSessionRecords().isEmpty())

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

        val sessionId = "session_id"
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

        try {
            sessionTracker.trackSession(sessionId, state)
        } catch (e: Exception) {
            with(inOrder(storage, listener)) {
                verify(storage).readAllSessionRecords()
                verify(storage).createSessionRecord(SessionRecord(sessionId, state))
            }

            verifyNoMoreInteractions(storage, listener)

            assertTrue(sessionTracker.getSessionRecords().isEmpty())

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

        val sessionId = "session_id"
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

        try {
            sessionTracker.trackSession(sessionId, state)
        } catch (e: Exception) {
            with(inOrder(storage, listener)) {
                verify(storage).readAllSessionRecords()
                verify(storage).createSessionRecord(SessionRecord(sessionId, state))
            }

            verifyNoMoreInteractions(storage, listener)

            assertTrue(sessionTracker.getSessionRecords().isEmpty())

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

        val sessionRecord1 = SessionRecord("session_id_1", State.ACTIVE)
        val sessionRecord2 = SessionRecord("session_id_2", State.INACTIVE)

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn listOf(sessionRecord1, sessionRecord2)
            on { deleteSessionRecord(sessionRecord1.sessionId) } doAnswer {
                sessionTrackerRef.get().consumeEvent(sessionRecord2.sessionId, Event.LOGIN)
                Unit
            }
        }

        initSessionTracker(storage)

        try {
            sessionTracker.untrackSession(sessionRecord1.sessionId)
        } catch (e: Exception) {
            with(inOrder(storage, listener)) {
                verify(storage).readAllSessionRecords()
                verify(listener).onSessionTrackingStarted(sessionTracker, sessionRecord1)
                verify(listener).onSessionTrackingStarted(sessionTracker, sessionRecord2)
                verify(storage).deleteSessionRecord(sessionRecord1.sessionId)
            }

            verifyNoMoreInteractions(storage, listener)

            assertEquals(listOf(sessionRecord1, sessionRecord2), sessionTracker.getSessionRecords())

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

        val sessionRecord = SessionRecord("session_id_1", State.ACTIVE)

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn listOf(sessionRecord)
            on { deleteSessionRecord(sessionRecord.sessionId) } doAnswer {
                sessionTrackerRef.get().trackSession("session_id_2", State.INACTIVE)
                Unit
            }
        }

        initSessionTracker(storage)

        try {
            sessionTracker.untrackSession(sessionRecord.sessionId)
        } catch (e: Exception) {
            with(inOrder(storage, listener)) {
                verify(storage).readAllSessionRecords()
                verify(listener).onSessionTrackingStarted(sessionTracker, sessionRecord)
                verify(storage).deleteSessionRecord(sessionRecord.sessionId)
            }

            verifyNoMoreInteractions(storage, listener)

            assertEquals(listOf(sessionRecord), sessionTracker.getSessionRecords())

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

        try {
            sessionTracker.untrackSession(sessionRecord1.sessionId)
        } catch (e: Exception) {
            with(inOrder(storage, listener)) {
                verify(storage).readAllSessionRecords()
                verify(listener).onSessionTrackingStarted(sessionTracker, sessionRecord1)
                verify(listener).onSessionTrackingStarted(sessionTracker, sessionRecord2)
                verify(storage).deleteSessionRecord(sessionRecord1.sessionId)
            }

            verifyNoMoreInteractions(storage, listener)

            assertEquals(listOf(sessionRecord1, sessionRecord2), sessionTracker.getSessionRecords())

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

        try {
            sessionTracker.untrackSession(sessionRecord1.sessionId)
        } catch (e: Exception) {
            with(inOrder(storage, listener)) {
                verify(storage).readAllSessionRecords()
                verify(listener).onSessionTrackingStarted(sessionTracker, sessionRecord1)
                verify(listener).onSessionTrackingStarted(sessionTracker, sessionRecord2)
                verify(storage).deleteSessionRecord(sessionRecord1.sessionId)
            }

            verifyNoMoreInteractions(storage, listener)

            assertEquals(listOf(sessionRecord1, sessionRecord2), sessionTracker.getSessionRecords())

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

        val sessionRecord1 = SessionRecord("session_id_1", State.ACTIVE)
        val sessionRecord2 = SessionRecord("session_id_2", State.INACTIVE)

        val storage = mock<ISessionTrackerStorage<State>> {
            on { readAllSessionRecords() } doReturn listOf(sessionRecord1, sessionRecord2)
            on { deleteAllSessionRecords() } doAnswer {
                sessionTrackerRef.get().consumeEvent(sessionRecord1.sessionId, Event.LOGOUT)
                Unit
            }
        }

        initSessionTracker(storage)

        try {
            sessionTracker.untrackAllSessions()
        } catch (e: Exception) {
            with(inOrder(storage, listener)) {
                verify(storage).readAllSessionRecords()
                verify(listener).onSessionTrackingStarted(sessionTracker, sessionRecord1)
                verify(listener).onSessionTrackingStarted(sessionTracker, sessionRecord2)
                verify(storage).deleteAllSessionRecords()
            }

            verifyNoMoreInteractions(storage, listener)

            assertEquals(listOf(sessionRecord1, sessionRecord2), sessionTracker.getSessionRecords())

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

        try {
            sessionTracker.untrackAllSessions()
        } catch (e: Exception) {
            with(inOrder(storage, listener)) {
                verify(storage).readAllSessionRecords()
                verify(listener).onSessionTrackingStarted(sessionTracker, sessionRecord1)
                verify(listener).onSessionTrackingStarted(sessionTracker, sessionRecord2)
                verify(storage).deleteAllSessionRecords()
            }

            verifyNoMoreInteractions(storage, listener)

            assertEquals(listOf(sessionRecord1, sessionRecord2), sessionTracker.getSessionRecords())

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

        try {
            sessionTracker.untrackAllSessions()
        } catch (e: Exception) {
            with(inOrder(storage, listener)) {
                verify(storage).readAllSessionRecords()
                verify(listener).onSessionTrackingStarted(sessionTracker, sessionRecord1)
                verify(listener).onSessionTrackingStarted(sessionTracker, sessionRecord2)
                verify(storage).deleteAllSessionRecords()
            }

            verifyNoMoreInteractions(storage, listener)

            assertEquals(listOf(sessionRecord1, sessionRecord2), sessionTracker.getSessionRecords())

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

        try {
            sessionTracker.untrackAllSessions()
        } catch (e: Exception) {
            with(inOrder(storage, listener)) {
                verify(storage).readAllSessionRecords()
                verify(listener).onSessionTrackingStarted(sessionTracker, sessionRecord1)
                verify(listener).onSessionTrackingStarted(sessionTracker, sessionRecord2)
                verify(storage).deleteAllSessionRecords()
            }

            verifyNoMoreInteractions(storage, listener)

            assertEquals(listOf(sessionRecord1, sessionRecord2), sessionTracker.getSessionRecords())

            throw e
        }
    }
}