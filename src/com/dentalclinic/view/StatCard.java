package com.dentalclinic.view;

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A single headline figure on the dashboard, for example "4 appointments
 * today".
 *
 * USABILITY REASONING: the receptionist's first question each morning is how
 * busy the day is. Answering it with a table they must read and count is
 * slower than answering it with one large number. A coloured rule along the
 * top distinguishes the cards at a glance without needing icons.
 *
 * @author [Your Name]
 */
public class StatCard extends JPanel {

    private final JLabel valueLabel;

    public StatCard(String caption, String value, Color accent) {
        setLayout(new BorderLayout(0, 4));
        setBackground(Theme.CARD);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(3, 0, 0, 0, accent),
                BorderFactory.createCompoundBorder(
                        Theme.hairline(),
                        Theme.pad(14, 18, 16, 18))));

        valueLabel = new JLabel(value);
        valueLabel.setFont(Theme.STAT);
        valueLabel.setForeground(Theme.NAVY);

        JLabel captionLabel = new JLabel(caption.toUpperCase());
        captionLabel.setFont(Theme.SMALL);
        captionLabel.setForeground(Theme.TEXT_MUTED);

        add(valueLabel,   BorderLayout.CENTER);
        add(captionLabel, BorderLayout.SOUTH);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }
}
