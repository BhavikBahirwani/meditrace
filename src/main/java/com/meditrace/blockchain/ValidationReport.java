package com.meditrace.blockchain;

import com.meditrace.util.DateUtil;

import java.time.LocalDateTime;

/**
 * Immutable result of one full-chain verification run (shown as the
 * "chain verified" banner on every dashboard).
 * <p>
 * Written as a plain class with getters (not a record) so JSP Expression Language can
 * read its properties on Tomcat 10.1.
 */
public final class ValidationReport {

    private final boolean valid;
    private final int blocksChecked;
    private final int brokenAtIndex;   // -1 when the chain is valid
    private final String message;
    private final LocalDateTime checkedAt;

    public ValidationReport(boolean valid, int blocksChecked, int brokenAtIndex,
                            String message, LocalDateTime checkedAt) {
        this.valid = valid;
        this.blocksChecked = blocksChecked;
        this.brokenAtIndex = brokenAtIndex;
        this.message = message;
        this.checkedAt = checkedAt;
    }

    /** Factory for a successful run. */
    public static ValidationReport ok(int blocksChecked) {
        return new ValidationReport(true, blocksChecked, -1,
                "All " + blocksChecked + " blocks link correctly - hashes and links intact.",
                LocalDateTime.now());
    }

    /** Factory for a failed run. */
    public static ValidationReport broken(int blocksChecked, int brokenAtIndex, String reason) {
        return new ValidationReport(false, blocksChecked, brokenAtIndex, reason, LocalDateTime.now());
    }

    public boolean isValid() {
        return valid;
    }

    public int getBlocksChecked() {
        return blocksChecked;
    }

    public int getBrokenAtIndex() {
        return brokenAtIndex;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }

    /** e.g. "15 Oct 2026, 08:30 PM" - pre-formatted for the JSP. */
    public String getCheckedAtText() {
        return DateUtil.formatDateTime(checkedAt);
    }

    @Override
    public String toString() {
        return (valid ? "VALID: " : "INVALID: ") + message;
    }
}
