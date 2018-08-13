import numpy as np
from ..tests.density_plot_tests import read_density
from ..io.density_writer import write_matrix_to_file

# before processing trajectories file check attribute's column position
INDEX_TIME_STEP = 0
INDEX_PED_ID = 1
INDEX_POS_X = 2
INDEX_POS_Y = 3
INDEX_TARGET_ID = 4

RADIUS_PED = 0.195

# ----------------------------------------------------------------------
# generates a vector of matrices each containing the density data for one time step
# @param data : trajectories
# @param area : position (x,y) and size (width, height) of measurement area [m]
# @param resolution : resolution of the density image [m]
# @area ((cp_x,cp_y)(width,height)) corner point of the measurement field referencing to c.sys. of complete scenario
#       and area of the measurement field
# @param gauss_density_bounds : bounds for the gaussian density
# @param current_dist : matrix with the current distribution of pedestrians on the targets per time step
# @param file : outputfile

def calculate_density_timeseries(data, obs_area, resolution, gauss_density_bounds, sigma, current_dist, file):

    meas_area_width = obs_area[2]
    meas_area_height = obs_area[3]

    size = (int(meas_area_height/ resolution), int(meas_area_width / resolution))
    
    density_field = get_gaussian_grid(gauss_density_bounds[0], gauss_density_bounds[1], resolution, sigma)
    #density_field = get_vadere_gaussian_grid()

    matrix = np.zeros(size)
    #print("Size observation area:", size)
    index = 0
    for timestep in data:
        for ped in timestep:
            matrix = add_pedestrian_density(ped, matrix, density_field, obs_area, resolution)


        write_matrix_to_file(matrix, current_dist[index],file)
        matrix = np.zeros(size)  # new matrix
        index += 1

def get_gaussian_grid(gauss_bound_start, gauss_bound_stop, resolution, sigma):
    x = np.arange(-gauss_bound_start, gauss_bound_stop + resolution, resolution) # gives gauss_bound_start:resolution:gauss_bound_stop

    xx, yy = np.meshgrid(x, x, sparse=False) # Make all grid points (based on resolution) in [gauss_bound_start, gauss_bound_stop] (2-dim-array)
    grid = (xx ** 2 + yy ** 2) # distance to the origin of the observation area / ((stop ** 2) * 2)
    ped_radius = np.zeros([1,1])
    gauss = np.vectorize(gaussian_pdf)

    gauss_grid = gauss(ped_radius, sigma, grid)
    #print("Shape Gaussian grid: ", gauss_grid.shape)
    return gauss_grid

def gaussian_pdf(ped_rad, sigma, x):
    # S_p = g_p^2 * sqrt(3) /2
    zaehler = ((RADIUS_PED*2)**2)*np.sqrt(3)/2  # S_p
    nenner = (2*np.pi*sigma**2)

    normalization_factor = np.sqrt(zaehler/nenner)
    individual_density = normalization_factor * np.exp(-x / (2 * sigma ** 2))

    return normalization_factor*individual_density

    # ----------------------------------------------------------------------------------------------------------------------
    # density_field matrix with density values for ped calculated with static Gaussian density field
def add_pedestrian_density(ped, matrix, density_field, area, resolution):
    # calculate the density for one ped and add to matrix

    size = density_field.shape
    radius = int(size[0] / 2) # equal to bounds[1]*resolution
    origin_x = area[0]
    origin_y = area[1]
    width = int(area[2] / resolution)
    height = int(area[3] / resolution)

    # necessary to map to center of the grid instead of the edge!
    offset = resolution/2

    # find grid cell of pedestrian in observation area
    diff_x = int(
        np.round((ped[INDEX_POS_X] - origin_x - offset) / resolution, 0))  # TODO do not round -> divide pedestrian density %
    diff_y = int(
        np.round((ped[INDEX_POS_Y] - origin_y - offset) / resolution, 0))  # TODO do not round -> divide pedestrian density %

    # area in which the pedestrian has an influence on the pedestrian density
    left_bound = int(max(0, diff_x - radius))
    right_bound = int(min(diff_x + radius, width - 1))
    upper_bound = int(min(diff_y + radius, height - 1))
    lower_bound = int(max(0, diff_y - radius))


    # j > 0: gaussian field (y-direction) is limited (from origin) by measurement area
    j = max(0, radius - diff_y)

    # for each position in the measurement area (x,y)
    for y in range(lower_bound, upper_bound + 1):

        # i > 0: gaussian field (x-direction) is limited (from origin) by measurement area
        i = max(0, radius - diff_x)
        for x in range(left_bound, right_bound + 1):
            # choose the right position within the density_field (i,j) to add to current matrix value
            # origin in matrix is at [height-1][0]
            
            matrix[height - 1 - y][x] += density_field[size[1] - 1 - j][i]
            #matrix[x][height - 1 - y] += density_field[i][size[1] - 1 - j]
            
            i += 1

        j += 1

    return matrix

####################################### OLD functions


def get_vadere_gaussian_grid():
    data = read_density("vadere_gaussian.csv")
    return np.array(data)
