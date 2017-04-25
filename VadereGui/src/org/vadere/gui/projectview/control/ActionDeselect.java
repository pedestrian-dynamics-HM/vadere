package org.vadere.gui.projectview.control;

import org.vadere.gui.components.model.IDefaultModel;
import org.vadere.gui.topographycreator.control.ActionSelectSelectShape;

import java.awt.event.ActionEvent;

import javax.swing.*;

public class ActionDeselect extends AbstractAction {

	private static final long serialVersionUID = 1L;
	private final IDefaultModel panelModel;
	private JPanel jpanel;
	private ActionSelectSelectShape selectSelectShape;

	public ActionDeselect(final IDefaultModel panelModel, JPanel jpanel, ActionSelectSelectShape selectSelectShape) {
		this.panelModel = panelModel;
		this.jpanel = jpanel;
		this.selectSelectShape = selectSelectShape;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		panelModel.deselectSelectedElement();

		//Reset to select mode
		if (selectSelectShape != null) {
			selectSelectShape.actionPerformed(e);
		}

		jpanel.repaint();
	}
}
