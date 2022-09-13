package org.vadere.gui.topographycreator.control.attribtable.cells.renderer;

import org.vadere.gui.topographycreator.control.attribtable.cells.delegates.AttributeEditor;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class FieldValueRenderer extends DefaultTableCellRenderer implements TableCellRenderer {
    AttributeEditor editor;

    public FieldValueRenderer(JTable table, AttributeEditor editor) {
        table.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                editor.setSize(table.getWidth() / 2, table.getRowHeight());
            }
        });
        this.editor = editor;
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
            }
        });
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        editor.setPreferredSize(new Dimension(table.getWidth() / 2, table.getRowHeight()));
        return this.editor;
    }
}
