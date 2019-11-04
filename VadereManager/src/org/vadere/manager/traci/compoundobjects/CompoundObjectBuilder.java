package org.vadere.manager.traci.compoundobjects;

import org.vadere.manager.TraCIException;
import org.vadere.manager.traci.TraCIDataType;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.LinkedList;

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

	static public CompoundObject createPerson(String x, String y, String... targets){

		VPoint p = new VPoint(Double.parseDouble(x), Double.parseDouble(y));

		return CompoundObjectBuilder.builder()
				.rest()
				.add(TraCIDataType.POS_2D)
				.add(TraCIDataType.STRING_LIST)
				.build(p, targets);
	}


	static public CompoundObject createIdPosData(String id, String x, String y){

		VPoint p = new VPoint(Double.parseDouble(x), Double.parseDouble(y));

		return CompoundObjectBuilder.builder()
				.rest()
				.add(TraCIDataType.STRING)
				.add(TraCIDataType.POS_2D)
				.build(id, p);
	}


}
