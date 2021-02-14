package vit.khudenko.android.sessiontracker.sample.koin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.disposables.Disposable
import org.koin.android.ext.android.getKoin
import org.koin.androidx.viewmodel.ViewModelOwner
import org.koin.androidx.viewmodel.scope.emptyState
import org.koin.androidx.viewmodel.scope.getViewModel
import vit.khudenko.android.sessiontracker.SessionId

class MainActivity : AppCompatActivity() {

    private lateinit var logoutButton: Button
    private lateinit var logoutAndForget: Button
    private lateinit var progressView: View
    private lateinit var sessionIdView: TextView

    private val viewModel: MainViewModel by lazy {
        getKoin().getScope(scopeId = getSessionId()).getViewModel(
            qualifier = null,
            state = emptyState(),
            owner = { ViewModelOwner.from(this, this) },
            parameters = null
        )
    }

    private var disposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        logoutButton = findViewById(R.id.logout)
        logoutAndForget = findViewById(R.id.logout_and_forget)
        progressView = findViewById(R.id.loading)
        sessionIdView = findViewById(R.id.session_id)

        logoutButton.setOnClickListener {
            viewModel.onLogOutButtonClicked(getSessionId())
        }
        logoutAndForget.setOnClickListener {
            viewModel.onLogOutAndForgetButtonClicked(getSessionId())
        }

        sessionIdView.text = getSessionId()

    }

    private fun getSessionId(): SessionId {
        return intent.getStringExtra(EXTRA_CURRENT_SESSION_ID)!!
    }

    override fun onStart() {
        super.onStart()
        disposable = viewModel.observeState()
            .subscribe {
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
                            this,
                            SplashActivity::class.java
                        ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                    }
                }
            }
    }

    override fun onStop() {
        super.onStop()
        disposable?.dispose()
        disposable = null
    }
}
