package app.prostuti.feature.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhoneNumbersTest {
    @Test fun `accepts local, +88 and Bengali-digit numbers`() {
        assertTrue(PhoneNumbers.isValid("01712345678"))
        assertTrue(PhoneNumbers.isValid("+8801712345678"))
        assertTrue(PhoneNumbers.isValid("০১৭১২৩৪৫৬৭৮"))
        assertEquals("01912345678", PhoneNumbers.normalize("+880 1912-345678"))
    }

    @Test fun `rejects invalid operator prefixes and lengths`() {
        assertFalse(PhoneNumbers.isValid("01212345678"))
        assertFalse(PhoneNumbers.isValid("0171234567"))
        assertFalse(PhoneNumbers.isValid(""))
    }
}
