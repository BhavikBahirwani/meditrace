package com.meditrace.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Everything the verify page needs to render one scan result. Built by
 * {@code VerificationService}; read by {@code verify.jsp} through its getters.
 */
public class VerificationResult {

    private final String packCode;
    private VerificationStatus status;
    private String detail;
    private Pack pack;
    private Batch batch;
    private User manufacturer;
    private User currentHolder;
    private Recall recall;
    private long previousScans;
    private final List<JourneyStep> journey = new ArrayList<>();

    public VerificationResult(String packCode, VerificationStatus status) {
        this.packCode = packCode;
        this.status = status;
        this.detail = status.getDefaultMessage();
    }

    public void addStep(JourneyStep step) {
        journey.add(step);
    }

    // ---------------------------------------------------------------- getters / setters

    public String getPackCode() {
        return packCode;
    }

    public VerificationStatus getStatus() {
        return status;
    }

    public void setStatus(VerificationStatus status) {
        this.status = status;
    }

    /** Human explanation of the status (e.g. which record failed the tamper check). */
    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public Pack getPack() {
        return pack;
    }

    public void setPack(Pack pack) {
        this.pack = pack;
    }

    public Batch getBatch() {
        return batch;
    }

    public void setBatch(Batch batch) {
        this.batch = batch;
    }

    public User getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(User manufacturer) {
        this.manufacturer = manufacturer;
    }

    public User getCurrentHolder() {
        return currentHolder;
    }

    public void setCurrentHolder(User currentHolder) {
        this.currentHolder = currentHolder;
    }

    public Recall getRecall() {
        return recall;
    }

    public void setRecall(Recall recall) {
        this.recall = recall;
    }

    public long getPreviousScans() {
        return previousScans;
    }

    public void setPreviousScans(long previousScans) {
        this.previousScans = previousScans;
    }

    public List<JourneyStep> getJourney() {
        return Collections.unmodifiableList(journey);
    }

    public boolean isFound() {
        return status != VerificationStatus.NOT_FOUND;
    }
}
