package org.vadere.gui.onlinevisualization.control;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.*;

public class ActionOnlineVisMenu extends AbstractAction {

	private final List<Action> actions;
	private JPopupMenu menu;
	private Component parent;


	public ActionOnlineVisMenu(final String name, Icon icon, final List<Action> actions) {
		super(name, icon);
		this.actions = actions;
	}

	public void setParent(Component parent) {
		this.parent = parent;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				if (menu == null) {
					menu = new JPopupMenu();
					for (Action action : actions) {
						menu.add(action);
					}
				}
				menu.show(parent, 0, parent.getHeight());
			}
		});
	}
}
