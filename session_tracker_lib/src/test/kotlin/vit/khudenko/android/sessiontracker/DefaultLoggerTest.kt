package vit.khudenko.android.sessiontracker

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class DefaultLoggerTest {

    private lateinit var logger: SessionTracker.Logger

    @Before
    fun setUp() {
        logger = SessionTracker.Logger.DefaultImpl()
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any()) } returns 0
    }

    @Test
    fun `logger#d()`() {
        val tag = "tag"
        val message = "message"

        logger.d(tag, message)

        verify(exactly = 1) { Log.d(tag, message) }
        verify(exactly = 0) { Log.w(any(), any<String>()) }
        verify(exactly = 0) { Log.e(any(), any()) }
    }

    @Test
    fun `logger#w()`() {
        val tag = "tag"
        val message = "message"

        logger.w(tag, message)

        verify(exactly = 0) { Log.d(any(), any()) }
        verify(exactly = 1) { Log.w(tag, message) }
        verify(exactly = 0) { Log.e(any(), any()) }
    }

    @Test
    fun `logger#e()`() {
        val tag = "tag"
        val message = "message"

        logger.e(tag, message)

        verify(exactly = 0) { Log.d(any(), any()) }
        verify(exactly = 0) { Log.w(any(), any<String>()) }
        verify(exactly = 1) { Log.e(tag, message) }
    }
}