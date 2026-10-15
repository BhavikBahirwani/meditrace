package com.meditrace.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A deliberately tiny JSON helper for flat block payloads (no external library needed).
 * <p>
 * Key order is preserved ({@link LinkedHashMap}) so the same data always produces the
 * same JSON text - important, because the text is hashed into the block.
 * <pre>
 * String json = JsonUtil.object().put("type", "REGISTER").put("quantity", 12).toJson();
 * String type = JsonUtil.getString(json, "type");   // "REGISTER"
 * </pre>
 */
public final class JsonUtil {

    private JsonUtil() {
        // no instances
    }

    /** Starts building a JSON object. */
    public static Builder object() {
        return new Builder();
    }

    /**
     * Reads a top-level string or number value from flat JSON produced by {@link Builder}.
     *
     * @return the value as text, or {@code null} if the key is absent or JSON null
     */
    public static String getString(String json, String key) {
        if (json == null || key == null) {
            return null;
        }
        Pattern p = Pattern.compile("\"" + Pattern.quote(key)
                + "\"\\s*:\\s*(\"((?:[^\"\\\\]|\\\\.)*)\"|(-?[0-9.]+)|true|false|null)");
        Matcher m = p.matcher(json);
        if (!m.find()) {
            return null;
        }
        if (m.group(2) != null) {
            return unescape(m.group(2));
        }
        if (m.group(3) != null) {
            return m.group(3);
        }
        String literal = m.group(1);
        return "null".equals(literal) ? null : literal;
    }

    /** Escapes a Java string for use inside JSON double quotes. */
    static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    private static String unescape(String s) {
        return s.replace("\\\"", "\"").replace("\\n", "\n").replace("\\r", "\r")
                .replace("\\t", "\t").replace("\\\\", "\\");
    }

    /** Fluent builder for a flat JSON object (string, number, boolean or null values). */
    public static final class Builder {

        private final Map<String, Object> values = new LinkedHashMap<>();

        public Builder put(String key, Object value) {
            values.put(key, value);
            return this;
        }

        public String toJson() {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> e : values.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append('"').append(escape(e.getKey())).append("\":");
                Object v = e.getValue();
                if (v == null) {
                    sb.append("null");
                } else if (v instanceof Number || v instanceof Boolean) {
                    sb.append(v);
                } else {
                    sb.append('"').append(escape(v.toString())).append('"');
                }
            }
            return sb.append('}').toString();
        }
    }
}
