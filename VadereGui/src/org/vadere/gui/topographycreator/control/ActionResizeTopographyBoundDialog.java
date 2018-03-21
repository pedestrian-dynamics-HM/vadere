package org.vadere.gui.topographycreator.control;

import org.vadere.gui.projectview.view.ProjectView;
import org.vadere.util.geometry.shapes.VRectangle;

import java.awt.*;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class ActionResizeTopographyBoundDialog {


	JTextField textField;
	VRectangle bound;
	VRectangle boundOld;
	boolean valid;



	public ActionResizeTopographyBoundDialog(double width, double height){

		textField = new JTextField();
		textField.setText(String.format("%.3f x %.3f", width, height));
		textField.getDocument().addDocumentListener(new DialogListener());

		bound = new VRectangle(0.0,0.0, width, height);
		boundOld = new VRectangle(0.0,0.0, width, height);
		valid = false;
	}

	public VRectangle getBound() {
		return valid ? bound : boundOld;
	}

	public boolean getValue(){
		return JOptionPane.showConfirmDialog(
				ProjectView.getMainWindow(),
				textField,
				"width x heigt",
				JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
	}

	private class DialogListener implements DocumentListener{

		JTextField textField;
		String text;

		DialogListener(){
			textField = ActionResizeTopographyBoundDialog.this.textField;
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
			text = textField.getText().replace(" ", "");
			String[] tmp = text.split("x");
			double width;
			double height;
			try {
				width = Double.valueOf(tmp[0]);
				height = Double.valueOf(tmp[1]);
				ActionResizeTopographyBoundDialog.this.bound =
						new VRectangle(0.0,0.0, width, height);
				ActionResizeTopographyBoundDialog.this.valid = true;
				textField.setForeground(Color.BLACK);
			}catch (Exception ex){
				ActionResizeTopographyBoundDialog.this.valid = false;
				textField.setForeground(Color.RED);
			}
		}
	}
}


