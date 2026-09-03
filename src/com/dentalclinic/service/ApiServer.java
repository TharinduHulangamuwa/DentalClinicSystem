package com.dentalclinic.service;

import com.dentalclinic.model.Appointment;
import com.dentalclinic.model.AppointmentDAO;
import com.dentalclinic.model.DBConnection;
import com.dentalclinic.model.ReminderService;
import com.dentalclinic.model.Session;
import com.dentalclinic.model.SessionDAO;
import com.dentalclinic.model.User;
import com.dentalclinic.model.UserDAO;
import com.dentalclinic.model.Validator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * THE WEB SERVICE TIER.
 *
 * A REST service over HTTP returning JSON. Run as a SEPARATE PROCESS from the
 * Swing client, which is what makes the system a distributed application:
 *
 *     [ JFrame client ] --HTTP/JSON--> [ this server ] --JDBC--> [ MySQL ]
 *          process 1                       process 2
 *
 * The client holds no database credentials and no JDBC connection. Several
 * clients on several machines can share one server.
 *
 * WHY com.sun.net.httpserver RATHER THAN SPRING BOOT
 * It ships inside the JDK, so there is no extra dependency, no build tool and
 * no application server to install. The whole service is about 400 readable
 * lines. Spring Boot would add tens of megabytes of dependencies to expose a
 * handful of endpoints. A justified simple choice is better engineering than
 * an unjustified heavy one.
 *
 * AUTHENTICATION
 * Every endpoint except /api/login requires an Authorization header carrying
 * the session token issued at sign-in:
 *
 *     Authorization: Bearer <token>
 *
 * This is exactly how a web application uses a session cookie, with the token
 * sent explicitly because a desktop client has no cookie jar. The token is
 * validated against the sessions table on every request.
 *
 * THREADING: a fixed pool of ten threads serves requests concurrently, so ten
 * reception desks can work at once.
 *
 * @author [Your Name]
 */
public class ApiServer {

    public static final int PORT = 8081;

    private static final AppointmentDAO APPTS    = new AppointmentDAO();
    private static final UserDAO        USERS    = new UserDAO();
    private static final SessionDAO     SESSIONS = new SessionDAO();

    public static void main(String[] args) throws IOException {

        if (!DBConnection.isReachable()) {
            System.err.println("FATAL: cannot reach the dental_clinic database.");
            System.err.println("Start WampServer and run sql/dental_clinic.sql first.");
            System.exit(1);
        }

        try {
            int closed = SESSIONS.purgeExpired();
            if (closed > 0) {
                System.out.println("[API] " + closed + " expired session(s) closed");
            }
        } catch (Exception e) {
            System.err.println("[API] session housekeeping failed: " + e.getMessage());
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // The ONLY route with no authentication, so a browser or a
        // monitoring tool can confirm the service is alive.
        server.createContext("/api/health",       ApiServer::handleHealth);

        server.createContext("/api/login",        ApiServer::handleLogin);
        server.createContext("/api/logout",       ApiServer::handleLogout);
        server.createContext("/api/appointments", ApiServer::handleAppointments);
        server.createContext("/api/treatments",   ApiServer::handleTreatments);
        server.createContext("/api/dashboard",    ApiServer::handleDashboard);
        server.createContext("/api/reports",      ApiServer::handleReports);
        server.createContext("/api/reminders",    ApiServer::handleReminders);
        server.createContext("/api/session",      ApiServer::handleWhoAmI);
        server.createContext("/api/sessions",     ApiServer::handleSessions);
        server.createContext("/api/users",        ApiServer::handleUsers);

        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        System.out.println("==================================================");
        System.out.println(" Sunrise Dental Clinic - REST Web Service");
        System.out.println(" Listening on http://localhost:" + PORT + "/api/");
        System.out.println("");
        System.out.println(" Check it in a browser:");
        System.out.println("   http://localhost:" + PORT + "/api/health");
        System.out.println("");
        System.out.println(" Now start the client: run Main.java");
        System.out.println(" Press Ctrl+C to stop this server");
        System.out.println("==================================================");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[API] shutting down");
            DBConnection.close();
        }));
    }

    // =================================================================
    // PUBLIC ENDPOINT
    // =================================================================

    /**
     * GET /api/health - service status, no token required.
     *
     * Every other route is protected, which is correct: patient names,
     * contact numbers and appointment times must not be readable by anyone
     * who types a URL. This route exists so the service can be checked from
     * a browser without signing in, and it deliberately returns no patient
     * data of any kind.
     */
    private static void handleHealth(HttpExchange ex) throws IOException {
        log(ex);
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("service", "Sunrise Dental Clinic API");
        status.put("status",  "running");
        status.put("port",    PORT);
        status.put("version", "1.0");
        try {
            status.put("database",
                    DBConnection.isReachable() ? "connected" : "unreachable");
        } catch (Exception e) {
            status.put("database", "unreachable");
        }
        respond(ex, 200, JsonUtil.toJson(status));
    }

    // =================================================================
    // AUTHENTICATION ENDPOINTS
    // =================================================================

    /** POST /api/login  {username, password, rememberMe} -> {token, user} */
    private static void handleLogin(HttpExchange ex) throws IOException {
        log(ex);
        try {
            if (!"POST".equals(ex.getRequestMethod())) {
                respond(ex, 405, JsonUtil.message("error", "Method not allowed"));
                return;
            }
            Map<String, String> body = JsonUtil.parseObject(readBody(ex));
            String username = body.get("username");
            String password = body.get("password");
            boolean remember = "true".equals(body.get("rememberMe"));

            User user = USERS.authenticate(username, password);
            if (user == null) {
                respond(ex, 401, JsonUtil.message("error",
                        "Invalid username or password"));
                return;
            }

            String token = SESSIONS.create(user, remember);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("token",    token);
            result.put("userId",   user.getUserId());
            result.put("username", user.getUsername());
            result.put("fullName", user.getFullName());
            result.put("role",     user.getRole());
            respond(ex, 200, JsonUtil.toJson(result));

        } catch (Exception e) {
            fail(ex, e);
        }
    }

    /** POST /api/logout  -> closes the session carried by the token */
    private static void handleLogout(HttpExchange ex) throws IOException {
        log(ex);
        try {
            String token = bearerToken(ex);
            if (token != null) {
                SESSIONS.end(token);
            }
            respond(ex, 200, JsonUtil.message("status", "signed out"));
        } catch (Exception e) {
            fail(ex, e);
        }
    }

    // =================================================================
    // APPOINTMENT ENDPOINTS
    // =================================================================
    private static void handleAppointments(HttpExchange ex) throws IOException {
        log(ex);
        try {
            User caller = requireAuth(ex);
            if (caller == null) {
                return;
            }
            String method = ex.getRequestMethod();
            String path   = ex.getRequestURI().getPath();
            String tail   = path.substring("/api/appointments".length());

            if ("GET".equals(method)) {
                if (tail.startsWith("/check")) {
                    Map<String, String> q = query(ex);
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("taken", APPTS.slotTaken(q.get("dentist"),
                                                   q.get("date"), q.get("time")));
                    respond(ex, 200, JsonUtil.toJson(r));

                } else if (tail.startsWith("/search")) {
                    String term = query(ex).get("q");
                    respond(ex, 200, JsonUtil.appointmentsToJson(
                            term == null || term.isEmpty()
                                ? APPTS.findAll() : APPTS.search(term)));

                } else if (tail.startsWith("/date")) {
                    respond(ex, 200, JsonUtil.appointmentsToJson(
                            APPTS.findByDate(query(ex).get("on"))));

                } else if (tail.startsWith("/next")) {
                    respond(ex, 200, JsonUtil.message("next",
                            APPTS.nextAppointmentNo()));

                } else if (tail.isEmpty() || "/".equals(tail)) {
                    respond(ex, 200, JsonUtil.appointmentsToJson(APPTS.findAll()));

                } else {
                    Appointment found = APPTS.findByNo(tail.substring(1));
                    if (found == null) {
                        respond(ex, 404, JsonUtil.message("error",
                                "No appointment with that number"));
                    } else {
                        respond(ex, 200, JsonUtil.toJson(found));
                    }
                }

            } else if ("POST".equals(method)) {
                createAppointment(ex);

            } else if ("PUT".equals(method)) {
                updateAppointment(ex, tail.isEmpty() ? "" : tail.substring(1));

            } else if ("DELETE".equals(method)) {
                String no = tail.isEmpty() ? "" : tail.substring(1);
                if (APPTS.delete(no)) {
                    respond(ex, 200, JsonUtil.message("status", "deleted"));
                } else {
                    respond(ex, 404, JsonUtil.message("error", "Nothing was deleted"));
                }

            } else {
                respond(ex, 405, JsonUtil.message("error", "Method not allowed"));
            }
        } catch (Exception e) {
            fail(ex, e);
        }
    }

    private static void createAppointment(HttpExchange ex) throws Exception {
        Appointment a = JsonUtil.toAppointment(JsonUtil.parseObject(readBody(ex)));

        // SERVER-SIDE VALIDATION.
        // The client validates too, but a server must never trust a client:
        // another client could be written tomorrow, or a request could be
        // sent by hand. This duplication is defence in depth, not oversight.
        String error = validate(a);
        if (error != null) {
            respond(ex, 400, JsonUtil.message("error", error));
            return;
        }
        if (APPTS.findByNo(a.getAppointmentNo()) != null) {
            respond(ex, 409, JsonUtil.message("error",
                    "Appointment number " + a.getAppointmentNo() + " already exists"));
            return;
        }
        if (APPTS.slotTaken(a.getDentistName(), a.getAppointmentDate(),
                            a.getAppointmentTime())) {
            respond(ex, 409, JsonUtil.message("error", "DOUBLE_BOOKING: "
                    + a.getDentistName() + " is already booked at "
                    + a.getAppointmentDate() + " " + a.getAppointmentTime()));
            return;
        }
        APPTS.save(a);
        respond(ex, 201, JsonUtil.toJson(a));
    }

    private static void updateAppointment(HttpExchange ex, String no) throws Exception {
        Appointment a = JsonUtil.toAppointment(JsonUtil.parseObject(readBody(ex)));
        a.setAppointmentNo(no);

        String error = validate(a);
        if (error != null) {
            respond(ex, 400, JsonUtil.message("error", error));
            return;
        }
        if (APPTS.slotTakenByOther(a.getDentistName(), a.getAppointmentDate(),
                                   a.getAppointmentTime(), no)) {
            respond(ex, 409, JsonUtil.message("error", "DOUBLE_BOOKING: "
                    + a.getDentistName() + " is already booked at that time"));
            return;
        }
        if (APPTS.update(a)) {
            respond(ex, 200, JsonUtil.toJson(a));
        } else {
            respond(ex, 404, JsonUtil.message("error", "No such appointment"));
        }
    }

    // =================================================================
    // SUPPORTING ENDPOINTS
    // =================================================================
    private static void handleTreatments(HttpExchange ex) throws IOException {
        log(ex);
        try {
            if (requireAuth(ex) == null) {
                return;
            }
            respond(ex, 200, JsonUtil.toJson(APPTS.findTreatments()));
        } catch (Exception e) {
            fail(ex, e);
        }
    }

    private static void handleDashboard(HttpExchange ex) throws IOException {
        log(ex);
        try {
            if (requireAuth(ex) == null) {
                return;
            }
            respond(ex, 200, JsonUtil.toJson(APPTS.dashboardFigures()));
        } catch (Exception e) {
            fail(ex, e);
        }
    }

    private static void handleReports(HttpExchange ex) throws IOException {
        log(ex);
        try {
            if (requireAuth(ex) == null) {
                return;
            }
            respond(ex, 200, JsonUtil.rowsToJson(
                    new String[]{"date", "dentist", "appointments", "revenue"},
                    APPTS.dailyScheduleReport()));
        } catch (Exception e) {
            fail(ex, e);
        }
    }

    private static void handleReminders(HttpExchange ex) throws IOException {
        log(ex);
        try {
            if (requireAuth(ex) == null) {
                return;
            }
            List<String> messages =
                    new ReminderService(APPTS).generateTomorrowReminders();
            respond(ex, 200, JsonUtil.stringsToJson(messages));
        } catch (Exception e) {
            fail(ex, e);
        }
    }

    /** GET /api/session/me -> the user the current token belongs to. */
    private static void handleWhoAmI(HttpExchange ex) throws IOException {
        log(ex);
        try {
            User caller = requireAuth(ex);
            if (caller != null) {
                respond(ex, 200, JsonUtil.toJson(caller));
            }
        } catch (Exception e) {
            fail(ex, e);
        }
    }

    private static void handleSessions(HttpExchange ex) throws IOException {
        log(ex);
        try {
            User caller = requireAuth(ex);
            if (caller == null) {
                return;
            }
            if ("DELETE".equals(ex.getRequestMethod())) {
                int closed = SESSIONS.endAllForUser(caller.getUserId());
                respond(ex, 200, JsonUtil.message("closed", String.valueOf(closed)));
                return;
            }
            SESSIONS.purgeExpired();
            respond(ex, 200, JsonUtil.rowsToJson(
                    new String[]{"username", "fullName", "machine",
                                 "signedIn", "lastActivity", "idle"},
                    SESSIONS.activeSessions()));
        } catch (Exception e) {
            fail(ex, e);
        }
    }

    // =================================================================
    // STAFF ACCOUNT ENDPOINTS - administrator only
    // =================================================================
    private static void handleUsers(HttpExchange ex) throws IOException {
        log(ex);
        try {
            User caller = requireAuth(ex);
            if (caller == null) {
                return;
            }

            // The server enforces the role itself. The client hides the
            // screen from non-administrators as a convenience, but hiding a
            // button is not a security control - this check is.
            if (!caller.isAdmin()) {
                respond(ex, 403, JsonUtil.message("error",
                        "Only an administrator can manage staff accounts"));
                return;
            }

            String method = ex.getRequestMethod();
            String tail   = ex.getRequestURI().getPath()
                              .substring("/api/users".length());

            if ("GET".equals(method)) {
                respond(ex, 200, JsonUtil.rowsToJson(
                        new String[]{"userId", "username", "fullName",
                                     "role", "created", "liveSessions"},
                        USERS.staffRows()));

            } else if ("POST".equals(method)) {
                Map<String, String> b = JsonUtil.parseObject(readBody(ex));
                String error = validateUser(b);
                if (error != null) {
                    respond(ex, 400, JsonUtil.message("error", error));
                    return;
                }
                try {
                    int id = USERS.create(b.get("username"), b.get("password"),
                                          b.get("fullName"), b.get("role"));
                    respond(ex, 201, JsonUtil.message("userId", String.valueOf(id)));
                } catch (Exception dup) {
                    respond(ex, 409, JsonUtil.message("error", dup.getMessage()));
                }

            } else if ("PUT".equals(method)) {
                int id = Integer.parseInt(tail.substring(1));
                Map<String, String> b = JsonUtil.parseObject(readBody(ex));

                if (b.containsKey("password")) {
                    if (!Validator.isStrongPassword(b.get("password"))) {
                        respond(ex, 400, JsonUtil.message("error",
                                "Password must be at least "
                                + Validator.MIN_PASSWORD_LENGTH
                                + " characters with a letter and a digit"));
                        return;
                    }
                    USERS.resetPassword(id, b.get("password"));
                    respond(ex, 200, JsonUtil.message("status", "password reset"));
                    return;
                }

                String role = b.get("role");
                if ("STAFF".equals(role) && USERS.countAdmins() <= 1) {
                    User target = USERS.findById(id);
                    if (target != null && target.isAdmin()) {
                        respond(ex, 409, JsonUtil.message("error",
                                "This is the only administrator account"));
                        return;
                    }
                }
                USERS.update(id, b.get("fullName"), role);
                respond(ex, 200, JsonUtil.message("status", "updated"));

            } else if ("DELETE".equals(method)) {
                int id = Integer.parseInt(tail.substring(1));

                if (id == caller.getUserId()) {
                    respond(ex, 409, JsonUtil.message("error",
                            "You cannot delete the account you are signed in as"));
                    return;
                }
                User target = USERS.findById(id);
                if (target != null && target.isAdmin() && USERS.countAdmins() <= 1) {
                    respond(ex, 409, JsonUtil.message("error",
                            "This is the only administrator account"));
                    return;
                }
                if (USERS.delete(id)) {
                    respond(ex, 200, JsonUtil.message("status", "deleted"));
                } else {
                    respond(ex, 404, JsonUtil.message("error", "No such account"));
                }

            } else {
                respond(ex, 405, JsonUtil.message("error", "Method not allowed"));
            }
        } catch (Exception e) {
            fail(ex, e);
        }
    }

    // =================================================================
    // VALIDATION - the same Validator the client uses
    // =================================================================
    static String validate(Appointment a) {
        if (!Validator.isValidAppointmentNo(a.getAppointmentNo())) {
            return "Appointment number must be APT followed by four digits";
        }
        if (!Validator.isValidName(a.getPatientName())) {
            return "Patient name is invalid";
        }
        if (!Validator.isValidContact(a.getContactNo())) {
            return "Contact number must be ten digits starting with 0";
        }
        if (!Validator.isNotEmpty(a.getDentistName())) {
            return "Dentist name is required";
        }
        if (!Validator.isValidDate(a.getAppointmentDate())) {
            return "Date must be in yyyy-MM-dd format";
        }
        if (!Validator.isValidTime(a.getAppointmentTime())) {
            return "Time must be in HH:mm format";
        }
        if (!Validator.isWithinClinicHours(a.getAppointmentTime())) {
            return "The clinic is open from 08:00 to 20:00";
        }
        return null;
    }

    static String validateUser(Map<String, String> b) {
        if (!Validator.isValidUsername(b.get("username"))) {
            return "Username must be 4 to 20 characters starting with a letter";
        }
        if (!Validator.isValidName(b.get("fullName"))) {
            return "Full name is invalid";
        }
        if (!Validator.isStrongPassword(b.get("password"))) {
            return "Password must be at least " + Validator.MIN_PASSWORD_LENGTH
                 + " characters with a letter and a digit";
        }
        String role = b.get("role");
        if (!"ADMIN".equals(role) && !"STAFF".equals(role)) {
            return "Role must be ADMIN or STAFF";
        }
        return null;
    }

    // =================================================================
    // HELPERS
    // =================================================================

    /** Reads the bearer token from the Authorization header. */
    static String bearerToken(HttpExchange ex) {
        String header = ex.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        String token = header.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    /**
     * Validates the token and returns the caller, or writes 401 and returns
     * null. Every endpoint except /api/login begins with this.
     */
    private static User requireAuth(HttpExchange ex) throws IOException {
        try {
            String token = bearerToken(ex);
            if (token == null) {
                respond(ex, 401, JsonUtil.message("error", "Missing session token"));
                return null;
            }
            User user = SESSIONS.validate(token);
            if (user == null) {
                respond(ex, 401, JsonUtil.message("error",
                        "Session expired or invalid"));
                return null;
            }
            SESSIONS.touch(token);
            return user;
        } catch (Exception e) {
            respond(ex, 500, JsonUtil.message("error", String.valueOf(e.getMessage())));
            return null;
        }
    }

    private static void log(HttpExchange ex) {
        System.out.println(ex.getRequestMethod() + " " + ex.getRequestURI());
    }

    private static void fail(HttpExchange ex, Exception e) throws IOException {
        e.printStackTrace();
        respond(ex, 500, JsonUtil.message("error", String.valueOf(e.getMessage())));
    }

    static String readBody(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toString("UTF-8");
        }
    }

    static Map<String, String> query(HttpExchange ex) {
        return parseQuery(ex.getRequestURI().getQuery());
    }

    /** Parses a URL query string into a map. Package-private so it is testable. */
    static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return params;
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                try {
                    params.put(URLDecoder.decode(pair.substring(0, eq), "UTF-8"),
                               URLDecoder.decode(pair.substring(eq + 1), "UTF-8"));
                } catch (IOException ignored) {
                    // a malformed parameter is skipped rather than failing
                }
            }
        }
        return params;
    }

    static void respond(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type",
                                    "application/json; charset=UTF-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }
}
