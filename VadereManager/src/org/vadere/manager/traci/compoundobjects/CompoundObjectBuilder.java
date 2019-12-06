package org.vadere.manager.traci.compoundobjects;

import org.vadere.manager.TraCIException;
import org.vadere.manager.traci.TraCIDataType;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * Builder class to create any combination atomar data types combined in a @{@link CompoundObject}.
 * See static methods on how the builder is used. Ensure that the number of {@link #add(TraCIDataType)}
 * calls is equal to the number of arguments to the {@link #build(Object...)}.
 */
public class CompoundObjectBuilder {

	private LinkedList<TraCIDataType> types;

	public CompoundObjectBuilder(){
		this.types = new LinkedList<>();
	}

	public CompoundObjectBuilder rest(){
		types.clear();
		return this;
	}

	public CompoundObjectBuilder add(TraCIDataType type){
		types.add(type);
		return this;
	}

	public CompoundObject build(Object... data){
		CompoundObject obj = new CompoundObject(data.length);
		if (types.size() == data.length){
			int idx = 0;
			for (TraCIDataType type : types) {
				obj.add(type, data[idx]);
				idx++;
			}

		} else {
			throw  new TraCIException("CompoundObjectBuilder error. Number of Types does not match received number of data items");
		}

		return obj;
	}


	static public CompoundObjectBuilder builder(){
		return new CompoundObjectBuilder();
	}

	static public CompoundObject createPerson(String id, String x, String y, String... targets){

		VPoint p = new VPoint(Double.parseDouble(x), Double.parseDouble(y));
		ArrayList<String> targetList = new ArrayList<>(Arrays.asList(targets));

		return CompoundObjectBuilder.builder()
				.rest()
				.add(TraCIDataType.STRING)
				.add(TraCIDataType.POS_2D)
				.add(TraCIDataType.STRING_LIST)
				.build(id, p, targetList);
	}

	static public CompoundObject createTargetChanger(String id, ArrayList<String> points, double reachDist, int nextTargetIsPedestrian, String nextTarget, double prob) {
		return CompoundObjectBuilder.builder()
				.rest()
				.add(TraCIDataType.STRING)
				.add(TraCIDataType.STRING_LIST)
				.add(TraCIDataType.DOUBLE)
				.add(TraCIDataType.INTEGER)
				.add(TraCIDataType.STRING)
				.add(TraCIDataType.DOUBLE)
				.build(id, points, reachDist, nextTargetIsPedestrian, nextTarget, prob);
	}

	static public CompoundObject createIdPosData(String id, String x, String y){

		VPoint p = new VPoint(Double.parseDouble(x), Double.parseDouble(y));

		return CompoundObjectBuilder.builder()
				.rest()
				.add(TraCIDataType.STRING)
				.add(TraCIDataType.POS_2D)
				.build(id, p);
	}

	static public CompoundObject createWaitingArea(String elementID, double startTime, double endTime, int repeat, double waitTimeBetweenRepetition, double time, ArrayList<String> points){
		return CompoundObjectBuilder.builder()
				.rest()
				.add(TraCIDataType.STRING) // dummy, because setters always need id
				.add(TraCIDataType.DOUBLE)
				.add(TraCIDataType.DOUBLE)
				.add(TraCIDataType.INTEGER)
				.add(TraCIDataType.DOUBLE)
				.add(TraCIDataType.DOUBLE)
				.add(TraCIDataType.STRING_LIST)
				.build(elementID, startTime, endTime, repeat, waitTimeBetweenRepetition, time, points);
	}


}
