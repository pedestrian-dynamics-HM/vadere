
//IDistanceFunction distanceFunc = p -> Math.abs(6 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 4;
// abs(6 - length(v))-4

#pragma OPENCL EXTENSION cl_khr_fp64 : enable
#pragma OPENCL EXTENSION cl_khr_global_int32_base_atomics : enable
#pragma OPENCL EXTENSION cl_amd_printf : enable
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
inline float2 getCircumcenter(float2 p1, float2 p2, float2 p3) {
    float d = 2 * (p1.s0 * (p2.s1 - p3.s1) + p2.s0 * (p3.s1 - p1.s1) + p3.s0 * (p1.s1 - p2.s1));

    float x = ((p1.s0 * p1.s0 + p1.s1 * p1.s1) * (p2.s1 - p3.s1)
    				+ (p2.s0 * p2.s0 + p2.s1 * p2.s1) * (p3.s1 - p1.s1)
    				+ (p3.s0 * p3.s0 + p3.s1 * p3.s1) * (p1.s1 - p2.s1)) / d;
    		float y = ((p1.s0 * p1.s0 + p1.s1 * p1.s1) * (p3.s0 - p2.s0)
    				+ (p2.s0 * p2.s0 + p2.s1 * p2.s1) * (p1.s0 - p3.s0)
    				+ (p3.s0 * p3.s0 + p3.s1 * p3.s1) * (p2.s0 - p1.s0)) / d;
    return (float2) (x, y);
}

inline bool isCCW(float2 q, float2 p, float2 r) {
    return ((p.y - q.y) * (r.x - p.x) - (p.x - q.x) * (r.y - p.y)) < 0;
}

inline bool isInCircle(float2 a, float2 b, float2 c, float x , float y) {
    float eps = 0.00001f;
    float adx = a.x - x;
    float ady = a.y - y;
    float bdx = b.x - x;
    float bdy = b.y - y;
    float cdx = c.x - x;
    float cdy = c.y - y;

    float abdet = adx * bdy - bdx * ady;
    float bcdet = bdx * cdy - cdx * bdy;
    float cadet = cdx * ady - adx * cdy;
    float alift = adx * adx + ady * ady;
    float blift = bdx * bdx + bdy * bdy;
    float clift = cdx * cdx + cdy * cdy;

    float disc = alift * bcdet + blift * cadet + clift * abdet;
    bool ccw = isCCW(a, b, c);
    return (disc-eps > 0 && ccw) || (disc+eps < 0 && !ccw);
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
kernel void label(__global float2* vertices,
                  __global int4* edges,
                  __global int3* triangles,
                  __global int* labeledEdges,
                  __global int* isLegal) {
    int edgeId = get_global_id(0);
    // check if edge is illegal

    float eps = 0.0001;

    int v0 = edges[edgeId].s0;
    int v1 = edges[edgeId].s1;
    int ta = edges[edgeId].s2;
    int tb = edges[edgeId].s3;

    // edge is a non-boundary edge
    if(tb != -1) {

        int v2 = getDiffVertex(v0, v1, triangles[ta]);
        int p = getDiffVertex(v0, v1, triangles[tb]);
        float2 c = getCircumcenter(vertices[v0], vertices[v1], vertices[v2]);
        // require a flip?
        if(length(c-vertices[p]) < length(c-vertices[v0])) {
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
kernel void updateLabel(__global float2* vertices,
                  __global int4* edges,
                  __global int3* triangles,
                  __global int* labeledEdges,
                  __global int* isLegal,
                  __global int* lockedTriangles) {
    int edgeId = get_global_id(0);
    // check if edge is illegal

    float eps = 0.001f;

    int v0 = edges[edgeId].s0;
    int v1 = edges[edgeId].s1;
    int ta = edges[edgeId].s2;
    int tb = edges[edgeId].s3;

    //lockedTriangles[ta] = -1;
    //lockedTriangles[tb] = -1;

    // edge is a non-boundary edge
    if(tb != -1 && labeledEdges[edgeId] == 1) {
        int v2 = getDiffVertex(v0, v1, triangles[ta]);
        int p = getDiffVertex(v0, v1, triangles[tb]);

         float2 c = getCircumcenter(vertices[v0], vertices[v1], vertices[v2]);
         // require a flip?
         if(length(c-vertices[p]) < length(c-vertices[v0])) {
            labeledEdges[edgeId] = 1;
            atomic_xchg(isLegal, 0);
            //printf("%f\n", (length(c-vertices[p])- length(c-vertices[v0])));
            //printf("[%d, %d, %d, %d], [%d, %d, %d],[%d, %d, %d]\n", v0, v1, v2, p, triangles[ta].x, triangles[ta].y, triangles[ta].z , triangles[tb].x, triangles[tb].y, triangles[tb].z);
        }
        else {
            labeledEdges[edgeId] = 0;
        }
    } else {
        labeledEdges[edgeId] = 0;
    }
}

kernel void flipStage1(__global int4* edges,
                 __global int* labeledEdges,
                 __global int* lockedTriangles) {
    int e = get_global_id(0);
    int localSize = get_local_size(0);

    // is the edge illegal
    if(labeledEdges[e] == 1) {
        int ta = edges[e].s2;
        int tb = edges[e].s3;

        int tmp = min(ta, tb);
        tb = max(ta, tb);
        ta = tmp;

        // atomic lock
        lockedTriangles[ta] = e;
    }
}

kernel void flipStage2(__global int4* edges,
                 __global int* labeledEdges,
                 __global int* lockedTriangles) {
    int e = get_global_id(0);
    int localSize = get_local_size(0);

    if(labeledEdges[e]) {
        int ta = edges[e].s2;
        int tb = edges[e].s3;

        int tmp = min(ta, tb);
        tb = max(ta, tb);
        ta = tmp;

        if(lockedTriangles[ta] == e) {
            // atomic lock
            lockedTriangles[tb] = e;
        }
    }
}

kernel void flipStage3(
                 __global int4* edges,
                 __global int3* triangles,
                 __global int* labeledEdges,
                 __global int* lockedTriangles,
                 __global int* R,
                 __global int* twins,
                 __global float2* vertices) {
    int e = get_global_id(0);
    int localSize = get_local_size(0);

    if(labeledEdges[e] == 1) {
        int ta = edges[e].s2;
        int tb = edges[e].s3;

        int tmp = min(ta, tb);
        tb = max(ta, tb);
        ta = tmp;

        // swap if both triangles are locked by ta, i.e. by this thread
        if(lockedTriangles[ta] == e && lockedTriangles[tb] == e) {
            printf("ta: %d, tb : %d \n", ta, tb);
            int v0 = edges[e].s0;
            int v1 = edges[e].s1;

            int u0 = getDiffVertex(v0, v1, triangles[ta]);
            int u1 = getDiffVertex(v0, v1, triangles[tb]);

             float2 c1 = getCircumcenter(vertices[v0], vertices[v1], vertices[u0]);
            if(length(c1-vertices[u1]) >= length(c1-vertices[v0])) {
                printf("error2 - %d, %d \n", ta, tb);
            }

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

            float2 c = getCircumcenter(vertices[u0], vertices[u1], vertices[v0]);
            // require a flip?
            if(length(c-vertices[v1]) < length(c-vertices[u0])) {
                printf("error [%f] - %d, %d \n", (length(c-vertices[v1]) - length(c-vertices[u0])), ta, tb);
            }
            //labeledEdges[e] = 0;
            //labeledEdges[twins[e]] = 0;
        }
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
    if(labeledEdges[e] == 1) {
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
                labeledEdges[e] = 0;
                labeledEdges[twins[e]] = 0;
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
    float desiredLen = lengths[i].s1 * (*scalingFactor) * 1.2f;
    float lenDiff = (desiredLen - len);
    lenDiff = lenDiff > 0 ? lenDiff : 0;
    float2 partialForce = v * lenDiff;

    //volatile __global int* addr = &mutexes[p1Index];

    //int waiting = 1;
    //while (waiting) {
    //    while (LOCK(addr)) {}
    //printf("before %f \n", forces[p1Index].x);
    //float2 tmp = forces[p1Index];
    // TODO this might be slow!
    global float* forceP = (global float*)(&forces[p1Index]);
    atomicAdd_g_f(forceP, partialForce.x);
    atomicAdd_g_f((forceP+1), partialForce.y);
    //forces[p1Index] = forces[p1Index] + partialForce;
    //    UNLOCK(addr);
    //    waiting = 0;
    //}
    //printf("adder-x [%d] %f \n", p1Index, partialForce.x);
    //printf("adder-y [%d] %f \n", p1Index, partialForce.y);
    //printf("after-x %f \n", forces[p1Index].x);
    //printf("test-x: %f == %f \n", (tmp + partialForce).x, forces[p1Index].x);
    //printf("test-y: %f == %f \n", (tmp + partialForce).y, forces[p1Index].y);
}

//inline float fabs(float d) {return d < 0 ? -d : d;}

kernel void moveVertices(__global float2* vertices, __global float2* forces, const float delta) {
    int i = get_global_id(0);
    //float2 force = (float2)(1.0f, 1.0f);
    float2 force = forces[i];
    float2 v = vertices[i];

    /*if(i == 100) {
        printf("force-x %f \n" , forces[i].x);
        printf("force-y %f \n" , forces[i].y);
    }*/

    // project back if necessary
    float distance = fabs(6.0f - length(v))-4.0f;
    if(distance <= 0) {
        v = v + (force * 0.2f);
    }
    else {
        float deps = 0.0001f;
        float2 dX = (deps, 0.0f);
        float2 dY = (0.0f, deps);
        float dGradPX = ((fabs(6.0f - length(v + dX))-4.0f)-distance) / deps;
        float dGradPY = ((fabs(6.0f - length(v + dY))-4.0f)-distance) / deps;
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

        //float2 len = (float2)(1.0, 1.0);
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
            output[get_group_id(0)] = (float2)(0.0, 0.0);
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