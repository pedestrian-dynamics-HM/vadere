package org.vadere.gui.topographycreator.control.celleditor;

import org.vadere.gui.topographycreator.control.cellrenderer.EditorDelegate;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.ArrayList;
import java.util.EventObject;

public class ItemColumnEditor implements TableCellEditor {
    private final ArrayList<EditorDelegate> editorDelegates;

    public ItemColumnEditor(ArrayList<EditorDelegate> editorDelegates) {
        this.editorDelegates = editorDelegates;
    }

    private static boolean isItemInLastRow(JTable table, int row) {
        return row == table.getModel().getRowCount() - 1;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        if (isItemInLastRow(table, row)) {
            return emptyPanel();
        }
        return itemEditor(row);
    }

    private Component emptyPanel() {
        return new JPanel();
    }

    public EditorDelegate itemEditor(int row) {
        var delegate = (EditorDelegate) editorDelegates.get(row);
        delegate.updateDelegate(row);
        return delegate;
    }

    @Override
    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public boolean isCellEditable(EventObject anEvent) {
        return true;
    }

    @Override
    public boolean shouldSelectCell(EventObject anEvent) {
        return false;
    }

    @Override
    public boolean stopCellEditing() {
        return true;
    }

    @Override
    public void cancelCellEditing() {

    }

    @Override
    public void addCellEditorListener(CellEditorListener l) {

    }

    @Override
    public void removeCellEditorListener(CellEditorListener l) {

    }
}
