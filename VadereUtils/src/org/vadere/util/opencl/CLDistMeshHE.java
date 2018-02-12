package org.vadere.util.opencl;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CLContextCallback;
import org.lwjgl.opencl.CLProgramCallback;
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
import java.util.*;
import java.util.stream.Collectors;

import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.opencl.CL11.CL_KERNEL_PREFERRED_WORK_GROUP_SIZE_MULTIPLE;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memUTF8;

/**
 * @author Benedikt Zoennchen
 *
 * DistMesh GPU implementation.
 */
public class CLDistMeshHE<P extends IPoint> {

    private static Logger log = LogManager.getLogger(CLDistMeshHE.class);

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

    private long clKernelFlipStage1;
    private long clKernelFlipStage2;
    private long clKernelFlipStage3;
    private long clKernelUnlockFaces;

    private long clKernelLabelEdges;
    private long clKernelLabelEdgesUpdate;
    private long clKernelCheckTriangles;

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
    private IntBuffer vToE;
    private IntBuffer borderV;
    private IntBuffer edgeLabels;
    private double delta = 0.02;
    private float fDelta = 0.02f;

    // addresses to memory on the GPU
    private long clVertices;
    private long clBorderVertices;
    private long clEdges;
    private long clVtoE;
    private long clFaces;
    private long clForces;
    private long clLengths;
    private long clqLengths;
    private long clPartialSum;
    private long clScalingFactor;
    private long clEdgeLabels;
    private long clIllegalEdges;
    private long clIllegalTriangles;

    // size
    private int numberOfVertices;
    private int numberOfEdges;
    private int numberOfFaces;

    private long maxGroupSize;
    private long maxComputeUnits;
    private long prefdWorkGroupSizeMultiple;
    private long prefdWorkGroupSizeMultipleForces;

    private PointerBuffer clGlobalWorkSizeEdges;
    private PointerBuffer clGlobalWorkSizeVertices;
    private PointerBuffer clGlobalWorkSizeTriangles;

    private PointerBuffer clGloblWorkSizeSFPartial;
    private PointerBuffer clLocalWorkSizeSFPartial;
    private PointerBuffer clGloblWorkSizeForces;
    private PointerBuffer clLocalWorkSizeForces;

    private PointerBuffer clGloblWorkSizeSFComplete;
    private PointerBuffer clLocalWorkSizeSFComplete;
    private PointerBuffer clLocalWorkSizeOne;

    // time measurement
    private PointerBuffer clEvent;
    private ByteBuffer startTime;
    private ByteBuffer endTime;
    private PointerBuffer retSize;

    private AMesh<P> mesh;

    private boolean doublePrecision = false;
    private boolean profiling = true;

    private boolean hasToRead = false;

    public CLDistMeshHE(@NotNull AMesh<P> mesh) {
        this.mesh = mesh;
        this.mesh.garbageCollection();
        this.stack = MemoryStack.stackPush();
        if(doublePrecision) {
            this.vD = CLGatherer.getVerticesD(mesh);
        }
        else {
            this.vF = CLGatherer.getVerticesF(mesh);
        }
        this.e = CLGatherer.getHalfEdges(mesh);
        this.t = CLGatherer.getFaces(mesh);
        this.vToE = CLGatherer.getVertexToEdge(mesh);
        this.borderV = CLGatherer.getBorderVertices(mesh);

        this.numberOfVertices = mesh.getNumberOfVertices();
        this.numberOfEdges = mesh.getNumberOfEdges();
        this.numberOfFaces = mesh.getNumberOfFaces();
        this.edgeLabels = MemoryUtil.memAllocInt(numberOfEdges);

        for(int i = 0; i < numberOfEdges; i++) {
            this.edgeLabels.put(i, 0);
        }
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

        if(profiling) {
            clQueue = clCreateCommandQueue(clContext, clDevice, CL_QUEUE_PROFILING_ENABLE, errcode_ret);
        }
        else {
            clQueue = clCreateCommandQueue(clContext, clDevice, 0, errcode_ret);
        }

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
                source = CLUtils.ioResourceToByteBuffer("DistMeshHE.cl", 4096);
            }
            else {
                source = CLUtils.ioResourceToByteBuffer("DistMeshHE.cl", 4096);
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
        clGetKernelWorkGroupInfo(clKernelForces, clDevice, CL_KERNEL_PREFERRED_WORK_GROUP_SIZE_MULTIPLE, pp, null);
        prefdWorkGroupSizeMultipleForces = pp.get(0);
        log.info("PREF_WORK_GRP_SIZE_MUL = " + prefdWorkGroupSizeMultipleForces + " (forces)");

        clKernelMove = clCreateKernel(clProgram, "moveVertices", errcode_ret);
        CLInfo.checkCLError(errcode_ret);

        clKernelFlipStage1 = clCreateKernel(clProgram, "flipStage1", errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clKernelFlipStage2 = clCreateKernel(clProgram, "flipStage2", errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clKernelFlipStage3 = clCreateKernel(clProgram, "flipStage3", errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clKernelUnlockFaces = clCreateKernel(clProgram, "unlockFaces", errcode_ret);
        CLInfo.checkCLError(errcode_ret);

        clKernelLabelEdges = clCreateKernel(clProgram, "label", errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clKernelLabelEdgesUpdate = clCreateKernel(clProgram, "updateLabel", errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clKernelCheckTriangles = clCreateKernel(clProgram, "checkTriangles", errcode_ret);
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
        clVtoE = clCreateBuffer(clContext, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, vToE, errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clBorderVertices = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, borderV, errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clEdges = clCreateBuffer(clContext, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, e, errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clFaces = clCreateBuffer(clContext, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, t, errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clForces = clCreateBuffer(clContext, CL_MEM_READ_WRITE, factor * 2 * numberOfVertices, errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clLengths = clCreateBuffer(clContext, CL_MEM_READ_WRITE, factor * 2 * numberOfEdges, errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clqLengths = clCreateBuffer(clContext, CL_MEM_READ_WRITE, factor * 2 * numberOfEdges, errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clScalingFactor = clCreateBuffer(clContext, CL_MEM_READ_WRITE, factor, errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clEdgeLabels = clCreateBuffer(clContext, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, edgeLabels, errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clIllegalEdges = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4, errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clIllegalTriangles = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4, errcode_ret);
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

        clSetKernelArg1p(clKernelForces, 0, numberOfVertices);
        clSetKernelArg1p(clKernelForces, 1, clVertices);
        clSetKernelArg1p(clKernelForces, 2, clEdges);
        clSetKernelArg1p(clKernelForces, 3, clVtoE);
        clSetKernelArg1p(clKernelForces, 4, clLengths);
        clSetKernelArg1p(clKernelForces, 5, clScalingFactor);
        clSetKernelArg1p(clKernelForces, 6, clForces);


        clSetKernelArg1p(clKernelMove, 0, clVertices);
        clSetKernelArg1p(clKernelMove, 1, clBorderVertices);
        clSetKernelArg1p(clKernelMove, 2, clForces);
        if(doublePrecision) {
            clSetKernelArg1d(clKernelMove, 3, delta);
        }
        else {
            clSetKernelArg1f(clKernelMove, 3, fDelta);
        }

        clSetKernelArg1p(clKernelLabelEdges, 0, clVertices);
        clSetKernelArg1p(clKernelLabelEdges, 1, clEdges);
        clSetKernelArg1p(clKernelLabelEdges, 2, clEdgeLabels);
        clSetKernelArg1p(clKernelLabelEdges, 3, clIllegalEdges);

        clSetKernelArg1p(clKernelLabelEdgesUpdate, 0, clVertices);
        clSetKernelArg1p(clKernelLabelEdgesUpdate, 1, clEdges);
        clSetKernelArg1p(clKernelLabelEdgesUpdate, 2, clEdgeLabels);
        clSetKernelArg1p(clKernelLabelEdgesUpdate, 3, clIllegalEdges);

        clSetKernelArg1p(clKernelFlipStage1, 0, clEdges);
        clSetKernelArg1p(clKernelFlipStage1, 1, clEdgeLabels);
        clSetKernelArg1p(clKernelFlipStage1, 2, clFaces);

        clSetKernelArg1p(clKernelFlipStage2, 0, clEdges);
        clSetKernelArg1p(clKernelFlipStage2, 1, clEdgeLabels);
        clSetKernelArg1p(clKernelFlipStage2, 2, clFaces);

        clSetKernelArg1p(clKernelFlipStage3, 0, clVertices);
        clSetKernelArg1p(clKernelFlipStage3, 1, clVtoE);
        clSetKernelArg1p(clKernelFlipStage3, 2, clEdges);
        clSetKernelArg1p(clKernelFlipStage3, 3, clEdgeLabels);
        clSetKernelArg1p(clKernelFlipStage3, 4, clFaces);

        clSetKernelArg1p(clKernelUnlockFaces, 0, clFaces);

        clSetKernelArg1p(clKernelCheckTriangles, 0, clVertices);
        clSetKernelArg1p(clKernelCheckTriangles, 1, clEdges);
        clSetKernelArg1p(clKernelCheckTriangles, 2, clFaces);
        clSetKernelArg1p(clKernelCheckTriangles, 3, clIllegalTriangles);

        clGloblWorkSizeSFPartial = BufferUtils.createPointerBuffer(1);
        clLocalWorkSizeSFPartial = BufferUtils.createPointerBuffer(1);
        clGloblWorkSizeSFPartial.put(0, (int)(maxGroupSize * prefdWorkGroupSizeMultiple));
        clLocalWorkSizeSFPartial.put(0, (int)maxGroupSize);

        clGloblWorkSizeForces = BufferUtils.createPointerBuffer(1);
        clLocalWorkSizeForces = BufferUtils.createPointerBuffer(1);
        clGloblWorkSizeForces.put(0, (int)(maxGroupSize * prefdWorkGroupSizeMultipleForces));
        clLocalWorkSizeForces.put(0, (int)maxGroupSize);

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

        clEvent = BufferUtils.createPointerBuffer(1);
        startTime = BufferUtils.createByteBuffer(8);
        endTime = BufferUtils.createByteBuffer(8);
        retSize = BufferUtils.createPointerBuffer(1);

        retSize.put(0, 8);
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
         * 1. generate scaling factor
		 * 2. generate forces;
		 * 3. update vertices;
		 * 4. check for illegal triangles
		 * 5. flip all
         *
         */
        hasToRead = true;
        enqueueNDRangeKernel(clQueue, clKernelLengths, 1, null, clGlobalWorkSizeEdges, null, null, null);
        log.info("computed edge lengths");

        if(numberOfEdges > prefdWorkGroupSizeMultiple) {
            enqueueNDRangeKernel(clQueue, clKernelPartialSF, 1, null, clGloblWorkSizeSFPartial, clLocalWorkSizeSFPartial, null, null);
        }

        enqueueNDRangeKernel(clQueue, clKernelCompleteSF, 1, null, clGloblWorkSizeSFComplete, clLocalWorkSizeSFComplete, null, null);
        log.info("computed scale factor");

        // force to use only 1 work group => local size = local size
        enqueueNDRangeKernel(clQueue, clKernelForces, 1, null, clGloblWorkSizeForces, clLocalWorkSizeForces, null, null);
        log.info("computed forces");

        enqueueNDRangeKernel(clQueue, clKernelMove, 1, null, clGlobalWorkSizeVertices, null, null, null);
        log.info("move vertices");

        IntBuffer illegalTriangles = stack.mallocInt(1);
        illegalTriangles.put(0, 0);
        //clEnqueueWriteBuffer(clQueue, clIllegalTriangles, true, 0, illegalTriangles, null, null);
        //enqueueNDRangeKernel(clQueue, clKernelCheckTriangles, 1, null, clGlobalWorkSizeTriangles, null, null, null);
        //clFinish(clQueue);
        //clEnqueueReadBuffer(clQueue, clIllegalTriangles, true, 0, illegalTriangles, null, null);
        log.info("check for illegal triangles");
        if(illegalTriangles.get(0) == 1) {
            log.info("illegal triangle found!");
            //return true;
        }

        // flip as long as there are no more flips possible
        if(flipAll) {
            IntBuffer illegalEdges = stack.mallocInt(1);
            // while there is any illegal edge, do: // TODO: this is not the same as in the java distmesh!

            enqueueNDRangeKernel(clQueue, clKernelLabelEdges, 1, null, clGlobalWorkSizeEdges, null, null, null);
            log.info("label illegal edges");

            do  {
                illegalEdges.put(0, 0);
                clEnqueueWriteBuffer(clQueue, clIllegalEdges, true, 0, illegalEdges, null, null);

                enqueueNDRangeKernel(clQueue, clKernelFlipStage1, 1, null, clGlobalWorkSizeEdges, null, null, null);
                enqueueNDRangeKernel(clQueue, clKernelFlipStage2, 1, null, clGlobalWorkSizeEdges, null, null, null);
                enqueueNDRangeKernel(clQueue, clKernelFlipStage3, 1, null, clGlobalWorkSizeEdges, null, null, null);
                enqueueNDRangeKernel(clQueue, clKernelUnlockFaces, 1, null, clGlobalWorkSizeTriangles, null, null, null);
                log.info("flip some illegal edges");

                clEnqueueNDRangeKernel(clQueue, clKernelLabelEdgesUpdate, 1, null, clGlobalWorkSizeEdges, null, null, null);
                log.info("refresh old labels");
                clFinish(clQueue);

                //clEnqueueReadBuffer(clQueue, clTriLocks, true, 0, triLocks, null, null);
                //checkTriLocks();
                //clEnqueueReadBuffer(clQueue, clIllegalEdges, true, 0, illegalEdges, null, null);
                log.info("isLegal = " + illegalEdges.get(0));

            } while(illegalEdges.get(0) == 1);
            //log.info("flip all");
        }

       /* clEnqueueNDRangeKernel(clQueue, clKernelFlip, 1, null, clGlobalWorkSizeEdes, clLocalWorkSizeOne, null, null);
        clEnqueueNDRangeKernel(clQueue, clKernelRepair, 1, null, clGlobalWorkSizeEdes, null, null, null);

        clEnqueueNDRangeKernel(clQueue, clKernelFlip, 1, null, clGlobalWorkSizeEdes, clLocalWorkSizeOne, null, null);
        clEnqueueNDRangeKernel(clQueue, clKernelRepair, 1, null, clGlobalWorkSizeEdes, null, null, null);

        clEnqueueNDRangeKernel(clQueue, clKernelFlip, 1, null, clGlobalWorkSizeEdes, clLocalWorkSizeOne, null, null);
        clEnqueueNDRangeKernel(clQueue, clKernelRepair, 1, null, clGlobalWorkSizeEdes, null, null, null);
*/
        clFinish(clQueue);

        return false;
    }

    private long enqueueNDRangeKernel(long command_queue, long kernel, int work_dim, PointerBuffer global_work_offset, PointerBuffer global_work_size, PointerBuffer local_work_size, PointerBuffer event_wait_list, PointerBuffer event) {
        if(profiling) {
            long result = clEnqueueNDRangeKernel(command_queue, kernel, work_dim, global_work_offset, global_work_size, local_work_size, event_wait_list, clEvent);
            clWaitForEvents(clEvent);
            long eventAddr = clEvent.get();
            clGetEventProfilingInfo(eventAddr, CL_PROFILING_COMMAND_START, startTime, retSize);
            clGetEventProfilingInfo(eventAddr, CL_PROFILING_COMMAND_END, endTime, retSize);
            clEvent.clear();
            // in nanaSec
            System.out.println("event time " + "0x"+eventAddr + ": " + (endTime.getLong() - startTime.getLong()) + " ns");
            endTime.clear();
            startTime.clear();
            return result;
        }
        else {
            return clEnqueueNDRangeKernel(command_queue, kernel, work_dim, global_work_offset, global_work_size, local_work_size, event_wait_list, event);
        }
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
            log.info("scale factor = " + scalingFactorF.get(0));
        }

        clEnqueueReadBuffer(clQueue, clFaces, true, 0, t, null, null);
        clEnqueueReadBuffer(clQueue, clEdges, true, 0, e, null, null);
        clEnqueueReadBuffer(clQueue, clVtoE, true, 0, vToE, null, null);
    }

    private void readResultFromHost() {
        //AMesh<P> mesh = new AMesh<>(mesh.getPointConstructor());

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

        // scatter data
        mesh.setPositions(pointSet);
        CLGatherer.scatterFaces(mesh, t);
        CLGatherer.scatterHalfEdges(mesh, e);
        CLGatherer.scatterVertexToEdge(mesh, vToE);
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
        clReleaseMemObject(clBorderVertices);
        clReleaseMemObject(clEdges);
        clReleaseMemObject(clVtoE);
        clReleaseMemObject(clFaces);
        clReleaseMemObject(clLengths);
        clReleaseMemObject(clqLengths);
        clReleaseMemObject(clPartialSum);
        clReleaseMemObject(clScalingFactor);
        clReleaseMemObject(clEdgeLabels);
        clReleaseMemObject(clIllegalEdges);
        clReleaseMemObject(clIllegalTriangles);

        clReleaseKernel(clKernelForces);
        clReleaseKernel(clKernelMove);
        clReleaseKernel(clKernelLengths);
        clReleaseKernel(clKernelPartialSF);
        clReleaseKernel(clKernelCompleteSF);

        clReleaseKernel(clKernelFlipStage1);
        clReleaseKernel(clKernelFlipStage2);
        clReleaseKernel(clKernelFlipStage3);
        clReleaseKernel(clKernelUnlockFaces);

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

        MemoryUtil.memFree(borderV);
        MemoryUtil.memFree(e);
        MemoryUtil.memFree(t);
        MemoryUtil.memFree(vToE);
        MemoryUtil.memFree(edgeLabels);
    }

    public void init() {
        initCallbacks();
        initCL();
        buildProgram();
        createMemory();
        initialKernelArgs();
    }

    public void refresh () {
        if(hasToRead) {
            readResultFromGPU();
            readResultFromHost();
            hasToRead = false;
        }
        //printResult();
    }

    public void finish() {
        refresh();
        //updateMesh();
        clearCL();
        //clearHost();
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

        CLDistMeshHE clDistMesh = new CLDistMeshHE(mesh);
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
