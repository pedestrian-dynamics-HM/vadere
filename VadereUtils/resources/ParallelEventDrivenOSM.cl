//#pragma OPENCL EXTENSION cl_intel_printf : enable

////////////////////////////////////////////////////////////////////////////////
// Common definitions
////////////////////////////////////////////////////////////////////////////////
#define UMAD(a, b, c)  ((a) * (b) + (c))

#define RADIUS 0.2f
#define DIAMETER 0.4f

#define POTENTIAL_WIDTH 0.5f

#define COORDOFFSET 2
#define X 0
#define Y 1

#define OFFSET 2
#define STEPSIZE 0
#define DESIREDSPEED 1

inline void ComparatorPrivate(
    uint *keyA,
    uint *valA,
    uint *keyB,
    uint *valB,
    uint dir
){
    if( (*keyA > *keyB) == dir ){
        uint t;
        t = *keyA; *keyA = *keyB; *keyB = t;
        t = *valA; *valA = *valB; *valB = t;
    }
}

inline void ComparatorLocal(
    __local uint *keyA,
    __local uint *valA,
    __local uint *keyB,
    __local uint *valB,
    uint dir
){
    if( (*keyA > *keyB) == dir ){
        uint t;
        t = *keyA; *keyA = *keyB; *keyB = t;
        t = *valA; *valA = *valB; *valB = t;
    }
}

////////////////////////////////////////////////////////////////////////////////
// Save particle grid cell hashes and indices
////////////////////////////////////////////////////////////////////////////////
inline int2 getGridPos(const float2 p, __constant const float* cellSize, __constant const float2* worldOrigin){
    int2 gridPos;
    float2 wordOr = (*worldOrigin);
    gridPos.x = max(0, (int)floor((p.x - wordOr.x) / (*cellSize)));
    gridPos.y = max(0, (int)floor((p.y - wordOr.y) / (*cellSize)));
    return gridPos;
}

inline bool isValidCell(__constant const int2 *gridSize, int2 uGridPos) {
    return uGridPos.x >= 0 && uGridPos.y >= 0 && uGridPos.x < (*gridSize).x && uGridPos.y < (*gridSize).y;
}

//Calculate address in grid from position (clamping to edges)
inline uint getGridHash(const int2 gridPos, __constant const uint2* gridSize){
    return UMAD(  (*gridSize).x, gridPos.y, gridPos.x );
}

inline int2 getGridPosFromIndex(const uint hash, __constant const uint2* gridSize){
    int y = hash / (*gridSize).x;
    int x = hash - ((*gridSize).x * y);
    return (int2) (x, y);
}

////////////////////////////////////////////////////////////////////////////////
// Potential field helper methods
////////////////////////////////////////////////////////////////////////////////

// see PotentialFieldPedestrianCompact with useHardBodyShell = true:
inline float getPedestrianPotential(const float2 pos, const float2 otherPedPosition) {
    float d = distance(pos, otherPedPosition) - DIAMETER;
    float width = 0.5f;
    float height = 12.6f;
    if(d < 0) {
        return 10000;
    }
    if (d < width) {
        return height * native_exp(1 / (pown(d / width, 2) - 1));
    } else {
        return 0.0f;
    }
}

inline float getFullPedestrianPotential(
        __global const float        *orderedPositions,  //input
        __global const uint         *d_CellStart,
        __global const uint         *d_CellEnd,
        __constant const float      *cellSize,
        __constant const uint2      *gridSize,
        __constant const float2     *worldOrigin,
        const float2                pos,
        const float2                pedPosition)
{
    float potential = 0;
    int2 gridPos = getGridPos(pedPosition, cellSize, worldOrigin);
    for(int y = -1; y <= 1; y++) {
        for(int x = -1; x <= 1; x++){
            int2 uGridPos = gridPos - (int2)(x, y);

            // note if uGridPos.x == 0 than uGridPos.x -1 = 2^N - 1 and the step is also continued!
            if(isValidCell(gridSize, uGridPos)) {
                uint hash = getGridHash(uGridPos, gridSize);
                uint startI = d_CellStart[hash];
                uint endI = d_CellEnd[hash];
                for(uint j = startI; j < endI; j++){
                    // TODO: seperate position from rest , remove global memory access
                    float2 otherPos = (float2) (orderedPositions[j * COORDOFFSET + X], orderedPositions[j * COORDOFFSET + Y]);
                    potential += getPedestrianPotential(pos, otherPos);
                }
            }
        }
    }
    potential -= getPedestrianPotential(pos, pedPosition);
    return potential;
}

inline uint2 getNearestPointTowardsOrigin(float2 evalPoint, float potentialCellSize, float2 potentialFieldSize) {
    evalPoint = max(evalPoint, (float2)(0.0f, 0.0f));
    evalPoint = min(evalPoint, (float2)(potentialFieldSize.x, potentialFieldSize.y));
    uint2 result;
    result.x = (uint) floor(evalPoint.x / potentialCellSize);
    result.y = (uint) floor(evalPoint.y / potentialCellSize);
    return result;
}

inline float2 pointToCoord(uint2 point, float potentialCellSize) {
    return (float2) (point.x * potentialCellSize, point.y * potentialCellSize);
}

inline float getPotentialFieldGridValue(__global const float *targetPotential, uint2 cell, uint2 potentialGridSize) {
    return targetPotential[potentialGridSize.x * cell.y + cell.x];
}

inline float2 bilinearInterpolationWithUnkown(const float4 z, const float2 delta) {
    float4 weights = (float4)((1.0f - delta.x) * (1.0f - delta.y), delta.x * (1.0f - delta.y), delta.x * delta.y, (1.0f - delta.x) * delta.y);
    float4 result = weights * z;
    return (float2) (result.s0 + result.s1 + result.s2 + result.s3, weights.s0 + weights.s1 + weights.s2 + weights.s3);
}

inline float getPotentialFieldValue(const float2 evalPoint, __global const float *potentialField, const float potentialCellSize, const float2 potentialFieldSize, const uint2 potentialGridSize) {
    uint2 gridPoint = getNearestPointTowardsOrigin(evalPoint, potentialCellSize, potentialFieldSize);
    float2 gridPointCoord = pointToCoord(gridPoint, potentialCellSize);
    uint incX = 1, incY = 1;

    if (evalPoint.x + 1 >= potentialFieldSize.x) {
        incX = 0;
    }

    if (evalPoint.y + 1 >= potentialFieldSize.y) {
        incY = 0;
    }

    float4 gridPotentials = (
        getPotentialFieldGridValue(potentialField, gridPoint, potentialGridSize),
        getPotentialFieldGridValue(potentialField, gridPoint + (uint2)(incX, 0), potentialGridSize),
        getPotentialFieldGridValue(potentialField, gridPoint + (uint2)(incX, incY), potentialGridSize),
        getPotentialFieldGridValue(potentialField, gridPoint + (uint2)(0, incY), potentialGridSize)
    );

    float2 result = bilinearInterpolationWithUnkown(gridPotentials,(float2) ((evalPoint.x - gridPointCoord.x) / potentialCellSize, (evalPoint.y - gridPointCoord.y) / potentialCellSize));

    return (float)result.x;
}

inline float getObstaclePotential(float minDistanceToObstacle){
    float currentPotential = 0;
    float width = 0.25f;
    float height = 20.1f;

    if (minDistanceToObstacle <= 0.0f) {
        currentPotential = 1000000.0f;
    } else if (minDistanceToObstacle < width) {
        currentPotential = height * native_exp(1.0f / (pown(minDistanceToObstacle / width, 2) - 1.0f));
    }

    currentPotential = max(0.0f, currentPotential);
    return currentPotential;
}

// end potential field helper methods

// each work group is used for exactly one agent!
__kernel void move (
    __global float          *orderedPositions,      //input
    __global float          *argValues,             // in
    __global float          *values,                // in
    __global int            *ids,
    __local float           *local_point,
    __local float           *local_values) {

    const uint pedId = ids[get_group_id(0)];

    // load everything into local memory
    local_point[get_local_id(0) * COORDOFFSET + X] = argValues[get_global_id(0) * COORDOFFSET + X];
    local_point[get_local_id(0) * COORDOFFSET + Y] = argValues[get_global_id(0) * COORDOFFSET + Y];
    local_values[get_local_id(0)] = values[get_global_id(0)];

    barrier(CLK_LOCAL_MEM_FENCE);

    // reduce
    for(uint stride = get_local_size(0) / 2; stride >= 1; stride /= 2) {
        if (get_local_id(0) < stride) {
            if(local_values[get_local_id(0)] > local_values[get_local_id(0) + stride]) {
                local_values[get_local_id(0)] = local_values[get_local_id(0) + stride];
                local_point[get_local_id(0) * COORDOFFSET + X] = local_point[(get_local_id(0) + stride) * COORDOFFSET + X];
                local_point[get_local_id(0) * COORDOFFSET + Y] = local_point[(get_local_id(0) + stride) * COORDOFFSET + Y];
            }
        }
        barrier(CLK_LOCAL_MEM_FENCE);
    }

    if(get_local_id(0) == 0) {
        orderedPositions[pedId * COORDOFFSET + X] = local_point[X];
        orderedPositions[pedId * COORDOFFSET + Y] = local_point[Y];
    }
}

__kernel void evalPoints(
    __global float          *orderedPedestrians,    //input
    __global float          *orderedPositions,      //input
    __global float          *orderedEventTimes,
    __global float          *possiblePositions,     // out
    __global float          *potentialValues,       // out
    __global int            *ids,                   // input
    __global const float2   *circlePositions,       //input
    __global const uint     *d_CellStart,           //input: cell boundaries
    __global const uint     *d_CellEnd,             //input
    __constant const float  *cellSize,
    __constant const uint2  *gridSize,
    __global const float    *distanceField,         //input
    __global const float    *targetPotentialField,  //input
    __constant const float2 *worldOrigin,           //input
    __constant const uint2  *potentialGridSize,
    __constant const float2 *potentialFieldSize,    //input
    const float             potentialCellSize,      //input
    const uint              numberOfPoints,         //input
    const uint              numberOfPedestrians) {

    uint pedNr = get_global_id(0) / numberOfPoints;
    uint pedId = ids[pedNr];
    uint pointId = get_global_id(0) - (pedNr * numberOfPoints);

    float2 pedPosition = (float2)(orderedPositions[pedId * COORDOFFSET + X], orderedPositions[pedId * COORDOFFSET + Y]);
    float stepSize = orderedPedestrians[pedId * OFFSET + STEPSIZE];
    float desiredSpeed = orderedPedestrians[pedId * OFFSET + DESIREDSPEED];

    float2 circlePosition = circlePositions[pointId];
    float2 evalPoint = pedPosition + (float2)(circlePosition * stepSize);
    float targetPotential = getPotentialFieldValue(evalPoint, targetPotentialField, potentialCellSize, (*potentialFieldSize), (*potentialGridSize));
    float minDistanceToObstacle = getPotentialFieldValue(evalPoint, distanceField, potentialCellSize, (*potentialFieldSize), (*potentialGridSize));
    float obstaclePotential = getObstaclePotential(minDistanceToObstacle);
    float pedestrianPotential = getFullPedestrianPotential(orderedPositions, d_CellStart, d_CellEnd, cellSize, gridSize, worldOrigin, evalPoint, pedPosition);
    float value = targetPotential + obstaclePotential + pedestrianPotential;

    possiblePositions[get_global_id(0) * COORDOFFSET + X] = evalPoint.x;
    possiblePositions[get_global_id(0) * COORDOFFSET + Y] = evalPoint.y;
    potentialValues[get_global_id(0)] = value;

    // update event times once
    if(pointId == 0) {
        orderedEventTimes[pedId] += (stepSize / desiredSpeed);
    }
}

__kernel void eventTimes(
    __global int            *ids,               // out
    __global float          *eventTimes,        // out
    __global const uint     *d_CellStart,       //input: cell boundaries
    __global const uint     *d_CellEnd          //input
) {
    uint gid = get_global_id(0);
    float minEventTime = HUGE_VALF;
    int id = -1;
    for(uint i = d_CellStart[gid]; i < d_CellEnd[gid]; i++) {
        if(eventTimes[i] < minEventTime) {
            id = i;
            minEventTime = eventTimes[i];
        }
    }
    ids[gid] = id;
}

__kernel void filterIds(
    __global int            *ids,                 // in
    __global int            *mask,              // out
    __global float          *eventTimes,
    __constant const uint2  *gridSize,
    const float             simTimeInSec) {

    const uint gid = get_global_id(0);

    mask[gid] = (ids[gid] >= 0) ? 1 : - 1;

    if(ids[gid] >= 0 && eventTimes[ids[gid]] > simTimeInSec){
        mask[gid] = -1;
        //return;
    }

    if(mask[gid] >= 0) {
        for(int y = -1; y <= 1; y++) {
            for(int x = -1; x <= 1; x++){
                int2 gridPos = getGridPosFromIndex(gid, gridSize);
                int2 uGridPos = (gridPos + (int2)(x, y));
                if(isValidCell(gridSize, uGridPos)){
                    uint hash = getGridHash(uGridPos, gridSize);
                    if(ids[hash] >= 0 && eventTimes[ids[hash]] < eventTimes[ids[gid]]) {
                        mask[gid] = -1;
                    }
                }
            }
        }
    }
}

__kernel void align(
    __global int  *idsIn,       // in
    __global int  *prefixSum,   // in
    __global int  *mask,        // in
    __global int  *idsOut
    ) {

    const uint gid = get_global_id(0);
    const uint pedId = idsIn[gid];

    if(idsIn[gid] >= 0 && mask[gid] >= 0){
        idsOut[gid + prefixSum[gid]] = pedId;
    }
}

__kernel void minEventTimeLocal(
    __global float* minEventTime,       // out
    __global float* eventTimes,         // in
    __local  float* local_eventTimes,   // cache
    uint numberOfElements
){
    const uint gid = get_global_id(0);
    const uint local_size = get_local_size(0);

    if(numberOfElements > local_size) {
        uint elementsPerItem = ceil(numberOfElements / ((float)local_size));

        float minVal = HUGE_VALF;
        for(int i = gid * elementsPerItem; i < (gid + 1) * elementsPerItem; i++) {
            if(i < numberOfElements && minVal > eventTimes[i]) {
                minVal = eventTimes[i];
            }
        }

        local_eventTimes[get_local_id(0)] = minVal;
    } else {
        if(gid < numberOfElements) {
            local_eventTimes[get_local_id(0)] = eventTimes[gid];
        } else {
            local_eventTimes[get_local_id(0)] = HUGE_VALF;
        }
    }

    barrier(CLK_LOCAL_MEM_FENCE);
    // Reduce
    for(uint stride = get_local_size(0) / 2; stride >= 1; stride /= 2) {
        if (get_local_id(0) < stride) {
            local_eventTimes[get_local_id(0)] = min(local_eventTimes[get_local_id(0)], local_eventTimes[get_local_id(0) + stride]);
        }
        barrier(CLK_LOCAL_MEM_FENCE);
    }

    // Save
    (*minEventTime) = local_eventTimes[0];
}


__kernel void swap(
    __global const float    *d_ReorderedPedestrians,
    __global const float    *d_ReorderedPos,
    __global const float    *d_ReorderedEventTimes,
    __global float          *d_Pedestrians,
    __global float          *d_Pos,
    __global float          *d_eventTimes,
    uint                    numParticles
){
    const uint index = get_global_id(0);
    //TODO maybe split this into 4 kernels to improve memory loads / writes
    if(index < numParticles){
        d_Pos[index * COORDOFFSET + X] = d_ReorderedPos[index * COORDOFFSET + X];
        d_Pos[index * COORDOFFSET + Y] = d_ReorderedPos[index * COORDOFFSET + Y];

        d_Pedestrians[index * OFFSET + STEPSIZE] = d_ReorderedPedestrians[index * OFFSET + STEPSIZE];
        d_Pedestrians[index * OFFSET + DESIREDSPEED] = d_ReorderedPedestrians[index * OFFSET + DESIREDSPEED];

        d_eventTimes[index] = d_ReorderedEventTimes[index];
    }
}

__kernel void setMem(
    __global uint *d_Data,
    uint val,
    uint N
){
    if(get_global_id(0) < N)
        d_Data[get_global_id(0)] = val;
}

__kernel void swapIndex(
    __global uint           *d_GlobalIndexOut,
    __global const uint     *d_GlobalIndexIn,
    __global const uint     *d_Index,
    uint                    numberOfElements
){
    uint index = get_global_id(0);
    if(index < numberOfElements) {
        d_GlobalIndexOut[index] = d_GlobalIndexIn[d_Index[index]];
    }
}

//Calculate grid hash value for each particle
__kernel void calcHash(
    __global uint           *d_Hash,        //output
    __global uint           *d_Index,       //output
    __global const float            *d_Pos,         //input: positions
    __constant const float        *cellSize,
    __constant const float2       *worldOrigin,
    __constant const uint2        *gridSize,
    const uint                    numParticles
){
    const uint index = get_global_id(0);
    uint gridHash;
    if(index >= numParticles) {
        gridHash = (*gridSize).x * (*gridSize).y + 1;
    } else {
        const float2 p = (float2) (d_Pos[index * COORDOFFSET + X], d_Pos[index * COORDOFFSET + Y]);
        //Get address in grid
        int2 gridPos = getGridPos(p, cellSize, worldOrigin);
        gridHash = getGridHash(gridPos, gridSize);
    }
    //Store grid hash and particle index
    d_Hash[index] = gridHash;
    d_Index[index] = index;
}

////////////////////////////////////////////////////////////////////////////////
// Find cell bounds and reorder positions+velocities by sorted indices
////////////////////////////////////////////////////////////////////////////////
__kernel void findCellBoundsAndReorder(
    __global uint           *d_CellStart,     //output: cell start index
    __global uint           *d_CellEnd,       //output: cell end index
    __global float          *d_ReorderedPedestrians,  //output: reordered by cell hash positions
    __global float          *d_ReorderedPos,  //output: reordered by cell hash positions
    __global float          *d_ReorderedEventTimes,
    __global const uint     *d_Hash,    //input: sorted grid hashes
    __global const uint     *d_Index,
    __global float          *d_Pedestrians,  //output: reordered by cell hash positions
    __global const float    *d_Pos,     //input: positions array sorted by hash
    __global const float    *d_eventTimes,
    __local uint            *localHash,          //get_group_size(0) + 1 elements
    uint                    numParticles
){
    uint hash;
    const uint index = get_global_id(0);

    //Handle case when no. of particles not multiple of block size
    if(index < numParticles){
        hash = d_Hash[index];

        //Load hash data into local memory so that we can look
        //at neighboring particle's hash value without loading
        //two hash values per thread
        localHash[get_local_id(0) + 1] = hash;

        //First thread in block must load neighbor particle hash
        if(index > 0 && get_local_id(0) == 0)
            localHash[0] = d_Hash[index - 1];
    }

    barrier(CLK_LOCAL_MEM_FENCE);

    if(index < numParticles){
        //Border case
        if(index == 0)
            d_CellStart[hash] = 0;

        //Main case
        else{
            if(hash != localHash[get_local_id(0)])
                d_CellEnd[localHash[get_local_id(0)]]  = d_CellStart[hash] = index;
        };

        //Another border case
        if(index == numParticles - 1)
            d_CellEnd[hash] = numParticles;


        //Now use the sorted index to reorder the pos and vel arrays
        uint sortedIndex = d_Index[index];

        d_ReorderedPos[index * COORDOFFSET + X] = d_Pos[sortedIndex * COORDOFFSET + X];
        d_ReorderedPos[index * COORDOFFSET + Y] = d_Pos[sortedIndex * COORDOFFSET + Y];

        d_ReorderedPedestrians[index * OFFSET + STEPSIZE] = d_Pedestrians[sortedIndex * OFFSET + STEPSIZE];
        d_ReorderedPedestrians[index * OFFSET + DESIREDSPEED] = d_Pedestrians[sortedIndex * OFFSET + DESIREDSPEED];

        d_ReorderedEventTimes[index] = d_eventTimes[sortedIndex];
    }
}

////////////////////////////////////////////////////////////////////////////////
// Monolithic bitonic sort kernel for short arrays fitting into local memory
////////////////////////////////////////////////////////////////////////////////
__kernel void bitonicSortLocal(
    __global uint *d_DstKey,
    __global uint *d_DstVal,
    __global uint *d_SrcKey,
    __global uint *d_SrcVal,
    uint arrayLength,
    uint dir,
    __local uint *l_key,
    __local uint *l_val
){
    uint LOCAL_SIZE_LIMIT = get_local_size(0) * 2;

    //Offset to the beginning of subbatch and load data
    d_SrcKey += get_group_id(0) * LOCAL_SIZE_LIMIT + get_local_id(0);
    d_SrcVal += get_group_id(0) * LOCAL_SIZE_LIMIT + get_local_id(0);
    d_DstKey += get_group_id(0) * LOCAL_SIZE_LIMIT + get_local_id(0);
    d_DstVal += get_group_id(0) * LOCAL_SIZE_LIMIT + get_local_id(0);
    l_key[get_local_id(0) +                      0] = d_SrcKey[                     0];
    l_val[get_local_id(0) +                      0] = d_SrcVal[                     0];
    l_key[get_local_id(0) + (LOCAL_SIZE_LIMIT / 2)] = d_SrcKey[(LOCAL_SIZE_LIMIT / 2)];
    l_val[get_local_id(0) + (LOCAL_SIZE_LIMIT / 2)] = d_SrcVal[(LOCAL_SIZE_LIMIT / 2)];

    for(uint size = 2; size < arrayLength; size <<= 1){
        //Bitonic merge
        uint ddd = dir ^ ( (get_local_id(0) & (size / 2)) != 0 );
        for(uint stride = size / 2; stride > 0; stride >>= 1){
            barrier(CLK_LOCAL_MEM_FENCE);
            uint pos = 2 * get_local_id(0) - (get_local_id(0) & (stride - 1));
            ComparatorLocal(
                &l_key[pos +      0], &l_val[pos +      0],
                &l_key[pos + stride], &l_val[pos + stride],
                ddd
            );
        }
    }

    //ddd == dir for the last bitonic merge step
    {
        for(uint stride = arrayLength / 2; stride > 0; stride >>= 1){
            barrier(CLK_LOCAL_MEM_FENCE);
            uint pos = 2 * get_local_id(0) - (get_local_id(0) & (stride - 1));
            ComparatorLocal(
                &l_key[pos +      0], &l_val[pos +      0],
                &l_key[pos + stride], &l_val[pos + stride],
                dir
            );
        }
    }

    barrier(CLK_LOCAL_MEM_FENCE);
    d_DstKey[                     0] = l_key[get_local_id(0) +                      0];
    d_DstVal[                     0] = l_val[get_local_id(0) +                      0];
    d_DstKey[(LOCAL_SIZE_LIMIT / 2)] = l_key[get_local_id(0) + (LOCAL_SIZE_LIMIT / 2)];
    d_DstVal[(LOCAL_SIZE_LIMIT / 2)] = l_val[get_local_id(0) + (LOCAL_SIZE_LIMIT / 2)];
}

////////////////////////////////////////////////////////////////////////////////
// Bitonic sort kernel for large arrays (not fitting into local memory)
////////////////////////////////////////////////////////////////////////////////
//Bottom-level bitonic sort
//Almost the same as bitonicSortLocal with the only exception
//of even / odd subarrays (of LOCAL_SIZE_LIMIT points) being
//sorted in opposite directions
__kernel void bitonicSortLocal1(
    __global uint *d_DstKey,
    __global uint *d_DstVal,
    __global uint *d_SrcKey,
    __global uint *d_SrcVal,
    __local uint *l_key,
    __local uint *l_val
){
    uint LOCAL_SIZE_LIMIT = get_local_size(0) * 2;
    //Offset to the beginning of subarray and load data
    d_SrcKey += get_group_id(0) * LOCAL_SIZE_LIMIT + get_local_id(0);
    d_SrcVal += get_group_id(0) * LOCAL_SIZE_LIMIT + get_local_id(0);
    d_DstKey += get_group_id(0) * LOCAL_SIZE_LIMIT + get_local_id(0);
    d_DstVal += get_group_id(0) * LOCAL_SIZE_LIMIT + get_local_id(0);
    l_key[get_local_id(0) +                      0] = d_SrcKey[                     0];
    l_val[get_local_id(0) +                      0] = d_SrcVal[                     0];
    l_key[get_local_id(0) + (LOCAL_SIZE_LIMIT / 2)] = d_SrcKey[(LOCAL_SIZE_LIMIT / 2)];
    l_val[get_local_id(0) + (LOCAL_SIZE_LIMIT / 2)] = d_SrcVal[(LOCAL_SIZE_LIMIT / 2)];

    uint comparatorI = get_global_id(0) & ((LOCAL_SIZE_LIMIT / 2) - 1);

    for(uint size = 2; size < LOCAL_SIZE_LIMIT; size <<= 1){
        //Bitonic merge
        uint ddd = (comparatorI & (size / 2)) != 0;
        for(uint stride = size / 2; stride > 0; stride >>= 1){
            barrier(CLK_LOCAL_MEM_FENCE);
            uint pos = 2 * get_local_id(0) - (get_local_id(0) & (stride - 1));
            ComparatorLocal(
                &l_key[pos +      0], &l_val[pos +      0],
                &l_key[pos + stride], &l_val[pos + stride],
                ddd
            );
        }
    }

    //Odd / even arrays of LOCAL_SIZE_LIMIT elements
    //sorted in opposite directions
    {
        uint ddd = (get_group_id(0) & 1);
        for(uint stride = LOCAL_SIZE_LIMIT / 2; stride > 0; stride >>= 1){
            barrier(CLK_LOCAL_MEM_FENCE);
            uint pos = 2 * get_local_id(0) - (get_local_id(0) & (stride - 1));
            ComparatorLocal(
                &l_key[pos +      0], &l_val[pos +      0],
                &l_key[pos + stride], &l_val[pos + stride],
               ddd
            );
        }
    }

    barrier(CLK_LOCAL_MEM_FENCE);
    d_DstKey[                     0] = l_key[get_local_id(0) +                      0];
    d_DstVal[                     0] = l_val[get_local_id(0) +                      0];
    d_DstKey[(LOCAL_SIZE_LIMIT / 2)] = l_key[get_local_id(0) + (LOCAL_SIZE_LIMIT / 2)];
    d_DstVal[(LOCAL_SIZE_LIMIT / 2)] = l_val[get_local_id(0) + (LOCAL_SIZE_LIMIT / 2)];
}

//Bitonic merge iteration for 'stride' >= LOCAL_SIZE_LIMIT
__kernel void bitonicMergeGlobal(
    __global uint *d_DstKey,
    __global uint *d_DstVal,
    __global uint *d_SrcKey,
    __global uint *d_SrcVal,
    uint arrayLength,
    uint size,
    uint stride,
    uint dir
){
    uint global_comparatorI = get_global_id(0);
    uint        comparatorI = global_comparatorI & (arrayLength / 2 - 1);

    //Bitonic merge
    uint ddd = dir ^ ( (comparatorI & (size / 2)) != 0 );
    uint pos = 2 * global_comparatorI - (global_comparatorI & (stride - 1));

    uint keyA = d_SrcKey[pos +      0];
    uint valA = d_SrcVal[pos +      0];
    uint keyB = d_SrcKey[pos + stride];
    uint valB = d_SrcVal[pos + stride];

    ComparatorPrivate(
        &keyA, &valA,
        &keyB, &valB,
        ddd
    );

    d_DstKey[pos +      0] = keyA;
    d_DstVal[pos +      0] = valA;
    d_DstKey[pos + stride] = keyB;
    d_DstVal[pos + stride] = valB;
}

//Combined bitonic merge steps for
//'size' > LOCAL_SIZE_LIMIT and 'stride' = [1 .. LOCAL_SIZE_LIMIT / 2]
__kernel void bitonicMergeLocal(
    __global uint *d_DstKey,
    __global uint *d_DstVal,
    __global uint *d_SrcKey,
    __global uint *d_SrcVal,
    uint arrayLength,
    uint stride,
    uint size,
    uint dir,
    __local uint *l_key,
    __local uint *l_val
){
    uint LOCAL_SIZE_LIMIT = get_local_size(0) * 2;
    d_SrcKey += get_group_id(0) * LOCAL_SIZE_LIMIT + get_local_id(0);
    d_SrcVal += get_group_id(0) * LOCAL_SIZE_LIMIT + get_local_id(0);
    d_DstKey += get_group_id(0) * LOCAL_SIZE_LIMIT + get_local_id(0);
    d_DstVal += get_group_id(0) * LOCAL_SIZE_LIMIT + get_local_id(0);
    l_key[get_local_id(0) +                      0] = d_SrcKey[                     0];
    l_val[get_local_id(0) +                      0] = d_SrcVal[                     0];
    l_key[get_local_id(0) + (LOCAL_SIZE_LIMIT / 2)] = d_SrcKey[(LOCAL_SIZE_LIMIT / 2)];
    l_val[get_local_id(0) + (LOCAL_SIZE_LIMIT / 2)] = d_SrcVal[(LOCAL_SIZE_LIMIT / 2)];

    //Bitonic merge
    uint comparatorI = get_global_id(0) & ((arrayLength / 2) - 1);
    uint         ddd = dir ^ ( (comparatorI & (size / 2)) != 0 );
    for(; stride > 0; stride >>= 1){
        barrier(CLK_LOCAL_MEM_FENCE);
        uint pos = 2 * get_local_id(0) - (get_local_id(0) & (stride - 1));
        ComparatorLocal(
            &l_key[pos +      0], &l_val[pos +      0],
            &l_key[pos + stride], &l_val[pos + stride],
            ddd
        );
    }

    barrier(CLK_LOCAL_MEM_FENCE);
    d_DstKey[                     0] = l_key[get_local_id(0) +                      0];
    d_DstVal[                     0] = l_val[get_local_id(0) +                      0];
    d_DstKey[(LOCAL_SIZE_LIMIT / 2)] = l_key[get_local_id(0) + (LOCAL_SIZE_LIMIT / 2)];
    d_DstVal[(LOCAL_SIZE_LIMIT / 2)] = l_val[get_local_id(0) + (LOCAL_SIZE_LIMIT / 2)];
}



/*
 * Inplace upsweep (reduce) on a local array [x] of length [m].
 * NB: [m] must be a power of two.
 */
inline void upsweep_pow2(__local int *x, int m, int dir) {
  int lid = get_local_id(0);
  int bi = (lid*2)+1;

  int depth = 1 + (int) log2((float)m);
  for (int d=0; d<depth; d++) {
    barrier(CLK_LOCAL_MEM_FENCE);
    int mask = (0x1 << d) - 1;
    if ((lid & mask) == mask) {
      int offset = (0x1 << d);
      int ai = bi - offset;
      x[bi] += (x[ai] * dir > 0 ? x[ai] : 0);
    }
  }
}

/*
 * Inplace sweepdown on a local array [x] of length [m].
 * NB: [m] must be a power of two.
 */
inline void sweepdown_pow2(__local int *x, int m, int dir) {
  int lid = get_local_id(0);
  int bi = (lid*2)+1;

  int depth = (int) log2((float)m);
  for (int d=depth; d>-1; d--) {
    barrier(CLK_LOCAL_MEM_FENCE);
    int mask = (0x1 << d) - 1;
    if ((lid & mask) == mask) {
      int offset = (0x1 << d);
      int ai = bi - offset;
      int tmp = x[ai];
                x[ai] = x[bi];
                        x[bi] += (tmp * dir > 0 ? tmp : 0);
    }
  }
}

/*
 * Inplace scan on a local array [x] of length [m].
 * NB: m must be a power of two.
 */
inline void scan_pow2(__local int *x, int m, int dir) {
  int lid = get_local_id(0);
  int lane1 = (lid*2)+1;
  upsweep_pow2(x, m, dir);
  if (lane1 == (m-1)) {
    x[lane1] = 0;
  }
  sweepdown_pow2(x, m, dir);
}

/*
 * Inplace scan on a global array [data] of length [m].
 * We load data into a local array [x] (also of length [m]),
 *   and use a local upsweep and sweepdown.
 * NB: [m] must be a power of two, and
 *     there must be exactly one workgroup of size m/2
 */
__kernel void scan_pow2_wrapper(__global int *data, __local int *x, int m, int dir) {
  int gid = get_global_id(0);
  int lane0 = (gid*2);
  int lane1 = (gid*2)+1;

  // load data into local arrays
  x[lane0] = (data[lane0] * dir > 0 ? data[lane0] : 0);
  x[lane1] = (data[lane1] * dir > 0 ? data[lane1] : 0);

  // inplace local scan
  scan_pow2(x, m, dir);

  // writeback data
  data[lane0] = x[lane0];
  data[lane1] = x[lane1];
}

__kernel void scan_pad_to_pow2(__global int *data, __local int * x, int n, int dir) {
  int gid = get_global_id(0);
  int lane0 = (gid*2);
  int lane1 = (gid*2)+1;
  int m = 2*get_local_size(0);

  x[lane0] = (lane0 < n && data[lane0] * dir > 0) ? data[lane0] : 0;
  x[lane1] = (lane1 < n && data[lane1] * dir > 0) ? data[lane1] : 0;

  upsweep_pow2(x, m, dir);
  if (lane1 == (m-1)) {
    x[lane1] = 0;
  }
  sweepdown_pow2(x, m, dir);

  if (lane0 < n)
    data[lane0] = x[lane0];
  if (lane1 < n)
    data[lane1] = x[lane1];
}

/*
 * First phase of a multiblock scan.
 *
 * Given a global array [data] of length arbitrary length [n].
 * We assume that we have k workgroups each of size m/2 workitems.
 * Each workgroup handles a subarray of length [m] (where m is a power of two).
 * The last subarray will be padded with 0 if necessary (n < k*m).
 * We use the primitives above to perform a scan operation within each subarray.
 * We store the intermediate reduction of each subarray (following upsweep_pow2) in [part].
 * These partial values can themselves be scanned and fed into [scan_inc_subarrays].
 */
__kernel void scan_subarrays(
  __global int *data, //length [n]
  __local  int *x,    //length [m]
  __global int *part, //length [m]
           int n,
           int dir
#if DEBUG
  , __global int *debug   //length [k*m]
#endif
) {
  // workgroup size
  int wx = get_local_size(0);
  // global identifiers and indexes
  int gid = get_global_id(0);
  int lane0 = (2*gid)  ;
  int lane1 = (2*gid)+1;
  // local identifiers and indexes
  int lid = get_local_id(0);
  int local_lane0 = (2*lid)  ;
  int local_lane1 = (2*lid)+1;
  int grpid = get_group_id(0);
  // list lengths
  int m = wx * 2;
  int k = get_num_groups(0);

  // copy into local data padding elements >= n with 0
  x[local_lane0] = (lane0 < n && data[lane0] * dir > 0) ? data[lane0] : 0;
  x[local_lane1] = (lane1 < n && data[lane1] * dir > 0) ? data[lane1] : 0;

  // ON EACH SUBARRAY
  // a reduce on each subarray
  upsweep_pow2(x, m, dir);
  // last workitem per workgroup saves last element of each subarray in [part] before zeroing
  if (lid == (wx-1)) {
    part[grpid] = x[local_lane1];
                  x[local_lane1] = 0;
  }
  // a sweepdown on each subarray
  sweepdown_pow2(x, m, dir);

  // copy back to global data
  if (lane0 < n) {
    data[lane0] = x[local_lane0];
  }
  if (lane1 < n) {
    data[lane1] = x[local_lane1];
  }

#if DEBUG
  debug[lane0] = x[local_lane0];
  debug[lane1] = x[local_lane1];
#endif
}

/*
 * Perform the second phase of an inplace exclusive scan on a global array [data] of arbitrary length [n].
 *
 * We assume that we have k workgroups each of size m/2 workitems.
 * Each workgroup handles a subarray of length [m] (where m is a power of two).
 * We sum each element by the sum of the preceding subarrays taken from [part].
 */
__kernel void scan_inc_subarrays(
  __global int *data, //length [n]
  __local  int *x,    //length [m]
  __global int *part, //length [m]
           int n,
           int dir
#if DEBUG
  , __global int *debug   //length [k*m]
#endif
) {
  // global identifiers and indexes
  int gid = get_global_id(0);
  int lane0 = (2*gid)  ;
  int lane1 = (2*gid)+1;
  // local identifiers and indexes
  int lid = get_local_id(0);
  int local_lane0 = (2*lid)  ;
  int local_lane1 = (2*lid)+1;
  int grpid = get_group_id(0);

  // copy into local data padding elements >= n with identity
  x[local_lane0] = (lane0 < n && data[lane0] * dir > 0) ? data[lane0] : 0;
  x[local_lane1] = (lane1 < n && data[lane1] * dir > 0) ? data[lane1] : 0;

  x[local_lane0] += (part[grpid] * dir > 0 ? part[grpid] : 0);
  x[local_lane1] += (part[grpid] * dir > 0 ? part[grpid] : 0);

  // copy back to global data
  if (lane0 < n) {
    data[lane0] = x[local_lane0];
  }
  if (lane1 < n) {
    data[lane1] = x[local_lane1];
  }

#if DEBUG
  debug[lane0] = x[local_lane0];
  debug[lane1] = x[local_lane1];
#endif
}