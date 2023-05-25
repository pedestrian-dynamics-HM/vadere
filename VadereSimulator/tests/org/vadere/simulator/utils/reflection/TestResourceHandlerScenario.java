package org.vadere.simulator.utils.reflection;

import static org.junit.Assert.fail;

import java.io.IOException;
import org.vadere.simulator.entrypoints.ScenarioFactory;
import org.vadere.simulator.projects.Scenario;
import org.vadere.util.test.TestResourceHandler;

/**
 * Manage resource directory used within a unit test. Implement {@link #getTestDir()} method and use
 * default implementation to access resources.
 *
 * <p>If a test changes files during execution it is useful to call {@link #backupTestDir()} in the
 * (at)Before method and {@link #loadFromBackup()} in the (at)After method to ensure a clean working
 * environment for each test even if previous test failed.
 */
public interface TestResourceHandlerScenario extends TestResourceHandler {

  default Scenario getScenarioFromRelativeResource(String name) {
    Scenario scenario = null;
    try {
      scenario = ScenarioFactory.createScenarioWithScenarioFilePath(getRelativeTestPath(name));
    } catch (IOException e) {
      fail("Resource not found: " + getTestDir().resolve(name).toString());
    }
    return scenario;
  }
}
