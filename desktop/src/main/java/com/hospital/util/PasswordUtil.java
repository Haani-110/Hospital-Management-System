package com.hospital.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility for hashing passwords and verifying them.
 *
 * Uses PBKDF2-HMAC-SHA256 (RFC 2898) with a random salt per password. The
 * PBKDF2 core is implemented directly on top of {@code Mac.getInstance("HmacSHA256")}
 * rather than through {@code SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")},
 * because different JDK/JCE providers have historically disagreed on how the
 * char[] password is converted to bytes under PBEKeySpec (and at least one
 * OpenJDK 21 build on Ubuntu showed a real hash/verify mismatch for valid
 * seeded credentials). Implementing PBKDF2 ourselves with explicit UTF-8
 * makes hashing deterministic and portable across JDK vendors/versions.
 *
 * The stored format is: {@code iterations:saltBase64:hashBase64}.
 */
public final class PasswordUtil {

    private static final int ITERATIONS = 65_536;
    private static final int KEY_BYTES = 32;          // 256-bit output
    private static final int SALT_BYTES = 16;
    private static final String HMAC_ALG = "HmacSHA256";

    private static final int HMAC_LEN = 32;           // SHA-256 produces 32-byte blocks

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    /**
     * Hash a raw password for storage.
     */
    public static String hash(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        byte[] salt = new byte[SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2HmacSha256(utf8(password), salt, ITERATIONS, KEY_BYTES);
        return ITERATIONS + ":"
                + Base64.getEncoder().encodeToString(salt) + ":"
                + Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Verify a raw password against a stored hash string.
     */
    public static boolean verify(String password, String storedHash) {
        if (password == null || storedHash == null) return false;
        String[] parts = storedHash.split(":");
        if (parts.length != 3) return false;
        try {
            int iterations = Integer.parseInt(parts[0]);
            if (iterations <= 0) return false;
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[2]);
            if (expectedHash.length == 0 || salt.length == 0) return false;
            // Always derive exactly as many bytes as the stored hash, so the
            // comparison is length-safe even if KEY_BYTES changes in the future.
            byte[] actualHash = pbkdf2HmacSha256(utf8(password), salt, iterations, expectedHash.length);
            return constantTimeEquals(expectedHash, actualHash);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return false;
        }
    }

    /**
     * RFC 2898 PBKDF2 using HMAC-SHA256 as the PRF. Package-private so tests
     * can exercise deterministic vectors directly.
     */
    static byte[] pbkdf2HmacSha256(byte[] password, byte[] salt, int iterations, int keyBytes) {
        if (iterations <= 0) throw new IllegalArgumentException("iterations must be > 0");
        if (keyBytes <= 0) throw new IllegalArgumentException("keyBytes must be > 0");

        Mac mac = newHmacSha256(password);
        int blocks = (keyBytes + HMAC_LEN - 1) / HMAC_LEN;
        byte[] out = new byte[blocks * HMAC_LEN];
        int offset = 0;

        for (int i = 1; i <= blocks; i++) {
            mac.update(salt);
            mac.update(intToBytes(i));
            byte[] u = mac.doFinal();
            System.arraycopy(u, 0, out, offset, u.length);

            for (int iter = 1; iter < iterations; iter++) {
                u = mac.doFinal(u);
                for (int k = 0; k < HMAC_LEN; k++) {
                    out[offset + k] ^= u[k];
                }
            }
            offset += HMAC_LEN;
        }

        if (out.length == keyBytes) return out;
        byte[] truncated = new byte[keyBytes];
        System.arraycopy(out, 0, truncated, 0, keyBytes);
        return truncated;
    }

    private static Mac newHmacSha256(byte[] key) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALG);
            mac.init(new SecretKeySpec(key, HMAC_ALG));
            return mac;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HmacSHA256 not available", e);
        }
    }

    private static byte[] utf8(String s) {
        return s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static byte[] intToBytes(int i) {
        return new byte[] {
                (byte) (i >>> 24),
                (byte) (i >>> 16),
                (byte) (i >>> 8),
                (byte) i
        };
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null) return false;
        if (a.length != b.length) return false;
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}
