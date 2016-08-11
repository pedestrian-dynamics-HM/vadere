package org.vadere.simulator.projects;

public interface ScenarioFinishedListener {
	void scenarioStarted(final ScenarioRunManager scenario);

	void scenarioFinished(final ScenarioRunManager scenario);

	void scenarioRunThrewException(final ScenarioRunManager scenario, final Throwable ex);
}
