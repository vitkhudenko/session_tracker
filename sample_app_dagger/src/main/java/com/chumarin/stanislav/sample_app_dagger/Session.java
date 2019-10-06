package com.chumarin.stanislav.sample_app_dagger;

public class Session {

    public enum State {
        INACTIVE,
        ACTIVE,
        FORGOTTEN
    }

    public enum Event {
        LOGIN,
        LOGOUT,
        LOGOUT_AND_FORGET
    }
}
