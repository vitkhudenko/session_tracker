package vit.khudenko.android.sessiontracker.sample.koin

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import vit.khudenko.android.sessiontracker.ISessionTrackerStorage
import vit.khudenko.android.sessiontracker.SessionRecord

class SessionTrackerStorage(appContext: Application) : ISessionTrackerStorage<Session.State> {
    private val prefs: SharedPreferences = appContext.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)

    companion object {
        const val PREFS_FILENAME = "session_tracker_storage"
        private const val KEY_SESSION_RECORDS = "session_records"
        private const val KEY_SESSION_ID = "id"
        private const val KEY_SESSION_STATE = "state"

        private fun sessionRecordToJson(sessionRecord: SessionRecord<Session.State>): JSONObject {
            return JSONObject(
                mapOf(
                    KEY_SESSION_ID to sessionRecord.sessionId,
                    KEY_SESSION_STATE to sessionRecord.state.ordinal
                )
            )
        }

        private fun jsonToSessionRecord(json: JSONObject): SessionRecord<Session.State> {
            return SessionRecord(
                json.getString(KEY_SESSION_ID),
                Session.State.values()[json.getInt(KEY_SESSION_STATE)]
            )
        }
    }

    override fun readAllSessionRecords(): List<SessionRecord<Session.State>> {
        val sessionRecords = mutableListOf<SessionRecord<Session.State>>()
        val jsonArray = JSONArray(prefs.getString(KEY_SESSION_RECORDS, "[]"))
        for (i in 0 until jsonArray.length()) {
            val sessionRecord = jsonToSessionRecord(jsonArray.getJSONObject(i))
            sessionRecords.add(sessionRecord)
        }
        return sessionRecords
    }

    override fun createSessionRecord(sessionRecord: SessionRecord<Session.State>) {
        saveSessionRecords(
            readAllSessionRecords() + sessionRecord
        )
    }

    override fun updateSessionRecord(sessionRecord: SessionRecord<Session.State>) {
        saveSessionRecords(
            readAllSessionRecords().map {
                if (it.sessionId == sessionRecord.sessionId) {
                    sessionRecord
                } else {
                    it
                }
            }
        )
    }

    override fun deleteSessionRecord(sessionId: String) {
        saveSessionRecords(
            readAllSessionRecords().filter { it.sessionId != sessionId }
        )
    }

    override fun deleteAllSessionRecords() {
        saveSessionRecords(emptyList())
    }

    @SuppressLint("ApplySharedPref")
    private fun saveSessionRecords(sessionRecords: List<SessionRecord<Session.State>>) {
        prefs.edit()
            .putString(
                KEY_SESSION_RECORDS,
                JSONArray(
                    sessionRecords.map { sessionRecordToJson(it) }
                ).toString()
            )
            .commit()
    }
}