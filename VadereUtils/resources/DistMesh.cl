kernel void computeForces(
    __global float2* vertices,
    __global int4* edges,
    __global float2* lengths,
    __global float* scalingFactor,
    __global float2* forces)
{
    int i = get_global_id(0);
    float2 p1 = vertices[edges[i].s0];
    float2 p2 = vertices[edges[i].s1];
    float2 v = normalize(p1-p2);


    // F= ...
    float len = lengths[i].s0;
    float desiredLen = lengths[i].s1 * (*scalingFactor) * 1.2f;
    float lenDiff = (desiredLen - len);
    lenDiff = lenDiff > 0 ? lenDiff : 0;


    float2 partialForce = v * lenDiff; // TODO;

    forces[edges[i].s0] = forces[edges[i].s0] + partialForce; // TODO sync
    //forces[edges[i].s0] = (float2) (1.0f, 1.0f);
    //forces[edges[i].s1] = forces[edges[i].s1] - partialForce; // TODO sync
}

kernel void moveVertices(__global float2* vertices, __global float2* forces, const float delta) {
    int i = get_global_id(0);
    //float2 force = (float2)(1.0, 1.0);
    float2 force = forces[i];
    vertices[i] = vertices[i] + (force * delta);
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
    qLengths[i] = (float2) (v.s0*v.s0 + v.s1*v.s1, desiredLen*desiredLen);
}


// kernel for multiple work-groups
kernel void computePartialSF(__const int size, __global float2* qlengths, __local float2* partialSums, __global float2* output) {
    int gid = get_global_id(0);

    if(gid < size){
        int global_index = gid;
        float2 accumulator = (float2)(0, 0);
        // Loop sequentially over chunks of input vector
        while (global_index < size) {
            float2 element = qlengths[global_index];
            accumulator += element;
            global_index += get_global_size(0);
        }

        int lid = get_local_id(0);
        int group_size = get_local_size(0);
        float2 len = qlengths[gid];
        partialSums[lid] = len;

        barrier(CLK_LOCAL_MEM_FENCE);
        // group_size has to be a power of 2!
        for(int i = group_size/2; i > 0; i>>=1){
            if(lid < i && lid + i < size) {
                partialSums[lid] += partialSums[lid + i];
            }
            barrier(CLK_LOCAL_MEM_FENCE);
        }

        if(lid == 0){
            output[get_group_id(0)] = partialSums[0];
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
          *scaleFactor = sqrt(partialSums[0].s0 / partialSums[0].s1);
        }
    }
}