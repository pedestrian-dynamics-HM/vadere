package org.vadere.util.factory.outputfiles;

import org.vadere.util.factory.BaseFactory;

import java.util.HashMap;
import java.util.function.Supplier;

/**
 * Overwrite the standard {@link org.vadere.util.factory.FactoryObject} to save
 * the additional mapping information between DataKey and OutputFileType
 * @param <T>
 */
public class OutputFileBaseFactory<T> extends BaseFactory<T, OutputFileFactoryObject<T>> {

	public void addMember(Class clazz, Supplier supplier, String label, String desc, String keyName) {
		supplierMap.put(clazz.getCanonicalName(), new OutputFileFactoryObject<T>(clazz, supplier, label, desc, keyName));
	}

	public HashMap<String, String> getDataKeyOutputFileMap() {
		HashMap<String, String> out = new HashMap<>();
		supplierMap.forEach((s, factoryObject) -> out.put(factoryObject.getKeyName(), s));
		return out;
	}
}
