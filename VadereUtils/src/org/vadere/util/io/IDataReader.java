package org.vadere.util.io;

import java.io.DataInputStream;
import java.io.File;
import java.io.InputStream;

public interface IDataReader<T> {

	T fromTextFile(File file) throws Exception;
	T fromTextFile(InputStream inputStream) throws Exception;
	T fromBinary(File file) throws Exception;
	T fromBinary(DataInputStream stream) throws Exception;

}
