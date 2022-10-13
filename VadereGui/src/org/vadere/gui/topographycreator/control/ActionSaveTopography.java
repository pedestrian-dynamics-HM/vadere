package org.vadere.gui.topographycreator.control;

import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.gui.topographycreator.utils.TopographyJsonWriter;
import org.vadere.util.config.VadereConfig;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * Action: save the topography to a new file.
 * 
 * 
 */
public class ActionSaveTopography extends TopographyAction {

	private static final long serialVersionUID = -4666995743959028627L;

	public ActionSaveTopography(String name, String iconPath, IDrawPanelModel panelModel) {
		super(name, iconPath, panelModel);
	}
	public ActionSaveTopography(String name, IDrawPanelModel panelModel) {
		super(name, panelModel);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		final JFileChooser fc = new JFileChooser();

		int returnVal = fc.showDialog(null, "Save");

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			// new XMLWriter(getScenarioPanelModel(), file).writeScenario();
			TopographyJsonWriter.writeTopography(getScenarioPanelModel().build(), file);
			VadereConfig.getConfig().setProperty("last_save_point", file.getAbsolutePath());
		}
	}

}
