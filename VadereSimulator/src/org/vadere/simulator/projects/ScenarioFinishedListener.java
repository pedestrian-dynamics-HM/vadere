package org.vadere.simulator.projects;

public interface ScenarioFinishedListener {
	void scenarioStarted(final Scenario scenario);

	void scenarioFinished(final Scenario scenario);

	void scenarioRunThrewException(final Scenario scenario, final Throwable ex);
}
