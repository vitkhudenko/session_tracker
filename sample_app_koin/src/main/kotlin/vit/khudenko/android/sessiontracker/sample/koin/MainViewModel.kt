package vit.khudenko.android.sessiontracker.sample.koin

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import vit.khudenko.android.sessiontracker.SessionId
import vit.khudenko.android.sessiontracker.SessionTracker
import vit.khudenko.android.sessiontracker.sample.koin.util.BaseViewModel
import java.util.concurrent.TimeUnit

class MainViewModel(
    private val sessionTracker: SessionTracker<Session.Event, Session.State>
) : BaseViewModel() {

    private val state: BehaviorSubject<State> = BehaviorSubject.createDefault(
        State.Idle
    )

    private var disposable: Disposable? = null

    fun onLogOutButtonClicked(sessionId: SessionId) {
        state.onNext(State.Progress)

        disposable = Completable.fromAction {
            sessionTracker.consumeEvent(sessionId, Session.Event.LOGOUT)
        }
            .subscribeOn(Schedulers.io())
            .delay(2, TimeUnit.SECONDS, Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                state.onNext(State.Success)
            }
    }

    fun onLogOutAndForgetButtonClicked(sessionId: SessionId) {
        state.onNext(State.Progress)

        disposable = Completable.fromAction {
            sessionTracker.consumeEvent(sessionId, Session.Event.LOGOUT_AND_FORGET)
        }
            .subscribeOn(Schedulers.io())
            .delay(2, TimeUnit.SECONDS, Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                state.onNext(State.Success)
            }
    }

    fun observeState(): Observable<State> = state.hide()

    override fun onCleared() {
        super.onCleared()
        disposable?.dispose()
        disposable = null
    }

    sealed class State {
        object Idle : State()
        object Progress : State()
        object Success : State()
    }
}