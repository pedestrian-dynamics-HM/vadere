package org.vadere.util.factory.processors;

import org.vadere.util.factory.FactoryObject;

import java.util.ArrayList;
import java.util.function.Supplier;

public class ProcessorFactoryObject<T> extends FactoryObject<T> {

	private final String label;
	private final String description;
	private final ArrayList<Flag> flags;

	public ProcessorFactoryObject(Supplier<T> supplier, String label, String description, Class clazz, String... flags){
		super(clazz, supplier);
		this.label = label;
		this.description = description;
		this.flags = Flag.getFlags(flags);
	}

	public String getLabel() {
		return label;
	}

	//todo: this should be locale with Messages.
	public String getDescription() {
		return description;
	}

	public ArrayList<Flag> getFlags(){
		return flags;
	}
}
