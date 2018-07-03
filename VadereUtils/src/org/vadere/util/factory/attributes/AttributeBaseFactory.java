package org.vadere.util.factory.attributes;

import org.vadere.util.factory.BaseFactory;
import org.vadere.util.factory.FactoryObject;

import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class AttributeBaseFactory<T> extends BaseFactory<T, FactoryObject<T>> {

	public void addMember(Class clazz, Supplier supplier) {
		supplierMap.put(clazz.getCanonicalName(), new FactoryObject<>(clazz, supplier));
	}

	public Stream<String> sortedAttributeStream() {
		return supplierMap.keySet().stream().sorted();
	}
}
