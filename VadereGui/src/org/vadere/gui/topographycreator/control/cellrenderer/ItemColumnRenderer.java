package org.vadere.gui.topographycreator.control.cellrenderer;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;

public class ItemColumnRenderer implements TableCellRenderer {
    private final ArrayList<EditorDelegate> editorDelegates;

    public ItemColumnRenderer(ArrayList<EditorDelegate> editorDelegates) {
        this.editorDelegates = editorDelegates;
    }

    private static boolean isItemInLastRow(JTable table, int row) {
        return row == table.getModel().getRowCount() - 1;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (isItemInLastRow(table, row)) {
            return emptyPanel();
        }
        return itemEditor(row);
    }

    private Component emptyPanel() {
        return new JPanel();
    }

    public JComponent itemEditor(int row) {
        var delegate = (EditorDelegate) editorDelegates.get(row);
        delegate.updateDelegate(row);
        return delegate;
    }
}
