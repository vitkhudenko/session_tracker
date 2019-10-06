package com.chumarin.stanislav.sample_app_dagger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.chumarin.stanislav.sample_app_dagger.util.BaseViewModel;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import vit.khudenko.android.sessiontracker.SessionTracker;

import java.util.concurrent.TimeUnit;

public class MainViewModel extends BaseViewModel {

    @NonNull
    private final String sessionId;
    @NonNull
    private final SessionTracker<Session.Event, Session.State> sessionTracker;

    @NonNull
    private final BehaviorSubject<State> state = BehaviorSubject.createDefault(State.Idle.INSTANCE);

    @Nullable
    private Disposable disposable;

    public MainViewModel(@NonNull String sessionId,
                         @NonNull SessionTracker<Session.Event, Session.State> sessionTracker) {
        this.sessionId = sessionId;
        this.sessionTracker = sessionTracker;
    }

    void onLogOutButtonClicked() {
        state.onNext(State.Progress.INSTANCE);

        disposable = Completable.fromAction(() -> sessionTracker.consumeEvent(sessionId, Session.Event.LOGOUT))
                .subscribeOn(Schedulers.io())
                .delay(2, TimeUnit.SECONDS, Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> state.onNext(State.Success.INSTANCE));
    }

    void onLogOutAndForgetButtonClicked() {
        state.onNext(State.Progress.INSTANCE);

        disposable = Completable.fromAction(() -> sessionTracker.consumeEvent(sessionId, Session.Event.LOGOUT_AND_FORGET))
                .subscribeOn(Schedulers.io())
                .delay(2, TimeUnit.SECONDS, Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> state.onNext(State.Success.INSTANCE));
    }

    Observable<State> observeState() {
        return state.hide();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (disposable != null) {
            disposable.dispose();
            disposable = null;
        }
    }

    public static abstract class State {
        private State() {
        }

        static class Idle extends State {
            static final Idle INSTANCE = new Idle();

            private Idle() {
            }
        }

        static class Progress extends State {
            static final Progress INSTANCE = new Progress();

            private Progress() {
            }
        }

        static class Success extends State {
            static final Success INSTANCE = new Success();

            private Success() {
            }
        }
    }
}
