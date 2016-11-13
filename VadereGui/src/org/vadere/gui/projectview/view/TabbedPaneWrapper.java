package org.vadere.gui.projectview.view;

import javax.swing.*;

public class TabbedPaneWrapper extends JTabbedPane {

	public TabbedPaneWrapper(int top) {
		super(top);
	}

	@Override
	public void setSelectedIndex(int index) {
		String errorMsg = ScenarioPanel.getActiveJsonParsingErrorMsg();
		if (errorMsg == null)
			super.setSelectedIndex(index);
		else
			VDialogManager.showMessageDialogWithBodyAndTextArea(
					"Fix invalid json before leaving tab.",
					"Repair the json before leaving this tab.",
					errorMsg, JOptionPane.ERROR_MESSAGE);
	}

}
