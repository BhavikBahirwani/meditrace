package com.meditrace.service;

import com.meditrace.dao.UserDAO;
import com.meditrace.exception.AuthenticationException;
import com.meditrace.exception.DataAccessException;
import com.meditrace.model.Role;
import com.meditrace.model.User;
import com.meditrace.model.UserFactory;
import com.meditrace.util.PasswordUtil;
import com.meditrace.util.ValidationUtil;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registration and login rules.
 * <p>
 * Includes a simple brute-force guard: after {@value #MAX_FAILURES} wrong passwords a
 * username is locked for 5 minutes. The counters live in a {@link ConcurrentHashMap}
 * because many servlet threads log in at the same time.
 */
public class AuthService {

    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_TIME = Duration.ofMinutes(5);

    /** Hash of a random password; checked when the username does not exist so timing looks the same. */
    private static final String DUMMY_HASH = PasswordUtil.hash("timing-equaliser-" + System.nanoTime());

    /** Shared across all AuthService instances: username -> failed attempts. */
    private static final Map<String, FailedAttempts> FAILURES = new ConcurrentHashMap<>();

    private final UserDAO userDAO = new UserDAO();

    /** Failure counter for one username (immutable; replaced atomically via Map.merge). */
    private record FailedAttempts(int count, Instant lastFailure) {
        boolean isLocked() {
            return count >= MAX_FAILURES && lastFailure.plus(LOCK_TIME).isAfter(Instant.now());
        }
    }

    /**
     * Checks username + password.
     *
     * @return the logged-in user (as its concrete subclass)
     * @throws AuthenticationException with a message that is safe to show
     */
    public User login(String username, String password) throws AuthenticationException, DataAccessException {
        if (ValidationUtil.isBlank(username) || ValidationUtil.isBlank(password)) {
            throw new AuthenticationException("Please enter your username and password.");
        }
        String key = username.trim().toLowerCase(Locale.ROOT);
        FailedAttempts attempts = FAILURES.get(key);
        if (attempts != null && attempts.isLocked()) {
            throw new AuthenticationException("Too many failed attempts. Please try again in a few minutes.");
        }

        Optional<User> found = userDAO.findByUsername(username.trim());
        String storedHash = found.map(User::getPasswordHash).orElse(DUMMY_HASH);
        boolean ok = PasswordUtil.verify(password, storedHash) && found.isPresent();

        if (!ok) {
            FAILURES.merge(key, new FailedAttempts(1, Instant.now()),
                    (old, one) -> new FailedAttempts(old.count() + 1, Instant.now()));
            // Same message for "no such user" and "wrong password" - do not reveal which usernames exist.
            throw new AuthenticationException("Incorrect username or password.");
        }
        FAILURES.remove(key);
        return found.get();
    }

    /**
     * Validates the form and creates a new account.
     *
     * @throws AuthenticationException describing the first invalid field
     */
    public User register(String username, String email, String password, String confirm, String fullName,
                         String roleValue, String company, String city, String licenseNo)
            throws AuthenticationException, DataAccessException {

        Role role = Role.parse(roleValue)
                .orElseThrow(() -> new AuthenticationException("Please choose your role in the supply chain."));
        if (!ValidationUtil.isValidUsername(username)) {
            throw new AuthenticationException("Username must be 3-30 letters, digits, dots, dashes or underscores.");
        }
        if (!ValidationUtil.isValidEmail(email)) {
            throw new AuthenticationException("Please enter a valid email address.");
        }
        if (!ValidationUtil.isValidPassword(password)) {
            throw new AuthenticationException("Password must be at least "
                    + ValidationUtil.MIN_PASSWORD_LENGTH + " characters.");
        }
        if (!password.equals(confirm)) {
            throw new AuthenticationException("The two passwords do not match.");
        }
        if (ValidationUtil.isBlank(fullName) || ValidationUtil.isBlank(company) || ValidationUtil.isBlank(city)) {
            throw new AuthenticationException("Name, company and city are required.");
        }
        if (userDAO.existsByUsername(username)) {
            throw new AuthenticationException("That username is already taken.");
        }
        if (userDAO.existsByEmail(email.trim())) {
            throw new AuthenticationException("An account with that email already exists.");
        }

        User user = UserFactory.create(role);
        user.setUsername(username.trim());
        user.setEmail(email.trim());
        user.setPasswordHash(PasswordUtil.hash(password));
        user.setFullName(ValidationUtil.truncate(fullName.trim(), 100));
        user.setCompany(ValidationUtil.truncate(company.trim(), 120));
        user.setCity(ValidationUtil.truncate(city.trim(), 60));
        user.setLicenseNo(ValidationUtil.truncate(ValidationUtil.clean(licenseNo), 40));
        userDAO.save(user);
        return user;
    }
}
