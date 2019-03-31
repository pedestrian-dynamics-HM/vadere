package org.vadere.util.factory.processors;

import org.vadere.util.factory.BaseFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProcessorBaseFactory<T> extends BaseFactory<T, ProcessorFactoryObject<T>> {

	public void addMember(Supplier supplier, String label, String desc, Class clazz, String... flags) {
		supplierMap.put(clazz.getCanonicalName(), new ProcessorFactoryObject<>(supplier, label, desc, clazz, flags));
	}

	public HashMap<String, String> getLabelMap() {
		HashMap<String, String> out = new HashMap<>();
		supplierMap.forEach((s, factoryObject) -> out.put(factoryObject.getLabel(), s));
		return out;
	}

	public List<String> getProcessors() {
		return supplierMap.keySet().stream().collect(Collectors.toList());
	}

	public ArrayList<Flag> getFlag(String key){
		ProcessorFactoryObject<T> processorFactoryObject = supplierMap.get(key);
		return processorFactoryObject.getFlags();
	}

	public boolean containsFlag(Class processor, String flagStr){
		ProcessorFactoryObject<T> processorFactoryObject = supplierMap.get(processor.getCanonicalName());
		if (processorFactoryObject == null)
			return false;

		Flag flag = new Flag(flagStr);
		return processorFactoryObject.getFlags().stream().anyMatch(f -> f.equals(flag));
	}
}
