package vit.khudenko.android.sessiontracker

import org.junit.Assert.assertSame
import org.junit.Test
import vit.khudenko.android.sessiontracker.test_util.State

class SessionRecordTest {

    @Test
    fun getSession() {
        val sessionId = "session_id"

        val sessionRecord = SessionRecord(
            sessionId,
            State.INACTIVE
        )

        assertSame(sessionId, sessionRecord.sessionId)
    }

    @Test
    fun getState() {
        val state = State.INACTIVE

        val sessionRecord = SessionRecord(
            "session_id",
            state
        )

        assertSame(state, sessionRecord.state)
    }
}