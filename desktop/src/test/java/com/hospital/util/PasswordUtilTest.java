package com.hospital.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Focused tests for PasswordUtil to guarantee hash/verify round-trips work
 * correctly (the bug that broke seeded logins lived here).
 */
class PasswordUtilTest {

    @Test
    void hashAndVerifyRoundTrip_adminPassword() {
        String hash = PasswordUtil.hash("admin123");
        assertTrue(PasswordUtil.verify("admin123", hash));
    }

    @Test
    void hashAndVerifyRoundTrip_doctorPassword() {
        String hash = PasswordUtil.hash("doctor123");
        assertTrue(PasswordUtil.verify("doctor123", hash));
    }

    @Test
    void hashAndVerifyRoundTrip_receptionistPassword() {
        String hash = PasswordUtil.hash("receptionist123");
        assertTrue(PasswordUtil.verify("receptionist123", hash));
    }

    @Test
    void wrongPasswordIsRejected() {
        String hash = PasswordUtil.hash("admin123");
        assertFalse(PasswordUtil.verify("wrong", hash));
        assertFalse(PasswordUtil.verify("admin123 ", hash));
        assertFalse(PasswordUtil.verify("Admin123", hash));
    }

    @Test
    void nullAndEmptyInputsDontCrash() {
        assertFalse(PasswordUtil.verify(null, "anything"));
        assertFalse(PasswordUtil.verify("admin123", null));
        assertFalse(PasswordUtil.verify("admin123", "garbage"));
        assertFalse(PasswordUtil.verify("admin123", "1:abc")); // only 2 segments
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hash(null));
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hash(""));
    }

    @Test
    void storedFormatLooksCorrect() {
        String hash = PasswordUtil.hash("secret");
        String[] parts = hash.split(":");
        assertEquals(3, parts.length);
        assertEquals("65536", parts[0]);
        // 16-byte salt -> 24 chars base64
        assertEquals(24, parts[1].length());
        // 32-byte key -> 44 chars base64
        assertEquals(44, parts[2].length());
    }

    @Test
    void deterministicForKnownInputs() {
        // If the same (password, salt, iterations) is passed to pbkdf2HmacSha256,
        // output must be identical. This guards against nondeterministic bugs.
        byte[] salt = new byte[16];
        for (int i = 0; i < salt.length; i++) salt[i] = (byte) i;
        byte[] a = PasswordUtil.pbkdf2HmacSha256(
                "admin123".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                salt, 65536, 32);
        byte[] b = PasswordUtil.pbkdf2HmacSha256(
                "admin123".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                salt, 65536, 32);
        assertArrayEquals(a, b);
        assertEquals(32, a.length);
    }
}
