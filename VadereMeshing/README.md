# EikMesh - A parallel mesh generator for 2D unstructured meshes

## Introduction

The eikmesh Java library contains multiple algorithms and data structures to generate, change and use unstructured 2D triangular meshes.
The library implements a generic version of the edge based half-edge data structure also called doubly connected edge list (DCEL).
User defined data types can be easily stored at and accessed via mesh elements (vertices, (half-)edges, faces / triangles).
Given some mesh elements, adjacent elements can be accessed in O(1) time.
Each generated mesh is conforming. Holes are supported.

The aim was to provide a fast, light and user-friendly meshing tool with parametric input, generic data types and advanced visualization capabilities.
eikmesh generates
- exact Delaunay triangulations (DT),
- constrained Delaunay triangulations (CDT),
- conforming Delaunay triangulations (CCDT),
- Voronoi diagrams, and 
- high-quality unstructured and conforming triangular meshes.

## EikMesh

The EikMesh mesh generator was published in [1] and is heavily based on DistMesh a simple and mesh generator (in MATLAB) which was developed by Per-Olof Persson and Gilbert Strang.
EikMesh inherits its specification of the geometry via signed distance functions and the concept of iterative smoothing by converging towards a force equilibrium from DistMesh.
However, EikMesh completely avoids the computation of the Delaunay triangulation, generates a different and cache friendly initial triangulation and treats boundary elements more carefully.
Additionally, EikMesh supports geometries defined by a [segment bounded planar straight-line graphs](https://en.wikipedia.org/wiki/Planar_straight-line_graph) (PSLG).

## Documentation

- [eikmesh - a Java library for 2D unstructured triangular meshes](TODO)
- [A parallel generator for sparse unstructured meshes to solve the eikonal equation](https://doi.org/10.1016/j.jocs.2018.09.009)
- [Wiki](https://gitlab.lrz.de/vadere/vadere/wikis/eikmesh/Overview): Learn how to use the mesh data structure, execute different meshing algorithms and EikMesh by examples.

## Download

The source code is available at [GitLab](https://gitlab.lrz.de/vadere/vadere/tree/master/VadereMeshing).
A pre-compiled version can be download [here](TODO). 
eikmesh is part of [Vadere](http://www.vadere.org/) but can be used seperately. It is distributed under the LGPL license.

## Examples

### Videos

### Code

```java
VRectangle bound = new VRectangle(-0.1, -0.1, 2.2, 2.2);
IDistanceFunction d_r = IDistanceFunction.createRing(1, 1, 0.2, 1.0);
double h_min = 0.1;
PEikMesh meshImprover = new PEikMesh(d_r,h_min,bound);
meshImprover.generate();
```

```java 
// read a planar straight line graph from an input stream
PSLG pslg = ...
double h_min = 0.02;
PEikMesh meshImprover = new PEikMesh(pslg.getSegmentBound(), h_min, pslg.getHoles());
```


```java
VRectangle bound = ...
VRectangle rect = new VRectangle(0.5, 0.5, 1, 1);
IDistanceFunction d_c = IDistanceFunction.createDisc(0.5, 0.5, 0.5);
IDistanceFunction d_r = IDistanceFunction.create(rect);
IDistanceFunction d = IDistanceFunction.substract(d_c, d_r);
double h_min = 0.03;
var meshImprover = new PEikMeshGen<EikMeshPoint, Double, Double>(
				d,
				p -> h_min + 0.5 * Math.abs(d.apply(p)),
				h_min,
				bound,
				Arrays.asList(rect),
				(x, y) -> new EikMeshPoint(x, y, false));
```

```java
// inner rectangle
VRectangle rect = new VRectangle(-0.5, -0.5, 1, 1);

// outer rectangle
VRectangle boundary = new VRectangle(-2,-0.7,4,1.4);

// construction of the distance function, define the 2 discs
IDistanceFunction d1_c = IDistanceFunction.createDisc(-0.5, 0, 0.5);
IDistanceFunction d2_c = IDistanceFunction.createDisc(0.5, 0, 0.5);

// define the two rectangles
IDistanceFunction d_r = IDistanceFunction.create(rect);
IDistanceFunction d_b = IDistanceFunction.create(boundary);

// combine distance functions
IDistanceFunction d_unionTmp = IDistanceFunction.union(d1_c, d_r)
IDistanceFunction d_union = IDistanceFunction.union(d_unionTmp, d2_c);
IDistanceFunction d = IDistanceFunction.substract(d_b,d_union);

// h_min
double h_min = 0.03;

var meshImprover = new PEikMeshGen<EikMeshPoint, Double, Double>(
				d,
				p -> h_min + 0.5 * Math.abs(d.apply(p)),
				edgeLength,
				GeometryUtils.boundRelative(boundary.getPath()),
				Arrays.asList(rect),
				(x, y) -> new EikMeshPoint(x, y, false));

// generate the mesh
var triangulation = meshImprover.generate();
```