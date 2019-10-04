package com.chumarin.stanislav.sample_app_dagger.di;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.chumarin.stanislav.sample_app_dagger.App;

public class ViewModels {
    private ViewModels() {
    }

    @NonNull
    public static <VM extends ViewModel> VM create(@NonNull ViewModelStoreOwner viewModelStoreOwner,
                                                   @NonNull Class<VM> clazz) {
        return new ViewModelProvider(
                viewModelStoreOwner,
                App.getInstance().getAppComponent().getViewModelFactory()
        )
                .get(clazz);
    }

    @NonNull
    public static <VM extends ViewModel> VM createFromSession(
            @NonNull String sessionId,
            @NonNull ViewModelStoreOwner viewModelStoreOwner,
            @NonNull Class<VM> clazz) {
        return new ViewModelProvider(
                viewModelStoreOwner,
                new ViewModelFactory(App.getInstance().getAppComponent().getSessionTrackerListener().getUserComponent(sessionId).vmCreators())
        )
                .get(clazz);
    }
}
