package org.vadere.simulator.projects.dataprocessing.processors;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * The factory is mainly for the gui. If one uses processors in the code directly, its
 * easy to create them by hand.
 * 
 *
 */
public class ProcessorFactory {
	private static Logger logger = LogManager.getLogger(ProcessorFactory.class);

	private Map<String, Class<? extends Processor>> processorClazzes;
	private Set<String> guiSupportedClazzNames;
	private Set<String> densityClazzNames;
	private Set<String> forEachPedClazzNames;

	private static ProcessorFactory instance;

	private ProcessorFactory() {
		processorClazzes = new HashMap<>();
		guiSupportedClazzNames = new HashSet<>();
		densityClazzNames = new HashSet<>();
		forEachPedClazzNames = new HashSet<>();

		// TODO [priority=high] [task=refactoring] maybe do this hole work with reflection, to avoid by hand augmentation
		processorClazzes.put(DensityCountingProcessor.class.getSimpleName(), DensityCountingProcessor.class);
		processorClazzes.put(DensityVoronoiProcessor.class.getSimpleName(), DensityVoronoiProcessor.class);
		processorClazzes.put(DensityVoronoiGeoProcessor.class.getSimpleName(), DensityVoronoiGeoProcessor.class);
		processorClazzes.put(AreaVoronoiProcessor.class.getSimpleName(), AreaVoronoiProcessor.class);
		processorClazzes.put(DensityGaussianProcessor.class.getSimpleName(), DensityGaussianProcessor.class);
		processorClazzes.put(MeanEvacuationTimeProcessor.class.getSimpleName(), MeanEvacuationTimeProcessor.class);
		processorClazzes.put(PedestrianDensityProcessor.class.getSimpleName(), PedestrianDensityProcessor.class);
		processorClazzes.put(PedestrianLastPositionProcessor.class.getSimpleName(),
				PedestrianLastPositionProcessor.class);
		processorClazzes.put(PedestrianPositionProcessor.class.getSimpleName(), PedestrianPositionProcessor.class);
		
		processorClazzes.put(PedestrianVelocityProcessor.class.getSimpleName(), PedestrianVelocityProcessor.class);
		processorClazzes.put(PedestrianOverlapProcessor.class.getSimpleName(), PedestrianOverlapProcessor.class);
		processorClazzes.put(PedestrianFlowProcessor.class.getSimpleName(), PedestrianFlowProcessor.class);
		processorClazzes.put(CombineProcessor.class.getSimpleName(), CombineProcessor.class);
		processorClazzes.put(FloorFieldProcessor.class.getSimpleName(), FloorFieldProcessor.class);
		processorClazzes.put(StrideLengthProcessor.class.getSimpleName(), StrideLengthProcessor.class);
		processorClazzes.put(PedestrianWaitingTimeProcessor.class.getSimpleName(),
				PedestrianWaitingTimeProcessor.class);
		processorClazzes.put(PedestrianCountingAreaProcessor.class.getSimpleName(),
				PedestrianCountingAreaProcessor.class);
		processorClazzes.put(PedestrianWaitingTimeTest.class.getSimpleName(), PedestrianWaitingTimeTest.class);
		processorClazzes.put(PedestrianEvacuationTimeTest.class.getSimpleName(), PedestrianEvacuationTimeTest.class);
		processorClazzes.put(PedestrianDensityTest.class.getSimpleName(), PedestrianDensityTest.class);
		processorClazzes.put(PedestrianTargetProcessor.class.getSimpleName(), PedestrianTargetProcessor.class);
		processorClazzes.put(SnapshotOutputProcessor.class.getSimpleName(), SnapshotOutputProcessor.class);

		guiSupportedClazzNames.add(PedestrianFlowProcessor.class.getSimpleName());
		guiSupportedClazzNames.add(PedestrianDensityProcessor.class.getSimpleName());
		guiSupportedClazzNames.add(PedestrianLastPositionProcessor.class.getSimpleName());
		guiSupportedClazzNames.add(PedestrianPositionProcessor.class.getSimpleName());
		guiSupportedClazzNames.add(PedestrianVelocityProcessor.class.getSimpleName());
		guiSupportedClazzNames.add(PedestrianOverlapProcessor.class.getSimpleName());
		guiSupportedClazzNames.add(CombineProcessor.class.getSimpleName());
		guiSupportedClazzNames.add(MeanEvacuationTimeProcessor.class.getSimpleName());
		guiSupportedClazzNames.add(FloorFieldProcessor.class.getSimpleName());
		guiSupportedClazzNames.add(StrideLengthProcessor.class.getSimpleName());
		guiSupportedClazzNames.add(PedestrianWaitingTimeProcessor.class.getSimpleName());
		guiSupportedClazzNames.add(PedestrianCountingAreaProcessor.class.getSimpleName());
		guiSupportedClazzNames.add(PedestrianWaitingTimeTest.class.getSimpleName());
		guiSupportedClazzNames.add(PedestrianEvacuationTimeTest.class.getSimpleName());
		guiSupportedClazzNames.add(PedestrianDensityTest.class.getSimpleName());
		guiSupportedClazzNames.add(AreaVoronoiProcessor.class.getSimpleName());
		guiSupportedClazzNames.add(PedestrianTargetProcessor.class.getSimpleName());
		guiSupportedClazzNames.add(SnapshotOutputProcessor.class.getSimpleName());


		densityClazzNames.add(DensityGaussianProcessor.class.getSimpleName());
		densityClazzNames.add(DensityVoronoiGeoProcessor.class.getSimpleName());
		densityClazzNames.add(DensityVoronoiProcessor.class.getSimpleName());
		densityClazzNames.add(DensityCountingProcessor.class.getSimpleName());

		forEachPedClazzNames = generateForEachPedestrianPositionProcessorNames();
	}

	public static ProcessorFactory getInstance() {
		if (instance == null) {
			instance = new ProcessorFactory();
		}
		return instance;
	}


	public PedestrianDensityProcessor createPedestrianDensityProcessor(final DensityProcessor densityProcessor) {
		return new PedestrianDensityProcessor(new PedestrianPositionProcessor(), densityProcessor);
	}

	public PedestrianFlowProcessor createPedestrianFlowProcessor(final DensityProcessor densityProcessor) {
		return new PedestrianFlowProcessor(createPedestrianDensityProcessor(densityProcessor),
				new PedestrianVelocityProcessor());
	}

	public CombineProcessor createCombineProcessor(final String[] processorNames) {

		List<ForEachPedestrianPositionProcessor> forEachPedPosProcessorList = new ArrayList<>();

		for (String forEachPedPosName : processorNames) {
			Processor processor = createProcessor(toProcessorType(forEachPedPosName));
			if (processor instanceof ForEachPedestrianPositionProcessor) {
				forEachPedPosProcessorList.add((ForEachPedestrianPositionProcessor) processor);
			} else {
				logger.warn(processor + " is not instanceof " + ForEachPedestrianPositionProcessor.class);
			}
		}
		return new CombineProcessor(forEachPedPosProcessorList);
	}

	public Processor createProcessor(final Class<? extends Processor> clazz) {

		try {
			return clazz.getConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}

		return null;
	}

	public Processor createProcessor(final String simpleClassName) {
		return createProcessor(toProcessorType(simpleClassName));
	}

	public Collection<Class<? extends Processor>> getProcessorClazzes() {
		return processorClazzes.values();
	}

	public Set<String> getSimpleProcessorNames() {
		return processorClazzes.keySet();
	}

	public Class<? extends Processor> toProcessorType(final String simpleProcessorName) {
		return processorClazzes.get(simpleProcessorName);
	}

	public Set<String> getSimpleProcessorNamesForGui() {
		return guiSupportedClazzNames;
	}

	public Set<String> getDensityProcessorNames() {
		return densityClazzNames;
	}

	public Set<String> getForEachPedestrianPositionProcessorNames() {
		return forEachPedClazzNames;
	}

	private Set<String> generateForEachPedestrianPositionProcessorNames() {
		Set<String> names = new HashSet<>();
		for (String name : getSimpleProcessorNames()) {
			if (hasInterface(processorClazzes.get(name).getInterfaces(), ForEachPedestrianPositionProcessor.class)) {
				names.add(name);
			}
		}
		return names;
	}

	private boolean hasInterface(final Class<?>[] clazzes, Class<?> clazz) {
		for (Class<?> clazzEntry : clazzes) {
			if (clazzEntry.equals(clazz)) {
				return true;
			}
		}
		return false;
	}
}
