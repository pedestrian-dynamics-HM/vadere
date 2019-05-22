package org.vadere.simulator.projects.dataprocessing.processor.tests;

import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.models.osm.optimization.OptimizationMetric;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.EventtimePedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.processor.PedestrianEvacuationTimeProcessor;
import org.vadere.simulator.projects.dataprocessing.processor.PedestrianMetricOptimizationProcessor;
import org.vadere.simulator.projects.migration.MigrationLogger;
import org.vadere.state.attributes.processor.AttributesTestNumberOverlapsProcessor;
import org.vadere.state.attributes.processor.AttributesTestOptimizationMetricProcessor;
import org.vadere.state.attributes.processor.AttributesTestPedestrianEvacuationTimeProcessor;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Map.entry;

/**
 *
 *
 */
@DataProcessorClass()
public class TestOptimizationMetricNelderMeadProcessor extends TestProcessor {

	private PedestrianMetricOptimizationProcessor pedestrianMetricOptimizationProcessor;
	private String scenarioName;

	public TestOptimizationMetricNelderMeadProcessor() {
		super("test-pedestrianMetricOptimizationProcessor");
		// TODO: for now there are no Attributes needed, but maybe it is possible to the test cases via attributes?
		setAttributes(new AttributesTestOptimizationMetricProcessor());
	}

	@Override
	public void init(@NotNull final ProcessorManager manager) {
		super.init(manager);

		// will be set by during "doUpdate", because the name is only accessible via the SimualtionState
		this.scenarioName = null;

		AttributesTestOptimizationMetricProcessor att = this.getAttributes();
		pedestrianMetricOptimizationProcessor =
				(PedestrianMetricOptimizationProcessor) manager.getProcessor(
						att.getOptimizationMetricNelderMeadProcessor());
	}

	@Override
	protected void doUpdate(@NotNull final SimulationState state) {

		if(this.scenarioName == null){
			this.scenarioName = state.getName();
		}else{
			if(!this.scenarioName.equals(state.getName())){
				throw new RuntimeException("This should never happen!");
			}
		}

		pedestrianMetricOptimizationProcessor.update(state);
	}

	@Override
	public void preLoop(SimulationState state) {
		pedestrianMetricOptimizationProcessor.preLoop(state);
	}

	@Override
	public void postLoop(SimulationState state) {

		Map<EventtimePedestrianIdKey, OptimizationMetric> processorData = pedestrianMetricOptimizationProcessor.getData();

		ArrayList<Double> pointDistanceL2Values = new ArrayList<>();
		ArrayList<Double> differenceFuncValues = new ArrayList<>();

		for(OptimizationMetric singleMetic : processorData.values()){

			pointDistanceL2Values.add(singleMetic.getOptimalPoint().distance(singleMetic.getFoundPoint()));

			// Insert all values for difference in the function values.
			if(singleMetic.getOptimalFuncValue() > singleMetic.getFoundFuncValue()){
				Logger.getLogger(TestOptimizationMetricNelderMeadProcessor.class).warn(
						"Found optimal value is better than brute force. This can indicate that the " +
						"brute force is not fine grained enough.");
			}

			differenceFuncValues.add(singleMetic.getOptimalFuncValue() - singleMetic.getFoundFuncValue());
		}

		var metricStatistics = computeStatistics(pointDistanceL2Values, differenceFuncValues);


		System.out.println("---------------------------------------------------------------");
		System.out.println("OUTPUT FROM PedestrianMetricOptimizationProcessor: ");
		System.out.println("SCENARIO: " + scenarioName);
		System.out.println("STATISTICS: " + metricStatistics);
		System.out.println("---------------------------------------------------------------");

		// TODO: later on checks are required
/*		String msg = invalidEvacuationTimes + "(#invalid evacuation times) <= " + 0;
		handleAssertion(invalidEvacuationTimes <= 0, msg);*/
	}


	private HashMap<String, Double> computeStatistics(ArrayList<Double> pointDistanceL2Values, ArrayList<Double> differenceFuncValues){
		if(differenceFuncValues.size() != pointDistanceL2Values.size()){
			throw new RuntimeException("This should never happen!");
		}

		HashMap<String, Double> statistics = new HashMap<>();
		statistics.put("minPointDistanceL2", Collections.min(pointDistanceL2Values));
		statistics.put("maxPointDistanceL2", Collections.max(pointDistanceL2Values));
		statistics.put("minDifferenceFuncValue", Collections.min(differenceFuncValues));
		statistics.put("maxDifferenceFuncValue", Collections.max(differenceFuncValues));

		int numberElements = differenceFuncValues.size();

		double sumPointDistance = 0;
		double sumDifferenceFuncValue = 0;

		for(int i = 0; i < numberElements; ++i){
			sumPointDistance += pointDistanceL2Values.get(i);
			sumDifferenceFuncValue += differenceFuncValues.get(i);
		}

		statistics.put("meanPointDistance", sumPointDistance / numberElements);
		statistics.put("meanDifferenceFuncValue", sumDifferenceFuncValue / numberElements);

		double stddevPointDistance = 0;
		double stddevDifferenceFuncValue = 0;

		for(int i = 0; i < numberElements; ++i){
			stddevPointDistance += Math.pow((pointDistanceL2Values.get(i) - statistics.get("meanPointDistance")), 2);
			stddevDifferenceFuncValue += Math.pow((differenceFuncValues.get(i) - statistics.get("meanDifferenceFuncValue")), 2);
		}

		stddevPointDistance = Math.sqrt(stddevPointDistance / (numberElements-1));
		stddevDifferenceFuncValue = Math.sqrt(stddevDifferenceFuncValue / (numberElements-1));

		statistics.put("stddevPointDistance", stddevPointDistance);
		statistics.put("stddevDifferenceFuncValue", stddevDifferenceFuncValue);

		return statistics;

	}


	@Override
	public AttributesTestOptimizationMetricProcessor getAttributes() {
		if (super.getAttributes() == null) {
			setAttributes(new AttributesTestOptimizationMetricProcessor());
		}

		return (AttributesTestOptimizationMetricProcessor)super.getAttributes();
	}
}
