/*
 * Copyright 1993-2010 NVIDIA Corporation.  All rights reserved.
 *
 * Please refer to the NVIDIA end user license agreement (EULA) associated
 * with this source code for terms and conditions that govern your use of
 * this software. Any use, reproduction, disclosure, or distribution of
 * this software and related documentation outside the terms of the EULA
 * is strictly prohibited.
 *
 */

//#pragma OPENCL EXTENSION cl_amd_printf : enable

////////////////////////////////////////////////////////////////////////////////
// Common definitions
////////////////////////////////////////////////////////////////////////////////
#define UMAD(a, b, c)  ( (a) * (b) + (c) )

#define LOCAL_PED_COUNT_ID 5
#define RADIUS 0.2
#define DIAMETER 0.4

#define POTENTIAL_WIDTH 0.5f

#define COORDOFFSET 2
#define X 0
#define Y 1

#define OFFSET 5
#define STEPSIZE 0
#define DESIREDSPEED 1
#define TIMECREDIT 2
#define NEWX 3
#define NEWY 4

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
inline uint2 getGridPos(const float2 p, __constant const float* cellSize, __constant const float2* worldOrigin){
    uint2 gridPos;
    float2 wordOr = (*worldOrigin);
    gridPos.x = (uint)max(0, (int)floor((p.x - wordOr.x) / (*cellSize)));
    gridPos.y = (uint)max(0, (int)floor((p.y - wordOr.y) / (*cellSize)));
    return gridPos;
}

//Calculate address in grid from position (clamping to edges)
inline uint getGridHash(const uint2 gridPos, __constant const uint2* gridSize){
    //Wrap addressing, assume power-of-two grid dimensions
    //gridPos.x = gridPos.x & ((*gridSize).x - 1);
    //gridPos.y = gridPos.y & ((*gridSize).y - 1);
    // a * b + c = hash => b + c = hash / a, b = hash / a - c
    return UMAD(  (*gridSize).x, gridPos.y, gridPos.x );
}

inline uint2 getGridPosFromIndex(const uint hash, __constant const uint2* gridSize){
    uint y = hash / (*gridSize).x;
    uint x = hash - ((*gridSize).x * y);
    return (uint2) (x, y);
}

////////////////////////////////////////////////////////////////////////////////
// Potential field helper methods
////////////////////////////////////////////////////////////////////////////////

// see PotentialFieldPedestrianCompact with useHardBodyShell = false:
/*inline float getPedestrianPotential(const float2 pos, const float2 otherPedPosition) {
    float d = distance(pos, otherPedPosition);
    if (d < POTENTIAL_WIDTH) {
        return 12.6f * native_exp(1 / (pown(d / POTENTIAL_WIDTH, 2) - 1));
    } else {
        return 0.0f;
    }
}*/

// see PotentialFieldPedestrianCompact with useHardBodyShell = false:
inline float getPedestrianPotential(const float2 pos, const float2 otherPedPosition) {
    float d = distance(pos, otherPedPosition);
    float width = 0.5f;
    float height = 12.6f;
    if (d < width) {
        return 12.6f * native_exp(1 / (pown(d / 0.5f, 2) - 1));
    } else {
        return 0.0f;
    }
}

inline float getLocalFullPedestrianPotential(__local float *localPositions, const uint size, const float2 pos){
    float potential = 0.0f;
    for(uint j = 0; j < size; j++){
        float2 otherPos = (float2) (localPositions[j * OFFSET + NEWX], localPositions[j * OFFSET + NEWY]);
        potential += getPedestrianPotential(pos, otherPos);
    }
    return potential;
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
    uint2 gridPos = getGridPos(pedPosition, cellSize, worldOrigin);
    for(int y = -1; y <= 1; y++) {
        for(int x = -1; x <= 1; x++){
            uint2 uGridPos = (uint2)(gridPos - (int2)(x, y));

            // note if uGridPos.x == 0 than uGridPos.x -1 = 2^N - 1 and the step is also continued!
            if(uGridPos.x > (*gridSize).x || uGridPos.y > (*gridSize).y){
                continue;
            }
            uint   hash = getGridHash(uGridPos, gridSize);
            uint startI = d_CellStart[hash];

            //Skip empty cell
            //if(startI == 0xFFFFFFFFU)
            //    continue;
            //Iterate over particles in this cell
            uint endI = d_CellEnd[hash];
            for(uint j = startI; j < endI; j++){
                // TODO: seperate position from rest , remove global memory access
                float2 otherPos = (float2) (orderedPositions[j * COORDOFFSET + X], orderedPositions[j * COORDOFFSET + Y]);
                potential += getPedestrianPotential(pos, otherPos);
            }
        }
    }
    potential -= getPedestrianPotential(pos, pedPosition);
    return potential;
}

inline bool hasConflict(
        __global float  *orderedPedestrians,  //input
        __global const uint   *d_CellStart,
        __global const uint   *d_CellEnd,
        __constant const float        *cellSize,
        __constant const uint2        *gridSize,
        __constant const float2       *worldOrigin,
        const float timeCredit,
        const float2 pedPosition)
{
    uint2 gridPos = getGridPos(pedPosition, cellSize, worldOrigin);
    uint collisions = 0;
    for(int y = -1; y <= 1; y++) {
        for(int x = -1; x <= 1; x++){
           uint2 uGridPos = gridPos - (int2)(x, y);

            // note if uGridPos.x == 0 than uGridPos.x -1 = 2^N - 1 and the step is also continued!
            if(uGridPos.x > (*gridSize).x || uGridPos.y > (*gridSize).y){
                continue;
            }
            uint   hash = getGridHash(uGridPos, gridSize);
            uint startI = d_CellStart[hash];

            //Skip empty cell
            //if(startI == 0xFFFFFFFFU)
            //    continue;
            //Iterate over particles in this cell
            uint endI = d_CellEnd[hash];
            for(uint j = startI; j < endI; j++){
                float2 otherPedestrian = (float2) (orderedPedestrians[j * OFFSET + NEWX], orderedPedestrians[j * OFFSET + NEWY]);
                float otherTimeCredit = orderedPedestrians[j * OFFSET + TIMECREDIT];

                // for itself dist < RADIUS but otherTimeCredit == timeCredit and otherPedestrian.x == pedPosition.x
                if(distance(otherPedestrian, pedPosition) < DIAMETER &&
                        (otherTimeCredit < timeCredit || (otherTimeCredit == timeCredit && otherPedestrian.x < pedPosition.x))) {
                    collisions = collisions + 1;
                }
            }
        }
    }
    return collisions >= 1;
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

inline float2 bilinearInterpolationWithUnkown(float4 z, float2 delta) {
    //float knownWeights = 0;
    float4 weights = (float4)((1.0f - delta.x) * (1.0f - delta.y), delta.x * (1.0f - delta.y), delta.x * delta.y, (1.0f - delta.x) * delta.y);
    float4 result = weights * z;
    return (float2) (result.s0 + result.s1 + result.s2 + result.s3, weights.s0 + weights.s1 + weights.s2 + weights.s3);
}

inline float getPotentialFieldValue(float2 evalPoint, __global const float *potentialField, float potentialCellSize, float2 potentialFieldSize, uint2 potentialGridSize) {
    uint2 gridPoint = getNearestPointTowardsOrigin(evalPoint, potentialCellSize, potentialFieldSize);
    float2 gridPointCoord = pointToCoord(gridPoint, potentialCellSize);
    uint incX = 1, incY = 1;

    if (evalPoint.x >= potentialFieldSize.x) {
        incX = 0;
    }

    if (evalPoint.y >= potentialFieldSize.y) {
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
        currentPotential = height * exp(1.0f / (pow(minDistanceToObstacle / width, 2) - 1.0f));
    }

    currentPotential = max(0.0f, currentPotential);
    return currentPotential;
}

// end potential field helper methods

__kernel void seek(
    __global float          *orderedPedestrians,    //input
    __global float          *orderedPositions,      //input
    __global const float2   *circlePositions,       //input
    __global const uint     *d_CellStart,           //input: cell boundaries
    __global const uint     *d_CellEnd,             //input
    __constant const float  *cellSize,
    __constant const uint2  *gridSize,
    __global const float    *distanceField,         //input
    __global const float    *targetPotentialField,  //input
    __global const uint     *maxPedsInCell,
    __constant const float2 *worldOrigin,           //input
    __constant const uint2  *potentialGridSize,
    __constant const float2 *potentialFieldSize,    //input
    const float             potentialCellSize,      //input
    const float             timeStepInSec,
    const uint              numberOfPoints,         //input
    __local float           *localPositions,
    __local float           *results){

    __local int             pedCount[10];
    //const uint numberOfPedsPerCell = 8;
    const uint lid = get_local_id(0);
    const uint gid = get_global_id(0);
    const uint hash = get_group_id(0);

    const uint startI = d_CellStart[hash];
    const uint endI = d_CellEnd[hash];
    const uint numberOfPedsInCell = endI - startI;
    const uint numberOfEvals = numberOfPedsInCell * numberOfPoints;
    const uint2 cell = getGridPosFromIndex(hash, gridSize);

    // empty cell
    if(lid < 9) {
        int y = lid / 3 - 1;
        int x = lid - ((y + 1) * 3) - 1;
        uint2 uGridPos = (uint2)(cell.x + x, cell.y + y);
        if(uGridPos.x < (*gridSize).x && uGridPos.y < (*gridSize).y) {
            uint hashIO = getGridHash(uGridPos, gridSize);
            pedCount[lid+1] = d_CellEnd[hashIO] - d_CellStart[hashIO];
        }
        else {
            pedCount[lid+1] = 0;
        }
    }
    barrier(CLK_LOCAL_MEM_FENCE);

    if(lid == 0) {
        pedCount[0] = 0;
        for(int i = 1; i < 10; i++) {
            pedCount[i] = pedCount[i-1] + pedCount[i];
        }
    }

    barrier(CLK_LOCAL_MEM_FENCE);

    // cell is not empty
    if(startI < endI) {
        // load positions around the cell into local memory (aligned!)
        for(int y = -1; y <= 1; y++) {
            for(int x = -1; x <= 1; x++){
                uint2 uGridPos = (uint2)(cell.x + x, cell.y + y);
                // note if uGridPos.x == 0 than uGridPos.x -1 = 2^N - 1 and the step is also continued!
                if(uGridPos.x < (*gridSize).x && uGridPos.y < (*gridSize).y){
                    uint hashIOO = getGridHash(uGridPos, gridSize);
                    uint startIO = d_CellStart[hashIOO];
                    uint endIO = d_CellEnd[hashIOO];

                    // other cell is not empty
                    if(startIO + lid < endIO){
                        int c = 4 + x + 3 * y;
                        // here we look how many agents are before
                        //TODO here we access global memory which might be highly distributed.
                        localPositions[(pedCount[c] + lid) * OFFSET + STEPSIZE] = orderedPedestrians[(startIO + lid) * OFFSET + STEPSIZE];
                        localPositions[(pedCount[c] + lid) * OFFSET + DESIREDSPEED] = orderedPedestrians[(startIO + lid) * OFFSET + DESIREDSPEED];
                        localPositions[(pedCount[c] + lid) * OFFSET + TIMECREDIT] = orderedPedestrians[(startIO + lid) * OFFSET + TIMECREDIT] + timeStepInSec;
                        localPositions[(pedCount[c] + lid) * OFFSET + NEWX] = orderedPositions[(startIO + lid) * COORDOFFSET + X];
                        localPositions[(pedCount[c] + lid) * OFFSET + NEWY] = orderedPositions[(startIO + lid) * COORDOFFSET + Y];
                    }
                }
            }
        }
        barrier(CLK_LOCAL_MEM_FENCE);

        // from here on we only use shared memory!
        for(int j = 0; j < numberOfEvals; j += get_local_size(0)) {
            uint pointNr = j + lid;
            uint pedNr = pointNr / numberOfPoints;
            uint pointId = pointNr - numberOfPoints * pedNr;

            if(pedNr < pedCount[LOCAL_PED_COUNT_ID]) {
                uint offset = (pedCount[LOCAL_PED_COUNT_ID-1] + pedNr) * OFFSET;
                float stepSize = localPositions[offset + STEPSIZE];
                float desiredSpeed = localPositions[offset + DESIREDSPEED];
                float timeCredit = localPositions[offset + TIMECREDIT];
                float duration = stepSize / desiredSpeed;
                if(duration <= timeCredit) {
                    float2 pedPosition = (float2)(localPositions[offset + NEWX],
                                                  localPositions[offset + NEWY]);
                    float2 circlePosition = circlePositions[pointId];
                    uint pedId = startI + pedNr;

                    // TODO: use local memory
                    float2 evalPoint = pedPosition + (float2)(circlePosition * stepSize);
                    float targetPotential = getPotentialFieldValue(evalPoint, targetPotentialField, potentialCellSize, (*potentialFieldSize), (*potentialGridSize));
                    float minDistanceToObstacle = getPotentialFieldValue(evalPoint, distanceField, potentialCellSize, (*potentialFieldSize), (*potentialGridSize));
                    float obstaclePotential = getObstaclePotential(minDistanceToObstacle);
                    float pedestrianPotential = getLocalFullPedestrianPotential(localPositions, numberOfPoints, pedPosition);
                    //float pedestrianPotential = 0.0f;
                    float value = targetPotential + obstaclePotential + pedestrianPotential;
                    results[j] = value;
                }
            }
        }
        barrier(CLK_LOCAL_MEM_FENCE);

        // finally find the best solution
        if(lid < numberOfPedsInCell) {
            uint pedId = startI + lid;
            uint offset = (pedCount[LOCAL_PED_COUNT_ID-1] + lid) * OFFSET;
            float2 pedPosition = (float2)(  localPositions[offset + NEWX],
                                            localPositions[offset + NEWY]);

            float stepSize = localPositions[offset + STEPSIZE];
            float desiredSpeed = localPositions[offset + DESIREDSPEED];
            float timeCredit = localPositions[offset + TIMECREDIT];
            float duration = stepSize / desiredSpeed;

            if(duration <= timeCredit) {
                float minVal = 100000;
                float2 minArg = (0.0f, 0.0f);
                for(uint i = 0; i < numberOfPoints; i++) {
                    if(results[lid * numberOfPoints + i] < minVal){
                        minVal = results[lid * numberOfPoints + i];
                        minArg = pedPosition + (float2)(circlePositions[i] * stepSize);
                    }
                }

                // write back to global memory
                orderedPedestrians[pedId * OFFSET + NEWX] = minArg.x;
                orderedPedestrians[pedId * OFFSET + NEWY] = minArg.y;
            }
            orderedPedestrians[pedId * OFFSET + TIMECREDIT] = timeCredit;
        }
    }
}

__kernel void move(
    __global float                *orderedPedestrians,    //input, update this
    __global float                *orderedPositions,    //input, update this
    __global const uint           *d_CellStart,           //input: cell boundaries
    __global const uint           *d_CellEnd,             //input
    __constant const float        *cellSize,              //input
    __constant const uint2        *gridSize,              //input
    __constant const float2       *worldOrigin,           //input
    const float                   timeStepInSec,
    const uint                    numberOfPedestrians    //input
){
    const uint index = get_global_id(0);
    if(index < numberOfPedestrians) {
        float2 newPedPosition = (float2)(orderedPedestrians[index * OFFSET + NEWX], orderedPedestrians[index * OFFSET + NEWY]);
        float stepSize = orderedPedestrians[index * OFFSET + STEPSIZE];
        float desiredSpeed = orderedPedestrians[index * OFFSET + DESIREDSPEED];
        float timeCredit = orderedPedestrians[index * OFFSET + TIMECREDIT];
        float duration = stepSize / desiredSpeed;

        if(duration <= timeCredit && !hasConflict(orderedPedestrians, d_CellStart, d_CellEnd, cellSize, gridSize, worldOrigin, timeCredit, newPedPosition)) {
            orderedPositions[index * COORDOFFSET + X] = orderedPedestrians[index * OFFSET + NEWX];
            orderedPositions[index * COORDOFFSET + Y] = orderedPedestrians[index * OFFSET + NEWY];
            orderedPedestrians[index * OFFSET + TIMECREDIT] = orderedPedestrians[index * OFFSET + TIMECREDIT] - timeStepInSec;
        }
    }
}

__kernel void swap(
    __global const float  *d_ReorderedPedestrians,
    __global const float  *d_ReorderedPos,
    __global float  *d_Pedestrians,
    __global float  *d_Pos,
    uint    numParticles
){
    const uint index = get_global_id(0);
    if(index < numParticles){
        d_Pos[index * COORDOFFSET + X] = d_ReorderedPos[index * COORDOFFSET + X];
        d_Pos[index * COORDOFFSET + Y] = d_ReorderedPos[index * COORDOFFSET + Y];

        d_Pedestrians[index * OFFSET + STEPSIZE] = d_ReorderedPedestrians[index * OFFSET + STEPSIZE];
        d_Pedestrians[index * OFFSET + DESIREDSPEED] = d_ReorderedPedestrians[index * OFFSET + DESIREDSPEED];
        d_Pedestrians[index * OFFSET + TIMECREDIT] = d_ReorderedPedestrians[index * OFFSET + TIMECREDIT];
        d_Pedestrians[index * OFFSET + NEWX] = d_ReorderedPedestrians[index * OFFSET + NEWX];
        d_Pedestrians[index * OFFSET + NEWY] = d_ReorderedPedestrians[index * OFFSET + NEWY];
    }
}

// TODO: this kernel does only run on 1
__kernel void count(
    __global uint           *maxPedsInCell, // output
    __global uint           *d_CellStart,   // input: cell start index
    __global uint           *d_CellEnd,     // input: cell end index
    __constant const uint2  *gridSize       // input
) {
    if(get_global_id(0) == 0) {
        uint maxVal = 0;
        for(int i = 0; i < (*gridSize).x * (*gridSize).y; i++) {
            maxVal = max(maxVal, d_CellEnd[i] - d_CellStart[i]);
        }

        (*maxPedsInCell) = maxVal;
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
        uint2 gridPos = getGridPos(p, cellSize, worldOrigin);
        gridHash = getGridHash(gridPos, gridSize);
    }
    //Store grid hash and particle index
    d_Hash[index] = gridHash;
    d_Index[index] = index;
}

/*__kernel void calcHash(
    __global uint           *d_Hash, //output
    __global uint           *d_Index, //output
    __global const float    *d_Pos, //input: positions
    __constant float        *cellSize,
    __constant float2       *worldOrigin,
    __constant uint2        *gridSize,
    uint numParticles
){
    const uint index = get_global_id(0);
    if(index >= numParticles)
        return;

    float2 p = (float2) (d_Pos[index*3], d_Pos[index*3+1]);
    //Get address in grid
    int2  gridPos = getGridPos(p, cellSize, worldOrigin);
    uint gridHash = getGridHash(gridPos, gridSize);

    //Store grid hash and particle index
    d_Hash[index] = gridHash;
    d_Index[index] = index;
}*/


////////////////////////////////////////////////////////////////////////////////
// Find cell bounds and reorder positions+velocities by sorted indices
////////////////////////////////////////////////////////////////////////////////
__kernel void setMem(
    __global uint *d_Data,
    uint val,
    uint N
){
    if(get_global_id(0) < N)
        d_Data[get_global_id(0)] = val;
}

__kernel void findCellBoundsAndReorder(
    __global uint   *d_CellStart,     //output: cell start index
    __global uint   *d_CellEnd,       //output: cell end index
    __global float  *d_ReorderedPedestrians,  //output: reordered by cell hash positions
    __global float  *d_ReorderedPos,  //output: reordered by cell hash positions
    __global const uint   *d_Hash,    //input: sorted grid hashes
    __global const uint   *d_Index,   //input: particle indices sorted by hash
    __global float  *d_Pedestrians,  //output: reordered by cell hash positions
    __global const float  *d_Pos,     //input: positions array sorted by hash
    __local uint *localHash,          //get_group_size(0) + 1 elements
    uint    numParticles
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
        d_ReorderedPedestrians[index * OFFSET + TIMECREDIT] = d_Pedestrians[sortedIndex * OFFSET + TIMECREDIT];
        d_ReorderedPedestrians[index * OFFSET + NEWX] = d_Pedestrians[sortedIndex * OFFSET + NEWX];
        d_ReorderedPedestrians[index * OFFSET + NEWY] = d_Pedestrians[sortedIndex * OFFSET + NEWY];
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