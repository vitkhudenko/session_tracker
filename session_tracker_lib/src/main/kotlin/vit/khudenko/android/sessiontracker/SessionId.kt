package vit.khudenko.android.sessiontracker

import java.io.Serializable

@JvmInline
value class SessionId(val value: String) : Serializable {

    init {
        require(value.isNotEmpty()) { "${SessionId::class.java.simpleName} value can not be empty" }
        require(value.isNotBlank()) { "${SessionId::class.java.simpleName} value can not be blank" }
    }

}
