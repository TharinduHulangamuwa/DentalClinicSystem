package com.dentalclinic.view;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;

/**
 * One item in the sidebar navigation.
 *
 * USABILITY REASONING: an earlier version used a JTabbedPane. Tabs work for
 * three or four screens but become cramped beyond that, and leave no room to
 * describe what each screen does. A vertical sidebar scales to as many
 * screens as the clinic needs, keeps every destination visible at once, and
 * marks the current one with a coloured bar - so the user always knows where
 * they are, which is Nielsen's "visibility of system status".
 *
 * @author [Your Name]
 */
public class NavButton extends JButton {

    private boolean selected;

    public NavButton(String text) {
        super(text);
        setFont(Theme.NAV);
        setForeground(Theme.TEXT_ON_DARK);
        setBackground(Theme.NAVY);
        setHorizontalAlignment(LEFT);
        setBorder(BorderFactory.createEmptyBorder(13, 26, 13, 16));
        setFocusPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setMaximumSize(new Dimension(240, 46));
        setPreferredSize(new Dimension(240, 46));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!selected) {
                    setBackground(Theme.NAVY_HOVER);
                    repaint();
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (!selected) {
                    setBackground(Theme.NAVY);
                    repaint();
                }
            }
        });
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        setBackground(selected ? Theme.NAVY_LIGHT : Theme.NAVY);
        setForeground(selected ? Color.WHITE : Theme.TEXT_ON_DARK);
        setFont(selected ? Theme.NAV.deriveFont(Font.BOLD) : Theme.NAV);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRect(0, 0, getWidth(), getHeight());

        if (selected) {
            g2.setColor(Theme.ACCENT);
            g2.fillRect(0, 0, 4, getHeight());
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
