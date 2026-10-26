package com.jiankang.app

import com.jiankang.app.notifications.WorkdayBreakTimes
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class WorkdayBreakTimesTest {
    private val zone = ZoneId.of("Europe/Zurich")
    private val start = LocalTime.of(8, 0)
    private val end = LocalTime.of(17, 0)
    private fun at(day: Int, hour: Int, minute: Int = 0) =
        ZonedDateTime.of(2026, 10, day, hour, minute, 0, 0, zone)

    @Test fun defaultWorkdayFirstAlertAtNine() {
        assertEquals(at(26, 9), WorkdayBreakTimes.next(at(26, 8), start, end))
        assertEquals(at(26, 10), WorkdayBreakTimes.next(at(26, 9), start, end))
    }

    @Test fun lastAlertIsFourOClock() {
        assertEquals(at(26, 16), WorkdayBreakTimes.next(at(26, 15, 30), start, end))
        assertEquals(at(27, 9), WorkdayBreakTimes.next(at(26, 16), start, end))
    }

    @Test fun skipsSaturdaySundayAndOutsideHours() {
        assertEquals(at(26, 9), WorkdayBreakTimes.next(at(24, 12), start, end))
        assertEquals(at(26, 9), WorkdayBreakTimes.next(at(25, 12), start, end))
        assertEquals(at(26, 9), WorkdayBreakTimes.next(at(23, 17), start, end))
    }

    @Test fun respectsCustomTimeWindow() {
        val customStart = LocalTime.of(9, 30)
        val customEnd = LocalTime.of(12, 30)
        assertEquals(at(26, 10, 30), WorkdayBreakTimes.next(at(26, 8), customStart, customEnd))
        assertEquals(at(26, 11, 30), WorkdayBreakTimes.next(at(26, 10, 30), customStart, customEnd))
        assertEquals(at(27, 10, 30), WorkdayBreakTimes.next(at(26, 11, 30), customStart, customEnd))
    }

    @Test fun invalidWindowIsRejected() {
        assertFalse(WorkdayBreakTimes.validWindow(LocalTime.of(16, 30), LocalTime.of(17, 0)))
        assertFalse(WorkdayBreakTimes.validWindow(end, start))
        assertTrue(WorkdayBreakTimes.validWindow(start, end))
    }
}
