package org.vadere.gui.topographycreator.control;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;

import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.gui.topographycreator.utils.TopographyJsonWriter;

/**
 * Action: save the topography to a new file.
 * 
 * 
 */
public class ActionSaveTopography extends TopographyAction {
	private static Resources resources = Resources.getInstance("topographycreator");
	private static final long serialVersionUID = -4666995743959028627L;

	public ActionSaveTopography(String name, ImageIcon icon, IDrawPanelModel panelModel) {
		super(name, icon, panelModel);
	}

	/**
	 * @param name
	 * @param panelModel
	 */
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
			resources.setProperty("last_save_point", file.getAbsolutePath());
		}
	}

}
