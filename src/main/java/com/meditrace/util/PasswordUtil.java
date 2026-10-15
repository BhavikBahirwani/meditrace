package com.meditrace.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Password hashing with PBKDF2 (Password-Based Key Derivation Function 2), built into the JDK.
 * <p>
 * Stored format (one string in {@code users.password_hash}):
 * <pre>pbkdf2_sha512$210000$&lt;base64 salt&gt;$&lt;base64 hash&gt;</pre>
 * <ul>
 *   <li>a random 16-byte salt per user means two users with the same password get
 *       different hashes, and pre-computed rainbow tables are useless;</li>
 *   <li>210,000 iterations of HMAC-SHA-512 (OWASP recommendation) make brute force slow;</li>
 *   <li>the algorithm and iteration count are stored with the hash, so they can be raised
 *       later without breaking existing accounts.</li>
 * </ul>
 * Run {@code main} to generate a hash for seed data:
 * {@code java -cp target/classes com.meditrace.util.PasswordUtil demo1234}
 */
public final class PasswordUtil {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA512";
    private static final String PREFIX = "pbkdf2_sha512";
    private static final int ITERATIONS = 210_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 512;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
        // no instances
    }

    /** Hashes a plain-text password with a fresh random salt. */
    public static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] derived = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_BITS);
        Base64.Encoder b64 = Base64.getEncoder();
        return PREFIX + "$" + ITERATIONS + "$" + b64.encodeToString(salt) + "$" + b64.encodeToString(derived);
    }

    /**
     * Checks a login attempt against a stored hash.
     * Uses {@link MessageDigest#isEqual} - a constant-time comparison that does not leak,
     * through timing, how many leading bytes matched.
     */
    public static boolean verify(String password, String stored) {
        if (password == null || stored == null) {
            return false;
        }
        String[] parts = stored.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = pbkdf2(password.toCharArray(), salt, iterations, expected.length * 8);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            return false; // malformed number or Base64
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyBits) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyBits);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 not available", e);
        } finally {
            spec.clearPassword();
            Arrays.fill(password, '\0'); // wipe the plain-text copy from memory
        }
    }

    /** Command-line helper: prints one hash per argument (used to build sql/seed.sql). */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java com.meditrace.util.PasswordUtil <password> [more passwords]");
            return;
        }
        for (String password : args) {
            System.out.println(hash(password));
        }
    }
}
