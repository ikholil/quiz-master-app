package app.prostuti.core.domain.exam

import app.prostuti.core.common.TimeSource
import java.time.Duration
import java.time.Instant

/**
 * Computes remaining exam time from the server's clock, never from the device's wall clock.
 *
 * When an attempt is fetched we record the server time and the device's *monotonic* time.
 * Remaining time = deadline − (serverNow + monotonic elapsed since fetch). Changing the phone's
 * date, time zone or clock has no effect. The server still enforces the deadline on submit.
 */
class ServerAnchoredClock(
    private val timeSource: TimeSource,
    serverNow: Instant,
    private val deadline: Instant,
) {
    private var anchorServerTime: Instant = serverNow
    private var anchorMonotonicMs: Long = timeSource.elapsedRealtimeMillis()

    fun estimatedServerNow(): Instant =
        anchorServerTime.plusMillis(timeSource.elapsedRealtimeMillis() - anchorMonotonicMs)

    fun remaining(): Duration {
        val left = Duration.between(estimatedServerNow(), deadline)
        return if (left.isNegative) Duration.ZERO else left
    }

    fun isExpired(): Boolean = remaining().isZero

    /** Re-anchor after any server round-trip (autosave responses carry the server time). */
    fun resync(serverNow: Instant) {
        anchorServerTime = serverNow
        anchorMonotonicMs = timeSource.elapsedRealtimeMillis()
    }
}
