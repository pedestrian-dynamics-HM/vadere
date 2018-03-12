

// TODO: old use the float version
//IDistanceFunction distanceFunc = p -> Math.abs(6 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 4;
// abs(6 - length(v))-4

#pragma OPENCL EXTENSION cl_khr_fp64 : enable
#pragma OPENCL EXTENSION cl_khr_global_int32_base_atomics : enable
#define LOCK(a) atomic_cmpxchg(a, 0, 1)
#define UNLOCK(a) atomic_xchg(a, 0)

inline void atomicAdd_g_f(volatile __global float *addr, float val)
   {
       union{
           unsigned int u32;
           float        f32;
       } next, expected, current;
   	current.f32    = *addr;
       do{
   	   expected.f32 = current.f32;
           next.f32     = expected.f32 + val;
   		current.u32  = atomic_cmpxchg( (volatile __global unsigned int *)addr,
                               expected.u32, next.u32);
       } while( current.u32 != expected.u32 );
   }

// helper methods!
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

inline bool isCCW(double2 q, double2 p, double2 r) {
    return -((q.x - p.x) * (r.y - p.y) - (r.x - p.x) * (q.y - p.y)) > 0;
}

inline bool isInCircle(double2 a, double2 b, double2 c, double x , double y) {
    double adx = a.x - x;
    double ady = a.y - y;
    double bdx = b.x - x;
    double bdy = b.y - y;
    double cdx = c.x - x;
    double cdy = c.y - y;

    double abdet = adx * bdy - bdx * ady;
    double bcdet = bdx * cdy - cdx * bdy;
    double cadet = cdx * ady - adx * cdy;
    double alift = adx * adx + ady * ady;
    double blift = bdx * bdx + bdy * bdy;
    double clift = cdx * cdx + cdy * cdy;

    double disc = alift * bcdet + blift * cadet + clift * abdet;
    bool ccw = isCCW(a, b, c);
    return (disc > 0 && ccw) || (disc < 0 && !ccw);
}


inline int getDiffVertex(int v0, int v1, int3 t) {
    if(t.s0 != v0 && t.s0 != v1) {
        return t.s0;
    }

    if(t.s1 != v0 && t.s1 != v1) {
        return t.s1;
    }

    if(t.s2 != v0 && t.s2 != v1) {
        return t.s2;
    }

    return -1;
}

inline bool isPartOf(int v0, int v1, int3 t) {
    return
        (v0 == t.s0 && t.s1 == v1) || (v0 == t.s1 && t.s0 == v1) ||
        (v0 == t.s1 && t.s2 == v1) || (v0 == t.s2 && t.s1 == v1) ||
        (v0 == t.s2 && t.s0 == v1) || (v0 == t.s0 && t.s2 == v1);
}

inline void waitGlobally(volatile __global int *g_mutex) {
    int num_groups = get_num_groups(0);
    int lid = get_local_id(0);

    if(lid == 0) {
        atom_inc(g_mutex);

        // spin-lock
        //while(*g_mutex != num_groups) {}
    }
}

inline void waitLocally() {
    barrier(CLK_LOCAL_MEM_FENCE);
}
// end helper

// for each edge in parallel
kernel void label(__global double2* vertices,
                  __global int4* edges,
                  __global int3* triangles,
                  __global int* labeledEdges,
                  __global int* isLegal) {
    int edgeId = get_global_id(0);
    // check if edge is illegal

    double eps = 0.0001;

    int v0 = edges[edgeId].s0;
    int v1 = edges[edgeId].s1;
    int ta = edges[edgeId].s2;
    int tb = edges[edgeId].s3;

    // edge is a non-boundary edge
    if(tb != -1) {

        int v2 = getDiffVertex(v0, v1, triangles[ta]);
        int p = getDiffVertex(v0, v1, triangles[tb]);

        // require a flip?
        if(isInCircle(vertices[v0], vertices[v1], vertices[v2], vertices[p].x, vertices[p].y)) {
            labeledEdges[edgeId] = 1;
            atomic_xchg(isLegal, 0);
        }
        else {
            labeledEdges[edgeId] = 0;
        }
    } else {
        labeledEdges[edgeId] = 0;
    }
}

// for each edge in parallel
kernel void updateLabel(__global double2* vertices,
                  __global int4* edges,
                  __global int3* triangles,
                  __global int* labeledEdges,
                  __global int* isLegal) {
    int edgeId = get_global_id(0);
    // check if edge is illegal

    double eps = 0.00001;

    int v0 = edges[edgeId].s0;
    int v1 = edges[edgeId].s1;
    int ta = edges[edgeId].s2;
    int tb = edges[edgeId].s3;
    printf("test");

    // edge is a non-boundary edge
    if(tb != -1 && labeledEdges[edgeId] == 1) {
        int v2 = getDiffVertex(v0, v1, triangles[ta]);
        int p = getDiffVertex(v0, v1, triangles[tb]);

        if(isInCircle(vertices[v0], vertices[v1], vertices[v2], vertices[p].x, vertices[p].y)) {
            labeledEdges[edgeId] = 1;
            printf("[%d, %d, %d %d]\n", v0, v1, v2, p);
            atomic_xchg(isLegal, 0);
        }
        else {
            labeledEdges[edgeId] = 0;
        }
    } else {
        labeledEdges[edgeId] = 0;
    }
}

// for each edge in parallel
kernel void flip(__global int4* edges,
                 __global int3* triangles,
                 __global int* labeledEdges,
                 __global int* lockedTriangles,
                 __global int* R,
                 __global int* g_mutex,
                 __global int* twins)
 {
    int e = get_global_id(0);
    int localSize = get_local_size(0);

    // is the edge illegal
    if(labeledEdges[e]) {
        int ta = edges[e].s2;
        int tb = edges[e].s3;

        int tmp = min(ta, tb);
        tb = max(ta, tb);
        ta = tmp;

        // atomic lock
        lockedTriangles[ta] = e;

        // barrier: wait for all work items globally
        barrier(CLK_LOCAL_MEM_FENCE);
        waitGlobally(g_mutex);

        if(lockedTriangles[ta] == e) {
            // atomic lock
            lockedTriangles[tb] = e;

            // barrier: wait for all work items globally
            barrier(CLK_LOCAL_MEM_FENCE);
            waitGlobally(g_mutex);

            // swap if both triangles are locked by ta, i.e. by this thread
            if(lockedTriangles[ta] == e && lockedTriangles[tb] == e) {
                int v0 = edges[e].s0;
                int v1 = edges[e].s1;

                int u0 = getDiffVertex(v0, v1, triangles[ta]);
                int u1 = getDiffVertex(v0, v1, triangles[tb]);

                // edge flip for the origin
                edges[e].s0 = u0;
                edges[e].s1 = u1;

                // edge flip for the twin
                edges[twins[e]].s0 = u1;
                edges[twins[e]].s1 = u0;

                triangles[ta].s0 = u0;
                triangles[ta].s1 = u1;
                triangles[ta].s2 = v0;

                triangles[tb].s0 = u1;
                triangles[tb].s1 = u0;
                triangles[tb].s2 = v1;

                // save the relation of the changes
                R[ta] = tb;
                R[tb] = ta;
                //labeledEdges[e] = 0;
                //labeledEdges[twins[e]] = 0;
            }
        }
        else {
            barrier(CLK_LOCAL_MEM_FENCE);
            waitGlobally(g_mutex);
        }
    }
    else {
        barrier(CLK_LOCAL_MEM_FENCE);
        waitGlobally(g_mutex);
        barrier(CLK_LOCAL_MEM_FENCE);
        waitGlobally(g_mutex);
    }

    // edge might be no longer illegal!
    //labeledEdges[e] = 0;
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
    if(!isPartOf(v0, v1, triangles[ta])) {
        edges[edgeId].s2 = R[ta];
    }

    if(tb != -1 && !isPartOf(v0, v1, triangles[tb])) {
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
    //while (waiting) {
    //    while (LOCK(addr)) {}

        forces[p1Index] = forces[p1Index] + partialForce;
    //    UNLOCK(addr);
    //    waiting = 0;
    //}
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
        v = v + (force * 0.05);
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
          *scaleFactor = sqrt(partialSums[0].s0 / partialSums[0].s1);
        }
    }
}