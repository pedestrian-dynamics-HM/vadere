package org.vadere.util.lwjgl;

import org.junit.Test;
import org.vadere.util.logging.Logger;
import org.vadere.util.opencl.CLUtils;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Benedikt Zoennchen
 */
public class TestLWJGL {
	private static Logger logger = Logger.getLogger(TestLWJGL.class);

	// ignore cause our worker for CI does not support OpenCL
	@Test
	public void testNativeLinking() {
		try {
			CLUtils.isOpenCLSupported();
		} catch (UnsatisfiedLinkError linkError) {
			fail("could not test for OpenCL support cause of missing native lib support.");
		}
	}

	// ignore cause our worker for CI does not support OpenCL
	@Test
	public void testOpenCLSuppert() {
		assertTrue("OpenCL is not supported on your machine", CLUtils.isOpenCLSupported());
	}
}
