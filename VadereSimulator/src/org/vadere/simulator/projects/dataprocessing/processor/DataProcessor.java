package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.SimulationResult;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.DataKey;
import org.vadere.state.attributes.processor.AttributesProcessor;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Base class for data processors.
 *
 * This class contains all common functionality for all data processors.
 * It provides access to the internal data map for saving concrete data of type <tt>V</tt> with key type <tt>K</tt>.
 *
 * The methods <tt>preLoop</tt> and <tt>postLoop</tt> are called at corresponding points in time related to the simulation loop.
 *
 * The method <tt>doUpdate</tt> gets called after every simulation step with the current <tt>SimulationState</tt>.
 * Here, one gets the opportunity to compute a new value or to update the state for a computation in <tt>postLoop</tt>.
 * The computed value can be stored afterwards in the data by using the <tt>putValue</tt> method.
 *
 * To get specific attributes defined in JSON or access to the <tt>MainModel</tt>, one has to use the <tt>init</tt> method which
 * gives access to all significant things via the argument <tt>manager</tt> of type <tt>ProcessorManager</tt>.
 *
 * @param <K> key type
 * @param <V> value type
 *
 * @author Mario Teixeira Parente
 *
 */

public abstract class DataProcessor<K extends DataKey<K>, V> {
	private int id;
	private AttributesProcessor attributes;

	private String[] headers;
	private Map<K, V> data;

	private int lastStep;
	private K lastKey;

	private boolean stopSimBeforeSimFinish;

	protected DataProcessor() {
		this(new String[] { });
	}

	protected DataProcessor(final String... headers) {
		this.headers = headers;
		this.data = new TreeMap<>(); // TreeMap to avoid sorting data later

		this.lastStep = 0;
		this.stopSimBeforeSimFinish = false;
	}


	public Map<K, V> getData() {
		return this.data;
	}

	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public AttributesProcessor getAttributes() {
		return attributes;
	}

	public void setAttributes(AttributesProcessor attributes) {
		this.attributes = attributes;
	}

	public String[] getHeaders() {
		return this.headers;
	}

	public void setHeaders(final String... headers) {
		this.headers = headers;
	}

	public Set<K> getKeys() {
		return this.getData().keySet();
	}

	public Collection<V> getValues() {
		return this.getData().values();
	}

	public boolean hasValue(final K key) {
		return this.data.containsKey(key);
	}

	public V getValue(final K key) {
		return data.get(key);
	}

	public int getLastStep()
	{
		return this.lastStep;
	}

	public K getLastKey() {
		return lastKey;
	}

	protected void putValue(final K key, final V value) {
		this.lastKey = key;
		this.data.put(key, value);
	}

	protected void removeKey(final K key) {
		this.data.remove(key);
	}

	public void preLoop(final SimulationState state) { data.clear(); }

	protected abstract void doUpdate(final SimulationState state);

	public final void update(final SimulationState state) {
		int step = state.getStep();

		if (this.lastStep < step)  {
			this.doUpdate(state);
			this.lastStep = step;
		}
		// stop simulation if a criteria defined in your data processor is fulfilled
		state.setSimStop(this.stopSimBeforeSimFinish);

	}

	public Optional<Model> getSubModel(SimulationState state, Class clazz){
		Optional<MainModel> mainModel = state.getMainModel();
		if(mainModel.isPresent()){
			Optional<Model> model = mainModel.get().getSubmodels()
					.stream().filter(clazz::isInstance).findAny();
			return model;
		} else {
			return Optional.empty();
		}
	}

	public void postLoop(final SimulationState state) { }

	public void postLoopAddResultInfo(final SimulationState state, SimulationResult result){

	}

	public void init(final ProcessorManager manager){
		this.data.clear();
		this.lastStep = 0;
	}

	public String[] toStrings(final K key) {
		return new String[] { this.hasValue(key) ? this.getValue(key).toString() : "NA" };
	}

	public String getSimpleProcessorTypeName() {
		return getClass().getSimpleName();
	}

	public void setStopSimBeforeSimFinish(boolean stopSimBeforeSimFinish) {
		this.stopSimBeforeSimFinish = stopSimBeforeSimFinish;
	}

	@Override
	public String toString() {
		return id + ": " + getSimpleProcessorTypeName();
	}

	public void sealAttributes() {
		if (attributes != null) // some processors might not have attributes
			attributes.seal();
	}

}
