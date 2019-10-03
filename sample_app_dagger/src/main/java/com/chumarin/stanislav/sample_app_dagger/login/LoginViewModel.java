package com.chumarin.stanislav.sample_app_dagger.login;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chumarin.stanislav.sample_app_dagger.Session;
import com.chumarin.stanislav.sample_app_dagger.util.BaseViewModel;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import vit.khudenko.android.sessiontracker.SessionRecord;
import vit.khudenko.android.sessiontracker.SessionTracker;

public class LoginViewModel extends BaseViewModel {

    @NonNull
    private final SessionTracker<Session, Session.Event, Session.State> sessionTracker;
    @NonNull
    private BehaviorSubject<State> state = BehaviorSubject.createDefault(State.Idle.INSTANCE);

    @Nullable
    private Disposable disposable;

    @Inject
    public LoginViewModel(@NonNull SessionTracker<Session, Session.Event, Session.State> sessionTracker) {
        this.sessionTracker = sessionTracker;
    }

    public void onLoginButtonClicked(@NonNull String userId) {
        state.onNext(State.Progress.INSTANCE);

        disposable = Observable.fromCallable(() -> {
            synchronized (sessionTracker) {
                Optional<Session> optionalSession = sessionTracker.getSessions()
                        .stream()
                        .map(SessionRecord::getSession)
                        .filter(session -> session.getSessionId().equals(userId))
                        .findFirst();
                if (optionalSession.isPresent()) {
                    sessionTracker.consumeEvent(userId, Session.Event.LOGIN);
                } else {
                    sessionTracker.trackSession(new Session(userId), Session.State.ACTIVE);
                }
            }
            return userId;
        })
                .subscribeOn(Schedulers.io())
                .delay(2, TimeUnit.SECONDS, Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(loggedInUserId -> state.onNext(new State.Success(loggedInUserId)));
    }

    public Observable<State> observeState() {
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

        public static class Idle extends State {
            public static final Idle INSTANCE = new Idle();

            private Idle() {
            }
        }

        public static class Progress extends State {
            public static final Progress INSTANCE = new Progress();

            private Progress() {
            }
        }

        public static class Success extends State {
            @NonNull
            private final String userId;

            public Success(@NonNull String userId) {
                this.userId = userId;
            }

            @NonNull
            public String getUserId() {
                return userId;
            }
        }
    }
}
