"""
2D Steady Navier-Stokes Solver with Chen's Zero-Equation Turbulence Model
STABILIZED VERSION for realistic air viscosity (high Reynolds number flows)

Turbulence Model Reference:
    Chen, Q. and Xu, W. (1998). "A zero-equation turbulence model for
    indoor airflow simulation." Energy and Buildings, 28: 137-144.

Key stabilization features:
    - Under-relaxation for velocity updates
    - Continuation method (gradual viscosity reduction)
    - Minimum turbulent viscosity floor during early iterations
    - Robust convergence monitoring
"""

import sys
import os
import time
import argparse
import numpy as np
import meshio
from dolfin import *
from pathlib import Path

# Import helper functions
from helpers import extract_attributes, get_parameter_string, plot_results
from build_mesh_fenics import build_mesh

# Optimization for FEniCS
set_log_level(LogLevel.WARNING)
parameters["std_out_all_processes"] = False
parameters['form_compiler']['optimize'] = True
parameters['form_compiler']['cpp_optimize'] = True


# =============================================================================
# MESH ADAPTER
# =============================================================================

class SfePyMeshAdapter:
    """Wraps a FEniCS mesh to look like a SfePy mesh for the plot_results function."""
    def __init__(self, fenics_mesh):
        self.coors = fenics_mesh.coordinates()
        self.descs = ['2_3']
        self._conn = fenics_mesh.cells()

    def get_conn(self, desc):
        return self._conn


# =============================================================================
# BOUNDARY MARKING
# =============================================================================

def mark_boundaries_in_fenics(mesh, geom_data):
    """Marks boundaries robustly using FEniCS SubDomains."""
    boundaries = MeshFunction("size_t", mesh, mesh.topology().dim() - 1)
    boundaries.set_all(0)

    class Wall(SubDomain):
        def inside(self, x, on_boundary): return on_boundary
    Wall().mark(boundaries, 3)

    eps = 1e-2
    x_min, x_max = geom_data['x_min'], geom_data['x_max']
    y_min, y_max = geom_data['y_min'], geom_data['y_max']

    class FeatureBoundary(SubDomain):
        def __init__(self, c, s, **kwargs):
            super().__init__(**kwargs); self.c = c; self.s = s
        def inside(self, x, on_boundary):
            if not on_boundary: return False
            if self.s == 'left':   return near(x[0], x_min, eps) and (x[1] >= self.c[0] - eps and x[1] <= self.c[1] + eps)
            if self.s == 'right':  return near(x[0], x_max, eps) and (x[1] >= self.c[0] - eps and x[1] <= self.c[1] + eps)
            if self.s == 'bottom': return near(x[1], y_min, eps) and (x[0] >= self.c[0] - eps and x[0] <= self.c[1] + eps)
            if self.s == 'top':    return near(x[1], y_max, eps) and (x[0] >= self.c[0] - eps and x[0] <= self.c[1] + eps)
            return False

    for inlet in geom_data['inlets']:
        FeatureBoundary(inlet['coords'], inlet['side']).mark(boundaries, 1)
    for outlet in geom_data['outlets']:
        FeatureBoundary(outlet['coords'], outlet['side']).mark(boundaries, 2)

    return boundaries


# =============================================================================
# WALL DISTANCE COMPUTATION
# =============================================================================

def compute_wall_distance(mesh, boundaries, wall_marker=3):
    """Compute distance to nearest wall geometrically."""
    V = FunctionSpace(mesh, "CG", 1)
    d_func = Function(V, name="wall_distance")

    dof_coords = V.tabulate_dof_coordinates()

    wall_points = []
    for facet in SubsetIterator(boundaries, wall_marker):
        mp = facet.midpoint()
        wall_points.append([mp.x(), mp.y()])

    wall_points = np.array(wall_points)

    if len(wall_points) == 0:
        x_min, x_max = dof_coords[:, 0].min(), dof_coords[:, 0].max()
        y_min, y_max = dof_coords[:, 1].min(), dof_coords[:, 1].max()
        distances = np.minimum.reduce([
            dof_coords[:, 0] - x_min, x_max - dof_coords[:, 0],
            dof_coords[:, 1] - y_min, y_max - dof_coords[:, 1]
        ])
        d_func.vector().set_local(np.maximum(distances, 1e-10))
        return d_func

    distances = np.zeros(len(dof_coords))
    for i, coord in enumerate(dof_coords):
        dists = np.linalg.norm(wall_points - coord[:2], axis=1)
        distances[i] = np.min(dists)

    distances = np.maximum(distances, 1e-10)
    d_func.vector().set_local(distances)
    return d_func


# =============================================================================
# CHEN'S TURBULENCE MODEL (with stabilization options)
# =============================================================================

def chen_turbulent_viscosity(u, wall_distance, L_room, nu_molecular,
                              nu_t_min=None, nu_t_max=None):
    """
    Chen's zero-equation turbulent viscosity with optional bounds.

    Parameters:
    -----------
    nu_t_min : float, optional
        Minimum turbulent viscosity (for stability during early iterations)
    nu_t_max : float, optional
        Maximum turbulent viscosity (to prevent blowup)
    """
    kappa = 0.41
    C_chen = 0.03874
    L_max = 0.09 * L_room

    # Velocity magnitude with stability epsilon
    u_mag = sqrt(inner(u, u) + Constant(1e-10))

    # Mixing length
    L_mix = conditional(lt(kappa * wall_distance, L_max),
                        kappa * wall_distance, Constant(L_max))

    # Base turbulent viscosity
    nu_t = Constant(C_chen) * u_mag * L_mix

    # Apply bounds if specified
    if nu_t_min is not None:
        nu_t = conditional(lt(nu_t, nu_t_min), Constant(nu_t_min), nu_t)

    if nu_t_max is not None:
        nu_t = conditional(gt(nu_t, nu_t_max), Constant(nu_t_max), nu_t)

    # Effective viscosity
    nu_eff = Constant(nu_molecular) + nu_t

    return nu_eff


def compute_nu_t_field(u, wall_distance, L_room, mesh):
    """Compute turbulent viscosity as a field for diagnostics."""
    V_scalar = FunctionSpace(mesh, "CG", 1)

    kappa = 0.41
    C_chen = 0.03874
    L_max = 0.09 * L_room

    u_mag = sqrt(inner(u, u) + Constant(1e-10))
    L_mix = conditional(lt(kappa * wall_distance, L_max),
                        kappa * wall_distance, Constant(L_max))
    nu_t_expr = Constant(C_chen) * u_mag * L_mix

    return project(nu_t_expr, V_scalar)


# =============================================================================
# MAIN FUNCTION WITH STABILIZATION
# =============================================================================

def main():
    start_time = time.time()

    parser = argparse.ArgumentParser()
    parser.add_argument('scenario')
    parser.add_argument('hash')
    args = parser.parse_args()
    cache_dir = Path(args.scenario).parent / "cache"
    cache_dir.mkdir(parents=True, exist_ok=True)
    scenario_name = Path(args.scenario).stem

    # =========================================================================
    # 1. Generate & Load Mesh
    # =========================================================================
    geom_data = extract_attributes(args.scenario)

    print("Generating mesh...")
    mesh_file = build_mesh(geom_data, mesh_filename=f"temp_{args.hash}")

    mesh = Mesh()
    with XDMFFile(mesh_file) as infile:
        infile.read(mesh)
    boundaries = mark_boundaries_in_fenics(mesh, geom_data)

    n_inlet = sum(1 for _ in SubsetIterator(boundaries, 1))
    n_outlet = sum(1 for _ in SubsetIterator(boundaries, 2))
    n_wall = sum(1 for _ in SubsetIterator(boundaries, 3))
    print(f"\nMesh: {mesh.num_vertices()} vertices, {mesh.num_cells()} cells")
    print(f"Boundaries: {n_inlet} inlet, {n_outlet} outlet, {n_wall} wall facets")

    if n_inlet == 0 or n_outlet == 0:
        print("CRITICAL: Inlets or Outlets missing!")
        sys.exit(1)

    # =========================================================================
    # 2. Compute Wall Distance and Flow Parameters
    # =========================================================================
    print("\nComputing wall distance...")
    wall_distance = compute_wall_distance(mesh, boundaries, wall_marker=3)

    x_min, x_max = geom_data['x_min'], geom_data['x_max']
    y_min, y_max = geom_data['y_min'], geom_data['y_max']
    L_room = min(x_max - x_min, y_max - y_min)

    nu_molecular = geom_data['viscosity']
    inlet_vel = geom_data['inlet_velocity']

    Re_molecular = inlet_vel * L_room / nu_molecular

    print(f"\n=== Flow Parameters ===")
    print(f"Molecular viscosity: {nu_molecular:.2e} m²/s")
    print(f"Inlet velocity: {inlet_vel:.3f} m/s")
    print(f"Room dimension: {L_room:.2f} m")
    print(f"Reynolds number: {Re_molecular:.0f}")

    # Determine if we need stabilization
    HIGH_RE_THRESHOLD = 5000
    need_stabilization = Re_molecular > HIGH_RE_THRESHOLD

    if need_stabilization:
        print(f"\n*** HIGH REYNOLDS NUMBER DETECTED (Re > {HIGH_RE_THRESHOLD}) ***")
        print("*** Using stabilized solver with continuation method ***")

    # =========================================================================
    # 3. Function Spaces and Boundary Conditions
    # =========================================================================
    V = VectorElement("P", mesh.ufl_cell(), 2)
    Q = FiniteElement("P", mesh.ufl_cell(), 1)
    W = FunctionSpace(mesh, MixedElement([V, Q]))

    print(f"DOFs: {W.dim()}")

    bcs = []
    bcs.append(DirichletBC(W.sub(0), Constant((0, 0)), boundaries, 3))  # Walls
    bcs.append(DirichletBC(W.sub(1), Constant(0.0), boundaries, 2))     # Outlet

    # Inlet BC
    for inlet in geom_data['inlets']:
        c, s = inlet['coords'], inlet['side']
        center = (c[0] + c[1]) / 2.0
        width = c[1] - c[0]
        U_max = 1.5 * inlet_vel
        if s == 'left':    e = f"{U_max}*(1.0 - pow((x[1]-{center})/({width}/2.0), 2))"
        elif s == 'right': e = f"-{U_max}*(1.0 - pow((x[1]-{center})/({width}/2.0), 2))"
        elif s == 'bottom': e = f"{U_max}*(1.0 - pow((x[0]-{center})/({width}/2.0), 2))"
        elif s == 'top':    e = f"-{U_max}*(1.0 - pow((x[0]-{center})/({width}/2.0), 2))"
        val = Expression((e, "0.0") if s in ['left', 'right'] else ("0.0", e), degree=2)
        bcs.append(DirichletBC(W.sub(0), val, boundaries, 1))

    # =========================================================================
    # 4. Solver Setup
    # =========================================================================
    (u, p) = TrialFunctions(W)
    (v, q) = TestFunctions(W)
    w = Function(W)
    w_old = Function(W)  # For under-relaxation

    u_k = Function(W.sub(0).collapse())
    u_k.vector().zero()

    # =========================================================================
    # 5. CONTINUATION METHOD: Start with high viscosity, gradually reduce
    # =========================================================================

    if need_stabilization:
        # Estimate target turbulent viscosity
        nu_t_target = 0.03874 * inlet_vel * 0.09 * L_room
        nu_eff_target = nu_molecular + nu_t_target

        # Start with artificially high viscosity for stability
        nu_start = max(1e-2, 10 * nu_eff_target)  # Start high
        nu_end = nu_molecular  # End at physical value

        # Continuation schedule: logarithmic reduction
        n_continuation_steps = 5
        nu_schedule = np.logspace(np.log10(nu_start), np.log10(nu_end), n_continuation_steps)

        print(f"\n--- Continuation Schedule ---")
        print(f"Starting viscosity: {nu_start:.2e}")
        print(f"Target viscosity: {nu_end:.2e}")
        print(f"Steps: {n_continuation_steps}")

    else:
        # No continuation needed for low Re
        nu_schedule = [nu_molecular]
        n_continuation_steps = 1

    # =========================================================================
    # 6. Phase 1: Stokes Initialization with HIGH viscosity
    # =========================================================================
    print("\n--- Phase 1: Stokes Initialization ---")

    nu_init = nu_schedule[0] if need_stabilization else nu_molecular + 0.03874 * inlet_vel * 0.09 * L_room
    print(f"Initial viscosity: {nu_init:.2e} m²/s")

    F_stokes = (Constant(nu_init) * inner(grad(u), grad(v)) * dx
                - p * div(v) * dx
                + div(u) * q * dx)

    solve(lhs(F_stokes) == rhs(F_stokes), w, bcs,
          solver_parameters={'linear_solver': 'mumps'})

    u_init, _ = w.split(deepcopy=True)
    u_k.assign(u_init)
    w_old.assign(w)
    print("Stokes initialization complete.")

    # =========================================================================
    # 7. Phase 2: Continuation + Picard with Under-Relaxation
    # =========================================================================
    print("\n--- Phase 2: Picard Iteration with Continuation ---")

    # Under-relaxation parameter (0 < omega <= 1)
    # Lower = more stable but slower; Higher = faster but less stable
    omega = 0.5 if need_stabilization else 0.7

    max_picard_iter = 50
    picard_tol = 1e-4

    for cont_step, nu_base in enumerate(nu_schedule):
        print(f"\n  [Continuation step {cont_step+1}/{len(nu_schedule)}: ν_base = {nu_base:.2e}]")

        # Minimum ν_t to ensure stability during this continuation step
        # Gradually reduce the floor as we progress
        if need_stabilization and cont_step < len(nu_schedule) - 1:
            nu_t_min = 0.1 * nu_base  # 10% of current base viscosity as floor
            nu_t_max = 10 * nu_base   # Cap at 10x to prevent blowup
        else:
            nu_t_min = None
            nu_t_max = None

        converged = False

        for picard_iter in range(max_picard_iter):
            # Compute effective viscosity with Chen model
            # Use nu_base instead of nu_molecular during continuation
            if need_stabilization and cont_step < len(nu_schedule) - 1:
                # During continuation: use elevated base + bounded Chen
                nu_eff = chen_turbulent_viscosity(u_k, wall_distance, L_room,
                                                   nu_base, nu_t_min, nu_t_max)
            else:
                # Final step: use true molecular + unbounded Chen
                nu_eff = chen_turbulent_viscosity(u_k, wall_distance, L_room,
                                                   nu_molecular)

            # Picard-linearized Navier-Stokes
            F_picard = (inner(dot(u_k, nabla_grad(u)), v) * dx
                        + nu_eff * inner(grad(u), grad(v)) * dx
                        - p * div(v) * dx
                        + div(u) * q * dx)

            # Solve
            solve(lhs(F_picard) == rhs(F_picard), w, bcs,
                  solver_parameters={'linear_solver': 'mumps'})

            # Under-relaxation: w_new = omega * w + (1-omega) * w_old
            w_array = w.vector().get_local()
            w_old_array = w_old.vector().get_local()
            w_relaxed = omega * w_array + (1.0 - omega) * w_old_array
            w.vector().set_local(w_relaxed)

            # Extract and check
            u_new, p_new = w.split(deepcopy=True)

            # Convergence check
            error = errornorm(u_new, u_k, 'L2')
            u_norm = norm(u_new, 'L2')
            rel_error = error / max(u_norm, 1e-10)

            # Velocity magnitude check
            u_array = u_new.vector().get_local()
            n_dofs = len(u_array) // 2
            u_mag_array = np.sqrt(u_array[:n_dofs]**2 + u_array[n_dofs:]**2)
            u_max = np.max(u_mag_array) if len(u_mag_array) > 0 else 0.0

            # Stability check
            if u_max > 100 * inlet_vel or np.isnan(u_max):
                print(f"    Iter {picard_iter+1}: UNSTABLE (u_max = {u_max:.2f}), reducing relaxation")
                omega *= 0.8  # Reduce relaxation
                w.vector().set_local(w_old_array)  # Revert
                continue

            if picard_iter % 5 == 0 or rel_error < picard_tol:
                print(f"    Iter {picard_iter+1}: rel_error = {rel_error:.2e}, u_max = {u_max:.4f} m/s")

            # Update for next iteration
            u_k.assign(u_new)
            w_old.assign(w)

            if rel_error < picard_tol:
                print(f"    Converged after {picard_iter+1} iterations!")
                converged = True
                break

        if not converged:
            print(f"    Warning: Max iterations reached at continuation step {cont_step+1}")
            # Continue anyway to next step

    # =========================================================================
    # 8. Final Statistics and Validation
    # =========================================================================
    print("\n--- Final Solution Check ---")

    u_final, p_final = w.split(deepcopy=True)

    # Check for valid solution
    u_array = u_final.vector().get_local()
    n_dofs = len(u_array) // 2
    u_mag_array = np.sqrt(u_array[:n_dofs]**2 + u_array[n_dofs:]**2)
    u_max_final = np.max(u_mag_array)
    u_mean_final = np.mean(u_mag_array)

    # Sanity check: velocities should be reasonable
    if u_max_final > 50 * inlet_vel:
        print(f"WARNING: Solution may be unstable (u_max = {u_max_final:.2f} m/s)")
        print("Consider: finer mesh, lower inlet velocity, or higher viscosity")
    else:
        print(f"Solution looks stable: u_max = {u_max_final:.4f} m/s")

    # Compute turbulent viscosity statistics
    nu_t_field = compute_nu_t_field(u_final, wall_distance, L_room, mesh)
    nu_t_array = nu_t_field.vector().get_local()
    nu_t_max = np.max(nu_t_array)
    nu_t_mean = np.mean(nu_t_array)

    print(f"\n--- Turbulence Statistics ---")
    print(f"Turbulent viscosity ν_t:")
    print(f"  Max:  {nu_t_max:.2e} m²/s  (ν_t/ν = {nu_t_max/nu_molecular:.1f})")
    print(f"  Mean: {nu_t_mean:.2e} m²/s  (ν_t/ν = {nu_t_mean/nu_molecular:.1f})")
    print(f"Effective viscosity (ν + ν_t):")
    print(f"  Max:  {nu_molecular + nu_t_max:.2e} m²/s")
    print(f"  Mean: {nu_molecular + nu_t_mean:.2e} m²/s")

    # =========================================================================
    # 9. Export Results
    # =========================================================================
    print("\n--- Exporting Results ---")

    res = geom_data.get('rect_grid_cell_size', 0.1)
    xmin, xmax = geom_data['x_min'], geom_data['x_max']
    ymin, ymax = geom_data['y_min'], geom_data['y_max']
    nx = int(np.round((xmax - xmin) / res)) + 1
    ny = int(np.round((ymax - ymin) / res)) + 1
    x_rng, y_rng = np.linspace(xmin, xmax, nx), np.linspace(ymin, ymax, ny)
    X, Y = np.meshgrid(x_rng, y_rng)

    Vx_grid, Vy_grid = np.zeros_like(X), np.zeros_like(Y)
    u_eval = w.sub(0, deepcopy=True)
    u_eval.set_allow_extrapolation(True)

    print(f"Interpolating to {nx}x{ny} grid...")
    for i in range(ny):
        for j in range(nx):
            try:
                val = u_eval(Point(X[i, j], Y[i, j]))
                Vx_grid[i, j], Vy_grid[i, j] = val[0], val[1]
            except:
                pass

    vel_mag = np.hypot(Vx_grid, Vy_grid)
    print(f"Max velocity on grid: {np.max(vel_mag):.4f} m/s")
    print(f"Mean velocity on grid: {np.mean(vel_mag):.4f} m/s")

    # Validate before saving
    if np.max(vel_mag) > 1e6 or np.isnan(np.max(vel_mag)):
        print("\nERROR: Solution is unstable. Not saving results.")
        print("Suggestions:")
        print("  1. Use a finer mesh")
        print("  2. Reduce inlet velocity")
        print("  3. Use a slightly elevated viscosity (e.g., 1e-4 instead of 1.5e-5)")
        sys.exit(1)

    # Save
    param_str = get_parameter_string(geom_data)
    header = f'{ny}_{nx}_{param_str}'
    vx_path = cache_dir / f"{scenario_name}_{args.hash}_Vx.txt"
    vy_path = cache_dir / f"{scenario_name}_{args.hash}_Vy.txt"
    img_path = cache_dir / f"{scenario_name}_{args.hash}_results.png"
    np.savetxt(vx_path, Vx_grid, header=header)
    np.savetxt(vy_path, Vy_grid, header=header)

    # Plot
    if np.max(vel_mag) > 1e-6 and np.max(vel_mag) < 1000.0:
        adapter = SfePyMeshAdapter(mesh)
        plot_results(adapter, X, Y, Vx_grid, Vy_grid, vel_mag, geom_data["obstacles"], str(img_path))
    else:
        from matplotlib import pyplot as plt
        plt.figure()
        plt.text(0.5, 0.5, "Error: Unstable Solution", ha='center', va='center')
        plt.savefig(str(img_path))
        plt.close()

    # Cleanup
    for f in [f"temp_{args.hash}.msh", f"temp_{args.hash}.xdmf", f"temp_{args.hash}.h5"]:
        if os.path.exists(f):
            os.remove(f)

    print(f"\n=== Complete ===")
    print(f"Total time: {time.time() - start_time:.2f} s")


if __name__ == '__main__':
    main()