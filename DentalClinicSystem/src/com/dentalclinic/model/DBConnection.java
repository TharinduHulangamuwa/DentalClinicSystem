package com.dentalclinic.model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * SINGLETON PATTERN.
 *
 * Guarantees that the entire application shares exactly one JDBC connection
 * to the MySQL database running under WAMP.
 *
 * Why this pattern was chosen:
 *   - Opening a TCP connection to MySQL is expensive (tens of milliseconds).
 *     Doing it on every button click would make the UI feel sluggish.
 *   - Connection settings exist in exactly one place, so moving the database
 *     to another machine is a one-line change.
 *   - The private constructor makes it impossible for any other class to
 *     create a second instance by mistake.
 *
 * @author [Your Name]
 */
public class DBConnection {

    // --- Connection settings. Change only these three lines if WAMP differs. ---
    private static final String URL  = "jdbc:mysql://localhost:3306/dental_clinic"
                                     + "?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = "";      // WAMP default root password is blank

    /** The single shared instance. */
    private static Connection connection;

    /** Private constructor blocks external instantiation. */
    private DBConnection() { }

    /**
     * Returns the shared connection, creating it on first use.
     * Also recreates it if it was closed, so the app recovers gracefully.
     */
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                throw new SQLException(
                    "MySQL JDBC driver not found. Add mysql-connector-j to Libraries.", e);
            }
            connection = DriverManager.getConnection(URL, USER, PASS);
        }
        return connection;
    }

    /** Called once when the user exits, so MySQL is released cleanly. */
    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }

    /** Simple health check used by the startup self-test. */
    public static boolean isReachable() {
        try {
            return getConnection() != null && !getConnection().isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}