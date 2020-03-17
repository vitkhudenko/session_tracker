package vit.khudenko.android.sessiontracker

import org.junit.Assert.assertSame
import org.junit.Test
import vit.khudenko.android.sessiontracker.test_util.Event
import vit.khudenko.android.sessiontracker.test_util.State
import vit.khudenko.android.sessiontracker.test_util.assertThrows

class TransitionTest {

    @Test
    fun getEvent() {
        val event = Event.LOGIN

        val transition = Transition(
            event,
            listOf(State.INACTIVE, State.ACTIVE)
        )

        assertSame(event, transition.event)
    }

    @Test
    fun getStatePath() {
        val statePath = listOf(State.INACTIVE, State.ACTIVE)

        val transition = Transition(
            Event.LOGIN,
            statePath
        )

        assertSame(statePath, transition.statePath)
    }

    @Test
    fun `construction fails if state path is empty`() {
        val statePath = emptyList<State>()

        assertThrows(IllegalArgumentException::class.java, "statePath must contain at least 2 items") {
            Transition(Event.LOGIN, statePath)
        }
    }

    @Test
    fun `construction fails if state path has 1 item`() {
        val statePath = listOf(State.INACTIVE)

        assertThrows(IllegalArgumentException::class.java, "statePath must contain at least 2 items") {
            Transition(Event.LOGIN, statePath)
        }
    }

    @Test
    fun `construction fails if state path has repeating items in a row`() {
        val statePath = listOf(State.INACTIVE, State.INACTIVE)

        assertThrows(IllegalArgumentException::class.java, "statePath must not have repeating items in a row") {
            Transition(Event.LOGIN, statePath)
        }
    }
}