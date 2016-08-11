package org.vadere.gui.projectview.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A simple about dialog for the Vadere GUI.
 * 
 * 
 */
public class AboutDialogView extends JDialog {

	private static final long serialVersionUID = 7906201118628730905L;
	private final JPanel contentPanel = new JPanel();

	/**
	 * Create the dialog.
	 */
	public AboutDialogView() {
		final JDialog currentDialog = this;

		setTitle("About");
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout(0, 0));
		{
			JLabel lblVadereGUI = new JLabel("Vadere GUI");
			contentPanel.add(lblVadereGUI, BorderLayout.NORTH);
		}
		{
			JLabel lblThisSoftwareManages = new JLabel(
					"This software manages scenarios for the Vadere Pedestrian Crowd Simulator.");
			contentPanel.add(lblThisSoftwareManages, BorderLayout.CENTER);
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton closeButton = new JButton("Close");
				closeButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						currentDialog.setVisible(false);
					}
				});
				closeButton.setActionCommand("Close");
				buttonPane.add(closeButton);
				getRootPane().setDefaultButton(closeButton);
			}
		}
	}

}
