package org.vadere.simulator.context;

import java.util.HashMap;

public class Context {

	private HashMap<String, Object> ctxValueMap;


	public Context() {
		ctxValueMap = new HashMap<>();
	}

	public void put(String key, Object data){
		ctxValueMap.put(key, data);
	}

	public Object get(String key){
		return ctxValueMap.getOrDefault(key, null);
	}

	public Object getOrDefault(String key, Object o){
		return ctxValueMap.getOrDefault(key, o);
	}

	public int getInt(String key){
		if (!ctxValueMap.containsKey(key))
			throw new ContextException("no element found in context object for key: " + key);
		try {
			return (int) ctxValueMap.get(key);
		} catch (ClassCastException e){
			throw new ContextException("data type of key " + key + " is not of type integer", e);
		}
	}

	public double getDouble(String key){
		if (!ctxValueMap.containsKey(key))
			throw new ContextException("no element found in context object for key: " + key);
		try {
			return (double) ctxValueMap.get(key);
		} catch (ClassCastException e){
			throw new ContextException("data type of key " + key + " is not of type double", e);
		}
	}

	public String getString(String key){
		if (!ctxValueMap.containsKey(key))
			throw new ContextException("no element found in context object for key: " + key);
		try {
			return  (String) ctxValueMap.get(key);
		} catch (ClassCastException e){
			throw new ContextException("data type of key " + key + " is not of type String", e);
		}
	}

	public boolean containsKey(String key){
		return ctxValueMap.containsKey(key);
	}

}
