package org.vadere.gui.components.view;

import javax.swing.*;

import org.vadere.gui.components.model.IDefaultModel;

import java.util.Observable;
import java.util.Observer;

public class ScenarioScrollPane extends JScrollPane implements Observer {

	public ScenarioScrollPane(final JComponent component, final IDefaultModel defaultModel) {
		super(component);
	}

	@Override
	public void update(Observable o, Object arg) {}
}
