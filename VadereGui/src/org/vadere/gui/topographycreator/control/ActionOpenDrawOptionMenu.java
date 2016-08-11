package org.vadere.gui.topographycreator.control;

import javax.swing.*;

import org.vadere.gui.topographycreator.model.IDrawPanelModel;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * Action: Opens the menu containing a set of drawing methods (for example: rectangle, pen, ...).
 * 
 *
 */
public class ActionOpenDrawOptionMenu extends TopographyAction {

	private static final long serialVersionUID = 2337382087222665146L;
	private final TopographyAction action;
	private final Component parent;
	private final List<Action> actions;
	private JPopupMenu menu;

	public ActionOpenDrawOptionMenu(final String name, final ImageIcon icon, final IDrawPanelModel panelModel,
			final TopographyAction action, final Component parent, final List<Action> actions) {
		super(name, icon, panelModel);
		this.action = action;
		this.parent = parent;
		this.actions = actions;
	}

	public ActionOpenDrawOptionMenu(final String name, final IDrawPanelModel panelModel, final TopographyAction action,
			final Component parent, final List<Action> actions) {
		super(name, panelModel);
		this.action = action;
		this.parent = parent;
		this.actions = actions;
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

		action.actionPerformed(e);
	}
}
