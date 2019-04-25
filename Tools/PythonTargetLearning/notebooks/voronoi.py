import os
import matplotlib.pyplot as plt
import numpy as np
import scipy.spatial as spatial
from tqdm import tqdm
from multiprocessing import Pool
from collections import defaultdict
from shapely.geometry import Polygon, MultiPolygon, Point
from functools import reduce


def extract_pedestrians_in_area(df, area):
    is_x = (df['x'] >= area[0]) & (df['x'] <= (area[0] + area[2]))
    is_y = (df['y'] >= area[1]) & (df['y'] <= (area[1] + area[3]))
    return df[is_x & is_y]


def voronoi_polygons(voronoi, diameter):
    """Generate shapely.geometry.Polygon objects corresponding to the
    regions of a scipy.spatial.Voronoi object, in the order of the
    input points. The polygons for the infinite regions are large
    enough that all points within a distance 'diameter' of a Voronoi
    vertex are contained in one of the infinite polygons.

    """
    centroid = voronoi.points.mean(axis=0)

    # Mapping from (input point index, Voronoi point index) to list of
    # unit vectors in the directions of the infinite ridges starting
    # at the Voronoi point and neighbouring the input point.
    ridge_direction = defaultdict(list)
    for (p, q), rv in zip(voronoi.ridge_points, voronoi.ridge_vertices):
        u, v = sorted(rv)
        if u == -1:
            # Infinite ridge starting at ridge point with index v,
            # equidistant from input points with indexes p and q.
            t = voronoi.points[q] - voronoi.points[p] # tangent
            n = np.array([-t[1], t[0]]) / np.linalg.norm(t) # normal
            midpoint = voronoi.points[[p, q]].mean(axis=0)
            direction = np.sign(np.dot(midpoint - centroid, n)) * n
            ridge_direction[p, v].append(direction)
            ridge_direction[q, v].append(direction)

    for i, r in enumerate(voronoi.point_region):
        region = voronoi.regions[r]
        if -1 not in region:
            # Finite region.
            yield Polygon(voronoi.vertices[region]), voronoi.points[i]
            continue
        # Infinite region.
        inf = region.index(-1)              # Index of vertex at infinity.
        j = region[(inf - 1) % len(region)] # Index of previous vertex.
        k = region[(inf + 1) % len(region)] # Index of next vertex.
        if j == k:
            # Region has one Voronoi vertex with two ridges.
            dir_j, dir_k = ridge_direction[i, j]
        else:
            # Region has two Voronoi vertices, each with one ridge.
            dir_j, = ridge_direction[i, j]
            dir_k, = ridge_direction[i, k]

        # Length of ridges needed for the extra edge to lie at least
        # 'diameter' away from all Voronoi vertices.
        length = 2 * diameter / np.linalg.norm(dir_j + dir_k)

        # Polygon consists of finite part plus an extra edge.
        finite_part = voronoi.vertices[region[inf + 1:] + region[:inf]]
        extra_edge = [voronoi.vertices[j] + dir_j * length,
                      voronoi.vertices[k] + dir_k * length]
        yield Polygon(np.concatenate((finite_part, extra_edge))), voronoi.points[i]


def process_timestep(args):
  _, trajectories, context = args

  area = context.get('area')
  pedestrians = extract_pedestrians_in_area(trajectories, area)

  # skip timesteps with less than 4 pedestrians
  if len(pedestrians) < 4:
      return None

  
  boundary = context.get('boundary')

  positions = pedestrians[['x', 'y']].to_numpy()
  
  voronoi = spatial.Voronoi(positions)
  
  x = np.arange(area[0], area[0] + area[2], 0.1)
  y = np.arange(area[1], area[1] + area[3], 0.1)
  xdim = len(x)
  ydim = len(y)

  xx, yy = np.meshgrid(x, y)
  xr = xx.ravel()
  yr = yy.ravel()
  grid = np.c_[xr, yr, [0 for i in range(len(xr))], [0 for i in range(len(xr))]]

  # generate voronoi diagramm 
  bb = np.array(boundary.boundary.coords)
  diameter = np.linalg.norm(bb.ptp(axis=0))

  polygons = []
  for p, pos in voronoi_polygons(voronoi, diameter):
      intersection = p.intersection(boundary)
      if type(intersection) == MultiPolygon:
          for i in range(len(intersection)):
              polygons.append((intersection[i], 1 / intersection[i].area, pos))

      if type(intersection) == Polygon:
          polygons.append((intersection, 1 / intersection.area, pos))

  for point in grid:
      p = Point(point[:2])
      
      # skip points outside of scenario
      if not boundary.contains(p):
          continue

      number_of_polygons = len(polygons)
      for i in range(int(number_of_polygons / 2) + 2):
        if polygons[i][0].contains(p):
          point[2] = polygons[i][1]
          point[3] = list(pedestrians[ (pedestrians['x'] == polygons[i][2][0]) & (pedestrians['y'] == polygons[i][2][1])]['velocity'])[0]
          break
        
        if polygons[number_of_polygons - 1 - i][0].contains(p):
          point[2] = polygons[number_of_polygons - 1 - i][1]
          point[3] = list(pedestrians[ (pedestrians['x'] == polygons[number_of_polygons - 1 - i][2][0]) & (pedestrians['y'] == polygons[number_of_polygons - 1 - i][2][1])]['velocity'])[0]
          break
  
  # normalize
  D = grid[:, 2].reshape((xdim*ydim, 1))
  D = D / (np.max(D) / 4.5)
  V = grid[:, 3].reshape((xdim*ydim, 1))
  
  return D, V, xdim, ydim


def sum(x1, x2):
  return x1 + x2


def run(df, context, nprocesses=3):  
  
  skip = context.get('skip', 1)

  grouped = list(df.groupby('timeStep'))
  timesteps = list(grouped)[::skip]
  timesteps = list(map(lambda a: (*a, context), timesteps))
  total = len(timesteps)

  densities = []
  velocities = []
  xdim = 0
  ydim = 0
  with Pool(processes=nprocesses) as p:
        with tqdm(total=total) as pbar:
            for _, result in enumerate(p.imap_unordered(process_timestep, timesteps)):
              pbar.update()

              if result is not None:
                D, V, xdim, ydim = result
                densities.append(D)
                velocities.append(V)


  density = reduce(sum, densities) / len(densities)
  # density = density - np.min(density[density > 0])
  # density[density < 0] = 0

  velocity = reduce(sum, velocities) / len(velocities)
  # velocity = velocity - np.min(velocity[velocity > 0])
  # velocity[velocity < 0] = 0

  return np.flipud(density.reshape((ydim, xdim))), np.flipud(velocity.reshape((ydim, xdim)))
