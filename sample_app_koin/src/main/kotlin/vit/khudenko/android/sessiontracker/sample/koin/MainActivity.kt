package vit.khudenko.android.sessiontracker.sample.koin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.android.ext.android.getKoin
import org.koin.androidx.viewmodel.ViewModelOwner
import org.koin.androidx.viewmodel.scope.emptyState
import org.koin.androidx.viewmodel.scope.getViewModel
import vit.khudenko.android.sessiontracker.SessionId

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by lazy {
        getKoin().getScope(scopeId = getSessionId()).getViewModel(
            qualifier = null,
            owner = { ViewModelOwner.from(this, this) },
            parameters = null
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val logoutButton = findViewById<Button>(R.id.logout)
        val logoutAndForget = findViewById<Button>(R.id.logout_and_forget)
        val progressView = findViewById<View>(R.id.loading)
        val sessionIdView = findViewById<TextView>(R.id.session_id)

        logoutButton.setOnClickListener {
            viewModel.onLogOutButtonClicked(getSessionId())
        }
        logoutAndForget.setOnClickListener {
            viewModel.onLogOutAndForgetButtonClicked(getSessionId())
        }

        sessionIdView.text = getSessionId()

        lifecycleScope.launch {
            viewModel.stateFlow()
                .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .collect {
                    when (it) {
                        MainViewModel.State.Idle -> {
                            logoutButton.isEnabled = true
                            progressView.visibility = View.GONE
                        }
                        MainViewModel.State.Progress -> {
                            logoutButton.isEnabled = false
                            progressView.visibility = View.VISIBLE
                        }
                        MainViewModel.State.Success -> {
                            val intent = Intent(
                                this@MainActivity,
                                SplashActivity::class.java
                            ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            startActivity(intent)
                        }
                    }
                }
        }
    }

    private fun getSessionId(): SessionId {
        return intent.getStringExtra(EXTRA_CURRENT_SESSION_ID)!!
    }
}
