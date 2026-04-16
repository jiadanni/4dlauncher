package net.gsantner.opoc.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileUtilsTest {

    @Test
    fun hasExtension_shouldReturnTrue_whenSingleExtensionMatches() {
        assertTrue(FileUtils.hasExtension("test.jpg", "jpg"))
    }

    @Test
    fun hasExtension_shouldReturnTrue_whenOneOfMultipleExtensionsMatches() {
        assertTrue(FileUtils.hasExtension("test.png", "jpg", "png", "gif"))
    }

    @Test
    fun hasExtension_shouldReturnTrue_whenInputIsUpperCase() {
        assertTrue(FileUtils.hasExtension("TEST.JPG", "jpg"))
    }

    @Test
    fun hasExtension_shouldReturnTrue_whenExtensionIsUpperCase() {
        assertTrue(FileUtils.hasExtension("test.jpg", "JPG"))
    }

    @Test
    fun hasExtension_shouldReturnFalse_whenMissingDot() {
        // Implementation prepends "." to the extension
        assertFalse(FileUtils.hasExtension("testjpg", "jpg"))
    }

    @Test
    fun hasExtension_shouldReturnFalse_whenExtensionDoesNotMatch() {
        assertFalse(FileUtils.hasExtension("test.txt", "jpg"))
    }

    @Test
    fun hasExtension_shouldReturnFalse_whenFilenameIsEmpty() {
        assertFalse(FileUtils.hasExtension("", "jpg"))
    }

    @Test
    fun hasExtension_shouldReturnFalse_whenExtensionListIsEmpty() {
        assertFalse(FileUtils.hasExtension("test.jpg"))
    }
}
