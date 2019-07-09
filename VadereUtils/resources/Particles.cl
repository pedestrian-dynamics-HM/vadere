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



////////////////////////////////////////////////////////////////////////////////
// Common definitions
////////////////////////////////////////////////////////////////////////////////
#define UMAD(a, b, c)  ( (a) * (b) + (c) )
//#define LOCAL_SIZE_LIMIT 512U

typedef struct{
    float x;
    float y;
    float z;
} Float3;

typedef struct{
    uint x;
    uint y;
    uint z;
}Uint3;

typedef struct{
    int x;
    int y;
    int z;
}Int3;


typedef struct{
    Float3 colliderPos;
    float  colliderRadius;

    Float3 gravity;
    float globalDamping;
    float particleRadius;

    Uint3 gridSize;
    uint numCells;
    Float3 worldOrigin;
    Float3 cellSize;

    uint numBodies;
    uint maxParticlesPerCell;

    float spring;
    float damping;
    float shear;
    float attraction;
    float boundaryDamping;
} simParams_t;

typedef struct {
    float2 position;
    float stepLength;
} pedestrian;

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
    return UMAD(  (*gridSize).x, gridPos.y, gridPos.x );
}


//Calculate grid hash value for each particle
__kernel void calcHash(
    __global uint        *d_Hash, //output
    __global uint       *d_Index, //output
    __global const float2 *d_Pos, //input: positions
    __constant float* cellSize,
    __constant float2* worldOrigin,
    __constant uint2* gridSize,
    uint numParticles
){
    const uint index = get_global_id(0);
    if(index >= numParticles)
        return;

    float2 p = d_Pos[index];

    //Get address in grid
    uint2  gridPos = getGridPos(p, cellSize, worldOrigin);
    uint gridHash = getGridHash(gridPos, gridSize);

    //Store grid hash and particle index
    d_Hash[index] = gridHash;
    d_Index[index] = index;
}



////////////////////////////////////////////////////////////////////////////////
// Find cell bounds and reorder positions+velocities by sorted indices
////////////////////////////////////////////////////////////////////////////////
__kernel void Memset(
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
    __global float2 *d_ReorderedPos,  //output: reordered by cell hash positions

    __global const uint   *d_Hash,    //input: sorted grid hashes
    __global const uint   *d_Index,   //input: particle indices sorted by hash
    __global const float2 *d_Pos,     //input: positions array sorted by hash
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
        float2 pos = d_Pos[sortedIndex];

        d_ReorderedPos[index] = pos;
    }
}


////////////////////////////////////////////////////////////////////////////////
// Monolithic bitonic sort kernel for short arrays fitting into local memory
////////////////////////////////////////////////////////////////////////////////
/*__kernel void bitonicSortLocal1(
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
}*/

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
    d_SrcKey += get_local_id(0);
    d_SrcVal += get_local_id(0);
    d_DstKey += get_local_id(0);
    d_DstVal += get_local_id(0);
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