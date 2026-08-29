package com.dentalclinic.service;

import com.dentalclinic.model.Appointment;
import com.dentalclinic.model.AppointmentDAO;
import com.dentalclinic.model.DBConnection;
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
 * WEB SERVICE TIER - a REST service over HTTP returning JSON.
 *
 * This is a SEPARATE PROCESS from the Swing client. Running it turns the
 * system into a genuinely distributed application:
 *
 *     [ Swing client ] --HTTP/JSON--> [ this server ] --JDBC--> [ MySQL ]
 *
 * The client no longer holds database credentials or a JDBC connection.
 * Several clients, on several machines, can share one server.
 *
 * Endpoints
 *   GET  /api/appointments              all appointments
 *   GET  /api/appointments/{no}         one appointment
 *   POST /api/appointments              create an appointment
 *   GET  /api/appointments/check?...    is a dentist slot already taken
 *   GET  /api/treatments                treatment price list
 *
 * MULTITHREADING NOTE: HttpServer is given a fixed thread pool of ten
 * threads, so ten reception desks can be served concurrently. This is a
 * third, server-side demonstration of concurrency in the system.
 */
public class AppointmentServer {

    public static final int PORT = 8081;

    private static final AppointmentDAO DAO = new AppointmentDAO();

    public static void main(String[] args) throws IOException {

        if (!DBConnection.isReachable()) {
            System.err.println("FATAL: cannot reach the dental_clinic database.");
            System.err.println("Start WampServer and try again.");
            System.exit(1);
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/api/appointments", AppointmentServer::handleAppointments);
        server.createContext("/api/treatments",   AppointmentServer::handleTreatments);

        // Concurrency: ten worker threads serve requests in parallel
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        System.out.println("=================================================");
        System.out.println(" Sunrise Dental Clinic - Appointment Web Service");
        System.out.println(" Listening on http://localhost:" + PORT + "/api/");
        System.out.println(" Press Ctrl+C to stop");
        System.out.println("=================================================");

        // Close the database cleanly if the process is terminated
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down web service...");
            DBConnection.close();
        }));
    }

    // =================================================================
    // /api/appointments
    // =================================================================
    private static void handleAppointments(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path   = exchange.getRequestURI().getPath();

        System.out.println(method + " " + exchange.getRequestURI());

        try {
            if ("GET".equals(method)) {

                if (path.endsWith("/check")) {
                    handleSlotCheck(exchange);
                    return;
                }

                // /api/appointments        -> list
                // /api/appointments/APT1001 -> single
                String tail = path.substring("/api/appointments".length());
                if (tail.isEmpty() || "/".equals(tail)) {
                    List<Appointment> all = DAO.findAll();
                    respond(exchange, 200, JsonUtil.toJson(all));
                } else {
                    String no = tail.substring(1);
                    Appointment found = DAO.findByNo(no);
                    if (found == null) {
                        respond(exchange, 404,
                                JsonUtil.message("error", "No appointment with number " + no));
                    } else {
                        respond(exchange, 200, JsonUtil.toJson(found));
                    }
                }

            } else if ("POST".equals(method)) {
                handleCreate(exchange);

            } else {
                respond(exchange, 405, JsonUtil.message("error", "Method not allowed"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            respond(exchange, 500, JsonUtil.message("error", e.getMessage()));
        }
    }

    private static void handleCreate(HttpExchange exchange) throws Exception {
        String body = readBody(exchange);
        Appointment a = JsonUtil.toAppointment(JsonUtil.parseObject(body));

        // SERVER-SIDE VALIDATION.
        // The Swing client validates too, but a server must never trust a
        // client - another client could be written tomorrow. This duplication
        // is deliberate defence in depth, not an oversight.
        String error = validate(a);
        if (error != null) {
            respond(exchange, 400, JsonUtil.message("error", error));
            return;
        }

        if (DAO.findByNo(a.getAppointmentNo()) != null) {
            respond(exchange, 409, JsonUtil.message("error",
                    "Appointment number " + a.getAppointmentNo() + " already exists"));
            return;
        }

        if (DAO.slotTaken(a.getDentistName(), a.getAppointmentDate(), a.getAppointmentTime())) {
            respond(exchange, 409, JsonUtil.message("error",
                    "DOUBLE_BOOKING: " + a.getDentistName() + " is already booked at "
                    + a.getAppointmentDate() + " " + a.getAppointmentTime()));
            return;
        }

        DAO.save(a);
        respond(exchange, 201, JsonUtil.toJson(a));
    }

    private static void handleSlotCheck(HttpExchange exchange) throws Exception {
        Map<String, String> q = queryParams(exchange.getRequestURI().getQuery());

        boolean taken = DAO.slotTaken(q.get("dentist"), q.get("date"), q.get("time"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taken", taken);
        respond(exchange, 200, JsonUtil.toJson(result));
    }

    // =================================================================
    // /api/treatments
    // =================================================================
    private static void handleTreatments(HttpExchange exchange) throws IOException {
        try {
            if (!"GET".equals(exchange.getRequestMethod())) {
                respond(exchange, 405, JsonUtil.message("error", "Method not allowed"));
                return;
            }
            Map<String, Double> treatments = DAO.findTreatments();
            respond(exchange, 200, JsonUtil.toJson(treatments));
        } catch (Exception e) {
            respond(exchange, 500, JsonUtil.message("error", e.getMessage()));
        }
    }

    // =================================================================
    // helpers
    // =================================================================
    private static String validate(Appointment a) {
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

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toString("UTF-8");
        }
    }

    private static Map<String, String> queryParams(String query) {
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
                    // malformed parameter - skip it
                }
            }
        }
        return params;
    }

    private static void respond(HttpExchange exchange, int status, String json)
            throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
