package vit.khudenko.android.sessiontracker.sample.koin.login

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import vit.khudenko.android.sessiontracker.SessionTracker
import vit.khudenko.android.sessiontracker.sample.koin.Session
import vit.khudenko.android.sessiontracker.sample.koin.util.BaseViewModel
import java.util.concurrent.TimeUnit

class LoginViewModel(
    private val sessionTracker: SessionTracker<Session.Event, Session.State>
) : BaseViewModel() {

    private val state: BehaviorSubject<State> = BehaviorSubject.createDefault(State.Idle)

    private var disposable: Disposable? = null

    fun onLoginButtonClicked(userId: String) {
        state.onNext(State.Progress)

        disposable = Observable.fromCallable {
            synchronized(sessionTracker) {
                val targetSessionRecord = sessionTracker.getSessionRecords()
                    .firstOrNull { record -> record.sessionId == userId }
                if (targetSessionRecord == null) {
                    sessionTracker.trackSession(userId, Session.State.ACTIVE)
                } else {
                    sessionTracker.consumeEvent(userId, Session.Event.LOGIN)
                }
            }
            userId
        }
            .subscribeOn(Schedulers.io())
            .delay(2, TimeUnit.SECONDS, Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                state.onNext(State.Success(it))
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
        class Success(val userId: String) : State()
    }
}
