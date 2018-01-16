package org.vadere.gui.projectview.view;

import javax.swing.*;

import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.utils.Resources;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class JsonValidIndicator extends JPanel {

	private JLabel labelJsonValid;
	private JLabel labelJsonInvalid;

	public JsonValidIndicator() {

		// setBorder(BorderFactory.createLineBorder(Color.black));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		// VALID

		labelJsonValid = new JLabel(Messages.getString("TextView.lbljsonvalid.text"));
		add(labelJsonValid);
		labelJsonValid.setIcon(new ImageIcon(Resources.class.getResource("/icons/Inform.gif")));

		// INVALID

		labelJsonInvalid = new JLabel("<html>" + Messages.getString("TextView.lbljsoninvalid.text") +
				" <font color=gray size=-1><a href=#>" + Messages.getString("TextView.lbljsoninvalid.showErrorText")
				+ "</a></font></html>");
		add(labelJsonInvalid);
		labelJsonInvalid.setIcon(new ImageIcon(Resources.class.getResource("/icons/Error.gif")));
		labelJsonInvalid.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				VDialogManager.showMessageDialogWithTextArea(
						Messages.getString("TextView.lbljsoninvalid.errorMsgPopup.title"),
						ScenarioPanel.getActiveJsonParsingErrorMsg(),
						JOptionPane.ERROR_MESSAGE);
			}
		});

		setValid();
	}

	public void setValid() {
		labelJsonInvalid.setVisible(false);
		labelJsonValid.setVisible(true);
	}

	public void setInvalid() {
		labelJsonInvalid.setVisible(true);
		labelJsonValid.setVisible(false);
	}

	@Override
	public void hide() {
		labelJsonInvalid.setVisible(false);
		labelJsonValid.setVisible(false);
	}

}
