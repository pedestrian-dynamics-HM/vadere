package org.vadere.manager.traci.compoundobjects;

import org.vadere.manager.TraCIException;

public abstract class GenericCompoundObject {


	GenericCompoundObject(CompoundObject o, int size){
		assertElementCount(o, size);
		init(o);
	}

	protected void assertElementCount(final CompoundObject o, int size){
		if (o.size() == size)
			throw new TraCIException("Cannot create %s from CompoundObject containing %s", getClass().getName(), o.types());
	}

	abstract protected void init(CompoundObject o);
}
