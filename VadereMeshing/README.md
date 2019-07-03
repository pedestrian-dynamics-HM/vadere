# EikMesh - A parallel mesh generator for 2D unstructured meshes

## Introduction

The EikMesh Java library is part of Vadere and was developed to generate high quality meshes for spatial discretization in the domain of pedestrian dynamics.
We are especially interested in reducing the complexity of dynamic floor field computation which is one of our active research topics.
Computing a floor field involves the solution of the eikonal equation which led us to the name EikMesh.
Aside from the EikMesh algorithm, the library contains multiple algorithms and data structures to generate, change and use unstructured 2D triangular meshes.
The library implements a generic version of the edge based half-edge data structure also called doubly connected edge list (DCEL).
User defined data types can be easily stored at and accessed via mesh elements (vertices, (half-)edges, faces / triangles).
Given some mesh elements, adjacent elements can be accessed in O(1) time. Each generated mesh is conforming. Holes are supported.

The aim was to provide a fast, light and user-friendly meshing tool with parametric input, generic data types and advanced visualization capabilities.
The EikMesh library generates
- Delaunay triangulations (DT),
- constrained Delaunay triangulations (CDT),
- conforming Delaunay triangulations (CCDT),
- Voronoi diagrams, and 
- ****high-quality unstructured conforming triangular meshes****.

## The EikMesh algorithm

The  [EikMesh](https://doi.org/10.1016/j.jocs.2018.09.009) algorithm is heavily based on [DistMesh](http://persson.berkeley.edu/distmesh/) a simple and mesh generator (in MATLAB) which was developed by Per-Olof Persson and Gilbert Strang.
EikMesh inherits from DistMesh its specification of the geometry via signed distance functions and the concept of iterative smoothing by converging towards a force equilibrium.
However, EikMesh completely avoids the computation of the Delaunay triangulation, generates a different and cache friendly initial triangulation and treats boundary elements more carefully.
Additionally, EikMesh supports geometries defined by a [segment bounded planar straight-line graphs](https://en.wikipedia.org/wiki/Planar_straight-line_graph) (PSLG).

## Documentation

- User manuel: [documentation.pdf](http://www.vadere.org/documentation/eikmesh_manual_short.pdf) (offline manual), [Wiki](https://gitlab.lrz.de/vadere/vadere/wikis/eikmesh/EikMesh-Wiki) (online manual)
- Publication: [A parallel generator for sparse unstructured meshes to solve the eikonal equation](https://doi.org/10.1016/j.jocs.2018.09.009)
- Demos: [vadere.org](http://www.vadere.org/the-eikmesh-library/)

## Download

The source code is available at [GitLab](https://gitlab.lrz.de/vadere/vadere/tree/master/VadereMeshing).
A pre-compiled version can be download [here](TODO). 
EikMesh is part of [Vadere](http://www.vadere.org/) but can be used seperately. 
It is distributed under the LGPL license.