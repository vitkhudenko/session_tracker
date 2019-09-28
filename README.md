## TL;DR

SessionTracker is a general purpose framework to provide a foundation for session management in your app.
Your app provides (a) session storage implementation and (b) session state machine configuration while SessionTracker
provides callbacks to create/release session resources.

## Integration

At the project level `build.gradle`, add a maven repo pointing to `https://dl.bintray.com/vit-khudenko/libs`, e.g.:

```groovy
allprojects {
    repositories {
        google()
        jcenter()
        maven { url 'https://dl.bintray.com/vit-khudenko/libs' } // this is it
    }
}
```

At a module level `build.gradle`, add the following dependency:

```groovy
implementation 'vit.khudenko.android:sessiontracker:0.1.0'
```

## Contract description

### What is session?
Session is a flexible entity - it could be a user that logs in/out or a bluetooth device with all its possible
states. In SessionTracker framework, sessions are represented by `ISession` interface with the only
requirement to have a unique session ID. Actual implementation of ISession is up to your app.

### Persistence

SessionTracker framework supports automatic session initialization on application process restarts. Your
app must provide an implementation of `ISessionTrackerStorage`, which is used by
SessionTracker to persist/read sessions data.

### Session states and events

SessionTracker maintains a state machine per session. Your app must define a set of possible events and
states per session. Using events and states your app should provide state machine transitions, which are
used to configure session state machine. For example, your app may define the following session events and states:

```kotlin
class Session(override val sessionId: String) : ISession {
    enum class State {
        INACTIVE, ACTIVE
    }
    enum class Event {
        LOGIN, LOGOUT
    }
}
```

then a sample transitions config (an implementation of `ISessionStateTransitionsSupplier`) could be as following:

```kotlin
val sessionStateTransitionsSupplier =
        object : ISessionStateTransitionsSupplier<Session, Session.Event, Session.State> {
    override fun getStateTransitions(session: Session) = listOf(
        Transition(
            event = Session.Event.LOGIN,
            statePath = listOf(Session.State.INACTIVE, Session.State.ACTIVE)
        ),
        Transition(
            event = Session.Event.LOGOUT,
            statePath = listOf(Session.State.ACTIVE, Session.State.INACTIVE)
        )
    )
}
```

Such config would mean there are two possible session states (`ACTIVE`/`INACTIVE`) and two possible events: `LOGIN`
(to move session from `INACTIVE` to `ACTIVE` state) and `LOGOUT` (to move session from `ACTIVE` to `INACTIVE` state).

### Session tracking

In order to make SessionTracker ready to function it should be initialized first. The most appropriate place for
`initialize()` call is `android.app.Application.onCreate()`.

Suppose your user hits "Login" button, your app authenticates user and creates a Session object. In order to make
use of SessionTracker the session should be "attached" to SessionTracker:

```kotlin
sessionTracker.trackSession(session, Session.State.ACTIVE)
```

Now SessionTracker is tracking the session until your app calls `sessionTracker.untrackSession(sessionId)`.
Next time your app starts (and SessionTracker is initialized), the session will be automatically tracked by
SessionTracker with the same `ACTIVE` state.

As long as session is tracked, its state changes are propagated to your app via `SessionTracker.Listener`.

Suppose eventually your user hits "Log Out" button, then your app is responsible to communicate this event
to SessionTracker by asking to consume `LOGOUT` event for the session:

```kotlin
sessionTracker.consumeEvent(session.sessionId, Session.Event.LOGOUT)
```

Now SessionTracker updates session state to `INACTIVE`, propagates this state change via
`SessionTracker.Listener` and persists session with its new state . Note, the session is still tracked 
by SessionTracker, so next time your app starts, the session will be automatically picked up for
tracking by SessionTracker with the same `INACTIVE` state.

### Management of session resources

`SessionTracker.Listener` has useful for your app callbacks that allow to manage session resources appropriately:

- `onSessionTrackingStarted(sessionTracker: SessionTracker<S, Event, State>, session: S, initState: State)`

    SessionTracker has added session to the list of tracked sessions.
    This happens as a result of calling `SessionTracker.trackSession(session, state)` or `SessionTracker.initialize()`.
    This callback is the right place to create any resources for the session (a DB connection, a DI scope, etc.)
    depending on the initState.

- `onSessionStateChanged(sessionTracker: SessionTracker<S, Event, State>, session: S, oldState: State, newState: State)`

    Session state has changed.
    This happens as a result of calling `SessionTracker.consumeEvent(sessionId, event)`.
    This callback is the right place to create or release any resources for the session (a DB connection,
    a DI scope, etc.).

- `onSessionTrackingStopped(sessionTracker: SessionTracker<S, Event, State>, session: S, state: State)`

    SessionTracker has removed session from the list of tracked sessions. This happens as a result
    of calling `SessionTracker.untrackSession(sessionId)`.
    This may also happen as a result of calling `SessionTracker.consumeEvent` if session appears in one of 
    the `autoUntrackStates` (a `SessionTracker` constructor parameter).
    This callback is the right place to release any resources for the session (a DB connection, a DI scope, etc.).

- `onAllSessionsTrackingStopped(sessionTracker: SessionTracker<S, Event, State>, sessionsData: List<Pair<S, State>>)`

    SessionTracker has removed session from the list of tracked sessions. This happens as a result
    of calling `SessionTracker.untrackAllSessions()`.
    This callback is the right place to release any resources for the sessions (a DB connection, a DI scope, etc.).

## Usage examples

A sample app, that uses [Koin][koin] DI framework, can be found at the [sample_app_koin][sample_app_koin] module.

## Threading

SessionTracker is thread-safe. Public methods are declared as `synchronized`. Thread-safe compound actions are
possible by using synchronized statement on `SessionTracker` instance:

```kotlin
synchronized(sessionTracker) {
    sessionTracker.consumeEvent(..) // step 1 of the compound action
    sessionTracker.consumeEvent(..) // step 2 of the compound action
}
```

SessionTracker is a synchronous tool, meaning it neither creates threads nor uses thread-pools or handlers.

## Miscellaneous

Typical simple SessionTracker usage implies being a singleton in your app.

## License

> MIT License
> 
> Copyright (c) 2019 Vitaliy Khudenko
> 
> Permission is hereby granted, free of charge, to any person obtaining a copy
> of this software and associated documentation files (the "Software"), to deal
> in the Software without restriction, including without limitation the rights
> to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
> copies of the Software, and to permit persons to whom the Software is
> furnished to do so, subject to the following conditions:
> 
> The above copyright notice and this permission notice shall be included in all
> copies or substantial portions of the Software.
> 
> THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
> IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
> FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
> AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
> LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
> OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
> SOFTWARE.

[koin]: https://github.com/InsertKoinIO/koin
[sample_app_koin]: https://github.com/vitkhudenko/session_tracker/tree/master/sample_app_koin
