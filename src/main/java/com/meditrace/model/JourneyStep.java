package com.meditrace.model;

/**
 * One stop on the pack's journey timeline shown to the customer
 * (e.g. "Received by distributor - ABC Distributors, Delhi - 18 Jul 2026 - block #5").
 */
public class JourneyStep {

    private final String label;
    private final String actor;
    private final String city;
    private final String timeText;
    private final String roleSlug;      // drives the dot colour: manufacturer / distributor / pharmacy / customer
    private final Integer blockIndex;   // null when not yet sealed (or for the "sold" step)
    private final String shortHash;
    private final boolean verified;

    public JourneyStep(String label, String actor, String city, String timeText, String roleSlug,
                       Integer blockIndex, String shortHash, boolean verified) {
        this.label = label;
        this.actor = actor;
        this.city = city;
        this.timeText = timeText;
        this.roleSlug = roleSlug;
        this.blockIndex = blockIndex;
        this.shortHash = shortHash;
        this.verified = verified;
    }

    public String getLabel() {
        return label;
    }

    public String getActor() {
        return actor;
    }

    public String getCity() {
        return city;
    }

    public String getTimeText() {
        return timeText;
    }

    public String getRoleSlug() {
        return roleSlug;
    }

    public Integer getBlockIndex() {
        return blockIndex;
    }

    public String getShortHash() {
        return shortHash;
    }

    /** True when the step's block and record fingerprints checked out. */
    public boolean isVerified() {
        return verified;
    }
}
