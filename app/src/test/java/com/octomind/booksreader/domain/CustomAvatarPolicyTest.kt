package com.octomind.booksreader.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomAvatarPolicyTest {
    @Test
    fun `accepts supported image within safe limits`() {
        assertTrue(CustomAvatarPolicy.accepts("image/jpeg", 2_000_000, 1_200, 1_600))
        assertTrue(CustomAvatarPolicy.accepts("image/png", null, 512, 512))
        assertTrue(CustomAvatarPolicy.accepts("image/webp", 400_000, 800, 800))
    }

    @Test
    fun `rejects unsupported or oversized files`() {
        assertFalse(CustomAvatarPolicy.accepts("image/gif", 200_000, 512, 512))
        assertFalse(CustomAvatarPolicy.accepts("image/jpeg", CustomAvatarPolicy.MAX_FILE_BYTES + 1, 512, 512))
    }

    @Test
    fun `rejects unsafe dimensions`() {
        assertFalse(CustomAvatarPolicy.accepts("image/png", 200_000, 64, 512))
        assertFalse(CustomAvatarPolicy.accepts("image/png", 200_000, 8_193, 512))
        assertFalse(CustomAvatarPolicy.accepts("image/png", 200_000, 8_000, 8_000))
    }
}
