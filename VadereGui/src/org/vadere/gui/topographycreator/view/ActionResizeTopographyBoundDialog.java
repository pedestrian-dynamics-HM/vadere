package org.vadere.gui.topographycreator.view;

import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.projectview.view.ProjectView;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Locale;

import javax.swing.*;
import javax.swing.event.DocumentEvent;

public class ActionResizeTopographyBoundDialog {


	private JTextField textField;
	private Rectangle2D.Double bound;
	private Rectangle2D.Double boundOld;
	private boolean valid;

	public ActionResizeTopographyBoundDialog(final Rectangle2D.Double topographyBound){

		textField = new JTextField();
		textField.setText(String.format(Locale.US, "%.3f x %.3f", topographyBound.getWidth(), topographyBound.getHeight()));
		textField.getDocument().addDocumentListener(new SimpleDocumentListener() {
			@Override
			public void handle(DocumentEvent e) {
				String text = textField.getText().replace(" ", "");
				String[] tmp = text.split("x");
				double width;
				double height;
				try {
					width = Double.valueOf(tmp[0]);
					height = Double.valueOf(tmp[1]);

					ActionResizeTopographyBoundDialog.this.bound =
							new Rectangle2D.Double(ActionResizeTopographyBoundDialog.this.boundOld.getMinX(),
									ActionResizeTopographyBoundDialog.this.boundOld.getMinY(), width, height);
					ActionResizeTopographyBoundDialog.this.valid = true;
					textField.setForeground(Color.BLACK);
				}catch (Exception ex){
					ActionResizeTopographyBoundDialog.this.valid = false;
					textField.setForeground(Color.RED);
				}
			}
		});

		bound = topographyBound;
		boundOld = topographyBound;
		valid = false;
	}

	public Rectangle2D.Double getBound() {
		return valid ? bound : boundOld;
	}

	public boolean getValue(){
		return JOptionPane.showConfirmDialog(
				ProjectView.getMainWindow(),
				textField,
				Messages.getString("TopographyBoundDialog.title"),
				JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
	}

}


