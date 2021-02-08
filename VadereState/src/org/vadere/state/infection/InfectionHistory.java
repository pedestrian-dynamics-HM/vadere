/**
 * Author: Mina Abadeer
 * Group Parallel and Distributed Systems
 * Department of Computer Science
 * University of Muenster, Germany
 *
 * Co-author(s):
 * Sameh Magharious
 * Dell Technologies, USA
 *
 * This software is licensed under the GNU Lesser General Public License (LGPL).
 */

package org.vadere.state.infection;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class InfectionHistory {
    //TODO: consider using atomic integers
    @Getter
    private boolean inInfectionZoneLastStep;
    @Getter
    private int totalStepsInInfectionZone = 0;
    @Getter
    private int totalStepsSinceInfected = 0;
    @Getter
    private int totalStepsSinceRecovered = 0;

    public void intoInfectionZone() {
        this.totalStepsInInfectionZone = 1;
        this.inInfectionZoneLastStep = true;
    }

    public void incrementTotalStepsInInfectionZone() {
        this.totalStepsInInfectionZone += 1;
    }
    public void resetTotalStepsInInfectionZone() {
        totalStepsInInfectionZone = 0;
    }

    public void incrementTotalStepsSinceInfected() {
        totalStepsSinceInfected += 1;
    }
    public void resetTotalStepsSinceInfected() {
        totalStepsSinceInfected = 0;
    }

    public void incrementTotalStepsSinceRecovered() {
        totalStepsSinceRecovered += 1;
    }
    public void resetTotalStepsSinceRecovered() {
        totalStepsSinceRecovered = 0;
    }


}
