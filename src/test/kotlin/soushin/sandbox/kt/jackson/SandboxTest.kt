package soushin.sandbox.kt.jackson

import io.kotlintest.matchers.shouldBe
import org.junit.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SandboxTest {

    @Test
    fun deserialize() {
        Sandbox.deserialize<MyState>(json).let {
            format(formatter)(it.expirationDate.toLocalDateTime()) shouldBe "2016-06-17 01:27:28"
        }
    }

    private fun format(f: DateTimeFormatter): (LocalDateTime) -> String {
        return { date -> f.format(date) }
    }

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private val json = """
{
  "expiration_date": "2016-06-17 01:27:28 Etc/GMT"
}
"""
}
