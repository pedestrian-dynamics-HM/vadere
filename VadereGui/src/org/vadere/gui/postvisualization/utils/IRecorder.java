package org.vadere.gui.postvisualization.utils;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Observer;

public interface IRecorder extends Observer {
	void startRecording() throws IOException;

	void startRecording(final Rectangle2D.Double imageSize) throws IOException;

	void stopRecording() throws IOException;
}
