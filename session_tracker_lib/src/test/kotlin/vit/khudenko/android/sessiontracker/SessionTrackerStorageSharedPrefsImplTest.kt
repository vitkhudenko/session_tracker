package vit.khudenko.android.sessiontracker

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import org.junit.Assert.assertEquals
import org.junit.Test
import vit.khudenko.android.sessiontracker.test_util.State
import java.util.EnumSet

class SessionTrackerStorageSharedPrefsImplTest {

    @Test
    fun `createSessionRecord - empty storage`() {
        val sessionRecordState = mockk<State> {
            every { ordinal } returns 123
        }
        val sessionRecord = mockk<SessionRecord<State>> {
            every { sessionId } returns "sessionId"
            every { state } returns sessionRecordState
        }

        val prefsEditor = mockk<SharedPreferences.Editor> {
            every { putString("session_records", any()) } returns this@mockk
            every { commit() } returns true
        }
        val prefs = mockk<SharedPreferences> {
            every { getString("session_records", "[]") } returns "[]"
            every { edit() } returns prefsEditor
        }
        val storage = ISessionTrackerStorage.SharedPrefsImpl<State>(
            prefs,
            EnumSet.allOf(State::class.java)
        )

        storage.createSessionRecord(sessionRecord)

        verifySequence {
            prefs.getString("session_records", "[]")
            prefs.edit()
            sessionRecord.sessionId
            sessionRecord.state
            sessionRecordState.ordinal
            prefsEditor.putString("session_records", "[{\"id\":\"sessionId\",\"state\":123}]")
            prefsEditor.commit()
        }
    }

    @Test
    fun `createSessionRecord - non-empty storage`() {
        val prefsEditor = mockk<SharedPreferences.Editor> {
            every { putString("session_records", any()) } returns this@mockk
            every { commit() } returns true
        }
        val prefs = mockk<SharedPreferences> {
            every { getString("session_records", "[]") } returns "[{\"id\":\"sessionId-1\",\"state\":1}]"
            every { edit() } returns prefsEditor
        }
        val storage = ISessionTrackerStorage.SharedPrefsImpl<State>(
            prefs,
            EnumSet.allOf(State::class.java)
        )

        val sessionRecordState = mockk<State> {
            every { ordinal } returns 123
        }
        val sessionRecord = mockk<SessionRecord<State>> {
            every { sessionId } returns "sessionId"
            every { state } returns sessionRecordState
        }

        storage.createSessionRecord(sessionRecord)

        verifySequence {
            prefs.getString("session_records", "[]")
            prefs.edit()
            sessionRecord.sessionId
            sessionRecord.state
            sessionRecordState.ordinal
            prefsEditor.putString(
                "session_records",
                "[{\"id\":\"sessionId-1\",\"state\":1},{\"id\":\"sessionId\",\"state\":123}]"
            )
            prefsEditor.commit()
        }
    }

    @Test
    fun `readAllSessionRecords - empty storage`() {
        val prefs = mockk<SharedPreferences> {
            every { getString("session_records", "[]") } returns "[]"
        }
        val storage = ISessionTrackerStorage.SharedPrefsImpl<State>(
            prefs,
            EnumSet.allOf(State::class.java)
        )

        assertEquals(emptyList<SessionRecord<State>>(), storage.readAllSessionRecords())

        verifySequence {
            prefs.getString("session_records", "[]")
        }
    }

    @Test
    fun `readAllSessionRecords - non-empty storage`() {
        val prefs = mockk<SharedPreferences> {
            every { getString("session_records", "[]") } returns "[{\"id\":\"sessionId-1\",\"state\":1}]"
        }
        val storage = ISessionTrackerStorage.SharedPrefsImpl<State>(
            prefs,
            EnumSet.allOf(State::class.java)
        )

        assertEquals(
            mutableListOf(
                SessionRecord("sessionId-1", State.values()[1])
            ),
            storage.readAllSessionRecords()
        )

        verifySequence {
            prefs.getString("session_records", "[]")
        }
    }

    @Test
    fun `updateSessionRecord - target record is present`() {
        val prefsEditor = mockk<SharedPreferences.Editor> {
            every { putString("session_records", any()) } returns this@mockk
            every { commit() } returns true
        }
        val prefs = mockk<SharedPreferences> {
            every { getString("session_records", "[]") } returns "[{\"id\":\"sessionId\",\"state\":0}]"
            every { edit() } returns prefsEditor
        }

        val storage = ISessionTrackerStorage.SharedPrefsImpl<State>(
            prefs,
            EnumSet.allOf(State::class.java)
        )

        val sessionRecordState = mockk<State> {
            every { ordinal } returns 1
        }
        val sessionRecord = mockk<SessionRecord<State>> {
            every { sessionId } returns "sessionId"
            every { state } returns sessionRecordState
        }

        storage.updateSessionRecord(sessionRecord)

        verifySequence {
            prefs.getString("session_records", "[]")
            sessionRecord.sessionId
            prefs.edit()
            sessionRecord.sessionId
            sessionRecord.state
            sessionRecordState.ordinal
            prefsEditor.putString("session_records", "[{\"id\":\"sessionId\",\"state\":1}]")
            prefsEditor.commit()
        }
    }

    @Test
    fun `updateSessionRecord - target record is absent`() {
        val prefsEditor = mockk<SharedPreferences.Editor> {
            every { putString("session_records", any()) } returns this@mockk
            every { commit() } returns true
        }
        val prefs = mockk<SharedPreferences> {
            every { getString("session_records", "[]") } returns "[{\"id\":\"sessionId-1\",\"state\":1}]"
            every { edit() } returns prefsEditor
        }

        val storage = ISessionTrackerStorage.SharedPrefsImpl<State>(
            prefs,
            EnumSet.allOf(State::class.java)
        )

        val sessionRecord = mockk<SessionRecord<State>> {
            every { sessionId } returns "sessionId-0"
        }

        storage.updateSessionRecord(sessionRecord)

        verifySequence {
            prefs.getString("session_records", "[]")
            sessionRecord.sessionId
            prefs.edit()
            prefsEditor.putString("session_records", "[{\"id\":\"sessionId-1\",\"state\":1}]")
            prefsEditor.commit()
        }
    }

    @Test
    fun `deleteSessionRecord - target record is present`() {
        val prefsEditor = mockk<SharedPreferences.Editor> {
            every { putString("session_records", any()) } returns this@mockk
            every { commit() } returns true
        }
        val prefs = mockk<SharedPreferences> {
            every {
                getString("session_records", "[]")
            } returns "[{\"id\":\"sessionId-0\",\"state\":0},{\"id\":\"sessionId-1\",\"state\":1}]"
            every { edit() } returns prefsEditor
        }

        val storage = ISessionTrackerStorage.SharedPrefsImpl<State>(
            prefs,
            EnumSet.allOf(State::class.java)
        )

        storage.deleteSessionRecord("sessionId-0")

        verifySequence {
            prefs.getString("session_records", "[]")
            prefs.edit()
            prefsEditor.putString("session_records", "[{\"id\":\"sessionId-1\",\"state\":1}]")
            prefsEditor.commit()
        }
    }

    @Test
    fun `deleteSessionRecord - target record is absent`() {
        val prefsEditor = mockk<SharedPreferences.Editor> {
            every { putString("session_records", any()) } returns this@mockk
            every { commit() } returns true
        }
        val prefs = mockk<SharedPreferences> {
            every { getString("session_records", "[]") } returns "[{\"id\":\"sessionId-1\",\"state\":1}]"
            every { edit() } returns prefsEditor
        }

        val storage = ISessionTrackerStorage.SharedPrefsImpl<State>(
            prefs,
            EnumSet.allOf(State::class.java)
        )

        storage.deleteSessionRecord("sessionId-0")

        verifySequence {
            prefs.getString("session_records", "[]")
            prefs.edit()
            prefsEditor.putString("session_records", "[{\"id\":\"sessionId-1\",\"state\":1}]")
            prefsEditor.commit()
        }
    }

    @Test
    fun deleteAllSessionRecords() {
        val prefsEditor = mockk<SharedPreferences.Editor> {
            every { putString("session_records", any()) } returns this@mockk
            every { commit() } returns true
        }
        val prefs = mockk<SharedPreferences> {
            every { edit() } returns prefsEditor
        }

        val storage = ISessionTrackerStorage.SharedPrefsImpl<State>(
            prefs,
            EnumSet.allOf(State::class.java)
        )

        storage.deleteAllSessionRecords()

        verifySequence {
            prefs.edit()
            prefsEditor.putString("session_records", "[]")
            prefsEditor.commit()
        }
    }
}
