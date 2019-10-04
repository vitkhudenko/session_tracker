package com.chumarin.stanislav.sample_app_dagger.di;

import androidx.lifecycle.ViewModel;

import java.util.Map;

import javax.inject.Provider;

import dagger.Subcomponent;

@Subcomponent(modules = UserModule.class)
public interface UserComponent {

    Map<Class<? extends ViewModel>, Provider<ViewModel>> vmCreators();

}
