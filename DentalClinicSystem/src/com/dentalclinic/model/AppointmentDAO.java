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
 * Every SQL statement that touches appointments lives in this one class.
 * Controllers call save(), findByNo(), findAll() and never write SQL.
 *
 * All queries use PreparedStatement with bound parameters, so user input
 * can never be interpreted as SQL (injection defence).
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
            ps.setString(1, a.getAppointmentNo());
            ps.setString(2, a.getPatientName());
            ps.setString(3, a.getAddress());
            ps.setString(4, a.getContactNo());
            ps.setString(5, a.getDentistName());
            ps.setString(6, a.getTreatmentType());
            ps.setString(7, a.getAppointmentDate());
            ps.setString(8, a.getAppointmentTime());
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
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<Appointment> findAll() throws SQLException {
        List<Appointment> list = new ArrayList<>();
        String sql = "SELECT * FROM appointments "
                   + "ORDER BY appointment_date DESC, appointment_time DESC";
        try (Statement st = DBConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }


    /** Appointments on one date, used by the reminder service. */
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

    /** Powers the "double booking" pre-check before we even attempt the insert. */
    public boolean slotTaken(String dentist, String date, String time) throws SQLException {
        String sql = "SELECT 1 FROM appointments "
                   + "WHERE dentist_name = ? AND appointment_date = ? AND appointment_time = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, dentist);
            ps.setString(2, date);
            ps.setString(3, time);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // -----------------------------------------------------------------
    // UPDATE / DELETE
    // -----------------------------------------------------------------
    public boolean update(Appointment a) throws SQLException {
        String sql = "UPDATE appointments SET patient_name=?, address=?, contact_no=?, "
                   + "dentist_name=?, treatment_type=?, appointment_date=?, appointment_time=? "
                   + "WHERE appointment_no=?";
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
    // TREATMENTS (price list)
    // -----------------------------------------------------------------
    public Map<String, Double> findTreatments() throws SQLException {
        Map<String, Double> treatments = new LinkedHashMap<>();
        String sql = "SELECT treatment_type, cost FROM treatments ORDER BY treatment_type";
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
                if (rs.next()) {
                    return rs.getDouble("cost");
                }
            }
        }
        return 0.0;
    }

    // -----------------------------------------------------------------
    // REPORT (reads the SQL VIEW created in Guide 01)
    // -----------------------------------------------------------------
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

    /** Shared ResultSet-to-object mapping, so the SQL columns appear once. */
    private Appointment mapRow(ResultSet rs) throws SQLException {
        return new Appointment(
                rs.getString("appointment_no"),
                rs.getString("patient_name"),
                rs.getString("address"),
                rs.getString("contact_no"),
                rs.getString("dentist_name"),
                rs.getString("treatment_type"),
                rs.getString("appointment_date"),
                rs.getString("appointment_time"));
    }
}
