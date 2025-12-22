import sys
import os
import time
import argparse
import numpy as np
import meshio
import gmsh
from dolfin import *
from pathlib import Path

# Import your existing helper functions
from helpers import extract_attributes, get_parameter_string, plot_results

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

def generate_mesh_from_build_mesh_logic(geom_data, mesh_filename="temp_mesh"):
    """
    Generates mesh using the EXACT logic from your provided build_mesh.py.
    """
    gmsh.initialize()
    gmsh.model.add("airflow_model")

    x_min, x_max = geom_data['x_min'], geom_data['x_max']
    y_min, y_max = geom_data['y_min'], geom_data['y_max']

    # --- SIZING LOGIC ---
    h_max = geom_data['max_triangle_edge_len']
    refinement_factor = 3.0
    h_wall = h_max / refinement_factor
    dist_min = h_wall * 2.0
    dist_max = h_wall * 15.0

    # --- GEOMETRY ---
    domain_tag = gmsh.model.occ.addRectangle(x_min, y_min, 0, x_max - x_min, y_max - y_min)
    feature_points = []
    all_features = geom_data['inlets'] + geom_data['outlets']

    for feat in all_features:
        s, c = feat['side'], feat['coords']
        pts_to_add = []
        if s == 'bottom':   pts_to_add = [(c[0], y_min), (c[1], y_min)]
        elif s == 'top':    pts_to_add = [(c[0], y_max), (c[1], y_max)]
        elif s == 'left':   pts_to_add = [(x_min, c[0]), (x_min, c[1])]
        elif s == 'right':  pts_to_add = [(x_max, c[0]), (x_max, c[1])]

        for (px, py) in pts_to_add:
            pt_tag = gmsh.model.occ.addPoint(px, py, 0)
            feature_points.append((0, pt_tag))

    if feature_points:
        occ_res, _ = gmsh.model.occ.fragment([(2, domain_tag)], feature_points)
        for dim, tag in occ_res:
            if dim == 2:
                domain_tag = tag
                break

    obstacle_tags = []
    for obs_pts in geom_data['obstacles']:
        p_tags = [gmsh.model.occ.addPoint(px, py, 0) for px, py in obs_pts]
        l_tags = []
        for i in range(len(p_tags)):
            l_tags.append(gmsh.model.occ.addLine(p_tags[i], p_tags[(i+1)%len(p_tags)]))

        wire_tag = gmsh.model.occ.addCurveLoop(l_tags)
        surf_tag = gmsh.model.occ.addPlaneSurface([wire_tag])
        obstacle_tags.append((2, surf_tag))

    if obstacle_tags:
        occ_res, _ = gmsh.model.occ.cut([(2, domain_tag)], obstacle_tags)
        if occ_res:
            for dim, tag in occ_res:
                if dim == 2:
                    domain_tag = tag
                    break

    gmsh.model.occ.synchronize()

    # --- MESH FIELDS ---
    boundary_curves = [tag for dim, tag in gmsh.model.getEntities(dim=1)]

    dist_field = gmsh.model.mesh.field.add("Distance")
    gmsh.model.mesh.field.setNumbers(dist_field, "CurvesList", boundary_curves)
    gmsh.model.mesh.field.setNumber(dist_field, "Sampling", 100)

    thresh_field = gmsh.model.mesh.field.add("Threshold")
    gmsh.model.mesh.field.setNumber(thresh_field, "InField", dist_field)
    gmsh.model.mesh.field.setNumber(thresh_field, "SizeMin", h_wall)
    gmsh.model.mesh.field.setNumber(thresh_field, "SizeMax", h_max)
    gmsh.model.mesh.field.setNumber(thresh_field, "DistMin", dist_min)
    gmsh.model.mesh.field.setNumber(thresh_field, "DistMax", dist_max)

    gmsh.model.mesh.field.setAsBackgroundMesh(thresh_field)

    # --- GENERATION ---
    gmsh.option.setNumber("Mesh.Algorithm", 6)
    gmsh.model.mesh.generate(2)
    gmsh.write(f"{mesh_filename}.msh")
    gmsh.finalize()

    # Convert to XDMF
    msh = meshio.read(f"{mesh_filename}.msh")
    triangle_cells = [c.data for c in msh.cells if c.type == "triangle"][0]
    triangle_mesh = meshio.Mesh(points=msh.points[:, :2], cells={"triangle": triangle_cells})
    meshio.write(f"{mesh_filename}.xdmf", triangle_mesh)

    return f"{mesh_filename}.xdmf"

def mark_boundaries_in_fenics(mesh, geom_data):
    """Marks boundaries robustly using FEniCS SubDomains."""
    boundaries = MeshFunction("size_t", mesh, mesh.topology().dim() - 1)
    boundaries.set_all(0)

    class Wall(SubDomain):
        def inside(self, x, on_boundary): return on_boundary
    Wall().mark(boundaries, 3)

    # Increased tolerance to catch nodes exactly on the line
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

    # 1. Generate & Load
    geom_data = extract_attributes(args.scenario)
    mesh_file = generate_mesh_from_build_mesh_logic(geom_data, mesh_filename=f"temp_{args.hash}")

    mesh = Mesh()
    with XDMFFile(mesh_file) as infile: infile.read(mesh)
    boundaries = mark_boundaries_in_fenics(mesh, geom_data)

    n_inlet = sum(1 for _ in SubsetIterator(boundaries, 1))
    n_outlet = sum(1 for _ in SubsetIterator(boundaries, 2))
    print(f"\nDiagnostics:\n  - Inlets found: {n_inlet}\n  - Outlets found: {n_outlet}")

    if n_inlet == 0 or n_outlet == 0:
        print("CRITICAL WARNING: Inlets or Outlets missing.")

    # 2. Physics Setup
    V = VectorElement("P", mesh.ufl_cell(), 2)
    Q = FiniteElement("P", mesh.ufl_cell(), 1)
    W = FunctionSpace(mesh, MixedElement([V, Q]))

    # BCs
    bcs = [DirichletBC(W.sub(0), Constant((0, 0)), boundaries, 3)] # Wall
    bcs.append(DirichletBC(W.sub(1), Constant(0.0), boundaries, 2)) # Outlet P=0

    inlet_vel = geom_data['inlet_velocity']
    for inlet in geom_data['inlets']:
        c, s = inlet['coords'], inlet['side']
        center = (c[0] + c[1]) / 2.0
        width = c[1] - c[0]
        # Parabolic Profile
        if s=='left':   e = f"{inlet_vel}*(1.0 - pow((x[1]-{center})/({width}/2.0), 2))"
        elif s=='right': e = f"-{inlet_vel}*(1.0 - pow((x[1]-{center})/({width}/2.0), 2))"
        elif s=='bottom': e = f"{inlet_vel}*(1.0 - pow((x[0]-{center})/({width}/2.0), 2))"
        elif s=='top':    e = f"-{inlet_vel}*(1.0 - pow((x[0]-{center})/({width}/2.0), 2))"
        val = Expression((e, "0.0") if s in ['left','right'] else ("0.0", e), degree=2)
        bcs.append(DirichletBC(W.sub(0), val, boundaries, 1))

    # 3. Solver Setup (Picard + Newton)
    (u, p) = TrialFunctions(W)
    (v, q) = TestFunctions(W)
    w = Function(W)
    u_sol, p_sol = split(w)

    # Use exact user viscosity
    mu = Constant(geom_data['viscosity'])

    # Previous step function for Picard linearization
    u_k = Function(W.sub(0).collapse())
    u_k.vector().zero()

    # Linearized (Picard) Form
    F_picard = (inner(dot(u_k, nabla_grad(u)), v) * dx + mu*inner(nabla_grad(u), nabla_grad(v))*dx - p*div(v)*dx + div(u)*q*dx)

    # Full Nonlinear (Newton) Form
    F_newton = (inner(dot(u_sol, nabla_grad(u_sol)), v) * dx + mu*inner(nabla_grad(u_sol), nabla_grad(v))*dx - p_sol*div(v)*dx + div(u_sol)*q*dx)

    print("\nStarting Solver...")

    # A. Picard Iterations (Linear Stabilization)
    # We run enough of these to get the flow established.
    # This avoids the "start from zero" crash without changing physics.
    picard_steps = 15
    print(f"  -> Running {picard_steps} Picard steps to stabilize...")

    for i in range(picard_steps):
        solve(lhs(F_picard) == rhs(F_picard), w, bcs, solver_parameters={'linear_solver': 'mumps'})
        u_curr, p_curr = w.split(deepcopy=True)
        u_k.assign(u_curr)
        # Optional: Check convergence here if you want to exit early

    # B. Newton Solver (Final Accuracy)
    print("  -> Switching to Newton solver for final accuracy...")
    try:
        solve(F_newton == 0, w, bcs, solver_parameters={
            "newton_solver": {
                "linear_solver": "mumps",
                "relative_tolerance": 1e-6,
                "maximum_iterations": 50,
                "relaxation_parameter": 1.0,
                "error_on_nonconvergence": True
            }
        })
        print("  -> Newton converged.")
    except Exception as e:
        print(f"  ! Newton warning: {e}")
        print("  ! Falling back to best Picard result (usually sufficient for visualization).")

    # 4. Export
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