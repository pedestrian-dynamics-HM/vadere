import sys
import os
import itertools
import numpy as np
import pandas as pd
from multiprocessing import Pool, Lock
from tqdm import tqdm


def extract_observation_area(frame, area):
    is_x = (frame['x'] >= area[0]) & (frame['x'] <= (area[0] + area[2]))
    is_y = (frame['y'] >= area[1]) & (frame['y'] <= (area[1] + area[3]))
    return frame[is_x & is_y]


def get_target_percentiles(pedestrians, ped2target, targets):
    if len(pedestrians) == 0:
        return None

    ids = list(pedestrians['pedestrianId'])
    total = len(ids)

    filtered_dict = {k: v for k, v in ped2target.items() if k in ids}
    used_targets = filtered_dict.values()

    percentiles = {k: (len(list(v)) / total)
                       for k, v in itertools.groupby(sorted(used_targets))}

    for k in targets:
        if k not in used_targets:
            percentiles[k] = 0.0

    return percentiles


def get_gaussian_grid(gauss_bound, resolution, sigma, ped_pos, radius):
        # gives gauss_bound_start:resolution:gauss_bound_stop
    x = np.arange(-gauss_bound, gauss_bound + resolution, resolution)

       # Make all grid points (based on resolution) in [gauss_bound_start, gauss_bound_stop] (2-dim-array)
    xx, yy = np.meshgrid(x, x, sparse=False)

       # distance to the origin of the observation area
    grid = np.sqrt(np.square(xx-ped_pos[0]) + np.square(yy-ped_pos[1]))

    gauss = np.vectorize(gaussian_pdf)

    gauss_grid = gauss(sigma, grid, radius)

    return gauss_grid


def gaussian_pdf(sigma, x, radius):
    zaehler = ((radius*2)**2) * np.sqrt(3)  # S_p
    nenner = 2 * 2 * np.pi * (sigma**2)

    normalization_factor = zaehler/nenner
    individual_density = normalization_factor * \
        np.exp(-x / (2 * np.square(sigma)))

    return individual_density


def add_pedestrian_to_field(ped, matrix, area, dimension, resolution, gauss_density_bound, sigma, ped_radius, general_density_matrix):
    if general_density_matrix is None:  # not None
        bool_individual_position = True
    else:
        bool_individual_position = False

    # calculate the density for one ped and add to matrix
    size = int(gauss_density_bound * 2 / resolution + 1)
    radius = int(size / 2)  # equal to gauss_density_bound/resolution

    origin_x = area[0]
    origin_y = area[1]

    height, width = dimension
    # print(width, height)

    # necessary to map to center of the grid instead of the edge!
    offset = resolution / 2

    # find grid cell of pedestrian in observation area
    diff_x = int(np.round((ped['x'] - origin_x - offset) / resolution, 0))
    diff_y = int(np.round((ped['y'] - origin_y - offset) / resolution, 0))

    # area in which the pedestrian has an influence on the pedestrian density
    left_bound = int(max(0, diff_x - radius))
    right_bound = int(min(diff_x + radius, width - 1))
    upper_bound = int(min(diff_y + radius, height - 1))
    lower_bound = int(max(0, diff_y - radius))

    # create density_field
    # position of pedestrian relative to center of grid cell
    # gives gauss_bound_start:resolution:gauss_bound_stop
    grid_points_x = np.arange(
        origin_x+offset, origin_x + area[2] + resolution, resolution)
    grid_points_y = np.arange(
        origin_y+offset, origin_y + area[3] + resolution, resolution)

    # center of cell in which the pedestrian is located
    grid_point_x = grid_points_x[diff_x]
    grid_point_y = grid_points_y[diff_y]

    ped_pos_rel_x = ped['x'] - grid_point_x
    ped_pos_rel_y = ped['y'] - grid_point_y

    # Position of pedestrian relative to cell center
    ped_pos_rel = np.array([ped_pos_rel_x, ped_pos_rel_y])

    if bool_individual_position:
        density_field = get_gaussian_grid(
            gauss_density_bound, resolution, sigma, ped_pos_rel, ped_radius)
    else:
        density_field = general_density_matrix

    # cutout of density field that is within the camera cutout
    left_bound_field = max(0, radius - diff_x)
    right_bound_field = left_bound_field + right_bound - left_bound

    lower_bound_field = max(0, radius - diff_y)
    upper_bound_field = lower_bound_field + upper_bound - lower_bound

    matrix[lower_bound:upper_bound+1, left_bound:right_bound+1] = \
        matrix[lower_bound:upper_bound+1, left_bound:right_bound+1] + \
            density_field[lower_bound_field:upper_bound_field +
                1, left_bound_field:right_bound_field+1]

    return matrix


def get_density_field(pedestrians, context):
    density_field = context.get('density_field', None)

    size = context.get('size', (0, 0))

    density_approx = np.zeros(size)
    ped_list = np.zeros([len(pedestrians), 2])

    idx = 0
    for _, pedestrian in pedestrians.iterrows():
        density_approx = add_pedestrian_to_field(
            pedestrian,
            density_approx,
            context.get('area', None),
            size,
            context.get('resolution', 1),
            context.get('gauss_bounds', 1),
            context.get('sigma', 0.7),
            context.get('pedestrian_radius', 0.195),
            density_field)

        ped_list[idx, :] = [pedestrian['x'], pedestrian['y']]
        idx += 1

    return density_approx


def juelich_filter_percentiles(timestep, pedestrians, percentiles, density, context):
    flag = False
    if percentiles['A'] == 0.5 and percentiles['B'] == 0.5:
        flag = not(context.get('pFlag', False))
        context['pFlag'] = flag

    return flag, context


def juelich_assign_target(pedestrian):
    return 'B' if pedestrian['x'] < 0 else 'A'


def process_percentiles(timestep, pedestrians, percentiles, density, context):
    data = context.get('percentiles', None)

    # create dataframe if not yet available
    if data is None:
        data = pd.DataFrame(columns=context.get('targets'))

    context['percentiles'] = data.append(percentiles, ignore_index=True)

    return context


def process_pedestrians(timestep, pedestrians, percentiles, density, context):
    data = context.get('pedestrians', None)

    # create dataframe if not yet available
    if data is None:
        data = []

    data.append(pedestrians)

    context['pedestrians'] = data

    return context


def process_peds_per_step(timestep, pedestrians, percentiles, density, context):
    data = context.get('pedestrians', None)

    # create dataframe if not yet available
    if data is None:
        data = pd.DataFrame(columns=['#peds'])

    context['pedestrians'] = data.append(
        {'#peds': len(pedestrians)}, ignore_index=True)

    return context


def process_densities(timestep, pedestrians, percentiles, density, context):
    data = context.get('densities', None)

    # create dataframe if not yet available
    if data is None:
        data = []
        context['densities'] = data

    distribution = [percentiles[key] for key in sorted(percentiles.keys())]
    data.append(np.concatenate(([timestep], density.flatten(), distribution), axis=None))

    return context


def process_experiment(experiment, context):
    processors = context.get('processors', None)
    if processors is None or type(processors) != list:
        processors = []

    filters = context.get('filters', None)
    if filters is None or type(filters) != list:
        filters = []

    pId2Target = context.get('pId2Target', None)

    if callable(pId2Target):
        assign = pId2Target
        pId2Target = dict.fromkeys(
            list(experiment['pedestrianId'].unique()))

        # map pedestrianIds to targets
        for pId, group in experiment.groupby('pedestrianId'):
            pId2Target[pId] = assign(group.iloc[0])

    if pId2Target is None:
        raise ValueError(
            '** No Target Information found for Pedestrians **')

    if not context.get('exact', False):
        context['density_field'] = get_gaussian_grid(
            context.get('gauss_bounds', 0),
            context.get('resolution', 1),
            context.get('sigma', 1),
            [0, 0],
            context.get('pedestrian_radius', 0.195))

    timestep_path = context.get('timesteps', '')
    if timestep_path is '':
        sys.exit(1)

    area = context.get('area')
    resolution = context.get('resolution')

    size = (round(round(area[3] / resolution)), round(round(area[2] / resolution)))
    context['size'] = size
    context['pId2Target'] = pId2Target

    skip = context.get('skip', 1)
    all_timesteps =  list(map(lambda f: os.path.join(timestep_path, f), os.listdir(timestep_path))) # list(experiment.groupby('timeStep'))
    timesteps = all_timesteps[::skip]
    print('using every ', skip, 'timestep, # of used timesteps', len(timesteps), ', # of total timesteps', len(all_timesteps), 'image size', size)
    
    total = len(timesteps)
    timesteps = list(map(lambda a: (a, context), timesteps))

    lock = Lock()

    cores = context.get('number_of_cores', 1)

    with Pool(processes=cores) as p:
        with tqdm(total=total) as pbar:
            for _, result in enumerate(p.imap_unordered(process_timestep, timesteps)):
                pbar.update()

                pedestrians, percentiles, density, timestep = result

                if pedestrians is None:
                    continue

                 # execute filters
                lock.acquire()
                filtered = False
                for _filter in filters:
                    reject, context = _filter(
                        timestep, pedestrians, percentiles, density, context)
                    if reject:
                        #lock.release()
                        filtered = True
                        break
                
                if filtered:
                    lock.release()
                    continue

                # execute processors
                for processor in processors:
                    context = processor(
                        timestep, pedestrians, percentiles, density, context)
                lock.release()

    return context


def process_timestep(args):
    #_, trajectories, context = args
    file, context = args
    
    trajectories = pd.read_csv(file, sep=' ', header=None, names=['timeStep', 'pedestrianId', 'x', 'y'])

    area = context.get('area')
    pId2Target = context.get('pId2Target')

    pedestrians = extract_observation_area(trajectories, area)

    # skip if empty area
    if len(pedestrians) == 0:
        return None, None, None, None

    percentiles = get_target_percentiles(
        pedestrians, pId2Target, context.get('targets', ['A', 'B']))

    density = get_density_field(pedestrians, context)

    values = trajectories.timeStep.values[0]

    del trajectories

    return pedestrians, percentiles, density, values
