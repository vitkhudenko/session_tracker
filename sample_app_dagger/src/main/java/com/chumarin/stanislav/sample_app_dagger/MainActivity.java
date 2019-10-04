package com.chumarin.stanislav.sample_app_dagger;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.chumarin.stanislav.sample_app_dagger.di.ViewModels;

import java.util.Objects;

import dagger.Lazy;
import dagger.internal.DoubleCheck;
import io.reactivex.disposables.Disposable;

public class MainActivity extends AppCompatActivity {

    private static final String EXTRA_CURRENT_SESSION_ID = "extra_current_session_id";

    private Button logoutButton;
    private Button logoutAndForget;
    private View progressView;
    private TextView sessionIdView;

    @Nullable
    private Disposable disposable = null;

    private Lazy<MainViewModel> viewModel = DoubleCheck.lazy(() -> ViewModels.createFromSession(getSessionId(), this, MainViewModel.class));

    @NonNull
    public static Intent createIntent(@NonNull Context context, @NonNull String sessionId) {
        return new Intent(context, MainActivity.class)
                .putExtra(EXTRA_CURRENT_SESSION_ID, sessionId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logoutButton = findViewById(R.id.logout);
        logoutAndForget = findViewById(R.id.logout_and_forget);
        progressView = findViewById(R.id.loading);
        sessionIdView = findViewById(R.id.session_id);

        logoutButton.setOnClickListener(view ->
                viewModel.get().onLogOutButtonClicked()
        );
        logoutAndForget.setOnClickListener(view ->
                viewModel.get().onLogOutAndForgetButtonClicked()
        );

        sessionIdView.setText(getSessionId());
    }

    @NonNull
    private String getSessionId() {
        return Objects.requireNonNull(getIntent().getStringExtra(EXTRA_CURRENT_SESSION_ID));
    }

    @Override
    protected void onStart() {
        super.onStart();
        disposable = viewModel.get().observeState()
                .subscribe(state -> {
                    if (state instanceof MainViewModel.State.Idle) {
                        logoutButton.setEnabled(true);
                        progressView.setVisibility(View.GONE);
                    } else if (state instanceof MainViewModel.State.Progress) {
                        logoutButton.setEnabled(false);
                        progressView.setVisibility(View.VISIBLE);
                    } else if (state instanceof MainViewModel.State.Success) {
                        Intent intent = new Intent(
                                this,
                                SplashActivity.class
                        ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
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
}
