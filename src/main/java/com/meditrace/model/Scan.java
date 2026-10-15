package com.meditrace.model;

import com.meditrace.util.DateUtil;

import java.time.LocalDateTime;

/**
 * Audit log of one customer verification (one row of {@code scans}).
 * Unknown codes are logged too (packId null) - repeated scans of unknown codes from one
 * area are an early counterfeit signal.
 */
public class Scan {

    private long scanId;
    private String packCode;
    private Long packId;
    private VerificationStatus result;
    private String city;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime scannedAt;

    public Scan() {
    }

    public Scan(String packCode, Long packId, VerificationStatus result, String ipAddress, String userAgent) {
        this.packCode = packCode;
        this.packId = packId;
        this.result = result;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    public String getScannedAtText() {
        return DateUtil.formatDateTime(scannedAt);
    }

    // ---------------------------------------------------------------- getters / setters

    public long getScanId() {
        return scanId;
    }

    public void setScanId(long scanId) {
        this.scanId = scanId;
    }

    public String getPackCode() {
        return packCode;
    }

    public void setPackCode(String packCode) {
        this.packCode = packCode;
    }

    public Long getPackId() {
        return packId;
    }

    public void setPackId(Long packId) {
        this.packId = packId;
    }

    public VerificationStatus getResult() {
        return result;
    }

    public void setResult(VerificationStatus result) {
        this.result = result;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public LocalDateTime getScannedAt() {
        return scannedAt;
    }

    public void setScannedAt(LocalDateTime scannedAt) {
        this.scannedAt = scannedAt;
    }
}
