package com.cybersaad.hackstreak

import org.junit.Assert.assertEquals
import org.junit.Test

class NumberUtilsTest {
    @Test
    fun formatNumber_zero_returns0() {
        assertEquals("0", formatNumber(0))
    }

    @Test
    fun formatNumber_thousand_returnsComma() {
        assertEquals("1,234", formatNumber(1234))
    }
}
