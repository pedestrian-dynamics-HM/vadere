package org.vadere.gui.topographycreator.control.attribtable.cells.renderer;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class ButtonColumnRenderer extends JPanel implements TableCellRenderer {

    private static final JButton ERASE_BUTTON = new JButton("-");
    private static final JButton ADD_BUTTON = new JButton("+");

    public ButtonColumnRenderer() {
    }

    private static boolean isItemInLastRow(JTable table, int row) {
        return row == table.getModel().getRowCount() - 1;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (isItemInLastRow(table, row)) {
            return ADD_BUTTON;
        }
        return ERASE_BUTTON;
    }
}
