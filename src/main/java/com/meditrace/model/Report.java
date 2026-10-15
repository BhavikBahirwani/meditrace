package com.meditrace.model;

import com.meditrace.util.DateUtil;

import java.time.LocalDateTime;

/**
 * A customer's "I think this is fake" complaint (one row of {@code reports}).
 * The reporting form and city heat-map are on the roadmap; Review 1 ships the table,
 * DAO and seed data.
 */
public class Report {

    /** Workflow state of a report. Matches the {@code reports.status} ENUM. */
    public enum Status { OPEN, INVESTIGATING, CLOSED }

    private long reportId;
    private String packCode;
    private Long scanId;
    private String reporterName;
    private String reporterPhone;
    private String city;
    private String description;
    private Status status = Status.OPEN;
    private LocalDateTime createdAt;

    public String getCreatedAtText() {
        return DateUtil.formatDateTime(createdAt);
    }

    // ---------------------------------------------------------------- getters / setters

    public long getReportId() {
        return reportId;
    }

    public void setReportId(long reportId) {
        this.reportId = reportId;
    }

    public String getPackCode() {
        return packCode;
    }

    public void setPackCode(String packCode) {
        this.packCode = packCode;
    }

    public Long getScanId() {
        return scanId;
    }

    public void setScanId(Long scanId) {
        this.scanId = scanId;
    }

    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    public String getReporterPhone() {
        return reporterPhone;
    }

    public void setReporterPhone(String reporterPhone) {
        this.reporterPhone = reporterPhone;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
