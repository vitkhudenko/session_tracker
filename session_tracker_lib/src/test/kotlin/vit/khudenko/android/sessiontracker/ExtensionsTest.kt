package vit.khudenko.android.sessiontracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import vit.khudenko.android.sessiontracker.test_util.Event
import vit.khudenko.android.sessiontracker.test_util.State

class ExtensionsTest {

    @Test
    fun `test sessionStateTransitionsSupplier extension function`() {
        val supplier = sessionStateTransitionsSupplier(
            Event.LOGIN to listOf(State.INACTIVE, State.ACTIVE),
            Event.LOGOUT to listOf(State.ACTIVE, State.INACTIVE),
            Event.LOGOUT_AND_FORGET to listOf(State.ACTIVE, State.FORGOTTEN)
        )

        val expected = listOf(
            Transition(Event.LOGIN, listOf(State.INACTIVE, State.ACTIVE)),
            Transition(Event.LOGOUT, listOf(State.ACTIVE, State.INACTIVE)),
            Transition(Event.LOGOUT_AND_FORGET, listOf(State.ACTIVE, State.FORGOTTEN))
        )

        val actual = supplier.getStateTransitions("session ID")

        assertEquals(expected.size, actual.size)

        expected.indices.forEach { index ->
            val expectedTransition = expected[index]
            val actualTransition = actual[index]
            assertSame(expectedTransition.event, actualTransition.event)
            assertEquals(expectedTransition.statePath, actualTransition.statePath)
        }
    }

    @Test
    fun `state path must remain immutable even if source list is modified`() {
        val statePath = listOf(State.INACTIVE, State.ACTIVE).toMutableList()

        val supplier = sessionStateTransitionsSupplier(Event.LOGIN to statePath)

        statePath.add(State.FORGOTTEN)

        val actual = supplier.getStateTransitions("session ID")

        assertEquals(listOf(State.INACTIVE, State.ACTIVE), actual.first().statePath)
    }
}