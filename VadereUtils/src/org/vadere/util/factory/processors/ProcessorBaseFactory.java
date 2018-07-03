package org.vadere.util.factory.processors;

import org.vadere.util.factory.BaseFactory;

import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ProcessorBaseFactory<T> extends BaseFactory<T, ProcessorFactoryObject<T>> {

	public void addMember(Class clazz, Supplier supplier, String label, String desc) {
		supplierMap.put(clazz.getCanonicalName(), new ProcessorFactoryObject<>(clazz, supplier, label, desc));
	}

	public HashMap<String, String> getLabelMap() {
		HashMap<String, String> out = new HashMap<>();
		supplierMap.forEach((s, factoryObject) -> out.put(factoryObject.getLabel(), s));
		return out;
	}

	public List<String> getProcessors() {
		return supplierMap.keySet().stream().collect(Collectors.toList());
	}
}
