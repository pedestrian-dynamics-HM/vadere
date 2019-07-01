package org.vadere.util.io;

import java.io.DataInputStream;
import java.io.File;
import java.io.InputStream;

public interface DataReader<T> {

	T fromCsv(File file) throws Exception;
	T fromCsv(InputStream inputStream) throws Exception;
	T fromBinary(File file) throws Exception;
	T fromBinary(DataInputStream stream) throws Exception;

}
