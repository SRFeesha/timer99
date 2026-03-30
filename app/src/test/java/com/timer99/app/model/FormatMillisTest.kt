package com.timer99.app.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatMillisTest {

    @Test
    fun `zero millis formats as 00 00`() {
        assertEquals("00:00", formatMillis(0L))
    }

    @Test
    fun `exactly one second`() {
        assertEquals("00:01", formatMillis(1_000L))
    }

    @Test
    fun `sub-second remainder rounds up to next second`() {
        // 1 ms → ceiling to 1 second
        assertEquals("00:01", formatMillis(1L))
        // 999 ms → ceiling to 1 second
        assertEquals("00:01", formatMillis(999L))
    }

    @Test
    fun `exactly one minute`() {
        assertEquals("01:00", formatMillis(60_000L))
    }

    @Test
    fun `fractional minute rounds up to next second`() {
        // 90 500 ms → ceiling to 91 s → 1:31
        assertEquals("01:31", formatMillis(90_500L))
    }

    @Test
    fun `exact second boundary does not round up`() {
        assertEquals("01:30", formatMillis(90_000L))
    }

    @Test
    fun `five minutes default duration`() {
        assertEquals("05:00", formatMillis(300_000L))
    }

    @Test
    fun `large value formats correctly`() {
        // 25 minutes
        assertEquals("25:00", formatMillis(1_500_000L))
    }

    @Test
    fun `minutes and seconds both zero-padded`() {
        assertEquals("02:05", formatMillis(125_000L))
    }
}
