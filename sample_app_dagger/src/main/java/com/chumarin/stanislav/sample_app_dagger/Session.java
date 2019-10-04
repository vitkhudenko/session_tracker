package com.chumarin.stanislav.sample_app_dagger;

import androidx.annotation.NonNull;


import org.jetbrains.annotations.NotNull;

import vit.khudenko.android.sessiontracker.ISession;

public class Session implements ISession {

    @NonNull
    private final String sessionId;

    public Session(@NonNull String sessionId) {
        this.sessionId = sessionId;
    }

    @NotNull
    @Override
    public String getSessionId() {
        return sessionId;
    }

    public enum State {
        INACTIVE,
        ACTIVE,
        FORGOTTEN
    }

    public enum Event {
        LOGIN,
        LOGOUT,
        LOGOUT_AND_FORGET
    }
}
