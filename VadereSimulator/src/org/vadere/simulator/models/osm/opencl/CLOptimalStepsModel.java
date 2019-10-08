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
public class CLOptimalStepsModel {
	private static Logger log = Logger.getLogger(CLOptimalStepsModel.class);

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
	private IntBuffer hashes;
	private IntBuffer indices;
	private IntBuffer cellStarts;
	private IntBuffer cellEnds;
	private FloatBuffer reorderedPedestrians;
	private FloatBuffer pedestrians;
	private FloatBuffer worldOrigin;
	private FloatBuffer cellSize;
	private FloatBuffer targetPotentialField;
	private FloatBuffer obstaclePotentialField;
	private FloatBuffer circlePositions;
	private FloatBuffer potenialFieldSize;
	private IntBuffer gridSize;
	private IntBuffer potentialFieldGridSize;


	private IntBuffer inValues;
	private IntBuffer outValues;

	private ByteBuffer source;
	private ByteBuffer particleSource;

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
	private long clNextPositions;

	private int numberOfGridCells;
	private VRectangle bound;
	private float iCellSize;
	private int[] iGridSize;
	private List<PedestrianOpenCL> pedestrianList;
	private List<VPoint> circlePositionList;
	private final int deviceType;

	private final AttributesFloorField attributesFloorField;
	private final AttributesOSM attributesOSM;

	private int[] keys;
	private int[] values;

	private int[] resultValues;
	private int[] resultKeys;

	private static final Logger logger = Logger.getLogger(CLOptimalStepsModel.class);

	private long max_work_group_size;
	private long max_local_memory_size;

	// time measurement
	private boolean debug = false;
	private boolean profiling = false;

	private int numberOfSortElements;

	public enum KernelType {
		Separate,
		Col,
		Row,
		NonSeparate
	}

	private int counter = 0;

	public CLOptimalStepsModel(
			@NotNull final AttributesOSM attributesOSM,
			@NotNull final AttributesFloorField attributesFloorField,
			@NotNull final VRectangle bound,
			@NotNull final EikonalSolver targetPotential,
			@NotNull final EikonalSolver obstaclePotential) throws OpenCLException {
		this(attributesOSM, attributesFloorField, bound, targetPotential, obstaclePotential, CL_DEVICE_TYPE_GPU);
	}

	/**
	 * Default constructor.
	 *
	 * @param bound             the spatial bound of the linked cell.
	 *
	 * @throws OpenCLException
	 */
	public CLOptimalStepsModel(
			@NotNull final AttributesOSM attributesOSM,
			@NotNull final AttributesFloorField attributesFloorField,
			@NotNull final VRectangle bound,
			@NotNull final EikonalSolver targetPotential,
			@NotNull final EikonalSolver obstaclePotential,
			final int device) throws OpenCLException {
		this.attributesOSM = attributesOSM;
		this.attributesFloorField = attributesFloorField;
		this.bound = bound;
		this.deviceType = device;

		//TODO: this should be done in mallocHostMemory().
		this.targetPotentialField = generatePotentialFieldApproximation(targetPotential);
		this.obstaclePotentialField = generatePotentialFieldApproximation(obstaclePotential);

		if(debug) {
			Configuration.DEBUG.set(true);
			Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);
			Configuration.DEBUG_STACK.set(true);
		}
		init();
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
		public VPoint position;
		public VPoint newPosition;

		public PedestrianOpenCL(final VPoint position, final float stepRadius) {
			this.position = position;
			this.stepRadius = stepRadius;
		}

		@Override
		public String toString() {
			return position + " -> " + newPosition;
		}
	}

	/**
	 * The data structure representing the linked cell. The elements of cell i
	 * between (reorderedPedestrians[cellStart[i]*2], reorderedPedestrians[cellStart[i]*2+1])
	 * and (reorderedPedestrians[(cellEnds[i]-1)*2], reorderedPedestrians[(cellEnds[i]-1)*2+1]).
	 */
	public class LinkedCell {
		/**
		 * the starting index at which the cell starts, i.e. cell i starts at cellStart[i].
		 */
		public int[] cellStarts;

		/**
		 * the ending index at which the cell starts, i.e. cell i ends at cellStart[i].
		 */
		public int[] cellEnds;

		/**
		 * the ordered 2D-coordinates.
		 */
		public float[] reorderedPositions;

		/**
		 * the mapping between the unordered (original) pedestrians and the reorderedPedestrians,
		 * i.e. reorderedPedestrians[i] == pedestrians[indices[i]]
		 */
		public int[] indices;

		/**
		 * the hashes i.e. the cell of the pedestrians, i.e. hashes[i] is the cell of pedestrians[i].
		 */
		public int[] hashes;

		/**
		 * the original pedestrians in original order.
		 */
		public float[] positions;
	}


	/**
	 * Computes the {@link LinkedCell} of the list of pedestrians.
	 *
	 * @param pedestrians
	 * @return {@link LinkedCell} which is the linked list in an array based structure.
	 *
	 * @throws OpenCLException
	 */
	//TODO: dont sort if the size is <= 1!
	public List<PedestrianOpenCL> getNextSteps(@NotNull final List<PedestrianOpenCL> pedestrians, final double cellSize) throws OpenCLException {

		this.iGridSize = new int[]{ (int)Math.ceil(bound.getWidth() / cellSize),  (int)Math.ceil(bound.getHeight() / cellSize)};
		this.numberOfGridCells = this.iGridSize[0] * this.iGridSize[1];
		this.iCellSize = (float)cellSize;

		// support also not multiple of 2 !
		numberOfSortElements = expOf(pedestrians.size(), 2);
		int toRemove = 0;
		int originalSize = pedestrians.size();
		while(numberOfSortElements > pedestrians.size()) {
			pedestrians.add(new PedestrianOpenCL(new VPoint(0,0), 1.0f));
			toRemove++;
		}

//		log.info(numberOfSortElements);

		try (MemoryStack stack = stackPush()) {
			this.pedestrianList = pedestrians;
			allocHostMemory(pedestrians.size());
			allocDeviceMemory(pedestrians.size());
			clCalcHash(clHashes, clIndices, clPedestrians, clCellSize, clWorldOrigin, clGridSize, pedestrians.size());
			clBitonicSort(clHashes, clIndices, clHashes, clIndices, pedestrians.size(), 1);
			clFindCellBoundsAndReorder(clCellStarts, clCellEnds, clReorderedPedestrians, clHashes, clIndices, clPedestrians, pedestrians.size());
			clNextPosition(
					clPedestrianNextPositions,
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
					pedestrians.size());

			//clEnqueueReadBuffer(clQueue, clCellStarts, true, 0, cellStarts, null, null);
			//clEnqueueReadBuffer(clQueue, clCellEnds, true, 0, cellEnds, null, null);
			FloatBuffer nextPositions = stack.mallocFloat(pedestrians.size() * 2);
			clEnqueueReadBuffer(clQueue, clPedestrianNextPositions, true, 0, nextPositions, null, null);
			clEnqueueReadBuffer(clQueue, clIndices, true, 0, indices, null, null);
			//clEnqueueReadBuffer(clQueue, clHashes, true, 0, hashes, null, null);
			//clEnqueueReadBuffer(clQueue, clPedestrians, true, 0, this.pedestrians, null, null);

			int[] aIndices = CLUtils.toIntArray(indices, pedestrians.size());
			float[] positionsAndRadi = CLUtils.toFloatArray(nextPositions, pedestrians.size() * 2);
			for(int i = 0; i < pedestrians.size(); i++) {
				float x = positionsAndRadi[i * 2];
				float y = positionsAndRadi[i * 2 + 1];
				VPoint newPosition = new VPoint(x,y);
				pedestrians.get(aIndices[i]).newPosition = newPosition;
			}

			/*int[] aCellStarts = CLUtils.toIntArray(cellStarts, numberOfGridCells);
			int[] aCellEnds = CLUtils.toIntArray(cellEnds, numberOfGridCells);

			int[] aIndices = CLUtils.toIntArray(indices, numberOfElements);
			int[] aHashes = CLUtils.toIntArray(hashes, numberOfElements);
			float[] aPositions = CLUtils.toFloatArray(this.pedestrians, numberOfElements * 2);

			LinkedCell gridCells = new LinkedCell();
			gridCells.cellEnds = aCellEnds;
			gridCells.cellStarts = aCellStarts;
			gridCells.reorderedPedestrians = aReorderedPositions;
			gridCells.indices = aIndices;
			gridCells.hashes = aHashes;
			gridCells.positions = aPositions;*/

			/*clearMemory();
			clearCL();*/
			counter++;
			//clearIterationMemory();
			while (pedestrians.size() > originalSize) {
				pedestrians.remove(pedestrians.size()-1);
			}

			return pedestrians;
			//clBitonicSort(clHashes, clIndices, clHashes, clIndices, numberOfElements, 1);
			//clFindCellBoundsAndReorder(clCellStarts, clCellEnds, clReorderedPedestrians, clHashes, clIndices, clPedestrians, numberOfElements, numberOfGridCells);

		} finally {
			clearIterationMemory();
		}
	}

	/**
	 * Computes all the hash values, i.e. cells of each position and sort these hashes and construct a mapping
	 * of the rearrangement. This method exists to test the bitonic sort algorithm on the GPU.
	 *
	 * @param positions the pedestrians which will be hashed.
	 * @return  the sorted hashes.
	 * @throws OpenCLException
	 */
	public int[] calcSortedHashes(@NotNull final List<PedestrianOpenCL> positions) throws OpenCLException {
		this.pedestrianList = positions;
		allocHostMemory(positions.size());
		allocDeviceMemory(positions.size());

		clCalcHash(clHashes, clIndices, clPedestrians, clCellSize, clWorldOrigin, clGridSize, positions.size());
		clBitonicSort(clHashes, clIndices, clHashes, clIndices, positions.size(), 1);
		clEnqueueReadBuffer(clQueue, clHashes, true, 0, hashes, null, null);
		int[] result = CLUtils.toIntArray(hashes, positions.size());

		clearMemory();
		clearCL();
		return result;

		//clBitonicSort(clHashes, clIndices, clHashes, clIndices, numberOfElements, 1);
		//clFindCellBoundsAndReorder(clCellStarts, clCellEnds, clReorderedPedestrians, clHashes, clIndices, clPedestrians, numberOfElements, numberOfGridCells);
	}

	/**
	 * Computes all the hash values, i.e. cells of each position.
	 * This method exists to test the hash computation on the GPU.
	 *
	 * @param positions the pedestrians which will be hashed.
	 * @return the (unsorted) hashes.
	 * @throws OpenCLException
	 */
	public int[] calcHashes(@NotNull final List<PedestrianOpenCL> positions) throws OpenCLException {
		this.pedestrianList = positions;
		allocHostMemory(positions.size());
		allocDeviceMemory(positions.size());

		clCalcHash(clHashes, clIndices, clPedestrians, clCellSize, clWorldOrigin, clGridSize, positions.size());
		clEnqueueReadBuffer(clQueue, clHashes, true, 0, hashes, null, null);
		int[] result = CLUtils.toIntArray(hashes, positions.size());

		clearMemory();
		clearCL();
		return result;

		//clBitonicSort(clHashes, clIndices, clHashes, clIndices, numberOfElements, 1);
		//clFindCellBoundsAndReorder(clCellStarts, clCellEnds, clReorderedPedestrians, clHashes, clIndices, clPedestrians, numberOfElements, numberOfGridCells);
	}

	/**
	 * Returns the gridSizes of the linked cell, i.e. result[0] is the x and
	 * result[1] the y direction.
	 *
	 * @return the gridSizes (2D) stored in an array.
	 */
	public int[] getGridSize() {
		return new int[]{iGridSize[0], iGridSize[1]};
	}

	/**
	 * Returns the gridSize which is equal in x and y direction.
	 *
	 * @return the gridSize
	 */
	public float getCellSize() {
		return iCellSize;
	}

	public VPoint getWorldOrign() {
		return new VPoint(bound.getMinX(), bound.getMinY());
	}

	public void allocHostMemory(final int numberOfElements) {

		/*
		 * (1) pedestrian positions and step length
		 */
		float[] posAndRadius = new float[numberOfElements*3];
		for(int i = 0; i < numberOfElements; i++) {
			posAndRadius[i*3] = (float) pedestrianList.get(i).position.getX();
			posAndRadius[i*3+1] = (float) pedestrianList.get(i).position.getY();
			posAndRadius[i*3+2] = pedestrianList.get(i).stepRadius;
		}
		this.pedestrians = CLUtils.toFloatBuffer(posAndRadius);

		/*
		 * (2) circle / disc positions at (0,0)
		 */
		circlePositionList = GeometryUtils.getDiscDiscretizationPoints(new Random(), false,
				new VCircle(new VPoint(0,0), 1.0),
				attributesOSM.getNumberOfCircles(),
				attributesOSM.getStepCircleResolution(),
				0,
				2*Math.PI);
		circlePositionList.add(new VPoint(0, 0));

		float[] circlePositions = new float[circlePositionList.size()*2];
		for(int i = 0; i < circlePositionList.size(); i++) {
			circlePositions[i*2] = (float) circlePositionList.get(i).getX();
			circlePositions[i*2+1] = (float) circlePositionList.get(i).getY();
		}

		this.circlePositions = CLUtils.toFloatBuffer(circlePositions);
		this.hashes = MemoryUtil.memAllocInt(pedestrianList.size());

		if(counter == 0) {
			float[] originArray = new float[]{(float)bound.getMinX(), (float)bound.getMinX()};
			this.worldOrigin = CLUtils.toFloatBuffer(originArray);

			this.potenialFieldSize = MemoryUtil.memAllocFloat(2);
			this.potenialFieldSize.put(0, (float)bound.width);
			this.potenialFieldSize.put(1, (float)bound.height);

			this.potentialFieldGridSize = MemoryUtil.memAllocInt(2);
			this.potentialFieldGridSize.put(0, getPotentialFieldWidth());
			this.potentialFieldGridSize.put(1, getPotentialFieldHeight());
		}
		this.cellSize = MemoryUtil.memAllocFloat(1);
		this.cellSize.put(0, iCellSize);

		this.gridSize = CLUtils.toIntBuffer(iGridSize);

		this.cellStarts = MemoryUtil.memAllocInt(numberOfGridCells);
		this.cellEnds = MemoryUtil.memAllocInt(numberOfGridCells);
		this.indices = MemoryUtil.memAllocInt(pedestrianList.size());
		this.reorderedPedestrians = MemoryUtil.memAllocFloat(numberOfElements * 3);
	}

	private void allocDeviceMemory(final int numberOfElements) {
		try (MemoryStack stack = stackPush()) {
			IntBuffer errcode_ret = stack.callocInt(1);

			clCellSize = clCreateBuffer(clContext,  CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR | CL_MEM_COPY_HOST_PTR, cellSize, errcode_ret);
			clGridSize = clCreateBuffer(clContext,  CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR | CL_MEM_COPY_HOST_PTR, gridSize, errcode_ret);

			if(counter == 0) {
				clPotentialFieldSize = clCreateBuffer(clContext,  CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR | CL_MEM_COPY_HOST_PTR, potenialFieldSize, errcode_ret);
				clPotentialFieldGridSize = clCreateBuffer(clContext,  CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR | CL_MEM_COPY_HOST_PTR, potentialFieldGridSize, errcode_ret);
				clWorldOrigin = clCreateBuffer(clContext,  CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR | CL_MEM_COPY_HOST_PTR, worldOrigin, errcode_ret);
				clTargetPotential = clCreateBuffer(clContext,  CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR | CL_MEM_COPY_HOST_PTR, targetPotentialField, errcode_ret);
				clObstaclePotential = clCreateBuffer(clContext,  CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR | CL_MEM_COPY_HOST_PTR, obstaclePotentialField, errcode_ret);
				clCirclePositions = clCreateBuffer(clContext,  CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR | CL_MEM_COPY_HOST_PTR, circlePositions, errcode_ret);
			}

			clHashes = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * numberOfElements, errcode_ret);
			clIndices = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * numberOfElements, errcode_ret);
			clCellStarts = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * numberOfGridCells, errcode_ret);
			clCellEnds = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * numberOfGridCells, errcode_ret);
			clReorderedPedestrians = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 3 * 4 * numberOfElements, errcode_ret);
			clPedestrians = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 3 * 4 * numberOfElements, errcode_ret);
			clPedestrianNextPositions = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 2 * 4 * numberOfElements, errcode_ret);
			clEnqueueWriteBuffer(clQueue, clPedestrians, true, 0, pedestrians, null, null);
		}
	}

	public int[] getResultKeys() {
		return resultKeys;
	}

	public int[] getResultValues() {
		return resultValues;
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
			final int numberOfElements) throws OpenCLException {
		try (MemoryStack stack = stackPush()) {
			PointerBuffer clGlobalWorkSize = stack.callocPointer(1);
			CLInfo.checkCLError(clSetKernelArg1p(clCalcHash, 0, clHashes));
			CLInfo.checkCLError(clSetKernelArg1p(clCalcHash, 1, clIndices));
			CLInfo.checkCLError(clSetKernelArg1p(clCalcHash, 2, clPositions));
			CLInfo.checkCLError(clSetKernelArg1p(clCalcHash, 3, clCellSize));
			CLInfo.checkCLError(clSetKernelArg1p(clCalcHash, 4, clWorldOrign));
			CLInfo.checkCLError(clSetKernelArg1p(clCalcHash, 5, clGridSize));
			CLInfo.checkCLError(clSetKernelArg1i(clCalcHash, 6, numberOfElements));
			clGlobalWorkSize.put(0, numberOfElements);
			//TODO: local work size?
			CLInfo.checkCLError((int)enqueueNDRangeKernel("clCalcHash", clQueue, clCalcHash, 1, null, clGlobalWorkSize, null, null, null));
		}
	}

        /*__kernel void nextSteps(
    __global float2       *newPositions,        //output
    __global const float3 *orderedPedestrians,  //input
    __global const uint   *d_CellStart,         //input: cell boundaries
    __global const uint   *d_CellEnd,           //input
    __global const float  *obstaclePotential,   //input
    __global const float  *targetPotential,     //input
    __constant float2     *worldOrigin,         //input
    __constant float      *potentialCellSize    //input
){*/

	private void clNextPosition(
			final long clPedestrianNextPositions,
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
			IntBuffer errcode_ret = stack.callocInt(1);
			long maxWorkGroupSize = getMaxWorkGroupSizeForKernel(clDevice, clNextPositions, 0); // local 4 byte (integer)

			CLInfo.checkCLError(clSetKernelArg1p(clNextPositions, 0, clPedestrianNextPositions));
			CLInfo.checkCLError(clSetKernelArg1p(clNextPositions, 1, clReorderedPedestrians));
			CLInfo.checkCLError(clSetKernelArg1p(clNextPositions, 2, clCirclePositions));
			CLInfo.checkCLError(clSetKernelArg1p(clNextPositions, 3, clCellStarts));
			CLInfo.checkCLError(clSetKernelArg1p(clNextPositions, 4, clCellEnds));
			CLInfo.checkCLError(clSetKernelArg1p(clNextPositions, 5, clCellSize));
			CLInfo.checkCLError(clSetKernelArg1p(clNextPositions, 6, clGridSize));
			CLInfo.checkCLError(clSetKernelArg1p(clNextPositions, 7, clObstaclePotential));
			CLInfo.checkCLError(clSetKernelArg1p(clNextPositions, 8, clTargetPotential));
			CLInfo.checkCLError(clSetKernelArg1p(clNextPositions, 9, clWorldOrigin));
			CLInfo.checkCLError(clSetKernelArg1p(clNextPositions, 10, clPotentialFieldGridSize));
			CLInfo.checkCLError(clSetKernelArg1p(clNextPositions, 11, clPotentialFieldSize));
			CLInfo.checkCLError(clSetKernelArg1f(clNextPositions, 12, (float)attributesFloorField.getPotentialFieldResolution()));
			CLInfo.checkCLError(clSetKernelArg1i(clNextPositions, 13, circlePositionList.size()));

			long globalWorkSize;
			long localWorkSize;
			if(numberOfElements <= maxWorkGroupSize){
				localWorkSize = numberOfElements;
				globalWorkSize = numberOfElements;
			}
			else {
				localWorkSize = maxWorkGroupSize;
				globalWorkSize = multipleOf(numberOfElements, localWorkSize);
			}

			clGlobalWorkSize.put(0, globalWorkSize);
			clLocalWorkSize.put(0, localWorkSize);
			//TODO: local work size? + check 2^n constrain!
			CLInfo.checkCLError((int)enqueueNDRangeKernel("clNextPositions", clQueue, clNextPositions, 1, null, clGlobalWorkSize, clLocalWorkSize, null, null));
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
			long maxWorkGroupSize = getMaxWorkGroupSizeForKernel(clDevice, clNextPositions, 0); // local 4 byte (integer)

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
				globalWorkSize = multipleOf(numberOfElements, localWorkSize);
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

	private int expOf(int value, int multiple) {
		int result = 2;
		while (result < value) {
			result *= multiple;
		}
		return result;
	}

	private long multipleOf(long value, long multiple) {
		long result = multiple;
		while (result < value) {
			result += multiple;
		}
		return result;
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
	}

	private void clearIterationMemory() throws OpenCLException {
		try {
			CLInfo.checkCLError(clReleaseMemObject(clCellSize));
			CLInfo.checkCLError(clReleaseMemObject(clGridSize));
			CLInfo.checkCLError(clReleaseMemObject(clHashes));
			CLInfo.checkCLError(clReleaseMemObject(clIndices));
			CLInfo.checkCLError(clReleaseMemObject(clCellStarts));
			CLInfo.checkCLError(clReleaseMemObject(clCellEnds));
			CLInfo.checkCLError(clReleaseMemObject(clReorderedPedestrians));
			CLInfo.checkCLError(clReleaseMemObject(clPedestrians));
			CLInfo.checkCLError(clReleaseMemObject(clPedestrianNextPositions));
		}
		catch (OpenCLException ex) {
			throw ex;
		}
		finally {
			MemoryUtil.memFree(gridSize);
			MemoryUtil.memFree(cellSize);
			MemoryUtil.memFree(cellStarts);
			MemoryUtil.memFree(cellEnds);
			MemoryUtil.memFree(hashes);
			MemoryUtil.memFree(indices);
			MemoryUtil.memFree(reorderedPedestrians);
			MemoryUtil.memFree(pedestrians);
		}
	}

	private void clearMemory() throws OpenCLException {
		// release memory and devices
		try {
			CLInfo.checkCLError(clReleaseMemObject(clHashes));
			CLInfo.checkCLError(clReleaseMemObject(clIndices));
			CLInfo.checkCLError(clReleaseMemObject(clCellStarts));
			CLInfo.checkCLError(clReleaseMemObject(clCellEnds));
			CLInfo.checkCLError(clReleaseMemObject(clReorderedPedestrians));
			CLInfo.checkCLError(clReleaseMemObject(clPedestrians));
			CLInfo.checkCLError(clReleaseMemObject(clPedestrianNextPositions));
			CLInfo.checkCLError(clReleaseMemObject(clCellSize));
			CLInfo.checkCLError(clReleaseMemObject(clPotentialFieldSize));
			CLInfo.checkCLError(clReleaseMemObject(clWorldOrigin));
			CLInfo.checkCLError(clReleaseMemObject(clGridSize));
			CLInfo.checkCLError(clReleaseMemObject(clTargetPotential));
			CLInfo.checkCLError(clReleaseMemObject(clObstaclePotential));
			CLInfo.checkCLError(clReleaseMemObject(clCirclePositions));
			CLInfo.checkCLError(clReleaseMemObject(clPotentialFieldGridSize));

		}
		catch (OpenCLException ex) {
			throw ex;
		}
		finally {
			MemoryUtil.memFree(hashes);
			MemoryUtil.memFree(indices);
			MemoryUtil.memFree(cellStarts);
			MemoryUtil.memFree(cellEnds);
			MemoryUtil.memFree(reorderedPedestrians);
			MemoryUtil.memFree(pedestrians);
			MemoryUtil.memFree(worldOrigin);
			MemoryUtil.memFree(cellSize);
			MemoryUtil.memFree(potenialFieldSize);
			MemoryUtil.memFree(gridSize);
			MemoryUtil.memFree(circlePositions);
			MemoryUtil.memFree(potentialFieldGridSize);
			MemoryUtil.memFree(source);
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

			// TODO delete memory?

			try {
				source = CLUtils.ioResourceToByteBuffer("OSM.cl", 4096);
			} catch (IOException e) {
				throw new OpenCLException(e.getMessage());
			}

			strings.put(0, source);
			lengths.put(0, source.remaining());

			clProgram = clCreateProgramWithSource(clContext, strings, lengths, errcode_ret);
			CLInfo.checkCLError(clBuildProgram(clProgram, clDevice, "", programCB, NULL));
			clBitonicSortLocal = clCreateKernel(clProgram, "bitonicSortLocal", errcode_ret);
			CLInfo.checkCLError(errcode_ret);
			clBitonicSortLocal1 = clCreateKernel(clProgram, "bitonicSortLocal1", errcode_ret);
			CLInfo.checkCLError(errcode_ret);
			clBitonicMergeGlobal = clCreateKernel(clProgram, "bitonicMergeGlobal", errcode_ret);
			CLInfo.checkCLError(errcode_ret);
			clBitonicMergeLocal = clCreateKernel(clProgram, "bitonicMergeLocal", errcode_ret);
			CLInfo.checkCLError(errcode_ret);
			clNextPositions = clCreateKernel(clProgram, "nextSteps", errcode_ret);
			CLInfo.checkCLError(errcode_ret);

			clCalcHash = clCreateKernel(clProgram, "calcHash", errcode_ret);
			CLInfo.checkCLError(errcode_ret);
			clFindCellBoundsAndReorder = clCreateKernel(clProgram, "findCellBoundsAndReorder", errcode_ret);
			CLInfo.checkCLError(errcode_ret);

			max_work_group_size = InfoUtils.getDeviceInfoPointer(clDevice, CL_DEVICE_MAX_WORK_GROUP_SIZE);
			logger.debug("CL_DEVICE_MAX_WORK_GROUP_SIZE = " + max_work_group_size);

			max_local_memory_size = InfoUtils.getDeviceInfoLong(clDevice, CL_DEVICE_LOCAL_MEM_SIZE);
			logger.debug("CL_DEVICE_LOCAL_MEM_SIZE = " + max_local_memory_size);
		}

	}
}
