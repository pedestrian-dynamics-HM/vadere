package org.vadere.util.factory;

import java.util.HashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class OutputFileBaseFactory<T> extends BaseFactory<T> {

	protected HashMap<String, String> dataKeyMap;

	public OutputFileBaseFactory(){
		dataKeyMap = new HashMap<>();
	}

	protected void addExistingKey(String keyName, String outputFileName){
		dataKeyMap.put(keyName, outputFileName);
	}

	public HashMap<String, String> getDataKeyOutputFileMap() {
		return dataKeyMap;
	}
}
