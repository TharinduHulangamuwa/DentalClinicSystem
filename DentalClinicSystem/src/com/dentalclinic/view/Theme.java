package com.dentalclinic.view;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.border.Border;

/**
 * Central place for every colour, font and border used by the interface.
 *
 * DESIGN DECISION: before this class existed, colours and fonts were written
 * as literals wherever they were needed. That made the interface visually
 * inconsistent - the same shade of blue appeared as three slightly different
 * values - and meant a change to the clinic's branding required edits in
 * every view class.
 *
 * Defining them once here gives the application a single visual identity and
 * makes restyling a one-file change. The idea is the same one behind a CSS
 * stylesheet, applied to Swing.
 *
 * @author [Your Name]
 */
public class Theme {

    private Theme() { }

    // ---------------- colours ----------------
    /** Clinic brand colour, used for headers. */
    public static final Color BRAND        = new Color(23, 58, 95);
    public static final Color BRAND_LIGHT  = new Color(232, 240, 250);

    /** Feedback colours. Chosen for contrast against white backgrounds. */
    public static final Color ERROR        = new Color(178, 34, 34);
    public static final Color SUCCESS      = new Color(21, 115, 71);
    public static final Color WARNING      = new Color(176, 106, 0);

    /** Field states. */
    public static final Color FIELD_OK     = Color.WHITE;
    public static final Color FIELD_ERROR  = new Color(255, 235, 235);

    /** Table striping, kept subtle so it aids scanning without distracting. */
    public static final Color ROW_STRIPE   = new Color(245, 248, 252);

    public static final Color TEXT_MUTED   = new Color(105, 105, 105);

    // ---------------- fonts ----------------
    public static final Font TITLE   = new Font("SansSerif", Font.BOLD,  20);
    public static final Font HEADING = new Font("SansSerif", Font.BOLD,  14);
    public static final Font LABEL   = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font SMALL   = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font MONO    = new Font("Monospaced", Font.PLAIN, 13);

    // ---------------- borders ----------------
    public static final Border FIELD_BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(190, 195, 205)),
            BorderFactory.createEmptyBorder(4, 6, 4, 6));

    public static final Border FIELD_BORDER_ERROR = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ERROR),
            BorderFactory.createEmptyBorder(4, 6, 4, 6));

    /** Standard padding inside a panel. */
    public static Border pad(int top, int left, int bottom, int right) {
        return BorderFactory.createEmptyBorder(top, left, bottom, right);
    }

    // ---------------- component helpers ----------------

    /** A form label, right aligned so labels sit close to their fields. */
    public static JLabel formLabel(String text) {
        JLabel label = new JLabel(text, JLabel.RIGHT);
        label.setFont(LABEL);
        return label;
    }

    /** Small grey helper text shown beside a field. */
    public static JLabel hint(String text) {
        JLabel label = new JLabel(text);
        label.setFont(SMALL);
        label.setForeground(TEXT_MUTED);
        return label;
    }

    /**
     * A primary action button: the one the user is most likely to press.
     * Mnemonic underlines the given letter for keyboard access.
     */
    public static JButton primaryButton(String text, char mnemonic, String tooltip) {
        JButton button = new JButton(text);
        button.setFont(HEADING);
        button.setBackground(BRAND);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        button.setMnemonic(mnemonic);
        button.setToolTipText(tooltip);
        return button;
    }

    /** A secondary button for supporting actions. */
    public static JButton button(String text, char mnemonic, String tooltip) {
        JButton button = new JButton(text);
        button.setFont(LABEL);
        button.setMnemonic(mnemonic);
        button.setToolTipText(tooltip);
        return button;
    }
}
