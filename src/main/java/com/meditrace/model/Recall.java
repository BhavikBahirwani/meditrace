package com.meditrace.model;

import com.meditrace.util.DateUtil;

import java.time.LocalDate;

/**
 * A batch withdrawn from the market (one row of {@code recalls}). Classes follow the usual
 * regulator convention: Class I = serious health risk, Class III = unlikely to cause harm.
 */
public class Recall {

    /** Matches the {@code recalls.severity} ENUM. */
    public enum Severity { CLASS_I, CLASS_II, CLASS_III }

    private long recallId;
    private long batchId;
    private long issuedBy;
    private String reason;
    private Severity severity;
    private LocalDate recallDate;
    private boolean active = true;

    public String getRecallDateText() {
        return DateUtil.formatDate(recallDate);
    }

    /** e.g. "Class II" */
    public String getSeverityText() {
        return severity == null ? "" : "Class " + severity.name().substring("CLASS_".length());
    }

    // ---------------------------------------------------------------- getters / setters

    public long getRecallId() {
        return recallId;
    }

    public void setRecallId(long recallId) {
        this.recallId = recallId;
    }

    public long getBatchId() {
        return batchId;
    }

    public void setBatchId(long batchId) {
        this.batchId = batchId;
    }

    public long getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(long issuedBy) {
        this.issuedBy = issuedBy;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public LocalDate getRecallDate() {
        return recallDate;
    }

    public void setRecallDate(LocalDate recallDate) {
        this.recallDate = recallDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
