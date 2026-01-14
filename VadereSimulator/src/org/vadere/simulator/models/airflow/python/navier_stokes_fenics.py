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
# Import your custom mesh generator (assumed to be in build_mesh.py)
from build_mesh_fenics import build_mesh

# Optimization for FEniCS
parameters["std_out_all_processes"] = False
parameters['form_compiler']['optimize'] = True
parameters['form_compiler']['cpp_optimize'] = True

class SfePyMeshAdapter:
    """Wraps a FEniCS mesh to look like a SfePy mesh for the plot_results function."""
    def __init__(self, fenics_mesh):
        self.coors = fenics_mesh.coordinates()
        self.descs = ['2_3']
        self._conn = fenics_mesh.cells()

    def get_conn(self, desc):
        return self._conn

def mark_boundaries_in_fenics(mesh, geom_data):
    """Marks boundaries robustly using FEniCS SubDomains."""
    boundaries = MeshFunction("size_t", mesh, mesh.topology().dim() - 1)
    boundaries.set_all(0)

    # Mark everything as Wall (3) initially
    class Wall(SubDomain):
        def inside(self, x, on_boundary): return on_boundary
    Wall().mark(boundaries, 3)

    # Use a tolerance to catch nodes on the lines
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

def main():
    start_time = time.time()

    parser = argparse.ArgumentParser()
    parser.add_argument('scenario'); parser.add_argument('hash')
    args = parser.parse_args()
    cache_dir = Path(args.scenario).parent / "cache"
    cache_dir.mkdir(parents=True, exist_ok=True)
    scenario_name = Path(args.scenario).stem

    # --- 1. Generate & Load Mesh ---
    geom_data = extract_attributes(args.scenario)

    print("Generating mesh...")
    # This calls your build_mesh.py logic and returns the path to the XDMF file
    mesh_file = build_mesh(geom_data, mesh_filename=f"temp_{args.hash}")

    mesh = Mesh()
    with XDMFFile(mesh_file) as infile: infile.read(mesh)
    boundaries = mark_boundaries_in_fenics(mesh, geom_data)

    n_inlet = sum(1 for _ in SubsetIterator(boundaries, 1))
    n_outlet = sum(1 for _ in SubsetIterator(boundaries, 2))
    print(f"\nDiagnostics:\n  - Inlets found: {n_inlet}\n  - Outlets found: {n_outlet}")

    if n_inlet == 0 or n_outlet == 0:
        print("CRITICAL WARNING: Inlets or Outlets missing. Solver will likely fail.")
        sys.exit(1)

    # --- 2. Physics Setup ---
    # Taylor-Hood Elements (P2-P1) for stability
    V = VectorElement("P", mesh.ufl_cell(), 2)
    Q = FiniteElement("P", mesh.ufl_cell(), 1)
    W = FunctionSpace(mesh, MixedElement([V, Q]))

    # Define Boundary Conditions
    #bcs = []
    # Wall (3): No Slip
    bcs = [DirichletBC(W.sub(0), Constant((0, 0)), boundaries, 3)]
    # Outlet (2): Pressure = 0 (Standard open boundary)
    bcs.append(DirichletBC(W.sub(1), Constant(0.0), boundaries, 2))

    # Inlet (1): Parabolic Profile
    inlet_vel = geom_data['inlet_velocity']
    for inlet in geom_data['inlets']:
        c, s = inlet['coords'], inlet['side']
        center = (c[0] + c[1]) / 2.0
        width = c[1] - c[0]
        # Profile: U_max * (1 - r^2)
        if s=='left':   e = f"{inlet_vel}*(1.0 - pow((x[1]-{center})/({width}/2.0), 2))"
        elif s=='right': e = f"-{inlet_vel}*(1.0 - pow((x[1]-{center})/({width}/2.0), 2))"
        elif s=='bottom': e = f"{inlet_vel}*(1.0 - pow((x[0]-{center})/({width}/2.0), 2))"
        elif s=='top':    e = f"-{inlet_vel}*(1.0 - pow((x[0]-{center})/({width}/2.0), 2))"
        val = Expression((e, "0.0") if s in ['left','right'] else ("0.0", e), degree=2)
        bcs.append(DirichletBC(W.sub(0), val, boundaries, 1))

    # --- 3. Solver Setup ---
    (u, p) = TrialFunctions(W)
    (v, q) = TestFunctions(W)
    w = Function(W)
    u_sol, p_sol = split(w)

    mu = Constant(geom_data['viscosity']) # Realistic viscosity

    # Function to hold the velocity from the previous iteration
    u_k = Function(W.sub(0).collapse())
    u_k.vector().zero()

    # Form 1: Picard Linearization (Stabilized)
    # Replaces nonlinear term (u.grad)u with (u_k.grad)u
    F_picard = (inner(dot(u_k, nabla_grad(u)), v) * dx
                + mu * inner(nabla_grad(u), nabla_grad(v)) * dx
                - p * div(v) * dx
                + div(u) * q * dx)

    # Form 2: Full Newton (Accurate)
    F_newton = (inner(dot(u_sol, nabla_grad(u_sol)), v) * dx
                + mu * inner(nabla_grad(u_sol), nabla_grad(v)) * dx
                - p_sol * div(v) * dx
                + div(u_sol) * q * dx)

    print("\n--- Phase 1: Initialization (Picard) ---")
    print("Stabilizing flow field...")
    # Run 15 linear steps to orient the flow before turning on full physics
    for i in range(15):
        solve(lhs(F_picard) == rhs(F_picard), w, bcs, solver_parameters={'linear_solver': 'mumps'})
        # Update u_k with the result of this step
        u_curr, p_curr = w.split(deepcopy=True)
        u_k.assign(u_curr)
    print("Flow initialized.")

    print("\n--- Phase 2: Final Solve (Newton) ---")
    # Now that 'w' contains a valid flow field, Newton will converge easily
    try:
        solve(F_newton == 0, w, bcs, solver_parameters={
            "newton_solver": {
                "linear_solver": "gmres",      # mumps, gmres, amg, hypre_euclid
                "preconditioner": "ilu",       # Incomplete LU factorization
                "relative_tolerance": 1e-6,
                "maximum_iterations": 50,
                "relaxation_parameter": 1.0
            }
        })
        print("Converged successfully.")
    except Exception as e:
        print(f"Convergence warning: {e}")
        print("Falling back to Picard result (approximate but stable).")

    # --- 4. Export ---
    print("\nInterpolating...")
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

    for i in range(ny):
        for j in range(nx):
            try:
                val = u_eval(Point(X[i, j], Y[i, j]))
                Vx_grid[i, j], Vy_grid[i, j] = val[0], val[1]
            except: pass

    vel_mag = np.hypot(Vx_grid, Vy_grid)
    print(f"Max velocity: {np.max(vel_mag):.4f} m/s")

    param_str = get_parameter_string(geom_data)
    header = f'{ny}_{nx}_{param_str}'
    vx_path = cache_dir / f"{scenario_name}_{args.hash}_Vx.txt"
    vy_path = cache_dir / f"{scenario_name}_{args.hash}_Vy.txt"
    img_path = cache_dir / f"{scenario_name}_{args.hash}_results.png"
    np.savetxt(vx_path, Vx_grid, header=header)
    np.savetxt(vy_path, Vy_grid, header=header)

    if np.max(vel_mag) > 1e-6 and np.max(vel_mag) < 1000.0:
        adapter = SfePyMeshAdapter(mesh)
        plot_results(adapter, X, Y, Vx_grid, Vy_grid, vel_mag, geom_data["obstacles"], str(img_path))
    else:
        from matplotlib import pyplot as plt
        plt.figure(); plt.text(0.5,0.5,"Error: Unstable Solution"); plt.savefig(str(img_path)); plt.close()

    for f in [f"temp_{args.hash}.msh", f"temp_{args.hash}.xdmf", f"temp_{args.hash}.h5"]:
        if os.path.exists(f): os.remove(f)

    print(f"Total time: {time.time() - start_time:.2f} s")

if __name__ == '__main__':
    main()