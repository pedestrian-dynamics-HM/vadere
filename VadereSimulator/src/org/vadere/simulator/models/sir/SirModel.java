package org.vadere.simulator.models.sir;

import org.vadere.simulator.models.Model;
import org.vadere.state.health.InfectionStatus;
import org.vadere.state.scenario.Pedestrian;

public interface SirModel extends Model {
	// add any methods **ALL** SIR models have in common.

    default void updatePedestrianInfectionStatus(Pedestrian pedestrian, double simTimeInSec) {
        InfectionStatus infectionStatus = pedestrian.getInfectionStatus();
        switch (infectionStatus) {
            case SUSCEPTIBLE:
                if (pedestrian.getPathogenAbsorbedLoad() >= pedestrian.getSusceptibility()) {
                    pedestrian.setInfectionStatus(InfectionStatus.EXPOSED);
                    pedestrian.setLastInfectionStatusUpdateTime(simTimeInSec);
                }
                break;
            case EXPOSED:
                if (simTimeInSec >= pedestrian.getLastInfectionStatusUpdateTime() + pedestrian.getExposedPeriod()) {
                    pedestrian.setInfectionStatus(InfectionStatus.INFECTIOUS);
                    pedestrian.setLastInfectionStatusUpdateTime(simTimeInSec);
                }
                break;
            case INFECTIOUS:
                if (simTimeInSec >= pedestrian.getLastInfectionStatusUpdateTime() + pedestrian.getInfectiousPeriod()) {
                    pedestrian.setInfectionStatus(InfectionStatus.RECOVERED);
                    pedestrian.setLastInfectionStatusUpdateTime(simTimeInSec);
                    pedestrian.setPathogenAbsorbedLoad(0.0);
                }
                break;
            case RECOVERED:
                if (simTimeInSec >= pedestrian.getLastInfectionStatusUpdateTime() + pedestrian.getRecoveredPeriod()) {
                    pedestrian.setInfectionStatus(InfectionStatus.SUSCEPTIBLE);
                }
                break;
        }
    }

    default void updatePedestrianPathogenAbsorbedLoad(Pedestrian pedestrian, double pathogenLoad) {
        InfectionStatus infectionStatus = pedestrian.getInfectionStatus();
        double absorbedPathogen = pedestrian.getPathogenAbsorptionRate() * pathogenLoad;
        double accumulatedAbsorbedPathogenLoad = pedestrian.getPathogenAbsorbedLoad() + absorbedPathogen;

        switch (infectionStatus) {
            case SUSCEPTIBLE:
                pedestrian.setPathogenAbsorbedLoad(accumulatedAbsorbedPathogenLoad);
                break;
            case EXPOSED:
                pedestrian.setPathogenAbsorbedLoad(accumulatedAbsorbedPathogenLoad);
                break;
            case INFECTIOUS:
            case RECOVERED:
                break; // do not absorb
            default:
                throw new IllegalStateException("Unexpected value: " + infectionStatus);
        }
    }


}
