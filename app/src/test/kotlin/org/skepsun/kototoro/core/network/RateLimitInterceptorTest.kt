package org.skepsun.kototoro.core.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.skepsun.kototoro.parsers.exception.TooManyRequestExceptions
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class RateLimitInterceptorTest {

    @Test
    fun `Retry-After HTTP date is converted to relative delay`() {
        MockWebServer().use { server ->
            val retryAt = ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(2)
            server.enqueue(
                MockResponse()
                    .setResponseCode(429)
                    .setHeader("Retry-After", DateTimeFormatter.RFC_1123_DATE_TIME.format(retryAt)),
            )
            server.start()

            val error = runCatching {
                OkHttpClient.Builder()
                    .addInterceptor(RateLimitInterceptor())
                    .build()
                    .newCall(Request.Builder().url(server.url("/image")).build())
                    .execute()
            }.exceptionOrNull()

            assertTrue(error is TooManyRequestExceptions)
            assertTrue((error as TooManyRequestExceptions).getRetryDelay() in 0..2_500)
        }
    }

    @Test
    fun `Retry-After seconds remains a relative delay`() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "7"))
            server.start()

            val error = runCatching {
                OkHttpClient.Builder()
                    .addInterceptor(RateLimitInterceptor())
                    .build()
                    .newCall(Request.Builder().url(server.url("/image")).build())
                    .execute()
            }.exceptionOrNull()

            assertTrue(error is TooManyRequestExceptions)
            assertTrue((error as TooManyRequestExceptions).getRetryDelay() in 6_000..7_000)
        }
    }
}
