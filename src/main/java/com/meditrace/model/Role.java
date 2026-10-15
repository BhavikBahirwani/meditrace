package com.meditrace.model;

import java.util.Locale;
import java.util.Optional;

/**
 * The three kinds of supply-chain participant. Matches the {@code users.role} ENUM column.
 * <p>
 * An enum with fields and methods: each constant carries its display name, URL slug
 * (used for role-based URLs like {@code /manufacturer/dashboard}) and its UI area colour.
 */
public enum Role {

    MANUFACTURER("Manufacturer", "manufacturer", "#1D4ED8"),
    DISTRIBUTOR("Distributor", "distributor", "#0E7490"),
    PHARMACY("Pharmacy", "pharmacy", "#A21CAF");

    private final String displayName;
    private final String slug;
    private final String areaColour;

    Role(String displayName, String slug, String areaColour) {
        this.displayName = displayName;
        this.slug = slug;
        this.areaColour = areaColour;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Lower-case name used in URLs and CSS classes, e.g. "pharmacy". */
    public String getSlug() {
        return slug;
    }

    public String getAreaColour() {
        return areaColour;
    }

    /** Context-relative URL of this role's dashboard. */
    public String getDashboardPath() {
        return "/" + slug + "/dashboard";
    }

    /** Parses a form / DB value safely ("pharmacy", "PHARMACY" ...). */
    public static Optional<Role> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Role.valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** Finds the role owning a URL slug, e.g. "distributor" to DISTRIBUTOR. */
    public static Optional<Role> fromSlug(String slug) {
        for (Role r : values()) {
            if (r.slug.equals(slug)) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }
}
