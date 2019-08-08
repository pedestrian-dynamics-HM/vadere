package org.vadere.simulator.models.osm.opencl;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.simulator.models.potential.solver.calculators.EikonalSolver;
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

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.lwjgl.opencl.CL10.CL_DEVICE_TYPE_GPU;
import static org.lwjgl.opencl.CL10.CL_MEM_ALLOC_HOST_PTR;
import static org.lwjgl.opencl.CL10.CL_MEM_COPY_HOST_PTR;
import static org.lwjgl.opencl.CL10.CL_MEM_READ_ONLY;
import static org.lwjgl.opencl.CL10.CL_MEM_READ_WRITE;
import static org.lwjgl.opencl.CL10.clCreateBuffer;
import static org.lwjgl.opencl.CL10.clCreateKernel;
import static org.lwjgl.opencl.CL10.clEnqueueReadBuffer;
import static org.lwjgl.opencl.CL10.clEnqueueWriteBuffer;
import static org.lwjgl.opencl.CL10.clFinish;
import static org.lwjgl.opencl.CL10.clReleaseKernel;
import static org.lwjgl.opencl.CL10.clReleaseMemObject;
import static org.lwjgl.opencl.CL10.clSetKernelArg;
import static org.lwjgl.opencl.CL10.clSetKernelArg1i;
import static org.lwjgl.opencl.CL10.clSetKernelArg1p;
import static org.lwjgl.system.MemoryStack.stackPush;

public abstract class CLAbstractOSM extends CLAbstract implements ICLOptimalStepsModel {

	protected int COORDOFFSET = 2;
	protected int X = 0;
	protected int Y = 1;

	protected int OFFSET = 4;
	protected int STEPSIZE = 0;
	protected int DESIREDSPEED = 1;
	protected int NEWX = 2;
	protected int NEWY = 3;

	private static Logger logger = Logger.getLogger(CLAbstractOSM.class);

	static {
		logger.setDebug();
	}

	// device memory
	protected long clPositions;
	protected long clPedestrians;
	protected long clGlobalIndexOut;
	protected long clGlobalIndexIn;
	protected long clHashes;
	protected long clIndices;
	protected long clReorderedPedestrians;
	protected long clReorderedPositions;
	protected long clCellStarts;
	protected long clCellEnds;
	protected long clCellSize;
	protected long clWorldOrigin;
	protected long clGridSize;
	protected long clTargetPotential;
	protected long clObstaclePotential;
	protected long clCirclePositions;
	protected long clPotentialFieldSize;
	protected long clPotentialFieldGridSize;

	// host Memory
	private FloatBuffer memWorldOrigin;
	private FloatBuffer memCellSize;
	private FloatBuffer memTargetPotentialField;
	private FloatBuffer memObstaclePotentialField;
	private FloatBuffer memCirclePositions;
	private FloatBuffer memPotentialFieldSize;
	private IntBuffer memGridSize;
	private IntBuffer memPotentialFieldGridSize;

	// kernels
	protected long clResetCells;
	protected long clCalcHash;
	protected long clBitonicSortLocal;
	protected long clBitonicSortLocal1;
	protected long clBitonicMergeGlobal;
	protected long clBitonicMergeLocal;
	protected long clSwapIndex;


	protected FloatBuffer memNextPositions;
	//protected FloatBuffer memEventTimes;
	protected IntBuffer memIndices;

	// fixed
	protected AttributesOSM attributesOSM;
	protected AttributesFloorField attributesFloorField;
	protected VRectangle bound;
	protected final int deviceType;
	protected  EikonalSolver targetPotential;
	protected  EikonalSolver obstaclePotential;
	protected int[] iGridSize;
	protected int numberOfGridCells;
	protected float iCellSize;
	protected int maxNumberOfElementsPerCell;

	// changes
	protected boolean swap = false;
	protected List<VPoint> positions;
	protected List<VPoint> circlePositionList;
	protected int[] indices;
	//protected float[] eventTimes;
	protected boolean pedestrianSet;

	// maybe changes
	protected int numberOfElements;

	protected int numberOfSortElements;

	protected int counter;


	public CLAbstractOSM(
			@NotNull final AttributesOSM attributesOSM,
			@NotNull final AttributesFloorField attributesFloorField,
			@NotNull final VRectangle bound,
			@NotNull final EikonalSolver targetPotential,
			@NotNull final EikonalSolver obstaclePotential,
			final double cellSize) throws OpenCLException {
		this(attributesOSM, attributesFloorField, bound, targetPotential, obstaclePotential, CL_DEVICE_TYPE_GPU, cellSize);
	}

	public CLAbstractOSM(
			@NotNull final AttributesOSM attributesOSM,
			@NotNull final AttributesFloorField attributesFloorField,
			@NotNull final VRectangle bound,
			@NotNull final EikonalSolver targetPotential,
			@NotNull final EikonalSolver obstaclePotential,
			final int device,
			final double cellSize) throws OpenCLException {
		super();
		this.counter = 0;
		this.pedestrianSet = false;
		this.attributesOSM = attributesOSM;
		this.attributesFloorField = attributesFloorField;
		this.bound = bound;
		this.deviceType = device;
		this.targetPotential = targetPotential;
		this.obstaclePotential = obstaclePotential;

		//TODO: this should be done in mallocHostMemory().
		this.iGridSize = new int[]{ (int)Math.ceil(bound.getWidth() / cellSize),  (int)Math.ceil(bound.getHeight() / cellSize)};
		this.numberOfGridCells = this.iGridSize[0] * this.iGridSize[1];
		//this.iCellSize = (float)cellSize;
		this.iCellSize = 3.0f;
		double radius = 0.2;
		this.maxNumberOfElementsPerCell = (int)Math.ceil( (cellSize + radius) * (cellSize + radius) / (Math.PI * radius * radius));

		circlePositionList = GeometryUtils.getDiscDiscretizationPoints(new Random(), false,
				new VCircle(new VPoint(0,0), 1.0),
				attributesOSM.getNumberOfCircles(),
				attributesOSM.getStepCircleResolution(),
				0,
				2*Math.PI);
		circlePositionList.add(VPoint.ZERO);
	}

	@Override
	public void setPedestrians(@NotNull final List<PedestrianOSM> pedestrians) throws OpenCLException {
		this.numberOfElements = pedestrians.size();
		this.numberOfSortElements = (int) CLUtils.power(numberOfElements, 2);
		this.indices = new int[pedestrians.size()];

		for (int i = 0; i < indices.length; i++) {
			indices[i] = i;
		}

		if(pedestrianSet) {
			clearIterationDeviceMemory();
			MemoryUtil.memFree(memNextPositions);
			MemoryUtil.memFree(memIndices);
		}

		FloatBuffer memPedestrians = allocPedestrianHostMemory(pedestrians);
		FloatBuffer memPositions = allocPositionHostMemory(pedestrians);
		int power = (int)CLUtils.power(pedestrians.size(), 2);
		try (MemoryStack stack = stackPush()) {
			IntBuffer errcode_ret = stack.callocInt(1);
			clPedestrians = clCreateBuffer(clContext, CL_MEM_READ_WRITE, OFFSET * 4 * pedestrians.size(), errcode_ret);
			clPositions = clCreateBuffer(clContext, CL_MEM_READ_WRITE, COORDOFFSET * 4 * pedestrians.size(), errcode_ret);
			clHashes = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * power, errcode_ret);
			clIndices = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * power, errcode_ret);
			clReorderedPedestrians = clCreateBuffer(clContext, CL_MEM_READ_WRITE, OFFSET * 4 * pedestrians.size(), errcode_ret);
			clReorderedPositions = clCreateBuffer(clContext, CL_MEM_READ_WRITE, COORDOFFSET * 4 * pedestrians.size(), errcode_ret);

			clGlobalIndexIn = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * power, errcode_ret);
			clGlobalIndexOut = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * power, errcode_ret);

			clEnqueueWriteBuffer(clQueue, clPedestrians, true, 0, memPedestrians, null, null);
			clEnqueueWriteBuffer(clQueue, clPositions, true, 0, memPositions, null, null);
			memNextPositions = MemoryUtil.memAllocFloat(numberOfElements * COORDOFFSET);

			memIndices = CLUtils.toIntBuffer(indices);
			clEnqueueWriteBuffer(clQueue, clIndices, true, 0, memIndices, null, null);
			clEnqueueWriteBuffer(clQueue, clGlobalIndexIn, true, 0, memIndices, null, null);

			pedestrianSet = true;
		}
		MemoryUtil.memFree(memPedestrians);
		MemoryUtil.memFree(memPositions);
	}

	/**
	 * Allocates the device memory for objects which do not change during the simulation e.g. the static potential field.
	 * Therefore this initialization is done once for a simulation.
	 */
	protected void allocGlobalDeviceMemory() {
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
				int[] neg = new int[numberOfGridCells];
				Arrays.fill(neg, -1);
				IntBuffer start = CLUtils.toIntBuffer(neg);
				IntBuffer end = CLUtils.toIntBuffer(neg);
				clCellStarts = clCreateBuffer(clContext, CL_MEM_READ_WRITE | CL_MEM_ALLOC_HOST_PTR | CL_MEM_COPY_HOST_PTR, start, errcode_ret);
				clCellEnds = clCreateBuffer(clContext, CL_MEM_READ_WRITE | CL_MEM_ALLOC_HOST_PTR | CL_MEM_COPY_HOST_PTR, end, errcode_ret);
				MemoryUtil.memFree(start);
				MemoryUtil.memFree(end);
			}
		}
	}

	protected void allocGlobalHostMemory() {
		if(counter == 0) {
			float[] circlePositions = new float[circlePositionList.size() * COORDOFFSET];
			for(int i = 0; i < circlePositionList.size(); i++) {
				circlePositions[i * COORDOFFSET + 0] = (float) circlePositionList.get(i).getX();
				circlePositions[i * COORDOFFSET + 1] = (float) circlePositionList.get(i).getY();
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

	protected abstract FloatBuffer allocPedestrianHostMemory(@NotNull final List<PedestrianOSM> pedestrians);

	protected abstract FloatBuffer allocPositionHostMemory(@NotNull final List<PedestrianOSM> pedestrians);

	protected void clearIterationDeviceMemory() throws OpenCLException {
		freeCLMemory(clPedestrians);
		freeCLMemory(clPositions);
		freeCLMemory(clHashes);
		freeCLMemory(clIndices);
		freeCLMemory(clReorderedPedestrians);
		freeCLMemory(clReorderedPositions);
		freeCLMemory(clGlobalIndexOut);
		freeCLMemory(clGlobalIndexIn);
	}

	@Override
	protected void clearMemory() throws OpenCLException {
		// release memory and devices
		try {
			if(pedestrianSet) {
				CLInfo.checkCLError(clReleaseMemObject(clPedestrians));
				CLInfo.checkCLError(clReleaseMemObject(clPositions));
				CLInfo.checkCLError(clReleaseMemObject(clHashes));
				CLInfo.checkCLError(clReleaseMemObject(clIndices));
				CLInfo.checkCLError(clReleaseMemObject(clReorderedPedestrians));
				CLInfo.checkCLError(clReleaseMemObject(clReorderedPositions));
				CLInfo.checkCLError(clReleaseMemObject(clGlobalIndexIn));
				CLInfo.checkCLError(clReleaseMemObject(clGlobalIndexOut));
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

	@Override
	public void readFromDevice() {
		clEnqueueReadBuffer(clQueue, clPositions, true, 0, memNextPositions, null, null);
		clEnqueueReadBuffer(clQueue, swap ? clGlobalIndexOut : clGlobalIndexIn, true, 0, memIndices, null, null);
		//clEnqueueReadBuffer(clQueue, clEventTimesData, true, 0, memEventTimes, null, null);
		clFinish(clQueue);

		positions = new ArrayList<>(numberOfElements);
		CLAbstractOSM.fill(positions, VPoint.ZERO, numberOfElements);
		indices = CLUtils.toIntArray(memIndices, numberOfElements);
		float[] positionsAndRadi = CLUtils.toFloatArray(memNextPositions, numberOfElements * COORDOFFSET);
		for(int i = 0; i < numberOfElements; i++) {
			float x = positionsAndRadi[i * COORDOFFSET + X];
			float y = positionsAndRadi[i * COORDOFFSET + Y];
			VPoint newPosition = new VPoint(x,y);
			positions.set(indices[i], newPosition);
		}

		/*eventTimes = new float[numberOfElements];
		float[] tmp = CLUtils.toFloatArray(memEventTimes, numberOfElements);
		for(int i = 0; i < numberOfElements; i++) {
			eventTimes[indices[i]] = tmp[i];
			//System.out.println("evac-time: " + eventTimes[indices[i]]);
		}*/
	}

	@Override
	public List<VPoint> getPositions() {
		return positions;
	}

	/*public float[] getEventTimes() {
		return eventTimes;
	}*/

	public static <T> void fill(@NotNull final List<T> list, @NotNull T element, final int n) {
		assert list.isEmpty() && n >= 0;
		for(int i = 0; i < n; i++) {
			list.add(element);
		}
	}

	protected int getPotentialFieldWidth() {
		return (int) Math.ceil(bound.getWidth() / attributesFloorField.getPotentialFieldResolution()) + 1;
	}

	protected int getPotentialFieldHeight() {
		return (int) Math.ceil(bound.getHeight() / attributesFloorField.getPotentialFieldResolution()) + 1;
	}

	protected int getPotentialFieldSize() {
		return getPotentialFieldWidth() * getPotentialFieldHeight();
	}

	protected FloatBuffer generatePotentialFieldApproximation(@NotNull final EikonalSolver eikonalSolver) {
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

	// kernel calls
	protected void clMemSet(final long clData, final int val, final int len) throws OpenCLException {
		try (MemoryStack stack = stackPush()) {
			PointerBuffer clGlobalWorkSize = stack.callocPointer(1);
			CLInfo.checkCLError(clSetKernelArg1p(clResetCells, 0, clData));
			CLInfo.checkCLError(clSetKernelArg1i(clResetCells, 1, val));
			CLInfo.checkCLError(clSetKernelArg1i(clResetCells, 2, len));
			clGlobalWorkSize.put(0, len);
			//TODO: local work size?
			CLInfo.checkCLError((int)enqueueNDRangeKernel("clMemSet", clQueue, clResetCells, 1, null, clGlobalWorkSize, null, null, null));
		}
	}

	@Override
	protected void releaseKernels() throws OpenCLException {
		CLInfo.checkCLError(clReleaseKernel(clBitonicSortLocal));
		CLInfo.checkCLError(clReleaseKernel(clBitonicSortLocal1));
		CLInfo.checkCLError(clReleaseKernel(clBitonicMergeGlobal));
		CLInfo.checkCLError(clReleaseKernel(clBitonicMergeLocal));
		CLInfo.checkCLError(clReleaseKernel(clCalcHash));
		CLInfo.checkCLError(clReleaseKernel(clResetCells));
		CLInfo.checkCLError(clReleaseKernel(clSwapIndex));
	}

	@Override
	protected void buildKernels() throws OpenCLException {
		try (MemoryStack stack = stackPush()) {
			IntBuffer errcode_ret = stack.callocInt(1);
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
			clSwapIndex = clCreateKernel(clProgram, "swapIndex", errcode_ret);
			CLInfo.checkCLError(errcode_ret);
			clResetCells = clCreateKernel(clProgram, "setMem", errcode_ret);
			CLInfo.checkCLError(errcode_ret);
		}
	}

	protected void clSwapIndex(
			final long clGlobalIndexOut,
			final long clGlobalIndexIn,
			final long clIndices,
			final int numberOfElements) throws OpenCLException {

		try (MemoryStack stack = stackPush()) {
			PointerBuffer clGlobalWorkSize = stack.callocPointer(1);
			clGlobalWorkSize.put(0, numberOfElements);

			CLInfo.checkCLError(clSetKernelArg1p(clSwapIndex, 0, clGlobalIndexOut));
			CLInfo.checkCLError(clSetKernelArg1p(clSwapIndex, 1, clGlobalIndexIn));
			CLInfo.checkCLError(clSetKernelArg1p(clSwapIndex, 2, clIndices));
			CLInfo.checkCLError(clSetKernelArg1i(clSwapIndex, 3, numberOfElements));
			CLInfo.checkCLError((int)enqueueNDRangeKernel("clSwapIndices", clQueue, clSwapIndex, 1, null, clGlobalWorkSize, null, null, null));
		}
	}

	protected void clCalcHash(
			final long clHashes,
			final long clIndices,
			final long clPositions,
			final long clCellSize,
			final long clWorldOrigin,
			final long clGridSize,
			final int numberOfElements,
			final int numberOfElementsPower) throws OpenCLException {
		try (MemoryStack stack = stackPush()) {
			PointerBuffer clGlobalWorkSize = stack.callocPointer(1);
			CLInfo.checkCLError(clSetKernelArg1p(clCalcHash, 0, clHashes));
			CLInfo.checkCLError(clSetKernelArg1p(clCalcHash, 1, clIndices));
			CLInfo.checkCLError(clSetKernelArg1p(clCalcHash, 2, clPositions));
			CLInfo.checkCLError(clSetKernelArg1p(clCalcHash, 3, clCellSize));
			CLInfo.checkCLError(clSetKernelArg1p(clCalcHash, 4, clWorldOrigin));
			CLInfo.checkCLError(clSetKernelArg1p(clCalcHash, 5, clGridSize));
			CLInfo.checkCLError(clSetKernelArg1i(clCalcHash, 6, numberOfElements));
			clGlobalWorkSize.put(0, numberOfElementsPower);
			//TODO: local work size?
			CLInfo.checkCLError((int)enqueueNDRangeKernel("clCalcHash", clQueue, clCalcHash, 1, null, clGlobalWorkSize, null, null, null));
		}
	}

	protected void clBitonicSort(
			final long clKeysIn,
			final long clValuesIn,
			final long clKeysOut,
			final long clValuesOut,
			final int numberOfElements,
			final int dir) throws OpenCLException {

		try (MemoryStack stack = stackPush()) {
			long[] runtime = new long[1];
			PointerBuffer clGlobalWorkSize = stack.callocPointer(1);
			PointerBuffer clLocalWorkSize = stack.callocPointer(1);
			IntBuffer errcode_ret = stack.callocInt(1);
			long maxWorkGroupSize = CLUtils.getMaxWorkGroupSizeForKernel(clDevice, clBitonicSortLocal, 8, getMaxWorkGroupSize(), getMaxLocalMemorySize()); // local memory for key and values (integer)

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
				CLInfo.checkCLError((int)enqueueNDRangeKernel("clBitonicSortLocal", clQueue, clBitonicSortLocal, 1, null, clGlobalWorkSize, clLocalWorkSize, null, null, runtime, false));
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

				CLInfo.checkCLError((int)enqueueNDRangeKernel("clBitonicSortLocal", clQueue, clBitonicSortLocal1, 1, null, clGlobalWorkSize, clLocalWorkSize, null, null,  runtime, false));
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

							CLInfo.checkCLError((int)enqueueNDRangeKernel("clBitonicMergeGlobal", clQueue, clBitonicMergeGlobal, 1, null, clGlobalWorkSize, clLocalWorkSize, null, null, runtime, false));
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

							CLInfo.checkCLError((int)enqueueNDRangeKernel("clBitonicMergeLocal", clQueue, clBitonicMergeLocal, 1, null, clGlobalWorkSize, clLocalWorkSize, null, null, runtime, false));
							CLInfo.checkCLError(clFinish(clQueue));
							break;
						}
					}
				}
			}
			logger.debug("sorting required: " + toMillis(runtime[0]));
		}
	}
}
