package org.vadere.util.opencl;

import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLDevice;
import com.nativelibs4java.opencl.JavaCL;

import org.junit.Test;

/**
 * @author Benedikt Zoennchen
 */
public class TestJavaCL {

	@Test
	public void testContext() {
		CLDevice devices = JavaCL.getBestDevice();
		/*CLContext context = JavaCL.createContext(null, device);
		for (int i = 0; i < devices.length; i++) {
			System.err.println(i+": "+devices[i]);
		}*/
		System.err.println("Now GC'ing");
		System.gc(); // crash here
		System.err.println("GC'ed");
	}

}
