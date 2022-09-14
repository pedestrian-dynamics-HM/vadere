package org.vadere.gui.topographycreator.control.attribtable.cells;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class ButtonColumnRenderer extends JPanel implements TableCellRenderer {
    private static final JButton ERASE_BUTTON = new JButton("-");
    public ButtonColumnRenderer() {
    }
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        return ERASE_BUTTON;
    }
}
