package com.dentalclinic.view;

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

/**
 * A JTable configured once for readability, so the two report tables do not
 * repeat the same setup code.
 *
 * USABILITY REASONING: alternating row shading lets the eye track a single
 * record across seven columns without losing its place. Tall rows and a bold
 * header give the table visual structure. Both are small changes that make a
 * dense table noticeably easier to read, which matters because the
 * receptionist scans this list many times a day.
 *
 * @author [Your Name]
 */
public class StripedTable extends JTable {

    public StripedTable(DefaultTableModel model) {
        super(model);

        setRowHeight(26);
        setShowVerticalLines(false);
        setGridColor(Theme.ROW_STRIPE);
        setFont(Theme.LABEL);
        setAutoCreateRowSorter(true);
        setSelectionBackground(Theme.BRAND_LIGHT);
        setSelectionForeground(java.awt.Color.BLACK);

        JTableHeader header = getTableHeader();
        header.setFont(Theme.HEADING);
        header.setReorderingAllowed(false);
    }

    /** Paints every second row in a pale tint. */
    @Override
    public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer,
                                     int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);

        if (!isRowSelected(row)) {
            c.setBackground(row % 2 == 0 ? getBackground() : Theme.ROW_STRIPE);
        }
        return c;
    }

    /** Right aligns a column, used for money and counts. */
    public void rightAlign(int columnIndex) {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(DefaultTableCellRenderer.RIGHT);
        getColumnModel().getColumn(columnIndex).setCellRenderer(renderer);
    }

    /** Sets a preferred width for one column. */
    public void width(int columnIndex, int pixels) {
        getColumnModel().getColumn(columnIndex).setPreferredWidth(pixels);
    }
}
