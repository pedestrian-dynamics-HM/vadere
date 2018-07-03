package org.vadere.util.factory.processors;

import org.vadere.util.factory.FactoryObject;

import java.util.function.Supplier;

public class ProcessorFactoryObject<T> extends FactoryObject<T> {

	private final String label;
	private final String description;

	public ProcessorFactoryObject(Class clazz, Supplier<T> supplier, String label, String description){
		super(clazz, supplier);
		this.label = label;
		this.description = description;
	}

	public String getLabel() {
		return label;
	}

	//todo: this should be locale with Messages.
	public String getDescription() {
		return description;
	}

}
