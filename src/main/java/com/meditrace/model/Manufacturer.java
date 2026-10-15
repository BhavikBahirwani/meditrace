package com.meditrace.model;

/**
 * A pharmaceutical company that produces medicine batches (e.g. XYZ Pharma, Baddi).
 * The only role allowed to register batches and generate pack codes.
 */
public class Manufacturer extends User {

    private static final long serialVersionUID = 1L;

    @Override
    public Role getRole() {
        return Role.MANUFACTURER;
    }

    @Override
    public String getResponsibility() {
        return "Registers batches, prints a unique code on every pack and seals each batch on the blockchain.";
    }

    @Override
    public String getJourneyLabel() {
        return "Manufactured & sealed";
    }

    @Override
    public boolean canRegisterBatch() {
        return true;
    }
}
