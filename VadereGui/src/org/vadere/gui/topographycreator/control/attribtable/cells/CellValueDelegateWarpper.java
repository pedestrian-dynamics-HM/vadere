package org.vadere.gui.topographycreator.control.attribtable.cells;

import org.vadere.gui.topographycreator.control.attribtable.cells.delegates.AttributeEditor;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.EventObject;

public class CellValueDelegateWarpper extends JPanel implements TableCellRenderer, TableCellEditor {
    private final AttributeEditor editor;
    public CellValueDelegateWarpper(AttributeEditor editor) {
        super();
        this.editor = editor;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        return editor;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        return editor;
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
        return true;
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
