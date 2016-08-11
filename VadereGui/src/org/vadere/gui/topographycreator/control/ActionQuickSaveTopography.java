package org.vadere.gui.topographycreator.control;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;

import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.gui.topographycreator.utils.JSONWriter;

/**
 * Action: save the topography to the current file (last_save_point).
 * 
 *
 */
public class ActionQuickSaveTopography extends TopographyAction {
	private static Resources resources = Resources.getInstance("topologycreator");
	private static final long serialVersionUID = -6802761549259354252L;

	public ActionQuickSaveTopography(String name, ImageIcon icon, IDrawPanelModel panelModel) {
		super(name, icon, panelModel);
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
		String lastSavePoint = resources.getProperty("last_save_point");

		if (lastSavePoint == null) {
			final JFileChooser fc = new JFileChooser();

			int returnVal = fc.showDialog(null, "Save");

			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File file = fc.getSelectedFile().toString().endsWith(".json") ? fc.getSelectedFile()
						: new File(fc.getSelectedFile().toString() + ".json");
				JSONWriter.writeTopography(getScenarioPanelModel().build(), file);
				resources.putProperty("last_save_point", file.getAbsolutePath());
			}
		} else {
			JSONWriter.writeTopography(getScenarioPanelModel().build(), new File(lastSavePoint));
		}
	}


}
