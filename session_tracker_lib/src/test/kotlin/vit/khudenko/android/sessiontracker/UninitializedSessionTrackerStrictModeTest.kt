package vit.khudenko.android.sessiontracker

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import org.junit.Before
import org.junit.Test
import vit.khudenko.android.sessiontracker.test_util.Event
import vit.khudenko.android.sessiontracker.test_util.State
import vit.khudenko.android.sessiontracker.test_util.assertThrows

class UninitializedSessionTrackerStrictModeTest {

    private val mode = SessionTracker.Mode.STRICT

    private lateinit var logger: SessionTracker.Logger
    private lateinit var storage: ISessionTrackerStorage<State>
    private lateinit var listener: SessionTracker.Listener<Event, State>
    private lateinit var sessionStateTransitionsSupplier: ISessionStateTransitionsSupplier<Event, State>
    private lateinit var sessionTracker: SessionTracker<Event, State>

    @Before
    fun setUp() {
        logger = mock()
        listener = mock()
        storage = mock()
        sessionStateTransitionsSupplier = mock()

        sessionTracker = SessionTracker(
            sessionTrackerStorage = storage,
            listener = listener,
            sessionStateTransitionsSupplier = sessionStateTransitionsSupplier,
            autoUntrackStates = emptySet(),
            mode = mode,
            logger = logger
        )
    }

    @Test
    fun `consumeEvent() called with uninitialized sessionTracker`() {
        verify("consumeEvent") { sessionTracker.consumeEvent("session_id", Event.LOGIN) }
    }

    @Test
    fun `trackSession() called with uninitialized sessionTracker`() {
        verify("trackSession") { sessionTracker.trackSession("session_id", State.ACTIVE) }
    }

    @Test
    fun `untrackSession() called with uninitialized sessionTracker`() {
        verify("untrackSession") { sessionTracker.untrackSession("session_id") }
    }

    @Test
    fun `untrackAllSessions() called with uninitialized sessionTracker`() {
        verify("untrackAllSessions") { sessionTracker.untrackAllSessions() }
    }

    @Test
    fun `getSessionRecords() called with uninitialized sessionTracker`() {
        verify("getSessionRecords") { sessionTracker.getSessionRecords() }
    }

    private fun verify(
        methodName: String,
        sessionTrackerAction: () -> Unit
    ) {
        assertThrows(RuntimeException::class.java, "SessionTracker must be initialized before calling its #$methodName method") {
            sessionTrackerAction.invoke()
        }
        verifyZeroInteractions(storage, listener, sessionStateTransitionsSupplier)
    }
}