package vit.khudenko.android.sessiontracker.sample.koin.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import vit.khudenko.android.sessiontracker.sample.koin.EXTRA_CURRENT_SESSION_ID
import vit.khudenko.android.sessiontracker.sample.koin.MainActivity
import vit.khudenko.android.sessiontracker.sample.koin.R

class LoginActivity : AppCompatActivity() {

    private lateinit var userIdView: EditText
    private lateinit var loginButton: Button
    private lateinit var progressView: View

    private val viewModel: LoginViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        userIdView = findViewById(R.id.user_id)
        loginButton = findViewById(R.id.login)
        progressView = findViewById(R.id.loading)

        loginButton.setOnClickListener {
            val userId = userIdView.text.toString().trim()
            if (userId.isNotBlank()) {
                viewModel.onLoginButtonClicked(userId)
                hideKeyboard(userIdView)
            }
        }

        lifecycleScope.launch {
            viewModel.stateFlow()
                .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .collect {
                    when (it) {
                        LoginViewModel.State.Idle -> {
                            loginButton.isEnabled = true
                            progressView.visibility= View.GONE
                        }
                        LoginViewModel.State.Progress -> {
                            loginButton.isEnabled = false
                            progressView.visibility= View.VISIBLE
                        }
                        is LoginViewModel.State.Success -> {
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            intent.putExtra(EXTRA_CURRENT_SESSION_ID, it.userId)
                            startActivity(intent)
                            finish()
                        }
                    }
                }
        }
    }

    private fun hideKeyboard(editText: EditText) {
        val inputMethodManager = editText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(editText.windowToken, 0)
    }
}
