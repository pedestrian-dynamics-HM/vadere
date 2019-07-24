package org.vadere.util.io;

import java.io.File;

public interface IDataWriter {

	void toTextFile(File file) throws Exception;
	void toBinary(File file) throws Exception;

}
