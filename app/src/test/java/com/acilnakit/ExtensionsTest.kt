package com.acilnakit

import com.acilnakit.util.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.*

/**
 * Advanced Unit Tests for Architect's Power Pack
 * Covers formatting, relative time, and complex validations.
 */
class ExtensionsTest {

    // --- FORMATTING TESTS ---

    @Test
    fun `Double toTL - Correctly formats currency`() {
        assertEquals("₺125,50", 125.50.toTL())
        assertEquals("₺100,00", 100.0.toTL())
    }

    @Test
    fun `Double toPrice - Optimizes display for whole numbers`() {
        assertEquals("₺100", 100.0.toPrice())
        assertEquals("₺125,50", 125.50.toPrice())
    }

    // --- RELATIVE TIME TESTS ---

    @Test
    fun `Long toRelativeTime - Handles extreme cases`() {
        val now = System.currentTimeMillis()
        assertEquals("Az önce", now.toRelativeTime())
        
        val minuteAgo = now - 61_000
        assertEquals("1 dk önce", minuteAgo.toRelativeTime())
        
        val hourAgo = now - (3600_000 + 10_000)
        assertEquals("1 saat önce", hourAgo.toRelativeTime())
    }

    // --- VALIDATION TESTS ---

    @Test
    fun `String isUniversityEmail - Correctly identifies domains`() {
        assertTrue("ahmet@itu.edu.tr".isUniversityEmail())
        assertTrue("user@harvard.edu".isUniversityEmail())
        assertFalse("fake@gmail.com".isUniversityEmail())
        assertFalse("fake@edu.co".isUniversityEmail()) // Only .edu or .edu.tr
        assertFalse("not-an-email".isUniversityEmail())
    }

    @Test
    fun `String isValidPhone - Correctly validates Turkish format`() {
        assertTrue("05321112233".isValidPhone())
        assertTrue("5321112233".isValidPhone())
        assertTrue("0532 111 22 33".isValidPhone()) // Checks replacement of space
        assertFalse("02121112233".isValidPhone()) // Landline not allowed
        assertFalse("12345".isValidPhone()) // Too short
        assertFalse("0532A112233".isValidPhone()) // Contains letter
    }

    // --- DATA UTILS TESTS ---

    @Test
    fun `Map getValueOrDefault - Returns default on missing key or wrong type`() {
        val data = mapOf("balance" to 100.0, "name" to "Ahmet")
        
        assertEquals(100.0, data.getValueOrDefault("balance", 0.0), 0.0)
        assertEquals("Ahmet", data.getValueOrDefault("name", "Unknown"))
        assertEquals("Default", data.getValueOrDefault("age", "Default")) // Missing key
        assertEquals(0, data.getValueOrDefault("balance", 0)) // Wrong type (Int vs Double)
    }
}
