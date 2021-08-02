## TL;DR

SessionTracker is a general purpose framework to provide a foundation for session management in your app.
Your app provides (a) session tracking storage implementation and (b) session tracking state machine configuration,
while SessionTracker provides callbacks to create/update/release session resources.

## Integration

At the project level `build.gradle`, add a maven repo pointing to `https://jitpack.io`, e.g.:

```groovy
allprojects {
    repositories {
        google()
        maven { url "https://jitpack.io" } // this is it
        jcenter()
    }
}
```

At a module level `build.gradle`, add the following dependency:

```groovy
implementation 'com.github.vitkhudenko:session_tracker:1.0.1'
```

## Contract description

### What is session?

Session is a flexible entity - it could be a user session with user signing in/out or a bluetooth device session
with all its possible states.

### Session tracking state VS. session state

In SessionTracker framework, sessions are represented by session tracking records - instances of
`SessionRecord`. It is an immutable data structure of 2 fields - session ID and
session tracking state.

Note, there are two (partially intersecting) types of session state:
1. The session state that is tracked by SessionTracker, which is always an instance of enum by the contract.
2. The session state that is specific to your app, which can be as diverse as your app's business logic requires
and which can not be represented by the `SessionRecord`.

Please don't mess one with another. Actual implementation of session, including its persistence, is up to your app
and is out of SessionTracker framework responsibility. It is correct to say that session tracking state (the one
tracked by SessionTracker) is a subset of a full session state in your app.


### Persistence

SessionTracker framework supports session tracking auto-restoration on application process restarts. Your
app must provide an implementation of `ISessionTrackerStorage`, which is used by SessionTracker to make CRUD 
operations on session tracking records.

### Session tracking state machine

SessionTracker maintains a state machine per session. Your app must define a set of possible events and
states per session. Using events and states, your app should provide state machine transitions, which are
used to configure session state machine. For example, your app may define the following session tracking 
events and states:

```kotlin
enum class State {
    INACTIVE, ACTIVE
}

enum class Event {
    LOGIN, LOGOUT
}
```

then a sample transitions config (an implementation of `ISessionStateTransitionsSupplier`) could be as following:

```kotlin
val sessionStateTransitionsSupplier = object : ISessionStateTransitionsSupplier<Event, State> {
    override fun getStateTransitions(session: Session) = listOf(
        Transition(
            event = Event.LOGIN,
            statePath = listOf(State.INACTIVE, State.ACTIVE)
        ),
        Transition(
            event = Event.LOGOUT,
            statePath = listOf(State.ACTIVE, State.INACTIVE)
        )
    )
}
```

Such config would mean there are two possible session tracking states (`ACTIVE`/`INACTIVE`) and two possible 
session tracking events: `LOGIN` (to move session from `INACTIVE` to `ACTIVE` state) and `LOGOUT` (to move 
session from `ACTIVE` to `INACTIVE` state).

### Session tracking

In order to make SessionTracker ready to function it should be initialized first. The most appropriate place for
`initialize(sessionTrackerListener: Listener<Event, State>)` call is `android.app.Application.onCreate()`.

Suppose your user hits "Login" button, your app authenticates user and creates a session. In order to make
use of SessionTracker the session should be "attached" to SessionTracker:

```kotlin
sessionTracker.trackSession(sessionId, State.ACTIVE)
```

Now SessionTracker is tracking the session until your app calls `untrackSession(sessionId)`. Next time 
your app starts (and SessionTracker is initialized), the session tracking will be automatically restored
by SessionTracker with the same `ACTIVE` state.

As long as session is tracked, its session tracking state changes are propagated to your app via `SessionTracker.Listener`.

Suppose eventually your user hits "Log Out" button, then your app is responsible to communicate this event
to SessionTracker by asking to consume `LOGOUT` event for the session:

```kotlin
sessionTracker.consumeEvent(sessionId, Event.LOGOUT)
```

Now SessionTracker updates session tracking state to `INACTIVE`, persists session record with the new state and
propagates this state change via `SessionTracker.Listener`. Note, the session is still tracked by SessionTracker,
so next time your app starts, the session tracking will be automatically restored by SessionTracker with 
the same `INACTIVE` state.

### Management of session resources

`SessionTracker.Listener` has useful for your app callbacks that allow to manage session resources appropriately:

- `onSessionTrackerInitialized(sessionTracker: SessionTracker<Event, State>, sessionRecords: List<SessionRecord<State>>)`

    SessionTracker has added sessions to the list of tracked sessions.
    This happens as a result of calling `SessionTracker.initialize(sessionTrackerListener: Listener<Event, State>)`.
    This callback is the right place to create any resources for the sessions (a DB connection, a DI scope, etc.).

- `onSessionTrackingStarted(sessionTracker: SessionTracker<Event, State>, sessionRecord: SessionRecord<State>)`

    SessionTracker has added session to the list of tracked sessions.
    This happens as a result of calling `SessionTracker.trackSession(sessionId, state)`.
    This callback is the right place to create any resources for the session (a DB connection, a DI scope, etc.).

- `onSessionStateChanged(sessionTracker: SessionTracker<Event, State>, sessionRecord: SessionRecord<State>, oldState: State)`

    Session tracking state has changed.
    This happens as a result of calling `SessionTracker.consumeEvent(sessionId, event)`.
    This callback is the right place to create or release any resources for the session (a DB connection,
    a DI scope, etc.).

- `onSessionTrackingStopped(sessionTracker: SessionTracker<Event, State>, sessionRecord: SessionRecord<State>)`

    SessionTracker has removed session from the list of tracked sessions. This happens as a result
    of calling `SessionTracker.untrackSession(sessionId)`.
    This may also happen as a result of calling `SessionTracker.consumeEvent` if session appears in one of 
    the `autoUntrackStates` (a `SessionTracker` constructor parameter).
    This callback is the right place to release any resources for the session (a DB connection, a DI scope, etc.).

- `onAllSessionsTrackingStopped(sessionTracker: SessionTracker<Event, State>, sessionRecords: List<SessionRecord<State>>)`

    SessionTracker has removed all sessions from the list of tracked sessions. This happens as a result
    of calling `SessionTracker.untrackAllSessions()`.
    This callback is the right place to release any resources for the sessions (a DB connection, a DI scope, etc.).

## Usage examples

| DI framework      | Sample app module                      |
|-------------------|----------------------------------------|
| [Koin][koin]      | [sample_app_koin][sample_app_koin]     |
| [Dagger][dagger]  | [sample_app_dagger][sample_app_dagger] |

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

## Test coverage

[Test Summary][test_summary]

[Test Coverage report][test_coverage]


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
[dagger]: https://github.com/google/dagger
[sample_app_dagger]: https://github.com/vitkhudenko/session_tracker/tree/master/sample_app_dagger

[test_summary]: https://htmlpreview.github.io/?https://github.com/vitkhudenko/session_tracker/blob/master/session_tracker_lib/reports/tests/testReleaseUnitTest/index.html
[test_coverage]: https://htmlpreview.github.io/?https://github.com/vitkhudenko/session_tracker/blob/master/session_tracker_lib/reports/jacoco/jacocoUnitTestReport/html/index.html
