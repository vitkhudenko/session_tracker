package com.chumarin.stanislav.sample_app_dagger;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chumarin.stanislav.sample_app_dagger.di.UserComponent;
import com.chumarin.stanislav.sample_app_dagger.di.UserModule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import vit.khudenko.android.sessiontracker.SessionRecord;
import vit.khudenko.android.sessiontracker.SessionTracker;

public class SessionTrackerListener implements SessionTracker.Listener<Session.Event, Session.State> {

    private static final String TAG = SessionTrackerListener.class.getSimpleName();
    @NonNull
    private final App app;

    @NonNull
    private final Map<String, UserComponent> sessionUserComponents = new HashMap<>();

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
    public void onSessionTrackerInitialized(@NotNull SessionTracker<Session.Event, Session.State> sessionTracker,
                                            @NotNull List<SessionRecord<Session.State>> sessionRecords) {
        Log.d(TAG, "onSessionTrackerInitialized");
        if (sessionRecords.stream().filter(sessionRecord -> sessionRecord.getState() == Session.State.ACTIVE).count() > 1) {
            throw new IllegalStateException("One active session is allowed at most");
        }
        for (SessionRecord<Session.State> sessionRecord : sessionRecords) {
            if (sessionRecord.getState() == Session.State.ACTIVE) {
                createDaggerScope(sessionRecord.sessionId());
            }
        }
    }

    @Override
    public void onSessionTrackingStarted(@NotNull SessionTracker<Session.Event, Session.State> sessionTracker,
                                         @NotNull SessionRecord<Session.State> sessionRecord) {
        String sessionId = sessionRecord.sessionId();
        Session.State state = sessionRecord.getState();
        Log.d(TAG, "onSessionTrackingStarted: session ID = " + sessionId + ", state = " + state);
        if (state == Session.State.ACTIVE) {
            createDaggerScope(sessionId);
        }
    }

    @Override
    public void onSessionStateChanged(@NotNull SessionTracker<Session.Event, Session.State> sessionTracker,
                                      @NotNull SessionRecord<Session.State> sessionRecord,
                                      @NotNull Session.State oldState) {
        String sessionId = sessionRecord.sessionId();
        Session.State newState = sessionRecord.getState();
        Log.d(TAG, "onSessionStateChanged: session ID = " + sessionId + ", states = (" + oldState + " -> " + newState + ")");
        switch (newState) {
            case ACTIVE:
                createDaggerScope(sessionId);
                break;
            case INACTIVE:
            case FORGOTTEN:
                closeDaggerScope(sessionId);
                break;
            default:
                throw new IllegalArgumentException("Unknown newState [" + newState + "]");
        }
    }

    @Override
    public void onSessionTrackingStopped(@NotNull SessionTracker<Session.Event, Session.State> sessionTracker,
                                         @NotNull SessionRecord<Session.State> sessionRecord) {
        String sessionId = sessionRecord.sessionId();
        Session.State state = sessionRecord.getState();
        Log.d(TAG, "onSessionTrackingStopped: session ID = " + sessionId + ", state = " + state);
        closeDaggerScope(sessionId);
    }

    @Nullable
    public UserComponent getUserComponent(String sessionId) {
        return sessionUserComponents.get(sessionId);
    }

    @Override
    public void onAllSessionsTrackingStopped(
            @NotNull SessionTracker<Session.Event, Session.State> sessionTracker,
            @NotNull List<SessionRecord<Session.State>> sessionRecords) {
        Log.d(TAG, "onAllSessionsTrackingStopped");
        for (SessionRecord<Session.State> sessionRecord : sessionRecords) {
            closeDaggerScope(sessionRecord.sessionId());
        }
    }
}
