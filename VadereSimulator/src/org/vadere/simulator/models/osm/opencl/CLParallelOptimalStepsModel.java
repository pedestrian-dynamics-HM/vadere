package org.vadere.simulator.models.osm.opencl;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CLContextCallback;
import org.lwjgl.opencl.CLProgramCallback;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.models.AttributesOSM;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.logging.Logger;
import org.vadere.util.opencl.CLInfo;
import org.vadere.util.opencl.CLUtils;
import org.vadere.util.opencl.OpenCLException;
import org.vadere.simulator.models.potential.solver.calculators.EikonalSolver;
import org.vadere.util.opencl.examples.InfoUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.lwjgl.opencl.CL10.CL_CONTEXT_PLATFORM;
import static org.lwjgl.opencl.CL10.CL_DEVICE_LOCAL_MEM_SIZE;
import static org.lwjgl.opencl.CL10.CL_DEVICE_MAX_WORK_GROUP_SIZE;
import static org.lwjgl.opencl.CL10.CL_DEVICE_NAME;
import static org.lwjgl.opencl.CL10.CL_DEVICE_TYPE_GPU;
import static org.lwjgl.opencl.CL10.CL_KERNEL_LOCAL_MEM_SIZE;
import static org.lwjgl.opencl.CL10.CL_KERNEL_WORK_GROUP_SIZE;
import static org.lwjgl.opencl.CL10.CL_MEM_ALLOC_HOST_PTR;
import static org.lwjgl.opencl.CL10.CL_MEM_COPY_HOST_PTR;
import static org.lwjgl.opencl.CL10.CL_MEM_READ_ONLY;
import static org.lwjgl.opencl.CL10.CL_MEM_READ_WRITE;
import static org.lwjgl.opencl.CL10.CL_PROFILING_COMMAND_END;
import static org.lwjgl.opencl.CL10.CL_PROFILING_COMMAND_START;
import static org.lwjgl.opencl.CL10.CL_PROGRAM_BUILD_LOG;
import static org.lwjgl.opencl.CL10.CL_PROGRAM_BUILD_STATUS;
import static org.lwjgl.opencl.CL10.CL_QUEUE_PROFILING_ENABLE;
import static org.lwjgl.opencl.CL10.CL_SUCCESS;
import static org.lwjgl.opencl.CL10.clBuildProgram;
import static org.lwjgl.opencl.CL10.clCreateBuffer;
import static org.lwjgl.opencl.CL10.clCreateCommandQueue;
import static org.lwjgl.opencl.CL10.clCreateContext;
import static org.lwjgl.opencl.CL10.clCreateKernel;
import static org.lwjgl.opencl.CL10.clCreateProgramWithSource;
import static org.lwjgl.opencl.CL10.clEnqueueNDRangeKernel;
import static org.lwjgl.opencl.CL10.clEnqueueReadBuffer;
import static org.lwjgl.opencl.CL10.clEnqueueWriteBuffer;
import static org.lwjgl.opencl.CL10.clFinish;
import static org.lwjgl.opencl.CL10.clGetDeviceIDs;
import static org.lwjgl.opencl.CL10.clGetEventProfilingInfo;
import static org.lwjgl.opencl.CL10.clGetKernelWorkGroupInfo;
import static org.lwjgl.opencl.CL10.clGetPlatformIDs;
import static org.lwjgl.opencl.CL10.clGetProgramBuildInfo;
import static org.lwjgl.opencl.CL10.clReleaseCommandQueue;
import static org.lwjgl.opencl.CL10.clReleaseContext;
import static org.lwjgl.opencl.CL10.clReleaseKernel;
import static org.lwjgl.opencl.CL10.clReleaseMemObject;
import static org.lwjgl.opencl.CL10.clReleaseProgram;
import static org.lwjgl.opencl.CL10.clSetKernelArg;
import static org.lwjgl.opencl.CL10.clSetKernelArg1f;
import static org.lwjgl.opencl.CL10.clSetKernelArg1i;
import static org.lwjgl.opencl.CL10.clSetKernelArg1p;
import static org.lwjgl.opencl.CL10.clWaitForEvents;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memUTF8;

/**
 * @author Benedikt Zoennchen
 *
 * This class offers the methods to compute an array based linked-cell which contains 2D-coordinates i.e. {@link VPoint}
 * using the GPU (see. green-2007 Building the Grid using Sorting).
 */
public class CLParallelOptimalStepsModel {
    private static Logger log = Logger.getLogger(CLParallelOptimalStepsModel.class);

    //
	private static final int COORDOFFSET = 2;
	private static final int OFFSET = 7;
	private static final int X = 0;
	private static final int Y = 1;
	private static final int STEPSIZE = 2;
	private static final int DESIREDSPEED = 3;
	private static final int TIMECREDIT = 4;
	private static final int NEWX = 5;
	private static final int NEWY = 6;

    // CL ids
    private long clPlatform;
    private long clDevice;
    private long clContext;
    private long clQueue;
    private long clProgram;

    // CL Memory
    private long clHashes;
    private long clIndices;
    private long clCellStarts;
    private long clCellEnds;
    private long clReorderedPedestrians;
    private long clPedestrians;
    private long clCellSize;
    private long clWorldOrigin;
    private long clGridSize;
    private long clTargetPotential;
    private long clObstaclePotential;
    private long clPedestrianNextPositions;
    private long clCirclePositions;
    private long clPotentialFieldSize;
    private long clPotentialFieldGridSize;

    // Host Memory
    private FloatBuffer memWorldOrigin;
    private FloatBuffer memCellSize;
    private FloatBuffer memTargetPotentialField;
    private FloatBuffer memObstaclePotentialField;
    private FloatBuffer memCirclePositions;
    private FloatBuffer memPotentialFieldSize;
    private IntBuffer memGridSize;
    private IntBuffer memPotentialFieldGridSize;

    // Host Memory to write update to the host
    private FloatBuffer memNextPositions;
    private IntBuffer memIndices;

    // CL callbacks
    private CLContextCallback contextCB;
    private CLProgramCallback programCB;

    // CL kernel
    private long clBitonicSortLocal;
	private long clBitonicSortLocal1;
	private long clBitonicMergeGlobal;
    private long clBitonicMergeLocal;
    private long clCalcHash;
    private long clFindCellBoundsAndReorder;
    private long clSeek;
    private long clMove;

    private int numberOfGridCells;
    private VRectangle bound;
    private float iCellSize;
    private int[] iGridSize;
    private List<VPoint> circlePositionList;
	private final int deviceType;

	private final AttributesFloorField attributesFloorField;
	private final AttributesOSM attributesOSM;

    private static final Logger logger = Logger.getLogger(CLParallelOptimalStepsModel.class);

    static {
    	logger.setDebug();
    }

	private long max_work_group_size;
	private long max_local_memory_size;

	// time measurement
	private boolean debug = true;
	private boolean profiling = true;
	private boolean pedestrianSet = false;

    private int numberOfSortElements;

    private int counter = 0;
    private float timeStepInSec = 0.4f;
    private int numberOfElements = 0;
    private final EikonalSolver targetPotential;
    private final EikonalSolver obstaclePotential;

	public CLParallelOptimalStepsModel(
			@NotNull final AttributesOSM attributesOSM,
			@NotNull final AttributesFloorField attributesFloorField,
			@NotNull final VRectangle bound,
			@NotNull final EikonalSolver targetPotential,
			@NotNull final EikonalSolver obstaclePotential,
			final double cellSize) throws OpenCLException {
		this(attributesOSM, attributesFloorField, bound, targetPotential, obstaclePotential, CL_DEVICE_TYPE_GPU, cellSize);
	}

	/**
	 * Default constructor.
	 *
	 * @param bound             the spatial bound of the linked cell.
	 *
	 * @throws OpenCLException
	 */
    public CLParallelOptimalStepsModel(
		    @NotNull final AttributesOSM attributesOSM,
		    @NotNull final AttributesFloorField attributesFloorField,
		    @NotNull final VRectangle bound,
		    @NotNull final EikonalSolver targetPotential,
		    @NotNull final EikonalSolver obstaclePotential,
			final int device,
		    final double cellSize) throws OpenCLException {
    	this.attributesOSM = attributesOSM;
	    this.attributesFloorField = attributesFloorField;
		this.bound = bound;
		this.deviceType = device;
		this.targetPotential = targetPotential;
		this.obstaclePotential = obstaclePotential;

		//TODO: this should be done in mallocHostMemory().
    	if(debug) {
		    Configuration.DEBUG.set(true);
		    Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);
		    Configuration.DEBUG_STACK.set(true);
	    }
	    this.iGridSize = new int[]{ (int)Math.ceil(bound.getWidth() / cellSize),  (int)Math.ceil(bound.getHeight() / cellSize)};
	    this.numberOfGridCells = this.iGridSize[0] * this.iGridSize[1];
	    this.iCellSize = (float)cellSize;
	    init();
    }

	/**
	 * Set's new set of agents which we want to simulate. This will remove all other agents.
	 * This method will free old data from the device memory and transfer required data to the device
	 * as well as reserve new device memory.
	 *
	 * @param pedestrians       the list of pedestrians / agents
	 * @throws OpenCLException
	 */
	public void setPedestrians(@NotNull final List<PedestrianOpenCL> pedestrians) throws OpenCLException {
    	this.numberOfElements = pedestrians.size();
    	this.numberOfSortElements = (int)CLUtils.power(numberOfElements, 2);

    	// clear the old memory before re-initialization
    	if(pedestrianSet) {
		    freeCLMemory(clPedestrians);
		    freeCLMemory(clHashes);
		    freeCLMemory(clIndices);
		    freeCLMemory(clReorderedPedestrians);
		    freeCLMemory(clPedestrians);
		    freeCLMemory(clPedestrianNextPositions);
		    MemoryUtil.memFree(memNextPositions);
		    MemoryUtil.memFree(memIndices);
	    }

	    FloatBuffer memPedestrians = allocHostMemory(pedestrians);
	    int power = (int)CLUtils.power(pedestrians.size(), 2);
	    try (MemoryStack stack = stackPush()) {
		    IntBuffer errcode_ret = stack.callocInt(1);
		    clPedestrians = clCreateBuffer(clContext, CL_MEM_READ_WRITE | CL_MEM_ALLOC_HOST_PTR | CL_MEM_COPY_HOST_PTR, memPedestrians, errcode_ret);
		    clHashes = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * power, errcode_ret);
		    clIndices = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * power, errcode_ret);
		    clReorderedPedestrians = clCreateBuffer(clContext, CL_MEM_READ_WRITE, OFFSET * 4 * pedestrians.size(), errcode_ret);
		    clPedestrians = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 6 * 4 * pedestrians.size(), errcode_ret);
		    clPedestrianNextPositions = clCreateBuffer(clContext, CL_MEM_READ_WRITE, COORDOFFSET * 4 * pedestrians.size(), errcode_ret);

		    memNextPositions = MemoryUtil.memAllocFloat(numberOfElements * COORDOFFSET);
		    memIndices = MemoryUtil.memAllocInt(numberOfElements);
		    pedestrianSet = true;
	    }
	    MemoryUtil.memFree(memPedestrians);
    }

	//TODO: dont sort if the size is <= 1!
	public List<VPoint> update() throws OpenCLException {
		try (MemoryStack stack = stackPush()) {
			List<VPoint> newPositions = new ArrayList<>();
			Collections.fill(newPositions, VPoint.ZERO);
			allocGlobalHostMemory();
			allocGlobalDeviceMemory();
			clCalcHash(clHashes, clIndices, clPedestrians, clCellSize, clWorldOrigin, clGridSize, numberOfElements, numberOfSortElements);
			clBitonicSort(clHashes, clIndices, clHashes, clIndices, numberOfSortElements, 1);
			clFindCellBoundsAndReorder(clCellStarts, clCellEnds, clReorderedPedestrians, clHashes, clIndices, clPedestrians, numberOfElements);

			clSeek(
					clReorderedPedestrians,
					clCirclePositions,
					clCellStarts,
					clCellEnds,
					clCellSize,
					clGridSize,
					clObstaclePotential,
					clTargetPotential,
					clWorldOrigin,
					clPotentialFieldGridSize,
					clPotentialFieldSize,
					numberOfElements);

			clMove(clReorderedPedestrians, clCellStarts, clCellEnds, clCellSize, clGridSize, clWorldOrigin, numberOfElements);

			clEnqueueReadBuffer(clQueue, clPedestrianNextPositions, true, 0, memNextPositions, null, null);
			clEnqueueReadBuffer(clQueue, clIndices, true, 0, memIndices, null, null);

			int[] aIndices = CLUtils.toIntArray(memIndices, numberOfElements);
			float[] positionsAndRadi = CLUtils.toFloatArray(memNextPositions, numberOfElements * COORDOFFSET);
			for(int i = 0; i < numberOfElements; i++) {
				float x = positionsAndRadi[i * 2];
				float y = positionsAndRadi[i * 2 + 1];
				VPoint newPosition = new VPoint(x,y);
				newPositions.set(aIndices[i], newPosition);
			}

			counter++;
			return newPositions;
		}
	}

	/**
	 * Transforms the a list of {@link PedestrianOpenCL} into a {@link FloatBuffer} i.e. a array
	 * @param pedestrians
	 * @return
	 */
	private FloatBuffer allocHostMemory(@NotNull final List<PedestrianOpenCL> pedestrians) {
	    float[] pedestrianStruct = new float[pedestrians.size() * OFFSET];
	    for(int i = 0; i < pedestrians.size(); i++) {
		    pedestrianStruct[i * X] = (float) pedestrians.get(i).position.getX();
		    pedestrianStruct[i * Y] = (float) pedestrians.get(i).position.getY();
		    pedestrianStruct[i * STEPSIZE] = pedestrians.get(i).stepRadius;
		    pedestrianStruct[i * DESIREDSPEED] = 0;
		    pedestrianStruct[i * TIMECREDIT] = 0;
		    pedestrianStruct[i * NEWX] = 0;
		    pedestrianStruct[i * NEWY] = 0;
	    }
	    return CLUtils.toFloatBuffer(pedestrianStruct);
    }

	/**
	 * Allocates the host memory for objects which do not change during the simulation e.g. the static potential field.
	 * Therefore this initialization is done once for a simulation.
	 */
	private void allocGlobalHostMemory() {
		if(counter == 0) {
			circlePositionList = GeometryUtils.getDiscDiscretizationPoints(new Random(), false,
					new VCircle(new VPoint(0,0), 1.0),
					20, //attributesOSM.getNumberOfCircles(),
					50, //attributesOSM.getStepCircleResolution(),
					0,
					2*Math.PI);
			circlePositionList.add(VPoint.ZERO);
			float[] circlePositions = new float[circlePositionList.size() * COORDOFFSET];
			for(int i = 0; i < circlePositionList.size(); i++) {
				circlePositions[i * COORDOFFSET + X] = (float) circlePositionList.get(i).getX();
				circlePositions[i * COORDOFFSET + Y] = (float) circlePositionList.get(i).getY();
			}
			this.memCirclePositions = CLUtils.toFloatBuffer(circlePositions);

			float[] originArray = new float[]{(float)bound.getMinX(), (float)bound.getMinX()};
			this.memWorldOrigin = CLUtils.toFloatBuffer(originArray);
			this.memPotentialFieldSize = MemoryUtil.memAllocFloat(2);
			this.memPotentialFieldSize.put(0, (float)bound.width);
			this.memPotentialFieldSize.put(1, (float)bound.height);
			this.memPotentialFieldGridSize = MemoryUtil.memAllocInt(2);
			this.memPotentialFieldGridSize.put(0, getPotentialFieldWidth());
			this.memPotentialFieldGridSize.put(1, getPotentialFieldHeight());
			this.memCellSize = MemoryUtil.memAllocFloat(1);
			this.memCellSize.put(0, iCellSize);
			this.memGridSize = CLUtils.toIntBuffer(iGridSize);
			this.memTargetPotentialField = generatePotentialFieldApproximation(targetPotential);
			this.memObstaclePotentialField = generatePotentialFieldApproximation(obstaclePotential);
		}
	}

	/**
	 * Allocates the device memory for objects which do not change during the simulation e.g. the static potential field.
	 * Therefore this initialization is done once for a simulation.
	 */
    private void allocGlobalDeviceMemory() {
	    if(counter == 0) {
	        try (MemoryStack stack = stackPush()) {
		        IntBuffer errcode_ret = stack.callocInt(1);
			    clCellSize = clCreateBuffer(clContext,  CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR | CL_MEM_COPY_HOST_PTR, memCellSize, errcode_ret);
			    clGridSize = clCreateBuffer(clContext,  CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR | CL_MEM_COPY_HOST_PTR, memGridSize, errcode_ret);
			    clPotentialFieldSize = clCreateBuffer(clContext,  CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR | CL_MEM_COPY_HOST_PTR, memPotentialFieldSize, errcode_ret);
			    clPotentialFieldGridSize = clCreateBuffer(clContext,  CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR | CL_MEM_COPY_HOST_PTR, memPotentialFieldGridSize, errcode_ret);
			    clWorldOrigin = clCreateBuffer(clContext,  CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR | CL_MEM_COPY_HOST_PTR, memWorldOrigin, errcode_ret);
			    clTargetPotential = clCreateBuffer(clContext,  CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR | CL_MEM_COPY_HOST_PTR, memTargetPotentialField, errcode_ret);
			    clObstaclePotential = clCreateBuffer(clContext,  CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR | CL_MEM_COPY_HOST_PTR, memObstaclePotentialField, errcode_ret);
			    clCirclePositions = clCreateBuffer(clContext,  CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR | CL_MEM_COPY_HOST_PTR, memCirclePositions, errcode_ret);
			    clCellStarts = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * numberOfGridCells, errcode_ret);
			    clCellEnds = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * numberOfGridCells, errcode_ret);
		    }
	    }
    }

	private void init() throws OpenCLException {
        initCallbacks();
        initCL();
        buildProgram();
    }

    private void clCalcHash(
    		final long clHashes,
		    final long clIndices,
		    final long clPositions,
		    final long clCellSize,
		    final long clWorldOrign,
		    final long clGridSize,
		    final int numberOfElements,
		    final int numberOfElementsPower) throws OpenCLException {
	    try (MemoryStack stack = stackPush()) {
		    PointerBuffer clGlobalWorkSize = stack.callocPointer(1);
		    CLInfo.checkCLError(clSetKernelArg1p(clCalcHash, 0, clHashes));
		    CLInfo.checkCLError(clSetKernelArg1p(clCalcHash, 1, clIndices));
		    CLInfo.checkCLError(clSetKernelArg1p(clCalcHash, 2, clPositions));
		    CLInfo.checkCLError(clSetKernelArg1p(clCalcHash, 3, clCellSize));
		    CLInfo.checkCLError(clSetKernelArg1p(clCalcHash, 4, clWorldOrign));
		    CLInfo.checkCLError(clSetKernelArg1p(clCalcHash, 5, clGridSize));
		    CLInfo.checkCLError(clSetKernelArg1i(clCalcHash, 6, numberOfElements));
		    clGlobalWorkSize.put(0, numberOfElementsPower);
		    //TODO: local work size?
		    CLInfo.checkCLError((int)enqueueNDRangeKernel("clCalcHash", clQueue, clCalcHash, 1, null, clGlobalWorkSize, null, null, null));
	    }
    }

    private void clSeek(
		    final long clReorderedPedestrians,
		    final long clCirclePositions,
		    final long clCellStarts,
		    final long clCellEnds,
		    final long clCellSize,
		    final long clGridSize,
		    final long clObstaclePotential,
		    final long clTargetPotential,
		    final long clWorldOrigin,
		    final long clPotentialFieldGridSize,
		    final long clPotentialFieldSize,
		    final int numberOfElements)
		    throws OpenCLException {
	    try (MemoryStack stack = stackPush()) {

		    PointerBuffer clGlobalWorkSize = stack.callocPointer(1);
		    PointerBuffer clLocalWorkSize = stack.callocPointer(1);
			long maxWorkGroupSize = getMaxWorkGroupSizeForKernel(clDevice, clSeek, 0); // local 4 byte (integer)

		    CLInfo.checkCLError(clSetKernelArg1p(clSeek, 0, clReorderedPedestrians));
		    CLInfo.checkCLError(clSetKernelArg1p(clSeek, 1, clCirclePositions));
		    CLInfo.checkCLError(clSetKernelArg1p(clSeek, 2, clCellStarts));
		    CLInfo.checkCLError(clSetKernelArg1p(clSeek, 3, clCellEnds));
		    CLInfo.checkCLError(clSetKernelArg1p(clSeek, 4, clCellSize));
		    CLInfo.checkCLError(clSetKernelArg1p(clSeek, 5, clGridSize));
		    CLInfo.checkCLError(clSetKernelArg1p(clSeek, 6, clObstaclePotential));
		    CLInfo.checkCLError(clSetKernelArg1p(clSeek, 7, clTargetPotential));
		    CLInfo.checkCLError(clSetKernelArg1p(clSeek, 8, clWorldOrigin));
		    CLInfo.checkCLError(clSetKernelArg1p(clSeek, 9, clPotentialFieldGridSize));
		    CLInfo.checkCLError(clSetKernelArg1p(clSeek, 10, clPotentialFieldSize));
		    CLInfo.checkCLError(clSetKernelArg1f(clSeek, 11, (float)attributesFloorField.getPotentialFieldResolution()));
		    CLInfo.checkCLError(clSetKernelArg1f(clSeek, 12, timeStepInSec));
		    CLInfo.checkCLError(clSetKernelArg1i(clSeek, 13, circlePositionList.size()));
		    CLInfo.checkCLError(clSetKernelArg1i(clSeek, 14, numberOfElements));

		    long globalWorkSize;
		    long localWorkSize;
		    if(numberOfElements <= maxWorkGroupSize){
			    localWorkSize = numberOfElements;
			    globalWorkSize = numberOfElements;
		    }
		    else {
			    localWorkSize = maxWorkGroupSize;
			    //globalWorkSize = CLUtils.power(numberOfElements, 2);
			    globalWorkSize = CLUtils.multiple(numberOfElements, localWorkSize);
		    }

		    clGlobalWorkSize.put(0, globalWorkSize);
		    clLocalWorkSize.put(0, localWorkSize);
		    //TODO: local work size? + check 2^n constrain!
		    CLInfo.checkCLError((int)enqueueNDRangeKernel("clSeek", clQueue, clSeek, 1, null, clGlobalWorkSize, clLocalWorkSize, null, null));
	    }
    }

	private void clMove(
			final long clReorderedPedestrians,
			final long clCellStarts,
			final long clCellEnds,
			final long clCellSize,
			final long clGridSize,
			final long clWorldOrigin,
			final int numberOfElements)
			throws OpenCLException {
		try (MemoryStack stack = stackPush()) {

			PointerBuffer clGlobalWorkSize = stack.callocPointer(1);
			PointerBuffer clLocalWorkSize = stack.callocPointer(1);
			long maxWorkGroupSize = getMaxWorkGroupSizeForKernel(clDevice, clSeek, 0); // local 4 byte (integer)

			CLInfo.checkCLError(clSetKernelArg1p(clMove, 0, clPedestrianNextPositions));
			CLInfo.checkCLError(clSetKernelArg1p(clMove, 1, clReorderedPedestrians));
			CLInfo.checkCLError(clSetKernelArg1p(clMove, 2, clCellStarts));
			CLInfo.checkCLError(clSetKernelArg1p(clMove, 3, clCellEnds));
			CLInfo.checkCLError(clSetKernelArg1p(clMove, 4, clCellSize));
			CLInfo.checkCLError(clSetKernelArg1p(clMove, 5, clGridSize));
			CLInfo.checkCLError(clSetKernelArg1p(clMove, 6, clWorldOrigin));
			CLInfo.checkCLError(clSetKernelArg1i(clMove, 7, numberOfElements));

			long globalWorkSize;
			long localWorkSize;
			if(numberOfElements <= maxWorkGroupSize){
				localWorkSize = numberOfElements;
				globalWorkSize = numberOfElements;
			}
			else {
				localWorkSize = maxWorkGroupSize;
				globalWorkSize = CLUtils.power(numberOfElements, 2);
				//globalWorkSize = CLUtils.multiple(numberOfElements, localWorkSize);
			}

			clGlobalWorkSize.put(0, globalWorkSize);
			clLocalWorkSize.put(0, localWorkSize);
			//TODO: local work size? + check 2^n constrain!
			CLInfo.checkCLError((int)enqueueNDRangeKernel("clSeek", clQueue, clSeek, 1, null, clGlobalWorkSize, clLocalWorkSize, null, null));
		}
	}


    private void clFindCellBoundsAndReorder(
    		final long clCellStarts,
		    final long clCellEnds,
		    final long clReorderedPositions,
		    final long clHashes,
		    final long clIndices,
		    final long clPositions,
		    final int numberOfElements) throws OpenCLException {

	    try (MemoryStack stack = stackPush()) {

		    PointerBuffer clGlobalWorkSize = stack.callocPointer(1);
		    PointerBuffer clLocalWorkSize = stack.callocPointer(1);
		    IntBuffer errcode_ret = stack.callocInt(1);
			long maxWorkGroupSize = getMaxWorkGroupSizeForKernel(clDevice, clSeek, 0); // local 4 byte (integer)

		    CLInfo.checkCLError(clSetKernelArg1p(clFindCellBoundsAndReorder, 0, clCellStarts));
		    CLInfo.checkCLError(clSetKernelArg1p(clFindCellBoundsAndReorder, 1, clCellEnds));
		    CLInfo.checkCLError(clSetKernelArg1p(clFindCellBoundsAndReorder, 2, clReorderedPositions));
		    CLInfo.checkCLError(clSetKernelArg1p(clFindCellBoundsAndReorder, 3, clHashes));
		    CLInfo.checkCLError(clSetKernelArg1p(clFindCellBoundsAndReorder, 4, clIndices));
		    CLInfo.checkCLError(clSetKernelArg1p(clFindCellBoundsAndReorder, 5, clPositions));
		    CLInfo.checkCLError(clSetKernelArg(clFindCellBoundsAndReorder, 6, (Math.min(numberOfElements+1, maxWorkGroupSize)) * 4)); // local memory
		    CLInfo.checkCLError(clSetKernelArg1i(clFindCellBoundsAndReorder, 7, numberOfElements));

		    long globalWorkSize;
		    long localWorkSize;
			if(numberOfElements+1 < maxWorkGroupSize){
				localWorkSize = numberOfElements;
				globalWorkSize = numberOfElements;
			}
			else {
				localWorkSize = maxWorkGroupSize;
				globalWorkSize = CLUtils.multiple(numberOfElements, localWorkSize);
			}

		    clGlobalWorkSize.put(0, globalWorkSize);
		    clLocalWorkSize.put(0, localWorkSize);
		    //TODO: local work size? + check 2^n constrain!
		    CLInfo.checkCLError((int)enqueueNDRangeKernel("clFindCellBoundsAndReorder", clQueue, clFindCellBoundsAndReorder, 1, null, clGlobalWorkSize, clLocalWorkSize, null, null));
	    }
    }

	private long enqueueNDRangeKernel(final String name, long command_queue, long kernel, int work_dim, PointerBuffer global_work_offset, PointerBuffer global_work_size, PointerBuffer local_work_size, PointerBuffer event_wait_list, PointerBuffer event) throws OpenCLException {
		if(profiling) {
			try (MemoryStack stack = stackPush()) {
				PointerBuffer clEvent = stack.mallocPointer(1);
				LongBuffer startTime = stack.mallocLong(1);
				LongBuffer endTime = stack.mallocLong(1);
				long result = clEnqueueNDRangeKernel(command_queue, kernel, work_dim, global_work_offset, global_work_size, local_work_size, event_wait_list, clEvent);
				clWaitForEvents(clEvent);
				long eventAddr = clEvent.get();
				CLInfo.checkCLError(clGetEventProfilingInfo(eventAddr, CL_PROFILING_COMMAND_START, startTime, null));
				CLInfo.checkCLError(clGetEventProfilingInfo(eventAddr, CL_PROFILING_COMMAND_END, endTime, null));
				clEvent.clear();
				// in nanaSec
				log.info(name + " event time " + "0x"+eventAddr + ": " + ((double)endTime.get() - startTime.get()) / 1_000_000.0 + " [ms]");
				endTime.clear();
				startTime.clear();
				return result;
			}
		}
		else {
			return clEnqueueNDRangeKernel(command_queue, kernel, work_dim, global_work_offset, global_work_size, local_work_size, event_wait_list, event);
		}
	}

	private long getMaxWorkGroupSizeForKernel(long clDevice, long clKernel, long workItemMem) throws OpenCLException {
		try (MemoryStack stack = stackPush()) {
			LongBuffer pp = stack.mallocLong(1);
			CLInfo.checkCLError(clGetKernelWorkGroupInfo(clKernel, clDevice, CL_KERNEL_LOCAL_MEM_SIZE , pp, null));

			/*long kernelLocalMemory = pp.get(0);
			logger.debug("CL_KERNEL_LOCAL_MEM_SIZE = (" + clKernel + ") = " + kernelLocalMemory);
			logger.debug("memory for each  = " + (workItemMem + kernelLocalMemory));

			long maxWorkGroupSizeForLocalMemory = (workItemMem + kernelLocalMemory) == 0 ? 0 : (max_local_memory_size / (workItemMem + kernelLocalMemory));*/
			long maxWorkGroupSizeForLocalMemory = workItemMem == 0 ? max_work_group_size : (max_local_memory_size / (workItemMem));
			PointerBuffer ppp = stack.mallocPointer(1);
			CLInfo.checkCLError(clGetKernelWorkGroupInfo(clKernel, clDevice, CL_KERNEL_WORK_GROUP_SIZE , ppp, null));

			long maxWorkGroupSizeForPrivateMemory = ppp.get(0);
			logger.debug("CL_KERNEL_WORK_GROUP_SIZE (" + clKernel + ") = " + maxWorkGroupSizeForPrivateMemory);
			//return Math.min(max_work_group_size, Math.min(maxWorkGroupSizeForLocalMemory, maxWorkGroupSizeForPrivateMemory));
			return Math.min(max_work_group_size, Math.min(maxWorkGroupSizeForLocalMemory, maxWorkGroupSizeForPrivateMemory));
		}
	}

	// TODO: global and local work size computation
    private void clBitonicSort(
    		final long clKeysIn,
		    final long clValuesIn,
		    final long clKeysOut,
		    final long clValuesOut,
		    final int numberOfElements,
		    final int dir) throws OpenCLException {

	    try (MemoryStack stack = stackPush()) {

	    	PointerBuffer clGlobalWorkSize = stack.callocPointer(1);
		    PointerBuffer clLocalWorkSize = stack.callocPointer(1);
		    IntBuffer errcode_ret = stack.callocInt(1);
			long maxWorkGroupSize = getMaxWorkGroupSizeForKernel(clDevice, clBitonicMergeLocal, 8); // local memory for key and values (integer)

		    // small sorts
		    if (numberOfElements <= maxWorkGroupSize) {
			    CLInfo.checkCLError(clSetKernelArg1p(clBitonicSortLocal, 0, clKeysOut));
			    CLInfo.checkCLError(clSetKernelArg1p(clBitonicSortLocal, 1, clValuesOut));
			    CLInfo.checkCLError(clSetKernelArg1p(clBitonicSortLocal, 2, clKeysIn));
			    CLInfo.checkCLError(clSetKernelArg1p(clBitonicSortLocal, 3, clValuesIn));
			    CLInfo.checkCLError(clSetKernelArg1i(clBitonicSortLocal, 4, numberOfElements));
			    //TODO: check the hard coded 1, and the waiting of the queue
			    CLInfo.checkCLError(clSetKernelArg1i(clBitonicSortLocal, 5, 1));
			    CLInfo.checkCLError(clSetKernelArg(clBitonicSortLocal, 6, numberOfElements * 4)); // local memory
			    CLInfo.checkCLError(clSetKernelArg(clBitonicSortLocal, 7, numberOfElements * 4)); // local memory
			    clGlobalWorkSize.put(0, numberOfElements / 2);
			    clLocalWorkSize.put(0, numberOfElements / 2);

			    // run the kernel and read the result
			    CLInfo.checkCLError((int)enqueueNDRangeKernel("clBitonicSortLocal", clQueue, clBitonicSortLocal, 1, null, clGlobalWorkSize, clLocalWorkSize, null, null));
			    CLInfo.checkCLError(clFinish(clQueue));
		    } else {
			    //Launch bitonicSortLocal1
			    CLInfo.checkCLError(clSetKernelArg1p(clBitonicSortLocal1, 0, clKeysOut));
			    CLInfo.checkCLError(clSetKernelArg1p(clBitonicSortLocal1, 1, clValuesOut));
			    CLInfo.checkCLError(clSetKernelArg1p(clBitonicSortLocal1, 2, clKeysIn));
			    CLInfo.checkCLError(clSetKernelArg1p(clBitonicSortLocal1, 3, clValuesIn));
			    CLInfo.checkCLError(clSetKernelArg(clBitonicSortLocal1, 4, maxWorkGroupSize * 4)); // local memory
			    CLInfo.checkCLError(clSetKernelArg(clBitonicSortLocal1, 5, maxWorkGroupSize * 4)); // local memory

			    clGlobalWorkSize = stack.callocPointer(1);
			    clLocalWorkSize = stack.callocPointer(1);
			    clGlobalWorkSize.put(0, numberOfElements / 2);
			    clLocalWorkSize.put(0, maxWorkGroupSize / 2);

			    CLInfo.checkCLError((int)enqueueNDRangeKernel("clBitonicSortLocal", clQueue, clBitonicSortLocal1, 1, null, clGlobalWorkSize, clLocalWorkSize, null, null));
			    CLInfo.checkCLError(clFinish(clQueue));

			    for (int size = (int)(2 * maxWorkGroupSize); size <= numberOfElements; size <<= 1) {
				    for (int stride = size / 2; stride > 0; stride >>= 1) {
					    if (stride >= maxWorkGroupSize) {
						    //Launch bitonicMergeGlobal
						    CLInfo.checkCLError(clSetKernelArg1p(clBitonicMergeGlobal, 0, clKeysOut));
						    CLInfo.checkCLError(clSetKernelArg1p(clBitonicMergeGlobal, 1, clValuesOut));
						    CLInfo.checkCLError(clSetKernelArg1p(clBitonicMergeGlobal, 2, clKeysOut));
						    CLInfo.checkCLError(clSetKernelArg1p(clBitonicMergeGlobal, 3, clValuesOut));

						    CLInfo.checkCLError(clSetKernelArg1i(clBitonicMergeGlobal, 4, numberOfElements));
						    CLInfo.checkCLError(clSetKernelArg1i(clBitonicMergeGlobal, 5, size));
						    CLInfo.checkCLError(clSetKernelArg1i(clBitonicMergeGlobal, 6, stride));
						    CLInfo.checkCLError(clSetKernelArg1i(clBitonicMergeGlobal, 7, dir));

						    clGlobalWorkSize = stack.callocPointer(1);
						    clLocalWorkSize = stack.callocPointer(1);
						    clGlobalWorkSize.put(0, numberOfElements / 2);
						    clLocalWorkSize.put(0, maxWorkGroupSize / 4);

						    CLInfo.checkCLError((int)enqueueNDRangeKernel("clBitonicMergeGlobal", clQueue, clBitonicMergeGlobal, 1, null, clGlobalWorkSize, clLocalWorkSize, null, null));
						    CLInfo.checkCLError(clFinish(clQueue));
					    } else {
						    //Launch bitonicMergeLocal
						    CLInfo.checkCLError(clSetKernelArg1p(clBitonicMergeLocal, 0, clKeysOut));
						    CLInfo.checkCLError(clSetKernelArg1p(clBitonicMergeLocal, 1, clValuesOut));
						    CLInfo.checkCLError(clSetKernelArg1p(clBitonicMergeLocal, 2, clKeysOut));
						    CLInfo.checkCLError(clSetKernelArg1p(clBitonicMergeLocal, 3, clValuesOut));

						    CLInfo.checkCLError(clSetKernelArg1i(clBitonicMergeLocal, 4, numberOfElements));
						    CLInfo.checkCLError(clSetKernelArg1i(clBitonicMergeLocal, 5, stride));
						    CLInfo.checkCLError(clSetKernelArg1i(clBitonicMergeLocal, 6, size));
						    CLInfo.checkCLError(clSetKernelArg1i(clBitonicMergeLocal, 7, dir));
						    CLInfo.checkCLError(clSetKernelArg(clBitonicMergeLocal, 8, maxWorkGroupSize * 4)); // local memory
						    CLInfo.checkCLError(clSetKernelArg(clBitonicMergeLocal, 9, maxWorkGroupSize * 4)); // local memory

						    clGlobalWorkSize = stack.callocPointer(1);
						    clLocalWorkSize = stack.callocPointer(1);
						    clGlobalWorkSize.put(0, numberOfElements / 2);
						    clLocalWorkSize.put(0, maxWorkGroupSize / 2);

						    CLInfo.checkCLError((int)enqueueNDRangeKernel("clBitonicMergeLocal", clQueue, clBitonicMergeLocal, 1, null, clGlobalWorkSize, clLocalWorkSize, null, null));
						    CLInfo.checkCLError(clFinish(clQueue));
						    break;
					    }
				    }
			    }
		    }
	    }
    }

	static long factorRadix2(long L){
		if(L==0){
			return 0;
		}else{
			for(int log2L = 0; (L & 1) == 0; L >>= 1, log2L++);
			return L;
		}
	}

	public void clear() throws OpenCLException {
		clearMemory();
		clearCL();
	}

	private void freeCLMemory(long address)  throws OpenCLException {
		try {
			CLInfo.checkCLError(clReleaseMemObject(address));
		} catch (OpenCLException ex) {
			throw ex;
		}
	}

    private void clearMemory() throws OpenCLException {
        // release memory and devices
	    try {
	    	if(pedestrianSet) {
			    CLInfo.checkCLError(clReleaseMemObject(clPedestrians));
			    CLInfo.checkCLError(clReleaseMemObject(clHashes));
			    CLInfo.checkCLError(clReleaseMemObject(clIndices));
			    CLInfo.checkCLError(clReleaseMemObject(clReorderedPedestrians));
			    CLInfo.checkCLError(clReleaseMemObject(clPedestrianNextPositions));
		    }

	    	if(counter > 0) {
			    CLInfo.checkCLError(clReleaseMemObject(clCellStarts));
			    CLInfo.checkCLError(clReleaseMemObject(clCellEnds));
			    CLInfo.checkCLError(clReleaseMemObject(clCellSize));
			    CLInfo.checkCLError(clReleaseMemObject(clWorldOrigin));
			    CLInfo.checkCLError(clReleaseMemObject(clGridSize));
			    CLInfo.checkCLError(clReleaseMemObject(clTargetPotential));
			    CLInfo.checkCLError(clReleaseMemObject(clObstaclePotential));
			    CLInfo.checkCLError(clReleaseMemObject(clCirclePositions));
			    CLInfo.checkCLError(clReleaseMemObject(clPotentialFieldGridSize));
			    CLInfo.checkCLError(clReleaseMemObject(clPotentialFieldSize));
		    }

	    }
	    catch (OpenCLException ex) {
			throw ex;
	    }
		finally {
		    if(pedestrianSet) {
			    MemoryUtil.memFree(memNextPositions);
			    MemoryUtil.memFree(memIndices);
		    }
	    	if(counter > 0) {
			    MemoryUtil.memFree(memWorldOrigin);
			    MemoryUtil.memFree(memCellSize);
			    MemoryUtil.memFree(memTargetPotentialField);
			    MemoryUtil.memFree(memObstaclePotentialField);
			    MemoryUtil.memFree(memCirclePositions);
			    MemoryUtil.memFree(memPotentialFieldSize);
			    MemoryUtil.memFree(memGridSize);
			    MemoryUtil.memFree(memPotentialFieldGridSize);
		    }
	    }
    }

    private void clearCL() throws OpenCLException {
	    CLInfo.checkCLError(clReleaseKernel(clBitonicSortLocal));
	    CLInfo.checkCLError(clReleaseKernel(clBitonicSortLocal1));
	    CLInfo.checkCLError(clReleaseKernel(clBitonicMergeGlobal));
	    CLInfo.checkCLError(clReleaseKernel(clBitonicMergeLocal));
	    CLInfo.checkCLError(clReleaseKernel(clCalcHash));
	    CLInfo.checkCLError(clReleaseKernel(clFindCellBoundsAndReorder));

	    CLInfo.checkCLError(clReleaseCommandQueue(clQueue));
	    CLInfo.checkCLError(clReleaseProgram(clProgram));
	    CLInfo.checkCLError(clReleaseContext(clContext));
	    contextCB.free();
	    programCB.free();
    }

    // private helpers
    private void initCallbacks() {
        contextCB = CLContextCallback.create((errinfo, private_info, cb, user_data) ->
        {
            log.debug("[LWJGL] cl_context_callback" + "\tInfo: " + memUTF8(errinfo));
        });

        programCB = CLProgramCallback.create((program, user_data) ->
        {
	        try {
		        log.debug("The cl_program [0x"+program+"] was built " + (CLInfo.getProgramBuildInfoInt(program, clDevice, CL_PROGRAM_BUILD_STATUS) == CL_SUCCESS ? "successfully" : "unsuccessfully"));
	        } catch (OpenCLException e) {
		        e.printStackTrace();
	        }
        });
    }

    private void initCL() throws OpenCLException {
        try (MemoryStack stack = stackPush()) {
            IntBuffer errcode_ret = stack.callocInt(1);
            IntBuffer numberOfPlatforms = stack.mallocInt(1);

	        CLInfo.checkCLError(clGetPlatformIDs(null, numberOfPlatforms));
            PointerBuffer platformIDs = stack.mallocPointer(numberOfPlatforms.get(0));
	        CLInfo.checkCLError(clGetPlatformIDs(platformIDs, numberOfPlatforms));

            clPlatform = platformIDs.get(0);

            IntBuffer numberOfDevices = stack.mallocInt(1);
	        CLInfo.checkCLError(clGetDeviceIDs(clPlatform, CL_DEVICE_TYPE_GPU, null, numberOfDevices));
            PointerBuffer deviceIDs = stack.mallocPointer(numberOfDevices.get(0));
	        CLInfo.checkCLError(clGetDeviceIDs(clPlatform, CL_DEVICE_TYPE_GPU, deviceIDs, numberOfDevices));

            clDevice = deviceIDs.get(0);

	        log.debug("CL_DEVICE_NAME = " + CLInfo.getDeviceInfoStringUTF8(clDevice, CL_DEVICE_NAME));

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
        }
    }

    private void buildProgram() throws OpenCLException {
	    try (MemoryStack stack = stackPush()) {
		    IntBuffer errcode_ret = stack.callocInt(1);

		    PointerBuffer strings = stack.mallocPointer(1);
		    PointerBuffer lengths = stack.mallocPointer(1);

		    ByteBuffer source;
		    try {
			    source = CLUtils.ioResourceToByteBuffer("ParallelOSM.cl", 4096);
		    } catch (IOException e) {
			    throw new OpenCLException(e.getMessage());
		    }

		    strings.put(0, source);
		    lengths.put(0, source.remaining());
		    clProgram = clCreateProgramWithSource(clContext, strings, lengths, errcode_ret);
		    logger.debug(InfoUtils.getProgramBuildInfoStringASCII(clProgram, clDevice, CL_PROGRAM_BUILD_LOG));

		    CLInfo.checkCLError(clBuildProgram(clProgram, clDevice, "", programCB, NULL));
		    clBitonicSortLocal = clCreateKernel(clProgram, "bitonicSortLocal", errcode_ret);
		    CLInfo.checkCLError(errcode_ret);
		    clBitonicSortLocal1 = clCreateKernel(clProgram, "bitonicSortLocal1", errcode_ret);
		    CLInfo.checkCLError(errcode_ret);
		    clBitonicMergeGlobal = clCreateKernel(clProgram, "bitonicMergeGlobal", errcode_ret);
		    CLInfo.checkCLError(errcode_ret);
		    clBitonicMergeLocal = clCreateKernel(clProgram, "bitonicMergeLocal", errcode_ret);
		    CLInfo.checkCLError(errcode_ret);
		    clCalcHash = clCreateKernel(clProgram, "calcHash", errcode_ret);
		    CLInfo.checkCLError(errcode_ret);
		    clFindCellBoundsAndReorder = clCreateKernel(clProgram, "findCellBoundsAndReorder", errcode_ret);
		    CLInfo.checkCLError(errcode_ret);

		    clSeek = clCreateKernel(clProgram, "seek", errcode_ret);
		    CLInfo.checkCLError(errcode_ret);
		    clMove = clCreateKernel(clProgram, "move", errcode_ret);
		    CLInfo.checkCLError(errcode_ret);

			max_work_group_size = InfoUtils.getDeviceInfoPointer(clDevice, CL_DEVICE_MAX_WORK_GROUP_SIZE);
			logger.debug("CL_DEVICE_MAX_WORK_GROUP_SIZE = " + max_work_group_size);

			max_local_memory_size = InfoUtils.getDeviceInfoLong(clDevice, CL_DEVICE_LOCAL_MEM_SIZE);
			logger.debug("CL_DEVICE_LOCAL_MEM_SIZE = " + max_local_memory_size);

			MemoryUtil.memFree(source);
	    }
    }

	private int getPotentialFieldWidth() {
		return (int) Math.floor(bound.getWidth() / attributesFloorField.getPotentialFieldResolution()) + 1;
	}

	private int getPotentialFieldHeight() {
		return (int) Math.floor(bound.getHeight() / attributesFloorField.getPotentialFieldResolution()) + 1;
	}

	private int getPotentialFieldSize() {
		return getPotentialFieldWidth() * getPotentialFieldHeight();
	}

	private FloatBuffer generatePotentialFieldApproximation(@NotNull final EikonalSolver eikonalSolver) {
		FloatBuffer floatBuffer = MemoryUtil.memAllocFloat(getPotentialFieldSize());

		int index = 0;
		for(int row = 0; row < getPotentialFieldHeight(); row++) {
			for(int col = 0; col < getPotentialFieldWidth(); col++) {
				double y = row * attributesFloorField.getPotentialFieldResolution() + bound.getMinY();
				double x = col * attributesFloorField.getPotentialFieldResolution() + bound.getMinX();

				float value = (float)eikonalSolver.getPotential(new VPoint(x, y),
						attributesFloorField.getObstacleGridPenalty(),
						attributesFloorField.getTargetAttractionStrength());

				floatBuffer.put(index, value);
				index++;
			}
		}

		return floatBuffer;
	}

	public static class PedestrianOpenCL {
		public float stepRadius;
		public float freeFlowSpeed;
		public VPoint position;
		public VPoint newPosition;

		public PedestrianOpenCL(final VPoint position, final float stepRadius, final float freeFlowSpeed) {
			this.position = position;
			this.stepRadius = stepRadius;
			this.freeFlowSpeed = freeFlowSpeed;
		}

		public PedestrianOpenCL(final VPoint position, final float stepRadius) {
			this.position = position;
			this.stepRadius = stepRadius;
			this.freeFlowSpeed = 1.34f;
		}

		@Override
		public String toString() {
			return position + " -> " + newPosition;
		}
	}
}