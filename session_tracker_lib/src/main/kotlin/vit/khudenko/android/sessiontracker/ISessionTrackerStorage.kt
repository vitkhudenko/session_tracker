package vit.khudenko.android.sessiontracker

import android.annotation.SuppressLint
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.EnumSet

/**
 * Your app must assume `ISessionTrackerStorage` methods may be called by [`SessionTracker`][SessionTracker]
 * while processing the calls made by your app to the `SessionTracker`.
 *
 * `SessionTracker` calls `ISessionTrackerStorage` synchronously from the threads your application calls
 * `SessionTracker` from.
 *
 * `SessionTracker` implementation guarantees that `ISessionTrackerStorage` methods are never called concurrently.
 */
interface ISessionTrackerStorage<State : Enum<State>> {

    /**
     * This method is called by `SessionTracker` from within the
     * [`SessionTracker.trackSession()`][SessionTracker.trackSession] call.
     *
     * The implementation must not defer actual persisting for future.
     *
     * @param sessionRecord [`SessionRecord`][SessionRecord]
     */
    fun createSessionRecord(sessionRecord: SessionRecord<State>)

    /**
     * This is called by `SessionTracker` from within the
     * [`SessionTracker.initialize()`][SessionTracker.initialize] call.
     *
     * The implementation should read and create previously persisted (if any) list of [`SessionRecord`][SessionRecord]
     * instances with corresponding states. If storage is empty, then an empty list should be returned.
     */
    fun readAllSessionRecords(): List<SessionRecord<State>>

    /**
     * This method is called by `SessionTracker` from within the
     * [`SessionTracker.consumeEvent()`][SessionTracker.consumeEvent] call.
     *
     * The implementation must not defer actual persisting for future.
     *
     * @param sessionRecord [`SessionRecord`][SessionRecord]
     */
    fun updateSessionRecord(sessionRecord: SessionRecord<State>)

    /**
     * This method is called by `SessionTracker` from within the
     * [`SessionTracker.untrackSession()`][SessionTracker.untrackSession] call.
     *
     * The implementation must not defer actual persisting for future.
     *
     * @param sessionId [`SessionId`][SessionId]
     */
    fun deleteSessionRecord(sessionId: SessionId)

    /**
     * This method is called by `SessionTracker` from within the
     * [`SessionTracker.untrackAllSessions()`][SessionTracker.untrackAllSessions] call.
     *
     * The implementation must not defer actual persisting for future.
     */
    fun deleteAllSessionRecords()

    class SharedPrefsImpl<State : Enum<State>>(
        private val prefs: SharedPreferences,
        stateEnumValues: EnumSet<State>,
    ) : ISessionTrackerStorage<State> {

        companion object {
            private const val KEY_SESSION_RECORDS = "session_records"
            private const val KEY_SESSION_ID = "id"
            private const val KEY_SESSION_STATE = "state"
        }

        private val stateEnumValuesList: List<State> = stateEnumValues.toList()

        override fun readAllSessionRecords(): List<SessionRecord<State>> {
            val sessionRecords = mutableListOf<SessionRecord<State>>()
            val jsonArray = JSONArray(prefs.getString(KEY_SESSION_RECORDS, "[]"))
            for (i in 0 until jsonArray.length()) {
                val sessionRecord = jsonToSessionRecord(jsonArray.getJSONObject(i))
                sessionRecords.add(sessionRecord)
            }
            return sessionRecords
        }

        override fun createSessionRecord(sessionRecord: SessionRecord<State>) {
            saveSessionRecords(
                readAllSessionRecords() + sessionRecord
            )
        }

        override fun updateSessionRecord(sessionRecord: SessionRecord<State>) {
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
        private fun saveSessionRecords(sessionRecords: List<SessionRecord<State>>) {
            prefs.edit()
                .putString(
                    KEY_SESSION_RECORDS,
                    JSONArray(
                        sessionRecords.map { sessionRecordToJson(it) }
                    ).toString()
                )
                .commit()
        }

        private fun sessionRecordToJson(sessionRecord: SessionRecord<State>): JSONObject {
            return JSONObject(
                mapOf(
                    KEY_SESSION_ID to sessionRecord.sessionId,
                    KEY_SESSION_STATE to sessionRecord.state.ordinal
                )
            )
        }

        private fun jsonToSessionRecord(json: JSONObject): SessionRecord<State> {
            return SessionRecord(
                json.getString(KEY_SESSION_ID),
                stateEnumValuesList[json.getInt(KEY_SESSION_STATE)]
            )
        }
    }
}
