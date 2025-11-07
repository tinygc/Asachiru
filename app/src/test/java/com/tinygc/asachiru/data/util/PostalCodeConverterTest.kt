package com.tinygc.asachiru.data.util

import org.junit.Assert.*
import org.junit.Test

class PostalCodeConverterTest {

    @Test
    fun `getSampleMapping should return valid mapping`() {
        // Act
        val mapping = PostalCodeConverter.getSampleMapping()

        // Assert
        assertTrue(mapping.isNotEmpty())
        assertTrue(mapping.containsKey("100"))
        assertTrue(mapping.containsKey("530"))
        assertEquals("130010", mapping["100"])
        assertEquals("270000", mapping["530"])
    }

    @Test
    fun `getSampleMapping should contain Tokyo area codes`() {
        // Act
        val mapping = PostalCodeConverter.getSampleMapping()

        // Assert
        assertTrue(mapping.containsKey("100"))
        assertTrue(mapping.containsKey("101"))
        assertTrue(mapping.containsKey("102"))
        assertEquals("130010", mapping["100"])
        assertEquals("130010", mapping["101"])
        assertEquals("130010", mapping["102"])
    }

    @Test
    fun `getSampleMapping should contain Osaka area codes`() {
        // Act
        val mapping = PostalCodeConverter.getSampleMapping()

        // Assert
        assertTrue(mapping.containsKey("530"))
        assertTrue(mapping.containsKey("531"))
        assertTrue(mapping.containsKey("532"))
        assertEquals("270000", mapping["530"])
        assertEquals("270000", mapping["531"])
        assertEquals("270000", mapping["532"])
    }

    @Test
    fun `getSampleMapping should contain Hokkaido area codes`() {
        // Act
        val mapping = PostalCodeConverter.getSampleMapping()

        // Assert
        assertTrue(mapping.containsKey("001"))
        assertTrue(mapping.containsKey("060"))
        assertEquals("016010", mapping["001"])
        assertEquals("016010", mapping["060"])
    }

    @Test
    fun `getSampleMapping should have consistent format for area codes`() {
        // Act
        val mapping = PostalCodeConverter.getSampleMapping()

        // Assert
        mapping.values.forEach { areaCode ->
            // Area codes should be 6 digits
            assertEquals("Area code should be 6 digits: $areaCode", 6, areaCode.length)
            assertTrue("Area code should be numeric: $areaCode", areaCode.all { it.isDigit() })
        }
    }

    @Test
    fun `getSampleMapping should have valid postal code prefixes`() {
        // Act
        val mapping = PostalCodeConverter.getSampleMapping()

        // Assert
        mapping.keys.forEach { prefix ->
            // Postal code prefixes should be 3 digits
            assertEquals("Postal code prefix should be 3 digits: $prefix", 3, prefix.length)
            assertTrue("Postal code prefix should be numeric: $prefix", prefix.all { it.isDigit() })
        }
    }

    // Note: Tests that require Android Context (initialize() and convertToAreaCode())
    // should be implemented as Android instrumentation tests in androidTest directory
    // because they need access to Assets
}
