package vit.khudenko.android.sessiontracker

import org.junit.Assert.assertSame
import org.junit.Test
import vit.khudenko.android.sessiontracker.test_util.Session

class TransitionTest {

    @Test
    fun getEvent() {
        val event = Session.Event.LOGIN

        val transition = Transition(
            event,
            emptyList<Session.State>()
        )

        assertSame(event, transition.event)
    }

    @Test
    fun getStatePath() {
        val statePath = listOf(Session.State.INACTIVE, Session.State.ACTIVE)

        val transition = Transition(
            Session.Event.LOGIN,
            statePath
        )

        assertSame(statePath, transition.statePath)
    }
}