import os
import matplotlib.pyplot as plt
import numpy as np
import scipy.spatial as spatial
import cProfile
from tqdm import tqdm
from multiprocessing import Pool
from collections import defaultdict
from shapely.geometry import Polygon, MultiPolygon, Point
from functools import reduce


def extract_pedestrians_in_area(df, area):
  """ Extract pedestrian inside the given area
  """
  is_x = (df.x.values >= area[0]) & (df.x.values <= (area[0] + area[2]))
  is_y = (df.y.values >= area[1]) & (df.y.values <= (area[1] + area[3]))
  return df[is_x & is_y]

# Taken from 
# https://stackoverflow.com/questions/23901943/voronoi-compute-exact-boundaries-of-every-region
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
  """ Process given timestep
  """
  _, trajectories, context = args

  # get pedestrians inside area
  area = context.get('area')
  pedestrians = extract_pedestrians_in_area(trajectories, area)

  hasVelocity = 'velocity' in pedestrians.columns

  # skip timesteps with less than 4 pedestrians
  if len(pedestrians) < 4:
      return None

  boundary = context.get('boundary')

  positions = list(zip(pedestrians.x.values, pedestrians.y.values))

  # generate voronoi diagram
  voronoi = spatial.Voronoi(positions)
  
  xdim = context.get('xdim')
  ydim = context.get('ydim')
  coordinates = context.get('coordinates')
  D = np.zeros((ydim * xdim, 1))
  V = np.zeros((ydim * xdim, 1))

  diameter = context.get('diameter')

  polygons = []
  # convert infinite voronoi cell to finite 
  # and cut out area inside boundaries
  for p, pos in voronoi_polygons(voronoi, diameter):
    intersection = p.intersection(boundary)
    if type(intersection) == MultiPolygon:
      for i in range(len(intersection)):
        polygons.append((intersection[i], 1 / intersection[i].area, pos))

    if type(intersection) == Polygon:
      polygons.append((intersection, 1 / intersection.area, pos))

  number_of_polygons = len(polygons)
  interval = range(int(number_of_polygons / 2) + 2)

  for index, point in coordinates:
    p = Point(point)
      
    # skip points outside of scenario
    if not boundary.contains(p):
      continue

    # search for polygon containing current grid point
    for i in interval:
      if polygons[i][0].contains(p):
        D[index] = polygons[i][1]
        if hasVelocity:
          V[index] = pedestrians[(pedestrians.x.values == polygons[i][2][0]) & (pedestrians.y.values == polygons[i][2][1])].velocity.values[0]
        break
      
      end = number_of_polygons - 1 - i
      if polygons[end][0].contains(p):
        D[index] = polygons[end][1]
        if hasVelocity:
          V[index] = pedestrians[(pedestrians.x.values == polygons[end][2][0]) & (pedestrians.y.values == polygons[end][2][1])].velocity.values[0]
        break
  
  return D, V


def sum(x1, x2):
  return x1 + x2


def run(df, context, nprocesses=3):  
  """ Process experiment
  """

  # create coordinate grid
  area = context.get('area')
  resolution = context.get('resolution', 0.1)
  x = np.arange(area[0], area[0] + area[2], resolution)
  y = np.arange(area[1], area[1] + area[3], resolution)
  xdim = len(x)
  ydim = len(y)

  xx, yy = np.meshgrid(x, y)
  coordinates = list(zip(xx.ravel(), yy.ravel()))

  context['xdim'] = xdim
  context['ydim'] = ydim

  context['coordinates'] = enumerate(coordinates)

  # calculate radius of voronoi calculation
  boundary = context.get('boundary')
  bb = np.array(boundary.boundary.coords)
  diameter = np.linalg.norm(bb.ptp(axis=0))

  context['diameter'] = diameter

  skip = context.get('skip', 1)

  # group trajectories by timestep
  grouped = list(df.groupby('timeStep'))

  # take every n-th frame
  timesteps = list(grouped)[::skip]

  # extend data with context
  timesteps = list(map(lambda a: (*a, context), timesteps))
  total = len(timesteps)

  densities = []
  velocities = []

  
  with Pool(processes=nprocesses) as p:
    with tqdm(total=total, desc=context.get('name')) as pbar:
      # process timesteps in parallel
      for _, result in enumerate(p.imap_unordered(process_timestep, timesteps)):
        pbar.update()

        # append heatmaps for timestep to collection
        if result is not None:
          D, V = result
          densities.append(D)
          velocities.append(V)
  
  # sum up all heatmaps and normalize
  density = reduce(sum, densities) / len(densities)
  velocity = reduce(sum, velocities) / len(velocities)
  
  # flip and reshape data
  return np.flipud(density.reshape((ydim, xdim))), np.flipud(velocity.reshape((ydim, xdim)))
