package vit.khudenko.android.sessiontracker.test_util

import org.mockito.ArgumentMatcher

fun matches(regex: String) = ArgumentMatcher<String> { argument -> argument!!.matches(regex.toRegex()) }

fun equals(string: String) = ArgumentMatcher<String> { argument -> argument == string }