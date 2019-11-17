package org.vadere.simulator.projects.dataprocessing.processor.tests;

import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.models.osm.optimization.OptimizationMetric;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.EventtimePedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.processor.PedestrianMetricOptimizationProcessor;
import org.vadere.state.attributes.processor.AttributesTestOptimizationMetricProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


@DataProcessorClass()
public class TestOptimizationMetricNelderMeadProcessor extends TestProcessor {

	private PedestrianMetricOptimizationProcessor pedestrianMetricOptimizationProcessor;
	private TestEvacuationTimeProcessor testEvacuationTimeProcessor;
	private String scenarioName;

	public TestOptimizationMetricNelderMeadProcessor() {
		super("test-pedestrianMetricOptimizationProcessor");
		setAttributes(new AttributesTestOptimizationMetricProcessor());
	}

	@Override
	public void init(@NotNull final ProcessorManager manager) {
		super.init(manager);

		// will be set by during "doUpdate", because the name is only accessible via the SimulationState
		this.scenarioName = null;

		AttributesTestOptimizationMetricProcessor att = this.getAttributes();

		pedestrianMetricOptimizationProcessor =
				(PedestrianMetricOptimizationProcessor) manager.getProcessor(att.getOptimizationMetricProcessorId());

		testEvacuationTimeProcessor =
				(TestEvacuationTimeProcessor) manager.getProcessor(att.getTestEvacuationProcessorId());

	}

	@Override
	public void preLoop(SimulationState state) {
		pedestrianMetricOptimizationProcessor.preLoop(state);
		testEvacuationTimeProcessor.preLoop(state);
	}

	@Override
	protected void doUpdate(@NotNull final SimulationState state) {

		if(this.scenarioName == null){
			this.scenarioName = state.getName();
		}else{
			if(!this.scenarioName.equals(state.getName())){
				throw new RuntimeException("The scenario name should never get changed during simulation!");
			}
		}

		pedestrianMetricOptimizationProcessor.update(state);
		testEvacuationTimeProcessor.update(state);
	}

	@Override
	public void postLoop(SimulationState state) {
		// Check if every agent reached the target
		testEvacuationTimeProcessor.postLoop(state);

		// Check how the metric changed compared to the set values
		Map<EventtimePedestrianIdKey, OptimizationMetric> processorData =
				pedestrianMetricOptimizationProcessor.getData();

		ArrayList<Double> pointDistanceL2Values = new ArrayList<>();
		ArrayList<Double> differenceFuncValues = new ArrayList<>();

		for(OptimizationMetric singleMetric : processorData.values()){

			pointDistanceL2Values.add(singleMetric.getOptimalPoint().distance(singleMetric.getFoundPoint()));

			// Insert all values for difference in the function values.
			differenceFuncValues.add(singleMetric.getFoundFuncValue() - singleMetric.getOptimalFuncValue());
		}

		if(pointDistanceL2Values.isEmpty() || differenceFuncValues.isEmpty()){
			throw new NullPointerException("No values to compare. Reasons can be that i) there are no agents in the " +
					"scenario, ii) the option to compare with the brute force is turned off (see " +
					"`Testing.stepCircleOptimization.compareBruteForceSolution` in Vadere.conf) or iii) the " +
					"optimizer does not support setting the OptimizationMetric of iv) the simulation failed " +
					"unexpected.");
		}

		var metricStatistics = computeStatistics(pointDistanceL2Values, differenceFuncValues);

		printStatistics(metricStatistics);

		AttributesTestOptimizationMetricProcessor attr = this.getAttributes();
		String msg = getCompareValuesString("mean difference in point distance",
				metricStatistics.get("meanPointDistance"), attr.getMaxMeanPointDistance());

		handleAssertion(metricStatistics.get("meanPointDistance")<=attr.getMaxMeanPointDistance(), msg);

		msg = getCompareValuesString("mean difference in function value",
				metricStatistics.get("meanDifferenceFuncValue"), attr.getMaxMeanPointDistance());
		handleAssertion(
				metricStatistics.get("meanDifferenceFuncValue")<=attr.getMaxMeanDifferenceFuncValue(), msg);
	}

	private String getCompareValuesString(String valueName, double newValue, double referenceValue){
		double diff = newValue - referenceValue;
		String msg;

		if(newValue < referenceValue){
			msg = "POSITIVE -- The statistics '" + valueName + "' decreased by " + diff
					+ " (BEFORE:" + referenceValue + " NOW: " + newValue + ")";
		}else if(newValue > referenceValue){
			msg = "NEGATIVE -- The statistics '" + valueName + "' increased by " + diff +
					" (BEFORE:" + referenceValue + " NOW: " + newValue + ")";
		}else{
			msg = "NEUTRAL  -- The statistics '" + valueName + "' is equal to the reference value. Value = " + newValue;
		}
		return msg;
	}


	private void printStatistics(HashMap<String, Double> statistics){

		AttributesTestOptimizationMetricProcessor attr = this.getAttributes();

		System.out.println("######################################################################################");
		System.out.println("######################################################################################");
		System.out.println("######################################################################################");

		System.out.println("INFORMATION FROM TestOptimizationMetricNelderMeadProcessor");
		System.out.println();

		System.out.println("Main metric:");
		System.out.println(getCompareValuesString("mean function difference", statistics.get("meanDifferenceFuncValue"), attr.getMaxMeanDifferenceFuncValue()));
		System.out.println(getCompareValuesString("mean point distance", statistics.get("meanPointDistance"), attr.getMaxMeanPointDistance()));

		System.out.println();
		System.out.println("Further information:");
		System.out.println(getCompareValuesString("minimum point distance", statistics.get("minPointDistanceL2"), attr.getInfoMinPointDistanceL2()));
		System.out.println(getCompareValuesString("maximum point distance", statistics.get("maxPointDistanceL2"), attr.getInfoMaxPointDistanceL2()));
		System.out.println(getCompareValuesString("standard deviation point distance", statistics.get("stddevPointDistance"), attr.getInfoStddevPointDistance()));

		System.out.println(getCompareValuesString("minimum function difference", statistics.get("minDifferenceFuncValue"), attr.getInfoMinFuncDifference()));
		System.out.println(getCompareValuesString("maximum function difference", statistics.get("maxDifferenceFuncValue"), attr.getInfoMaxFuncDifference()));
		System.out.println(getCompareValuesString("standard deviation function difference", statistics.get("stddevDifferenceFuncValue"), attr.getInfoStddevDifferenceFuncValue()));

		System.out.println("\n In JSON format (for copying into TestProcessor");

		System.out.println("\"maxMeanPointDistance\" : " + statistics.get("meanPointDistance") + ",");
		System.out.println("\"maxMeanDifferenceFuncValue\" : " + statistics.get("meanDifferenceFuncValue") + ",");
		System.out.println("\"infoMinPointDistanceL2\" : " + statistics.get("minPointDistanceL2") + ",");
		System.out.println("\"infoMaxPointDistanceL2\" : " + statistics.get("maxPointDistanceL2") + ",");
		System.out.println("\"infoMinFuncDifference\" : " + statistics.get("minDifferenceFuncValue") + ",");
		System.out.println("\"infoMaxFuncDifference\" : " + statistics.get("maxDifferenceFuncValue") + ",");
		System.out.println("\"infoStddevPointDistance\" : " + statistics.get("stddevPointDistance") + ",");
		System.out.println("\"infoStddevDifferenceFuncValue\" : " + statistics.get("stddevDifferenceFuncValue"));

		System.out.println("######################################################################################");
		System.out.println("######################################################################################");

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
