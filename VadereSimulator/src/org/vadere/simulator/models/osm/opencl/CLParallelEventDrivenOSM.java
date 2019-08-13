package org.vadere.simulator.models.osm.opencl;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.models.AttributesOSM;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.logging.Logger;
import org.vadere.util.opencl.CLInfo;
import org.vadere.util.opencl.CLUtils;
import org.vadere.util.opencl.OpenCLException;
import org.vadere.simulator.models.potential.solver.calculators.EikonalSolver;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import static org.lwjgl.opencl.CL10.CL_DEVICE_TYPE_GPU;
import static org.lwjgl.opencl.CL10.CL_MEM_READ_WRITE;
import static org.lwjgl.opencl.CL10.clBuildProgram;
import static org.lwjgl.opencl.CL10.clCreateBuffer;
import static org.lwjgl.opencl.CL10.clCreateCommandQueue;
import static org.lwjgl.opencl.CL10.clCreateContext;
import static org.lwjgl.opencl.CL10.clCreateKernel;
import static org.lwjgl.opencl.CL10.clCreateProgramWithSource;
import static org.lwjgl.opencl.CL10.clEnqueueCopyBuffer;
import static org.lwjgl.opencl.CL10.clEnqueueReadBuffer;
import static org.lwjgl.opencl.CL10.clEnqueueWriteBuffer;
import static org.lwjgl.opencl.CL10.clFinish;
import static org.lwjgl.opencl.CL10.clGetDeviceIDs;
import static org.lwjgl.opencl.CL10.clGetEventProfilingInfo;
import static org.lwjgl.opencl.CL10.clGetKernelWorkGroupInfo;
import static org.lwjgl.opencl.CL10.clGetPlatformIDs;
import static org.lwjgl.opencl.CL10.clGetProgramBuildInfo;
import static org.lwjgl.opencl.CL10.clReleaseKernel;
import static org.lwjgl.opencl.CL10.clReleaseMemObject;
import static org.lwjgl.opencl.CL10.clSetKernelArg;
import static org.lwjgl.opencl.CL10.clSetKernelArg1f;
import static org.lwjgl.opencl.CL10.clSetKernelArg1i;
import static org.lwjgl.opencl.CL10.clSetKernelArg1p;
import static org.lwjgl.opencl.CL10.clWaitForEvents;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memUTF8;

/**
 * @author Benedikt Zoennchen
 *
 * This class offers the methods to compute an array based linked-cell which contains 2D-coordinates i.e. {@link VPoint}
 * using the GPU (see. green-2007 Building the Grid using Sorting).
 */
public class CLParallelEventDrivenOSM extends CLAbstractOSM implements ICLOptimalStepsModel {
	private static Logger log = Logger.getLogger(CLParallelEventDrivenOSM.class);

	static {
		log.setDebug();
	}

	private long clReorderedEventTimes;
	private long clEventTimesData;
	private long clIds;
	private long clIdsOut;
	private long clMask;
	private long clMinEventTime;
	private long clPossiblePositions;
	private long clPossibleValues;

	// Host Memory to write update to the host
	private FloatBuffer memEventTimes;

	// CL kernel
	private long clFindCellBoundsAndReorder;
	private long clEventTimes;
	private long clEvalPoints;
	private long clSwap;
	private long clSwapIndex;
	private long clCalcMinEventTime;
	private long clFilterIds;
	private long clMove;
	private long clAlign;
	private long cl_scan_pow2;
	private long cl_scan_pad_to_pow2;
	private long cl_scan_subarrays;
	private long cl_scan_inc_subarrays;

	private float[] eventTimes;
	private int ids[];
	private float minEventTime = 0;

	private IntBuffer memIds;
	private int wx = 256; // workgroup size

	private long clData;
	int m = 2 * 256;     // length of each subarray ( = wx*2 )

	public CLParallelEventDrivenOSM(
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
	public CLParallelEventDrivenOSM(
			@NotNull final AttributesOSM attributesOSM,
			@NotNull final AttributesFloorField attributesFloorField,
			@NotNull final VRectangle bound,
			@NotNull final EikonalSolver targetPotential,
			@NotNull final EikonalSolver obstaclePotential,
			final int device,
			final double cellSize) throws OpenCLException {
		super(attributesOSM, attributesFloorField, bound, targetPotential, obstaclePotential, device, cellSize);
		this.eventTimes = new float[0];
		super.COORDOFFSET = 2;
		super.X = 0;
		super.Y = 1;
		super.OFFSET = 2;
		super.STEPSIZE = 0;
		super.DESIREDSPEED = 1;
		init("ParallelEventDrivenOSM.cl");
	}

	/**
	 * Set's new set of agents which we want to simulate. This will remove all other agents.
	 * This method will free old data from the device memory and transfer required data to the device
	 * as well as reserve new device memory.
	 *
	 * @param pedestrians       the list of pedestrians / agents
	 * @throws OpenCLException
	 */
	@Override
	public void setPedestrians(@NotNull final List<PedestrianOSM> pedestrians) throws OpenCLException {

		// clear the old memory before re-initialization
		if(pedestrianSet) {
			freeCLMemory(clEventTimesData);
			freeCLMemory(clReorderedEventTimes);
			freeCLMemory(clIds);
			freeCLMemory(clMinEventTime);
			freeCLMemory(clPossiblePositions);
			freeCLMemory(clPossibleValues);
			freeCLMemory(clData);
			MemoryUtil.memFree(memEventTimes);
			MemoryUtil.memFree(memIds);
		}

		try (MemoryStack stack = stackPush()) {
			IntBuffer errcode_ret = stack.callocInt(1);
			clReorderedEventTimes = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * pedestrians.size(), errcode_ret);
			clIds = clCreateBuffer(clContext, CL_MEM_READ_WRITE, iGridSize[0] * iGridSize[1] * 4, errcode_ret);
			clIdsOut = clCreateBuffer(clContext, CL_MEM_READ_WRITE, iGridSize[0] * iGridSize[1] * 4, errcode_ret);
			clMask = clCreateBuffer(clContext, CL_MEM_READ_WRITE, iGridSize[0] * iGridSize[1] * 4, errcode_ret);
			clEventTimesData = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * pedestrians.size(), errcode_ret);
			clMinEventTime = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4, errcode_ret);
			clPossiblePositions = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * pedestrians.size() * circlePositionList.size() * 2, errcode_ret);
			clPossibleValues = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * pedestrians.size() * circlePositionList.size(), errcode_ret);

			int k = (int) Math.ceil((float) (iGridSize[0] * iGridSize[1]+1) / (float) m);
			clData = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * k * m, errcode_ret);

			memIds = MemoryUtil.memAllocInt(iGridSize[0] * iGridSize[1]);
			memEventTimes = allocPedestrianEventTimeMemory(pedestrians);
			clEnqueueWriteBuffer(clQueue, clEventTimesData, true, 0, memEventTimes, null, null);
		}
		this.eventTimes = new float[pedestrians.size()];
		super.setPedestrians(pedestrians);
	}


	//TODO: dont sort if the size is <= 1!
	@Override
	public boolean update(float timeStepInSec, float simTimeInSec) throws OpenCLException {
		try (MemoryStack stack = stackPush()) {
			int numberOfUpdates = 0;

			allocGlobalHostMemory();
			allocGlobalDeviceMemory();

			clCalcHash(clHashes, clIndices, clPositions, clCellSize, clWorldOrigin, clGridSize, numberOfElements, numberOfSortElements);
			clBitonicSort(clHashes, clIndices, clHashes, clIndices, numberOfSortElements, 1);
			clFindCellBoundsAndReorder(
					clCellStarts,
					clCellEnds,
					clReorderedPedestrians,
					clReorderedPositions,
					clReorderedEventTimes,
					clHashes, clIndices, clPedestrians, clPositions, clEventTimesData, numberOfElements);

			IntBuffer memUpdates = stack.mallocInt( 1);

			do {
				long ms = 0;
				clEventTimes(
						clIds,
						clReorderedEventTimes,
						clCellStarts,
						clCellEnds);
				clFilterIds(clIds, clMask, clReorderedEventTimes, clGridSize, simTimeInSec);
				clFinish(clQueue);

				ms = System.nanoTime();
				CLInfo.checkCLError(clEnqueueCopyBuffer(clQueue, clMask, clData, 0, 0, iGridSize[0] * iGridSize[1] * 4, null, null));
				clFinish(clQueue);

				if(debug) {
					log.debug("copyBuffer1: " + (System.nanoTime() - ms));
				}

				clScan(clData, iGridSize[0] * iGridSize[1] + 1, 1);
				clFinish(clQueue);

				ms = System.nanoTime();
				clEnqueueReadBuffer(clQueue, clData, true, iGridSize[0] * iGridSize[1] * 4, memUpdates, null, null);
				numberOfUpdates = memUpdates.get(0);
				if(debug) {
					log.debug("readBuffer: " + (System.nanoTime() - ms));
				}

				if(numberOfUpdates > 0) {
					ms = System.nanoTime();
					CLInfo.checkCLError(clEnqueueCopyBuffer(clQueue, clMask, clData, 0, 0, iGridSize[0] * iGridSize[1] * 4, null, null));
					clFinish(clQueue);
					if(debug) {
						log.debug("copyBuffer2: " + (System.nanoTime() - ms));
					}

					clScan(clData, iGridSize[0] * iGridSize[1] + 1, -1);
					clAlign(clIds, clData, clMask, clIdsOut);
					clEvalPoints(
							clReorderedPedestrians,
							clReorderedPositions,
							clReorderedEventTimes,
							clPossiblePositions,
							clPossibleValues,
							clIdsOut,
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
							numberOfUpdates);
					clMove(
							clReorderedPositions,
							clPossiblePositions,
							clPossibleValues,
							clIdsOut,
							numberOfUpdates);

					if(debug) {
						log.debug("#updated agents: " + numberOfUpdates);
					}
				}
				counter++;

			} while (numberOfUpdates > 0);

			long clGlobalIndexOut = !swap ? this.clGlobalIndexOut : this.clGlobalIndexIn;
			long clGlobalIndexIn = !swap ? this.clGlobalIndexIn : this.clGlobalIndexOut;

			clSwap(
					clReorderedPedestrians,
					clReorderedPositions,
					clReorderedEventTimes,
					clPedestrians,
					clPositions,
					clEventTimesData,
					numberOfElements);
			clSwapIndex(
					clGlobalIndexOut,
					clGlobalIndexIn,
					clIndices,
					numberOfElements);
			clMemSet(clCellStarts, -1, iGridSize[0] * iGridSize[1]);
			clMemSet(clCellEnds, -1, iGridSize[0] * iGridSize[1]);
			clFinish(clQueue);
			swap = !swap;
		}
		return false;
	}

	@Override
	public void readFromDevice() {
		super.readFromDevice();
		clEnqueueReadBuffer(clQueue, clEventTimesData, true, 0, memEventTimes, null, null);
		clFinish(clQueue);

		eventTimes = new float[numberOfElements];
		float[] tmp = CLUtils.toFloatArray(memEventTimes, numberOfElements);
		for(int i = 0; i < numberOfElements; i++) {
			eventTimes[indices[i]] = tmp[i];
		}
	}

	public float getMinEventTime() {
		return minEventTime;
	}

	public int getCounter() {
		return counter;
	}

	@Override
	public List<VPoint> getPositions() {
		return positions;
	}

	@Override
	public float[] getTimeCredits() {
		return new float[0];
	}

	public float[] getEventTimes() {
		/*System.out.println("event times");
		for(float et : eventTimes) {
			System.out.println(et);
		}*/
		return eventTimes;
	}

	/**
	 * Transforms the a list of {@link PedestrianOSM} into a {@link FloatBuffer} i.e. a array
	 * @param pedestrians
	 * @return
	 */
	@Override
	protected FloatBuffer allocPedestrianHostMemory(@NotNull final List<PedestrianOSM> pedestrians) {
		float[] pedestrianStruct = new float[pedestrians.size() * OFFSET];
		for(int i = 0; i < pedestrians.size(); i++) {
			pedestrianStruct[i * OFFSET + STEPSIZE] = (float) pedestrians.get(i).getDesiredStepSize();
			pedestrianStruct[i * OFFSET + DESIREDSPEED] = (float) pedestrians.get(i).getDesiredSpeed();
		}
		return CLUtils.toFloatBuffer(pedestrianStruct);
	}

	private FloatBuffer allocPedestrianEventTimeMemory(@NotNull final List<PedestrianOSM> pedestrians) {
		float[] pedestrianStruct = new float[pedestrians.size()];
		for(int i = 0; i < pedestrians.size(); i++) {
			pedestrianStruct[i] = 0.0f;
		}
		return CLUtils.toFloatBuffer(pedestrianStruct);
	}

	@Override
	protected FloatBuffer allocPositionHostMemory(@NotNull final List<PedestrianOSM> pedestrians) {
		float[] pedestrianStruct = new float[pedestrians.size() * COORDOFFSET];
		for(int i = 0; i < pedestrians.size(); i++) {
			pedestrianStruct[i * COORDOFFSET + X] = (float) pedestrians.get(i).getPosition().getX();
			pedestrianStruct[i * COORDOFFSET + Y] = (float) pedestrians.get(i).getPosition().getY();
		}
		return CLUtils.toFloatBuffer(pedestrianStruct);
	}

	/**
	 * Allocates the host memory for objects which do not change during the simulation e.g. the static potential field.
	 * Therefore this initialization is done once for a simulation.
	 */
	@Override
	protected void allocGlobalHostMemory() {
		super.allocGlobalHostMemory();
	}

	/**
	 * Allocates the device memory for objects which do not change during the simulation e.g. the static potential field.
	 * Therefore this initialization is done once for a simulation.
	 */
	protected void allocGlobalDeviceMemory() {
		super.allocGlobalDeviceMemory();
	}

	private void clEvalPoints(
			final long clReorderedPedestrians,
			final long clReorderedPositions,
			final long clReorderedEventTimes,
			final long clPossiblePositions,
			final long clPossibleValues,
			final long clIds,
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
			//long maxWorkGroupSize = CLUtils.getMaxWorkGroupSizeForKernel(clDevice, clEvalPoints, 0, max_work_group_size, max_local_memory_size); // local 4 byte (integer)

			CLInfo.checkCLError(clSetKernelArg1p(clEvalPoints, 0, clReorderedPedestrians));
			CLInfo.checkCLError(clSetKernelArg1p(clEvalPoints, 1, clReorderedPositions));
			CLInfo.checkCLError(clSetKernelArg1p(clEvalPoints, 2, clReorderedEventTimes));
			CLInfo.checkCLError(clSetKernelArg1p(clEvalPoints, 3, clPossiblePositions));
			CLInfo.checkCLError(clSetKernelArg1p(clEvalPoints, 4, clPossibleValues));
			CLInfo.checkCLError(clSetKernelArg1p(clEvalPoints, 5, clIds));
			CLInfo.checkCLError(clSetKernelArg1p(clEvalPoints, 6, clCirclePositions));
			CLInfo.checkCLError(clSetKernelArg1p(clEvalPoints, 7, clCellStarts));
			CLInfo.checkCLError(clSetKernelArg1p(clEvalPoints, 8, clCellEnds));
			CLInfo.checkCLError(clSetKernelArg1p(clEvalPoints, 9, clCellSize));
			CLInfo.checkCLError(clSetKernelArg1p(clEvalPoints, 10, clGridSize));
			CLInfo.checkCLError(clSetKernelArg1p(clEvalPoints, 11, clObstaclePotential));
			CLInfo.checkCLError(clSetKernelArg1p(clEvalPoints, 12, clTargetPotential));
			CLInfo.checkCLError(clSetKernelArg1p(clEvalPoints, 13, clWorldOrigin));
			CLInfo.checkCLError(clSetKernelArg1p(clEvalPoints, 14, clPotentialFieldGridSize));
			CLInfo.checkCLError(clSetKernelArg1p(clEvalPoints, 15, clPotentialFieldSize));
			CLInfo.checkCLError(clSetKernelArg1f(clEvalPoints, 16, (float)attributesFloorField.getPotentialFieldResolution()));
			CLInfo.checkCLError(clSetKernelArg1i(clEvalPoints, 17, circlePositionList.size()));
			CLInfo.checkCLError(clSetKernelArg1i(clEvalPoints, 18, numberOfElements));

			long globalWorkSize;
			globalWorkSize = numberOfElements * circlePositionList.size();
			clGlobalWorkSize.put(0, globalWorkSize);

			//TODO: local work size? + check 2^n constrain!
			CLInfo.checkCLError((int)enqueueNDRangeKernel("clEvalPoints", clQueue, clEvalPoints, 1, null, clGlobalWorkSize, null, null, null));
		}
	}
	/*// each work group is used for exactly one agent!
__kernel void move (
    __global float          *orderedPositions,      //input
    __global float          *argValues,             // in
    __global float          *values,                // in
    __global int            *ids) {*/

	private void clMove(
			final long clReorderedPedestrians,
			final long clPossiblePositions,
			final long clPossibleValues,
			final long clIds,
			final long numberOfElements
	) throws OpenCLException {
		try (MemoryStack stack = stackPush()) {
			PointerBuffer clGlobalWorkSize = stack.callocPointer(1);
			PointerBuffer clLocalWorkSize = stack.callocPointer(1);

			clGlobalWorkSize.put(0, numberOfElements * circlePositionList.size());
			clLocalWorkSize.put(0, circlePositionList.size());

			CLInfo.checkCLError(clSetKernelArg1p(clMove, 0, clReorderedPedestrians));
			CLInfo.checkCLError(clSetKernelArg1p(clMove, 1, clPossiblePositions));
			CLInfo.checkCLError(clSetKernelArg1p(clMove, 2, clPossibleValues));
			CLInfo.checkCLError(clSetKernelArg1p(clMove, 3, clIds));

			// local memory
			CLInfo.checkCLError(clSetKernelArg(clMove, 4, circlePositionList.size() * 4 * 2));
			CLInfo.checkCLError(clSetKernelArg(clMove, 5, circlePositionList.size() * 4));
			CLInfo.checkCLError((int)enqueueNDRangeKernel("clMove", clQueue, clMove, 1, null, clGlobalWorkSize, clLocalWorkSize, null, null));
		}
	}


	private void clSwap(
			final long clReorderedPedestrians,
			final long clReorderedPositions,
			final long clReorderedEventTimes,
			final long clPedestrians,
			final long clPositions,
			final long clEventTimes,
			final int numberOfElements) throws OpenCLException {

		try (MemoryStack stack = stackPush()) {
			PointerBuffer clGlobalWorkSize = stack.callocPointer(1);
			clGlobalWorkSize.put(0, numberOfElements);

			CLInfo.checkCLError(clSetKernelArg1p(clSwap, 0, clReorderedPedestrians));
			CLInfo.checkCLError(clSetKernelArg1p(clSwap, 1, clReorderedPositions));
			CLInfo.checkCLError(clSetKernelArg1p(clSwap, 2, clReorderedEventTimes));
			CLInfo.checkCLError(clSetKernelArg1p(clSwap, 3, clPedestrians));
			CLInfo.checkCLError(clSetKernelArg1p(clSwap, 4, clPositions));
			CLInfo.checkCLError(clSetKernelArg1p(clSwap, 5, clEventTimes));
			CLInfo.checkCLError(clSetKernelArg1i(clSwap, 6, numberOfElements));
			CLInfo.checkCLError((int)enqueueNDRangeKernel("clSwap", clQueue, clSwap, 1, null, clGlobalWorkSize, null, null, null));
		}
	}

	/*
	__kernel void minEventTimeLocal(
    __global float* minEventTime,
    __global float* eventTimes,
    __local  float* local_eventTimes,
    uint numberOfElements
	 */

	private void clMinEventTime(
			final long clMinEventTime,
			final long clEventTimes,
			final int numberOfElements) throws OpenCLException {

		try (MemoryStack stack = stackPush()) {
			PointerBuffer clGlobalWorkSize = stack.callocPointer(1);
			PointerBuffer clLocalWorkSize = stack.callocPointer(1);
			IntBuffer errcode_ret = stack.callocInt(1);
			long maxWorkGroupSize = CLUtils.getMaxWorkGroupSizeForKernel(clDevice, clFindCellBoundsAndReorder, 4, getMaxWorkGroupSize(), getMaxLocalMemorySize()); // local 4 byte (integer)
			clGlobalWorkSize.put(0, Math.min(maxWorkGroupSize, numberOfElements));
			clLocalWorkSize.put(0, Math.min(maxWorkGroupSize, numberOfElements));

			CLInfo.checkCLError(clSetKernelArg1p(clCalcMinEventTime, 0, clMinEventTime));
			CLInfo.checkCLError(clSetKernelArg1p(clCalcMinEventTime, 1, clEventTimes));
			CLInfo.checkCLError(clSetKernelArg(clCalcMinEventTime, 2, maxWorkGroupSize * 4));
			CLInfo.checkCLError(clSetKernelArg1i(clCalcMinEventTime, 3, numberOfElements));
			CLInfo.checkCLError((int)enqueueNDRangeKernel("clCalcMinEventTime", clQueue, clCalcMinEventTime, 1, null, clGlobalWorkSize, clLocalWorkSize, null, null));
		}
	}

	private void clFilterIds(
			final long clIds,
			final long clIdsOut,
			final long clReorderedEventTimes,
			final long clGridSize,
			final float simTimeInSec) throws OpenCLException {

		try (MemoryStack stack = stackPush()) {
			PointerBuffer clGlobalWorkSize = stack.callocPointer(1);
			clGlobalWorkSize.put(0, iGridSize[0] * iGridSize[1]);

			CLInfo.checkCLError(clSetKernelArg1p(clFilterIds, 0, clIds));
			CLInfo.checkCLError(clSetKernelArg1p(clFilterIds, 1, clIdsOut));
			CLInfo.checkCLError(clSetKernelArg1p(clFilterIds, 2, clReorderedEventTimes));
			CLInfo.checkCLError(clSetKernelArg1p(clFilterIds, 3, clGridSize));
			CLInfo.checkCLError(clSetKernelArg1f(clFilterIds, 4, simTimeInSec));
			CLInfo.checkCLError((int)enqueueNDRangeKernel("clFilterIds", clQueue, clFilterIds, 1, null, clGlobalWorkSize, null, null, null));
		}
	}

	private void clEventTimes(
			final long clIds,
			final long clReorderedEventTimes,
			final long clCellStarts,
			final long clCellEnds) throws OpenCLException {

		try (MemoryStack stack = stackPush()) {
			PointerBuffer clGlobalWorkSize = stack.callocPointer(1);
			clGlobalWorkSize.put(0, iGridSize[0] * iGridSize[1]);

			CLInfo.checkCLError(clSetKernelArg1p(clEventTimes, 0, clIds));
			CLInfo.checkCLError(clSetKernelArg1p(clEventTimes, 1, clReorderedEventTimes));
			CLInfo.checkCLError(clSetKernelArg1p(clEventTimes, 2, clCellStarts));
			CLInfo.checkCLError(clSetKernelArg1p(clEventTimes, 3, clCellEnds));
			CLInfo.checkCLError((int)enqueueNDRangeKernel("clEventTimes", clQueue, clEventTimes, 1, null, clGlobalWorkSize, null, null, null));
		}
	}

	private void clFindCellBoundsAndReorder(
			final long clCellStarts,
			final long clCellEnds,
			final long clReorderedPedestrians,
			final long clReorderedPositions,
			final long clReorderedEventTimes,
			final long clHashes,
			final long clIndices,
			final long clPedestrians,
			final long clPositions,
			final long clEventTimes,
			final int numberOfElements) throws OpenCLException {

		try (MemoryStack stack = stackPush()) {

			PointerBuffer clGlobalWorkSize = stack.callocPointer(1);
			PointerBuffer clLocalWorkSize = stack.callocPointer(1);
			IntBuffer errcode_ret = stack.callocInt(1);
			long maxWorkGroupSize = CLUtils.getMaxWorkGroupSizeForKernel(clDevice, clFindCellBoundsAndReorder, 0, getMaxWorkGroupSize(), getMaxLocalMemorySize()); // local 4 byte (integer)

			CLInfo.checkCLError(clSetKernelArg1p(clFindCellBoundsAndReorder, 0, clCellStarts));
			CLInfo.checkCLError(clSetKernelArg1p(clFindCellBoundsAndReorder, 1, clCellEnds));
			CLInfo.checkCLError(clSetKernelArg1p(clFindCellBoundsAndReorder, 2, clReorderedPedestrians));
			CLInfo.checkCLError(clSetKernelArg1p(clFindCellBoundsAndReorder, 3, clReorderedPositions));
			CLInfo.checkCLError(clSetKernelArg1p(clFindCellBoundsAndReorder, 4, clReorderedEventTimes));
			CLInfo.checkCLError(clSetKernelArg1p(clFindCellBoundsAndReorder, 5, clHashes));
			CLInfo.checkCLError(clSetKernelArg1p(clFindCellBoundsAndReorder, 6, clIndices));
			CLInfo.checkCLError(clSetKernelArg1p(clFindCellBoundsAndReorder, 7, clPedestrians));
			CLInfo.checkCLError(clSetKernelArg1p(clFindCellBoundsAndReorder, 8, clPositions));
			CLInfo.checkCLError(clSetKernelArg1p(clFindCellBoundsAndReorder, 9, clEventTimes));
			CLInfo.checkCLError(clSetKernelArg(clFindCellBoundsAndReorder, 10, (Math.min(numberOfElements+1, maxWorkGroupSize)) * 4)); // local memory
			CLInfo.checkCLError(clSetKernelArg1i(clFindCellBoundsAndReorder, 11, numberOfElements));

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

	private void recursive_scan(final long clData, int n, int dir) throws OpenCLException {
		int k = (int) Math.ceil((float) n / (float) m);
		long bufsize = 4 * m;

		// everything fits into one work group
		try (MemoryStack stack = stackPush()) {
			IntBuffer errcode_ret = stack.callocInt(1);
			if (k == 1) {
				PointerBuffer clGlobalWorkSize = stack.callocPointer(1);
				PointerBuffer clLocalWorkSize = stack.callocPointer(1);
				clGlobalWorkSize.put(0, wx);
				clLocalWorkSize.put(0, wx);
				CLInfo.checkCLError(clSetKernelArg1p(cl_scan_pad_to_pow2, 0, clData));
				CLInfo.checkCLError(clSetKernelArg(cl_scan_pad_to_pow2, 1, bufsize));
				CLInfo.checkCLError(clSetKernelArg1i(cl_scan_pad_to_pow2, 2, n));
				CLInfo.checkCLError(clSetKernelArg1i(cl_scan_pad_to_pow2, 3, dir));
				CLInfo.checkCLError((int) enqueueNDRangeKernel("cl_scan_pad_to_pow2", clQueue, cl_scan_pad_to_pow2, 1, null, clGlobalWorkSize, clLocalWorkSize, null, null));

			} // use multiple work groups
			else {
				long gx = k * wx;
				long clPartial = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * k, errcode_ret);

				CLInfo.checkCLError(clSetKernelArg1p(cl_scan_subarrays, 0, clData));
				CLInfo.checkCLError(clSetKernelArg(cl_scan_subarrays, 1, bufsize));
				CLInfo.checkCLError(clSetKernelArg1p(cl_scan_subarrays, 2, clPartial));
				CLInfo.checkCLError(clSetKernelArg1i(cl_scan_subarrays, 3, n));
				CLInfo.checkCLError(clSetKernelArg1i(cl_scan_subarrays, 4, dir));
				PointerBuffer clGlobalWorkSize = stack.callocPointer(1);
				PointerBuffer clLocalWorkSize = stack.callocPointer(1);
				clGlobalWorkSize.put(0, gx);
				clLocalWorkSize.put(0, wx);
				CLInfo.checkCLError((int) enqueueNDRangeKernel("cl_scan_subarrays", clQueue, cl_scan_subarrays, 1, null, clGlobalWorkSize, clLocalWorkSize, null, null));

				recursive_scan(clPartial, k, dir);

				CLInfo.checkCLError(clSetKernelArg1p(cl_scan_inc_subarrays, 0, clData));
				CLInfo.checkCLError(clSetKernelArg(cl_scan_inc_subarrays, 1, bufsize));
				CLInfo.checkCLError(clSetKernelArg1p(cl_scan_inc_subarrays, 2, clPartial));
				CLInfo.checkCLError(clSetKernelArg1i(cl_scan_inc_subarrays, 3, n));
				CLInfo.checkCLError(clSetKernelArg1i(cl_scan_inc_subarrays, 4, dir));
				PointerBuffer clGlobalWorkSize2 = stack.callocPointer(1);
				PointerBuffer clLocalWorkSize2 = stack.callocPointer(1);
				clGlobalWorkSize2.put(0, gx);
				clLocalWorkSize2.put(0, wx);
				CLInfo.checkCLError((int) enqueueNDRangeKernel("cl_scan_inc_subarrays", clQueue, cl_scan_inc_subarrays, 1, null, clGlobalWorkSize2, clLocalWorkSize2, null, null));
				freeCLMemory(clPartial);
			}
		}
	}

	private void clAlign(long clIds, long clPrefixSum, long clMask, long clIdsOut) throws OpenCLException {
		try (MemoryStack stack = stackPush()) {
			PointerBuffer clGlobalWorkSize = stack.callocPointer(1);
			clGlobalWorkSize.put(0, iGridSize[0] * iGridSize[1]);

			CLInfo.checkCLError(clSetKernelArg1p(clAlign, 0, clIds));
			CLInfo.checkCLError(clSetKernelArg1p(clAlign, 1, clPrefixSum));
			CLInfo.checkCLError(clSetKernelArg1p(clAlign, 2, clMask));
			CLInfo.checkCLError(clSetKernelArg1p(clAlign, 3, clIdsOut));

			CLInfo.checkCLError((int)enqueueNDRangeKernel("clAlign", clQueue, clAlign, 1, null, clGlobalWorkSize, null, null, null));
		}
	}

	private void clScan(long clData, int n, int dir) throws OpenCLException {
		recursive_scan(clData, n, dir);
	}

	protected void clearMemory() throws OpenCLException {
		super.clearMemory();
		// release memory and devices
		try {
			if(pedestrianSet) {
				CLInfo.checkCLError(clReleaseMemObject(clEventTimesData));
				CLInfo.checkCLError(clReleaseMemObject(clIds));
				CLInfo.checkCLError(clReleaseMemObject(clIdsOut));
				CLInfo.checkCLError(clReleaseMemObject(clMask));
				CLInfo.checkCLError(clReleaseMemObject(clReorderedEventTimes));
				CLInfo.checkCLError(clReleaseMemObject(clMinEventTime));
				CLInfo.checkCLError(clReleaseMemObject(clPossiblePositions));
				CLInfo.checkCLError(clReleaseMemObject(clPossibleValues));
				CLInfo.checkCLError(clReleaseMemObject(clData));

			}
		}
		catch (OpenCLException ex) {
			throw ex;
		}
		finally {
			if(pedestrianSet) {
				MemoryUtil.memFree(memEventTimes);
				MemoryUtil.memFree(memIds);
			}
		}
	}

	@Override
	protected void releaseKernels() throws OpenCLException {
		super.releaseKernels();
		CLInfo.checkCLError(clReleaseKernel(clFindCellBoundsAndReorder));
		CLInfo.checkCLError(clReleaseKernel(clEventTimes));
		CLInfo.checkCLError(clReleaseKernel(clEvalPoints));
		CLInfo.checkCLError(clReleaseKernel(clSwap));
		CLInfo.checkCLError(clReleaseKernel(clSwapIndex));
		CLInfo.checkCLError(clReleaseKernel(clCalcMinEventTime));
		CLInfo.checkCLError(clReleaseKernel(clFilterIds));
		CLInfo.checkCLError(clReleaseKernel(clMove));
		CLInfo.checkCLError(clReleaseKernel(clAlign));
		CLInfo.checkCLError(clReleaseMemObject(cl_scan_pow2));
		CLInfo.checkCLError(clReleaseMemObject(cl_scan_pad_to_pow2));
		CLInfo.checkCLError(clReleaseMemObject(cl_scan_subarrays));
		CLInfo.checkCLError(clReleaseMemObject(cl_scan_inc_subarrays));
	}

	@Override
	protected void buildKernels() throws OpenCLException {
		super.buildKernels();
		try (MemoryStack stack = stackPush()) {
			IntBuffer errcode_ret = stack.callocInt(1);

			clFindCellBoundsAndReorder = clCreateKernel(clProgram, "findCellBoundsAndReorder", errcode_ret);
			CLInfo.checkCLError(errcode_ret);

			clEventTimes = clCreateKernel(clProgram, "eventTimes", errcode_ret);
			CLInfo.checkCLError(errcode_ret);
			clEvalPoints = clCreateKernel(clProgram, "evalPoints", errcode_ret);
			CLInfo.checkCLError(errcode_ret);
			clMove = clCreateKernel(clProgram, "move", errcode_ret);
			CLInfo.checkCLError(errcode_ret);
			clSwap = clCreateKernel(clProgram, "swap", errcode_ret);
			CLInfo.checkCLError(errcode_ret);
			clSwapIndex = clCreateKernel(clProgram, "swapIndex", errcode_ret);
			CLInfo.checkCLError(errcode_ret);
			clCalcMinEventTime = clCreateKernel(clProgram, "minEventTimeLocal", errcode_ret);
			CLInfo.checkCLError(errcode_ret);
			clFilterIds = clCreateKernel(clProgram, "filterIds", errcode_ret);
			CLInfo.checkCLError(errcode_ret);
			clAlign = clCreateKernel(clProgram, "align", errcode_ret);
			CLInfo.checkCLError(errcode_ret);


			cl_scan_pow2 = clCreateKernel(clProgram, "scan_pow2_wrapper", errcode_ret);
			CLInfo.checkCLError(errcode_ret);
			cl_scan_pad_to_pow2 = clCreateKernel(clProgram, "scan_pad_to_pow2", errcode_ret);
			CLInfo.checkCLError(errcode_ret);
			cl_scan_subarrays = clCreateKernel(clProgram, "scan_subarrays", errcode_ret);
			CLInfo.checkCLError(errcode_ret);
			cl_scan_inc_subarrays = clCreateKernel(clProgram, "scan_inc_subarrays", errcode_ret);
			CLInfo.checkCLError(errcode_ret);
		}
	}
}
