package org.vadere.gui.projectview.control;

import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.utils.SwingUtils;
import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.util.io.IOUtils;
import org.vadere.util.logging.Logger;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.*;

public class ActionOpenInExplorer extends AbstractAction {

    private static Logger logger = Logger.getLogger(ActionRunOutput.class);

    private ProjectViewModel model;

    public ActionOpenInExplorer(final String name, final ProjectViewModel model) {
        super(name);
        this.model = model;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        try {
            ProjectViewModel.OutputBundle outputBundle = model.getSelectedOutputBundle();
            File file = outputBundle.getDirectory();
            if (file.isDirectory()){
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    public Void doInBackground() throws Exception {
                        Desktop.getDesktop().open(file);
                        return null;
                    }
                };
                worker.execute();
            } else {
                IOUtils.errorBox(Messages.getString("OpenInExplorerErrorMessage.text"),
                        Messages.getString("OpenInExplorerErrorMessage.title"));
                logger.errorf("Cannot access outputBundle directory for project: %s", outputBundle.getProject().getName());
            }

        } catch (IOException e) {
            logger.errorf("Cannot get output bundle. %s",e.getMessage());
        }
    }
}
