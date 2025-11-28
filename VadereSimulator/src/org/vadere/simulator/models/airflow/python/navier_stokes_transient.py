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
from skfem.helpers import dot, grad, sym_grad, inner
from build_mesh import build_mesh
from skfem_helpers import *
from vadere_helpers import extract_attributes
from plot_results import plot_results
import numpy as np
import argparse
import json

# Navier Stokes parameters
nu = 1.5e-5  # kinematic viscosity of air at 20°C [m²/s]
num_iterations = 200
tolerance = 1e-5
relaxation = 0.2
use_supg = False
supg_parameter = 0.1
use_pspg = False
dt = 0.01             # Time step size (needs to be small enough! start small)
final_time = 5.0      # Total simulation time
current_time = 0.0
max_steps = int(final_time / dt)

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

        area_threshold = 0.8

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
            return 0.5 * (dot(dot(w['w'], grad(u)), v) - dot(dot(w['w'], grad(v)), u))

        @BilinearForm
        def mass(u, v, w):
            """Vector mass matrix: (u, v) = dot(u, v)"""
            return dot(u, v)

        @BilinearForm
        def supg_stabilization(u, v, w):
            """
            SUPG (Streamline-Upwind Petrov-Galerkin) stabilization term
            Adds artificial diffusion only in streamline direction
            """
            vel = w['w']
            vel_mag = (dot(vel, vel) + 1e-12)**0.5
            h = w.h
            tau_adv = h / (2.0 * vel_mag + 1e-12)
            tau_diff = h * h / (4.0 * nu + 1e-12)
            tau = 1.0 / (1.0/tau_adv + 1.0/tau_diff)
            tau = supg_parameter * tau

            residual = dot(vel, grad(u))
            test_streamline = dot(vel, grad(v))
            return tau * dot(residual, test_streamline)

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

            tau_adv = h / (2.0 * vel_mag + 1e-12)
            tau_diff = h * h / (4.0 * nu + 1e-12)
            tau = 1.0 / (1.0/tau_adv + 1.0/tau_diff)
            tau = supg_parameter * tau

            return tau * dot(grad(p), grad(q))

        # Solve Stokes flow first for initial guess
        print("Solving initial Stokes flow...")
        D = define_dofs(basis, mesh, inlet_dict, outlet_dict)
        A_stokes = asm(stokes_visc, basis['u'])
        B = -asm(divergence, basis['u'], basis['p'])
        K_stokes = bmat([[A_stokes, B.T], [B, None]], 'csr')

        inlet_basis = define_inlet_basis(basis, mesh, inlets, inlet_velocity)
        uvp_initial = np.hstack((inlet_basis, basis['p'].zeros()))
        uvp = solve(*condense(K_stokes, x=uvp_initial, D=D))

        u_n = uvp[:basis['u'].N] # Previous time step velocity
        p_n = uvp[basis['u'].N:] # Previous time step pressure

        print(f"Starting Picard iteration with nu={nu:.2e} m²/s")
        print(f"SUPG stabilization: {'ENABLED' if use_supg else 'DISABLED'}")

        u_sum = np.zeros_like(u_n)
        start_averaging_at = 1950  # Let the flow develop first
        count_avg = 0

        M = asm(mass, basis['u'])
        A_visc = asm(stokes_visc, basis['u'])

        #target_nu = 1.5e-5
        #nu = 1.0e-2
        #nu_decay = 0.98

        for step in range(1, max_steps + 1):
            current_time += dt

            # 3. Linearization
            # We use the velocity from the previous step (u_n) to linearize convection
            # This is a standard "Semi-Implicit" approach.
            w_field = basis['u'].interpolate(u_n)

            # 4. Assemble Time-Dependent System
            # Convection A(u_n)
            A_conv = asm(convection, basis['u'], w=w_field)

            # SUPG Stabilization (Recommended for Transient too)
            if use_supg:
                A_supg = asm(supg_stabilization, basis['u'], w=w_field)
                A_total = A_visc + A_conv + A_supg
            else:
                A_total = A_visc + A_conv

            # LHS Matrix: (1/dt * M + A)
            # We scale M by 1/dt.
            lhs_u = (1.0 / dt) * M + A_total

            # System Matrix
            if use_pspg:
                 C_pspg = asm(pspg_stabilization, basis['p'], w=w_field)
                 K = bmat([[lhs_u, B.T], [B, -C_pspg]], 'csr')
            else:
                 K = bmat([[lhs_u, B.T], [B, None]], 'csr')

            # RHS Vector: (1/dt * M * u_n)
            rhs_u = (1.0 / dt) * M @ u_n
            rhs_p = np.zeros(basis['p'].N)
            rhs = np.concatenate([rhs_u, rhs_p])

            # 5. Solve
            # We use the previous uvp as the boundary condition "template"
            # (assuming constant inlet velocity)
            u_old = u_n
            uvp_next = solve(*condense(K, rhs, x=uvp, D=D))

            u_n = uvp_next[:basis['u'].N]
            p_n = uvp_next[basis['u'].N:]
            uvp = uvp_next # Update for next BC template

            # 6. Calculate Instantaneous Residual (Optional, to check stability)
            velocity_mag = np.linalg.norm(u_n)
            if step % 10 == 0:
                print(f"Time {current_time:.3f}s (Step {step}): Max Vel ~ {np.max(np.abs(u_n)):.2f}")

            diff_u = np.linalg.norm(u_n.ravel() - u_old.ravel()) / (np.linalg.norm(u_n.ravel()) + 1e-12)
            print(f"Iteration {step+1:3d}: ||Δu||/||u|| = {diff_u:.2e}")

            # 7. Accumulate Average
            if current_time > start_averaging_at:
                u_sum += u_n
                count_avg += 1

        # --- Post Loop ---
        if count_avg > 0:
            print(f"Averaging over {count_avg} time steps.")
            u_final = u_sum / count_avg
        else:
            print("Warning: Simulation ended before averaging started.")
            u_final = u_n

        # Post-process solution
        uv = u_final.ravel()
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

        plot_results(mesh, X, Y, Vx, Vy, velocity_magnitude, obstacles)
        elapsed_time = time.time() - start_time
        print(f"\nTotal computation time: {elapsed_time:.2f} seconds")