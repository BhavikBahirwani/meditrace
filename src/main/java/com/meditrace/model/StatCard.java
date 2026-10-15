package com.meditrace.model;

/**
 * One number tile on a dashboard ("Packs in stock: 10").
 */
public class StatCard {

    private final String label;
    private final long value;
    private final String hint;
    /** "alert" highlights the tile (e.g. suspicious scans > 0); empty otherwise. */
    private final String tone;

    public StatCard(String label, long value, String hint) {
        this(label, value, hint, "");
    }

    public StatCard(String label, long value, String hint, String tone) {
        this.label = label;
        this.value = value;
        this.hint = hint;
        this.tone = tone;
    }

    public String getLabel() {
        return label;
    }

    public long getValue() {
        return value;
    }

    public String getHint() {
        return hint;
    }

    public String getTone() {
        return tone;
    }
}
