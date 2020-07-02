package org.vadere.gui.topographycreator.view;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Observable;
import java.util.Observer;

import javax.swing.*;

import org.vadere.gui.components.control.HelpTextView;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.projectview.view.VDialogManager;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.state.scenario.ScenarioElement;

public class JLabelObserver extends JLabel implements Observer {

	public static final String DEFAULT_TEXT = Messages.getString("ProjectView.JSONDisplay.label");
	private static final long serialVersionUID = 9011952047793438028L;

	private IDrawPanelModel panelModel;
	private String selectedElementAttrFQN;

	public JLabelObserver(String labelText) {
		super(labelText);
		setForeground(Color.BLUE.darker());
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!selectedElementAttrFQN.equals("")){
					String body = getText() + ": Help and Field Description";
					VDialogManager.showMessageDialogWithBodyAndTextEditorPane("Help", body,
							HelpTextView.create(selectedElementAttrFQN), JOptionPane.INFORMATION_MESSAGE);
				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				super.mouseEntered(e);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				super.mouseExited(e);
			}
		});
	}



	public void setPanelModel(IDrawPanelModel panelModel) {
		this.panelModel = panelModel;
	}

	@Override
	public void update(Observable arg0, Object arg1) {
		if (panelModel != null) {
			ScenarioElement selectedElement = panelModel.getSelectedElement();
			String newText = DEFAULT_TEXT;

			if (selectedElement != null){
				newText = selectedElement.getClass().getSimpleName().toString();
				selectedElementAttrFQN = selectedElement.getAttributes().getClass().getName();
			} else {
				selectedElementAttrFQN = "";
			}

			setText(newText);
		}
	}

}
