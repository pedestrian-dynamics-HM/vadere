package org.vadere.gui.components.utils;

import javax.swing.*;

import org.vadere.gui.projectview.view.ProjectView;

import java.awt.*;

public class SwingUtils {

	public static JButton addActionToToolbar(final JToolBar toolbar, final Action action, final String tooltip) {
		JButton button = toolbar.add(action);
		button.setBorderPainted(false);
		button.setToolTipText(tooltip);
		return button;
	}

	public static JToggleButton addToggleActionToToolbar(final JToolBar toolbar, final Action action, final String tooltip) {
		JToggleButton button = new JToggleButton();
		button.setAction(action);
		button.setText("");
		button.setFocusable(false);
		button.setToolTipText(tooltip);
		toolbar.add(button);
		return button;
	}

	public static void centerComponent(final Component component) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		component.setLocation(screenSize.width / 2 - component.getSize().width / 2,
				screenSize.height / 2 - component.getSize().height / 2);
	}

}
