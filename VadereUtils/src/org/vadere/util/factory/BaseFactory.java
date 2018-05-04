package org.vadere.util.factory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class BaseFactory<T> {

	protected HashMap<String, FactoryObject<T>> supplierMap;

	public BaseFactory(){
		supplierMap = new HashMap<>();
	}

	public void addMember(Class clazz, Supplier<T> supplier){
		addMember(clazz, supplier, clazz.getName());
	}

	public void addMember(Class clazz, Supplier<T> supplier, String label){
		addMember(clazz, supplier,label, "");
	}

	public void addMember(Class clazz, Supplier<T> supplier, String label, String description){
		supplierMap.put(clazz.getCanonicalName(), new FactoryObject<>(clazz, label, description, supplier));
	}

	public Map<String, String> getLabelMap() {
		return supplierMap.entrySet().stream().collect(
				Collectors.toMap(entry -> entry.getValue().getLabel(), entry-> entry.getKey()));
	}

	public T getInstanceOf(String clazz){
		Optional<FactoryObject<T>> opt =Optional.ofNullable(supplierMap.get(clazz));
		return opt.isPresent() ? opt.get().getSupplier().get() : null;
	}

	public T getInstanceOf(Class clazz){
		return getInstanceOf(clazz.getCanonicalName());
	}

	public Supplier<T> getSupplierOf(String clazz){
		Optional<FactoryObject<T>> opt =Optional.ofNullable(supplierMap.get(clazz));
		return opt.isPresent() ? opt.get().getSupplier() : null;
	}

	public Supplier<T>  getSupplierOf(Class clazz){
		return getSupplierOf(clazz.getCanonicalName());
	}
}
