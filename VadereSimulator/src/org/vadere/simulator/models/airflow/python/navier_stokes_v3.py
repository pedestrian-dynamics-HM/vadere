import os
os.environ['OPENBLAS_NUM_THREADS'] = '1'
os.environ['MKL_NUM_THREADS'] = '1'
os.environ['NUMEXPR_NUM_THREADS'] = '1'

import faulthandler, sys
faulthandler.enable(file=sys.stderr, all_threads=True)

import time
start_time = time.time()
from skfem import *
from skfem.models.poisson import vector_laplace
from skfem.models.general import divergence
from matplotlib import pyplot as plt
from matplotlib.tri import LinearTriInterpolator, Triangulation
from skfem.helpers import dot, grad, sym_grad, inner, div
from build_mesh import build_mesh
from skfem_helpers import *
from vadere_helpers import extract_attributes
import numpy as np
import argparse
import json
from collections import deque

# Navier Stokes parameters
nu = 1.5e-5  # kinematic viscosity of air at 20°C [m²/s]
num_iterations = 200
tolerance = 1e-8
relaxation = 0.2  # Slightly higher for lower viscosity
use_supg = True  # Enable SUPG stabilization
supg_parameter = 1.0  # SUPG parameter (0.5-1.0 typical)
use_pspg = False
grad_div_gamma = 0
art_visc_alpha = 0
picard_threshold = 0.05  # Switch when error < 5%
diff_u = 1

N_anneal = 100
supg_parameter_start = 1.0
supg_parameter_end   = 0.5   # or 0.1
relaxation_start = 0.1
relaxation_end   = 0.8   # or 0.1


def define_dofs_corrected(basis, mesh, inlet_dict, outlet_dict):
    """
    Define DOFs with proper boundary conditions:
    - Inlets: Fixed velocity (Dirichlet)
    - Outlets: Natural BC (do-nothing) - DOFs left FREE
    - Walls: No-slip (u = 0)
    """
    # Get all boundary facets
    facets_inlet = mesh.facets_satisfying(lambda x: inlet_dict['condition'](x))
    facets_outlet = mesh.facets_satisfying(lambda x: outlet_dict['condition'](x))
    facets_walls = mesh.facets_satisfying(lambda x:
        not inlet_dict['condition'](x) and not outlet_dict['condition'](x))

    # Constrain velocity at inlets and walls only
    D_inlet = basis['u'].get_dofs(facets=facets_inlet)
    D_walls = basis['u'].get_dofs(facets=facets_walls)
    D_velocity = np.concatenate([D_inlet, D_walls])

    # Fix one pressure DOF for uniqueness (not at inlet/outlet)
    # Choose an interior point or wall point
    interior_pressure_dof = basis['p'].get_dofs(lambda x:
        np.isclose(x[0], (x_min + x_max)/2) & np.isclose(x[1], (y_min + y_max)/2))
    if len(interior_pressure_dof) == 0:
        interior_pressure_dof = [0]  # Fallback to first DOF

    D_pressure = basis['u'].N + interior_pressure_dof

    return np.concatenate([D_velocity, D_pressure])

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

        area_threshold = 0.2
        print(area_threshold)

        mesh, inlet_dict, outlet_dict, boundary_dict = build_mesh(inlets, outlets, obstacles, area_threshold, x_min, x_max, y_min, y_max)

        element = {'u': ElementVector(ElementTriP2()), 'p': ElementTriP1()}
        basis = {variable: Basis(mesh, e, intorder=4)
                 for variable, e in element.items()}

        print("Velocity DOFs:", basis['u'].N)
        print("Pressure DOFs:", basis['p'].N)
        print("Total system size:", basis['u'].N * 2 + basis['p'].N)


        # --- Define Bilinear Forms for Navier-Stokes ---
        @BilinearForm
        def stokes_visc(u, v, w):
            """Viscous term with symmetric gradient"""
            return nu * inner(sym_grad(u), sym_grad(v))

        @BilinearForm
        def convection(u, v, w):
            """Convection term, skew-symmetric form"""
            #return dot(dot(w['w'], grad(u)), v)
            #return 0.5*(dot(dot(w['w'], grad(u)), v) - dot(dot(w['w'], grad(v)), u))
            return (dot(dot(w['w'], grad(u)), v) - dot(dot(w['w'], grad(v)), u)) / 2.0



        @BilinearForm
        def pspg_stabilization(p, q, w):
            """
            PSPG (Pressure-Stabilizing Petrov-Galerkin) stabilization
            Stabilizes pressure, especially for equal-order elements
            (Less critical for P2-P1, but helps at low viscosity)
            """
            vel = w['w']
            vel_mag = (dot(vel, vel) + 1e-12)**0.5
            h = w.h

            # Same tau as SUPG
            tau_adv = h / (2.0 * vel_mag + 1e-12)
            tau_diff = h * h / (4.0 * nu + 1e-12)
            tau = 1.0 / (1.0/tau_adv + 1.0/tau_diff)
            tau = supg_parameter * tau

            # PSPG term: tau * ∇p · ∇q
            return tau * dot(grad(p), grad(q))

        @BilinearForm
        def grad_div_stabilization(u, v, w):
            return grad_div_gamma * dot(div(u), div(v))

        @BilinearForm
        def artificial_visc(u, v, w):
            return art_visc_alpha * inner(grad(u), grad(v))

        @BilinearForm
        def convection_jacobian(u, v, w):
            return dot(dot(u, grad(w['w'])), v)

        @BilinearForm
        def mass_matrix(u, v, w):
            return dot(u, v)

        # Solve Stokes flow first for initial guess
        print("Solving initial Stokes flow...")
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

        print(f"Starting Picard iteration with nu={nu:.2e} m²/s")
        print(f"SUPG stabilization: {'ENABLED' if use_supg else 'DISABLED'}")

        M = asm(mass_matrix, basis['u'])
        dt = 0.01  # Pseudo time-step
        M_block = bmat([[M, None],
                        [None, None]], format='csr')

        for iteration in range(num_iterations):
            # Interpolate velocity field for current iteration
            w_field = basis['u'].interpolate(u0.squeeze())

            # Assemble convection term
            A_conv = asm(convection, basis['u'], w=w_field)

            anneal = max(0.0, 1.0 - iteration / float(N_anneal))

            # Add SUPG stabilization if enabled
            if use_supg:
                #supg_parameter = supg_parameter_start * anneal + supg_parameter_end * (1-anneal)

                @BilinearForm
                def supg_stabilization(u, v, w):
                    """
                    SUPG (Streamline-Upwind Petrov-Galerkin) stabilization term
                    Adds artificial diffusion only in streamline direction
                    """
                    # Get velocity field and compute its magnitude
                    vel = w['w']
                    vel_mag = (dot(vel, vel) + 1e-12)**0.5

                    # Element size (approximate using mesh statistics)
                    h = w.h  # Element diameter from basis

                    # Compute intrinsic time scale (tau)
                    # tau = h / (2 * |u|) for advection-dominated flow
                    # Limited by viscous time scale: h²/(4*nu)
                    tau_adv = h / (2.0 * vel_mag + 1e-12)
                    tau_diff = h * h / (4.0 * nu + 1e-12)
                    tau = 1.0 / (1.0/tau_adv + 1.0/tau_diff)
                    tau = supg_parameter * tau

                    # SUPG term: tau * (u·∇u) · (w·∇v)
                    residual = dot(vel, grad(u))
                    test_streamline = dot(vel, grad(v))

                    return tau * dot(residual, test_streamline)

                A_supg = asm(supg_stabilization, basis['u'], w=w_field)
                A = A_stokes + A_conv + A_supg #+ asm(grad_div_stabilization, basis['u']) + asm(artificial_visc, basis['u'])
            else:
                A = A_stokes + A_conv

            #A_transient = A + M / dt
            #rhs = M @ u0 / dt

            # Assemble saddle point system
            #K = bmat([[A, B.T], [B, None]], 'csr')

            if use_pspg:
                # PSPG modifies the pressure block (lower-right block of saddle point system)
                C_pspg = asm(pspg_stabilization, basis['p'], w=w_field)
                # Assemble saddle point system with PSPG
                K = bmat([[A, B.T], [B, -C_pspg]], 'csr')  # Note the negative sign
            else:
                K = bmat([[A, B.T], [B, None]], 'csr')

            #rhs_transient = (M_block / dt).dot(uvp_old)

            # Solve linear system
            uvp = solve(*condense(K, x=uvp, D=D))
            u_new = uvp[:basis['u'].N]
            p_new = uvp[basis['u'].N:]

            #if iteration > 5:
            #    relaxation = 0.5
            #    nu = 1.5e-2
            #    supg_parameter = 0.8

            #if iteration > 10:
            #    relaxation = 0.5
            #    nu = 1.5e-3
            #    supg_parameter = 0.5

            #if iteration > 20:
            #    nu = 1.5e-4
            #    supg_parameter = 0.2
            #    relaxation = 0.5
            #if iteration > 25:
            #    nu = 1.5e-5
            #    relaxation = 0.5

            #if iteration > 30:
            #    relaxation = 0.8

            #if iteration > 40:
            #    relaxation = 1.0
            #relaxation = relaxation_start * anneal + relaxation_end * (1-anneal)

            # Apply relaxation for stability
            u_relaxed = relaxation * u_new + (1.0 - relaxation) * u0

            # Compute convergence metric
            diff_u = np.linalg.norm(u_relaxed.ravel() - u0.ravel()) / (np.linalg.norm(u_relaxed.ravel()) + 1e-12)

            # Update solution
            u0 = u_relaxed
            p0 = p_new

            # Print progress every 10 iterations
            #if (iteration + 1) % 10 == 0:
            print(f"Iteration {iteration+1:3d}: ||Δu||/||u|| = {diff_u:.2e}")

            # Check convergence
            if diff_u < tolerance:
                print(f"\n✓ Converged after {iteration+1} iterations (||Δu||/||u|| = {diff_u:.2e})")
                break

        else:
            print(f"\n⚠ Maximum iterations ({num_iterations}) reached. Final residual: {diff_u:.2e}")

        # Post-process solution
        uv = u0.ravel()
        X, Y, Vx, Vy, velocity_magnitude = postprocess_solution(basis, mesh, uv, grid_size, x_min, x_max, y_min, y_max)

        # Compute Reynolds number for reporting
        if inlet_velocity > 0:
            char_length = max(x_max - x_min, y_max - y_min)
            Re = inlet_velocity * char_length / nu
            print(f"\nFlow characteristics:")
            print(f"  Reynolds number: Re = {Re:.2e}")
            print(f"  Kinematic viscosity: ν = {nu:.2e} m²/s")
            print(f"  Characteristic velocity: U = {inlet_velocity:.2f} m/s")
            print(f"  Characteristic length: L = {char_length:.2f} m")

        # Save results
        np.savetxt(scenario_file_path + '_' + scenario_hash + '_Vx.txt', Vx,
                   header=f'{Vx.shape[0]}_{Vx.shape[1]}_{parameter_string}')
        np.savetxt(scenario_file_path + '_' + scenario_hash + '_Vy.txt', Vy,
                   header=f'{Vy.shape[0]}_{Vy.shape[1]}_{parameter_string}')

        # Visualization
        showPlot = True
        if showPlot:
            data_ratio = (x_max - x_min) / (y_max - y_min)
            fig_width = 20
            fig_height = fig_width / data_ratio

            fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(fig_width, fig_height/2))

            # Plot 1: Velocity magnitude with streamlines
            contour = ax1.contourf(X, Y, velocity_magnitude, levels=50, cmap='viridis')
            plt.colorbar(contour, ax=ax1, label='Velocity magnitude (m/s)')

            # Streamlines instead of quiver for better visualization
            ax1.streamplot(X, Y, Vx, Vy, color='white', linewidth=0.5,
                          density=2, arrowsize=1, arrowstyle='->')

            ax1.set_title(f'Velocity Field (Re = {Re:.1e}, ν = {nu:.1e} m²/s)')
            for obs in obstacles:
                x_coords = [vertex[0] for vertex in obs]
                y_coords = [vertex[1] for vertex in obs]
                ax1.fill(x_coords, y_coords, color='grey', alpha=1.0)
            ax1.set_xlabel("x (m)")
            ax1.set_ylabel("y (m)")
            ax1.axis("equal")

            # Plot 2: Velocity vectors (quiver)
            contour2 = ax2.contourf(X, Y, velocity_magnitude, levels=50, cmap='viridis')
            plt.colorbar(contour2, ax=ax2, label='Velocity magnitude (m/s)')
            ax2.quiver(X, Y, Vx, Vy, color='white', scale=2, width=0.004)
            ax2.set_title('Velocity Vectors')
            for obs in obstacles:
                x_coords = [vertex[0] for vertex in obs]
                y_coords = [vertex[1] for vertex in obs]
                ax2.fill(x_coords, y_coords, color='grey', alpha=1.0)
            ax2.set_xlabel("x (m)")
            ax2.set_ylabel("y (m)")
            ax2.axis("equal")

            plt.tight_layout()
            plt.show()

        elapsed_time = time.time() - start_time
        print(f"\nTotal computation time: {elapsed_time:.2f} seconds")