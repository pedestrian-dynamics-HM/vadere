package org.vadere.gui.topographycreator.control.attribtable.cells.renderer;

import org.vadere.gui.topographycreator.control.attribtable.cells.delegates.AttributeEditor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class ListValueRenderer implements TableCellRenderer {
    private final AttributeEditor editor;
    private final String id;

    public ListValueRenderer(AttributeEditor editor, String id) {
        this.editor = editor;
        this.id = id;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        var panel = new JPanel(new BorderLayout());
        var lable = new JLabel(id);
        lable.setBorder(new EmptyBorder(0, 8, 0, 8));
        panel.add(lable, BorderLayout.WEST);
        panel.add(editor);
        return panel;
    }
}
