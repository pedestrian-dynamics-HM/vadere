package org.vadere.util.opencl;

import org.apache.log4j.Logger;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CLContextCallback;
import org.lwjgl.opencl.CLProgramCallback;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memUTF8;

public interface CLBase {

    public void init() throws OpenCLException;

    public void initCallbacks();

    public abstract void initCL() throws OpenCLException;

    public abstract void buildProgram() throws OpenCLException;

    public abstract void clearMemory() throws OpenCLException;

    public void clearCL() throws OpenCLException;

}
