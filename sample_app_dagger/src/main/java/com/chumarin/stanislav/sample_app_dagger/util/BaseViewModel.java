package com.chumarin.stanislav.sample_app_dagger.util;

import android.util.Log;

import androidx.lifecycle.ViewModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class BaseViewModel extends ViewModel {

    private static final HashMap<String, AtomicInteger> DEBUG_STATE = new HashMap<>();

    public static String dump() {
        return DEBUG_STATE.entrySet()
                .stream()
                .map(entry -> "" + entry.getKey() + ": " + entry.getValue().get())
                .collect(Collectors.joining(", ", "[", "]"));
    }

    public BaseViewModel() {
        String tag = getClass().getSimpleName();
        Log.d(tag, "constructor: entered, viewModels dump: " + dump());

        Optional<Map.Entry<String, AtomicInteger>> entry =
                DEBUG_STATE.entrySet()
                        .stream()
                        .filter(saie -> saie.getKey().equals(tag))
                        .findFirst();

        if (entry.isPresent()) {
            entry.get().getValue().incrementAndGet();
        } else {
            DEBUG_STATE.put(tag, new AtomicInteger(1));
        }

        Log.d(tag, "constructor: entered, viewModels dump: " + dump());
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        String tag = getClass().getSimpleName();
        Log.d(tag, "onCleared: entered, viewModels dump: " + dump());

        Optional<Map.Entry<String, AtomicInteger>> entry =
                DEBUG_STATE.entrySet()
                        .stream()
                        .filter(saie -> saie.getKey().equals(tag))
                        .findFirst();

        if (entry.isPresent()) {
            entry.get().getValue().decrementAndGet();
        } else {
            throw new RuntimeException("no debug state for " + tag);
        }

        Log.d(tag, "onCleared: entered, viewModels dump: " + dump());
    }
}
