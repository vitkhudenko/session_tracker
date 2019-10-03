package com.chumarin.stanislav.sample_app_dagger.login;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class LoginViewModelProvider {
    @ContributesAndroidInjector
    abstract LoginViewModel loginViewModel();
}
