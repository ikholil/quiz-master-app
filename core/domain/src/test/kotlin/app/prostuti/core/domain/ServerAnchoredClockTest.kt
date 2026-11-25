package app.prostuti.core.domain

import app.prostuti.core.common.TimeSource
import app.prostuti.core.domain.exam.ServerAnchoredClock
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServerAnchoredClockTest {
    private class FakeTime : TimeSource {
        var monotonic = 1_000L
        var wall: Instant = Instant.parse("2026-01-01T00:00:00Z")
        override fun elapsedRealtimeMillis() = monotonic
        override fun wallClock() = wall
    }

    private val serverNow = Instant.parse("2026-11-25T10:00:00Z")

    @Test fun `remaining time follows monotonic time not the device wall clock`() {
        val time = FakeTime()
        val clock = ServerAnchoredClock(time, serverNow, serverNow.plus(Duration.ofMinutes(60)))
        time.monotonic += 10 * 60_000
        // Student moves the phone clock back by a day — must not add time.
        time.wall = time.wall.minus(Duration.ofDays(1))
        assertEquals(Duration.ofMinutes(50), clock.remaining())
    }

    @Test fun `remaining never goes negative and reports expiry`() {
        val time = FakeTime()
        val clock = ServerAnchoredClock(time, serverNow, serverNow.plusSeconds(30))
        time.monotonic += 45_000
        assertEquals(Duration.ZERO, clock.remaining())
        assertTrue(clock.isExpired())
    }

    @Test fun `resync re-anchors to the server`() {
        val time = FakeTime()
        val clock = ServerAnchoredClock(time, serverNow, serverNow.plus(Duration.ofMinutes(10)))
        time.monotonic += 60_000
        // Server says only 30s actually passed (e.g. device was suspended differently)
        clock.resync(serverNow.plusSeconds(30))
        assertEquals(Duration.ofSeconds(570), clock.remaining())
    }
}
