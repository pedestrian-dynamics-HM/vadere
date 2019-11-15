package org.vadere.manager.traci.compoundobjects;

import org.vadere.manager.traci.TraCIDataType;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.stream.Collectors;

public class PersonCreateData extends GenericCompoundObject{

	private String id;
	private VPoint pos;
	private ArrayList<String> targets;

	public PersonCreateData(CompoundObject o){
		super(o, 3);
	}

	@Override
	protected void init(CompoundObject o) {
		id = (String)o.getData(0, TraCIDataType.STRING);
		pos = (VPoint)o.getData(1, TraCIDataType.POS_2D);
		targets = (ArrayList<String>)o.getData(2, TraCIDataType.STRING_LIST);
	}

	public String getId() {
		return id;
	}

	public VPoint getPos() {
		return pos;
	}

	public ArrayList<String> getTargets() {
		return targets;
	}

	public LinkedList<Integer> getTargetsAsInt(){
		return targets.stream().mapToInt(Integer::parseInt).boxed().collect(Collectors.toCollection(LinkedList::new));
	}

}
