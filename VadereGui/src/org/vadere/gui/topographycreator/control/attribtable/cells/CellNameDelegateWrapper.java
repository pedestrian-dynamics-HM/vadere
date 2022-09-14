package org.vadere.gui.topographycreator.control.attribtable.cells;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.EventObject;

public class CellNameDelegateWrapper extends JPanel implements TableCellRenderer, TableCellEditor {

    private static final Color Transparent = new Color(0, 0, 0, 0);
    private Font font = UIManager.getFont("Label.font");
    private final String id;

    public CellNameDelegateWrapper(String id){
        this.id = id;
        this.font = new Font(font.getName(), Font.BOLD, font.getSize());
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        JPanel fieldNamePanel = new JPanel(new BorderLayout());

        JLabel nameTextPane = new JLabel();
        JLabel typeTextPane = new JLabel();

        nameTextPane.setText(id);
        //typeTextPane.setText(getType(field));

        nameTextPane.setBackground(Transparent);
        typeTextPane.setBackground(Transparent);

        nameTextPane.setIcon(null);

        typeTextPane.setFont(font.deriveFont(Font.BOLD, 10));
        typeTextPane.setForeground(new Color(0, 192, 163));

        nameTextPane.setHorizontalAlignment(JLabel.LEFT);
        typeTextPane.setHorizontalAlignment(JLabel.RIGHT);
        fieldNamePanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        fieldNamePanel.add(nameTextPane,BorderLayout.PAGE_START);
        //fieldNamePanel.add(typeTextPane,BorderLayout.PAGE_END);

        return fieldNamePanel;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        return getTableCellEditorComponent(table,value,isSelected,row,column);
    }

    @Override
    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public boolean isCellEditable(EventObject anEvent) {
        return false;
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
