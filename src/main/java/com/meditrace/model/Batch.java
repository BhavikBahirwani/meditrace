package com.meditrace.model;

import com.meditrace.blockchain.Hashable;
import com.meditrace.util.DateUtil;
import com.meditrace.util.HashUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * One production batch of a medicine (one row of {@code batches}), e.g.
 * B1023 - Paracetamol 500 mg, MRP ₹25.00.
 * <p>
 * Implements {@link Hashable}: its fingerprint covers the printed details AND every pack
 * code in the batch. The fingerprint is sealed into the REGISTER block, so if anyone later
 * edits the expiry date, MRP, or slips an extra pack code into the database, verification
 * reports the batch as <b>Tampered</b>.
 */
public class Batch implements Hashable {

    private long batchId;
    private String batchNo;
    private String medicineName;
    private String strength;
    private String dosageForm;
    private long manufacturerId;
    private LocalDate mfgDate;
    private LocalDate expiryDate;
    private BigDecimal mrp;               // BigDecimal: never use double for money
    private int packCount;
    private LocalDateTime createdAt;
    /** Pack codes of this batch; loaded when the fingerprint is needed. */
    private List<String> packCodes = new ArrayList<>();

    /** {@inheritDoc} SHA-256 over the batch details and the sorted list of pack codes. */
    @Override
    public String computeHash() {
        List<String> sorted = new ArrayList<>(packCodes);
        sorted.sort(null);                               // natural (alphabetical) order
        String packsDigest = HashUtil.sha256Hex(String.join(",", sorted));
        String mrpText = (mrp == null) ? "" : mrp.setScale(2).toPlainString();
        return HashUtil.sha256Hex(String.join("|",
                batchNo, medicineName, strength, dosageForm,
                String.valueOf(manufacturerId),
                String.valueOf(mfgDate), String.valueOf(expiryDate),
                mrpText, String.valueOf(packCount), packsDigest));
    }

    /** @return true if the batch is past its expiry date today */
    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now(DateUtil.IST));
    }

    /** e.g. "Paracetamol 500 mg" */
    public String getProductName() {
        return medicineName + " " + strength;
    }

    public String getMfgDateText() {
        return DateUtil.formatMonthYear(mfgDate);
    }

    public String getExpiryDateText() {
        return DateUtil.formatMonthYear(expiryDate);
    }

    /** e.g. "₹25.00" */
    public String getMrpText() {
        return mrp == null ? "" : "₹" + mrp.setScale(2).toPlainString();
    }

    // ---------------------------------------------------------------- getters / setters

    public long getBatchId() {
        return batchId;
    }

    public void setBatchId(long batchId) {
        this.batchId = batchId;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public void setMedicineName(String medicineName) {
        this.medicineName = medicineName;
    }

    public String getStrength() {
        return strength;
    }

    public void setStrength(String strength) {
        this.strength = strength;
    }

    public String getDosageForm() {
        return dosageForm;
    }

    public void setDosageForm(String dosageForm) {
        this.dosageForm = dosageForm;
    }

    public long getManufacturerId() {
        return manufacturerId;
    }

    public void setManufacturerId(long manufacturerId) {
        this.manufacturerId = manufacturerId;
    }

    public LocalDate getMfgDate() {
        return mfgDate;
    }

    public void setMfgDate(LocalDate mfgDate) {
        this.mfgDate = mfgDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public BigDecimal getMrp() {
        return mrp;
    }

    public void setMrp(BigDecimal mrp) {
        this.mrp = mrp;
    }

    public int getPackCount() {
        return packCount;
    }

    public void setPackCount(int packCount) {
        this.packCount = packCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<String> getPackCodes() {
        return packCodes;
    }

    public void setPackCodes(List<String> packCodes) {
        this.packCodes = (packCodes == null) ? new ArrayList<>() : new ArrayList<>(packCodes);
    }

    @Override
    public String toString() {
        return "Batch{" + batchNo + ", " + getProductName() + ", packs=" + packCount + "}";
    }
}
