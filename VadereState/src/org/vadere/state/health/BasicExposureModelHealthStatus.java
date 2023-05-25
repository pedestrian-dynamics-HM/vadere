package org.vadere.state.health;

/**
 * BasicExposureModelHealthStatus that is used in combination with: <lu>
 * <li><code>ProximityExposureModel</code>
 * <li><code>PostvisualizationModel</code> </lu>
 */
public class BasicExposureModelHealthStatus extends ExposureModelHealthStatus {

  public BasicExposureModelHealthStatus() {
    super();
  }

  public BasicExposureModelHealthStatus(BasicExposureModelHealthStatus other) {
    super(other.isInfectious(), other.getDegreeOfExposure());
  }

  @Override
  public BasicExposureModelHealthStatus clone() {
    return new BasicExposureModelHealthStatus(this);
  }
}
