package vit.khudenko.android.sessiontracker.sample.koin

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import vit.khudenko.android.sessiontracker.ISessionTrackerStorage

class SessionTrackerStorage(appContext: Application) : ISessionTrackerStorage<Session, Session.State> {

    private val prefs: SharedPreferences = appContext.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)

    companion object {
        const val PREFS_FILENAME = "sessions_storage"
        private const val KEY_SESSIONS = "sessions"
        private const val KEY_ID = "id"
        private const val KEY_STATE = "state"
    }

    @SuppressLint("ApplySharedPref")
    override fun saveSessionsData(sessionsData: List<Pair<Session, Session.State>>) {
        prefs.edit()
            .putString(
                KEY_SESSIONS,
                JSONArray(
                    sessionsData.map { (session, state) -> sessionToJson(session, state) }
                ).toString()
            )
            .commit()
    }

    override fun loadSessionsData(): List<Pair<Session, Session.State>> {
        val sessions = mutableListOf<Pair<Session, Session.State>>()
        val sessionsJsonArray = JSONArray(prefs.getString(KEY_SESSIONS, "[]"))
        for (i in 0 until sessionsJsonArray.length()) {
            val session = jsonToSession(sessionsJsonArray.getJSONObject(i))
            sessions.add(session)
        }
        return sessions
    }

    private fun sessionToJson(session: Session, sessionState: Session.State): JSONObject {
        return JSONObject(
            mapOf(
                KEY_ID to session.sessionId,
                KEY_STATE to sessionState.ordinal
            )
        )
    }

    private fun jsonToSession(json: JSONObject): Pair<Session, Session.State> {
        return Pair(
            Session(json.getString(KEY_ID)),
            Session.State.values()[json.getInt(KEY_STATE)]
        )
    }
}