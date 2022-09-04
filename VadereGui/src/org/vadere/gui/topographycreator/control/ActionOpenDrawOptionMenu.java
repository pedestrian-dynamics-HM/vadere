package org.vadere.gui.topographycreator.control;

import javax.swing.*;

import org.vadere.gui.components.utils.Resources;
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
	private final List<Action> drawActions;
	private final List<Action> miscActions;
	private JPopupMenu menu;

	public ActionOpenDrawOptionMenu(final String name, final String iconPath,String shortDescription, final IDrawPanelModel panelModel,
			final TopographyAction action, final Component parent, final List<Action> drawActions, final List<Action> miscActions) {
		super(name,iconPath,shortDescription, panelModel);
		this.action = action;
		this.parent = parent;
		this.drawActions = drawActions;
		this.miscActions = miscActions;
	}

	public ActionOpenDrawOptionMenu(final String name, final IDrawPanelModel panelModel, final TopographyAction action,
			final Component parent, final List<Action> actions, final List<Action> miscActions) {
		super(name, panelModel);
		this.action = action;
		this.parent = parent;
		this.drawActions = actions;
		this.miscActions = null;
	}

	public ActionOpenDrawOptionMenu(final String name, final IDrawPanelModel panelModel, final TopographyAction action,
									final Component parent, final List<Action> actions) {
		this(name, panelModel, action, parent, actions, null);
	}

	public ActionOpenDrawOptionMenu(final String name, final String iconPath,String shortDescription, final IDrawPanelModel panelModel,
									final TopographyAction action,JComponent parent, final List<Action> drawActions) {
		this(name, iconPath,shortDescription,
				panelModel,
				action,
				parent,
				drawActions, null);
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				if (menu == null) {
					menu = new JPopupMenu();
					for (Action action : drawActions) {
						menu.add(action);
					}
					if (miscActions != null){
						menu.addSeparator();
						miscActions.forEach(a -> menu.add(a));
					}
				}
				menu.show(parent, 0, parent.getHeight());
			}
		});

		action.actionPerformed(e);
	}
}
