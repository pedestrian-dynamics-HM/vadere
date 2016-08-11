package org.vadere.gui.topographycreator.control;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;

import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.simulator.projects.io.JsonSerializerTopography;
import org.vadere.state.scenario.Topography;
import org.vadere.util.io.IOUtils;

/**
 * Action: Loads a new Topography and replace the current one.
 * 
 */
public class ActionLoadTopography extends TopographyAction {

	private static Resources resources = Resources.getInstance("topologycreator");
	private static final long serialVersionUID = 2627893667804054668L;

	public ActionLoadTopography(final String name, ImageIcon icon, final IDrawPanelModel panelModel) {
		super(name, icon, panelModel);
	}

	public ActionLoadTopography(final String name, final IDrawPanelModel panelModel) {
		super(name, panelModel);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		/*
		 * final JFileChooser fc = new
		 * JFileChooser(resources.getProperty("View.outputDirectory.path"));
		 * 
		 * int returnVal = fc.showOpenDialog(null);
		 * 
		 * if (returnVal == JFileChooser.APPROVE_OPTION) {
		 * File file = fc.getSelectedFile();
		 * resources.setProperty("View.outputDirectory.path", file.getParent());
		 * try {
		 * String json = IOUtils.readTextFile(file.getAbsolutePath());
		 * Topography topography = JsonSerializerTopography.topographyFromJson(json);
		 * getScenarioPanelModel().setTopography(topography);
		 * getScenarioPanelModel().resetTopographySize();
		 * } catch (Exception e1) {
		 * e1.printStackTrace();
		 * }
		 * }
		 */
	}
}
