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
num_iterations = 100
tolerance = 1e-4
relaxation = 0.3  # Slightly higher for lower viscosity
use_supg = True  # Enable SUPG stabilization
supg_parameter = 1.0  # SUPG parameter (0.5-1.0 typical)
use_pspg = True
mixing_length_sq = 0.1**2

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

        area_threshold = 0.05

        mesh, inlet_dict, outlet_dict, boundary_dict = build_mesh(inlets, outlets, obstacles, area_threshold, x_min, x_max, y_min, y_max)

        element = {'u': ElementVector(ElementTriP2()), 'p': ElementTriP1()}
        basis = {variable: Basis(mesh, e, intorder=4)
                 for variable, e in element.items()}

        print("Velocity DOFs:", basis['u'].N)
        print("Pressure DOFs:", basis['p'].N)
        print("Total system size:", basis['u'].N * 2 + basis['p'].N)


        # --- Define Bilinear Forms for Navier-Stokes ---
        @BilinearForm
        def stokes_visc_old(u, v, w):
            """Viscous term with symmetric gradient"""
            return nu * inner(sym_grad(u), sym_grad(v))


        @BilinearForm
        def stokes_visc(u, v, w):
            """
            RANS Viscous term: Physical Nu + Turbulent Nu
            Uses a Smagorinsky-style Zero-Equation model
            """
            # 1. Get the velocity from the previous iteration (w['w'])
            # w['w'] is the vector field from the previous step

            # 2. Calculate the Symmetric Gradient (Strain Rate Tensor) of previous step
            # We use this to decide where the flow is turbulent
            du = sym_grad(w['w'])

            # 3. Calculate magnitude of strain: |S| = sqrt(2 * S_ij * S_ij)
            # 1e-12 prevents division by zero or errors in zero-flow regions
            strain_mag = (2.0 * inner(du, du) + 1e-12)**0.5

            # 4. Calculate Eddy Viscosity (Nu_t)
            # Formula: Nu_t = (Mixing_Length)^2 * |Strain|
            # We approximate Mixing Length as a constant factor of element size (w.h)
            # w.h is the local mesh element size provided by skfem
            nu_t = (0.1 * w.h)**2 * strain_mag

            # 5. Total Effective Viscosity
            nu_eff = nu + nu_t

            # Return the standard viscous form using the NEW effective viscosity
            return nu_eff * inner(sym_grad(u), sym_grad(v))

        @BilinearForm
        def convection(u, v, w):
            """Convection term, skew-symmetric form"""
            #return dot(dot(w['w'], grad(u)), v)
            #return 0.5*(dot(dot(w['w'], grad(u)), v) - dot(dot(w['w'], grad(v)), u))
            return (dot(dot(w['w'], grad(u)), v) - dot(dot(w['w'], grad(v)), u)) / 2.0

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
        A_stokes = asm(stokes_visc_old, basis['u'])
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

        # Target viscosity
        target_nu = 1.5e-5

        # Start with "Honey" (Very thick air)
        nu = 1.0e-2

        # Decrease factor per step
        decay = 0.95

        u_sum = np.zeros_like(u0)
        start_averaging_at = 50  # Let the flow develop first
        count_avg = 0

        for iteration in range(num_iterations):
            # 1. Update viscosity
            nu = max(target_nu, nu * decay)

            # Interpolate velocity field for current iteration
            w_field = basis['u'].interpolate(u0.squeeze())

            A_stokes = asm(stokes_visc, basis['u'], w=w_field)

            # Assemble convection term
            A_conv = asm(convection, basis['u'], w=w_field)

            # Add SUPG stabilization if enabled
            if use_supg:
                A_supg = asm(supg_stabilization, basis['u'], w=w_field)
                A = A_stokes + A_conv + A_supg
            else:
                A = A_stokes + A_conv

            # Assemble saddle point system
            #K = bmat([[A, B.T], [B, None]], 'csr')

            if use_pspg:
                # PSPG modifies the pressure block (lower-right block of saddle point system)
                C_pspg = asm(pspg_stabilization, basis['p'], w=w_field)
                # Assemble saddle point system with PSPG
                K = bmat([[A, B.T], [B, -C_pspg]], 'csr')  # Note the negative sign
            else:
                K = bmat([[A, B.T], [B, None]], 'csr')

            # Solve linear system
            uvp = solve(*condense(K, x=uvp_fixed_bc, D=D))
            u_new = uvp[:basis['u'].N]
            p_new = uvp[basis['u'].N:]

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

            if iteration >= start_averaging_at:
                u_sum += u0
                count_avg += 1

            # Check convergence
            if diff_u < tolerance:
                print(f"\n✓ Converged after {iteration+1} iterations (||Δu||/||u|| = {diff_u:.2e})")
                break
        else:
            print(f"\n⚠ Maximum iterations ({num_iterations}) reached. Final residual: {diff_u:.2e}")

        if count_avg > 0:
            print(f"Averaging solution over last {count_avg} steps...")
            u_final = u_sum / count_avg
        else:
            u_final = u0

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