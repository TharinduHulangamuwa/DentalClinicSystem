package com.dentalclinic.model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * SINGLETON PATTERN.
 *
 * Guarantees the whole application shares exactly one JDBC connection to the
 * MySQL database running under WampServer.
 *
 * Why this pattern:
 *   - opening a TCP connection to MySQL costs tens of milliseconds, so doing
 *     it on every button click would make the interface feel sluggish
 *   - connection settings exist in one place, so moving the database to
 *     another machine is a one-line change
 *   - the private constructor makes a second instance impossible to create
 *
 * @author [Your Name]
 */
public final class DBConnection {

    // ---- change only these three lines if your WAMP differs ----
    private static final String URL  = "jdbc:mysql://localhost:3306/dental_clinic"
                                     + "?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = "";      // WAMP default is blank

    private static Connection connection;

    private DBConnection() { }

    /** Returns the shared connection, opening it on first use. */
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                throw new SQLException("MySQL JDBC driver not found. "
                        + "Add mysql-connector-j to the project Libraries.", e);
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
                System.out.println("[DB] connection closed");
            }
        } catch (SQLException e) {
            System.err.println("[DB] error closing connection: " + e.getMessage());
        }
    }

    /**
     * Health check used by the startup self-test.
     * Logs the real cause, because a silent false here is very hard to debug.
     */
    public static boolean isReachable() {
        try {
            return getConnection() != null && !getConnection().isClosed();
        } catch (SQLException e) {
            System.err.println("=== DATABASE CONNECTION FAILED ===");
            System.err.println("Message  : " + e.getMessage());
            System.err.println("SQLState : " + e.getSQLState());
            System.err.println("Code     : " + e.getErrorCode());
            return false;
        }
    }
}
