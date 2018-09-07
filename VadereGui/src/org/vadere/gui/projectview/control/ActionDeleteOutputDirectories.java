package org.vadere.gui.projectview.control;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.gui.projectview.view.VTable;
import org.vadere.util.io.IOUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

public class ActionDeleteOutputDirectories extends AbstractAction {
	private static final long serialVersionUID = 1L;

	private static Logger logger = LogManager.getLogger(ActionDeleteOutputDirectories.class);

	private ProjectViewModel model;
	private VTable outputTable;

	public ActionDeleteOutputDirectories(final String name, final ProjectViewModel model, VTable outputTable) {
		super(name);
		this.model = model;
		this.outputTable = outputTable;
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		final int selectedCount = outputTable.getSelectedRowCount();
		final String questionText = selectedCount > 1
				? Messages.getString("DeleteOutputQuestionMultiple.text")
				: Messages.getString("DeleteOutputQuestionOne.text");

		if (selectedCount > 0 && IOUtils.chooseYesNoCancel(questionText,
				Messages.getString("DeleteOutputQuestion.title")) == JOptionPane.YES_OPTION) {
			try {
				model.deleteOutputFiles(outputTable.getSelectedRows());
			} catch (IOException e) {
				e.printStackTrace();
				logger.error(e.getLocalizedMessage());
			}
		}
	}
}
