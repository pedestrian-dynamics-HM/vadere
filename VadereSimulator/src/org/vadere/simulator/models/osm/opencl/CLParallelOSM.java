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
import static org.lwjgl.opencl.CL10.clCreateBuffer;
import static org.lwjgl.opencl.CL10.clCreateKernel;
import static org.lwjgl.opencl.CL10.clEnqueueReadBuffer;
import static org.lwjgl.opencl.CL10.clEnqueueWriteBuffer;
import static org.lwjgl.opencl.CL10.clFinish;
import static org.lwjgl.opencl.CL10.clReleaseKernel;
import static org.lwjgl.opencl.CL10.clSetKernelArg;
import static org.lwjgl.opencl.CL10.clSetKernelArg1f;
import static org.lwjgl.opencl.CL10.clSetKernelArg1i;
import static org.lwjgl.opencl.CL10.clSetKernelArg1p;
import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * @author Benedikt Zoennchen
 *
 * This class offers the methods to compute an array based linked-cell which contains 2D-coordinates i.e. {@link VPoint}
 * using the GPU (see. green-2007 Building the Grid using Sorting).
 */
public class CLParallelOSM extends CLAbstractOSM implements ICLOptimalStepsModel {

	private static final Logger logger = Logger.getLogger(CLParallelOSM.class);

	static {
		logger.setDebug();
	}

	// kernels
	private long clFindCellBoundsAndReorder;
    private long clSeek;
    private long clMove;
    private long clSwap;
	private long clCalcMinTimeCredit;

	// device memory
	private long clTimeCredit;
	private long clReorderedTimeCredit;
	private long clMinTimeCredit;

	public CLParallelOSM(
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
    public CLParallelOSM(
		    @NotNull final AttributesOSM attributesOSM,
		    @NotNull final AttributesFloorField attributesFloorField,
		    @NotNull final VRectangle bound,
		    @NotNull final EikonalSolver targetPotential,
		    @NotNull final EikonalSolver obstaclePotential,
			final int device,
		    final double cellSize) throws OpenCLException {
	    super(attributesOSM, attributesFloorField, bound, targetPotential, obstaclePotential, device, cellSize);
	    init("ParallelOSM.cl");
    }

	@Override
	public void setPedestrians(@NotNull List<PedestrianOSM> pedestrians) throws OpenCLException {
		if(pedestrianSet) {
			freeCLMemory(clTimeCredit);
			freeCLMemory(clMinTimeCredit);
			freeCLMemory(clReorderedTimeCredit);
		}

		try (MemoryStack stack = stackPush()) {
			IntBuffer errcode_ret = stack.callocInt(1);

			clTimeCredit = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * pedestrians.size(), errcode_ret);
			clReorderedTimeCredit = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * pedestrians.size(), errcode_ret);
			clMinTimeCredit = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4, errcode_ret);

			FloatBuffer memTimeCredits = allocTimeCreditMemory(pedestrians);
			clEnqueueWriteBuffer(clQueue, clTimeCredit, true, 0, memTimeCredits, null, null);
			MemoryUtil.memFree(memTimeCredits);
		}

		super.setPedestrians(pedestrians);
	}

	@Override
	public boolean update(float timeStepInSec, float currentTimeInSec) throws OpenCLException {
		try (MemoryStack stack = stackPush()) {
			allocGlobalHostMemory();
			allocGlobalDeviceMemory();
			long clGlobalIndexOut = !swap ? this.clGlobalIndexOut : this.clGlobalIndexIn;
			long clGlobalIndexIn = !swap ? this.clGlobalIndexIn : this.clGlobalIndexOut;
			clCalcHash(clHashes, clIndices, clPositions, clCellSize, clWorldOrigin, clGridSize, numberOfElements, numberOfSortElements);
			clBitonicSort(clHashes, clIndices, clHashes, clIndices, numberOfSortElements, 1);
			clFindCellBoundsAndReorder(clCellStarts, clCellEnds, clReorderedPedestrians, clReorderedPositions, clReorderedTimeCredit,
					clHashes, clIndices, clPedestrians, clPositions, clTimeCredit, numberOfElements);

			clSeek(
					clReorderedPedestrians,
					clReorderedPositions,
					clReorderedTimeCredit,
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
					timeStepInSec,
					numberOfElements);

			clMove(
					clReorderedPedestrians,
					clReorderedPositions,
					clReorderedTimeCredit,
					clCellStarts,
					clCellEnds,
					clCellSize,
					clGridSize,
					clWorldOrigin,
					numberOfElements);

			clSwap(
					clReorderedPedestrians,
					clReorderedPositions,
					clReorderedTimeCredit,
					clPedestrians,
					clPositions,
					clTimeCredit,
					numberOfElements);

			clCalcMinTimeCredit(
					clMinTimeCredit,
					clTimeCredit,
					numberOfElements
			);

			FloatBuffer minTimeCredit = stack.callocFloat(1);
			clEnqueueReadBuffer(clQueue, clMinTimeCredit, true, 0, minTimeCredit, null, null);

			clSwapIndex(
					clGlobalIndexOut,
					clGlobalIndexIn,
					clIndices,
					numberOfElements);

			clMemSet(clCellStarts, -1, iGridSize[0] * iGridSize[1]);
			clMemSet(clCellEnds, -1, iGridSize[0] * iGridSize[1]);

			clFinish(clQueue);

			counter++;
			swap = !swap;

			return minTimeCredit.get(0) < timeStepInSec;
		}
	}

	/**
	 * Transforms the a list of {@link PedestrianOSM} into a {@link FloatBuffer} i.e. a array
	 * @param pedestrians
	 * @return
	 */
	protected FloatBuffer allocPedestrianHostMemory(@NotNull final List<PedestrianOSM> pedestrians) {
	    float[] pedestrianStruct = new float[pedestrians.size() * OFFSET];
	    for(int i = 0; i < pedestrians.size(); i++) {
		    pedestrianStruct[i * OFFSET + STEPSIZE] = (float)pedestrians.get(i).getDesiredStepSize();
		    pedestrianStruct[i * OFFSET + DESIREDSPEED] = (float)pedestrians.get(i).getDesiredSpeed();

		    pedestrianStruct[i * OFFSET + NEWX] = 0.0f;
		    pedestrianStruct[i * OFFSET + NEWY] = 0.0f;
	    }
	    return CLUtils.toFloatBuffer(pedestrianStruct);
    }

    protected FloatBuffer allocTimeCreditMemory(@NotNull final List<PedestrianOSM> pedestrians) {
	    float[] timeCredits = new float[pedestrians.size()];
	    for(int i = 0; i < pedestrians.size(); i++) {
		    timeCredits[i] = 500.0f;
	    }
	    return CLUtils.toFloatBuffer(timeCredits);
    }

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

	private void clCalcMinTimeCredit(
			final long clMinTimeCredit,
			final long clTimeCredits,
			final int numberOfElements) throws OpenCLException {

		try (MemoryStack stack = stackPush()) {
			PointerBuffer clGlobalWorkSize = stack.callocPointer(1);
			PointerBuffer clLocalWorkSize = stack.callocPointer(1);
			IntBuffer errcode_ret = stack.callocInt(1);
			long maxWorkGroupSize = CLUtils.getMaxWorkGroupSizeForKernel(clDevice, clFindCellBoundsAndReorder, 4, getMaxWorkGroupSize(), getMaxLocalMemorySize()); // local 4 byte (integer)
			clGlobalWorkSize.put(0, Math.min(maxWorkGroupSize, numberOfElements));
			clLocalWorkSize.put(0, Math.min(maxWorkGroupSize, numberOfElements));

			CLInfo.checkCLError(clSetKernelArg1p(clCalcMinTimeCredit, 0, clMinTimeCredit));
			CLInfo.checkCLError(clSetKernelArg1p(clCalcMinTimeCredit, 1, clTimeCredits));
			CLInfo.checkCLError(clSetKernelArg(clCalcMinTimeCredit, 2, maxWorkGroupSize * 4));
			CLInfo.checkCLError(clSetKernelArg1i(clCalcMinTimeCredit, 3, numberOfElements));
			CLInfo.checkCLError((int)enqueueNDRangeKernel("clCalcMinTimeCredit", clQueue, clCalcMinTimeCredit, 1, null, clGlobalWorkSize, clLocalWorkSize, null, null));
		}
	}

    private void clSeek(
		    final long clReorderedPedestrians,
		    final long clReorderedPositions,
		    final long clReorderedTimeCredit,
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
		    final float timeStepInSec,
		    final int numberOfElements)
		    throws OpenCLException {
	    try (MemoryStack stack = stackPush()) {

		    PointerBuffer clGlobalWorkSize = stack.callocPointer(1);
		    PointerBuffer clLocalWorkSize = stack.callocPointer(1);
			long maxWorkGroupSize = CLUtils.getMaxWorkGroupSizeForKernel(clDevice, clSeek, 0, getMaxWorkGroupSize(), getMaxLocalMemorySize()); // local 4 byte (integer)

		    CLInfo.checkCLError(clSetKernelArg1p(clSeek, 0, clReorderedPedestrians));
		    CLInfo.checkCLError(clSetKernelArg1p(clSeek, 1, clReorderedPositions));
		    CLInfo.checkCLError(clSetKernelArg1p(clSeek, 2, clReorderedTimeCredit));
		    CLInfo.checkCLError(clSetKernelArg1p(clSeek, 3, clCirclePositions));
		    CLInfo.checkCLError(clSetKernelArg1p(clSeek, 4, clCellStarts));
		    CLInfo.checkCLError(clSetKernelArg1p(clSeek, 5, clCellEnds));
		    CLInfo.checkCLError(clSetKernelArg1p(clSeek, 6, clCellSize));
		    CLInfo.checkCLError(clSetKernelArg1p(clSeek, 7, clGridSize));
		    CLInfo.checkCLError(clSetKernelArg1p(clSeek, 8, clObstaclePotential));
		    CLInfo.checkCLError(clSetKernelArg1p(clSeek, 9, clTargetPotential));
		    CLInfo.checkCLError(clSetKernelArg1p(clSeek, 10, clWorldOrigin));
		    CLInfo.checkCLError(clSetKernelArg1p(clSeek, 11, clPotentialFieldGridSize));
		    CLInfo.checkCLError(clSetKernelArg1p(clSeek, 12, clPotentialFieldSize));
		    CLInfo.checkCLError(clSetKernelArg1f(clSeek, 13, (float)attributesFloorField.getPotentialFieldResolution()));
		    CLInfo.checkCLError(clSetKernelArg1f(clSeek, 14, timeStepInSec));
		    CLInfo.checkCLError(clSetKernelArg1i(clSeek, 15, circlePositionList.size()));
		    CLInfo.checkCLError(clSetKernelArg1i(clSeek, 16, numberOfElements));

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
			final long clReorderedPositions,
			final long clReorderedTimeCredit,
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
			long maxWorkGroupSize = CLUtils.getMaxWorkGroupSizeForKernel(clDevice, clSeek, 0, getMaxWorkGroupSize(), getMaxLocalMemorySize()); // local 4 byte (integer)

			CLInfo.checkCLError(clSetKernelArg1p(clMove, 0, clReorderedPedestrians));
			CLInfo.checkCLError(clSetKernelArg1p(clMove, 1, clReorderedPositions));
			CLInfo.checkCLError(clSetKernelArg1p(clMove, 2, clReorderedTimeCredit));
			CLInfo.checkCLError(clSetKernelArg1p(clMove, 3, clCellStarts));
			CLInfo.checkCLError(clSetKernelArg1p(clMove, 4, clCellEnds));
			CLInfo.checkCLError(clSetKernelArg1p(clMove, 5, clCellSize));
			CLInfo.checkCLError(clSetKernelArg1p(clMove, 6, clGridSize));
			CLInfo.checkCLError(clSetKernelArg1p(clMove, 7, clWorldOrigin));
			CLInfo.checkCLError(clSetKernelArg1i(clMove, 8, numberOfElements));

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
			CLInfo.checkCLError((int)enqueueNDRangeKernel("clMove", clQueue, clMove, 1, null, clGlobalWorkSize, clLocalWorkSize, null, null));
		}
	}

	private void clSwap(
			final long clReorderedPedestrians,
			final long clReorderedPositions,
			final long clReorderedTimeCredit,
			final long clPedestrians,
			final long clPositions,
			final long clTimeCredit,
			final int numberOfElements) throws OpenCLException {

		try (MemoryStack stack = stackPush()) {
			PointerBuffer clGlobalWorkSize = stack.callocPointer(1);
			clGlobalWorkSize.put(0, numberOfElements);

			CLInfo.checkCLError(clSetKernelArg1p(clSwap, 0, clReorderedPedestrians));
			CLInfo.checkCLError(clSetKernelArg1p(clSwap, 1, clReorderedPositions));
			CLInfo.checkCLError(clSetKernelArg1p(clSwap, 2, clReorderedTimeCredit));
			CLInfo.checkCLError(clSetKernelArg1p(clSwap, 3, clPedestrians));
			CLInfo.checkCLError(clSetKernelArg1p(clSwap, 4, clPositions));
			CLInfo.checkCLError(clSetKernelArg1p(clSwap, 5, clTimeCredit));
			CLInfo.checkCLError(clSetKernelArg1i(clSwap, 6, numberOfElements));
			CLInfo.checkCLError((int)enqueueNDRangeKernel("clSwap", clQueue, clSwap, 1, null, clGlobalWorkSize, null, null, null));
		}
	}

    private void clFindCellBoundsAndReorder(
    		final long clCellStarts,
		    final long clCellEnds,
		    final long clReorderedPedestrians,
		    final long clReorderedPositions,
		    final long clReorderedTimeCredit,
		    final long clHashes,
		    final long clIndices,
		    final long clPedestrians,
		    final long clPositions,
		    final long clTimeCredit,
		    final int numberOfElements) throws OpenCLException {

	    try (MemoryStack stack = stackPush()) {

		    PointerBuffer clGlobalWorkSize = stack.callocPointer(1);
		    PointerBuffer clLocalWorkSize = stack.callocPointer(1);
		    IntBuffer errcode_ret = stack.callocInt(1);
			long maxWorkGroupSize = CLUtils.getMaxWorkGroupSizeForKernel(clDevice, clSeek, 0, getMaxWorkGroupSize(), getMaxLocalMemorySize()); // local 4 byte (integer)

		    CLInfo.checkCLError(clSetKernelArg1p(clFindCellBoundsAndReorder, 0, clCellStarts));
		    CLInfo.checkCLError(clSetKernelArg1p(clFindCellBoundsAndReorder, 1, clCellEnds));
		    CLInfo.checkCLError(clSetKernelArg1p(clFindCellBoundsAndReorder, 2, clReorderedPedestrians));
		    CLInfo.checkCLError(clSetKernelArg1p(clFindCellBoundsAndReorder, 3, clReorderedPositions));
		    CLInfo.checkCLError(clSetKernelArg1p(clFindCellBoundsAndReorder, 4, clReorderedTimeCredit));
		    CLInfo.checkCLError(clSetKernelArg1p(clFindCellBoundsAndReorder, 5, clHashes));
		    CLInfo.checkCLError(clSetKernelArg1p(clFindCellBoundsAndReorder, 6, clIndices));
		    CLInfo.checkCLError(clSetKernelArg1p(clFindCellBoundsAndReorder, 7, clPedestrians));
		    CLInfo.checkCLError(clSetKernelArg1p(clFindCellBoundsAndReorder, 8, clPositions));
		    CLInfo.checkCLError(clSetKernelArg1p(clFindCellBoundsAndReorder, 9, clTimeCredit));
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

    @Override
    protected void clearMemory() throws OpenCLException {
        super.clearMemory();
    }

    @Override
    protected void clearCL() throws OpenCLException {
	    super.releaseKernels();
	    CLInfo.checkCLError(clReleaseKernel(clCalcMinTimeCredit));
	    CLInfo.checkCLError(clReleaseKernel(clFindCellBoundsAndReorder));
	    CLInfo.checkCLError(clReleaseKernel(clSeek));
	    CLInfo.checkCLError(clReleaseKernel(clMove));
	    CLInfo.checkCLError(clReleaseKernel(clSwap));
    }

	@Override
	protected void buildKernels() throws OpenCLException {
		super.buildKernels();
		try (MemoryStack stack = stackPush()) {
			IntBuffer errcode_ret = stack.callocInt(1);
			clFindCellBoundsAndReorder = clCreateKernel(clProgram, "findCellBoundsAndReorder", errcode_ret);
			CLInfo.checkCLError(errcode_ret);
			clCalcMinTimeCredit = clCreateKernel(clProgram, "minTimeCredit", errcode_ret);
			CLInfo.checkCLError(errcode_ret);
			clSeek = clCreateKernel(clProgram, "seek", errcode_ret);
			CLInfo.checkCLError(errcode_ret);
			clMove = clCreateKernel(clProgram, "move", errcode_ret);
			CLInfo.checkCLError(errcode_ret);
			clSwap = clCreateKernel(clProgram, "swap", errcode_ret);
			CLInfo.checkCLError(errcode_ret);
		}
	}
}
