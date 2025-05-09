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

# Navier Stokes parameters
nu = 0.01 # viscosity
num_iterations = 50
tolerance = 1e-3
relaxation = 0.5


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
        # SET MANUAL LIMITS
        #x_min = 7.2
        #x_max = 25.8

        mesh, inlet_dict, outlet_dict, boundary_dict = build_mesh(inlets, outlets, obstacles, area_threshold, x_min, x_max, y_min, y_max)

        plt.figure(figsize=(10, 8))
        ax = plt.gca()
        ax.triplot(mesh.p[0, :], mesh.p[1, :], mesh.t.T, linewidth=0.5, color='blue')
        ax.set_title("FEM Mesh")
        ax.set_xlabel("x (m)")
        ax.set_ylabel("y (m)")
        ax.axis('equal')
        plt.show()

        element = {'u': ElementVector(ElementTriP2()), 'p': ElementTriP1()}
        basis = {variable: Basis(mesh, e, intorder=4)
                 for variable, e in element.items()}

        # --- Define Bilinear Forms for Navier-Stokes ---
        @BilinearForm
        def stokes_visc(u, v, w):
            """Viscous term"""
            return nu * inner(sym_grad(u), sym_grad(v))

        @BilinearForm
        def convection(u, v, w):
            """Convection term, skew-symmetric form for more stability"""
            #return dot(dot(w['w'], grad(u)), v)
            return dot(dot(w['w'], grad(u)), v) - dot(dot(w['w'], grad(v)), u) / 2.0


        # Solve Stokes flow first for initial guess
        D = define_dofs(basis, mesh, inlet_dict, outlet_dict)
        A_stokes = asm(stokes_visc, basis['u'])
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
        uvp_fixed_bc = uvp

        for iteration in range(num_iterations):
            w_field = basis['u'].interpolate(u0.squeeze())
            A_conv = asm(convection, basis['u'], w=w_field)
            A = A_stokes + A_conv
            K = bmat([[A, B.T], [B, None]], 'csr')
            uvp = solve(*condense(K, x=uvp_fixed_bc, D=D))
            u_new = uvp[:basis['u'].N]
            p_new = uvp[basis['u'].N:]

            # Apply relaxation to velocity update for stability
            u_relaxed = relaxation * u_new + (1.0 - relaxation) * u0
            diff_u = np.linalg.norm(u_relaxed.ravel() - u0.ravel()) / (np.linalg.norm(u_relaxed.ravel()) + 1e-12)
            u0 = u_relaxed
            p0 = p_new

            #print(f"Iteration: {iteration}, difference u: {diff_u:.2f}")
            if diff_u < tolerance:
                 print(f"\nConverged after {iteration+1} iterations.")
                 break

        uv = u0.ravel()

        X, Y, Vx, Vy, velocity_magnitude = postprocess_solution(basis, mesh, uv, grid_size, x_min, x_max, y_min, y_max)

        np.savetxt(scenario_file_path + '_' + scenario_hash + '_Vx.txt', Vx, header=f'{Vx.shape[0]}_{Vx.shape[1]}_{parameter_string}')
        np.savetxt(scenario_file_path + '_' + scenario_hash + '_Vy.txt', Vy, header=f'{Vy.shape[0]}_{Vy.shape[1]}_{parameter_string}')


        showPlot = True
        if showPlot:
            data_ratio = (x_max - x_min) / (y_max - y_min)
            fig_width = 20
            fig_height = fig_width / data_ratio
            plt.figure(figsize=(fig_width, fig_height))
            contour = plt.contourf(X, Y, velocity_magnitude, levels=50, cmap='viridis')
            plt.colorbar(contour, label='Velocity magnitude (m/s)')
            plt.quiver(X, Y, Vx, Vy, color='white', scale=2, width=0.004)
            plt.title('Velocity field')
            for obs in obstacles:
                x_coords = [vertex[0] for vertex in obs]
                y_coords = [vertex[1] for vertex in obs]
                plt.fill(x_coords, y_coords, color='grey', alpha=1.0)
            plt.xlabel("x (m)")
            plt.ylabel("y (m)")
            plt.axis("equal")
            plt.show()

