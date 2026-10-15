package com.meditrace.model;

/**
 * A retail chemist that sells packs to customers (e.g. Sharma Medicals, Ghaziabad).
 */
public class Pharmacy extends User {

    private static final long serialVersionUID = 1L;

    @Override
    public Role getRole() {
        return Role.PHARMACY;
    }

    @Override
    public String getResponsibility() {
        return "Receives stock from distributors and sells verified packs to customers.";
    }

    @Override
    public String getJourneyLabel() {
        return "Received by pharmacy";
    }

    @Override
    public boolean canSellToCustomer() {
        return true;
    }
}
