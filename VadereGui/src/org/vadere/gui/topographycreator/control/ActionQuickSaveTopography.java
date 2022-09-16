package org.vadere.gui.topographycreator.control;

import org.apache.commons.configuration2.Configuration;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.gui.topographycreator.utils.TopographyJsonWriter;
import org.vadere.util.config.VadereConfig;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * Action: save the topography to the current file (last_save_point).
 * 
 *
 */
public class ActionQuickSaveTopography extends TopographyAction {

	private static final long serialVersionUID = -6802761549259354252L;
	private static final Configuration CONFIG = VadereConfig.getConfig();

	public ActionQuickSaveTopography(String name, String iconPath,String shortDescription, IDrawPanelModel panelModel) {
		super(name, iconPath, shortDescription, panelModel);
	}

	/**
	 * @param name
	 * @param panelModel
	 */
	public ActionQuickSaveTopography(String name, IDrawPanelModel panelModel) {
		super(name, panelModel);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String lastSavePoint = CONFIG.getString("Gui.lastSavePoint");

		if (lastSavePoint == null) {
			final JFileChooser fc = new JFileChooser();
			int returnVal = fc.showDialog(null, "Save");

			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File file = fc.getSelectedFile().toString().endsWith(".json") ? fc.getSelectedFile()
						: new File(fc.getSelectedFile().toString() + ".json");
				TopographyJsonWriter.writeTopography(getScenarioPanelModel().build(), file);

				CONFIG.setProperty("Gui.lastSavePoint", file.getAbsolutePath());
			}
		} else {
			TopographyJsonWriter.writeTopography(getScenarioPanelModel().build(), new File(lastSavePoint));
		}
	}


}
