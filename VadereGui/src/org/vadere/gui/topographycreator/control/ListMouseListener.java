package org.vadere.gui.topographycreator.control;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ListMouseListener extends MouseAdapter {
    private final JTable list;
    private final DefaultTableModel model;

    private final JPanel contentPanel;


    public ListMouseListener(JTable list, DefaultTableModel model, JPanel contentPanel) {
        this.list = list;
        this.model = model;
        this.contentPanel = contentPanel;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        int column = list.getColumnModel().getColumnIndexAtX(e.getX()); // get the coloum of the button
        int row = e.getY() / list.getRowHeight(); //get the row of the button
        System.out.println(column + " " + row);

        if (column == 1) {
            if (row < model.getRowCount() - 1) {
                model.removeRow(row);
            } else {
                model.addRow(new Object[]{0.0});
            }
            contentPanel.revalidate();
            contentPanel.repaint();
        }
    }
}
