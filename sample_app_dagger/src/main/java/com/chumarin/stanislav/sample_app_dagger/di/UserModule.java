package com.chumarin.stanislav.sample_app_dagger.di;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

import com.chumarin.stanislav.sample_app_dagger.MainViewModel;
import com.chumarin.stanislav.sample_app_dagger.Session;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import vit.khudenko.android.sessiontracker.SessionTracker;

@Module
public class UserModule {

    public static final String SESSION_ID = "session_id";
    @NonNull
    private final String sessionId;

    public UserModule(@NonNull String sessionId) {
        this.sessionId = sessionId;
    }

    @Provides
    @Named(SESSION_ID)
    public String sessionId() {
        return sessionId;
    }

    @Provides
    @IntoMap
    @ViewModelKey(MainViewModel.class)
    public ViewModel provideMainViewModel(@Named(UserModule.SESSION_ID) String sessionId,
                                          SessionTracker<Session, Session.Event, Session.State> sessionTracker) {
        return new MainViewModel(sessionId, sessionTracker);
    }

}
