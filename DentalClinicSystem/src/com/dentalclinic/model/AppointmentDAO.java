package com.dentalclinic.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DATA ACCESS OBJECT for appointments and treatments.
 *
 * Every SQL statement touching appointments lives in this one class.
 * Controllers call save(), findByNo(), findAll(), update(), delete() and
 * never write SQL themselves.
 *
 * All queries use PreparedStatement with bound parameters, so user input can
 * never be interpreted as SQL.
 *
 * @author [Your Name]
 */
public class AppointmentDAO {

    // -----------------------------------------------------------------
    // CREATE
    // -----------------------------------------------------------------
    public boolean save(Appointment a) throws SQLException {
        String sql = "INSERT INTO appointments "
                   + "(appointment_no, patient_name, address, contact_no, "
                   + " dentist_name, treatment_type, appointment_date, appointment_time) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            bind(ps, a);
            return ps.executeUpdate() > 0;
        }
    }

    // -----------------------------------------------------------------
    // READ
    // -----------------------------------------------------------------
    public Appointment findByNo(String appointmentNo) throws SQLException {
        String sql = "SELECT * FROM appointments WHERE appointment_no = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, appointmentNo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public List<Appointment> findAll() throws SQLException {
        String sql = "SELECT * FROM appointments "
                   + "ORDER BY appointment_date DESC, appointment_time DESC";
        return query(sql);
    }

    /** Appointments on one date, used by the dashboard and the reminders. */
    public List<Appointment> findByDate(String date) throws SQLException {
        List<Appointment> list = new ArrayList<>();
        String sql = "SELECT * FROM appointments WHERE appointment_date = ? "
                   + "ORDER BY appointment_time";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /** Free-text search across patient name, contact number and dentist. */
    public List<Appointment> search(String term) throws SQLException {
        List<Appointment> list = new ArrayList<>();
        String sql = "SELECT * FROM appointments "
                   + "WHERE appointment_no LIKE ? OR patient_name LIKE ? "
                   + "   OR contact_no LIKE ?    OR dentist_name LIKE ? "
                   + "ORDER BY appointment_date DESC";
        String like = "%" + term + "%";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            for (int i = 1; i <= 4; i++) {
                ps.setString(i, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /** The double-booking pre-check, run before the insert is attempted. */
    public boolean slotTaken(String dentist, String date, String time) throws SQLException {
        String sql = "SELECT 1 FROM appointments "
                   + "WHERE dentist_name = ? AND appointment_date = ? "
                   + "AND appointment_time = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, dentist);
            ps.setString(2, date);
            ps.setString(3, time);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Same check, ignoring one appointment - needed when editing that one. */
    public boolean slotTakenByOther(String dentist, String date, String time,
                                    String exceptNo) throws SQLException {
        String sql = "SELECT 1 FROM appointments "
                   + "WHERE dentist_name = ? AND appointment_date = ? "
                   + "AND appointment_time = ? AND appointment_no <> ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, dentist);
            ps.setString(2, date);
            ps.setString(3, time);
            ps.setString(4, exceptNo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // -----------------------------------------------------------------
    // UPDATE and DELETE
    // -----------------------------------------------------------------
    public boolean update(Appointment a) throws SQLException {
        String sql = "UPDATE appointments SET patient_name=?, address=?, contact_no=?, "
                   + "dentist_name=?, treatment_type=?, appointment_date=?, "
                   + "appointment_time=? WHERE appointment_no=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, a.getPatientName());
            ps.setString(2, a.getAddress());
            ps.setString(3, a.getContactNo());
            ps.setString(4, a.getDentistName());
            ps.setString(5, a.getTreatmentType());
            ps.setString(6, a.getAppointmentDate());
            ps.setString(7, a.getAppointmentTime());
            ps.setString(8, a.getAppointmentNo());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(String appointmentNo) throws SQLException {
        String sql = "DELETE FROM appointments WHERE appointment_no = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, appointmentNo);
            return ps.executeUpdate() > 0;
        }
    }

    // -----------------------------------------------------------------
    // TREATMENTS
    // -----------------------------------------------------------------
    public Map<String, Double> findTreatments() throws SQLException {
        Map<String, Double> treatments = new LinkedHashMap<>();
        String sql = "SELECT treatment_type, cost FROM treatments ORDER BY cost";
        try (Statement st = DBConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                treatments.put(rs.getString("treatment_type"), rs.getDouble("cost"));
            }
        }
        return treatments;
    }

    public double findTreatmentCost(String treatmentType) throws SQLException {
        String sql = "SELECT cost FROM treatments WHERE treatment_type = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, treatmentType);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble("cost") : 0.0;
            }
        }
    }

    // -----------------------------------------------------------------
    // NUMBERING AND REPORTS
    // -----------------------------------------------------------------

    /**
     * The next free appointment number, so staff do not invent one and risk
     * a clash. Reads the highest existing number and adds one.
     */
    public String nextAppointmentNo() throws SQLException {
        String sql = "SELECT MAX(CAST(SUBSTRING(appointment_no, 4) AS UNSIGNED)) "
                   + "AS highest FROM appointments";
        try (Statement st = DBConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                int next = rs.getInt("highest") + 1;
                return String.format("APT%04d", Math.max(next, 1001));
            }
        }
        return "APT1001";
    }

    /** Rows of vw_daily_schedule, for the management summary screen. */
    public List<String[]> dailyScheduleReport() throws SQLException {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT appointment_date, dentist_name, total_appointments, "
                   + "expected_treatment_revenue FROM vw_daily_schedule";
        try (Statement st = DBConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                rows.add(new String[]{
                    rs.getString("appointment_date"),
                    rs.getString("dentist_name"),
                    rs.getString("total_appointments"),
                    String.format("%,.2f", rs.getDouble("expected_treatment_revenue"))
                });
            }
        }
        return rows;
    }

    /** Headline figures for the dashboard: today, tomorrow, total, revenue. */
    public Map<String, String> dashboardFigures() throws SQLException {
        Map<String, String> stats = new LinkedHashMap<>();
        try (Statement st = DBConnection.getConnection().createStatement()) {

            ResultSet rs = st.executeQuery(
                "SELECT COUNT(*) c FROM appointments WHERE appointment_date = CURDATE()");
            rs.next();
            stats.put("today", rs.getString("c"));

            rs = st.executeQuery("SELECT COUNT(*) c FROM appointments "
                    + "WHERE appointment_date = DATE_ADD(CURDATE(), INTERVAL 1 DAY)");
            rs.next();
            stats.put("tomorrow", rs.getString("c"));

            rs = st.executeQuery("SELECT COUNT(*) c FROM appointments");
            rs.next();
            stats.put("total", rs.getString("c"));

            rs = st.executeQuery("SELECT IFNULL(SUM(t.cost),0) r FROM appointments a "
                    + "JOIN treatments t ON a.treatment_type = t.treatment_type "
                    + "WHERE a.appointment_date = CURDATE()");
            rs.next();
            stats.put("revenue", String.format("%,.2f", rs.getDouble("r")));
        }
        return stats;
    }

    // -----------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------
    private List<Appointment> query(String sql) throws SQLException {
        List<Appointment> list = new ArrayList<>();
        try (Statement st = DBConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    private void bind(PreparedStatement ps, Appointment a) throws SQLException {
        ps.setString(1, a.getAppointmentNo());
        ps.setString(2, a.getPatientName());
        ps.setString(3, a.getAddress());
        ps.setString(4, a.getContactNo());
        ps.setString(5, a.getDentistName());
        ps.setString(6, a.getTreatmentType());
        ps.setString(7, a.getAppointmentDate());
        ps.setString(8, a.getAppointmentTime());
    }

    /** Shared ResultSet mapping, so the column names appear once. */
    private Appointment mapRow(ResultSet rs) throws SQLException {
        Appointment a = new Appointment(
                rs.getString("appointment_no"),
                rs.getString("patient_name"),
                rs.getString("address"),
                rs.getString("contact_no"),
                rs.getString("dentist_name"),
                rs.getString("treatment_type"),
                rs.getString("appointment_date"),
                rs.getString("appointment_time"));
        a.setStatus(rs.getString("status"));
        return a;
    }
}
