package vit.khudenko.android.sessiontracker.sample.koin

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import org.koin.android.ext.android.inject
import vit.khudenko.android.sessiontracker.SessionTracker
import vit.khudenko.android.sessiontracker.sample.koin.login.LoginActivity

class SplashActivity : AppCompatActivity() {

    private val sessionTracker: SessionTracker<Session, Session.Event, Session.State> by inject()

    private val handler: Handler = Handler()

    private val action = Runnable {
        val activeSessions = sessionTracker.getSessions()
            .filter { (_, state) ->
                state == Session.State.ACTIVE
            }.map { (session, _) ->
                session
            }
        val currentSessionId = if (activeSessions.isEmpty()) {
            null
        } else {
            activeSessions.first().sessionId
        }
        if (currentSessionId == null) {
            startActivity(Intent(this, LoginActivity::class.java))
        } else {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra(EXTRA_CURRENT_SESSION_ID, currentSessionId)
            startActivity(intent)
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
    }

    override fun onResume() {
        super.onResume()
        handler.postDelayed(action, 500)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(action)
    }
}
