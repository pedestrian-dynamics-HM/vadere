package org.vadere.meshing.opencl;

import org.apache.commons.lang3.time.StopWatch;

import org.lwjgl.system.Configuration;
import org.vadere.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.vadere.meshing.mesh.iterators.EdgeIterator;
import org.vadere.meshing.mesh.gen.AFace;
import org.vadere.meshing.mesh.gen.AHalfEdge;
import org.vadere.meshing.mesh.gen.AMesh;
import org.vadere.meshing.mesh.gen.AVertex;
import org.vadere.meshing.mesh.gen.CLGatherer;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.opencl.CLInfo;
import org.vadere.util.opencl.CLOperation;
import org.vadere.util.opencl.CLUtils;
import org.vadere.util.opencl.OpenCLException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.stream.Collectors;

import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.opencl.CL11.CL_KERNEL_PREFERRED_WORK_GROUP_SIZE_MULTIPLE;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Benedikt Zoennchen
 *
 * DistMesh GPU implementation.
 */
public class CLDistMesh extends CLOperation {

    private static Logger log = Logger.getLogger(CLDistMesh.class);

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

    //private long clKernelRemoveTriangles;
    //private long clKernelCheckTriangles;


    // data on the host
    private DoubleBuffer vD;
    private FloatBuffer vF;
    private IntBuffer e;
    private IntBuffer t;
    private IntBuffer twins;
    private IntBuffer boundaryVertices;
    private IntBuffer triLocks;
    private IntBuffer edgeLabels;
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
    private long clIsBoundaryVertex;
    private long clRelation;
    private long clEdgeLabels;
    private long clTriLocks;
    private long clIllegalEdges;
    private long clIllegalTriangles;

    // size
    private int n;
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

    private ByteBuffer source;

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

    private AMesh mesh;

    private boolean doublePrecision = true;

    private List<IPoint> result;
    private boolean hasToRead = false;

    public CLDistMesh(@NotNull final AMesh mesh) {
    	super(CL_DEVICE_TYPE_GPU);
    	profiling = true;
        if(profiling) {
            Configuration.DEBUG.set(true);
            Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);
            Configuration.DEBUG_STACK.set(true);
            Configuration.DEBUG_STACK.set(true);
        }

        this.mesh = mesh;
        this.mesh.garbageCollection();
        if(doublePrecision) {
            this.vD = CLGatherer.getVerticesD(mesh);
        }
        else {
            this.vF = CLGatherer.getVerticesF(mesh);
        }
        this.e = CLGatherer.getEdges(mesh);
        this.t = CLGatherer.getTriangles(mesh);
        this.twins = CLGatherer.getTwins(mesh);
        this.numberOfVertices = mesh.getNumberOfVertices();
        this.numberOfEdges = mesh.getNumberOfEdges();
        this.numberOfFaces = mesh.getNumberOfFaces();
        this.boundaryVertices =  MemoryUtil.memAllocInt(numberOfVertices);
        this.triLocks = MemoryUtil.memAllocInt(numberOfFaces);
        for(int i = 0; i < numberOfFaces; i++) {
            this.triLocks.put(i, -1);
        }

        int j = 0;
        for(AVertex vertex : mesh.getVertices()) {
            int isBoundary = mesh.isAtBoundary(vertex) ? 1 : 0;
            this.boundaryVertices.put(vertex.getId(), isBoundary);
            assert j == vertex.getId();
            j++;
        }

        this.edgeLabels = MemoryUtil.memAllocInt(numberOfEdges);
        for(int i = 0; i < numberOfEdges; i++) {
            this.edgeLabels.put(i, 0);
        }
        this.result = mesh.streamPoints().collect(Collectors.toList());
    }

    private void buildProgram() throws OpenCLException {
        try (MemoryStack stack = stackPush()) {
            // helper for the memory allocation in java
            //stack = MemoryStack.stackPush();
            IntBuffer errcode_ret = stack.mallocInt(1);

	        PointerBuffer pp = stack.mallocPointer(1);
	        clGetDeviceInfo(clDevice, CL_DEVICE_MAX_WORK_GROUP_SIZE, pp, null);
	        maxGroupSize = pp.get(0);
	        clGetDeviceInfo(clDevice, CL_DEVICE_MAX_COMPUTE_UNITS, pp, null);
	        maxComputeUnits = pp.get(0);
	        log.info("MAX_GRP_SIZE = " + maxGroupSize);
	        log.info("MAX_COMPUTE_UNITS = " + maxComputeUnits);
	        PointerBuffer clProgramStrings = stack.mallocPointer(1);
	        PointerBuffer clProgramLengths = stack.mallocPointer(1);

            try {
                if (doublePrecision) {
                    source = CLUtils.ioResourceToByteBuffer("DistMeshDouble.cl", 4096);
                } else {
                    source = CLUtils.ioResourceToByteBuffer("DistMesh.cl", 4096);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            clProgramStrings.put(0, source);
            clProgramLengths.put(0, source.remaining());

            clProgram = clCreateProgramWithSource(clContext, clProgramStrings, clProgramLengths, errcode_ret);

            int errcode = clBuildProgram(clProgram, clDevice, "", programCB, NULL);
            CLInfo.checkCLError(errcode);

            clKernelLengths = clCreateKernel(clProgram, "computeLengths", errcode_ret);
            CLInfo.checkCLError(errcode_ret);

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
            /*clKernelFlip = clCreateKernel(clProgram, "flip", errcode_ret);
            CLInfo.checkCLError(errcode_ret);*/

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
            /*clKernelCheckTriangles = clCreateKernel(clProgram, "checkTriangles", errcode_ret);
            CLInfo.checkCLError(errcode_ret);

            clKernelRemoveTriangles = clCreateKernel(clProgram, "removeTriangles", errcode_ret);
            CLInfo.checkCLError(errcode_ret);*/
        }
    }

    private void createMemory() throws OpenCLException {
        try (MemoryStack stack = stackPush()) {
            IntBuffer errcode_ret = stack.mallocInt(1);

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
            clIsBoundaryVertex = clCreateBuffer(clContext, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, boundaryVertices, errcode_ret);
            CLInfo.checkCLError(errcode_ret);
            clTriLocks = clCreateBuffer(clContext, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, triLocks, errcode_ret);
            CLInfo.checkCLError(errcode_ret);
            clRelation = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * numberOfFaces, errcode_ret);
            CLInfo.checkCLError(errcode_ret);
            clEdgeLabels = clCreateBuffer(clContext, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, edgeLabels, errcode_ret);
            CLInfo.checkCLError(errcode_ret);
            clIllegalEdges = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4, errcode_ret);
            CLInfo.checkCLError(errcode_ret);
            clIllegalTriangles = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4, errcode_ret);
            CLInfo.checkCLError(errcode_ret);
            clTwins = clCreateBuffer(clContext, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, twins, errcode_ret);
            CLInfo.checkCLError(errcode_ret);
        }

    }

    private void initialKernelArgs() throws OpenCLException {
        try (MemoryStack stack = stackPush()) {
            IntBuffer errcode_ret = stack.mallocInt(1);
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

            clSetKernelArg1i(clKernelForces, 0, numberOfEdges);
            clSetKernelArg1p(clKernelForces, 1, clVertices);
            clSetKernelArg1p(clKernelForces, 2, clEdges);
            clSetKernelArg1p(clKernelForces, 3, clLengths);
            clSetKernelArg1p(clKernelForces, 4, clScalingFactor);
            clSetKernelArg1p(clKernelForces, 5, clForces);
            clSetKernelArg1p(clKernelForces, 6, clIsBoundaryVertex);

            clSetKernelArg1p(clKernelMove, 0, clVertices);
            clSetKernelArg1p(clKernelMove, 1, clForces);
            clSetKernelArg1p(clKernelMove, 2, clIsBoundaryVertex);
            if(doublePrecision) {
                clSetKernelArg1d(clKernelMove, 3, delta);
            }
            else {
                clSetKernelArg1f(clKernelMove, 3, fDelta);
            }

            clSetKernelArg1p(clKernelLabelEdges, 0, clVertices);
            clSetKernelArg1p(clKernelLabelEdges, 1, clEdges);
            clSetKernelArg1p(clKernelLabelEdges, 2, clTriangles);
            clSetKernelArg1p(clKernelLabelEdges, 3, clEdgeLabels);
            clSetKernelArg1p(clKernelLabelEdges, 4, clIllegalEdges);

            clSetKernelArg1p(clKernelLabelEdgesUpdate, 0, clVertices);
            clSetKernelArg1p(clKernelLabelEdgesUpdate, 1, clEdges);
            clSetKernelArg1p(clKernelLabelEdgesUpdate, 2, clTriangles);
            clSetKernelArg1p(clKernelLabelEdgesUpdate, 3, clEdgeLabels);
            clSetKernelArg1p(clKernelLabelEdgesUpdate, 4, clIllegalEdges);
            clSetKernelArg1p(clKernelLabelEdgesUpdate, 5, clTriLocks);

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

            /*clSetKernelArg1p(clKernelRemoveTriangles, 0, clVertices);
            clSetKernelArg1p(clKernelRemoveTriangles, 1, clEdges);
            clSetKernelArg1p(clKernelRemoveTriangles, 2, clTriangles);

            clSetKernelArg1p(clKernelCheckTriangles, 0, clVertices);
            clSetKernelArg1p(clKernelCheckTriangles, 1, clTriangles);
            clSetKernelArg1p(clKernelCheckTriangles, 2, clIllegalTriangles);*/

            clGloblWorkSizeSFPartial = MemoryUtil.memAllocPointer(1);
            clLocalWorkSizeSFPartial = MemoryUtil.memAllocPointer(1);
            clGloblWorkSizeSFPartial.put(0, (int)(maxGroupSize * prefdWorkGroupSizeMultiple));
            clLocalWorkSizeSFPartial.put(0, (int)maxGroupSize);

            clGloblWorkSizeForces = MemoryUtil.memAllocPointer(1);
            clLocalWorkSizeForces = MemoryUtil.memAllocPointer(1);
            clGloblWorkSizeForces.put(0, (int)(maxGroupSize * prefdWorkGroupSizeMultipleForces));
            clLocalWorkSizeForces.put(0, (int)maxGroupSize);

            clGloblWorkSizeSFComplete = MemoryUtil.memAllocPointer(1);
            clLocalWorkSizeSFComplete = MemoryUtil.memAllocPointer(1);
            clLocalWorkSizeOne = MemoryUtil.memAllocPointer(1);

            clGloblWorkSizeSFComplete.put(0, ceilPowerOf2(sizeSFComplete));
            clLocalWorkSizeSFComplete.put(0, ceilPowerOf2(sizeSFComplete));
            clLocalWorkSizeOne.put(0, 1);
            clGlobalWorkSizeEdges = MemoryUtil.memAllocPointer(1);
            clGlobalWorkSizeVertices = MemoryUtil.memAllocPointer(1);
            clGlobalWorkSizeTriangles = MemoryUtil.memAllocPointer(1);
            clGlobalWorkSizeEdges.put(0, numberOfEdges);
            clGlobalWorkSizeVertices.put(0, numberOfVertices);
            clGlobalWorkSizeTriangles.put(0, numberOfFaces);

            clEvent = MemoryUtil.memAllocPointer(1);
            startTime = MemoryUtil.memAlloc(8);
            endTime = MemoryUtil.memAlloc(8);
            retSize = MemoryUtil.memAllocPointer(1);

            retSize.put(0, 8);
        }
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

    public boolean step() throws OpenCLException {
        return step(true);
    }

    // TODO: think about the use of only 1 work-group!!! It might be bad! solution: use global barrier? force computation? flip hangs after some time?
    public boolean step(final boolean flipAll) throws OpenCLException {
        try (MemoryStack stack = stackPush()) {
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
            enqueueNDRangeKernel("edge len computation", clQueue, clKernelLengths, 1, null, clGlobalWorkSizeEdges, null, null, null);
            //log.info("computed edge lengths");

            if(numberOfEdges > prefdWorkGroupSizeMultiple) {
                enqueueNDRangeKernel("partial sum scaling factor", clQueue, clKernelPartialSF, 1, null, clGloblWorkSizeSFPartial, clLocalWorkSizeSFPartial, null, null);
            }

            enqueueNDRangeKernel("sum scaling factor", clQueue, clKernelCompleteSF, 1, null, clGloblWorkSizeSFComplete, clLocalWorkSizeSFComplete, null, null);
            //log.info("computed scale factor");

            // force to use only 1 work group => local size = local size
            enqueueNDRangeKernel("compute forces", clQueue, clKernelForces, 1, null, clGloblWorkSizeForces, clLocalWorkSizeForces, null, null);
            //log.info("(default) computed forces");

            //enqueueNDRangeKernel("compute forces", clQueue, clKernelForces, 1, null, clGlobalWorkSizeEdges, null, null, null);
            //log.info("(default) computed forces");

            enqueueNDRangeKernel("move vertices", clQueue, clKernelMove, 1, null, clGlobalWorkSizeVertices, null, null, null);
            //log.info("move vertices");

            //enqueueNDRangeKernel(clQueue, clKernelRemoveTriangles, 1, null, clGlobalWorkSizeEdges, null, null, null);
            //log.info("remove low quality triangles");

            /*IntBuffer illegalTriangles = stack.mallocInt(1);
            illegalTriangles.put(0, 0);
            clEnqueueWriteBuffer(clQueue, clIllegalTriangles, true, 0, illegalTriangles, null, null);
            enqueueNDRangeKernel(clQueue, clKernelCheckTriangles, 1, null, clGlobalWorkSizeTriangles, null, null, null);
            clFinish(clQueue);

           clEnqueueReadBuffer(clQueue, clIllegalTriangles, true, 0, illegalTriangles, null, null);
            log.info("check for illegal triangles");
            if(illegalTriangles.get(0) == 1) {
                log.info("illegal triangle found!");
                //return true;
            }*/

            // flip as long as there are no more flips possible
            if(flipAll) {
                IntBuffer illegalEdges = stack.mallocInt(1);
                // while there is any illegal edge, do: // TODO: this is not the same as in the java distmesh!

                enqueueNDRangeKernel("label edges",clQueue, clKernelLabelEdges, 1, null, clGlobalWorkSizeEdges, null, null, null);
                //log.info("label illegal edges");

                do  {
                    illegalEdges.put(0, 0);
                    clEnqueueWriteBuffer(clQueue, clIllegalEdges, true, 0, illegalEdges, null, null);

                    enqueueNDRangeKernel("flip 1", clQueue, clKernelFlipStage1, 1, null, clGlobalWorkSizeEdges, null, null, null);
                    enqueueNDRangeKernel("flip 2", clQueue, clKernelFlipStage2, 1, null, clGlobalWorkSizeEdges, null, null, null);
                    enqueueNDRangeKernel("flip 3", clQueue, clKernelFlipStage3, 1, null, clGlobalWorkSizeEdges, null, null, null);
                    //log.info("flip some illegal edges");

                    enqueueNDRangeKernel("repair ds", clQueue, clKernelRepair, 1, null, clGlobalWorkSizeEdges, null, null, null);
                    //log.info("repair data structure");

                    enqueueNDRangeKernel("re-label", clQueue, clKernelLabelEdgesUpdate, 1, null, clGlobalWorkSizeEdges, null, null, null);
                    //log.info("refresh old labels");
                    clFinish(clQueue);

                    //clEnqueueReadBuffer(clQueue, clTriLocks, true, 0, triLocks, null, null);
                    //checkTriLocks();
                    clEnqueueReadBuffer(clQueue, clIllegalEdges, true, 0, illegalEdges, null, null);
                    //log.info("isLegal = " + illegalEdges.get(0));

                } while(illegalEdges.get(0) == 1  && false);
                //log.info("flip all");
            }

            clFinish(clQueue);

            return false;
        }
    }

    private void checkTriLocks() {
        for(int i = 0; i < numberOfFaces; i++) {
            int lock = triLocks.get(i);

            for(int j = i+1; j < numberOfFaces; j++) {
                int lock2 = triLocks.get(j);

                for(int h = j+1; h < numberOfFaces; h++) {
                    int lock3 = triLocks.get(h);
                    if(lock != -1 && lock == lock2 && lock2 == lock3) {
                        throw new IllegalArgumentException(lock3 + ": the lock is wrong!");
                    }
                }
            }
        }
    }

    private void readResultFromGPU() {
        try (MemoryStack stack = stackPush()) {
            if(doublePrecision) {
                DoubleBuffer scalingFactorD = stack.mallocDouble(1);
                clEnqueueReadBuffer(clQueue, clScalingFactor, true, 0, scalingFactorD, null, null);
                clEnqueueReadBuffer(clQueue, clVertices, true, 0, vD, null, null);
                log.info("scale factor = " + scalingFactorD.get(0));
            }
            else {
                FloatBuffer scalingFactorF = stack.mallocFloat(1);
                clEnqueueReadBuffer(clQueue, clScalingFactor, true, 0, scalingFactorF, null, null);
                clEnqueueReadBuffer(clQueue, clVertices, true, 0, vF, null, null);
                log.info("scale factor = " + scalingFactorF.get(0));
            }

            clEnqueueReadBuffer(clQueue, clTriangles, true, 0, t, null, null);
            clEnqueueReadBuffer(clQueue, clEdges, true, 0, e, null, null);
        }
    }

    private void readResultFromHost() {
        //AMesh<P> mesh = new AMesh<>(mesh.getPointConstructor());

        List<IPoint> pointSet = new ArrayList<>(numberOfVertices);
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

        mesh.setPositions(pointSet);
        List<AHalfEdge> edges = mesh.getEdges();
        List<AVertex> vertices = mesh.getVertices();
        List<AFace> faces = mesh.getFaces();

        Map<Integer, LinkedList<AHalfEdge>> triangles = new HashMap<>();
        Set<AHalfEdge> toRemoveEdges = new HashSet<>();

        for(int i = 0; i < numberOfFaces; i++) {
            triangles.put(i, new LinkedList<>());
        }

        for(int edgeId = 0; edgeId < numberOfEdges; edgeId++) {
            int prefVertexId = e.get(edgeId * 4);
            int nextVertexId = e.get(edgeId * 4 + 1);
            int ta = e.get(edgeId * 4 +2);
            int tb = e.get(edgeId * 4 +3);

            //log.info(nextVertexId + "," + prefVertexId + "," + ta + "," + tb);
            // if the edge is not destroyed
            if(prefVertexId == -1 || nextVertexId == -1) {
                log.info(nextVertexId + "," + prefVertexId + "," + ta + "," + tb);
            }
            if(prefVertexId != -1) {
                //log.info("nextId: " + nextId);
                mesh.setVertex(edges.get(edgeId), vertices.get(nextVertexId));
                mesh.setEdge(vertices.get(nextVertexId), edges.get(edgeId));

                if(ta != -1) {
                    mesh.setFace(edges.get(edgeId), faces.get(ta));
                    mesh.setEdge(faces.get(ta), edges.get(edgeId));
                    LinkedList<AHalfEdge> tri = triangles.get(ta);
                    if(tri.isEmpty()) {
                        tri.addLast(edges.get(edgeId));
                    }
                    else {
                        if(mesh.getPoint(tri.peekLast()).equals(mesh.getPoint(vertices.get(prefVertexId)))) {
                            tri.addLast(edges.get(edgeId));
                        }
                        else {
                            tri.addFirst(edges.get(edgeId));
                        }
                    }
                }
                else {
                    assert mesh.isBoundary(edges.get(edgeId));
                }
            }
           /* else {
                toRemoveEdges.add(edges.get(edgeId));
            }*/
        }

       /* for(AHalfEdge<P> rEdge : toRemoveEdges) {
            if(!mesh.isDestroyed(rEdge)) {
                assert mesh.isAtBoundary(mesh.getTwin(rEdge));
                AFace<P> face = mesh.getFace(rEdge);

                if(!mesh.isBoundary(face) && !mesh.isDestroyed(face)) {
                    removeFaceAtBoundary(face);
                }
            }
        }*/


        for(int i = 0; i < numberOfFaces; i++) {
            List<AHalfEdge> tri = triangles.get(i);
            // face still exist
            assert tri.size() == 3;
            mesh.setNext(tri.get(0), tri.get(1));
            mesh.setNext(tri.get(1), tri.get(2));
            mesh.setNext(tri.get(2), tri.get(0));
        }
    }


    private void fixBorderFace(@NotNull final AFace borderFace, @NotNull final Set<AHalfEdge> toRemoveEdges) {
        // 1. get edge which is not destroyed

        AHalfEdge startEdge = mesh.getEdge(borderFace);
        while (toRemoveEdges.contains(startEdge)) {
            startEdge = mesh.getNext(startEdge);
        }

        AHalfEdge edge = startEdge;
        do {
            if(toRemoveEdges.contains(edge)) {
                AFace twinFace = mesh.getTwinFace(edge);
                if(mesh.isDestroyed(twinFace)) {
                    removeFaceAtBorder(twinFace);
                }
            }
            edge = mesh.getNext(edge);
        } while (edge != startEdge);

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

        //log.info("scalingFactor:" + (doublePrecision ? scalingFactorD.get(0) : scalingFactorF.get(0)));
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

    @Override
    protected void clearCL() throws OpenCLException {
        clReleaseMemObject(clVertices);
        clReleaseMemObject(clEdges);
        clReleaseMemObject(clTwins);
        clReleaseMemObject(clTriangles);
        clReleaseMemObject(clForces);
        clReleaseMemObject(clLengths);
        clReleaseMemObject(clqLengths);
        clReleaseMemObject(clPartialSum);
        clReleaseMemObject(clScalingFactor);
        clReleaseMemObject(clIsBoundaryVertex);
        clReleaseMemObject(clRelation);
        clReleaseMemObject(clEdgeLabels);
        clReleaseMemObject(clTriLocks);
        clReleaseMemObject(clIllegalEdges);

        clReleaseKernel(clKernelForces);
        clReleaseKernel(clKernelMove);
        clReleaseKernel(clKernelLengths);
        clReleaseKernel(clKernelPartialSF);
        clReleaseKernel(clKernelCompleteSF);
        clReleaseKernel(clKernelFlipStage1);
        clReleaseKernel(clKernelFlipStage2);
        clReleaseKernel(clKernelFlipStage3);
        clReleaseKernel(clKernelRepair);
        clReleaseKernel(clKernelLabelEdges);
        clReleaseKernel(clKernelLabelEdgesUpdate);
	    super.clearCL();
    }

    private void clearHost() {
        if(doublePrecision) {
            MemoryUtil.memFree(vD);
        }
        else {
            MemoryUtil.memFree(vF);
        }

        MemoryUtil.memFree(e);
        MemoryUtil.memFree(t);
        MemoryUtil.memFree(boundaryVertices);
        MemoryUtil.memFree(triLocks);
        MemoryUtil.memFree(edgeLabels);
        MemoryUtil.memFree(twins);

        MemoryUtil.memFree(clGlobalWorkSizeEdges);
        MemoryUtil.memFree(clGlobalWorkSizeVertices);

        MemoryUtil.memFree(clGloblWorkSizeSFPartial);
        MemoryUtil.memFree(clLocalWorkSizeSFPartial);

        MemoryUtil.memFree(clGloblWorkSizeSFComplete);
        MemoryUtil.memFree(clLocalWorkSizeSFComplete);

        MemoryUtil.memFree(clGloblWorkSizeForces);
        MemoryUtil.memFree(clLocalWorkSizeForces);

        MemoryUtil.memFree(clGlobalWorkSizeTriangles);

        MemoryUtil.memFree(clLocalWorkSizeOne);
        MemoryUtil.memFree(clEvent);
        MemoryUtil.memFree(startTime);
        MemoryUtil.memFree(endTime);
        MemoryUtil.memFree(retSize);
        MemoryUtil.memFree(source);
    }

    public void init() throws OpenCLException {
        StopWatch watch = new StopWatch();
        initCallbacks();
        initCL();
        buildProgram();
        watch.start();
        createMemory();
        initialKernelArgs();
        watch.stop();
        log.info("initCL time:" + watch.getTime() + "[ms]");
    }

    public void refresh () {
        if(hasToRead) {
            readResultFromGPU();
            readResultFromHost();
            hasToRead = false;
        }
        //printResult();
    }

    public void finish() throws OpenCLException {
        refresh();
        clearHost();
        clearCL();
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
    public static void main(String... args) throws OpenCLException {
        AMesh mesh = AMesh.createSimpleTriMesh();
        log.info("before");
        Collection<AVertex> vertices = mesh.getVertices();
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

    private static void printPlatformInfo(long platform, String param_name, int param) throws OpenCLException {
        System.out.println("\t" + param_name + " = " + CLInfo.getPlatformInfoStringUTF8(platform, param));
    }

    private static void printDeviceInfo(long device, String param_name, int param) throws OpenCLException {
        System.out.println("\t" + param_name + " = " + CLInfo.getDeviceInfoStringUTF8(device, param));
    }


    private AMesh getMesh() {
        return mesh;
    }

    private void removeFaceAtBorder(@NotNull final AFace face) {
        if(!getMesh().isDestroyed(face)) {
            List<AHalfEdge> delEdges = new ArrayList<>();
            List<AVertex> vertices = new ArrayList<>();

            // we only need the boundary if the face isNeighbourBorder
            AFace boundary = getMesh().getBorder();

            int count = 0;
            for(AHalfEdge edge : getMesh().getEdgeIt(face)) {
                AFace twinFace = getMesh().getTwinFace(edge);
                count++;
                if(twinFace.equals(boundary)) {
                    delEdges.add(edge);
                }
                else {
                    // update the edge of the boundary since it might be deleted!
                    getMesh().setEdge(boundary, edge);
                    getMesh().setFace(edge, boundary);
                }

                vertices.add(getMesh().getVertex(edge));
            }


            //TODO: this might be computational expensive!
            // special case: all edges will be deleted => adjust the border edge
            AHalfEdge borderEdge = null;
            if(getMesh().getTwinFace(getMesh().getEdge(boundary)) == face && delEdges.size() == count) {

                // all edges are border edges!
                borderEdge = getMesh().getTwin(getMesh().getEdge(face));
                EdgeIterator<AVertex, AHalfEdge, AFace> edgeIterator = new EdgeIterator<>(getMesh(), borderEdge);

                // walk along the border away from this faces to get another edge which won't be deleted
                AFace twinFace = getMesh().getTwinFace(borderEdge);
                while (edgeIterator.hasNext() && twinFace == face) {
                    borderEdge = edgeIterator.next();
                    twinFace = getMesh().getTwinFace(borderEdge);
                }

                if(getMesh().getTwinFace(borderEdge) == face) {
                    borderEdge = getMesh().streamEdges().filter(e -> getMesh().getTwinFace(e) != face).filter(e -> getMesh().isBoundary(e)).findAny().get();
                    //throw new IllegalArgumentException("could not adjust border edge! Deletion of " + face + " is not allowed.");
                }

                getMesh().setFace(borderEdge, boundary);
                getMesh().setEdge(boundary, borderEdge);
            }

            if(!delEdges.isEmpty()) {
                AHalfEdge h0, h1, next0, next1, prev0, prev1;
                AVertex v0, v1;

                for(AHalfEdge delEdge : delEdges) {
                    h0 = delEdge;
                    v0 = getMesh().getVertex(delEdge);
                    next0 = getMesh().getNext(h0);
                    prev0 = getMesh().getPrev(h0);

                    h1    = getMesh().getTwin(delEdge);
                    v1    = getMesh().getVertex(h1);
                    next1 = getMesh().getNext(h1);
                    prev1 = getMesh().getPrev(h1);

                    //getMesh().setEdge(hole, prev1);

                    // adjust next and prev half-edges
                    getMesh().setNext(prev0, next1);
                    getMesh().setNext(prev1, next0);

                    //boolean isolated0 = getMesh().getNext(prev1).equals(getMesh().getTwin(prev1));
                    //boolean isolated1 = getMesh().getNext(prev0).equals(getMesh().getTwin(prev0));

                    //boolean isolated0 = getMesh().getTwin(h0) == getMesh().getNext(h0) || getMesh().getTwin(h0) == getMesh().getPrev(h0);
                    //boolean isolated1 = getMesh().getTwin(h1) == getMesh().getNext(h1) || getMesh().getTwin(h1) == getMesh().getPrev(h1);

                    // adjust vertices
                    if(getMesh().getEdge(v0) == h0) {
                        getMesh().setEdge(v0, prev1);
                    }

                    if(getMesh().getEdge(v1) == h1) {
                        getMesh().setEdge(v1, prev0);
                    }


                    // mark edge deleted if the mesh has a edge status
                    getMesh().destroyEdge(h0);
                    getMesh().destroyEdge(h1);
                }
            }
            if(count > 0) {
                getMesh().destroyFace(face);
            }
            else {
                log.warn("could not delete face " + face + ". It is not at the border!");
            }
        }
    }
}
