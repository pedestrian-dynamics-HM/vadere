import numpy as np
from numpy import linalg as LA
from tests.density_plot_tests import read_density
import utils.writer.density as writer
import matplotlib.pyplot as plt
import time

# before processing trajectories file check attribute's column position
INDEX_TIME_STEP = 0
INDEX_PED_ID = 1
INDEX_POS_X = 2
INDEX_POS_Y = 3
INDEX_TARGET_ID = 4

RADIUS_PED = 0.195

def calculate_density_timeseries_formula(data, obs_area, resolution, sigma, current_dist, file):

    # find positions to calculate density
    width = obs_area[2]
    height = obs_area[3]
    nodes_x = np.linspace(obs_area[0] + resolution/2, obs_area[0]-resolution/2 + width ,width/resolution )
    nodes_y = np.linspace(obs_area[1] + resolution/2, obs_area[1]-resolution/2 + height,height/resolution)

    density = np.zeros([nodes_y.__len__(), nodes_x.__len__()])


    ## all ped positions


    i_timestep = 0
    for timestep in data:
        ped_position = np.zeros([len(timestep),2])
        i_ped = 0
        for ped in timestep:
            ped_position[i_ped,:] = [ped[2], ped[3]]
            i_ped = i_ped +1


        ## fill density matrix

        #t1 = clock()

        for i_node_x in range(0,nodes_x.__len__()):
            for i_node_y in range(0,nodes_y.__len__()):
                position = np.array([ nodes_x[i_node_x], nodes_y[i_node_y]])
                density[i_node_y, i_node_x] = calculate_density_position(position, ped_position, sigma)
        #t2 = clock()

        # print("Density calculation: %f s" %(t2-t1))

        # Write to file
        writer.write_matrix_to_file(density, current_dist[i_timestep],file)
        i_timestep += 1


    return None

# implementation of density formula
def calculate_density_position(position, ped_positions, sigma):
    D_p_z = 0
    for i in range(0,len(ped_positions)):
        f_xi_z = 1/(2*np.pi*np.square(sigma))*np.exp(-1/(2*np.square(sigma)) *np.square(LA.norm(ped_positions[i,:] - position)))
        D_p_z = D_p_z + f_xi_z

    S_p = np.square(RADIUS_PED*2) * np.sqrt(3)/ 2
    D_p_z = D_p_z * S_p


    return D_p_z


# ----------------------------------------------------------------------
# generates a vector of matrices each containing the density data for one time step
# @param data : trajectories
# @param area : position (x,y) and size (width, height) of measurement area [m]
# @param resolution : resolution of the density image [m]
# @area ((cp_x,cp_y)(width,height)) corner point of the measurement field referencing to c.sys. of complete scenario
#       and area of the measurement field
# @param gauss_density_bound : bounds for the gaussian density
# @param current_dist : matrix with the current distribution of pedestrians on the targets per time step
# @param file : outputfile
def calculate_density_timeseries(data, obs_area, resolution, gauss_density_bound, sigma, current_dist, file, bool_exact_position):

    if not(bool_exact_position):
        density_field = get_gaussian_grid(gauss_density_bound, resolution, sigma, [0, 0])

    width = obs_area[2]
    height = obs_area[3]

    size = (int(height/ resolution), int(width / resolution))
    #density_field = get_vadere_gaussian_grid()

    index = 0
    for timestep in data:

        ## Density calculation (bounds)
        density_approx = np.zeros(size)
        ped_list = np.zeros([len(timestep),2])
        i_ped = 0
        for ped in timestep:
            # relative to origin of camera cutout
            # ped_pos_rel = np.array([ped[INDEX_POS_X], ped[INDEX_POS_Y]]) - np.array([obs_area[0], obs_area[1]])

            if bool_exact_position:
                density_approx = add_pedestrian_density(ped, density_approx, obs_area, resolution, gauss_density_bound, sigma, None)
            else:
                density_approx = add_pedestrian_density(ped, density_approx, obs_area, resolution, gauss_density_bound, sigma, density_field)


            ped_list[i_ped,:] = [ped[2], ped[3]]
            i_ped = i_ped+1

        # Write to file
        writer.write_matrix_to_file(density_approx, current_dist[index],file)
        index += 1


def calculate_density_timeseries_both_methods(data, obs_area, resolution, gauss_density_bound, sigma, current_dist, file):

    width = obs_area[2]
    height = obs_area[3]

    size = (int(height/ resolution), int(width / resolution))
    #density_field = get_vadere_gaussian_grid()

    print("Size observation area:", size)
    index = 0
    for timestep in data:

        ## First implementation (bounds)
        density_approx = np.zeros(size)
        ped_list = np.zeros([len(timestep),2])
        i_ped = 0
        t1 = time.clock()
        for ped in timestep:
            # relative to origin of camera cutout
            # ped_pos_rel = np.array([ped[INDEX_POS_X], ped[INDEX_POS_Y]]) - np.array([obs_area[0], obs_area[1]])
            density_approx = add_pedestrian_density(ped, density_approx, obs_area, resolution, gauss_density_bound, sigma)
            ped_list[i_ped,:] = [ped[2], ped[3]]
            i_ped = i_ped+1


        t2 = time.clock()

        print("Density matrix calculation #1 (bounds ): %f s " % (t2-t1))

        ## Second implementation (formula) - to compare

        offset = resolution/2
        nodes_x = np.linspace(obs_area[0] + offset ,obs_area[0] - offset + width, width / resolution)
        nodes_y = np.linspace(obs_area[1] + offset, obs_area[1] - offset + height, height / resolution)

        density_formula = np.zeros([nodes_y.__len__(), nodes_x.__len__()])

        t1 = time.clock()

        for i_node_x in range(0, nodes_x.__len__()):
            for i_node_y in range(0, nodes_y.__len__()):
                position = np.array([nodes_x[i_node_x], nodes_y[i_node_y]])
                density_formula[i_node_y, i_node_x] = calculate_density_position(position, ped_list, sigma)
        t2 = time.clock()

        print("Density matrix calculation #2 (formula): %f s" % (t2 - t1))

        print("Diff between methods: %.2e " % LA.norm(density_approx - density_formula))
        assert(LA.norm(density_approx - density_formula) < 1e-10)



        ## Plot results
        fig = plt.figure()
        plt.subplot(121)
        plt.imshow(np.flipud(density_approx), extent =[obs_area[0], obs_area[0]+obs_area[2] ,obs_area[1], obs_area[1] + obs_area[3]])
        # plt.hold
        plt.plot(ped_list[:,0], ped_list[:,1],'kx')
        plt.title('Bounds')
        plt.colorbar()

        plt.subplot(122)
        plt.imshow(np.flipud(density_formula), extent =[obs_area[0], obs_area[0]+obs_area[2] ,obs_area[1], obs_area[1] + obs_area[3]])
        # plt.hold
        plt.plot(ped_list[:,0], ped_list[:,1],'kx')
        plt.title('Formula [seitz-2012]')
        plt.colorbar()

        # Write to file
        writer.write_matrix_to_file(density_approx, current_dist[index],file)
        index += 1

def get_gaussian_grid(gauss_bound, resolution, sigma, ped_pos):
    x = np.arange(-gauss_bound, gauss_bound + resolution, resolution) # gives gauss_bound_start:resolution:gauss_bound_stop

    xx, yy = np.meshgrid(x, x, sparse=False) # Make all grid points (based on resolution) in [gauss_bound_start, gauss_bound_stop] (2-dim-array)
    grid_regular = (xx**2 + yy ** 2)
    grid = np.square(xx-ped_pos[0]) + np.square(yy-ped_pos[1]) # distance to the origin of the observation area

    gauss = np.vectorize(gaussian_pdf)

    gauss_grid = gauss(sigma, grid)
    return gauss_grid

def gaussian_pdf(sigma, x ):
    # S_p = g_p^2 * sqrt(3) /2
    zaehler = ((RADIUS_PED*2)**2)*np.sqrt(3)/2  # S_p
    nenner = (2*np.pi*sigma**2)

    normalization_factor = np.sqrt(zaehler/nenner)
    individual_density = normalization_factor * np.exp(-x / (2 * np.square(sigma)))

    return normalization_factor*individual_density

    # ----------------------------------------------------------------------------------------------------------------------
    # density_field matrix with density values for ped calculated with static Gaussian density field
def add_pedestrian_density(ped, matrix, area, resolution, gauss_density_bound, sigma, general_density_matrix):

    if general_density_matrix is None: # not None
        bool_individual_position = True
    else:
        bool_individual_position = False

    # calculate the density for one ped and add to matrix
    size = int(gauss_density_bound * 2 / resolution +1)
    radius = int(size / 2) # equal to gauss_density_bound/resolution
    origin_x = area[0]
    origin_y = area[1]
    width = int(area[2] / resolution)
    height = int(area[3] / resolution)

    # necessary to map to center of the grid instead of the edge!
    offset = resolution/2

    # find grid cell of pedestrian in observation area
    diff_x = int(
        np.round((ped[INDEX_POS_X] - origin_x - offset) / resolution, 0))
    diff_y = int(
        np.round((ped[INDEX_POS_Y] - origin_y - offset) / resolution, 0))

    # area in which the pedestrian has an influence on the pedestrian density
    left_bound = int(max(0, diff_x - radius))
    right_bound = int(min(diff_x + radius, width - 1))
    upper_bound = int(min(diff_y + radius, height - 1))
    lower_bound = int(max(0, diff_y - radius))

    ## create density_field
    # position of pedestrian relative to center of grid cell
    grid_points_x = np.arange(origin_x+offset, origin_x + area[2] + resolution, resolution) # gives gauss_bound_start:resolution:gauss_bound_stop
    grid_points_y = np.arange(origin_y+offset, origin_y + area[3] + resolution, resolution)

    # center of cell in which the pedestrian is located
    grid_point_x = grid_points_x[diff_x]
    grid_point_y = grid_points_y[diff_y]

    ped_pos_rel_x = ped[INDEX_POS_X] - grid_point_x
    ped_pos_rel_y = ped[INDEX_POS_Y] - grid_point_y

    # Position of pedestrian relative to cell center
    ped_pos_rel = np.array([ped_pos_rel_x, ped_pos_rel_y])

    if bool_individual_position:
        density_field = get_gaussian_grid(gauss_density_bound, resolution, sigma, ped_pos_rel)
    else:
        density_field = general_density_matrix

    # cutout of density field that is within the camera cutout
    left_bound_field = max(0, radius - diff_x)
    right_bound_field = left_bound_field + right_bound - left_bound

    lower_bound_field = max(0, radius - diff_y)
    upper_bound_field = lower_bound_field + upper_bound - lower_bound

    matrix[lower_bound:upper_bound+1, left_bound:right_bound+1] = matrix[lower_bound:upper_bound+1, left_bound:right_bound+1] + \
    density_field[lower_bound_field:upper_bound_field+1, left_bound_field:right_bound_field+1]



    """ # for each position in the measurement area (x,y)
     # j > 0: gaussian field (y-direction) is limited (from origin) by measurement area
     j_y = max(0, radius - diff_y)
     for y in range(lower_bound, upper_bound + 1):
         # i > 0: gaussian field (x-direction) is limited (from origin) by measurement area
         i_x = max(0, radius - diff_x)
         for x in range(left_bound, right_bound + 1):
             # choose the right position within the density_field (i,j) to add to current matrix value
             # origin in matrix is at [height-1][0]
             matrix[y][x] += density_field[i_x][j_y]
             i_x += 1
         j_y += 1 """


    ## Plot density matrix and positions
    # plt.figure()
    # plt.imshow(matrix, extent = [area[0], area[0]+area[2] ,area[1], area[1] + area[3]])
    # plt.colorbar()
    # plt.axis('equal')
    # plt.hold
    # plt.plot(ped[INDEX_POS_X], ped[INDEX_POS_Y],'rx')
    # plt.show()

    return matrix

####################################### OLD functions


def get_vadere_gaussian_grid():
    data = read_density("vadere_gaussian.csv")
    return np.array(data)
