package com.meditrace.model;

import com.meditrace.util.DateUtil;

import java.time.LocalDateTime;

/**
 * One physical strip / bottle / box carrying a unique printed code (one row of {@code packs}).
 */
public class Pack {

    private long packId;
    private String packCode;          // e.g. MT-B1023-7F3A9C
    private long batchId;
    private PackStatus status = PackStatus.IN_CHAIN;
    private Long currentHolderId;     // wrapper type: may be NULL in the database
    private String soldCity;
    private LocalDateTime soldAt;
    private LocalDateTime createdAt;

    public Pack() {
    }

    public Pack(String packCode, long batchId, Long currentHolderId) {
        this.packCode = packCode;
        this.batchId = batchId;
        this.currentHolderId = currentHolderId;
    }

    public boolean isSold() {
        return status == PackStatus.SOLD;
    }

    public String getSoldAtText() {
        return DateUtil.formatDateTime(soldAt);
    }

    // ---------------------------------------------------------------- getters / setters

    public long getPackId() {
        return packId;
    }

    public void setPackId(long packId) {
        this.packId = packId;
    }

    public String getPackCode() {
        return packCode;
    }

    public void setPackCode(String packCode) {
        this.packCode = packCode;
    }

    public long getBatchId() {
        return batchId;
    }

    public void setBatchId(long batchId) {
        this.batchId = batchId;
    }

    public PackStatus getStatus() {
        return status;
    }

    public void setStatus(PackStatus status) {
        this.status = status;
    }

    public Long getCurrentHolderId() {
        return currentHolderId;
    }

    public void setCurrentHolderId(Long currentHolderId) {
        this.currentHolderId = currentHolderId;
    }

    public String getSoldCity() {
        return soldCity;
    }

    public void setSoldCity(String soldCity) {
        this.soldCity = soldCity;
    }

    public LocalDateTime getSoldAt() {
        return soldAt;
    }

    public void setSoldAt(LocalDateTime soldAt) {
        this.soldAt = soldAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Pack{" + packCode + ", " + status + "}";
    }
}
