package com.chumarin.stanislav.sample_app_dagger;

import android.app.Application;

import androidx.annotation.NonNull;

import com.chumarin.stanislav.sample_app_dagger.di.AppComponent;
import com.chumarin.stanislav.sample_app_dagger.di.DaggerAppComponent;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasAndroidInjector;

public class App extends Application implements HasAndroidInjector {
    private AppComponent appComponent;

    @Inject
    DispatchingAndroidInjector<Object> dispatchingAndroidInjector;

    private static App instance;

    public static App getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        appComponent = DaggerAppComponent.builder().application(this).build();
        appComponent.inject(this);
        appComponent.getSessionTracker().initialize();
    }

    @Override
    public AndroidInjector<Object> androidInjector() {
        return dispatchingAndroidInjector;
    }

    @NonNull
    public AppComponent getAppComponent() {
        return appComponent;
    }
}
