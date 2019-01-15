package org.vadere.gui.projectview.view;


import org.vadere.util.logging.Logger;

import java.awt.*;
import java.io.File;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

public class OutputTableRenderer extends DefaultTableCellRenderer {
	private static final long serialVersionUID = -3026076129551731012L;
	private static Logger logger = Logger.getLogger(OutputTableRenderer.class);
	private Collection<String> markedOutputFiles;

	public OutputTableRenderer() {
		super();
		setOpaque(true);
		markedOutputFiles = new LinkedList<>();
	}

	@Override
	public Component getTableCellRendererComponent(final JTable table, Object value, final boolean isSelected,
			final boolean hasFocus,
			final int row, final int column) {

		String text;
		if (value instanceof File) {
			text = ((File) value).getName();
		} else {
			text = "";
		}

		value = (value != null ? value.toString() : "");

		if (isSelected) {
			super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);
		} else {
			if (!table.isEnabled()) {
				setForeground(Color.black);
				setBackground(Color.gray);
			} else if (markedOutputFiles.contains(value)) {
				setForeground(Color.black);
				setBackground(Color.green);
			} else {
				setForeground(Color.black);
				setBackground(Color.white);
			}
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			setText(text);
		}
		return this;
	}

	public void setMarkedOutputFiles(Collection<String> selectedOutputFiles) {
		this.markedOutputFiles = selectedOutputFiles;
	}

	public void removeMarkedOutputFile(final String filename) {
		this.markedOutputFiles.remove(filename);
	}

	public void addMarkedOutputFile(final String filename) {
		this.markedOutputFiles.add(filename);
	}

}
