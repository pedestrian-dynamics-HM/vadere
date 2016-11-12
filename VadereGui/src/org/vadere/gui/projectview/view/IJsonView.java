package org.vadere.gui.projectview.view;


import org.vadere.simulator.projects.Scenario;

public interface IJsonView {

	void setVadereScenario(Scenario scenario);
	void isEditable(boolean isEditable);

}
