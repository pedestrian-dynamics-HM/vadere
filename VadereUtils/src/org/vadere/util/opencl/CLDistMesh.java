package org.vadere.util.opencl;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.vadere.util.geometry.mesh.gen.*;
import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.MPoint;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.opencl.CL10.clEnqueueNDRangeKernel;
import static org.lwjgl.opencl.CL11.CL_KERNEL_PREFERRED_WORK_GROUP_SIZE_MULTIPLE;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memUTF8;

/**
 * @author Benedikt Zoennchen
 *
 * DistMesh GPU implementation.
 */
public class CLDistMesh<P extends IPoint> {

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

    private long clKernelFlip;

    private long clKernelFlipStage1;
    private long clKernelFlipStage2;
    private long clKernelFlipStage3;

    private long clKernelRepair;
    private long clKernelLabelEdges;
    private long clKernelLabelEdgesUpdate;

    // error code buffer
    private IntBuffer errcode_ret;

    // CL callbacks
    private CLContextCallback contextCB;
    private CLProgramCallback programCB;


    // data on the host
    private DoubleBuffer vD;
    private DoubleBuffer scalingFactorD;
    private FloatBuffer vF;
    private FloatBuffer scalingFactorF;
    private IntBuffer e;
    private IntBuffer t;
    private IntBuffer twins;
    private IntBuffer mutexes;
    private IntBuffer triLocks;
    private IntBuffer edgeLabels;
    private IntBuffer isLegal;
    private double delta = 0.02;
    private float fDelta = 0.02f;

    // addresses to memory on the GPU
    private long clVertices;
    private long clEdges;
    private long clTwins;
    private long clTriangles;
    private long clForces;
    private long clLengths;
    private long clqLengths;
    private long clPartialSum;
    private long clScalingFactor;
    private long clMutexes;
    private long clMutex;
    private long clRelation;
    private long clEdgeLabels;
    private long clTriLocks;
    private long clIsLegal;
    private ArrayList<Long> clSizes = new ArrayList<>();

    // size
    private int n;
    private int numberOfVertices;
    private int numberOfEdges;
    private int numberOfFaces;

    private long maxGroupSize;
    private long maxComputeUnits;
    private long prefdWorkGroupSizeMultiple;

    private PointerBuffer clGlobalWorkSizeEdges;
    private PointerBuffer clGlobalWorkSizeVertices;
    private PointerBuffer clGlobalWorkSizeTriangles;

    private PointerBuffer clGloblWorkSizeSFPartial;
    private PointerBuffer clLocalWorkSizeSFPartial;

    private PointerBuffer clGloblWorkSizeSFComplete;
    private PointerBuffer clLocalWorkSizeSFComplete;
    private PointerBuffer clLocalWorkSizeOne;

    private AMesh<P> mesh;

    private boolean doublePrecision = false;

    private Collection<P> result;

    public CLDistMesh(@NotNull AMesh<P> mesh) {
        this.mesh = mesh;
        this.mesh.garbageCollection();
        this.stack = MemoryStack.stackPush();
        if(doublePrecision) {
            this.vD = CLGatherer.getVerticesD(mesh);
        }
        else {
            this.vF = CLGatherer.getVerticesF(mesh);
        }
        this.e = CLGatherer.getEdges(mesh);
        this.t = CLGatherer.getFaces(mesh);
        this.twins = CLGatherer.getTwins(mesh);
        this.numberOfVertices = mesh.getNumberOfVertices();
        this.numberOfEdges = mesh.getNumberOfEdges();
        this.numberOfFaces = mesh.getNumberOfFaces();
        this.mutexes =  MemoryUtil.memAllocInt(numberOfVertices);
        this.triLocks = MemoryUtil.memAllocInt(numberOfFaces);
        for(int i = 0; i < numberOfVertices; i++) {
            this.mutexes.put(i, 0);
        }
        this.edgeLabels = MemoryUtil.memAllocInt(numberOfEdges);
        for(int i = 0; i < numberOfEdges; i++) {
            this.edgeLabels.put(i, 0);
        }
        this.isLegal = MemoryUtil.memAllocInt(1);
        this.isLegal.put(0, 1);
        this.result = null;
    }

    private void initCallbacks() {
        contextCB = CLContextCallback.create((errinfo, private_info, cb, user_data) ->
        {
            log.warn("[LWJGL] cl_context_callback");
            log.warn("\tInfo: " + memUTF8(errinfo));
        });

        programCB = CLProgramCallback.create((program, user_data) ->
        {
            log.info("The cl_program [0x"+program+"] was built " + (CLInfo.getProgramBuildInfoInt(program, clDevice, CL_PROGRAM_BUILD_STATUS) == CL_SUCCESS ? "successfully" : "unsuccessfully"));
            String message = CLInfo.getProgramBuildInfoStringASCII(program, clDevice, CL_PROGRAM_BUILD_LOG);
            if (!message.isEmpty()) {
                log.info("BUILD LOG:\n----\n"+message+"\n-----");
            }
        });
    }

    private void initCL() {
        // helper for the memory allocation in java
        //stack = MemoryStack.stackPush();
        errcode_ret = MemoryUtil.memAllocInt(1);

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
        CLInfo.checkCLError(errcode_ret);

        clQueue = clCreateCommandQueue(clContext, clDevice, 0, errcode_ret);
        CLInfo.checkCLError(errcode_ret);

        PointerBuffer pp = stack.mallocPointer(1);
        clGetDeviceInfo(clDevice, CL_DEVICE_MAX_WORK_GROUP_SIZE, pp, null);
        maxGroupSize = pp.get(0);
        clGetDeviceInfo(clDevice, CL_DEVICE_MAX_COMPUTE_UNITS, pp, null);
        maxComputeUnits = pp.get(0);
        log.info("MAX_GRP_SIZE = " + maxGroupSize);
        log.info("MAX_COMPUTE_UNITS = " + maxComputeUnits);
    }

    private void buildProgram() {
        PointerBuffer strings = BufferUtils.createPointerBuffer(1);
        PointerBuffer lengths = BufferUtils.createPointerBuffer(1);

        ByteBuffer source;
        try {
            if(doublePrecision) {
                source = CLUtils.ioResourceToByteBuffer("DistMeshDouble.cl", 4096);
            }
            else {
                source = CLUtils.ioResourceToByteBuffer("DistMesh.cl", 4096);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        strings.put(0, source);
        lengths.put(0, source.remaining());

        clProgram = clCreateProgramWithSource(clContext, strings, lengths, errcode_ret);

        int errcode = clBuildProgram(clProgram, clDevice, "", programCB, NULL);
        CLInfo.checkCLError(errcode);

        clKernelLengths = clCreateKernel(clProgram, "computeLengths", errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        PointerBuffer pp = stack.mallocPointer(1);
        clGetKernelWorkGroupInfo(clKernelLengths, clDevice, CL_KERNEL_PREFERRED_WORK_GROUP_SIZE_MULTIPLE, pp, null);
        prefdWorkGroupSizeMultiple = pp.get(0);
        log.info("PREF_WORK_GRP_SIZE_MUL = " + prefdWorkGroupSizeMultiple);

        clKernelPartialSF = clCreateKernel(clProgram, "computePartialSF", errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clKernelCompleteSF = clCreateKernel(clProgram, "computeCompleteSF", errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clKernelForces = clCreateKernel(clProgram, "computeForces", errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clKernelMove = clCreateKernel(clProgram, "moveVertices", errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clKernelFlip = clCreateKernel(clProgram, "flip", errcode_ret);
        CLInfo.checkCLError(errcode_ret);

        clKernelFlipStage1 = clCreateKernel(clProgram, "flipStage1", errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clKernelFlipStage2 = clCreateKernel(clProgram, "flipStage2", errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clKernelFlipStage3 = clCreateKernel(clProgram, "flipStage3", errcode_ret);
        CLInfo.checkCLError(errcode_ret);

        clKernelLabelEdges = clCreateKernel(clProgram, "label", errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clKernelLabelEdgesUpdate = clCreateKernel(clProgram, "updateLabel", errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clKernelRepair = clCreateKernel(clProgram, "repair", errcode_ret);
        CLInfo.checkCLError(errcode_ret);
    }

    private void createMemory() {
        int factor = doublePrecision ? 8 : 4; // 8 or 4 byte for a floating point
        if(doublePrecision) {
            clVertices = clCreateBuffer(clContext, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, vD, errcode_ret);
        }
        else {
            clVertices = clCreateBuffer(clContext, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, vF, errcode_ret);
        }
        CLInfo.checkCLError(errcode_ret);
        clEdges = clCreateBuffer(clContext, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, e, errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clTriangles = clCreateBuffer(clContext, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, t, errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clForces = clCreateBuffer(clContext, CL_MEM_READ_WRITE, factor * 2 * numberOfVertices, errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clLengths = clCreateBuffer(clContext, CL_MEM_READ_WRITE, factor * 2 * numberOfEdges, errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clqLengths = clCreateBuffer(clContext, CL_MEM_READ_WRITE, factor * 2 * numberOfEdges, errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clScalingFactor = clCreateBuffer(clContext, CL_MEM_READ_WRITE, factor, errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clMutexes = clCreateBuffer(clContext, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, mutexes, errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clTriLocks = clCreateBuffer(clContext, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, triLocks, errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clRelation = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * numberOfFaces, errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clEdgeLabels = clCreateBuffer(clContext, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, edgeLabels, errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clIsLegal = clCreateBuffer(clContext, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, isLegal, errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clMutex = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4, errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clTwins = clCreateBuffer(clContext, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, twins, errcode_ret);
        CLInfo.checkCLError(errcode_ret);
    }

    private void initialKernelArgs() {
        int factor = doublePrecision ? 8 : 4;
        int sizeSFPartial = numberOfEdges;

        clSetKernelArg1p(clKernelLengths, 0, clVertices);
        clSetKernelArg1p(clKernelLengths, 1, clEdges);
        clSetKernelArg1p(clKernelLengths, 2, clLengths);
        clSetKernelArg1p(clKernelLengths, 3, clqLengths);

        clSetKernelArg1i(clKernelPartialSF, 0, sizeSFPartial);
        clSetKernelArg1p(clKernelPartialSF, 1, clqLengths);
        clSetKernelArg(clKernelPartialSF, 2, factor * 2 * maxGroupSize);
        clPartialSum = clCreateBuffer(clContext, CL_MEM_READ_WRITE, factor * 2 * prefdWorkGroupSizeMultiple, errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clSetKernelArg1p(clKernelPartialSF, 3, clPartialSum);

        int sizeSFComplete = Math.min((int)prefdWorkGroupSizeMultiple, numberOfEdges); // one item per work group
        clSetKernelArg1i(clKernelCompleteSF, 0, sizeSFComplete);
        if(numberOfEdges > prefdWorkGroupSizeMultiple) {
            clSetKernelArg1p(clKernelCompleteSF, 1, clPartialSum);
        }
        else {
            clSetKernelArg1p(clKernelCompleteSF, 1, clqLengths);
        }
        clSetKernelArg(clKernelCompleteSF, 2, factor * 2 * sizeSFComplete);
        clSetKernelArg1p(clKernelCompleteSF, 3, clScalingFactor);

        clSetKernelArg1p(clKernelForces, 0, clVertices);
        clSetKernelArg1p(clKernelForces, 1, clEdges);
        clSetKernelArg1p(clKernelForces, 2, clLengths);
        clSetKernelArg1p(clKernelForces, 3, clScalingFactor);
        clSetKernelArg1p(clKernelForces, 4, clForces);
        clSetKernelArg1p(clKernelForces, 5, clMutexes);

        clSetKernelArg1p(clKernelMove, 0, clVertices);
        clSetKernelArg1p(clKernelMove, 1, clForces);
        if(doublePrecision) {
            clSetKernelArg1d(clKernelMove, 2, delta);
        }
        else {
            clSetKernelArg1f(clKernelMove, 2, fDelta);
        }


        clSetKernelArg1p(clKernelLabelEdges, 0, clVertices);
        clSetKernelArg1p(clKernelLabelEdges, 1, clEdges);
        clSetKernelArg1p(clKernelLabelEdges, 2, clTriangles);
        clSetKernelArg1p(clKernelLabelEdges, 3, clEdgeLabels);
        clSetKernelArg1p(clKernelLabelEdges, 4, clIsLegal);

        clSetKernelArg1p(clKernelLabelEdgesUpdate, 0, clVertices);
        clSetKernelArg1p(clKernelLabelEdgesUpdate, 1, clEdges);
        clSetKernelArg1p(clKernelLabelEdgesUpdate, 2, clTriangles);
        clSetKernelArg1p(clKernelLabelEdgesUpdate, 3, clEdgeLabels);
        clSetKernelArg1p(clKernelLabelEdgesUpdate, 4, clIsLegal);
        clSetKernelArg1p(clKernelLabelEdgesUpdate, 5, clTriLocks);

        clSetKernelArg1p(clKernelFlip, 0, clEdges);
        clSetKernelArg1p(clKernelFlip, 1, clTriangles);
        clSetKernelArg1p(clKernelFlip, 2, clEdgeLabels);
        clSetKernelArg1p(clKernelFlip, 3, clTriLocks);
        clSetKernelArg1p(clKernelFlip, 4, clRelation);
        clSetKernelArg1p(clKernelFlip, 5, clMutex);
        clSetKernelArg1p(clKernelFlip, 6, clTwins);

        clSetKernelArg1p(clKernelFlipStage1, 0, clEdges);
        clSetKernelArg1p(clKernelFlipStage1, 1, clEdgeLabels);
        clSetKernelArg1p(clKernelFlipStage1, 2, clTriLocks);

        clSetKernelArg1p(clKernelFlipStage2, 0, clEdges);
        clSetKernelArg1p(clKernelFlipStage2, 1, clEdgeLabels);
        clSetKernelArg1p(clKernelFlipStage2, 2, clTriLocks);

        clSetKernelArg1p(clKernelFlipStage3, 0, clEdges);
        clSetKernelArg1p(clKernelFlipStage3, 1, clTriangles);
        clSetKernelArg1p(clKernelFlipStage3, 2, clEdgeLabels);
        clSetKernelArg1p(clKernelFlipStage3, 3, clTriLocks);
        clSetKernelArg1p(clKernelFlipStage3, 4, clRelation);
        clSetKernelArg1p(clKernelFlipStage3, 5, clTwins);
        clSetKernelArg1p(clKernelFlipStage3, 6, clVertices);

        clSetKernelArg1p(clKernelRepair, 0, clEdges);
        clSetKernelArg1p(clKernelRepair, 1, clTriangles);
        clSetKernelArg1p(clKernelRepair, 2, clRelation);

        clGloblWorkSizeSFPartial = BufferUtils.createPointerBuffer(1);
        clLocalWorkSizeSFPartial = BufferUtils.createPointerBuffer(1);
        clGloblWorkSizeSFPartial.put(0, (int)(maxGroupSize * prefdWorkGroupSizeMultiple));
        clLocalWorkSizeSFPartial.put(0, (int)maxGroupSize);

        clGloblWorkSizeSFComplete = BufferUtils.createPointerBuffer(1);
        clLocalWorkSizeSFComplete = BufferUtils.createPointerBuffer(1);
        clLocalWorkSizeOne = BufferUtils.createPointerBuffer(1);

        clGloblWorkSizeSFComplete.put(0, ceilPowerOf2(sizeSFComplete));
        clLocalWorkSizeSFComplete.put(0, ceilPowerOf2(sizeSFComplete));
        clLocalWorkSizeOne.put(0, 1);
        clGlobalWorkSizeEdges = BufferUtils.createPointerBuffer(1);
        clGlobalWorkSizeVertices = BufferUtils.createPointerBuffer(1);
        clGlobalWorkSizeTriangles = BufferUtils.createPointerBuffer(1);
        clGlobalWorkSizeEdges.put(0, numberOfEdges);
        clGlobalWorkSizeVertices.put(0, numberOfVertices);
        clGlobalWorkSizeTriangles.put(0, numberOfFaces);
    }

  /*  public void refreshPoints() {
        // 1. get the new points into the host memory
        if(doublePrecision) {
            this.vD = CLGatherer.getVerticesD(mesh, vD);
            th
        }
        else {
            this.vF = CLGatherer.getVerticesF(mesh, vF);
        }

        // 2. transfer the host memory to the gpu
        if(doublePrecision) {
            clEnqueueWriteBuffer(clQueue, clVertices, true, 0, vD, null, null);
        }
        else {
            clEnqueueWriteBuffer(clQueue, clVertices, true, 0, vF, null, null);
        }
    }*/

    public boolean step() {
        return step(true);
    }

    // TODO: think about the use of only 1 work-group!!! It might be bad! solution: use global barrier? force computation? flip hangs after some time?
    public boolean step(final boolean flipAll) {
        /*
         * DistMesh-Loop
         * 1. compute scaling factor
		 * 2. compute forces;
		 * 3. update vertices;
         *
         */
        clEnqueueNDRangeKernel(clQueue, clKernelLengths, 1, null, clGlobalWorkSizeEdges, null, null, null);
        if(numberOfEdges > prefdWorkGroupSizeMultiple) {
           clEnqueueNDRangeKernel(clQueue, clKernelPartialSF, 1, null, clGloblWorkSizeSFPartial, clLocalWorkSizeSFPartial, null, null);
        }

        clEnqueueNDRangeKernel(clQueue, clKernelCompleteSF, 1, null, clGloblWorkSizeSFComplete, clLocalWorkSizeSFComplete, null, null);
        log.info("computed scale factor");

        // force to use only 1 work group => local size = local size
        clEnqueueNDRangeKernel(clQueue, clKernelForces, 1, null, clGlobalWorkSizeEdges, null, null, null);
        log.info("computed forces");

        clEnqueueNDRangeKernel(clQueue, clKernelMove, 1, null, clGlobalWorkSizeVertices, null, null, null);
        log.info("move vertices");

        boolean requireRetriangulation = false;

        if(flipAll) {
            IntBuffer isLegal = stack.mallocInt(1);
            // while there is any illegal edge, do: // TODO: this is not the same as in the java distmesh!
            clEnqueueNDRangeKernel(clQueue, clKernelLabelEdges, 1, null, clGlobalWorkSizeEdges, null, null, null);
            int maxIt = 6;
            int it = 0;

            do  {
                it++;
                isLegal.put(0, 1);
                clEnqueueWriteBuffer(clQueue, clIsLegal, true, 0, isLegal, null, null);

                //clEnqueueNDRangeKernel(clQueue, clKernelFlip, 1, null, clGlobalWorkSizeEdges, null, null, null);

                clEnqueueNDRangeKernel(clQueue, clKernelFlipStage1, 1, null, clGlobalWorkSizeEdges, null, null, null);
                clFinish(clQueue);
                clEnqueueNDRangeKernel(clQueue, clKernelFlipStage2, 1, null, clGlobalWorkSizeEdges, null, null, null);
                clFinish(clQueue);
                clEnqueueNDRangeKernel(clQueue, clKernelFlipStage3, 1, null, clGlobalWorkSizeEdges, null, null, null);

                clEnqueueNDRangeKernel(clQueue, clKernelRepair, 1, null, clGlobalWorkSizeEdges, null, null, null);
                clEnqueueNDRangeKernel(clQueue, clKernelLabelEdgesUpdate, 1, null, clGlobalWorkSizeEdges, null, null, null);

                clEnqueueReadBuffer(clQueue, clIsLegal, true, 0, isLegal, null, null);
                log.info("isLegal = " + isLegal.get(0));

                break;
                /*if(it >= maxIt) {
                    requireRetriangulation = true;
                    break;
                }*/
            } while(isLegal.get(0) == 0);
            log.info("flip all");
        }

       /* clEnqueueNDRangeKernel(clQueue, clKernelFlip, 1, null, clGlobalWorkSizeEdes, clLocalWorkSizeOne, null, null);
        clEnqueueNDRangeKernel(clQueue, clKernelRepair, 1, null, clGlobalWorkSizeEdes, null, null, null);

        clEnqueueNDRangeKernel(clQueue, clKernelFlip, 1, null, clGlobalWorkSizeEdes, clLocalWorkSizeOne, null, null);
        clEnqueueNDRangeKernel(clQueue, clKernelRepair, 1, null, clGlobalWorkSizeEdes, null, null, null);

        clEnqueueNDRangeKernel(clQueue, clKernelFlip, 1, null, clGlobalWorkSizeEdes, clLocalWorkSizeOne, null, null);
        clEnqueueNDRangeKernel(clQueue, clKernelRepair, 1, null, clGlobalWorkSizeEdes, null, null, null);
*/
        clFinish(clQueue);

        return requireRetriangulation;
    }

    private void readResultFromGPU() {
        if(doublePrecision) {
            scalingFactorD = stack.mallocDouble(1);
            clEnqueueReadBuffer(clQueue, clScalingFactor, true, 0, scalingFactorD, null, null);
            clEnqueueReadBuffer(clQueue, clVertices, true, 0, vD, null, null);
        }
        else {
            scalingFactorF = stack.mallocFloat(1);
            clEnqueueReadBuffer(clQueue, clScalingFactor, true, 0, scalingFactorF, null, null);
            clEnqueueReadBuffer(clQueue, clVertices, true, 0, vF, null, null);
        }

        clEnqueueReadBuffer(clQueue, clTriangles, true, 0, t, null, null);
        clEnqueueReadBuffer(clQueue, clEdges, true, 0, e, null, null);

    }

    private Collection<P> readResultFromHost() {
        List<P> pointSet = new ArrayList<>(numberOfVertices);
        if(doublePrecision) {
            for(int i = 0; i < numberOfVertices*2; i+=2) {
                pointSet.add(mesh.createPoint(vD.get(i), vD.get(i+1)));
            }
        }
        else {
            for(int i = 0; i < numberOfVertices*2; i+=2) {
                pointSet.add(mesh.createPoint(vF.get(i), vF.get(i+1)));
            }
        }

        return pointSet;
    }

    /*private void updateMesh(){
        int i = 0;
        if(doublePrecision) {
            for(AVertex<P> vertex : mesh.getVertices()) {
                vertex.getPoint().set(vD.get(i), vD.get(i+1));
                i += 2;
            }
        }
        else {
            for(AVertex<P> vertex : mesh.getVertices()) {
                vertex.getPoint().set(vF.get(i), vF.get(i+1));
                i += 2;
            }
        }
    }*/

    private void printResult() {
        log.info("after");
        if(doublePrecision) {
            for(int i = 0; i < numberOfVertices*2; i += 2) {
                log.info(vD.get(i) + ", " + vD.get(i+1));
            }
        } else {
            for(int i = 0; i < numberOfVertices*2; i += 2) {
                log.info(vF.get(i) + ", " + vF.get(i+1));
            }
        }

        log.info("scalingFactor:" + (doublePrecision ? scalingFactorD.get(0) : scalingFactorF.get(0)));
    }

    private int ceilPowerOf2(int value) {
        int tmp = 1;
        while (tmp <= value) {
            tmp = tmp << 1;
        }
        return tmp;
    }

    private long ceilPowerOf2(long value) {
        long tmp = 1;
        while (tmp <= value) {
            tmp = tmp << 1;
        }
        return tmp;
    }

    private void clearCL() {
        clReleaseMemObject(clVertices);
        clReleaseMemObject(clEdges);
        clReleaseMemObject(clTriangles);
        clReleaseMemObject(clForces);
        clReleaseMemObject(clLengths);
        clReleaseMemObject(clqLengths);
        clReleaseMemObject(clPartialSum);
        clReleaseMemObject(clScalingFactor);
        clReleaseMemObject(clMutexes);
        clReleaseMemObject(clTriLocks);
        clReleaseMemObject(clRelation);
        clReleaseMemObject(clEdgeLabels);
        clReleaseMemObject(clIsLegal);
        clReleaseMemObject(clMutex);
        clReleaseMemObject(clTwins);

        clReleaseKernel(clKernelForces);
        clReleaseKernel(clKernelMove);
        clReleaseKernel(clKernelLengths);
        clReleaseKernel(clKernelPartialSF);
        clReleaseKernel(clKernelCompleteSF);

        clReleaseKernel(clKernelFlip);

        clReleaseKernel(clKernelFlipStage1);
        clReleaseKernel(clKernelFlipStage2);
        clReleaseKernel(clKernelFlipStage3);

        clReleaseKernel(clKernelRepair);
        clReleaseKernel(clKernelLabelEdges);
        clReleaseKernel(clKernelLabelEdgesUpdate);


        contextCB.free();
        programCB.free();
        clReleaseCommandQueue(clQueue);
        clReleaseProgram(clProgram);
        clReleaseContext(clContext);
    }

    private void clearHost() {
        if(doublePrecision) {
            MemoryUtil.memFree(vD);
            MemoryUtil.memFree(scalingFactorD);
        }
        else {
            MemoryUtil.memFree(vF);
            MemoryUtil.memFree(scalingFactorF);
        }

        MemoryUtil.memFree(e);
        MemoryUtil.memFree(t);
        MemoryUtil.memFree(mutexes);
        MemoryUtil.memFree(triLocks);
        MemoryUtil.memFree(edgeLabels);
        MemoryUtil.memFree(isLegal);
        MemoryUtil.memFree(twins);

        /*MemoryUtil.memFree(clGlobalWorkSizeEdges);
        MemoryUtil.memFree(clGlobalWorkSizeVertices);

        MemoryUtil.memFree(clGloblWorkSizeSFPartial);
        MemoryUtil.memFree(clLocalWorkSizeSFPartial);

        MemoryUtil.memFree(clGloblWorkSizeSFComplete);
        MemoryUtil.memFree(clLocalWorkSizeSFComplete);

        MemoryUtil.memFree(clGlobalWorkSizeEdes);
        MemoryUtil.memFree(clLocalWorkSizeOne);*/
    }

    public void init() {
        initCallbacks();
        initCL();
        buildProgram();
        createMemory();
        initialKernelArgs();
    }

    public void refresh () {
        readResultFromGPU();
        result = readResultFromHost();
        //printResult();
    }

    public void finish() {
        refresh();
        //updateMesh();
        clearCL();
        //clearHost();
    }

    public Collection<P> getResult() {
        return result;
    }

    private void printTri() {
        for(int i = 0; i < numberOfFaces*4; i+=4) {
            log.info("[" +t.get(i) + ", " + t.get(i+1) + ", " + t.get(i+2) + "]");
        }
    }

    private void printEdges() {
        for(int i = 0; i < numberOfEdges*4; i+=4) {
            log.info("[v0=" +e.get(i) + ", v1=" + e.get(i+1) + ", t_a=" + e.get(i+2) +", t_b=" + e.get(i+3) +  "]");
        }
    }

    /*
     *
     * Assumption: There is only one Platform with a GPU.
     */
    public static void main(String... args) {
        AMesh<MPoint> mesh = IFace.createSimpleTriMesh();
        log.info("before");
        Collection<AVertex<MPoint>> vertices = mesh.getVertices();
        log.info(vertices);

        CLDistMesh clDistMesh = new CLDistMesh(mesh);
        clDistMesh.init();

        clDistMesh.printTri();
        clDistMesh.printEdges();
        clDistMesh.step();

        /*clDistMesh.refresh();

        clDistMesh.printTri();
        clDistMesh.printEdges();
        clDistMesh.step();*/

        clDistMesh.refresh();
        clDistMesh.printTri();
        clDistMesh.printEdges();

        clDistMesh.finish();
    }

    private static void printPlatformInfo(long platform, String param_name, int param) {
        System.out.println("\t" + param_name + " = " + CLInfo.getPlatformInfoStringUTF8(platform, param));
    }

    private static void printDeviceInfo(long device, String param_name, int param) {
        System.out.println("\t" + param_name + " = " + CLInfo.getDeviceInfoStringUTF8(device, param));
    }


}
