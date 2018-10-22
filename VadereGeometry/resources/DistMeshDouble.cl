/**
 * The implementation of DistMesh using the half-edge data structure using single double precision.
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

inline void atomicAdd_g_f(volatile __global double *addr, double val) {
	// see OpelCL specification of union to understand the code!
	union {
	   unsigned long u64;
	   double f64;
	} next, expected, current;
   	
	current.f64 = *addr;
    do {
		expected.f64 = current.f64;
		next.f64 = expected.f64 + val;
   		current.u64 = atomic_cmpxchg( (volatile __global unsigned long *)addr, expected.u64, next.u64);
    } while(current.u64 != expected.u64);
}

// helper methods!
inline double2 getCircumcenter(double2 p1, double2 p2, double2 p3) {
    double d = 2 * (p1.x * (p2.y - p3.y) + p2.x * (p3.y - p1.y) + p3.x * (p1.y - p2.y));

    double x = ((p1.x * p1.x + p1.y * p1.y) * (p2.y - p3.y)
    				+ (p2.x * p2.x + p2.y * p2.y) * (p3.y - p1.y)
    				+ (p3.x * p3.x + p3.y * p3.y) * (p1.y - p2.y)) / d;
    		double y = ((p1.x * p1.x + p1.y * p1.y) * (p3.x - p2.x)
    				+ (p2.x * p2.x + p2.y * p2.y) * (p1.x - p3.x)
    				+ (p3.x * p3.x + p3.y * p3.y) * (p2.x - p1.x)) / d;
    return (double2) (x, y);
}

inline bool isCCW(double2 q, double2 p, double2 r) {
    return ((p.y - q.y) * (r.x - p.x) - (p.x - q.x) * (r.y - p.y)) < 0;
}

inline double dist(double2 v) {
    return fabs(7.0 - length(v))-3.0;
}

inline double quality(double2 p, double2 q, double2 r) {
    double a = length(p-q);
    double b = length(p-r);
    double c = length(q-r);
    return ((b + c - a) * (c + a - b) * (a + b - c)) / (a * b * c);
}


inline bool isInCircle(double2 a, double2 b, double2 c, double x , double y) {
    double eps = 0.00001;
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
kernel void removeTriangles(__global double2* vertices,
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
            double2 v0 = vertices[tri.s0];
            double2 v1 = vertices[tri.s1];
            double2 v2 = vertices[tri.s2];

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
kernel void label(__global double2* vertices,
                  __global int4* edges,
                  __global int3* triangles,
                  __global int* labeledEdges,
                  __global int* illegalEdge) {
    int edgeId = get_global_id(0);
    if(isEdgeAlive(edges[edgeId])){
        double eps = 0.0001;

        int v0 = edges[edgeId].s0;
        int v1 = edges[edgeId].s1;
        int ta = edges[edgeId].s2;
        int tb = edges[edgeId].s3;

        // edge is a non-boundary edge
        if(ta != -1 && tb != -1 && ta < tb) {

            int v2 = getDiffVertex(v0, v1, triangles[ta]);
            int p = getDiffVertex(v0, v1, triangles[tb]);
            double2 pp = vertices[p];
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
kernel void updateLabel(__global double2* vertices,
                        __global int4* edges,
						__global int3* triangles,
						__global int* labeledEdges,
						__global int* illegalEdge,
						__global int* lockedTriangles) {
    int edgeId = get_global_id(0);
    if(isEdgeAlive(edges[edgeId])){
        double eps = 0.000001;

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

            double2 pp = vertices[p];
            //double2 c = getCircumcenter(vertices[v0], vertices[v1], vertices[v2]);
             // require a flip?
            if(isInCircle(vertices[v0], vertices[v1], vertices[v2], pp.x, pp.y)) {
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
                 __global double2* vertices) {
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

            double2 c = getCircumcenter(vertices[u0], vertices[u1], vertices[v0]);
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
                     __global double2* vertices,
                     __global int3* triangles,
                     __global int* illegalTri)
{
    int triangleId = get_global_id(0);
    if(isTriAlive(triangles[triangleId])) {
        double2 v0 = vertices[triangles[triangleId].s0];
        double2 v1 = vertices[triangles[triangleId].s1];
        double2 v2 = vertices[triangles[triangleId].s2];

        // triangle is illegal => re-triangulation is necessary!
        if(*illegalTri != 1 && !isCCW(v0, v1, v2)) {
            *illegalTri = 1;
        }
    }
}


// for each edge in parallel
kernel void computeForces(
	__const int size,
    __global double2* vertices,
    __global int4* edges,
    __global double2* lengths,
    __global double* scalingFactor,
    __global double* forces,
    __global int* isBoundary)
{

	int gid = get_global_id(0);
	int gsize = get_global_size(0);
	int lid = get_local_id(0);
	int lsize = size / gsize + 1;

	for(int i = gid*lsize; i < gid*lsize+lsize && i < size; i++) {
	    if(isEdgeAlive(edges[i])) {
	        int p1Index = edges[i].s0;
            double2 p1 = vertices[edges[i].s0];
            double2 p2 = vertices[edges[i].s1];
            double2 v = normalize(p1-p2);

            // F= ...
            double len = lengths[i].s0;
            double desiredLen = lengths[i].s1 * (*scalingFactor) * 1.2;
            double lenDiff = (desiredLen - len);
            lenDiff = lenDiff > 0.0 ? lenDiff : 0.0;
            double2 partialForce = v * lenDiff;

            // TODO this might be slow!
            atomicAdd_g_f((global double*)(&forces[p1Index*2]), partialForce.x);
            atomicAdd_g_f((global double*)(&forces[p1Index*2+1]), partialForce.y);

            //printf("partialForce %f, %f \n force %f, %f \n", partialForce.x, partialForce.y, forces[p1Index*2], forces[p1Index*2+1]);
	    }
	}
}

inline double double2double (double a){
    unsigned int ia = __double_as_int (a);
    return __hiloint2double (__byte_perm (ia >> 3, ia, 0x7210), ia << 29);
}

kernel void moveVertices(__global double2* vertices, __global double2* forces, __global int* isBoundary, const double delta) {
    int vertexId = get_global_id(0);

	double deps = 1.4901e-8;
	double2 force = forces[vertexId];
	double2 v = vertices[vertexId];

	v = v + (force * 0.3);

	// project back if necessary
	double d = dist(v);
	if(d > 0.0 || isBoundary[vertexId] == 1) {
		double2 dX = (double2)(deps, 0.0);
		double2 dY = (double2)(0.0, deps);
		double2 vx = (double2)(v + dX);
		double2 vy = (double2)(v + dY);
		double dGradPX = (dist(vx) - d) / deps;
		double dGradPY = (dist(vy) - d) / deps;
		double2 projection = (double2)(dGradPX * d, dGradPY * d);
		v = v - projection;
	}

	// set force to 0.
	//printf("vertex( %d ) with force( %f, %f )\n", vertexId, force.x, force.y);
	forces[vertexId] = (double2)(0.0, 0.0);
	vertices[vertexId] = v;
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
    double desiredLen = 1.0;
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