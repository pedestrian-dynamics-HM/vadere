package org.vadere.gui.topographycreator.control;

import java.awt.event.ActionEvent;

import org.vadere.gui.topographycreator.model.IDrawPanelModel;

/**
 * A ActionBasic is the last action in the decorator pattern which will be cup.
 * Action: Notify all Observers.
 * 
 */
public class ActionBasic extends TopographyAction {

	private static final long serialVersionUID = 8860920305466879894L;

	public ActionBasic(String name, IDrawPanelModel panelModel) {
		super(name, panelModel);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		getScenarioPanelModel().notifyObservers();
	}

}
