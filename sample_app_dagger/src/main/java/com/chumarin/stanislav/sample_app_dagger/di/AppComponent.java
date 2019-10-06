package com.chumarin.stanislav.sample_app_dagger.di;

import androidx.lifecycle.ViewModelProvider;
import com.chumarin.stanislav.sample_app_dagger.App;
import com.chumarin.stanislav.sample_app_dagger.Session;
import com.chumarin.stanislav.sample_app_dagger.SessionTrackerListener;
import dagger.BindsInstance;
import dagger.Component;
import dagger.android.AndroidInjectionModule;
import dagger.android.AndroidInjector;
import vit.khudenko.android.sessiontracker.SessionTracker;

import javax.inject.Singleton;

@Singleton
@Component(modules = {AppModule.class, AndroidInjectionModule.class, ViewModelModule.class, ActivityModule.class})
public interface AppComponent extends AndroidInjector<App> {
    UserComponent attachComponent(UserModule userModule);

    @Singleton
    ViewModelProvider.Factory getViewModelFactory();

    @Singleton
    SessionTracker<Session.Event, Session.State> getSessionTracker();

    @Singleton
    SessionTrackerListener getSessionTrackerListener();

    @Component.Builder
    interface Builder {
        @BindsInstance
        Builder application(App application);

        AppComponent build();
    }

    void inject(App app);
}
