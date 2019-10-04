package com.chumarin.stanislav.sample_app_dagger;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chumarin.stanislav.sample_app_dagger.di.UserComponent;
import com.chumarin.stanislav.sample_app_dagger.di.UserModule;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kotlin.Pair;
import vit.khudenko.android.sessiontracker.SessionTracker;

public class SessionTrackerListener implements SessionTracker.Listener<Session, Session.Event, Session.State> {

    private static final String TAG = SessionTrackerListener.class.getSimpleName();
    @NonNull
    private final App app;

    @NonNull
    private Map<String, UserComponent> sessionUserComponents = new HashMap<>();

    public SessionTrackerListener(@NonNull App app) {
        this.app = app;
    }

    private void closeDaggerScope(@NonNull String sessionId) {
        if (sessionUserComponents.remove(sessionId) != null) {
            Log.d(TAG, "closeDaggerScope: session ID = '" + sessionId + "'");
        } else {
            Log.d(TAG, "closeDaggerScope: no scope to close for session with ID '" + sessionId + "'");
        }
    }

    private void createDaggerScope(@NonNull String sessionId) {
        if (sessionUserComponents.get(sessionId) != null) {
            Log.w(TAG, "createDaggerScope: scope already exists for session with ID '" + sessionId + "'");
        } else {
            Log.d(TAG, "createDaggerScope: session ID = '" + sessionId + "'");
            sessionUserComponents.put(sessionId, app.getAppComponent().attachComponent(new UserModule(sessionId)));
        }
    }

    @Override
    public void onSessionTrackingStarted(@NotNull SessionTracker<Session, Session.Event, Session.State> sessionTracker,
                                         @NotNull Session session,
                                         @NotNull Session.State initState) {
        Log.d(TAG, "onSessionTrackingStarted: session ID = " + session.getSessionId() + ", initState = " + initState);
        if (initState == Session.State.ACTIVE) {
            createDaggerScope(session.getSessionId());
        }
    }

    @Override
    public void onSessionStateChanged(@NotNull SessionTracker<Session, Session.Event, Session.State> sessionTracker,
                                      @NotNull Session session,
                                      @NotNull Session.State oldState,
                                      @NotNull Session.State newState) {
        Log.d(TAG, "onSessionStateChanged: session ID = " + session.getSessionId() + ", states = (" + oldState + " -> " + newState + ")");
        switch (newState) {
            case ACTIVE:
                createDaggerScope(session.getSessionId());
                break;
            case INACTIVE:
            case FORGOTTEN:
                closeDaggerScope(session.getSessionId());
                break;
            default:
                throw new IllegalArgumentException("Unknown newState [" + newState + "]");
        }
    }

    @Override
    public void onSessionTrackingStopped(@NotNull SessionTracker<Session, Session.Event, Session.State> sessionTracker,
                                         @NotNull Session session,
                                         @NotNull Session.State state) {
        Log.d(TAG, "onSessionTrackingStopped: session ID = " + session.getSessionId() + ", state = " + state);
        closeDaggerScope(session.getSessionId());
    }

    @Nullable
    public UserComponent getUserComponent(String sessionId) {
        return sessionUserComponents.get(sessionId);
    }

    @Override
    public void onAllSessionsTrackingStopped(
            @NotNull SessionTracker<Session, Session.Event, Session.State> sessionTracker,
            @NotNull List<? extends Pair<? extends Session, ? extends Session.State>> sessionsData) {
        Log.d(TAG, "onAllSessionsTrackingStopped");
        for (Pair<? extends Session, ? extends Session.State> sessionAndState : sessionsData) {
            closeDaggerScope(sessionAndState.getFirst().getSessionId());
        }
    }
}
