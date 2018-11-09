package org.vadere.simulator.projects.migration.jolttranformation;


import org.vadere.util.factory.BaseFactory;
import org.vadere.util.factory.FactoryObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.function.Supplier;

/**
 * This is the Base version for the JoltTransformationFactory which automatically build by
 * a Annotation Processor at build time. Do not directly use this class
 */
public class JoltTransformationBaseFactory extends BaseFactory<JoltTransformation, FactoryObject<JoltTransformation>> {

	HashMap<String, JoltTransformation> transformationMap;

	public JoltTransformationBaseFactory(){
		this.transformationMap = new LinkedHashMap<>();
	}

	public void addMember(String versionLabel, Class<?> clazz, Supplier supplier){
		supplierMap.put(versionLabel, new FactoryObject<>(clazz, supplier));
	}


	@Override
	public JoltTransformation getInstanceOf(String key) throws ClassNotFoundException {
		if (transformationMap.containsKey(key)){
			return transformationMap.get(key);
		} else {
			if (supplierMap.containsKey(key)){
				JoltTransformation tmp = supplierMap.get(key).getSupplier().get();
				transformationMap.put(key, tmp);
				return tmp;
			}
			throw new ClassNotFoundException("No class associated With Key: " + key + " in this Factory");
		}
	}
}
