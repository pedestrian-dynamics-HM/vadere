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
from plot_results import plot_results
from skfem_helpers import *
from vadere_helpers import extract_attributes
import numpy as np
import argparse
import json

# Navier Stokes parameters
nu = 1.5e-5  # kinematic viscosity of air at 20°C [m²/s]
num_iterations = 20
tolerance = 1e-8
relaxation = 0.3  # Slightly higher for lower viscosity
use_supg = True  # Enable SUPG stabilization
supg_parameter = 1.0  # SUPG parameter (0.5-1.0 typical)
use_pspg = False
dt = 0.01

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

        area_threshold = 0.1

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
        def mass_matrix(u, v, w):
            return dot(u, v) / dt


        @BilinearForm
        def stokes_turbulent(u, v, w):
            # Calculate Strain Rate Tensor magnitude |S|
            # S = 0.5 * (grad(u) + grad(u).T)
            # simplified for 2D: approx proportional to grad(u) magnitude

            # Get gradient of previous solution
            du = grad(w['w'])
            shear_rate = (inner(du, du) + 1e-12)**0.5

            # Smagorinsky-style Turbulent Viscosity
            # Cs is a constant (usually 0.1 to 0.2)
            # h is element size
            Cs = 0.15
            h = w.h
            nu_turbulent = (Cs * h)**2 * shear_rate

            # Total effective viscosity
            nu_eff = nu + nu_turbulent

            # Standard Stokes term with variable viscosity
            return nu_eff * inner(sym_grad(u), sym_grad(v))

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

        # Solve Stokes flow first for initial guess
        print("Solving initial Stokes flow...")
        D = define_dofs(basis, mesh, inlet_dict, outlet_dict)
        A_stokes = asm(stokes_visc, basis['u'])
        B = -asm(divergence, basis['u'], basis['p'])
        K_stokes = bmat([[A_stokes, B.T], [B, None]], 'csr')

        current_time = 0.0

        inlet_basis = define_inlet_basis(basis, mesh, inlets, inlet_velocity)
        outlet_basis = basis['p'].zeros()

        uvp = np.hstack((
            inlet_basis,
            outlet_basis,
        ))
        uvp_stokes = solve(*condense(K_stokes, x=uvp, D=D))
        uvp_stokes = np.zeros(len(uvp_stokes), dtype=float)
        u0, p0 = np.split(uvp_stokes, K_stokes.blocks)
        uvp_fixed_bc = uvp

        u_prev = u0

        M = asm(mass_matrix, basis['u'])
        #noise_level = 0.01 * np.max(np.abs(u0))
        #noise = np.random.normal(0, noise_level, u_prev.shape)
        #u_prev = u_prev + noise

        print(f"Starting Picard iteration with nu={nu:.2e} m²/s")
        print(f"SUPG stabilization: {'ENABLED' if use_supg else 'DISABLED'}")

        for iteration in range(num_iterations):
            # 1. Update System Matrices
            w_field = basis['u'].interpolate(u_prev.squeeze())

            A_conv = asm(convection, basis['u'], w=w_field)

            #A_stokes = asm(stokes_turbulent, basis['u'], w=w_field)

            if use_supg:
                A_supg = asm(supg_stabilization, basis['u'], w=w_field)
                A_int = A_stokes + A_conv + A_supg
            else:
                A_int = A_stokes + A_conv

            # Add Mass Matrix: (1/dt) * M
            A = A_int + (1/dt) * M

            # 2. Build RHS
            rhs_velocity = (1/dt) * M @ u_prev
            rhs_pressure = np.zeros(basis['p'].N)
            b_full = np.concatenate([rhs_velocity, rhs_pressure])

            # 3. Solve
            if use_pspg:
                C_pspg = asm(pspg_stabilization, basis['p'], w=w_field)
                K = bmat([[A, B.T], [B, -C_pspg]], 'csr')
            else:
                K = bmat([[A, B.T], [B, None]], 'csr')

            uvp = solve(*condense(K, b=b_full, x=uvp_fixed_bc, D=D))
            u_new = uvp[:basis['u'].N]

            # 4. Monitor "Evolution Speed" (Not "Error")
            # We call this evolution speed because it measures how much the flow changed
            # in this time step. In unsteady flow, this NEVER reaches zero.
            diff_u = np.linalg.norm(u_new - u_prev) / (np.linalg.norm(u_new) + 1e-12)

            print(f"Step {iteration+1:3d} (t={current_time+dt:.3f}): ||du||/||u|| = {diff_u:.4e}")

            # 5. Update
            u_prev = u_new
            u0 = u_new
            current_time += dt

            if iteration > 5 and diff_u < 0.1:
                dt *= 1.01

        #else:
        #    print(f"\n⚠ Maximum iterations ({num_iterations}) reached. Final residual: {diff_u:.2e}")

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

        plot_results(mesh, X, Y, Vx, Vy, velocity_magnitude, obstacles)
        elapsed_time = time.time() - start_time
        print(f"\nTotal computation time: {elapsed_time:.2f} seconds")