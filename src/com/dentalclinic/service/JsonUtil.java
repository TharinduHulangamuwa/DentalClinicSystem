package com.dentalclinic.service;

import com.dentalclinic.model.Appointment;
import com.dentalclinic.model.User;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON reader and writer for this application's flat data.
 *
 * DESIGN DECISION: a library such as Jackson or Gson would add a dependency
 * to handle something this application only needs in one simple form - flat
 * objects whose values are strings and numbers. Roughly 150 lines by hand
 * keeps the build dependency-free and keeps every line explainable.
 *
 * LIMITATION, stated honestly: this parser handles flat objects and arrays of
 * flat objects. It does not support nested objects, arrays as values, or
 * unicode escape sequences. A production system would use a full library.
 *
 * @author [Your Name]
 */
public final class JsonUtil {

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

    /** Serialises one appointment. */
    public static String toJson(Appointment a) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("appointmentNo",   a.getAppointmentNo());
        map.put("patientName",     a.getPatientName());
        map.put("address",         a.getAddress());
        map.put("contactNo",       a.getContactNo());
        map.put("dentistName",     a.getDentistName());
        map.put("treatmentType",   a.getTreatmentType());
        map.put("appointmentDate", a.getAppointmentDate());
        map.put("appointmentTime", a.getAppointmentTime());
        return toJson(map);
    }

    /** Serialises one user. The password is never included. */
    public static String toJson(User u) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("userId",   u.getUserId());
        map.put("username", u.getUsername());
        map.put("fullName", u.getFullName());
        map.put("role",     u.getRole());
        return toJson(map);
    }

    /** Serialises a key/value map. Numbers and booleans are left unquoted. */
    public static String toJson(Map<String, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, ?> e : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("\"").append(escape(e.getKey())).append("\":");
            Object v = e.getValue();
            if (v == null) {
                sb.append("\"\"");
            } else if (v instanceof Number || v instanceof Boolean) {
                sb.append(v);
            } else {
                sb.append("\"").append(escape(String.valueOf(v))).append("\"");
            }
        }
        return sb.append("}").toString();
    }

    /** Serialises a list of appointments as a JSON array. */
    public static String appointmentsToJson(List<Appointment> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(toJson(list.get(i)));
        }
        return sb.append("]").toString();
    }

    /** Serialises a list of string arrays as an array of objects. */
    public static String rowsToJson(String[] columns, List<String[]> rows) {
        StringBuilder sb = new StringBuilder("[");
        for (int r = 0; r < rows.size(); r++) {
            if (r > 0) {
                sb.append(",");
            }
            Map<String, Object> map = new LinkedHashMap<>();
            String[] row = rows.get(r);
            for (int c = 0; c < columns.length && c < row.length; c++) {
                map.put(columns[c], row[c]);
            }
            sb.append(toJson(map));
        }
        return sb.append("]").toString();
    }

    /** A short server message, for errors and confirmations. */
    public static String message(String key, String value) {
        return "{\"" + escape(key) + "\":\"" + escape(value) + "\"}";
    }

    // =================================================================
    // READING
    // =================================================================

    /** Parses a flat JSON object into a map of string values. */
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
            while (i < s.length()
                   && (Character.isWhitespace(s.charAt(i)) || s.charAt(i) == ',')) {
                i++;
            }
            if (i >= s.length() || s.charAt(i) != '"') {
                break;
            }
            int keyEnd = closingQuote(s, i + 1);
            if (keyEnd < 0) {
                break;
            }
            String key = unescape(s.substring(i + 1, keyEnd));
            i = keyEnd + 1;

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

            String value;
            if (s.charAt(i) == '"') {
                int valEnd = closingQuote(s, i + 1);
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
                    i++;
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

    /**
     * Parses a JSON array of plain strings, for example the reminder
     * messages endpoint. Honours escaped quotes inside each string, which a
     * naive split on "," would break on.
     */
    public static List<String> parseStringArray(String json) {
        List<String> list = new ArrayList<>();
        if (json == null) {
            return list;
        }
        String s = json.trim();
        if (!s.startsWith("[") || !s.endsWith("]")) {
            return list;
        }
        s = s.substring(1, s.length() - 1);

        int i = 0;
        while (i < s.length()) {
            while (i < s.length()
                   && (Character.isWhitespace(s.charAt(i)) || s.charAt(i) == ',')) {
                i++;
            }
            if (i >= s.length() || s.charAt(i) != '"') {
                break;
            }
            int end = closingQuote(s, i + 1);
            if (end < 0) {
                break;
            }
            list.add(unescape(s.substring(i + 1, end)));
            i = end + 1;
        }
        return list;
    }

    /** Serialises a list of plain strings as a JSON array. */
    public static String stringsToJson(List<String> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("\"").append(escape(values.get(i))).append("\"");
        }
        return sb.append("]").toString();
    }

    /** Rebuilds an Appointment from a parsed JSON object. */
    public static Appointment toAppointment(Map<String, String> map) {
        return new Appointment(
                map.get("appointmentNo"), map.get("patientName"),
                map.get("address"),       map.get("contactNo"),
                map.get("dentistName"),   map.get("treatmentType"),
                map.get("appointmentDate"), map.get("appointmentTime"));
    }

    /** Rebuilds a User from a parsed JSON object. */
    public static User toUser(Map<String, String> map) {
        int id = 0;
        try {
            id = Integer.parseInt(map.get("userId"));
        } catch (NumberFormatException ignored) {
            // an absent or malformed id leaves the default of 0
        }
        return new User(id, map.get("username"), map.get("fullName"), map.get("role"));
    }

    // ---------------- helpers ----------------

    private static int closingQuote(String s, int from) {
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
