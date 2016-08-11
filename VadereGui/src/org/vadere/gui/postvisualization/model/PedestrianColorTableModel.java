package org.vadere.gui.postvisualization.model;

import javax.swing.table.DefaultTableModel;

public class PedestrianColorTableModel extends DefaultTableModel {
	public static int COLOR_COLUMN = 1;
	public static int CIRTERIA_COLUMN = 0;

	@Override
	public boolean isCellEditable(int row, int col) {
		return col == 0;
	}
}
