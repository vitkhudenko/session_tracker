package com.chumarin.stanislav.sample_app_dagger.di;

import com.chumarin.stanislav.sample_app_dagger.SplashActivity;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
abstract class ActivityModule {

    @ContributesAndroidInjector
    abstract SplashActivity splashActivity();

}
