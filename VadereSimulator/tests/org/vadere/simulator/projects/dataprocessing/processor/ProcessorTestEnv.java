package org.vadere.simulator.projects.dataprocessing.processor;

import org.mockito.Mockito;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.DataKey;
import org.vadere.simulator.projects.dataprocessing.outputfile.OutputFile;
import org.vadere.simulator.projects.dataprocessing.outputfile.OutputFileFactory;
import org.vadere.simulator.projects.dataprocessing.writer.VadereStringWriter;
import org.vadere.simulator.projects.dataprocessing.writer.VadereWriterFactory;
import org.vadere.simulator.utils.reflection.ReflectionHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;

/**
 * A {@link ProcessorTestEnv} encapsulates needed  dependencies to test a {@link DataProcessor}. If
 * possible dependencies are mocked via {@link org.mockito.Mockito}.
 *
 * @author Stefan Schuhbäck
 */
public abstract class ProcessorTestEnv<K extends DataKey<K>, V> {

	/**
	 * processor under test
	 */
	DataProcessor<?, ?> testedProcessor;
	/**
	 * Ids of {@link DataProcessor}s
	 */
	int nextProcessorId;
	/**
	 * Corresponding {@link OutputFile} needed by {@link #testedProcessor}
	 */
	OutputFile outputFile;
	Map<K, V> expectedOutput;
	/**
	 * Factories
	 */
	org.vadere.simulator.projects.dataprocessing.processor.DataProcessorFactory processorFactory;
	OutputFileFactory outputFileFactory;
	/**
	 * Needed for DataProcessor doUpdate call. (mocked)
	 */
	ProcessorManager manager;
	/**
	 * List of {@link SimulationState}s used for test. (mocked)
	 */
	private List<SimulationStateMock> states;
	/**
	 * If {@link #testedProcessor} has dependencies to other {@link DataProcessor}s
	 */
	private List<ProcessorTestEnv> requiredProcessors;
	private String delimiter;

	/**
	 * Needed to access class of K in a type safe manner.
	 */
	private final Class<K> dataKeyType;
	/**
	 * Needed to access class of testedProcessor in a type safe manner.
	 */
	private final Class<? extends DataProcessor> testedProcessorClass;

	ProcessorTestEnv(Class<? extends DataProcessor> testedProcessorClass, Class<K> dataKeyType) {
		this(testedProcessorClass, dataKeyType, 1);
	}

	ProcessorTestEnv(Class<? extends DataProcessor> testedProcessorClass, Class<K> dataKeyType, int nextProcessorId) {
		this.testedProcessorClass = testedProcessorClass;
		this.dataKeyType = dataKeyType;
		this.manager = mock(ProcessorManager.class, Mockito.RETURNS_DEEP_STUBS);
		this.states = new ArrayList<>();
		this.nextProcessorId = nextProcessorId;
		this.expectedOutput = new HashMap<>();
		this.delimiter = " ";
		this.testedProcessor = null;
		this.outputFile = null;
		this.requiredProcessors = new LinkedList<>();
		this.processorFactory = org.vadere.simulator.projects.dataprocessing.processor.DataProcessorFactory.instance();
		this.outputFileFactory = OutputFileFactory.instance();

		// create processor under test an set processor id.
		initializeTestedProcessor();
		// add all dependencies such as other processors or measurement areas.
		initializeDependencies();
		// change writer in output file to use a StringWriter instead of a file writer.
		initializeOutputFile();

	}


	void initializeTestedProcessor() {
		try {
			testedProcessor = processorFactory.createDataProcessor(testedProcessorClass);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		testedProcessor.setId(nextProcessorId());
	}

	void initializeDependencies() {
		// default no dependencies. Override to add new ones.
	}

	void initializeOutputFile() {
		try {
			outputFile = outputFileFactory.createDefaultOutputfileByDataKey(
					getDataKeyType(),
					testedProcessor.getId()
			);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		outputFile.setVadereWriterFactory(VadereWriterFactory.getStringWriterFactory());
	}

	@SuppressWarnings("unchecked")
	int addDependentProcessor(Function<Integer, ProcessorTestEnv<?, ?>> envProducer) {
		int id = nextProcessorId();
		ProcessorTestEnv<?, ?> env = envProducer.apply(id);
		DataProcessor processor = env.getTestedProcessor();
		Mockito.when(manager.getProcessor(id)).thenReturn(processor);
		addRequiredProcessors(env);

		// if dependency also has dependencies ensure that no id is used twice.
		if (env.nextProcessorId > this.nextProcessorId)
			this.nextProcessorId = env.nextProcessorId + 1;

		return id;
	}

	public Class<K> getDataKeyType() {
		return this.dataKeyType;
	}

	DataProcessor<?, ?> getProcessorById(int id) {
		for (ProcessorTestEnv env : requiredProcessors) {
			if (env.getTestedProcessor().getId() == id)
				return env.getTestedProcessor();
		}
		return null;
	}

	/**
	 * Initialize {@link DataProcessor}, {@link OutputFile} and initialize all requiredProcessors if
	 * needed.
	 */
	@SuppressWarnings("unchecked")
	void init() {
		delimiter = outputFile.getSeparator();
		outputFile.init(getProcessorMap());
		testedProcessor.init(manager);
		requiredProcessors.forEach(ProcessorTestEnv::init);
	}

	/**
	 * Overwrite to add {@link SimulationStateMock}s needed for test.
	 */
	public abstract void loadDefaultSimulationStateMocks();

	/**
	 * Add Mocked SimulationsState to current Processor under test and all its required {@link
	 * DataProcessor}
	 */
	public void addSimState(SimulationStateMock mock) {
		states.add(mock);
		requiredProcessors.forEach(e -> e.addSimState(mock));
	}

	List<SimulationState> getSimStates() {
		return states.stream().map(SimulationStateMock::get).collect(Collectors.toList());
	}

	void addToExpectedOutput(K dataKey, V value) {
		expectedOutput.put(dataKey, value);
	}

	Map<K, V> getExpectedOutput() {
		return expectedOutput;
	}

	abstract List<String> getExpectedOutputAsList();

	ProcessorManager getManager() {
		return manager;
	}

	DataProcessor<?, ?> getTestedProcessor() {
		return testedProcessor;
	}

	String getDelimiter() {
		return delimiter;
	}

	void removeState(int index) {
		states.remove(index);
		expectedOutput.remove(index);
		requiredProcessors.forEach(env -> env.removeState(index));
	}

	void clearStates() {
		states.clear();
		expectedOutput.clear();
		requiredProcessors.forEach(ProcessorTestEnv::clearStates);
	}

	void addRequiredProcessors(ProcessorTestEnv env) {
		requiredProcessors.add(env);
	}

	/**
	 * Return the ProcessorMap for the current Test.
	 */
	private Map<Integer, DataProcessor<?, ?>> getProcessorMap() {
		Map<Integer, DataProcessor<?, ?>> processorMap = new LinkedHashMap<>();
		processorMap.put(testedProcessor.getId(), testedProcessor);
		if (requiredProcessors != null && requiredProcessors.size() > 0)
			requiredProcessors.forEach((e) ->
					processorMap.put(e.getTestedProcessor().getId(), e.getTestedProcessor()));

		return processorMap;
	}

	OutputFile getOutputFile() {
		return outputFile;
	}

	List<String> getOutput() throws NoSuchFieldException, IllegalAccessException {
		return getOutput(1);
	}

	List<String> getOutput(int fromLine) throws NoSuchFieldException, IllegalAccessException {
		ReflectionHelper r = ReflectionHelper.create(outputFile);
		VadereStringWriter writer = r.valOfField("writer");
		return writer.getOutput().subList(fromLine, writer.getOutput().size());
	}

	String getHeader() throws NoSuchFieldException, IllegalAccessException {
		ReflectionHelper r = ReflectionHelper.create(outputFile);
		VadereStringWriter writer = r.valOfField("writer");
		return writer.getOutput().get(0);
	}

	int nextProcessorId() {
		return nextProcessorId++;
	}
}
