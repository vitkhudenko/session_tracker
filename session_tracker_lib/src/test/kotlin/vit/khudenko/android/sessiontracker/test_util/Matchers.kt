package vit.khudenko.android.sessiontracker.test_util

import org.mockito.ArgumentMatcher
import org.mockito.Mockito
import vit.khudenko.android.sessiontracker.SessionId

fun matches(regex: String) = ArgumentMatcher<String> { argument -> argument!!.matches(regex.toRegex()) }

fun equals(string: String) = ArgumentMatcher<String> { argument -> argument == string }

fun anySessionId(): SessionId {
    return anyValueClass(
        underlyingClass = String::class.java,
        creator = {
            if (it != null) {
                SessionId(it)
            } else {
                SessionId("-")
            }
        }
    )
}

private fun <Outer, Inner> anyValueClass(
    underlyingClass: Class<Inner>,
    creator: (Inner?) -> Outer,
): Outer {
    return creator(Mockito.any(underlyingClass))
}
