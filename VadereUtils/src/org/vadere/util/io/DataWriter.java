package org.vadere.util.io;

import java.io.File;

public interface DataWriter {

	void toCsv(File file) throws Exception;
	void toBinary(File file) throws Exception;

}
