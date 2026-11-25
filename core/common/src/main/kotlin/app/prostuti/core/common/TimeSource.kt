package app.prostuti.core.common

import java.time.Clock
import java.time.Instant

/**
 * Abstractions over time so exam timing can be anchored to the server and tested deterministically.
 * [elapsedRealtimeMillis] must be monotonic (SystemClock.elapsedRealtime on Android) — it keeps
 * counting when the user changes the device clock or time zone.
 */
interface TimeSource {
    fun elapsedRealtimeMillis(): Long
    fun wallClock(): Instant
}

class SystemTimeSource(
    private val monotonic: () -> Long = { System.nanoTime() / 1_000_000 },
    private val clock: Clock = Clock.systemUTC(),
) : TimeSource {
    override fun elapsedRealtimeMillis(): Long = monotonic()
    override fun wallClock(): Instant = clock.instant()
}
