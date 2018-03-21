package org.vadere.gui.topographycreator.control;

import org.vadere.gui.postvisualization.view.DialogFactory;
import org.vadere.gui.projectview.view.ProjectView;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VRectangle;

import java.awt.event.ActionEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;

public class ActionResizeTopographyBound extends TopographyAction {

	private TopographyAction action;

	public ActionResizeTopographyBound(String name, ImageIcon icon, IDrawPanelModel<?> panelModel,
									   TopographyAction action) {
		super(name, icon, panelModel);
		this.action = action;
	}

	public ActionResizeTopographyBound(final String name, final IDrawPanelModel<?> panelModel,
									   TopographyAction action) {
		super(name, panelModel);
		this.action = action;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		//set to Selection to to be sure no accidental changes are introduced
		action.actionPerformed(e);

		TopographyCreatorModel model = (TopographyCreatorModel) getScenarioPanelModel();
		JTextField textField = new JTextField();
		textField.setText(String.format("%.3f x %.3f",
				model.getTopography().getBounds().width,
				model.getTopography().getBounds().height));
		if (JOptionPane.showConfirmDialog(ProjectView.getMainWindow(), textField,
				"Set width x height", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION){
			String input = textField.getText().trim().replace(" ", "");
			String[] in = input.split("x");
			double width;
			double height;
			VRectangle bound = null;
			try{
				width = Double.valueOf(in[0]);
				height = Double.valueOf(in[1]);
				bound = new VRectangle(0.0,0.0,width, height);
			}catch (NumberFormatException exp){
				System.out.println("wrong format");
			}
			if (bound != null){
				model.setTopographyBound(bound);
			}
		}
		getScenarioPanelModel().notifyObservers();
	}
}
