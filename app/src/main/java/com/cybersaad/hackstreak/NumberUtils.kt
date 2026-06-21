package com.cybersaad.hackstreak

/**
 * Small helper for formatting numbers for display.
 */
fun formatNumber(n: Int): String {
    if (n == 0) return "0"
    return String.format("%,d", n)
}
