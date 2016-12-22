package org.vadere.simulator.projects;

public interface SingleScenarioFinishedListener {
	void preScenarioRun(final Scenario scenario, final int scenariosLeft);

	void postScenarioRun(final Scenario scenario, final int scenariosLeft);

	void scenarioStarted(final Scenario scenario, final int scenariosLeft);

	void error(final Scenario scenario, final int scenariosLeft, final Throwable throwable);

	void scenarioPaused(final Scenario scenario, final int scenariosLeft);

	void scenarioInterrupted(final Scenario scenario, final int scenariosLeft);
}
