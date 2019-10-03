package com.chumarin.stanislav.sample_app_dagger.login;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class LoginModule {
    @ContributesAndroidInjector(modules = LoginViewModelProvider.class)
    abstract LoginActivity contributesLoginActivity();
}
