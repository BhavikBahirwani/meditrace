package com.meditrace.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests PBKDF2 hashing: correct password verifies, wrong one does not, salts differ. */
class PasswordUtilTest {

    @Test
    void correctPasswordVerifies() {
        String stored = PasswordUtil.hash("demo1234");
        assertTrue(stored.startsWith("pbkdf2_sha512$210000$"));
        assertTrue(PasswordUtil.verify("demo1234", stored));
    }

    @Test
    void wrongPasswordFails() {
        String stored = PasswordUtil.hash("demo1234");
        assertFalse(PasswordUtil.verify("demo12345", stored));
        assertFalse(PasswordUtil.verify("", stored));
        assertFalse(PasswordUtil.verify("demo1234", "not-a-hash"));
        assertFalse(PasswordUtil.verify(null, stored));
    }

    @Test
    void samePasswordGetsDifferentSalt() {
        assertNotEquals(PasswordUtil.hash("demo1234"), PasswordUtil.hash("demo1234"));
    }
}
