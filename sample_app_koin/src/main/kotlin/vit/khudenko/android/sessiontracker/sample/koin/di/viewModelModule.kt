package vit.khudenko.android.sessiontracker.sample.koin.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import vit.khudenko.android.sessiontracker.sample.koin.MainViewModel
import vit.khudenko.android.sessiontracker.sample.koin.login.LoginViewModel

val viewModelModule = module {

    viewModel {
        LoginViewModel(get())
    }

    scope(scopeName = named(SCOPE_ID_USER_SESSION)) {
        viewModel {
            MainViewModel(get())
        }
    }
}