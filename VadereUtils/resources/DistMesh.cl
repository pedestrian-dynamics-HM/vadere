/**
 * The implementation of DistMesh using the half-edge data structure using single float precision.
 * Forces are computed for edge in parallel using the atomic_add operation.
 * Edge flips are done via a three stages (for each edge in parallel):
 * (1) Lock the first triangle A for edge e
 * (2) Lock the second triangle B for edge e if e holds the lock of A
 * (3) Flip edge e if e holds the lock for A and B
 */

//IDistanceFunction distanceFunc = p -> Math.abs(6 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 4;
// abs(6 - length(v))-4

#pragma OPENCL EXTENSION cl_khr_fp64 : enable
#pragma OPENCL EXTENSION cl_khr_global_int32_base_atomics : enable
#pragma OPENCL EXTENSION cl_khr_int64_base_atomics : enable
#pragma OPENCL EXTENSION cl_amd_printf : enable
#define LOCK(a) atomic_cmpxchg(a, 0, 1)
#define UNLOCK(a) atomic_xchg(a, 0)

inline void atomicAdd_g_f(volatile __global float *addr, float val) {
	// see OpelCL specification of union to understand the code!
	union {
	   unsigned int u64;
	   float f64;
	} next, expected, current;
   	
	current.f64 = *addr;
    do {
		expected.f64 = current.f64;
		next.f64 = expected.f64 + val;
   		current.u64 = atomic_cmpxchg( (volatile __global unsigned int *)addr, expected.u64, next.u64);
    } while(current.u64 != expected.u64);
}

// helper methods!
inline float2 getCircumcenter(float2 p1, float2 p2, float2 p3) {
    float d = 2 * (p1.x * (p2.y - p3.y) + p2.x * (p3.y - p1.y) + p3.x * (p1.y - p2.y));

    float x = ((p1.x * p1.x + p1.y * p1.y) * (p2.y - p3.y)
    				+ (p2.x * p2.x + p2.y * p2.y) * (p3.y - p1.y)
    				+ (p3.x * p3.x + p3.y * p3.y) * (p1.y - p2.y)) / d;
    		float y = ((p1.x * p1.x + p1.y * p1.y) * (p3.x - p2.x)
    				+ (p2.x * p2.x + p2.y * p2.y) * (p1.x - p3.x)
    				+ (p3.x * p3.x + p3.y * p3.y) * (p2.x - p1.x)) / d;
    return (float2) (x, y);
}

inline bool isCCW(float2 q, float2 p, float2 r) {
    return ((p.y - q.y) * (r.x - p.x) - (p.x - q.x) * (r.y - p.y)) < 0;
}

inline float dist(float2 v) {
    return fabs(7.0f - length(v))-3.0f;
}

inline float quality(float2 p, float2 q, float2 r) {
    float a = length(p-q);
    float b = length(p-r);
    float c = length(q-r);
    return ((b + c - a) * (c + a - b) * (a + b - c)) / (a * b * c);
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

inline bool isEdgeAlive(int4 edge) {
    return edge.s0 != -1;
}

inline bool isTriAlive(int3 triangle) {
    return triangle.s0 != -1;
}
// end helper

// remove low quality triangles on the boundary
kernel void removeTriangles(__global float2* vertices,
                            __global int4* edges,
                            __global int3* triangles) {
    int edgeId = get_global_id(0);

    // edge is alive?
    if(isEdgeAlive(edges[edgeId])){
        int ta = edges[edgeId].s2;
        int tb = edges[edgeId].s3;

        // edge is a boundary edge
        if(ta == -1 || tb == -1) {
            ta = max(ta, tb);
            int3 tri = triangles[ta];
            float2 v0 = vertices[tri.s0];
            float2 v1 = vertices[tri.s1];
            float2 v2 = vertices[tri.s2];

            if(quality(v0, v1, v2) < 0.2f) {
                // destroy edge i.e. disconnect edge
                edges[edgeId].s0 = -1;
                edges[edgeId].s1 = -1;

                // destroy triangle
                tri.s0 = -1;
                tri.s1 = -1;
                tri.s2 = -1;
            }
        }

    }
}

// for each edge in parallel, label illegal edges
kernel void label(__global float2* vertices,
                  __global int4* edges,
                  __global int3* triangles,
                  __global int* labeledEdges,
                  __global int* illegalEdge) {
    int edgeId = get_global_id(0);
    if(isEdgeAlive(edges[edgeId])){
        float eps = 0.0001f;

        int v0 = edges[edgeId].s0;
        int v1 = edges[edgeId].s1;
        int ta = edges[edgeId].s2;
        int tb = edges[edgeId].s3;

        // edge is a non-boundary edge
        if(ta != -1 && tb != -1 && ta < tb) {

            int v2 = getDiffVertex(v0, v1, triangles[ta]);
            int p = getDiffVertex(v0, v1, triangles[tb]);
            //float2 c = getCircumcenter(vertices[v0], vertices[v1], vertices[v2]);
            // require a flip?

            float2 pp = vertices[p];
            //double2 c = getCircumcenter(vertices[v0], vertices[v1], vertices[v2]);
             // require a flip?
            if(isInCircle(vertices[v0], vertices[v1], vertices[v2], pp.x, pp.y)) {
                labeledEdges[edgeId] = 1;
                *illegalEdge = 1;
            }
            else {
                labeledEdges[edgeId] = 0;
            }
        } else {
            labeledEdges[edgeId] = 0;
        }
    }
}

// for each edge in parallel
kernel void updateLabel(__global float2* vertices,
                        __global int4* edges,
						__global int3* triangles,
						__global int* labeledEdges,
						__global int* illegalEdge,
						__global int* lockedTriangles) {
    int edgeId = get_global_id(0);
    if(isEdgeAlive(edges[edgeId])){
        float eps = 0.000001f;

        int v0 = edges[edgeId].s0;
        int v1 = edges[edgeId].s1;
        int ta = edges[edgeId].s2;
        int tb = edges[edgeId].s3;

        lockedTriangles[ta] = -1;
        lockedTriangles[tb] = -1;
//        labeledEdges[edgeId] = 0;
        // edge is a non-boundary edge
        if(ta != -1 && tb != -1 && ta < tb && labeledEdges[edgeId] == 1) {
            int v2 = getDiffVertex(v0, v1, triangles[ta]);
            int p = getDiffVertex(v0, v1, triangles[tb]);

            float2 c = getCircumcenter(vertices[v0], vertices[v1], vertices[v2]);
             // require a flip?
            if(length(c-vertices[p]) < length(c-vertices[v0])) {
                labeledEdges[edgeId] = 1;
                *illegalEdge = 1;
                //("found illegal edge!!!! %i \n", edgeId);
            }
            else {
                labeledEdges[edgeId] = 0;
            }
        } else {
            labeledEdges[edgeId] = 0;
        }
    }
}

kernel void flipStage1(__global int4* edges,
					   __global int* labeledEdges,
					   __global int* lockedTriangles) {
    int e = get_global_id(0);

    // is the edge illegal
    if(labeledEdges[e] == 1) {
        int ta = edges[e].s2;
        int tb = edges[e].s3;

        //printf("stage 1: lock ta %i for edge %i, tb = %i  \n", ta, e, tb);

        // to avoid the twin from locking
        if(ta < tb) {
            lockedTriangles[ta] = e;
        }
    }
}

kernel void flipStage2(__global int4* edges,
					   __global int* labeledEdges,
					   __global int* lockedTriangles) {
    int e = get_global_id(0);

    if(labeledEdges[e] == 1) {
        int ta = edges[e].s2;
        int tb = edges[e].s3;

        // to avoid the twin from locking
        if(ta < tb && lockedTriangles[ta] == e) {
            lockedTriangles[tb] = e;
            //printf("stage 2: lock tri %i for edge %i \n", tb, e);
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

    if(labeledEdges[e] == 1) {
        int ta = edges[e].s2;
        int tb = edges[e].s3;

        // swap if both triangles are locked by ta, i.e. by this thread
        if(lockedTriangles[ta] == e && lockedTriangles[tb] == e) {
            //printf("stage 3: lock tris: %i, %i for edge %i \n", ta, tb, e);
            int v0 = edges[e].s0;
            int v1 = edges[e].s1;

            int u0 = getDiffVertex(v0, v1, triangles[ta]);
            int u1 = getDiffVertex(v0, v1, triangles[tb]);

            // Here we keep the order in ccw assuming that everything is in ccw beforehand

            // edge flip for the origin
            edges[e].s0 = u0;
            edges[e].s1 = u1;

            // edge flip for the twin
            edges[twins[e]].s0 = u1;
            edges[twins[e]].s1 = u0;

            triangles[ta].s0 = u0;
            triangles[ta].s1 = u1;
            triangles[ta].s2 = v1;

            triangles[tb].s0 = u1;
            triangles[tb].s1 = u0;
            triangles[tb].s2 = v0;

            // save the relation of the changes
            R[ta] = tb;
            R[tb] = ta;

            float2 c = getCircumcenter(vertices[u0], vertices[u1], vertices[v0]);
            // require a flip?
            //if(length(c-vertices[v1]) < length(c-vertices[u0])) {
            //    printf("error [%f] - %d, %d, edge: %d \n", (length(c-vertices[v1]) - length(c-vertices[u0])), ta, tb, e);
            //}
            labeledEdges[e] = 0;
            labeledEdges[twins[e]] = 0;
        }
    }
}

 // for each edge in parallel
 kernel void repair(__global int4* edges,
                    __global int3* triangles,
                    __global int* R)
{
    int edgeId = get_global_id(0);
    if(isEdgeAlive(edges[edgeId])){
        int v0 = edges[edgeId].s0;
        int v1 = edges[edgeId].s1;
        int ta = edges[edgeId].s2;
        int tb = edges[edgeId].s3;

        // invalid
        if(ta != -1 && !isPartOf(v0, v1, triangles[ta])) {
            edges[edgeId].s2 = R[ta];
        }

        if(tb != -1 && !isPartOf(v0, v1, triangles[tb])) {
            edges[edgeId].s3 = R[tb];
        }
    }

}

// for each triangle test its legality
 kernel void checkTriangles(
                     __global float2* vertices,
                     __global int3* triangles,
                     __global int* illegalTri)
{
    int triangleId = get_global_id(0);
    if(isTriAlive(triangles[triangleId])) {
        float2 v0 = vertices[triangles[triangleId].s0];
        float2 v1 = vertices[triangles[triangleId].s1];
        float2 v2 = vertices[triangles[triangleId].s2];

        // triangle is illegal => re-triangulation is necessary!
        if(*illegalTri != 1 && !isCCW(v0, v1, v2)) {
            *illegalTri = 1;
        }
    }
}


// for each edge in parallel
kernel void computeForces(
	__const int size,
    __global float2* vertices,
    __global int4* edges,
    __global float2* lengths,
    __global float* scalingFactor,
    __global float* forces,
    __global int* isBoundary)
{

	int gid = get_global_id(0);
	int gsize = get_global_size(0);
	int lid = get_local_id(0);
	int lsize = size / gsize + 1;

	for(int i = gid*lsize; i < gid*lsize+lsize && i < size; i++) {
	    if(isEdgeAlive(edges[i])) {
	        int p1Index = edges[i].s0;
            float2 p1 = vertices[edges[i].s0];
            float2 p2 = vertices[edges[i].s1];
            float2 v = normalize(p1-p2);

            // F= ...
            float len = lengths[i].s0;
            float desiredLen = lengths[i].s1 * (*scalingFactor) * 1.2f;
            float lenDiff = (desiredLen - len);
            lenDiff = lenDiff > 0.0f ? lenDiff : 0.0f;
            float2 partialForce = v * lenDiff;

            // TODO this might be slow!
            atomicAdd_g_f((global float*)(&forces[p1Index*2]), partialForce.x);
            atomicAdd_g_f((global float*)(&forces[p1Index*2+1]), partialForce.y);

            //printf("partialForce %f, %f \n force %f, %f \n", partialForce.x, partialForce.y, forces[p1Index*2], forces[p1Index*2+1]);
	    }
	}
}

//inline float fabs(float d) {return if(d < 0){return -d;}else{return d;}}

inline float float2float (float a){
    unsigned int ia = __float_as_int (a);
    return __hiloint2float (__byte_perm (ia >> 3, ia, 0x7210), ia << 29);
}

kernel void moveVertices(__global float2* vertices, __global float2* forces, __global int* isBoundary, const float delta) {
    int vertexId = get_global_id(0);

	float deps = 0.00001f;
	float2 force = forces[vertexId];
	float2 v = vertices[vertexId];

	v = v + (force * 0.3f);

	// project back if necessary
	float d = dist(v);
	if(d > 0.0 || isBoundary[vertexId] == 1) {
		float2 dX = (float2)(deps, 0.0f);
		float2 dY = (float2)(0.0f, deps);
		float2 vx = (float2)(v + dX);
		float2 vy = (float2)(v + dY);
		float dGradPX = (dist(vx) - d) / deps;
		float dGradPY = (dist(vy) - d) / deps;
		float2 projection = (float2)(dGradPX * d, dGradPY * d);
		v = v - projection;
	}

	// set force to 0.
	//printf("vertex( %d ) with force( %f, %f )\n", vertexId, force.x, force.y);
	forces[vertexId] = (float2)(0.0f, 0.0f);
	vertices[vertexId] = v;
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
        float2 accumulator = (float2)(0.0f, 0.0f);
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