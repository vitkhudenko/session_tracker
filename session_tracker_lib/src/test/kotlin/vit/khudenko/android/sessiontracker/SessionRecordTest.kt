package vit.khudenko.android.sessiontracker

import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import vit.khudenko.android.sessiontracker.test_util.State

class SessionRecordTest {

    @Test
    fun sessionId() {
        val sessionId = SessionId("id")
        val sessionRecord = SessionRecord<State>(sessionId, mockk())

        assertEquals(sessionId, sessionRecord.sessionId())
    }
}
