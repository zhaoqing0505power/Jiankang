package com.jiankang.app

import com.jiankang.app.screentime.ScreenEvent
import com.jiankang.app.screentime.ScreenTimeMath
import org.junit.Assert.*
import org.junit.Test

class ScreenTimeMathTest {
    private val minute = 60_000L

    @Test fun clipsAcrossMidnightAndSumsMultipleSessions() {
        val usage = ScreenTimeMath.estimate(0L, 90 * minute, listOf(
            ScreenEvent(-10 * minute, true),
            ScreenEvent(20 * minute, false),
            ScreenEvent(30 * minute, true),
            ScreenEvent(60 * minute, false)
        ))!!
        assertEquals(50, usage.minutes)
        assertFalse(usage.partial)
    }
    @Test fun activeScreenCountsUntilNow() {
        val usage = ScreenTimeMath.estimate(0L, 35 * minute, listOf(
            ScreenEvent(-1L, false), ScreenEvent(10 * minute, true)
        ))!!
        assertEquals(25, usage.minutes)
        assertFalse(usage.partial)
    }
    @Test fun noInitialStateProducesPartialEstimate() {
        val usage = ScreenTimeMath.estimate(0L, 50 * minute, listOf(
            ScreenEvent(20 * minute, true), ScreenEvent(40 * minute, false)
        ))!!
        assertEquals(20, usage.minutes)
        assertTrue(usage.partial)
    }
    @Test fun emptyEventsAreUnknownNotZero() {
        assertNull(ScreenTimeMath.estimate(0L, minute, emptyList()))
    }
    @Test fun repeatedInteractiveEventsDoNotDoubleCount() {
        val usage = ScreenTimeMath.estimate(0L, 60 * minute, listOf(
            ScreenEvent(-1, false), ScreenEvent(10 * minute, true),
            ScreenEvent(20 * minute, true), ScreenEvent(40 * minute, false)
        ))!!
        assertEquals(30, usage.minutes)
    }
}
