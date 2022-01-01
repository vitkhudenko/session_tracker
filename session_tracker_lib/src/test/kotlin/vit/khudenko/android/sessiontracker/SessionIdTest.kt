package vit.khudenko.android.sessiontracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test
import vit.khudenko.android.sessiontracker.test_util.assertThrows
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class SessionIdTest {

    @Test
    fun `happy case instantiation`() {
        assertEquals("id", SessionId("id").value)
    }

    @Test
    fun `instances with the same value should be equal (==)`() {
        assertEquals(SessionId("id"), SessionId("id"))
    }

    @Test
    fun `instances with the same value should not be the same (===)`() {
        assertNotSame(SessionId("id"), SessionId("id"))
    }

    @Test
    fun `test validation - value must not be empty`() {
        assertThrows(IllegalArgumentException::class.java, "SessionId value can not be empty") {
            SessionId("")
        }
    }

    @Test
    fun `test validation - value must not be blank`() {
        assertThrows(IllegalArgumentException::class.java, "SessionId value can not be blank") {
            SessionId(" ")
        }
    }

    @Test
    fun `test serialization`() {
        val sessionId = SessionId("id")

        val byteArray = ByteArrayOutputStream().use { byteArrayOut ->
            ObjectOutputStream(byteArrayOut).use { objectOut ->
                objectOut.writeObject(sessionId)
                byteArrayOut.toByteArray()
            }
        }

        val deserializedSessionId = ObjectInputStream(ByteArrayInputStream(byteArray)).use { objectIn ->
            objectIn.readObject()
        }

        assertEquals(sessionId, deserializedSessionId)
    }
}