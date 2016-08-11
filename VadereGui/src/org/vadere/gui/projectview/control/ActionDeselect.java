package org.vadere.gui.projectview.control;

import javax.swing.*;

import org.vadere.gui.components.model.IDefaultModel;

import java.awt.event.ActionEvent;

public class ActionDeselect extends AbstractAction {

	private static final long serialVersionUID = 1L;
	private final IDefaultModel panelModel;
	private JPanel jpanel;

	public ActionDeselect(final IDefaultModel panelModel, JPanel jpanel) {
		this.panelModel = panelModel;
		this.jpanel = jpanel;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		panelModel.deselectSelectedElement();
		jpanel.repaint();
	}
}
