package com.dentalclinic.service;

import com.dentalclinic.model.Appointment;
import com.dentalclinic.model.User;
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
 * THE CLIENT SIDE of the distributed system.
 *
 * Calls the REST endpoints exposed by ApiServer using HttpURLConnection, and
 * turns the JSON replies back into model objects.
 *
 * This class contains no JDBC and no database credentials. A receptionist's
 * laptop running the client never needs MySQL access at all; it only needs to
 * reach the clinic server over HTTP. That is a real security improvement, and
 * it is worth stating in the report.
 *
 * SESSION HANDLING: after signIn() the token is held here and attached to
 * every later request as an Authorization: Bearer header. That is exactly
 * what a browser does with a session cookie, done explicitly because a
 * desktop client has no cookie jar.
 *
 * @author [Your Name]
 */
public class ApiClient {

    private final String baseUrl;

    /** The session token, set by signIn() or restoreSession(). */
    private String token;

    public ApiClient() {
        this("http://localhost:" + ApiServer.PORT);
    }

    /** Point at another machine to run the client on a different PC. */
    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getToken()            { return token; }
    public void   setToken(String t)    { this.token = t; }
    public boolean hasToken()           { return token != null && !token.isEmpty(); }
    public String getBaseUrl()          { return baseUrl; }

    // =================================================================
    // AUTHENTICATION
    // =================================================================

    /**
     * Signs in and stores the returned token.
     * @return the authenticated user
     * @throws ApiException 401 when the credentials are wrong
     */
    public User signIn(String username, String password, boolean rememberMe)
            throws Exception {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username",   username);
        body.put("password",   password);
        body.put("rememberMe", rememberMe);

        Map<String, String> reply =
                JsonUtil.parseObject(send("POST", "/api/login",
                                          JsonUtil.toJson(body), false));
        this.token = reply.get("token");
        return JsonUtil.toUser(reply);
    }

    /** Confirms a stored token is still valid, returning its owner. */
    public User restoreSession(String storedToken) throws Exception {
        this.token = storedToken;
        try {
            // Any authenticated endpoint proves the token; the dashboard is
            // the cheapest.
            send("GET", "/api/dashboard", null, true);
            Map<String, String> me =
                    JsonUtil.parseObject(send("GET", "/api/session/me", null, true));
            return me.containsKey("username") ? JsonUtil.toUser(me) : null;
        } catch (ApiException e) {
            this.token = null;
            if (e.isUnauthorized()) {
                return null;
            }
            throw e;
        }
    }

    /**
     * Tells the server this session is still in use.
     *
     * Every authenticated endpoint refreshes last_activity server-side, so
     * this simply calls the cheapest one. The controller throttles how often
     * it is called, because sending a request on every mouse move would be
     * pointless traffic.
     */
    public void touchSession() throws Exception {
        send("GET", "/api/session/me", null, true);
    }

    public void signOut() throws Exception {
        try {
            send("POST", "/api/logout", "{}", true);
        } finally {
            this.token = null;
        }
    }

    // =================================================================
    // APPOINTMENTS
    // =================================================================

    public List<Appointment> findAll() throws Exception {
        return toAppointments(send("GET", "/api/appointments", null, true));
    }

    public Appointment findByNo(String no) throws Exception {
        try {
            Map<String, String> map =
                    JsonUtil.parseObject(send("GET", "/api/appointments/" + enc(no),
                                              null, true));
            return map.containsKey("appointmentNo")
                    ? JsonUtil.toAppointment(map) : null;
        } catch (ApiException e) {
            if (e.isNotFound()) {
                return null;
            }
            throw e;
        }
    }

    public List<Appointment> search(String term) throws Exception {
        return toAppointments(send("GET",
                "/api/appointments/search?q=" + enc(term), null, true));
    }

    public List<Appointment> findByDate(String date) throws Exception {
        return toAppointments(send("GET",
                "/api/appointments/date?on=" + enc(date), null, true));
    }

    public boolean slotTaken(String dentist, String date, String time)
            throws Exception {
        String json = send("GET", "/api/appointments/check?dentist=" + enc(dentist)
                + "&date=" + enc(date) + "&time=" + enc(time), null, true);
        return "true".equals(JsonUtil.parseObject(json).get("taken"));
    }

    public String nextAppointmentNo() throws Exception {
        return JsonUtil.parseObject(
                send("GET", "/api/appointments/next", null, true)).get("next");
    }

    /** @throws ApiException 409 when the number is taken or the slot clashes */
    public boolean save(Appointment a) throws Exception {
        send("POST", "/api/appointments", JsonUtil.toJson(a), true);
        return true;
    }

    public boolean update(Appointment a) throws Exception {
        send("PUT", "/api/appointments/" + enc(a.getAppointmentNo()),
             JsonUtil.toJson(a), true);
        return true;
    }

    public boolean delete(String no) throws Exception {
        send("DELETE", "/api/appointments/" + enc(no), null, true);
        return true;
    }

    // =================================================================
    // SUPPORTING DATA
    // =================================================================

    public Map<String, Double> findTreatments() throws Exception {
        Map<String, Double> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e
                : JsonUtil.parseObject(send("GET", "/api/treatments", null, true))
                          .entrySet()) {
            try {
                out.put(e.getKey(), Double.parseDouble(e.getValue()));
            } catch (NumberFormatException ignored) {
                out.put(e.getKey(), 0.0);
            }
        }
        return out;
    }

    public double findTreatmentCost(String treatmentType) throws Exception {
        Double cost = findTreatments().get(treatmentType);
        return cost == null ? 0.0 : cost;
    }

    public Map<String, String> dashboardFigures() throws Exception {
        return JsonUtil.parseObject(send("GET", "/api/dashboard", null, true));
    }

    public List<String[]> dailyScheduleReport() throws Exception {
        return toRows(send("GET", "/api/reports/daily", null, true),
                      "date", "dentist", "appointments", "revenue");
    }

    public List<String> generateReminders() throws Exception {
        return JsonUtil.parseStringArray(send("POST", "/api/reminders", "{}", true));
    }

    public List<String[]> activeSessions() throws Exception {
        return toRows(send("GET", "/api/sessions", null, true),
                      "username", "fullName", "machine",
                      "signedIn", "lastActivity", "idle");
    }

    public int endAllSessions() throws Exception {
        String json = send("DELETE", "/api/sessions", null, true);
        try {
            return Integer.parseInt(JsonUtil.parseObject(json).get("closed"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // =================================================================
    // STAFF ACCOUNTS - the server rejects these unless the caller is ADMIN
    // =================================================================

    public List<String[]> staffRows() throws Exception {
        return toRows(send("GET", "/api/users", null, true),
                      "userId", "username", "fullName",
                      "role", "created", "liveSessions");
    }

    public int createUser(String username, String password,
                          String fullName, String role) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", username);
        body.put("password", password);
        body.put("fullName", fullName);
        body.put("role",     role);

        String json = send("POST", "/api/users", JsonUtil.toJson(body), true);
        try {
            return Integer.parseInt(JsonUtil.parseObject(json).get("userId"));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public boolean resetPassword(int userId, String password) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("password", password);
        send("PUT", "/api/users/" + userId, JsonUtil.toJson(body), true);
        return true;
    }

    public boolean changeRole(int userId, String fullName, String role)
            throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("fullName", fullName);
        body.put("role",     role);
        send("PUT", "/api/users/" + userId, JsonUtil.toJson(body), true);
        return true;
    }

    public boolean deleteUser(int userId) throws Exception {
        send("DELETE", "/api/users/" + userId, null, true);
        return true;
    }

    // =================================================================
    // LOW LEVEL HTTP
    // =================================================================

    /**
     * Sends one request and returns the response body.
     *
     * @param authenticate attach the Authorization: Bearer header
     * @throws ApiException carrying the HTTP status when the server refuses
     */
    private String send(String method, String path, String body,
                        boolean authenticate) throws Exception {

        HttpURLConnection conn =
                (HttpURLConnection) new URL(baseUrl + path).openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("Accept", "application/json");

        if (authenticate && hasToken()) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }

        if (body != null) {
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            try (OutputStream out = conn.getOutputStream()) {
                out.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }

        int status = conn.getResponseCode();
        if (status >= 200 && status < 300) {
            return read(conn.getInputStream());
        }

        String message = JsonUtil.parseObject(read(conn.getErrorStream())).get("error");
        throw new ApiException(status,
                message == null ? "Server returned status " + status : message);
    }

    private String read(InputStream in) throws Exception {
        if (in == null) {
            return "";
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            return out.toString("UTF-8");
        } finally {
            in.close();
        }
    }

    private List<Appointment> toAppointments(String json) {
        List<Appointment> list = new ArrayList<>();
        for (Map<String, String> row : JsonUtil.parseArray(json)) {
            list.add(JsonUtil.toAppointment(row));
        }
        return list;
    }

    private List<String[]> toRows(String json, String... columns) {
        List<String[]> rows = new ArrayList<>();
        for (Map<String, String> row : JsonUtil.parseArray(json)) {
            String[] values = new String[columns.length];
            for (int i = 0; i < columns.length; i++) {
                values[i] = row.get(columns[i]);
            }
            rows.add(values);
        }
        return rows;
    }

    private String enc(String value) throws Exception {
        return URLEncoder.encode(value == null ? "" : value, "UTF-8");
    }
}
