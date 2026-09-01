package com.dentalclinic.view;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.LayoutManager;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

/**
 * Every colour, font and border used by the interface, defined once.
 *
 * DESIGN DECISION: writing colours and fonts as literals wherever they are
 * needed makes an interface subtly inconsistent - the same blue ends up as
 * three slightly different values - and means a branding change requires
 * edits in every view class. Defining them here gives the application one
 * visual identity and makes restyling a single-file change.
 *
 * The idea is the same one behind a CSS stylesheet, applied to Swing.
 *
 * @author [Your Name]
 */
public final class Theme {

    private Theme() { }

    // ================= palette =================

    /** Clinical navy, used for the sidebar and header. */
    public static final Color NAVY       = new Color(19, 47, 76);
    public static final Color NAVY_LIGHT = new Color(31, 68, 105);
    public static final Color NAVY_HOVER = new Color(42, 88, 133);

    /** Teal accent for the selected nav item and primary actions. */
    public static final Color ACCENT      = new Color(0, 130, 180);
    public static final Color ACCENT_DARK = new Color(0, 105, 148);

    // surfaces
    public static final Color CANVAS     = new Color(243, 246, 250);
    public static final Color CARD       = Color.WHITE;
    public static final Color BORDER     = new Color(214, 221, 231);
    public static final Color ROW_STRIPE = new Color(247, 250, 253);

    // feedback
    public static final Color ERROR    = new Color(183, 28, 28);
    public static final Color ERROR_BG = new Color(255, 238, 238);
    public static final Color SUCCESS  = new Color(21, 115, 71);
    public static final Color WARNING  = new Color(176, 106, 0);
    public static final Color AMBER    = new Color(255, 196, 87);

    // text
    public static final Color TEXT         = new Color(28, 35, 45);
    public static final Color TEXT_MUTED   = new Color(112, 122, 136);
    public static final Color TEXT_ON_DARK = new Color(232, 238, 245);
    public static final Color TEXT_FADED   = new Color(150, 175, 200);

    public static final Color FIELD_OK = Color.WHITE;

    // ================= fonts =================

    /**
     * Picks the first font that is actually installed, so the interface looks
     * the same on Windows, macOS and Linux instead of falling back to a
     * default serif face.
     */
    private static final String FAMILY = pickFamily(
            "Segoe UI", "Inter", "Helvetica Neue", "DejaVu Sans", "SansSerif");

    private static String pickFamily(String... candidates) {
        java.util.List<String> installed = Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment()
                                   .getAvailableFontFamilyNames());
        for (String c : candidates) {
            if (installed.contains(c)) {
                return c;
            }
        }
        return "SansSerif";
    }

    public static final Font BRAND   = new Font(FAMILY, Font.BOLD,  19);
    public static final Font TITLE   = new Font(FAMILY, Font.BOLD,  17);
    public static final Font STAT    = new Font(FAMILY, Font.BOLD,  28);
    public static final Font HEADING = new Font(FAMILY, Font.BOLD,  13);
    public static final Font BODY    = new Font(FAMILY, Font.PLAIN, 13);
    public static final Font SMALL   = new Font(FAMILY, Font.PLAIN, 11);
    public static final Font NAV     = new Font(FAMILY, Font.PLAIN, 14);
    public static final Font MONO    = new Font(pickFamily(
            "Consolas", "DejaVu Sans Mono", "Monospaced"), Font.PLAIN, 13);

    // ================= borders =================

    public static final Border FIELD = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(6, 8, 6, 8));

    public static final Border FIELD_ERROR = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ERROR),
            BorderFactory.createEmptyBorder(6, 8, 6, 8));

    public static Border pad(int top, int left, int bottom, int right) {
        return BorderFactory.createEmptyBorder(top, left, bottom, right);
    }

    public static Border hairline() {
        return BorderFactory.createLineBorder(BORDER);
    }

    /** A titled white card, used to group related controls. */
    public static Border titledCard(String title) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(BORDER), " " + title + " ",
                        javax.swing.border.TitledBorder.LEFT,
                        javax.swing.border.TitledBorder.TOP, HEADING, NAVY),
                BorderFactory.createEmptyBorder(12, 16, 14, 16));
    }

    // ================= components =================

    public static JLabel formLabel(String text) {
        JLabel l = new JLabel(text, JLabel.RIGHT);
        l.setFont(BODY);
        l.setForeground(TEXT);
        return l;
    }

    public static JLabel hint(String text) {
        JLabel l = new JLabel(text);
        l.setFont(SMALL);
        l.setForeground(TEXT_MUTED);
        return l;
    }

    public static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setFont(TITLE);
        l.setForeground(NAVY);
        return l;
    }

    /** Solid accent button for the main action on a screen. */
    public static JButton primary(String text, char mnemonic, String tip) {
        JButton b = base(text, mnemonic, tip);
        b.setFont(HEADING);
        b.setBackground(ACCENT);
        b.setForeground(Color.WHITE);
        b.setBorderPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
        return b;
    }

    /** Outlined button for supporting actions. */
    public static JButton secondary(String text, char mnemonic, String tip) {
        JButton b = base(text, mnemonic, tip);
        b.setFont(BODY);
        b.setForeground(NAVY);
        b.setBackground(Color.WHITE);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(9, 20, 9, 20)));
        return b;
    }

    /** Outlined button in the error colour, for destructive actions. */
    public static JButton danger(String text, char mnemonic, String tip) {
        JButton b = base(text, mnemonic, tip);
        b.setFont(BODY);
        b.setForeground(ERROR);
        b.setBackground(Color.WHITE);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ERROR),
                BorderFactory.createEmptyBorder(9, 20, 9, 20)));
        return b;
    }

    /** Small flat button for the dark header. */
    public static JButton onDark(String text, char mnemonic, String tip) {
        JButton b = base(text, mnemonic, tip);
        b.setFont(SMALL);
        b.setForeground(TEXT_ON_DARK);
        b.setBackground(NAVY_LIGHT);
        b.setBorderPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
        return b;
    }

    private static JButton base(String text, char mnemonic, String tip) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMnemonic(mnemonic);
        b.setToolTipText(tip);
        return b;
    }

    public static JPanel canvas(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setBackground(CANVAS);
        return p;
    }

    public static JPanel white(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setBackground(CARD);
        return p;
    }
}
