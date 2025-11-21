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
supg_parameter = 0.5  # SUPG parameter (0.5-1.0 typical)
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

        print("Setting up boundary conditions...")

        # Use your existing define_dofs function
        D_original = define_dofs(basis, mesh, inlet_dict, outlet_dict)

        # CRITICAL FIX 1: Add pressure reference point
        # This fixes the singularity in your system
        D_pressure_ref = np.array([basis['u'].N], dtype=np.int32)  # Fix first pressure DOF
        D = np.unique(np.concatenate([D_original, D_pressure_ref]))

        print(f"Total constrained DOFs: {len(D)} (added {len(D) - len(D_original)} for pressure reference)")

        def parabolic(x):
            """return the plane Poiseuille parabolic inlet profile"""
            return np.stack([4 * x[1] * (1. - x[1]), np.zeros_like(x[0])])

        # CRITICAL FIX 2: Ensure pressure reference is set to zero in BC vector
        inlet_basis = define_inlet_basis(basis, mesh, inlets, inlet_velocity)
        outlet_basis = basis['p'].zeros()
        outlet_basis[0] = 0.0  # Fix pressure reference to zero

        uvp_fixed_bc = np.hstack((
            inlet_basis,
            outlet_basis,
        ))

        # Solve Stokes flow first for initial guess
        print("Solving initial Stokes flow...")
        A_stokes = asm(stokes_visc, basis['u'])
        B = -asm(divergence, basis['u'], basis['p'])
        K_stokes = bmat([[A_stokes, B.T], [B, None]], 'csr')

        uvp_stokes = solve(*condense(K_stokes, x=uvp_fixed_bc, D=D))
        uvp_stokes = np.zeros(len(uvp_stokes), dtype=float)
        u0, p0 = np.split(uvp_stokes, K_stokes.blocks)

        # CRITICAL FIX 3: Better convergence parameters
        supg_parameter_actual = 1.0  # Reduced from 1.0
        tolerance_actual = 1e-6
        num_iterations_actual = 300

        print(f"\nStarting Picard iteration with nu={nu:.2e} m²/s")
        print(f"SUPG parameter: {supg_parameter_actual}")
        print(f"SUPG stabilization: {'ENABLED' if use_supg else 'DISABLED'}")

        diff_u = 1.0  # Initialize

        for iteration in range(num_iterations_actual):
            # Interpolate velocity field for current iteration
            w_field = basis['u'].interpolate(u0.squeeze())

            # Assemble convection term
            A_conv = asm(convection, basis['u'], w=w_field)

            # Add SUPG stabilization if enabled
            if use_supg:
                @BilinearForm
                def supg_stabilization_fixed(u, v, w):
                    """SUPG with corrected parameter"""
                    vel = w['w']
                    vel_mag = (dot(vel, vel) + 1e-12)**0.5
                    h = w.h

                    tau_adv = h / (2.0 * vel_mag + 1e-12)
                    tau_diff = h * h / (4.0 * nu + 1e-12)
                    tau = 1.0 / (1.0/tau_adv + 1.0/tau_diff)
                    tau = supg_parameter_actual * tau  # Use 0.3 instead of 1.0

                    residual = dot(vel, grad(u))
                    test_streamline = dot(vel, grad(v))

                    return tau * dot(residual, test_streamline)

                A_supg = asm(supg_stabilization_fixed, basis['u'], w=w_field)
                A = A_stokes + A_conv + A_supg
            else:
                A = A_stokes + A_conv

            # Assemble saddle point system
            if use_pspg:
                C_pspg = asm(pspg_stabilization, basis['p'], w=w_field)
                K = bmat([[A, B.T], [B, -C_pspg]], 'csr')
            else:
                K = bmat([[A, B.T], [B, None]], 'csr')

            # Solve linear system
            uvp = solve(*condense(K, x=uvp_fixed_bc, D=D))
            u_new = uvp[:basis['u'].N]
            p_new = uvp[basis['u'].N:]

            # CRITICAL FIX 4: Adaptive relaxation
            if iteration < 5:
                relaxation_actual = 0.05  # Very slow start
            elif iteration < 15:
                relaxation_actual = 0.2
            elif diff_u > 0.1:
                relaxation_actual = 0.3
            elif diff_u > 0.01:
                relaxation_actual = 0.5
            else:
                relaxation_actual = 0.7  # Faster when converging

            # Apply relaxation
            u_relaxed = relaxation_actual * u_new + (1.0 - relaxation_actual) * u0

            # Compute convergence metric
            diff_u = np.linalg.norm(u_relaxed.ravel() - u0.ravel()) / (np.linalg.norm(u_relaxed.ravel()) + 1e-12)

            # Update solution
            u0 = u_relaxed
            p0 = p_new

            # Print progress
            if (iteration + 1) % 10 == 0 or iteration < 10 or diff_u < tolerance_actual:
                print(f"Iteration {iteration+1:3d}: ||Δu||/||u|| = {diff_u:.3e}, relax = {relaxation_actual:.2f}")

            # Check convergence
            if diff_u < tolerance_actual:
                print(f"\n✓ Converged after {iteration+1} iterations (||Δu||/||u|| = {diff_u:.3e})")
                break

        else:
            print(f"\n⚠ Maximum iterations reached. Final residual: {diff_u:.3e}")

        # DIAGNOSTIC: Check solution quality
        print("\n" + "="*60)
        print("Solution Quality Check")
        print("="*60)

        # Check if velocity field makes sense
        u_magnitude = np.sqrt(u0[::2]**2 + u0[1::2]**2)
        print(f"Velocity magnitude: min={u_magnitude.min():.4f}, max={u_magnitude.max():.4f}, mean={u_magnitude.mean():.4f} m/s")
        print(f"Pressure: min={p0.min():.4f}, max={p0.max():.4f}, mean={p0.mean():.4f} Pa")
        print(f"Pressure at reference (should be ~0): {p0[0]:.6e} Pa")

        # Check mass conservation
        from skfem.helpers import div as helper_div
        @LinearForm
        def divergence_check(v, w):
            return v * helper_div(w['u'])

        div_values = asm(divergence_check, basis['p'], u=basis['u'].interpolate(u0))
        max_divergence = np.abs(div_values).max()
        mean_divergence = np.abs(div_values).mean()
        print(f"Divergence: max={max_divergence:.3e}, mean={mean_divergence:.3e} (should be < 1e-8)")

        if max_divergence > 1e-6:
            print("⚠ WARNING: Large divergence detected - mass conservation violated!")
            print("  Possible causes: mesh quality, BC implementation, or numerical errors")

        print("="*60 + "\n")


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