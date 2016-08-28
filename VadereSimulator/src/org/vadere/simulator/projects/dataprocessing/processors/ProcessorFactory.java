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
		processorClazzes.put(PedestrianPositionProcessor.class.getSimpleName(), PedestrianPositionProcessor.class);
		processorClazzes.put(SnapshotOutputProcessor.class.getSimpleName(), SnapshotOutputProcessor.class);

		guiSupportedClazzNames.add(PedestrianPositionProcessor.class.getSimpleName());
		guiSupportedClazzNames.add(SnapshotOutputProcessor.class.getSimpleName());

		forEachPedClazzNames = generateForEachPedestrianPositionProcessorNames();
	}

	public static ProcessorFactory getInstance() {
		if (instance == null) {
			instance = new ProcessorFactory();
		}
		return instance;
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
