
//IDistanceFunction distanceFunc = p -> Math.abs(6 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 4;
// abs(6 - length(v))-4

#pragma OPENCL EXTENSION cl_khr_fp64 : enable
#pragma OPENCL EXTENSION cl_khr_global_int32_base_atomics : enable
#define LOCK(a) atomic_cmpxchg(a, 0, 1)
#define UNLOCK(a) atomic_xchg(a, 0)

// helper methods!
inline void atomicAdd_g_f(volatile __global double2 *addr, double2 val) {
    union{
        unsigned int u32;
        double2 f32;
    } next, expected, current;
   	current.f32 = *addr;
    do{
   	    expected.f32 = current.f32;
        next.f32 = expected.f32 + val;
   		current.u32 = atomic_cmpxchg( (volatile __global unsigned int *) addr, expected.u32, next.u32);
    } while(current.u32 != expected.u32);
}

inline double2 getCircumcenter(double2 p1, double2 p2, double2 p3) {
    double d = 2 * (p1.s0 * (p2.s1 - p3.s1) + p2.s0 * (p3.s1 - p1.s1) + p3.s0 * (p1.s1 - p2.s1));

    double x = ((p1.s0 * p1.s0 + p1.s1 * p1.s1) * (p2.s1 - p3.s1)
    				+ (p2.s0 * p2.s0 + p2.s1 * p2.s1) * (p3.s1 - p1.s1)
    				+ (p3.s0 * p3.s0 + p3.s1 * p3.s1) * (p1.s1 - p2.s1)) / d;
    		double y = ((p1.s0 * p1.s0 + p1.s1 * p1.s1) * (p3.s0 - p2.s0)
    				+ (p2.s0 * p2.s0 + p2.s1 * p2.s1) * (p1.s0 - p3.s0)
    				+ (p3.s0 * p3.s0 + p3.s1 * p3.s1) * (p2.s0 - p1.s0)) / d;
    return (double2) (x, y);
}

inline int getDiffVertex(int v0, int v1, int3 t,  __global int4* edges) {
    if(edges[t.s0].s0 != v0 && !edges[t.s0].s0 == v1) {
        return edges[t.s0].s0;
    }

    if(edges[t.s0].s1 != v0 && !edges[t.s0].s1 == v1) {
        return edges[t.s0].s1;
    }

    if(edges[t.s1].s0 != v0 && !edges[t.s1].s0 == v1) {
        return edges[t.s1].s0;
    }

    if(edges[t.s1].s1 != v0 && !edges[t.s1].s1 == v1) {
        return edges[t.s1].s1;
    }

    if(edges[t.s2].s0 != v0 && !edges[t.s2].s0 == v1) {
        return edges[t.s2].s0;
    }

    if(edges[t.s2].s1 != v0 && !edges[t.s2].s1 == v1) {
        return edges[t.s2].s1;
    }

    return -1;
}

inline boolean isSmallesFlipper(int edgeId, int3 ta, int3 tb, __global int* labeledEdges){
    if(ta.s0 < edgeId && labelEdges[ta.s0]) {
        return false;
    }

    if(ta.s1 < edgeId && labelEdges[ta.s1]) {
        return false;
    }

    if(ta.s2 < edgeId && labelEdges[ta.s2]) {
        return false;
    }

    if(tb.s0 < edgeId && labelEdges[tb.s0]) {
        return false;
    }

    if(tb.s1 < edgeId && labelEdges[tb.s1]) {
        return false;
    }

    if(tb.s2 < edgeId && labelEdges[tb.s2]) {
        return false;
    }

    return true;
}

inline boolean isPartOf(int edgeId, int3 t) {
    return edgeId == t.s0 || edgeId == t.s1 || edgeId == t.s2;
}
// end helper

// for each edge in parallel
kernel void label(__global double2* vertices,
                  __global int4* edges,
                  __global int3* triangles,
                  __global int* labeledEdges) {
    int edgeId = get_global_id(0);
    // check if edge is illegal

    double eps = 0.00001;

    int v0 = edges[edgeId].s0;
    int v1 = edges[edgeId].s1;
    int ta = edges[edgeId].s2;
    int tb = edges[edgeId].s3;

    // edge is a non-boundary edge
    if(tb != -1) {

        int v2 = getDiffVertex(v0, v1, triangles[ta], edges);
        int p = getDiffVertex(v0, v1, triangles[tb], edges);

        // test the delaunay criteria
        double2 c = getCircumcenter(vertices[v0], vertices[v1], vertices[v2]);
        double rad = length(c-vertices[v0]);

        // require a flip?
        if(rad + eps < length(c-vertices[p])) {
            labeledEdges[edgeId] = 1;
        }
        else {
            labeledEdges[edgeId] = 0;
        }
    } else {
        labeledEdges[edgeId] = 0;
    }
}


// for each edge in parallel
kernel void flip(__global double2* vertices,
                 __global int4* edges,
                 __global int3* triangles,
                 __global int* labeledEdges,
                 __global int* R)
 {
    int edgeId = get_global_id(0);
    // check if edge is illegal

    double eps = 0.00001;

    int v0 = edges[edgeId].s0;
    int v1 = edges[edgeId].s1;
    int ta = edges[edgeId].s2;
    int tb = edges[edgeId].s3;

    // edge is a non-boundary edge
    if(tb != -1 && isSmallesFlipper(edgeId, triangles[ta], triangles[tb], labeledEdges)) {

       int u0 = getDiffVertex(v0, v1, ta);
       int u1 = getDiffVertex(v0, v1, tb);

       edges[edgeId].s0 = u0;
       edges[edgeId].s1 = u1;

       // save the relation of the changes
       R[ta] = tb;
       R[tb] = ta;
    }
 }

 // for each edge in parallel
 kernel void repair(__global int4* edges,
                    __global int3* triangles,
                    __global int* R)
  {
    int edgeId = get_global_id(0);
    int v0 = edges[edgeId].s0;
    int v1 = edges[edgeId].s1;
    int ta = edges[edgeId].s2;
    int tb = edges[edgeId].s3;

    // invalid
    if(!isPartOf(edgeId, triangles[ta])) {
        edges[edgeId].s2 = R[ta];
    }

    if(tb != -1 && !isPartOf(edgeId, triangles[tb])) {
        edges[edgeId].s3 = R[tb];
    }
}

// for each edge in parallel
kernel void computeForces(
    __global double2* vertices,
    __global int4* edges,
    __global double2* lengths,
    __global double* scalingFactor,
    __global double2* forces,
    __global int* mutexes)
{
    int i = get_global_id(0);
    int p1Index = edges[i].s0;
    double2 p1 = vertices[edges[i].s0];
    double2 p2 = vertices[edges[i].s1];
    double2 v = normalize(p1-p2);

    // F= ...
    double len = lengths[i].s0;
    double desiredLen = lengths[i].s1 * (*scalingFactor) * 1.2;
    double lenDiff = (desiredLen - len);
    lenDiff = lenDiff > 0 ? lenDiff : 0;
    double2 partialForce = v * lenDiff;

    volatile __global int* addr = &mutexes[p1Index];

    // TODO does this sync work properly? This syncs too much?
    // for each vertex in parallel
    int waiting = 1;
    while (waiting) {
        while (LOCK(addr)) {}
        forces[p1Index] = forces[p1Index] + partialForce;
        UNLOCK(addr);
        waiting = 0;
    }
}

inline double dabs(double d) {return d < 0 ? -d : d;}

kernel void moveVertices(__global double2* vertices, __global double2* forces, const double delta) {
    int i = get_global_id(0);

    //double2 force = (double2)(1.0, 1.0);
    double2 force = forces[i];
    double2 v = vertices[i];


    // project back if necessary
    double distance = dabs(6.0 - length(v))-4.0;
    if(distance <= 0) {
        v = v + (force * 0.08);
    }
    else {
        double deps = 0.0001;
        double2 dX = (deps, 0.0);
        double2 dY = (0.0, deps);
        double dGradPX = ((dabs(6.0 - length(v + dX))-4.0)-distance) / deps;
        double dGradPY = ((dabs(6.0 - length(v + dY))-4.0)-distance) / deps;
        double2 projection = (dGradPX * distance, dGradPY * distance);
        v = v - projection;
    }

    // set force to 0.
    forces[i] = (0.0, 0.0);
    vertices[i] = v;
}

// computation of the scaling factor:
kernel void computeLengths(
    __global double2* vertices,
    __global int4* edges,
    __global double2* lengths,
    __global double2* qLengths)
{
    int i = get_global_id(0);
    double2 p1 = vertices[edges[i].s0];
    double2 p2 = vertices[edges[i].s1];
    double2 v = p1-p2;

    //TODO: desiredLenfunction required
    double desiredLen = 1.0f;
    double2 len = (double2) (length(v), desiredLen);
    lengths[i] = len;
    qLengths[i] = (double2) (length(v)*length(v), desiredLen*desiredLen);
}


// kernel for multiple work-groups
kernel void computePartialSF(__const int size, __global double2* qlengths, __local double2* partialSums, __global double2* output) {
    int gid = get_global_id(0);
    int lid = get_local_id(0);

    if(gid < size){
        int global_index = gid;
        double2 accumulator = (double2)(0, 0);
        // Loop sequentially over chunks of input vector
        while (global_index < size) {
            double2 element = qlengths[global_index];
            accumulator += element;
            global_index += get_global_size(0);
        }

        int group_size = get_local_size(0);
        double2 len = accumulator;

        //double2 len = (double2)(1.0, 1.0);
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
            output[get_group_id(0)] = (double2)(0.0, 0.0);
        }
    }
}

// kernel for 1 work-group
kernel void computeCompleteSF(__const int size, __global double2* qlengths, __local double2* partialSums, __global double* scaleFactor) {
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