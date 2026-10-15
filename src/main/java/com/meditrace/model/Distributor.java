package com.meditrace.model;

/**
 * A wholesaler / stockist that moves stock from manufacturers to pharmacies
 * (e.g. ABC Distributors, Delhi).
 */
public class Distributor extends User {

    private static final long serialVersionUID = 1L;

    @Override
    public Role getRole() {
        return Role.DISTRIBUTOR;
    }

    @Override
    public String getResponsibility() {
        return "Receives batches from manufacturers and hands them over to licensed pharmacies.";
    }

    @Override
    public String getJourneyLabel() {
        return "Received by distributor";
    }
}
