package org.vadere.gui.topographycreator.control.attribtable.cells.delegates;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class EditorDelegate extends JPanel {
    JTable table;

    JComponent editor;

    public EditorDelegate(JTable table, JComponent editor) {
        super(new BorderLayout());
        this.table = table;
        this.editor = editor;

    }

    public EditorDelegate(JTable table) {
        super(new BorderLayout());
        this.table = table;
        this.editor = new JPanel();

    }

    public void updateDelegate(int row) {
        this.removeAll();
        if (row == table.getModel().getRowCount() - 1) {
            var panel = new JPanel(new BorderLayout());
            this.add(panel, BorderLayout.CENTER);
            return;
        }
        var panel = new JPanel(new BorderLayout());
        var label = new JLabel(String.valueOf(row));
        label.setBorder(new EmptyBorder(0, 8, 0, 8));
        label.setBackground(Color.gray);
        panel.add(label, BorderLayout.WEST);
        panel.add(editor);
        this.add(panel, BorderLayout.CENTER);
    }
}
