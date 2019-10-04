package vit.khudenko.android.sessiontracker

import org.junit.Assert.assertSame
import org.junit.Test
import vit.khudenko.android.sessiontracker.test_util.Session

class SessionRecordTest {

    @Test
    fun getSession() {
        val session = Session("session_id")

        val sessionRecord = SessionRecord(
            session,
            Session.State.INACTIVE
        )

        assertSame(session, sessionRecord.session)
    }

    @Test
    fun getState() {
        val state = Session.State.INACTIVE

        val sessionRecord = SessionRecord(
            Session("session_id"),
            state
        )

        assertSame(state, sessionRecord.state)
    }
}