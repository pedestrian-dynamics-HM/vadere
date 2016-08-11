package org.vadere.gui.projectview.control;

import org.vadere.simulator.projects.VadereProject;

public interface IProjectChangeListener {

	void projectChanged(final VadereProject project);

	void propertyChanged(final VadereProject project);

}
