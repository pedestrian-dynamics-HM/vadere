package org.vadere.simulator.models.seating;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.vadere.simulator.models.seating.trainmodel.TestCompartment;
import org.vadere.simulator.models.seating.trainmodel.TestSeat;
import org.vadere.simulator.models.seating.trainmodel.TestSeatGroup;
import org.vadere.simulator.models.seating.trainmodel.TestTrainModel;

@RunWith(Suite.class)
@SuiteClasses({ TestChooseCompartment.class, TestChooseSeat.class, TestChooseSeatGroup.class, TestCompartment.class,
		TestSeat.class, TestSeatGroup.class, TestTrainModel.class })
public class AllSeatingModelRelatedTests {

}
