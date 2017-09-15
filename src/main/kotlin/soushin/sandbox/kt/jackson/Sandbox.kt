package soushin.sandbox.kt.jackson

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.ZonedDateTime

class Sandbox {

    companion object {

        val objectMapper = ObjectMapper()
                .registerModule(KotlinModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .registerModule(JavaTimeModule())

        inline fun <reified T : Any> deserialize(json: String): T = objectMapper.readValue<T>(json)
    }

}

data class MyState(
        @JsonProperty("expiration_date")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss VV")
        val expirationDate: ZonedDateTime
)


