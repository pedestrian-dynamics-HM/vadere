package org.vadere.gui.postvisualization.control;


import org.vadere.gui.components.control.simulation.ActionVisualization;
import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.view.SimulationRenderer;
import org.vadere.util.logging.Logger;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * @author Christina Mayr
 */
public class ActionImageOverlay extends ActionVisualization {
    private static Logger logger = Logger.getLogger(ActionImageOverlay.class);
    private final SimulationModel<? extends DefaultSimulationConfig> model;

    public ActionImageOverlay(final String name, final Icon icon, final SimulationModel<? extends DefaultSimulationConfig> model) {
        super(name, icon, model);
        this.model = model;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

	    String[] possibilities = {
	    		Messages.getString("OnlineVis.msgDialogImageOverlay.target")};
	    String s = (String)JOptionPane.showInputDialog(
			    null,
			    Messages.getString("OnlineVis.msgDialogShowPotentialfield.text"),
			    Messages.getString("OnlineVis.msgDialogShowPotentialfield.title"),
			    JOptionPane.PLAIN_MESSAGE,
			    null,
			    possibilities,
			    possibilities[0]);

		//If a string was returned, say so.
	    if(possibilities[0].equals(s)) {
		    model.config.setShowImage(true);
		    model.notifyObservers();
	    }
	    else if(possibilities[1].equals(s)) {
		    model.config.setShowImage(false);
		    model.notifyObservers();
	    }
	    else {
		    model.config.setShowImage(false);
	    }


    }


}
