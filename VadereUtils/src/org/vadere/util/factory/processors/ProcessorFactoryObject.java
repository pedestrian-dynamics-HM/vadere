package org.vadere.util.factory.processors;

import org.vadere.util.factory.FactoryObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Supplier;

public class ProcessorFactoryObject<T> extends FactoryObject<T> {

	private final String label;
	private final String description;
	private final ArrayList<ProcessorFlag> processorFlags;

	public ProcessorFactoryObject(Supplier<T> supplier, String label, String description, Class clazz, String... flags){
		super(clazz, supplier);
		this.label = label;
		this.description = description;
		this.processorFlags = ProcessorFlag.getFlags(flags);
	}

	public String getLabel() {
		return label;
	}

	//todo: this should be locale with Messages.
	public String getDescription() {
		return description;
	}

	public ArrayList<ProcessorFlag> getProcessorFlags(){
		return processorFlags;
	}
}
