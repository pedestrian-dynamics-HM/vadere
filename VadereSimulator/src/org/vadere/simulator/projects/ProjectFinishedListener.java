package org.vadere.simulator.projects;

public interface ProjectFinishedListener {
	void preProjectRun(final VadereProject project);

	void postProjectRun(final VadereProject project);
}
