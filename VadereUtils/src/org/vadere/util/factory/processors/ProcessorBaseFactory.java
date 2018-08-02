package org.vadere.util.factory.processors;

import org.vadere.util.factory.BaseFactory;

import java.util.HashMap;
import java.util.function.Supplier;

public class ProcessorBaseFactory<T> extends BaseFactory<T, ProcessorFactoryObject<T>> {

	public void addMember(Class clazz, Supplier supplier, String label, String desc) {
		supplierMap.put(clazz.getCanonicalName(), new ProcessorFactoryObject<>(clazz, supplier, label, desc));
	}

	public HashMap<String, String> getLabelMap() {
		HashMap<String, String> out = new HashMap<>();
		supplierMap.forEach((s, factoryObject) -> out.put(factoryObject.getLabel(), s));
		return out;
	}
}
