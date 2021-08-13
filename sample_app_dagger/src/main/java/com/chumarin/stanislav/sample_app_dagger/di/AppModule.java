package com.chumarin.stanislav.sample_app_dagger.di;

import android.content.Context;

import androidx.annotation.NonNull;

import com.chumarin.stanislav.sample_app_dagger.App;
import com.chumarin.stanislav.sample_app_dagger.Session;
import com.chumarin.stanislav.sample_app_dagger.SessionTrackerListener;

import java.util.Arrays;
import java.util.EnumSet;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import vit.khudenko.android.sessiontracker.ISessionStateTransitionsSupplier;
import vit.khudenko.android.sessiontracker.ISessionTrackerStorage;
import vit.khudenko.android.sessiontracker.SessionTracker;
import vit.khudenko.android.sessiontracker.Transition;

@Module
class AppModule {

    private static final String PREFS_FILENAME = "session_tracker_storage";

    @Provides
    @Singleton
    public SessionTracker<Session.Event, Session.State> provideSessionTracker(
            @NonNull ISessionTrackerStorage<Session.State> sessionStorage,
            @NonNull ISessionStateTransitionsSupplier<Session.Event, Session.State> stateTransitionsSupplier
    ) {
        return new SessionTracker<>(
                sessionStorage,
                stateTransitionsSupplier,
                EnumSet.of(Session.State.FORGOTTEN),
                SessionTracker.Mode.STRICT_VERBOSE,
                new SessionTracker.Logger.DefaultImpl(),
                "SessionTracker"
        );
    }

    @Provides
    @Singleton
    public ISessionStateTransitionsSupplier<Session.Event, Session.State> stateTransitionsSupplier() {
        return session -> Arrays.asList(
                new Transition<>(Session.Event.LOGIN, Arrays.asList(Session.State.INACTIVE, Session.State.ACTIVE)),
                new Transition<>(Session.Event.LOGOUT, Arrays.asList(Session.State.ACTIVE, Session.State.INACTIVE)),
                new Transition<>(Session.Event.LOGOUT_AND_FORGET, Arrays.asList(Session.State.ACTIVE, Session.State.FORGOTTEN))
        );
    }

    @Provides
    @Singleton
    public ISessionTrackerStorage<Session.State> sessionStorage(App app) {
        return new ISessionTrackerStorage.SharedPrefsImpl<>(
                app.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE),
                EnumSet.allOf(Session.State.class)
        );
    }

    @Provides
    @Singleton
    public SessionTrackerListener sessionStateListener(App app) {
        return new SessionTrackerListener(app);
    }
}
