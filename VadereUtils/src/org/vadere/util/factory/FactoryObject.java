package org.vadere.util.factory;

import java.util.function.Supplier;

public class FactoryObject<T> {

	private final Class clazz;
	private final String  label;
	private final String description;
	private Supplier<T> supplier;

	public FactoryObject(Class clazz, String label, String description, Supplier<T> supplier){
		this.clazz = clazz;
		this.label = label;
		this.description = description;
		this.supplier = supplier;
	}

	public String getLabel() {
		return label;
	}

	//todo: this should be locale with Messages.
	public String getDescription() {
		return description;
	}

	public Supplier<T> getSupplier() {
		return supplier;
	}
}
