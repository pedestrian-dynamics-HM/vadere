package org.vadere.gui.onlinevisualization.control;


import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.view.SimulationRenderer;
import org.vadere.gui.onlinevisualization.view.IRendererChangeListener;
import org.vadere.util.logging.Logger;

import java.awt.event.ActionEvent;

import javax.swing.*;

/**
 * @author Benedikt Zoennchen
 */
public class ActionShowPotentialField extends AbstractAction implements IRendererChangeListener {
    private static Logger logger = Logger.getLogger(ActionShowPotentialField.class);
    private final SimulationModel<? extends DefaultSimulationConfig> model;

    public ActionShowPotentialField(final String name, final Icon icon, final SimulationModel<? extends DefaultSimulationConfig> model) {
        super(name, icon);
        this.model = model;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

	    String[] possibilities = {
	    		Messages.getString("OnlineVis.msgDialogShowPotentialfield.target"),
			    Messages.getString("OnlineVis.msgDialogShowPotentialfield.overall"),
			    Messages.getString("OnlineVis.msgDialogShowPotentialfield.none")};
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
		    model.config.setShowTargetPotentialField(true);
		    model.config.setShowPotentialField(false);
		    model.notifyObservers();
	    }
	    else if(possibilities[1].equals(s)) {
		    model.config.setShowTargetPotentialField(false);
		    model.config.setShowPotentialField(true);
		    model.notifyObservers();
	    }
	    else {
		    model.config.setShowTargetPotentialField(false);
		    model.config.setShowPotentialField(false);
	    }


    }

    @Override
    public void update(SimulationRenderer renderer) {}
}
