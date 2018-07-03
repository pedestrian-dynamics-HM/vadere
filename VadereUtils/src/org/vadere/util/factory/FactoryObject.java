package org.vadere.util.factory;

import java.util.function.Supplier;

/**
 * Base factory object which provides {@link Supplier} for Type T
 * @param <T>
 */
public class FactoryObject<T> {

	protected final Class<? extends T> clazz;
	protected Supplier<T> supplier;

	public FactoryObject(Class<? extends T> clazz, Supplier<T> supplier) {
		this.supplier = supplier;
		this.clazz = clazz;
	}

	public Supplier<T> getSupplier() {
		return supplier;
	}

	public Class<? extends T> getClazz() {
		return clazz;
	}

	public T getInstance(){
		return supplier.get();
	}


}
