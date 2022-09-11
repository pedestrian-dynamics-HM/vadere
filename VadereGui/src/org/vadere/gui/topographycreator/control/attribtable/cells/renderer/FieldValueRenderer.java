package org.vadere.gui.topographycreator.control.attribtable.cells.renderer;

import org.vadere.gui.topographycreator.control.attribtable.cells.delegates.AttributeEditor;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class FieldValueRenderer extends DefaultTableCellRenderer implements TableCellRenderer {
    AttributeEditor editor;

    public FieldValueRenderer(AttributeEditor editor) {
        this.editor = editor;
    }
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        return this.editor;
    }
}
