package com.chumarin.stanislav.sample_app_dagger;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.chumarin.stanislav.sample_app_dagger.login.LoginActivity;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import vit.khudenko.android.sessiontracker.SessionRecord;
import vit.khudenko.android.sessiontracker.SessionTracker;

public class SplashActivity extends AppCompatActivity {

    @Inject
    SessionTracker<Session, Session.Event, Session.State> sessionTracker;

    private Handler handler = new Handler();

    private Runnable action = () -> {
        List<Session> activeSessions = sessionTracker.getSessions().stream()
                .filter(stateSessionRecord -> stateSessionRecord.getState() == Session.State.ACTIVE)
                .map(SessionRecord::getSession)
                .collect(Collectors.toList());

        String currentSessionId = activeSessions.isEmpty() ? null : activeSessions.get(0).getSessionId();

        if (currentSessionId == null) {
            startActivity(new Intent(this, LoginActivity.class));
        } else {
            Intent intent = MainActivity.createIntent(this, currentSessionId);
            startActivity(intent);
        }
        finish();
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.postDelayed(action, 500);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(action);
    }
}
