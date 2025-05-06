import time 
start_time = time.time()
from skfem import *
from skfem.models.poisson import vector_laplace
from skfem.models.general import divergence
from matplotlib import pyplot as plt
from matplotlib.tri import LinearTriInterpolator, Triangulation
from skfem.helpers import dot, grad, sym_grad, inner
from build_mesh import build_mesh
from skfem_helpers import *
from vadere_helpers import extract_attributes
import numpy as np
import argparse
import json


area_threshold = 0.1
x_min, x_max = 0., 10.
y_min, y_max = 0., 8.
inlets = [{"id": 0, "side": "left", "coords": [5,7]}]#,
          #{"id": 1, "side": "bottom", "coords": [5,7]}]
inlet_velocity = 0.3
outlets = [{"id": 0, "side": "right", "coords": [2,4]}]#,
           #{"id": 1, "side": "left", "coords": [2,4]}]
obstacles = [((4, 5), (5, 6)), ((1, 2), (2, 4))]
grid_size = 0.2

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('scenario')
    parser.add_argument('hash')
    args = parser.parse_args()
    config = vars(args)

    scenario_file_path = config['scenario']
    scenario_hash = config['hash']

    parameter_string = ""

    with open(scenario_file_path) as file:
        data = json.load(file)
        topography = data['scenario']['topography']
        attributes_model = data['scenario']['attributesModel']['org.vadere.state.attributes.models.airflow.AttributesAirFlowModel']
        grid_size, area_threshold, x_min, x_max, y_min, y_max, inlet_velocity, inlets, outlets, obstacles, parameter_string = extract_attributes(topography, attributes_model, parameter_string)

        mesh, inlet_dict, outlet_dict, boundary_dict = build_mesh(inlets, outlets, obstacles, area_threshold, x_min, x_max, y_min, y_max)
        element = {'u': ElementVector(ElementTriP2()), 'p': ElementTriP1()}
        basis = {variable: Basis(mesh, e, intorder=4)
                 for variable, e in element.items()}

        D = define_dofs(basis, mesh, inlet_dict, outlet_dict)
        A_stokes = asm(vector_laplace, basis['u'])
        B = -asm(divergence, basis['u'], basis['p'])
        K_stokes = bmat([[A_stokes, B.T], [B, None]], 'csr')

        inlet_basis = define_inlet_basis(basis, mesh, inlets, inlet_velocity)
        outlet_basis = basis['p'].zeros()

        uvp = np.hstack((
            inlet_basis,
            outlet_basis,
        ))
        uvp_stokes = solve(*condense(K_stokes, x=uvp, D=D))
        u0, p0 = np.split(uvp_stokes, K_stokes.blocks)
        uv = u0.ravel()

        X, Y, Vx, Vy, velocity_magnitude = postprocess_solution(basis, mesh, uv, grid_size, x_min, x_max, y_min, y_max)

        np.savetxt(scenario_file_path + '_' + scenario_hash + '_Vx.txt', Vx, header=f'{Vx.shape[0]}_{Vx.shape[1]}_{parameter_string}')
        np.savetxt(scenario_file_path + '_' + scenario_hash + '_Vy.txt', Vy, header=f'{Vy.shape[0]}_{Vy.shape[1]}_{parameter_string}')

        showPlot = True
        if showPlot:
            plt.figure(figsize=(12, 8))
            contour = plt.contourf(X, Y, velocity_magnitude, levels=50, cmap='viridis')
            plt.colorbar(contour, label='Velocity magnitude (m/s)')
            plt.quiver(X, Y, Vx, Vy, color='white', width=0.004)
            plt.title('Velocity field')
            #for obs in obstacles:
            #    (x1, y1), (x2, y2) = obs; plt.fill([x1, x2, x2, x1, x1], [y1, y1, y2, y2, y1], color='grey', alpha=0.7)
            plt.axis("equal")
            plt.xlabel("x (m)")
            plt.ylabel("y (m)")
            plt.show()

