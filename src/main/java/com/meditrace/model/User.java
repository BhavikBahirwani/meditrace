package com.meditrace.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Abstract base class for every supply-chain participant (one row of the {@code users} table).
 * <p>
 * <b>Abstraction:</b> a plain "User" never exists - every account is a
 * {@link Manufacturer}, {@link Distributor} or {@link Pharmacy}. Shared state (name,
 * company, city ...) lives here; role-specific behaviour is declared {@code abstract}
 * and supplied by each subclass.
 * <p>
 * <b>Polymorphism:</b> code such as the verify page's journey timeline holds a
 * {@code User} reference and calls {@link #getJourneyLabel()} - the JVM picks the right
 * subclass method at runtime (dynamic dispatch).
 * <p>
 * <b>Encapsulation:</b> fields are private and reached through getters/setters.
 * It is {@link Serializable} because it is stored in the HttpSession.
 */
public abstract class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private long userId;
    private String username;
    private String email;
    /** PBKDF2 hash - never the plain password. Marked transient so it is never serialised with the session. */
    private transient String passwordHash;
    private String fullName;
    private String company;
    private String city;
    private String licenseNo;
    private LocalDateTime createdAt;

    // ---------------------------------------------------------------- abstract behaviour

    /** @return which kind of participant this is */
    public abstract Role getRole();

    /** Short description of what this participant does in the supply chain. */
    public abstract String getResponsibility();

    /** Label used for this participant's step on the customer's journey timeline. */
    public abstract String getJourneyLabel();

    // ---------------------------------------------------------------- overridable permissions

    /** Only manufacturers create batches; {@link Manufacturer} overrides this. */
    public boolean canRegisterBatch() {
        return false;
    }

    /** Only pharmacies sell to customers; {@link Pharmacy} overrides this. */
    public boolean canSellToCustomer() {
        return false;
    }

    // ---------------------------------------------------------------- concrete helpers

    /** e.g. "Sharma Medicals, Ghaziabad" */
    public String getDisplayName() {
        return company + ", " + city;
    }

    public String getDashboardPath() {
        return getRole().getDashboardPath();
    }

    // ---------------------------------------------------------------- getters / setters

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getLicenseNo() {
        return licenseNo;
    }

    public void setLicenseNo(String licenseNo) {
        this.licenseNo = licenseNo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // ---------------------------------------------------------------- Object overrides

    /** Two User objects are equal when they represent the same database row. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User)) {
            return false;
        }
        User other = (User) o;
        return userId == other.userId && getRole() == other.getRole();
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, getRole());
    }

    @Override
    public String toString() {
        return getRole().getDisplayName() + "{" + username + ", " + getDisplayName() + "}";
    }
}
