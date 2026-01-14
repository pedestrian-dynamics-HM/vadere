import sys
import os
import time
import argparse
import numpy as np
import meshio
from dolfin import *
from pathlib import Path

from helpers import extract_attributes, get_parameter_string, plot_results
from build_mesh_fenics import build_mesh

# Optimization
parameters["std_out_all_processes"] = False
parameters['form_compiler']['optimize'] = True
parameters['form_compiler']['cpp_optimize'] = True

class SfePyMeshAdapter:
    def __init__(self, fenics_mesh):
        self.coors = fenics_mesh.coordinates()
        self.descs = ['2_3']
        self._conn = fenics_mesh.cells()
    def get_conn(self, desc): return self._conn

def mark_boundaries(mesh, geom_data):
    boundaries = MeshFunction("size_t", mesh, mesh.topology().dim() - 1)
    boundaries.set_all(0)

    class Wall(SubDomain):
        def inside(self, x, on_boundary): return on_boundary
    Wall().mark(boundaries, 3)

    eps = 5e-2
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

    # 1. Mesh
    geom_data = extract_attributes(args.scenario)
    print("Generating mesh...")
    mesh_file = build_mesh(geom_data, mesh_filename=f"temp_{args.hash}")
    mesh = Mesh()
    with XDMFFile(mesh_file) as infile: infile.read(mesh)
    boundaries = mark_boundaries(mesh, geom_data)

    # 2. Spaces
    V = VectorFunctionSpace(mesh, "P", 2)
    Q = FunctionSpace(mesh, "P", 1)
    DG = FunctionSpace(mesh, "DG", 0) # For turbulence calculation

    # 3. Time Setup
    # T=5.0s is long enough to let the average stabilize
    T = 5.0
    dt = 0.02         # 20ms steps (LES is robust enough for this)
    k = Constant(dt)
    num_steps = int(T / dt)

    # 4. Boundary Conditions (Wobbly Inlet to trigger Turbulence)
    inlet_vel = geom_data['inlet_velocity']
    wobble_scale = 0.25 # 25% side-to-side oscillation
    freq = 3.0          # Slow, heavy oscillation

    inlet_exprs = []
    bcu = []
    bcu.append(DirichletBC(V, Constant((0, 0)), boundaries, 3)) # Wall

    for inlet in geom_data['inlets']:
        c, s = inlet['coords'], inlet['side']
        center, width = (c[0] + c[1]) / 2.0, c[1] - c[0]

        # Parabolic Profile + Transverse Sine Wave
        if s=='left':
            u_x = f"{inlet_vel}*(1.0 - pow((x[1]-{center})/({width}/2.0), 2))"
            u_y = f"{wobble_scale} * {inlet_vel} * sin({freq}*t)"
            val = Expression((u_x, u_y), degree=2, t=0.0)
        elif s=='right':
            u_x = f"-{inlet_vel}*(1.0 - pow((x[1]-{center})/({width}/2.0), 2))"
            u_y = f"{wobble_scale} * {inlet_vel} * sin({freq}*t)"
            val = Expression((u_x, u_y), degree=2, t=0.0)
        elif s=='bottom':
            u_y = f"{inlet_vel}*(1.0 - pow((x[0]-{center})/({width}/2.0), 2))"
            u_x = f"{wobble_scale} * {inlet_vel} * sin({freq}*t)"
            val = Expression((u_x, u_y), degree=2, t=0.0)
        elif s=='top':
            u_y = f"-{inlet_vel}*(1.0 - pow((x[0]-{center})/({width}/2.0), 2))"
            u_x = f"{wobble_scale} * {inlet_vel} * sin({freq}*t)"
            val = Expression((u_x, u_y), degree=2, t=0.0)

        inlet_exprs.append(val)
        bcu.append(DirichletBC(V, val, boundaries, 1))

    bcp = [DirichletBC(Q, Constant(0.0), boundaries, 2)]

    # 5. Physics Setup
    u, v = TrialFunction(V), TestFunction(V)
    p, q = TrialFunction(Q), TestFunction(Q)

    u_n = Function(V)   # Previous Velocity
    u_  = Function(V)   # Tentative Velocity
    p_n = Function(Q)   # Previous Pressure
    p_  = Function(Q)   # New Pressure

    mu = Constant(geom_data['viscosity'])
    rho = Constant(1.0)
    n_vec = FacetNormal(mesh)

    # --- Smagorinsky LES Model ---
    # We define mu_eff (Effective Viscosity) as a Coefficient we update every step
    # This avoids recompiling the form
    mu_eff_fn = Function(DG)

    # Pre-calculate geometric constants for LES
    C_s = Constant(0.15)  # Smagorinsky constant
    h = CellDiameter(mesh)

    def S(u): return sym(nabla_grad(u))
    def sigma(u, p, visc): return 2*visc*S(u) - p*Identity(len(u))

    # Form 1: Tentative Velocity (Uses mu_eff_fn)
    U = 0.5*(u_n + u)
    F1 = rho*dot((u - u_n) / k, v)*dx + \
         rho*dot(dot(u_n, nabla_grad(u_n)), v)*dx + \
         inner(sigma(U, p_n, mu_eff_fn), S(v))*dx + \
         dot(p_n*n_vec, v)*ds - dot(mu_eff_fn*nabla_grad(U)*n_vec, v)*ds

    a1, L1 = lhs(F1), rhs(F1)

    # Form 2: Pressure (Constant LHS, Variable RHS)
    a2 = dot(nabla_grad(p), nabla_grad(q))*dx
    L2 = dot(nabla_grad(p_n), nabla_grad(q))*dx - (rho/k)*div(u_)*q*dx

    # Form 3: Correction
    a3 = dot(u, v)*dx
    L3 = dot(u_, v)*dx - k/rho * dot(nabla_grad(p_ - p_n), v)*dx

    # 6. Solvers
    # A2 and A3 are constant geometry, assemble ONCE
    A2 = assemble(a2)
    A3 = assemble(a3)

    solver1 = KrylovSolver("gmres", "ilu") # Step 1 changes (viscosity), re-assemble
    solver2 = KrylovSolver("gmres", "amg") # Step 2 constant matrix
    solver3 = KrylovSolver("cg", "sor")    # Step 3 constant matrix

    u_avg = Function(V); u_avg.vector().zero()
    count = 0; t = 0.0

    print(f"\n--- Starting LES Simulation ({num_steps} steps) ---")

    for step in range(num_steps):
        t += dt
        for expr in inlet_exprs: expr.t = t

        # 0. UPDATE TURBULENCE (Smagorinsky)
        # Calculate new viscosity based on current flow strain
        # nu_t = (Cs * h)^2 * |S(u_n)|
        strain_mag = sqrt(2 * inner(S(u_n), S(u_n)))
        viscosity_expr = mu + rho * (C_s * h)**2 * strain_mag

        # Project this onto the DG space for the solver to use
        # (Using local_project or just interpolate/project)
        # Simple projection is fast enough here
        mu_eff_fn.assign(project(viscosity_expr, DG, solver_type="cg", preconditioner_type="sor"))

        # 1. Tentative Velocity (Matrix A1 changes because mu_eff changed!)
        A1 = assemble(a1)
        b1 = assemble(L1)
        [bc.apply(A1, b1) for bc in bcu]
        solver1.solve(A1, u_.vector(), b1)

        # 2. Pressure (Reuse A2)
        b2 = assemble(L2)
        [bc.apply(A2, b2) for bc in bcp]
        solver2.solve(A2, p_.vector(), b2)

        # 3. Correction (Reuse A3)
        b3 = assemble(L3)
        [bc.apply(A3, b3) for bc in bcu]
        solver3.solve(A3, u_n.vector(), b3)

        p_n.assign(p_)

        # Average (Start after 1.5s to let turbulence develop)
        if t > 1.5:
            u_avg.vector().axpy(1.0, u_n.vector())
            count += 1

        if step % 20 == 0:
            # Print max velocity to ensure it's not blowing up
            max_v = u_n.vector().norm("linf")
            print(f"  Step {step}/{num_steps} (t={t:.2f}s) | Max V: {max_v:.2f}")

    if count > 0: u_avg.vector()[:] /= float(count)
    else: u_avg.assign(u_n)

    # 7. Export
    print("Interpolating...")
    res = geom_data.get('rect_grid_cell_size', 0.1)
    xmin, xmax = geom_data['x_min'], geom_data['x_max']
    ymin, ymax = geom_data['y_min'], geom_data['y_max']
    nx = int(np.round((xmax - xmin) / res)) + 1
    ny = int(np.round((ymax - ymin) / res)) + 1
    x_rng, y_rng = np.linspace(xmin, xmax, nx), np.linspace(ymin, ymax, ny)
    X, Y = np.meshgrid(x_rng, y_rng)

    Vx, Vy = np.zeros_like(X), np.zeros_like(Y)
    u_eval = u_avg
    u_eval.set_allow_extrapolation(True)

    for i in range(ny):
        for j in range(nx):
            try:
                val = u_eval(Point(X[i, j], Y[i, j]))
                Vx[i, j], Vy[i, j] = val[0], val[1]
            except: pass

    vel_mag = np.hypot(Vx, Vy)
    print(f"Max Average Vel: {np.max(vel_mag):.4f}")

    param_str = get_parameter_string(geom_data)
    header = f'{ny}_{nx}_{param_str}'
    vx_path = cache_dir / f"{scenario_name}_{args.hash}_Vx.txt"
    vy_path = cache_dir / f"{scenario_name}_{args.hash}_Vy.txt"
    img_path = cache_dir / f"{scenario_name}_{args.hash}_results.png"
    np.savetxt(vx_path, Vx, header=header)
    np.savetxt(vy_path, Vy, header=header)

    if np.max(vel_mag) > 1e-6:
        adapter = SfePyMeshAdapter(mesh)
        plot_results(adapter, X, Y, Vx, Vy, vel_mag, geom_data["obstacles"], str(img_path))
    else:
        from matplotlib import pyplot as plt
        plt.figure(); plt.text(0.5,0.5,"Zero Flow"); plt.savefig(str(img_path)); plt.close()

    for f in [f"temp_{args.hash}.msh", f"temp_{args.hash}.xdmf", f"temp_{args.hash}.h5"]:
        if os.path.exists(f): os.remove(f)

    print(f"Total Time: {time.time() - start_time:.2f} s")

if __name__ == '__main__':
    main()