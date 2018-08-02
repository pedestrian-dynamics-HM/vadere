package org.vadere.util.factory;

import java.util.HashMap;
import java.util.function.Supplier;

/**
 * The BaseFactory uses a HashMap to manage the different instances needed to be created.
 * The map allows creation of new instances via the FullyQualifiedName(FQN) as a String. This is
 * the main use case because of the serialization ot attributes to JSON.
 *
 * Within the Map the FQN as string maps to a FactoryObject congaing at least the Class Object and
 * a Supplier for the FQN. The Suppliers are generated via annotation processing an point to normal
 * getters using the >new< statement with the default constructor.
 *
 * In the FactoryObject additional information can be saved such as labels or descriptions. Access
 * to these additional information must be provided by the implementation of the {@link BaseFactory}
 * class.
 *
 * @param <T> super type of Object used for Supplier
 * @param <O> FactoryObject based on type T
 */
public abstract class BaseFactory<T, O extends FactoryObject<T>> {

	protected HashMap<String, O> supplierMap;

	public BaseFactory() {
		supplierMap = new HashMap<>();
	}


	public T getInstanceOf(String clazz) throws ClassNotFoundException {
		if (supplierMap.containsKey(clazz))
			return supplierMap.get(clazz).getSupplier().get();

		throw new ClassNotFoundException(clazz + " is not a class or is not contained within Factory");
	}

	public T getInstanceOf(Class clazz) throws ClassNotFoundException {
		return getInstanceOf(clazz.getCanonicalName());
	}

	public Supplier<T> getSupplierOf(String clazz) throws ClassNotFoundException {
		if (supplierMap.containsKey(clazz))
			return supplierMap.get(clazz).getSupplier();

		throw new ClassNotFoundException(clazz + " is not a class or is not contained within Factory");
	}

	public Supplier<T> getSupplierOf(Class clazz) throws ClassNotFoundException {
		return getSupplierOf(clazz.getCanonicalName());
	}
}
