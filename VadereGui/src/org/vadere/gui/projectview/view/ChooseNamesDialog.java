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

public class ChooseNamesDialog extends JDialog {

	private static final long serialVersionUID = 5750425148268941422L;
	private List<JCheckBox> checkBoxList;
	private final String title;
	private String[] selectedNames;
	private boolean aborted;

	public ChooseNamesDialog(final Frame owner, final String[] names) {
		this(owner, "Choose Names", names);
	}

	public ChooseNamesDialog(final Frame owner, final String title, final String[] names) {
		super(owner, true);
		this.title = title;
		this.selectedNames = new String[] {};
		this.aborted = false;
		checkBoxList = new ArrayList<>();

		for (String name : names) {
			checkBoxList.add(new JCheckBox(name));
		}
		initComponents();
	}

	private void initComponents() {
		setTitle(title);
		CellConstraints cc = new CellConstraints();
		StringBuilder row = new StringBuilder();
		row.append("2dlu");
		for (JCheckBox checkBox : checkBoxList) {
			row.append(", pref, 2dlu");
		}
		row.append(", pref, 2dlu");
		FormLayout layout = new FormLayout("2dlu, default, 2dlu", // col
				row.toString()); // rows
		getContentPane().setLayout(layout);
		for (int rowIndex = 0; rowIndex < checkBoxList.size(); rowIndex++) {
			getContentPane().add(checkBoxList.get(rowIndex), cc.xy(2, 2 * (1 + rowIndex)));
		}

		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				selectedNames = new String[checkBoxList.size()];
				List<String> gatherNamesList = new ArrayList<>();
				for (JCheckBox checkBox : checkBoxList) {
					if (checkBox.isSelected()) {
						gatherNamesList.add(checkBox.getText());
					}
				}
				selectedNames = gatherNamesList.toArray(new String[] {});

				ChooseNamesDialog.this.dispose();
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
		getContentPane().add(okButton, cc.xy(2, 2 * (checkBoxList.size() + 1)));

		setAlwaysOnTop(true);
		setLocationRelativeTo(getOwner());
		setModal(true);

		pack();
		setVisible(true);
	}

	public boolean isAborted() {
		return aborted;
	}

	public String[] getSelectedNames() {
		return selectedNames.clone();
	}
}
