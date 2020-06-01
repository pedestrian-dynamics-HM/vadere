package org.vadere.gui.topographycreator.view;

import org.vadere.gui.projectview.view.ProjectView;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Locale;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class ActionTranslateTopographyDialog {


	private JTextField textField;
	private double x;
	private double y;
	private final double xOld;
	private final double yOld;
	private boolean valid;
	private final String label;

	public ActionTranslateTopographyDialog(final double x, double y){
		this(x, y, "x, y");
	}

	public ActionTranslateTopographyDialog(final double x, double y, final String label){

		this.textField = new JTextField(30);
		this.textField.setText(String.format(Locale.ENGLISH, "%f, %f", x, y));
		this.textField.getDocument().addDocumentListener(new DialogListener());
		this.valid = false;
		this.x = x;
		this.y = y;
		this.xOld = x;
		this.yOld = y;
		this.label = label;
	}

	public double getX() {
		return valid ? x : xOld;
	}

	public double getY() {
		return valid ? y : yOld;
	}

	public boolean getValue(){
		return JOptionPane.showConfirmDialog(
				ProjectView.getMainWindow(),
				textField,
				label,
				JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
	}

	private class DialogListener implements DocumentListener{

		private JTextField textField;
		private String text;

		DialogListener(){
			textField = ActionTranslateTopographyDialog.this.textField;
		}

		@Override
		public void insertUpdate(DocumentEvent e) {
			handle(e);
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			handle(e);
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
			handle(e);
		}

		private void handle(DocumentEvent e){
			text = textField.getText().replaceAll("\\s+", "");
			String[] tmp = text.split(",");
			double x;
			double y;
			try {
				x = Double.valueOf(tmp[0]);
				y = Double.valueOf(tmp[1]);
				ActionTranslateTopographyDialog.this.x = x;
				ActionTranslateTopographyDialog.this.y = y;
				ActionTranslateTopographyDialog.this.valid = true;
				textField.setForeground(Color.BLACK);
			}catch (Exception ex){
				ActionTranslateTopographyDialog.this.valid = false;
				textField.setForeground(Color.RED);
			}
		}
	}
}


