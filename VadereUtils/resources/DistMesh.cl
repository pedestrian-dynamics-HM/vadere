
//IDistanceFunction distanceFunc = p -> Math.abs(6 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 4;
// abs(6 - length(v))-4

#pragma OPENCL EXTENSION cl_khr_global_int32_base_atomics : enable
#define LOCK(a) atomic_cmpxchg(a, 0, 1)
#define UNLOCK(a) atomic_xchg(a, 0)

inline void atomicAdd_g_f(volatile __global float2 *addr, float2 val) {
    union{
        unsigned int u32;
        float2 f32;
    } next, expected, current;
   	current.f32 = *addr;
    do{
   	    expected.f32 = current.f32;
        next.f32 = expected.f32 + val;
   		current.u32 = atomic_cmpxchg( (volatile __global unsigned int *) addr, expected.u32, next.u32);
    } while(current.u32 != expected.u32);
}

kernel void computeForces(
    __global float2* vertices,
    __global int4* edges,
    __global float2* lengths,
    __global float* scalingFactor,
    __global float2* forces,
    __global int* mutexes)
{
    int i = get_global_id(0);
    int p1Index = edges[i].s0;
    float2 p1 = vertices[edges[i].s0];
    float2 p2 = vertices[edges[i].s1];
    float2 v = normalize(p1-p2);


    // F= ...
    float len = lengths[i].s0;
    float desiredLen = lengths[i].s1 * (*scalingFactor) * 1.1f;
    float lenDiff = (desiredLen - len);
    lenDiff = lenDiff > 0 ? lenDiff : 0;


    float2 partialForce = v * lenDiff; // TODO;

    volatile __global int* addr = &mutexes[p1Index];

    //int waiting = 1;
    //while (waiting) {
    //    while (LOCK(addr)) {}
        forces[p1Index] = forces[p1Index] + partialForce;
    //    UNLOCK(addr);
    //    waiting = 0;
    //}
}

kernel void moveVertices(__global float2* vertices, __global float2* forces, const float delta) {
    int i = get_global_id(0);

    //float2 force = (float2)(1.0, 1.0);
    float2 force = forces[i];
    float2 v = vertices[i];
    v = v + (force * 0.01f);

    // project back if necessary
    float distance = fabs(6.0f - length(v))-4.0f;
    if(distance <= 0) {

    }
    else {
        float deps = 1.4901e-8f * 1.0f;
        float2 dX = (deps, 0.0f);
        float2 dY = (0.0f, deps);
        float dGradPX = ((fabs(6 - length(v + dX))-4)-distance)/deps;
        float dGradPY = ((fabs(6 - length(v + dY))-4)-distance)/deps;
        float2 projection = (dGradPX * distance, dGradPY * distance);
        v = v - projection;
    }

    // set force to 0.
    forces[i] = (0.0f, 0.0f);
    vertices[i] = v;
}

// computation of the scaling factor:
kernel void computeLengths(
    __global float2* vertices,
    __global int4* edges,
    __global float2* lengths,
    __global float2* qLengths)
{
    int i = get_global_id(0);
    float2 p1 = vertices[edges[i].s0];
    float2 p2 = vertices[edges[i].s1];
    float2 v = p1-p2;

    //TODO: desiredLenfunction required
    float desiredLen = 1.0f;
    float2 len = (float2) (length(v), desiredLen);
    lengths[i] = len;
    qLengths[i] = (float2) (length(v)*length(v), desiredLen*desiredLen);
}

// kernel for multiple work-groups
kernel void computePartialSF(__const int size, __global float2* qlengths, __local float2* partialSums, __global float2* output) {
    int gid = get_global_id(0);
    int lid = get_local_id(0);

    if(gid < size){
        int global_index = gid;
        float2 accumulator = (float2)(0, 0);
        // Loop sequentially over chunks of input vector
        while (global_index < size) {
            float2 element = qlengths[global_index];
            accumulator += element;
            global_index += get_global_size(0);
        }

        int group_size = get_local_size(0);
        float2 len = accumulator;

        //float2 len = (float2)(1.0f, 1.0f);
        partialSums[lid] = len;

        barrier(CLK_LOCAL_MEM_FENCE);
        // group_size has to be a power of 2!
        for(int i = group_size/2; i > 0; i>>=1){
            if(lid < i && gid + i < size) {
                partialSums[lid] += partialSums[lid + i];
            }
            barrier(CLK_LOCAL_MEM_FENCE);
        }

        if(lid == 0){
            output[get_group_id(0)] = partialSums[0];
        }
    }
    else {
        if(lid == 0){
            output[get_group_id(0)] = (float2)(0.0f, 0.0f);
        }
    }
}

// kernel for 1 work-group
kernel void computeCompleteSF(__const int size, __global float2* qlengths, __local float2* partialSums, __global float* scaleFactor) {
    int gid = get_global_id(0);
    int lid = get_local_id(0);
    if(lid < size){
        int group_size = get_local_size(0);
        partialSums[lid] = qlengths[lid];

        barrier(CLK_LOCAL_MEM_FENCE);

        // group_size has to be a power of 2!
        for(int i = group_size/2; i > 0; i>>=1){
            if(lid < i && lid + i < size) {
                partialSums[lid] += partialSums[lid + i];
            }
            barrier(CLK_LOCAL_MEM_FENCE);
        }


        if(lid == 0) {
          //*scaleFactor =  qlengths[10].s0;
          //*scaleFactor =  partialSums[0].s0;
          *scaleFactor = sqrt(partialSums[0].s0 / partialSums[0].s1);
        }
    }
}