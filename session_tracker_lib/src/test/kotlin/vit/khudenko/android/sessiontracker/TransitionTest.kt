package vit.khudenko.android.sessiontracker

import org.junit.Assert.assertSame
import org.junit.Test
import vit.khudenko.android.sessiontracker.test_util.Event
import vit.khudenko.android.sessiontracker.test_util.State

class TransitionTest {

    @Test
    fun getEvent() {
        val event = Event.LOGIN

        val transition = Transition(
            event,
            emptyList<State>()
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
}