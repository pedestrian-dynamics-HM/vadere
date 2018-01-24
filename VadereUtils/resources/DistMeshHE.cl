#pragma OPENCL EXTENSION cl_khr_fp64 : enable
#pragma OPENCL EXTENSION cl_khr_global_int32_base_atomics : enable
#pragma OPENCL EXTENSION cl_amd_printf : enable
#define LOCK(a) atomic_cmpxchg(a, 0, 1)
#define UNLOCK(a) atomic_xchg(a, 0)

// helper methods!
inline double2 getCircumcenter(double2 p1, double2 p2, double2 p3) {
    double d = 2 * (p1.x * (p2.y - p3.y) + p2.x * (p3.y - p1.y) + p3.x * (p1.y - p2.y));

    double x = ((p1.x * p1.x + p1.y * p1.y) * (p2.y - p3.y)
    				+ (p2.x * p2.x + p2.y * p2.y) * (p3.y - p1.y)
    				+ (p3.x * p3.x + p3.y * p3.y) * (p1.y - p2.y)) / d;
    		float y = ((p1.x * p1.x + p1.y * p1.y) * (p3.x - p2.x)
    				+ (p2.x * p2.x + p2.y * p2.y) * (p1.x - p3.x)
    				+ (p3.x * p3.x + p3.y * p3.y) * (p2.x - p1.x)) / d;
    return (x, y);
}

inline bool isCCW(double2 q, double2 p, double2 r) {
    return ((p.y - q.y) * (r.x - p.x) - (p.x - q.x) * (r.y - p.y)) < 0;
}

inline double quality(double2 p, double2 q, double2 r) {
    double a = length(p-q);
    double b = length(p-r);
    double c = length(q-r);
    double part = 0.0;

    //if(a != 0.0 && b != 0.0 && c != 0.0) {
        part = ((b + c - a) * (c + a - b) * (a + b - c)) / (a * b * c);
    //}

    return part;
}


inline bool isInCircle(double2 a, double2 b, double2 c, double x , double y) {
    double eps = 0.00001f;
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

inline bool alive(int4 edge) {
    return edge.s0 != -1;
}

inline bool faceAlive(int2 face) {
    return face.s0 != -1;
}

inline float dabs(double d) {return d < 0 ? -d : d;}

inline isAtBoundary(int4 edge, __global int4* edges){
    return getFace(edge) == -1 || getFace(edges[getTwin(edge)]);
}

inline int getVertex(int4 edge) {
    return edge.s0;
}

inline void setVertex(int4 edge, int vertexId) {
    edge.s0 = vertexId;
}

inline int getNext(int4 edge) {
    return edge.s1;
}

inline void setNext(int4 edge, int edgeId) {
    edge.s1 = edgeId;
}

inline int getTwin(int4 edge) {
    return edge.s2;
}

inline int getFace(int4 edge) {
    return edge.s3;
}

inline void setFace(int4 edge, int faceId) {
    edge.s3 = faceId;
}

inline double dist(double2 v) {

}
// end helper

// remove low quality triangles on the boundary
kernel void removeTriangles(__global double2* vertices,
                            __global int4* edges,
                            __global int3* triangles) {
    int edgeId = get_global_id(0);

    // edge is alive?
    if(alive(edges[edgeId])){
        float eps = 0.000001f;

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
                  __global int* labeledEdges,
                  __global int* illegalEdge) {
    int edgeId = get_global_id(0);

    /**
     * edge.s0 = edge.vertexId
     * edge.s1 = edge.nextId
     * edge.s2 = edge.twinId
     * edge.s3 = edge.faceId
     */
    int4 edge = edges[edgeId];

    if(alive(edge)){
        float eps = 0.0001;

        int v0 = getVertex(edge);

        // edge is a non-boundary edge
        if(!isAtBoundary(edge, edges)) {
            int4 nextEdge = edges[getNext(edge)];
            int4 prefEdge = edges[getNext(nextEdge)];
            int4 twinEdge = edges[getTwin(edge)];

            int p = getVertex(edges[getNext(twinEdge)]);
            int v1 = getVertex(nextEdge);
            int v2 = getVertex(prefEdge);

            double2 c = getCircumcenter(vertices[v0], vertices[v1], vertices[v2]);
            // require a flip?
            if(length(c-vertices[p]) < length(c-vertices[v0])) {
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

// for each edge in parallel, label illegal edges
kernel void updateLabel(__global double2* vertices,
                        __global int4* edges,
                        __global int* labeledEdges,
                        __global int* illegalEdge) {
    int edgeId = get_global_id(0);

    /**
     * edge.s0 = edge.vertexId
     * edge.s1 = edge.nextId
     * edge.s2 = edge.twinId
     * edge.s3 = edge.faceId
     */
    int4 edge = edges[edgeId];

    if(alive(edge)){

        // TODO:
        //lockedTriangles[ta] = -1;
        //lockedTriangles[tb] = -1;
        double eps = 0.00001;
        int v0 = getVertex(edge);

        // edge is a non-boundary edge
        if(!isAtBoundary(edge, edges) && labeledEdges[edgeId] == 1) {
            int4 nextEdge = edges[getNext(edge)];
            int4 prefEdge = edges[getNext(nextEdge)];
            int4 twinEdge = edges[getTwin(edge)];

            int p = getVertex(edges[getNext(twinEdge)]);
            int v1 = getVertex(nextEdge);
            int v2 = getVertex(prefEdge);

            double2 c = getCircumcenter(vertices[v0], vertices[v1], vertices[v2]);
            // require a flip?
            if(length(c-vertices[p]) < length(c-vertices[v0])) {
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
        if(edgeId < getTwin(edge) && faces[faceId].s1 == edgeId) {
            faces[getFace(twinEdge)].s1 = edgeId;
        }
    }
}

// face.s0 = edgeid , face.s1 = lockedby edge?
kernel void flipStage3(__global double2* vertices,
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

            //if(mesh.getEdge(fb).equals(b1)) {
                faces[fbId].s0 = a1Id;
            //	mesh.setEdge(fb, a1);
            //}

            //if(mesh.getEdge(fa).equals(a1)) {
                faces[faId].s0 = b1Id;
            //	mesh.setEdge(fa, b1);
            //}

            //if(mesh.getEdge(va0).equals(a0)) {
                vertexToEdge[va0Id] = b2Id;
            //	mesh.setEdge(va0, b2);
            //}

            //if(mesh.getEdge(vb0).equals(b0)) {
                vertexToEdge[vb0Id] = a2Id;
            //	mesh.setEdge(vb0, a2);
            //}

            setVertex(a0, va1Id);
            setVertex(b0, vb1Id);

            setNext(a0, a2Id);
            setNext(a2, b1Id);
            setNext(b1, a0Id);

            setNext(b0, b2Id);
            setNext(b2, a1Id);
            setNext(a1, b0Id);

            setFace(a1, fbId);
            setFace(b1, faId);

            // copy to global mem
            edges[a0Id] = a0;
            edges[a1Id] = a1;
            edges[a2Id] = a2;

            edges[b0Id] = b0;
            edges[b1Id] = b1;
            edges[b2Id] = b2;

            labeledEdges[edgeId] = 0;
            labeledEdges[getTwin(edge)] = 0;
        }
    }
}

// for each triangle test its legality TODO
 kernel void checkTriangles(__global double2* vertices,
                            __global int4* edges,
                            __global int2* faces,
                            __global int* illegalTri) {
    int faceId = get_global_id(0);
    int2 face = faces[faceId];
    if(faceAlive(face)) {
        int edgeId = face.s0;

        int4 edge = edges[edgeId];
        int4 nextEdge = edges[getNext(edge)];
        int4 prefEdge = edges[getNext(nextEdge)];

        int v0 = getVertex(edge);
        int v1 = getVertex(nextEdge);
        int v2 = getVertex(prefEdge);

        double2 p0 = vertices[v0];
        double2 p1 = vertices[v1];
        double2 p2 = vertices[v2];

        // triangle is illegal => re-triangulation is necessary!
        if(*illegalTri != 1 && !isCCW(p0, p1, p2)) {
            *illegalTri = 1;
        }
    }
}


// for each vertex in parallel
kernel void computeForces(__global double2* vertices,
                          __global int4* edges,
                          __global int* vertexToEdge,
                          __global double2* lengths,
                          __global double* scalingFactor,
                          __global double2* forces)
{
    int vertexId = get_global_id(0);
    int edgeId = vertexToEdge[vertexId];
    int4 edge = edges[edgeId];
    int nextEdgeId = edgeId;
    int4 nextEdge = edge;
    bool first = true;
    double2 force = (0.0, 0.0);

    while(first || edgeId != nextEdgeId){
        first = true;

        // compute force
        int4 twinEdge = edges[getTwin(edge)];
        double2 p0 = vertices[getVertex(twinEdge)];
        double2 p1 = vertices[getVertex(edge)];

        double2 v = normalize(p0-p1);
        double len = lengths[edgeId].s0;
        double desiredLen = lengths[edgeId].s1 * (*scalingFactor) * 1.2f;
        double lenDiff = (desiredLen - len);
        lenDiff = lenDiff > 0 ? lenDiff : 0;
        double2 partialForce = v * lenDiff;

        force = force + partialForce;

        // go on
        nextEdgeId = getNext(edge);
        nextEdge = edges[nextEdgeId];
    }

    forces[vertexId] = force;
}

// for each vertex
kernel void moveVertices(__global double2* vertices,
                         __global double2* forces,
                         const double delta) {
    int vertexId = get_global_id(0);

    double deps = 1.4901e-8f;
    double2 force = forces[vertexId];
    double2 v = vertices[vertexId];

    v = v + (force * 0.3f);

    // project back if necessary
    double distance = dist(v);
    if(distance > 0.0) {
        double2 dX = (deps, 0.0);
        double2 dY = (0.0, deps);
        double dGradPX = (dist(v + dX) - distance) / deps;
        double dGradPY = (dist(v + dY) - distance) / deps;
        double2 projection = (dGradPX * distance, dGradPY * distance);
        v = v - projection;
    }

    // set force to 0.
    forces[vertexId] = (0.0f, 0.0f);
    vertices[vertexId] = v;
}

// computation of the scaling factor:
kernel void computeLengths(
    __global double2* vertices,
    __global int4* edges,
    __global double2* lengths,
    __global double2* qLengths)
{
    int edgeId = get_global_id(0);
    int4 edge = edges[edgeId];
    int4 twinEdge = edges[getTwin(edge)];

    double2 p0 = vertices[getVertex(twinEdge)];
    double2 p1 = vertices[getVertex(edge)];
    double2 v = p0-p1;

    //TODO: desiredLenfunction required
    double desiredLen = 1.0;
    double2 len = (length(v), desiredLen);
    lengths[edgeId] = len;
    qLengths[edgeId] = (length(v)*length(v), desiredLen*desiredLen);
}


// kernel for multiple work-groups
kernel void computePartialSF(__const int size, __global double2* qlengths, __local double2* partialSums, __global double2* output) {
    int gid = get_global_id(0);
    int lid = get_local_id(0);

    if(gid < size){
        int global_index = gid;
        double2 accumulator = (0.0, 0.0);
        // Loop sequentially over chunks of input vector
        while (global_index < size) {
            double2 element = qlengths[global_index];
            accumulator += element;
            global_index += get_global_size(0);
        }

        int group_size = get_local_size(0);
        double2 len = accumulator;

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
            output[get_group_id(0)] = (0.0, 0.0);
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