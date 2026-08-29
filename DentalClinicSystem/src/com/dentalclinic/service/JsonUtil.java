package com.dentalclinic.service;

import com.dentalclinic.model.Appointment;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON reader and writer for this application's flat data.
 *
 * DESIGN DECISION: a third-party library such as Jackson or Gson would add
 * a dependency to demonstrate something the application only needs in one
 * simple form - flat objects whose values are all strings. Writing roughly
 * 100 lines by hand keeps the build dependency-free and keeps every line
 * explainable.
 *
 * Limitation, stated honestly in the report: this parser handles flat
 * objects and arrays of flat objects only. It does not support nested
 * objects, arrays as values, or unicode escape sequences. A production
 * system would use a full library.
 */
public class JsonUtil {

    private JsonUtil() { }

    // =================================================================
    // WRITING
    // =================================================================

    /** Escapes the characters that would otherwise break a JSON string. */
    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:   sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Serialises one appointment as a JSON object. */
    public static String toJson(Appointment a) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        appendField(sb, "appointmentNo",   a.getAppointmentNo(),   true);
        appendField(sb, "patientName",     a.getPatientName(),     true);
        appendField(sb, "address",         a.getAddress(),         true);
        appendField(sb, "contactNo",       a.getContactNo(),       true);
        appendField(sb, "dentistName",     a.getDentistName(),     true);
        appendField(sb, "treatmentType",   a.getTreatmentType(),   true);
        appendField(sb, "appointmentDate", a.getAppointmentDate(), true);
        appendField(sb, "appointmentTime", a.getAppointmentTime(), false);
        sb.append("}");
        return sb.toString();
    }

    /** Serialises a list of appointments as a JSON array. */
    public static String toJson(List<Appointment> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(toJson(list.get(i)));
        }
        return sb.append("]").toString();
    }

    /** Serialises a simple key/value map as a JSON object. */
    public static String toJson(Map<String, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("\"").append(escape(entry.getKey())).append("\":");
            Object value = entry.getValue();
            if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else {
                sb.append("\"").append(escape(String.valueOf(value))).append("\"");
            }
        }
        return sb.append("}").toString();
    }

    /** Convenience for short server messages such as errors. */
    public static String message(String key, String value) {
        return "{\"" + escape(key) + "\":\"" + escape(value) + "\"}";
    }

    private static void appendField(StringBuilder sb, String key,
                                    String value, boolean comma) {
        sb.append("\"").append(key).append("\":\"").append(escape(value)).append("\"");
        if (comma) {
            sb.append(",");
        }
    }

    // =================================================================
    // READING
    // =================================================================

    /**
     * Parses a flat JSON object into a map of string values.
     * Numeric and boolean values are returned as their text form.
     */
    public static Map<String, String> parseObject(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        if (json == null) {
            return map;
        }
        String s = json.trim();
        if (!s.startsWith("{") || !s.endsWith("}")) {
            return map;
        }
        s = s.substring(1, s.length() - 1);

        int i = 0;
        while (i < s.length()) {
            // skip whitespace and separators
            while (i < s.length() && (Character.isWhitespace(s.charAt(i)) || s.charAt(i) == ',')) {
                i++;
            }
            if (i >= s.length() || s.charAt(i) != '"') {
                break;
            }
            // read the key
            int keyEnd = findClosingQuote(s, i + 1);
            if (keyEnd < 0) {
                break;
            }
            String key = unescape(s.substring(i + 1, keyEnd));
            i = keyEnd + 1;

            // skip to the colon
            while (i < s.length() && s.charAt(i) != ':') {
                i++;
            }
            i++;
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
                i++;
            }
            if (i >= s.length()) {
                break;
            }

            // read the value
            String value;
            if (s.charAt(i) == '"') {
                int valEnd = findClosingQuote(s, i + 1);
                if (valEnd < 0) {
                    break;
                }
                value = unescape(s.substring(i + 1, valEnd));
                i = valEnd + 1;
            } else {
                int valEnd = i;
                while (valEnd < s.length() && s.charAt(valEnd) != ',') {
                    valEnd++;
                }
                value = s.substring(i, valEnd).trim();
                i = valEnd;
            }
            map.put(key, value);
        }
        return map;
    }

    /** Parses a JSON array of flat objects. */
    public static List<Map<String, String>> parseArray(String json) {
        List<Map<String, String>> list = new ArrayList<>();
        if (json == null) {
            return list;
        }
        String s = json.trim();
        if (!s.startsWith("[") || !s.endsWith("]")) {
            return list;
        }
        s = s.substring(1, s.length() - 1);

        int depth = 0;
        int start = -1;
        boolean inString = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (inString) {
                if (c == '\\') {
                    i++;                       // skip the escaped character
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    list.add(parseObject(s.substring(start, i + 1)));
                    start = -1;
                }
            }
        }
        return list;
    }

    /** Rebuilds an Appointment from a parsed JSON object. */
    public static Appointment toAppointment(Map<String, String> map) {
        return new Appointment(
                map.get("appointmentNo"),
                map.get("patientName"),
                map.get("address"),
                map.get("contactNo"),
                map.get("dentistName"),
                map.get("treatmentType"),
                map.get("appointmentDate"),
                map.get("appointmentTime"));
    }

    // ---- helpers ----

    /** Finds the closing quote of a string starting at index from, honouring escapes. */
    private static int findClosingQuote(String s, int from) {
        for (int i = from; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == '"') {
                return i;
            }
        }
        return -1;
    }

    private static String unescape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(++i);
                switch (next) {
                    case 'n':  sb.append('\n'); break;
                    case 'r':  sb.append('\r'); break;
                    case 't':  sb.append('\t'); break;
                    case '"':  sb.append('"');  break;
                    case '\\': sb.append('\\'); break;
                    default:   sb.append(next);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
