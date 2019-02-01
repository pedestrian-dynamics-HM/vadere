package org.vadere.util.io.filewatcher;

import org.vadere.util.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class LockFileHandler {

	public enum LockInfo {
		Created, Deleted, Both, Error
	}

	private static Logger logger = Logger.getLogger(LockFileHandler.class);

	private WatchDir dirWatcher = null;
	private File lockFile = null;
	private File lockDir = null;
	private static String notADirectiory = "Lock path is not a directory";
	private static String lockFileName = "lock.lck";
	private FileChannel channel = null;
	private FileLock lock = null;

	public LockFileHandler(String lockFilePath) throws Exception {

		lockDir = new File(lockFilePath);

		if (!lockDir.isDirectory()) {

			throw new Exception(notADirectiory);
		}

		lockFile = new File(lockFilePath + File.separator + lockFileName);
		lockFile.createNewFile();
		dirWatcher = new WatchDir(lockDir.toPath(), false);
	}

	public void waitForLockDelete() throws IOException {
		while (true) {
			LockInfo info = dirWatcher.processEvents();


			if (info == LockInfo.Deleted || info == LockInfo.Both) {
				break;
			}
		}
	}

	public void waitForLockCreate() throws IOException {
		while (true) {

			LockInfo info = dirWatcher.processEvents();

			if (info == LockInfo.Created || info == LockInfo.Both) {
				break;
			}
		}
	}

	public void writeLock() throws IOException {

		lockFile.createNewFile();
	}

	public void deleteLock() throws IOException {
		lockFile.delete();
	}
}
