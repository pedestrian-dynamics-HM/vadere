package org.vadere.gui.topographycreator.view;

import java.util.Observable;
import java.util.Observer;

import javax.swing.JLabel;

import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.state.scenario.ScenarioElement;

public class JLabelObserver extends JLabel implements Observer {

	public static final String DEFAULT_TEXT = Messages.getString("ProjectView.JSONDisplay.label");
	private static final long serialVersionUID = 9011952047793438028L;

	private IDrawPanelModel panelModel;

	public JLabelObserver(String labelText) {
		super(labelText);
	}

	public void setPanelModel(IDrawPanelModel panelModel) {
		this.panelModel = panelModel;
	}

	@Override
	public void update(Observable arg0, Object arg1) {
		if (panelModel != null) {
			ScenarioElement selectedElement = panelModel.getSelectedElement();
			String newText = DEFAULT_TEXT;

			if (selectedElement != null)
				newText = selectedElement.getClass().getSimpleName().toString();

			setText(newText);
		}
	}

}
