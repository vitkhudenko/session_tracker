package vit.khudenko.android.sessiontracker.test_util

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.mockito.ArgumentMatcher
import vit.khudenko.android.sessiontracker.ISessionStateTransitionsSupplier
import vit.khudenko.android.sessiontracker.ISessionTrackerStorage
import vit.khudenko.android.sessiontracker.SessionRecord
import vit.khudenko.android.sessiontracker.SessionTracker
import vit.khudenko.android.sessiontracker.Transition
import java.util.Collections

fun createSessionStateTransitionsSupplierMock() = mock<ISessionStateTransitionsSupplier<Event, State>> {
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

fun createStorageMock(sessions: List<SessionRecord<State>>) = mock<ISessionTrackerStorage<State>> {
    on { readAllSessionRecords() } doReturn Collections.unmodifiableList(sessions)
}

fun verifyInitialization(
    sessionTracker: SessionTracker<Event, State>,
    sessionRecords: List<SessionRecord<State>>,
    logger: SessionTracker.Logger,
    storage: ISessionTrackerStorage<State>,
    listener: SessionTracker.Listener<Event, State>,
    mode: SessionTracker.Mode
) {
    sessionTracker.initialize()

    with(inOrder(storage, listener, logger)) {
        if (mode.verbose) {
            verify(logger).d(SessionTracker.TAG, "initialize: starting..")
        }
        verify(storage).readAllSessionRecords()
        sessionRecords.forEach { sessionRecord ->
            verify(listener).onSessionTrackingStarted(sessionTracker, sessionRecord)
        }
        if (mode.verbose) {
            verify(logger).d(eq(SessionTracker.TAG), argThat(matches("^initialize: done, took \\d+ ms$")))
        }
    }

    if (mode.verbose) {
        verifyNoMoreInteractions(listener, storage, logger)
    } else {
        verifyNoMoreInteractions(listener, storage)
        verifyZeroInteractions(logger)
    }

    reset(listener, storage, logger)

    assertEquals(sessionRecords, sessionTracker.getSessionRecords())

    if (mode.verbose) {
        val dump = sessionRecords.joinToString(
            prefix = "[",
            postfix = "]"
        ) { (sessionId, state) -> "{ '${sessionId}': $state }" }
        verify(logger).d(SessionTracker.TAG, "getSessionRecords: $dump")
        verifyNoMoreInteractions(logger)
    } else {
        verifyZeroInteractions(listener, storage, logger)
    }

    reset(listener, storage, logger)
}

fun <T : Throwable> assertThrows(
    expectedThrowable: Class<T>,
    expectedThrowableMessage: String,
    action: () -> Unit
): T {
    return assertThrows(expectedThrowable, equals(expectedThrowableMessage), action)
}

fun <T : Throwable> assertThrows(
    expectedThrowable: Class<T>,
    expectedThrowableMessageMatcher: ArgumentMatcher<String>,
    action: () -> Unit
): T {
    val actualThrowable = Assert.assertThrows(null, expectedThrowable) { action.invoke() }
    assertTrue("unexpected throwable message", expectedThrowableMessageMatcher.matches(actualThrowable.message))
    return actualThrowable
}
