package app.prostuti.core.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppResultTest {
    @Test fun `map transforms success and passes failure through`() {
        assertEquals(AppResult.Success(4), AppResult.Success(2).map { it * 2 })
        val failure: AppResult<Int> = AppResult.Failure(AppError.NotFound)
        assertEquals(failure, failure.map { it * 2 })
    }

    @Test fun `retryable errors are transient ones only`() {
        assertTrue(AppError.NoConnection.isRetryable)
        assertTrue(AppError.Server(503, null).isRetryable)
        assertFalse(AppError.Server(400, null).isRetryable)
        assertFalse(AppError.SessionExpired.isRetryable)
    }

    @Test fun `getOrNull returns null for failures`() {
        assertNull(AppResult.Failure(AppError.Timeout).getOrNull())
    }
}
