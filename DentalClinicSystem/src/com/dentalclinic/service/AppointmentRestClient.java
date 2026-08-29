package com.dentalclinic.service;

import com.dentalclinic.model.Appointment;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CLIENT SIDE of the distributed system.
 *
 * Calls the REST endpoints exposed by AppointmentServer using HttpURLConnection.
 * Deliberately exposes the SAME method names as AppointmentDAO, so the
 * controller can be switched from direct JDBC to remote web service calls by
 * changing a single line. That is the DAO abstraction paying off.
 *
 * This class contains no JDBC and no database credentials. If the client is
 * installed on a receptionist's laptop, that laptop never needs MySQL
 * access - a real security improvement worth stating in the report.
 */
public class AppointmentRestClient {

    /** Change to the server's IP to run the client on a different machine. */
    private final String baseUrl;

    public AppointmentRestClient() {
        this("http://localhost:" + AppointmentServer.PORT);
    }

    public AppointmentRestClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    // =================================================================
    // READ
    // =================================================================
    public List<Appointment> findAll() throws Exception {
        String json = get("/api/appointments");
        List<Appointment> list = new ArrayList<>();
        for (Map<String, String> row : JsonUtil.parseArray(json)) {
            list.add(JsonUtil.toAppointment(row));
        }
        return list;
    }

    public Appointment findByNo(String appointmentNo) throws Exception {
        try {
            String json = get("/api/appointments/" + appointmentNo);
            Map<String, String> map = JsonUtil.parseObject(json);
            if (map.containsKey("error") || !map.containsKey("appointmentNo")) {
                return null;
            }
            return JsonUtil.toAppointment(map);
        } catch (NotFoundException e) {
            return null;
        }
    }

    public boolean slotTaken(String dentist, String date, String time) throws Exception {
        String query = "?dentist=" + enc(dentist) + "&date=" + enc(date) + "&time=" + enc(time);
        String json = get("/api/appointments/check" + query);
        return "true".equals(JsonUtil.parseObject(json).get("taken"));
    }

    public Map<String, Double> findTreatments() throws Exception {
        String json = get("/api/treatments");
        Map<String, Double> treatments = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : JsonUtil.parseObject(json).entrySet()) {
            try {
                treatments.put(e.getKey(), Double.parseDouble(e.getValue()));
            } catch (NumberFormatException ignored) {
                treatments.put(e.getKey(), 0.0);
            }
        }
        return treatments;
    }

    public double findTreatmentCost(String treatmentType) throws Exception {
        Double cost = findTreatments().get(treatmentType);
        return cost == null ? 0.0 : cost;
    }

    // =================================================================
    // CREATE
    // =================================================================
    public boolean save(Appointment a) throws Exception {
        HttpURLConnection conn = open("/api/appointments", "POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

        byte[] body = JsonUtil.toJson(a).getBytes(StandardCharsets.UTF_8);
        try (OutputStream out = conn.getOutputStream()) {
            out.write(body);
        }

        int status = conn.getResponseCode();
        if (status == 201) {
            return true;
        }

        String error = JsonUtil.parseObject(readStream(conn.getErrorStream())).get("error");
        if (status == 409) {
            throw new ConflictException(error == null ? "Conflict" : error);
        }
        throw new Exception(error == null ? "Server returned status " + status : error);
    }

    // =================================================================
    // low-level HTTP
    // =================================================================
    private String get(String path) throws Exception {
        HttpURLConnection conn = open(path, "GET");
        int status = conn.getResponseCode();

        if (status == 404) {
            throw new NotFoundException(path);
        }
        if (status >= 400) {
            throw new Exception("Server returned status " + status);
        }
        return readStream(conn.getInputStream());
    }

    private HttpURLConnection open(String path, String method) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + path).openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("Accept", "application/json");
        return conn;
    }

    private String readStream(InputStream in) throws Exception {
        if (in == null) {
            return "";
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toString("UTF-8");
        } finally {
            in.close();
        }
    }

    private String enc(String value) throws Exception {
        return URLEncoder.encode(value == null ? "" : value, "UTF-8");
    }

    /** Thrown when the server reports 404. */
    public static class NotFoundException extends Exception {
        public NotFoundException(String message) { super(message); }
    }

    /** Thrown when the server reports 409, for example a double booking. */
    public static class ConflictException extends Exception {
        public ConflictException(String message) { super(message); }
    }
}
