package org.vadere.gui.projectview.view;


import org.vadere.simulator.projects.ScenarioRunManager;

public interface IJsonView {

	void setVadereScenario(ScenarioRunManager scenario);
	void isEditable(boolean isEditable);

}
