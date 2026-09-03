package com.dentalclinic.view;

import java.awt.Component;
import java.awt.Dimension;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

/**
 * A JTable configured once for readability, so every table in the
 * application looks the same without repeating the setup.
 *
 * USABILITY REASONING: alternating row shading lets the eye track one record
 * across seven columns without losing its place. Taller rows and a bold
 * header give the table structure. Both are small changes that make a dense
 * table noticeably easier to read, which matters because the receptionist
 * scans these lists many times a day.
 *
 * @author [Your Name]
 */
public class StripedTable extends JTable {

    public StripedTable(DefaultTableModel model) {
        super(model);

        setRowHeight(30);
        setShowVerticalLines(false);
        setShowHorizontalLines(false);
        setIntercellSpacing(new Dimension(0, 0));
        setFont(Theme.BODY);
        setForeground(Theme.TEXT);
        setBackground(Theme.CARD);
        setAutoCreateRowSorter(true);
        setSelectionBackground(new java.awt.Color(213, 232, 245));
        setSelectionForeground(Theme.TEXT);

        JTableHeader header = getTableHeader();
        header.setFont(Theme.HEADING);
        header.setBackground(Theme.CANVAS);
        header.setForeground(Theme.NAVY);
        header.setPreferredSize(new Dimension(0, 34));
        header.setReorderingAllowed(false);
    }

    /** Paints every second row in a pale tint. */
    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        if (!isRowSelected(row)) {
            c.setBackground(row % 2 == 0 ? getBackground() : Theme.ROW_STRIPE);
        }
        return c;
    }

    /** Right aligns a column, used for money and counts. */
    public void rightAlign(int columnIndex) {
        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setHorizontalAlignment(DefaultTableCellRenderer.RIGHT);
        getColumnModel().getColumn(columnIndex).setCellRenderer(r);
    }

    /** Sets a preferred width for one column. */
    public void width(int columnIndex, int pixels) {
        getColumnModel().getColumn(columnIndex).setPreferredWidth(pixels);
    }
}
