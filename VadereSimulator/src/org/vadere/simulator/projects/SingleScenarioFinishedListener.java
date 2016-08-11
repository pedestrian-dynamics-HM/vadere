package org.vadere.simulator.projects;

public interface SingleScenarioFinishedListener {
	void preScenarioRun(final ScenarioRunManager scenario, final int scenariosLeft);

	void postScenarioRun(final ScenarioRunManager scenario, final int scenariosLeft);

	void scenarioStarted(final ScenarioRunManager scenario, final int scenariosLeft);

	void error(final ScenarioRunManager scenario, final int scenariosLeft, final Throwable throwable);

	void scenarioPaused(final ScenarioRunManager scenario, final int scenariosLeft);

	void scenarioInterrupted(final ScenarioRunManager scenario, final int scenariosLeft);
}
