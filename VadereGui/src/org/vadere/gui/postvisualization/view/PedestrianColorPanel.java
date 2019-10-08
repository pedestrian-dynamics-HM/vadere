package org.vadere.gui.postvisualization.view;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.vadere.gui.postvisualization.model.PostvisualizationModel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PedestrianColorPanel extends JPanel {
	private JTable colorTable;
	private final DefaultTableModel tableModel;

	public PedestrianColorPanel(final PostvisualizationModel model) {
		this.tableModel = model.getPredicateColoringModel().getPedestrianColorTableModel();
		String[] headers = new String[] {"Criteria", "Color"};
		Object[][] data = new Object[][] {
				new Object[] {"", model.config.getPedestrianDefaultColor()},
				new Object[] {"", model.config.getPedestrianDefaultColor()},
				new Object[] {"", model.config.getPedestrianDefaultColor()},
				new Object[] {"", model.config.getPedestrianDefaultColor()},
				new Object[] {"", model.config.getPedestrianDefaultColor()}
		};
		colorTable = new JTable(tableModel);
		JScrollPane scrollPane = new JScrollPane(colorTable);

		FormLayout formLayout = new FormLayout(
				"2dlu, pref:grow, 2dlu", // cols
				"2dlu, 60dlu, 2dlu"); // rows
		setLayout(formLayout);
		CellConstraints cc = new CellConstraints();
		add(scrollPane, cc.xy(2, 2));
		tableModel.setDataVector(data, headers);
		colorTable.setDefaultRenderer(Object.class, new PedestrianColorTableRenderer());
		colorTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent e) {
				int row = colorTable.rowAtPoint(e.getPoint());
				int col = colorTable.columnAtPoint(e.getPoint());

				if (row != -1 && col == 1) {
					Object obj = tableModel.getValueAt(row, col);
					Color ccolor = Color.white;
					if (obj instanceof Color) {
						ccolor = (Color) obj;
					}
					Color color = JColorChooser.showDialog(null, "Choose Color", ccolor);
					tableModel.setValueAt(color, row, col);
				}
			}
		});
	}

	private static class PedestrianColorTableRenderer extends DefaultTableCellRenderer {

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			if (value instanceof Color) {
				Color color = (Color) value;
				setForeground(Color.white);
				setBackground(color);
				setText("RGB=" + color.getRed() + " " + color.getGreen() + " " + color.getBlue());
			} else if (value instanceof JButton) {
				return (JButton) value;
			} else {
				setForeground(Color.black);
				setBackground(Color.white);
				super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			}
			return this;
		}
	}
}
