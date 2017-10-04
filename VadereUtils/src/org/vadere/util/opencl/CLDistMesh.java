package org.vadere.util.opencl;

import org.apache.commons.math.util.MathUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.*;
import org.lwjgl.system.MemoryStack;
import org.vadere.util.geometry.mesh.gen.*;
import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.opencl.examples.IOUtil;
import org.vadere.util.opencl.examples.InfoUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;

import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.opencl.CL10.clEnqueueNDRangeKernel;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memUTF8;

/**
 * @author Benedikt Zoennchen
 *
 * DistMesh GPU implementation.
 */
public class CLDistMesh {

    private static Logger log = LogManager.getLogger(CLDistMesh.class);

    // CL ids
    private MemoryStack stack;
    private long clPlatform;
    private long clDevice;
    private long clContext;
    private long clQueue;
    private long clProgram;

    // CL kernel ids
    private long clKernelForces;
    private long clKernelMove;
    private long clKernelLengths;
    private long clKernelPartialSF;
    private long clKernelCompleteSF;

    // error code buffer
    private IntBuffer errcode_ret;

    // CL callbacks
    private CLContextCallback contextCB;
    private CLProgramCallback programCB;


    // data on the host
    private FloatBuffer v;
    private FloatBuffer scalingFactor;
    private IntBuffer e;
    private IntBuffer t;
    private float delta = 0.5f;

    // addresses to memory on the GPU
    private long clVertices;
    private long clEdges;
    private long clTriangles;
    private long clForces;
    private long clLengths;
    private long clqLengths;
    private long clPartialSum;
    private long clScalingFactor;
    private ArrayList<Long> clSizes = new ArrayList<>();

    // size
    private int n;
    private int numberOfVertices;
    private int numberOfEdges;
    private int numberOfFaces;

    private long maxGroupSize;
    private long maxComputeUnits;

    private PointerBuffer clGlobalWorkSizeEdges;
    private PointerBuffer clGlobalWorkSizeVertices;

    public CLDistMesh(@NotNull AMesh<? extends IPoint> mesh) {
        this.stack = MemoryStack.stackPush();
        this.v = CLGatherer.getVertices(mesh, stack);
        this.e = CLGatherer.getEdges(mesh, stack);
        this.t = CLGatherer.getFaces(mesh, stack);
        this.numberOfVertices = mesh.getNumberOfVertices();
        this.numberOfEdges = mesh.getNumberOfEdges();
        this.numberOfFaces = mesh.getNumberOfFaces();
    }

    private void initCallbacks() {
        contextCB = CLContextCallback.create((errinfo, private_info, cb, user_data) ->
        {
            log.warn("[LWJGL] cl_context_callback");
            log.warn("\tInfo: " + memUTF8(errinfo));
        });

        programCB = CLProgramCallback.create((program, user_data) ->
        {
            log.info("The cl_program [0x"+program+"] was built " + (InfoUtils.getProgramBuildInfoInt(program, clDevice, CL_PROGRAM_BUILD_STATUS) == CL_SUCCESS ? "successfully" : "unsuccessfully"));
        });
    }

    private void initCL() {
        // helper for the memory allocation in java
        //stack = MemoryStack.stackPush();
        errcode_ret = stack.callocInt(1);

        IntBuffer numberOfPlatforms = stack.mallocInt(1);
        clGetPlatformIDs(null, numberOfPlatforms);
        PointerBuffer platformIDs = stack.mallocPointer(numberOfPlatforms.get(0));
        clGetPlatformIDs(platformIDs, numberOfPlatforms);

        clPlatform = platformIDs.get(0);

        IntBuffer numberOfDevices = stack.mallocInt(1);
        clGetDeviceIDs(clPlatform, CL_DEVICE_TYPE_GPU, null, numberOfDevices);
        PointerBuffer deviceIDs = stack.mallocPointer(numberOfDevices.get(0));
        clGetDeviceIDs(clPlatform, CL_DEVICE_TYPE_GPU, deviceIDs, numberOfDevices);

        clDevice = deviceIDs.get(0);

        printDeviceInfo(clDevice, "CL_DEVICE_NAME", CL_DEVICE_NAME);

        PointerBuffer ctxProps = stack.mallocPointer(3);
        ctxProps.put(CL_CONTEXT_PLATFORM)
                .put(clPlatform)
                .put(NULL)
                .flip();

        clContext = clCreateContext(ctxProps, clDevice, contextCB, NULL, errcode_ret);
        InfoUtils.checkCLError(errcode_ret);

        clQueue = clCreateCommandQueue(clContext, clDevice, 0, errcode_ret);

        PointerBuffer pp = stack.mallocPointer(1);
        clGetDeviceInfo(clDevice, CL_DEVICE_MAX_WORK_GROUP_SIZE, pp, null);
        maxGroupSize = pp.get(0);
        clGetDeviceInfo(clDevice, CL_DEVICE_MAX_COMPUTE_UNITS, pp, null);
        maxComputeUnits = pp.get(0);
    }

    private void buildProgram() {
        PointerBuffer strings = BufferUtils.createPointerBuffer(1);
        PointerBuffer lengths = BufferUtils.createPointerBuffer(1);

        ByteBuffer source;
        try {
            source = IOUtil.ioResourceToByteBuffer("DistMesh.cl", 4096);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        strings.put(0, source);
        lengths.put(0, source.remaining());

        clProgram = clCreateProgramWithSource(clContext, strings, lengths, errcode_ret);

        int errcode = clBuildProgram(clProgram, clDevice, "", programCB, NULL);
        InfoUtils.checkCLError(errcode);

        clKernelLengths = clCreateKernel(clProgram, "computeLengths", errcode_ret);
        clKernelPartialSF = clCreateKernel(clProgram, "computePartialSF", errcode_ret);
        clKernelCompleteSF = clCreateKernel(clProgram, "computeCompleteSF", errcode_ret);
        clKernelForces = clCreateKernel(clProgram, "computeForces", errcode_ret);
        clKernelMove = clCreateKernel(clProgram, "moveVertices", errcode_ret);
    }

    private void createMemory() {
        clVertices = clCreateBuffer(clContext, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, v, errcode_ret);
        clEdges = clCreateBuffer(clContext, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, e, errcode_ret);
        clTriangles = clCreateBuffer(clContext, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, t, errcode_ret);
        clForces = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * 2 * numberOfVertices, errcode_ret);
        clLengths = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * 2 * numberOfEdges, errcode_ret);
        clqLengths = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * 2 * numberOfEdges, errcode_ret);

        clScalingFactor = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4, errcode_ret);

        //TODO: smaller
        clPartialSum = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * 2 * numberOfEdges, errcode_ret);
    }

    private void initialKernelArgs() {
        clSetKernelArg1p(clKernelLengths, 0, clVertices);
        clSetKernelArg1p(clKernelLengths, 1, clEdges);
        clSetKernelArg1p(clKernelLengths, 2, clLengths);
        clSetKernelArg1p(clKernelLengths, 3, clqLengths);

        clSetKernelArg1p(clKernelPartialSF, 1, clqLengths);
        clSetKernelArg(clKernelPartialSF, 2, 4 * 2 * numberOfEdges);
        clSetKernelArg1p(clKernelPartialSF, 3, clPartialSum);

        clSetKernelArg1p(clKernelCompleteSF, 1, clqLengths);
        clSetKernelArg(clKernelCompleteSF, 2, 4 * 2 * numberOfEdges);
        clSetKernelArg1p(clKernelCompleteSF, 3, clScalingFactor);

        clSetKernelArg1p(clKernelForces, 0, clVertices);
        clSetKernelArg1p(clKernelForces, 1, clEdges);
        clSetKernelArg1p(clKernelForces, 2, clLengths);
        clSetKernelArg1p(clKernelForces, 3, clScalingFactor);
        clSetKernelArg1p(clKernelForces, 4, clForces);

        clSetKernelArg1p(clKernelMove, 0, clVertices);
        clSetKernelArg1p(clKernelMove, 1, clForces);
        clSetKernelArg1f(clKernelMove, 2, delta);


        clGlobalWorkSizeEdges = BufferUtils.createPointerBuffer(1);
        clGlobalWorkSizeVertices = BufferUtils.createPointerBuffer(1);
        clGlobalWorkSizeEdges.put(0, numberOfEdges);
        clGlobalWorkSizeVertices.put(0, numberOfVertices);
    }

    public void step() {
        /*
         * DistMesh-Loop
         * 1. compute scaling factor
		 * 2. compute forces;
		 * 3. update vertices;
         *
         */
        int potGrpSize = (int)Math.ceil(MathUtils.log(2, maxGroupSize));
        int potWorkLoad = (int)Math.ceil(MathUtils.log(2, numberOfEdges));
        int rounds = potWorkLoad - potGrpSize;

        clEnqueueNDRangeKernel(clQueue, clKernelLengths, 1, null, clGlobalWorkSizeEdges, null, null, null);

        int size = numberOfEdges;
        int globalWorkSize = (int)(maxGroupSize * maxComputeUnits);
        int localWorkSize = (int)maxGroupSize;
        PointerBuffer clGlobalWorkSize = BufferUtils.createPointerBuffer(1);
        PointerBuffer clLocalWorkSize = BufferUtils.createPointerBuffer(1);
        clGlobalWorkSize.put(0, globalWorkSize);
        clLocalWorkSize.put(0, localWorkSize);

        int index = 0;
        while(maxGroupSize < size) {
            IntBuffer clSize = BufferUtils.createIntBuffer(1);
            clSize.put(0, size);
            clSetKernelArg1i(clKernelPartialSF, 0, size);

            clEnqueueNDRangeKernel(clQueue, clKernelPartialSF, 1, null, clGlobalWorkSize, clLocalWorkSize, null, null);

            if(index == 0) {
                size = (int)maxComputeUnits;
            }
            else {
                size = (int)(Math.ceil(size * 1.0 / maxGroupSize));
            }
            index++;
        }
        PointerBuffer clGlobalWorkSize2 = BufferUtils.createPointerBuffer(1);
        PointerBuffer clLocalWorkSize2 = BufferUtils.createPointerBuffer(1);
        clGlobalWorkSize2.put(0, ceilPowerOf2(size));
        clLocalWorkSize2.put(0, ceilPowerOf2(size));
        IntBuffer clSize = BufferUtils.createIntBuffer(1);
        clSize.put(0, size);
        clSetKernelArg1i(clKernelCompleteSF, 0, size);
        clEnqueueNDRangeKernel(clQueue, clKernelCompleteSF, 1, null, clGlobalWorkSize2, clLocalWorkSize2, null, null);

        clEnqueueNDRangeKernel(clQueue, clKernelForces, 1, null, clGlobalWorkSizeEdges, null, null, null);
        clEnqueueNDRangeKernel(clQueue, clKernelMove, 1, null, clGlobalWorkSizeVertices, null, null, null);

        clFinish(clQueue);

        compareComputation();
        printResult();
    }

    private void compareComputation() {
        scalingFactor = stack.mallocFloat(1);
        clEnqueueReadBuffer(clQueue, clScalingFactor, true, 0, scalingFactor, null, null);
        clEnqueueReadBuffer(clQueue, clVertices, true, 0, v, null, null);
        System.out.println("scalingFactor:" + scalingFactor.get(0));
    }

    private void printResult() {
        log.info("after");
        for(int i = 0; i < numberOfVertices*2; i += 2) {
            log.info(v.get(i) + ", " + v.get(i+1));
        }
    }

    private int ceilPowerOf2(int value) {
        int tmp = 1;
        while (tmp < value) {
            tmp = tmp << 1;
        }
        return tmp;
    }

    private long ceilPowerOf2(long value) {
        long tmp = 1;
        while (tmp < value) {
            tmp = tmp << 1;
        }
        return tmp;
    }

    private void clearCL() {
        contextCB.free();
        programCB.free();
        clReleaseMemObject(clVertices);
        clReleaseMemObject(clEdges);
        clReleaseMemObject(clTriangles);
        clReleaseMemObject(clForces);
        clReleaseMemObject(clLengths);
        clReleaseMemObject(clqLengths);
        clReleaseMemObject(clPartialSum);
        clReleaseMemObject(clScalingFactor);

        clReleaseKernel(clKernelForces);
        clReleaseKernel(clKernelMove);
        clReleaseKernel(clKernelLengths);
        clReleaseKernel(clKernelPartialSF);
        clReleaseKernel(clKernelCompleteSF);

        clReleaseCommandQueue(clQueue);
        clReleaseProgram(clProgram);
        clReleaseContext(clContext);
    }

    public void init() {
        initCallbacks();
        initCL();
        buildProgram();
        createMemory();
        initialKernelArgs();
    }

    public void finish() {
        clearCL();
    }

    /*
     *
     * Assumption: There is only one Platform with a GPU.
     */
    public static void main(String... args) {
        AMesh<VPoint> mesh = IFace.createSimpleTriMesh();

        log.info("before");
        Collection<AVertex<VPoint>> vertices = mesh.getVertices();
        log.info(vertices);

        CLDistMesh clDistMesh = new CLDistMesh(mesh);
        clDistMesh.init();
        clDistMesh.step();
        clDistMesh.finish();
    }

    private static void printPlatformInfo(long platform, String param_name, int param) {
        System.out.println("\t" + param_name + " = " + InfoUtils.getPlatformInfoStringUTF8(platform, param));
    }

    private static void printDeviceInfo(long device, String param_name, int param) {
        System.out.println("\t" + param_name + " = " + InfoUtils.getDeviceInfoStringUTF8(device, param));
    }


}
