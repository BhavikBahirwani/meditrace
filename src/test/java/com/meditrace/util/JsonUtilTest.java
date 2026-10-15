package com.meditrace.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Tests the minimal JSON builder / reader used for block payloads. */
class JsonUtilTest {

    @Test
    void buildsAndReadsBackValues() {
        String json = JsonUtil.object()
                .put("type", "HANDOVER")
                .put("quantity", 12)
                .put("to", null)
                .put("note", "Sharma \"Medicals\"")
                .toJson();
        assertEquals("{\"type\":\"HANDOVER\",\"quantity\":12,\"to\":null,\"note\":\"Sharma \\\"Medicals\\\"\"}", json);
        assertEquals("HANDOVER", JsonUtil.getString(json, "type"));
        assertEquals("12", JsonUtil.getString(json, "quantity"));
        assertEquals("Sharma \"Medicals\"", JsonUtil.getString(json, "note"));
        assertNull(JsonUtil.getString(json, "to"));
        assertNull(JsonUtil.getString(json, "missing"));
    }
}
