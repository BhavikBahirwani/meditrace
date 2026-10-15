package com.meditrace.model;

/**
 * Factory Method: creates the correct {@link User} subclass for a {@link Role}.
 * Used by {@code UserDAO} when it turns a database row into an object.
 */
public final class UserFactory {

    private UserFactory() {
        // no instances
    }

    public static User create(Role role) {
        // Switch expression (Java 14+): the compiler checks that every enum constant is handled.
        return switch (role) {
            case MANUFACTURER -> new Manufacturer();
            case DISTRIBUTOR -> new Distributor();
            case PHARMACY -> new Pharmacy();
        };
    }
}
