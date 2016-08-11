package org.vadere.gui.projectview.view;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

public class ChooseNameDialog extends JDialog {

	private static final long serialVersionUID = 1186663896084884L;
	private List<JRadioButton> radioButtons;
	private ButtonGroup group;
	private final String title;
	private String selectedName;
	private boolean aborted;

	public ChooseNameDialog(final Frame owner, final String[] names) {
		this(owner, "Choose Name", names);
	}

	public ChooseNameDialog(final Frame owner, final String title, final String[] names) {
		super(owner, true);
		this.title = title;
		this.selectedName = null;
		this.aborted = false;
		radioButtons = new ArrayList<>();
		group = new ButtonGroup();

		for (String name : names) {
			JRadioButton rButton = new JRadioButton(name);
			group.add(rButton);
			radioButtons.add(rButton);
		}
		initComponents();
	}

	private void initComponents() {
		// setPreferredSize(new Dimension(200, 400));
		setTitle(title);
		CellConstraints cc = new CellConstraints();
		StringBuilder row = new StringBuilder();
		row.append("2dlu");
		for (JRadioButton radioButton : radioButtons) {
			row.append(", pref, 2dlu");
		}
		row.append(", pref, 2dlu");
		FormLayout layout = new FormLayout("2dlu, default, 2dlu", // col
				row.toString()); // rows
		getContentPane().setLayout(layout);
		for (int rowIndex = 0; rowIndex < radioButtons.size(); rowIndex++) {
			getContentPane().add(radioButtons.get(rowIndex), cc.xy(2, 2 * (1 + rowIndex)));
		}

		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				for (JRadioButton radioButton : ChooseNameDialog.this.radioButtons) {
					if (radioButton.isSelected()) {
						selectedName = radioButton.getText();
					}
				}
				ChooseNameDialog.this.dispose();
			}
		});

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				aborted = true;
			}
		});

		okButton.setActionCommand("OK");
		getRootPane().setDefaultButton(okButton);
		getContentPane().add(okButton, cc.xy(2, 2 * (radioButtons.size() + 1)));

		setAlwaysOnTop(true);
		setLocationRelativeTo(getOwner());
		setModal(true);

		pack();
		setVisible(true);
	}

	public String getSelectedName() {
		return selectedName;
	}

	public boolean isAborted() {
		return aborted;
	}
}
