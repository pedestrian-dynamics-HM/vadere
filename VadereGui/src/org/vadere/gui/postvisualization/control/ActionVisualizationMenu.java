package org.vadere.gui.postvisualization.control;

import org.vadere.gui.components.control.simulation.ActionVisualization;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.*;

public class ActionVisualizationMenu extends ActionVisualization {

	private final List<Action> actions;
	private JPopupMenu menu;
	private Component parent;
	private final ActionVisualization action;

	public ActionVisualizationMenu(String name, Icon icon, PostvisualizationModel model,
								   final ActionVisualization action,
								   final List<Action> actions) {
		super(name, icon, model);
		this.action = action;
		this.actions = actions;
	}

	public ActionVisualizationMenu(String name, PostvisualizationModel model,
								   final ActionVisualization action,
								   final List<Action> actions) {
		super(name, model);
		this.action = action;
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

//		action.actionPerformed(e);
	}
}
