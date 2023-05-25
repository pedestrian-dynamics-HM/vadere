package org.vadere.simulator.projects.dataprocessing.processor;

import org.junit.Before;
import org.junit.Test;

public class PedestrianDensityCountingProcessorTest extends ProcessorTest {

  @Before
  public void setup() {
    processorTestEnv = new PedestrianDensityCountingProcessorTestEnv();
    super.setup();
  }

  @Test
  public void init() throws Exception {
    super.init();
  }

  @Test
  public void doUpdate() throws Exception {
    super.doUpdate();
  }
}
