package com.chumarin.stanislav.sample_app_dagger.login;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.chumarin.stanislav.sample_app_dagger.MainActivity;
import com.chumarin.stanislav.sample_app_dagger.R;
import com.chumarin.stanislav.sample_app_dagger.di.ViewModels;

import dagger.Lazy;
import dagger.internal.DoubleCheck;
import io.reactivex.disposables.Disposable;

public class LoginActivity extends AppCompatActivity {

    private EditText userIdView;
    private Button loginButton;
    private View progressView;


    private Lazy<LoginViewModel> lazyViewModel = DoubleCheck.lazy(() -> ViewModels.create(this, LoginViewModel.class));

    private Disposable disposable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userIdView = findViewById(R.id.user_id);
        loginButton = findViewById(R.id.login);
        progressView = findViewById(R.id.loading);

        loginButton.setOnClickListener(view -> {
            String userId = userIdView.getText().toString().trim();
            if (!userId.isEmpty()) {
                lazyViewModel.get().onLoginButtonClicked(userId);
                hideKeyboard(userIdView);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        disposable = lazyViewModel.get().observeState()
                .subscribe(state -> {
                    if (state instanceof LoginViewModel.State.Idle) {
                        loginButton.setEnabled(true);
                        progressView.setVisibility(View.GONE);
                    } else if (state instanceof LoginViewModel.State.Progress) {
                        loginButton.setEnabled(false);
                        progressView.setVisibility(View.VISIBLE);
                    } else if (state instanceof LoginViewModel.State.Success) {
                        LoginViewModel.State.Success success = (LoginViewModel.State.Success) state;
                        startActivity(MainActivity.createIntent(this, success.getUserId()));
                        finish();
                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (disposable != null) {
            disposable.dispose();
            disposable = null;
        }
    }

    private void hideKeyboard(@NonNull View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

}
