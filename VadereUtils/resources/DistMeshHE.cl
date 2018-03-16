/**
 * The implementation of DistMesh using the half-edge data structure.
 * Forces are computed for each vertex in parallel instead of for each
 * edge in parallel. Edge flips are done via a three stages (for each edge in parallel):
 * (1) Lock the first triangle A for edge e
 * (2) Lock the second triangle B for edge e if e holds the lock of A
 * (3) Flip edge e if e holds the lock for A and B
 */

#pragma OPENCL EXTENSION cl_khr_global_int32_base_atomics : enable
#pragma OPENCL EXTENSION cl_amd_printf : enable
#define LOCK(a) atomic_cmpxchg(a, 0, 1)
#define UNLOCK(a) atomic_xchg(a, 0)

// helper methods!
inline float2 getCircumcenter(float2 p1, float2 p2, float2 p3) {
    float d = 2 * (p1.x * (p2.y - p3.y) + p2.x * (p3.y - p1.y) + p3.x * (p1.y - p2.y));

    float x = ((p1.x * p1.x + p1.y * p1.y) * (p2.y - p3.y)
    				+ (p2.x * p2.x + p2.y * p2.y) * (p3.y - p1.y)
    				+ (p3.x * p3.x + p3.y * p3.y) * (p1.y - p2.y)) / d;
    float y = ((p1.x * p1.x + p1.y * p1.y) * (p3.x - p2.x)
    				+ (p2.x * p2.x + p2.y * p2.y) * (p1.x - p3.x)
    				+ (p3.x * p3.x + p3.y * p3.y) * (p2.x - p1.x)) / d;
    return (float2)(x, y);
}

inline bool isCCW(float2 q, float2 p, float2 r) {
    return ((p.y - q.y) * (r.x - p.x) - (p.x - q.x) * (r.y - p.y)) < 0;
}

inline float quality(float2 p, float2 q, float2 r) {
    float a = length(p-q);
    float b = length(p-r);
    float c = length(q-r);
    return (float)((b + c - a) * (c + a - b) * (a + b - c)) / (a * b * c);
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

inline bool isEdgeAlive(int4 edge) {
    return edge.s0 != -1;
}

inline bool isFaceAlive(int2 face) {
    return face.s0 != -1;
}

inline float dabs(float d) {if(d < 0){ return -d; }else{ return d;}}

inline bool isAtBoundary(int4 edge, __global int4* edges){
    return getFace(edge) == -1 || getFace(edges[getTwin(edge)]) == -1;
}

inline int getVertex(int4 edge) {
    return edge.s0;
}

inline int getNext(int4 edge) {
    return edge.s1;
}

inline int getTwin(int4 edge) {
    return edge.s2;
}

inline int getFace(int4 edge) {
    return edge.s3;
}

inline float dist(float2 v) {
    return dabs(7.0f - length(v))-3.0f;
}
// end helper

// remove low quality triangles on the boundary
kernel void removeTriangles(__global float2* vertices,
                            __global int4* edges,
                            __global int3* triangles) {
    int edgeId = get_global_id(0);

    // edge is alive?
    if(isEdgeAlive(edges[edgeId])){
        float eps = 0.00001f;

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
                  __global int* labeledEdges,
                  __global int* illegalEdge) {
    /**
     * edge.s0 = edge.vertexId
     * edge.s1 = edge.nextId
     * edge.s2 = edge.twinId
     * edge.s3 = edge.faceId
     */
	 int edgeId = get_global_id(0);
    int4 edge = edges[edgeId];
    if(isEdgeAlive(edge) && !isAtBoundary(edge, edges)){
        float eps = 0.0001f;

        int v0 = getVertex(edge);
		int4 nextEdge = edges[getNext(edge)];
		int4 prefEdge = edges[getNext(nextEdge)];
		int4 twinEdge = edges[getTwin(edge)];

		int p = getVertex(edges[getNext(twinEdge)]);
		int v1 = getVertex(nextEdge);
		int v2 = getVertex(prefEdge);

		// test delaunay criteria
		//float2 c = getCircumcenter(vertices[v0], vertices[v1], vertices[v2]);
		float2 pp = vertices[p];
        //double2 c = getCircumcenter(vertices[v0], vertices[v1], vertices[v2]);
         // require a flip?
        if(isInCircle(vertices[v0], vertices[v1], vertices[v2], pp.x, pp.y)) {
            labeledEdges[edgeId] = 1;
            *illegalEdge = 1;
        } else {
             labeledEdges[edgeId] = 0;
        }
    }
    else {
        labeledEdges[edgeId] = 0;
    }
}

// for each edge in parallel, re-label illegal edges
kernel void updateLabel(__global float2* vertices,
                        __global int4* edges,
                        __global int* labeledEdges,
                        __global int* illegalEdge) {
    /**
     * edge.s0 = edge.vertexId
     * edge.s1 = edge.nextId
     * edge.s2 = edge.twinId
     * edge.s3 = edge.faceId
     */
	int edgeId = get_global_id(0);
    int4 edge = edges[edgeId];

    if(isEdgeAlive(edge) && labeledEdges[edgeId] == 1 && !isAtBoundary(edge, edges)){
        float eps = 0.0001f;
        int v0 = getVertex(edge);
		int4 nextEdge = edges[getNext(edge)];
		int4 prefEdge = edges[getNext(nextEdge)];
		int4 twinEdge = edges[getTwin(edge)];

		int p = getVertex(edges[getNext(twinEdge)]);
		int v1 = getVertex(nextEdge);
		int v2 = getVertex(prefEdge);

		// test delaunay criteria
		float2 pp = vertices[p];
        //double2 c = getCircumcenter(vertices[v0], vertices[v1], vertices[v2]);
         // require a flip?
        if(isInCircle(vertices[v0], vertices[v1], vertices[v2], pp.x, pp.y)) {
            labeledEdges[edgeId] = 1;
            *illegalEdge = 1;
        } else {
             labeledEdges[edgeId] = 0;
        }
    }
    else {
        labeledEdges[edgeId] = 0;
    }
}

// lock triangle A
kernel void flipStage1(__global int4* edges,
                       __global int* labeledEdges,
                       __global int2* faces) {
    int edgeId = get_global_id(0);

    // is the edge illegal
    if(labeledEdges[edgeId] == 1) {
        int4 edge = edges[edgeId];
        int face = getFace(edges[edgeId]);

        // to avoid the twin from locking
        if(edgeId < getTwin(edge)) {
            faces[face].s1 = edgeId;
        }
    }
}

// lock triangle B
kernel void flipStage2(__global int4* edges,
                       __global int* labeledEdges,
                       __global int2* faces) {

    int edgeId = get_global_id(0);

    // is the edge illegal
    if(labeledEdges[edgeId] == 1) {
        int4 edge = edges[edgeId];
        int4 twinEdge = edges[getTwin(edge)];
        int faceId = getFace(edges[edgeId]);

        // to avoid the twin from locking
        if(faces[faceId].s1 == edgeId) {
            faces[getFace(twinEdge)].s1 = edgeId;
        }
    }
}

// flip
kernel void flipStage3(__global float2* vertices,
                       __global int* vertexToEdge,
                       __global int4* edges,
                       __global int* labeledEdges,
                       __global int2* faces) {
    int edgeId = get_global_id(0);

    if(labeledEdges[edgeId] == 1) {
        int4 edge = edges[edgeId];
        int4 twinEdge = edges[getTwin(edge)];

        // swap if both triangles are locked by ta, i.e. by this thread
        if(faces[getFace(edge)].s1 == edgeId && faces[getFace(twinEdge)].s1 == edgeId) {
            // flip and repair ds

            // 1. gather all the references required
            int a0Id = edgeId;
            int4 a0 = edge;
            int a1Id = getNext(a0);
            int4 a1 = edges[a1Id];
            int a2Id = getNext(a1);
            int4 a2 = edges[getNext(a1)];

            int b0Id = getTwin(edge);
            int4 b0 = edges[b0Id];
            int b1Id = getNext(b0);
            int4 b1 = edges[b1Id];
            int b2Id = getNext(b1);
            int4 b2 = edges[b2Id];

            int faId = getFace(a0);
            int fbId = getFace(b0);

            int va1Id = getVertex(a1);
            int vb1Id = getVertex(b1);

            int va0Id = getVertex(a0);
            int vb0Id = getVertex(b0);

            if(faces[fbId].s0 == b1Id) {
				// setEdge(fb, a1);
                faces[fbId].s0 = a1Id;
            }

            if(faces[faId].s0 == a1Id) {
				// setEdge(fa, b1);
                faces[faId].s0 = b1Id;
            }

            if(vertexToEdge[va0Id] == a0Id) {
				// setEdge(va0, b2);
                vertexToEdge[va0Id] = b2Id;
            }

            if(vertexToEdge[vb0Id] == b0Id) {
				// setEdge(vb0,a2);
                vertexToEdge[vb0Id] = a2Id;
            }

			/**
			 * edge.s0 = edge.vertexId
			 * edge.s1 = edge.nextId
			 * edge.s2 = edge.twinId
			 * edge.s3 = edge.faceId
			 */
            a0.s0 = va1Id;
            b0.s0 = vb1Id;

            a0.s1 = a2Id;
            a2.s1 = b1Id;
            b1.s1 = a0Id;

            b0.s1 = b2Id;
            b2.s1 = a1Id;
            a1.s1 = b0Id;

            a1.s3 = fbId;

            //setFace(b1, faId);
            b1.s3 = faId;

            // copy to global mem
            edges[a0Id] = a0;
            edges[a1Id] = a1;
            edges[a2Id] = a2;

            edges[b0Id] = b0;
            edges[b1Id] = b1;
            edges[b2Id] = b2;

            labeledEdges[a0Id] = 0;
            labeledEdges[b0Id] = 0;
        }
    }
}

// for each triangle: remove all locks
kernel void unlockFaces(__global int2* faces) {
    int faceId = get_global_id(0);
    faces[faceId].s1 = -1;
}

// for each triangle: test its legality TODO
 kernel void checkTriangles(__global float2* vertices,
                            __global int4* edges,
                            __global int2* faces,
                            __global int* illegalTri) {
    int faceId = get_global_id(0);
    int2 face = faces[faceId];
    if(isFaceAlive(face)) {
        int edgeId = face.s0;

        int4 edge = edges[edgeId];
        int4 nextEdge = edges[getNext(edge)];
        int4 prefEdge = edges[getNext(nextEdge)];

        int v0 = getVertex(edge);
        int v1 = getVertex(nextEdge);
        int v2 = getVertex(prefEdge);

        float2 p0 = vertices[v0];
        float2 p1 = vertices[v1];
        float2 p2 = vertices[v2];

        // triangle is illegal => re-triangulation is necessary!
        if(*illegalTri != 1 && !isCCW(p0, p1, p2)) {
            *illegalTri = 1;
        }
    }
}


// for each vertex in parallel
kernel void computeForces(__global float2* vertices,
                          __global int4* edges,
                          __global int* vertexToEdge,
                          __global float2* lengths,
                          __global float* scalingFactor,
                          __global float2* forces)
{
    int vertexId = get_global_id(0);
    int edgeId = vertexToEdge[vertexId];
    if(edgeId != -1){
        int4 edge = edges[edgeId];
        int twinId = edgeId;
        int4 twinEdge = edge;
        bool first = true;
        float2 force = (float2)(0.0f, 0.0f);
        float2 p0 = vertices[vertexId];

        while(first || (edgeId != twinId)){
            first = false;

            // compute force
            int nextId = getNext(twinEdge);
            int4 nextEdge = edges[nextId];
            float2 p1 = vertices[getVertex(nextEdge)];

            float2 v = normalize(p0-p1);
            float len = lengths[nextId].s0;
            float desiredLen = lengths[nextId].s1 * (*scalingFactor) * 1.2f;
            float lenDiff = (desiredLen - len);
            lenDiff = max(lenDiff, 0.0f);
            float2 partialForce = v * lenDiff;
            force = force + partialForce;

            // go on
            twinId = getTwin(nextEdge);
            twinEdge = edges[twinId];
        }

        forces[vertexId] = force;
    }
}

// for each vertex
kernel void moveVertices(__global float2* vertices,
                         __global int* borderVertices,
                         __global float2* forces,
                         const float delta) {
    int vertexId = get_global_id(0);
    float deps = 0.00001f;
    float2 force = forces[vertexId];
    float2 v = vertices[vertexId];

    v = v + (force * 0.3f);

    // project back if necessary
    float distance = dist(v);
    if(distance > 0.0f || borderVertices[vertexId] == 1) {
        float2 dX = (float2)(deps, 0.0f);
        float2 dY = (float2)(0.0f, deps);
        float2 vx = (float2)(v + dX);
        float2 vy = (float2)(v + dY);
        float dGradPX = (dist(vx) - distance) / deps;
        float dGradPY = (dist(vy) - distance) / deps;
        float2 projection = (float2)(dGradPX * distance, dGradPY * distance);
        v = v - projection;
    }

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
    int edgeId = get_global_id(0);
    int4 edge = edges[edgeId];
    int4 twinEdge = edges[getTwin(edge)];

    float2 p0 = vertices[getVertex(twinEdge)];
    float2 p1 = vertices[getVertex(edge)];
    float2 v = p0-p1;

    //TODO: desiredLenfunction required
    float desiredLen = 1.0f;
    float2 len = (float2)(length(v), desiredLen);
    lengths[edgeId] = len;
    qLengths[edgeId] = (float2)(length(v)*length(v), desiredLen*desiredLen);
}


// kernel for multiple work-groups
kernel void computePartialSF(__const int size, __global float2* qlengths, __local float2* partialSums, __global float2* output) {
    int gid = get_global_id(0);
    int lid = get_local_id(0);

    if(gid < size){
        int global_index = gid;
        float2 accumulator = (0.0f, 0.0f);
        // Loop sequentially over chunks of input vector
        while (global_index < size) {
            float2 element = qlengths[global_index];
            accumulator += element;
            global_index += get_global_size(0);
        }

        int group_size = get_local_size(0);
        float2 len = accumulator;
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
          *scaleFactor = sqrt(partialSums[0].s0 / partialSums[0].s1);
        }
    }
}