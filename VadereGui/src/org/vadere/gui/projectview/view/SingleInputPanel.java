package org.vadere.gui.projectview.view;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Observable;

public class SingleInputPanel extends JPanel {
	private static final long serialVersionUID = 4239724157915052701L;
	private final String labelName;
	private JTextField input;
	private JLabel inputLabel;
	private Observable observable;

	public SingleInputPanel(final String labelName/* , final Observable observable */) {
		this.labelName = labelName;
		// this.observable = observable;
		initComponents();
	}

	private void initComponents() {
		FormLayout mainLayout = new FormLayout("5dlu, pref, 2dlu, pref, 5dlu", // col
				"5dlu, pref, 5dlu"); // rows
		CellConstraints cc = new CellConstraints();
		setLayout(mainLayout);
		inputLabel = new JLabel(labelName);
		input = new JTextField();
		input.setPreferredSize(new Dimension(250, 20));
		inputLabel.setLabelFor(input);

		add(inputLabel, cc.xy(2, 2));
		add(input, cc.xy(4, 2));

		/*
		 * input.getDocument().addDocumentListener(new DocumentListener() {
		 * 
		 * @Override
		 * public void removeUpdate(DocumentEvent e) {
		 * observable.notifyObservers();
		 * }
		 * 
		 * @Override
		 * public void insertUpdate(DocumentEvent e) {
		 * observable.notifyObservers();
		 * }
		 * 
		 * @Override
		 * public void changedUpdate(DocumentEvent e) {
		 * observable.notifyObservers();
		 * }
		 * });
		 */
	}
}
