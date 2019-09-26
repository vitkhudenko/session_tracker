package vit.khudenko.android.sessiontracker.sample.koin.util

import android.util.Log
import androidx.lifecycle.ViewModel
import java.util.concurrent.atomic.AtomicInteger

abstract class BaseViewModel : ViewModel() {

    companion object {
        private val DEBUG_STATE: HashMap<String, AtomicInteger> = HashMap()

        fun dump(): String {
            return DEBUG_STATE.entries.joinToString(
                prefix = "[", postfix = "]"
            ) { "${it.key}: ${it.value.get()}" }
        }
    }

    init {
        val tag = javaClass.simpleName
        Log.d(tag, "init: entered, viewModels dump: ${dump()}")

        val entry = DEBUG_STATE.entries.find { it.key == tag }
        if (entry != null) {
            entry.value.incrementAndGet()
        } else {
            DEBUG_STATE[tag] = AtomicInteger(1)
        }

        Log.d(tag, "init: passed, viewModels dump: ${dump()}")
    }

    override fun onCleared() {
        super.onCleared()

        val tag = javaClass.simpleName
        Log.d(tag, "onCleared: entered, viewModels dump: ${dump()}")

        val entry = DEBUG_STATE.entries.find { it.key == tag }
        if (entry != null) {
            entry.value.decrementAndGet()
        } else {
            throw RuntimeException("no debug state for $tag")
        }

        Log.d(tag, "onCleared: passed, viewModels dump: ${dump()}")
    }
}
