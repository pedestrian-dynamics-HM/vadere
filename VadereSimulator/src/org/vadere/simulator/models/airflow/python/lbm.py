import time
import argparse
import sys
import numpy as np
import matplotlib.pyplot as plt
from matplotlib.path import Path

# --- 1. IMPORTS & COMPATIBILITY ---
import pystencils as ps
import lbmpy
from lbmpy.enums import Method, Stencil

# Stencil API-Switch
try:
    from lbmpy.stencils import LBStencil
    def get_d2q9_stencil(): return LBStencil(Stencil.D2Q9)
except ImportError:
    from lbmpy.stencils import get_stencil
    def get_d2q9_stencil(): return get_stencil("D2Q9")

# Method API-Switch
try:
    from lbmpy.creationfunctions import create_lb_update_rule, create_lb_method
except ImportError:
    from lbmpy.creationfunctions import create_lb_collision_rule as create_lb_update_rule
    try:
        from lbmpy.creationfunctions import create_lb_method
    except ImportError:
        from lbmpy.methods import create_lb_method

from helpers import extract_attributes, get_cache_dir, get_parameter_string

# --- 2. CONSTANTS ---
INV_D2Q9 = [0, 2, 1, 4, 3, 8, 7, 6, 5]
WEIGHTS = np.array([4/9, 1/9, 1/9, 1/9, 1/9, 1/36, 1/36, 1/36, 1/36])
DIRS = np.array([
    [0, 0], [0, 1], [0, -1], [-1, 0], [1, 0],
    [-1, 1], [1, 1], [-1, -1], [1, -1]
])

# --- 3. NUMERICS ---

def calc_macroscopic(pdfs):
    """ Manual calculation of Density (rho) and Velocity (u). """
    rho = np.sum(pdfs, axis=2)
    momentum = np.dot(pdfs, DIRS)
    rho_safe = np.maximum(rho, 1e-5)
    u = momentum / rho_safe[..., np.newaxis]
    return rho, u

def get_equilibrium(rho, u_x, u_y):
    """ Calculate BGK Equilibrium Distribution. """
    u_sq = u_x**2 + u_y**2
    c_u = (DIRS[:, 0] * u_x + DIRS[:, 1] * u_y)
    t1 = 3.0 * c_u
    t2 = 4.5 * c_u**2
    t3 = 1.5 * u_sq
    return WEIGHTS * rho * (1.0 + t1 + t2 - t3)

# --- 4. ROBUST GEOMETRY ENGINE ---

def rasterize_obstacle_thick(wall_mask, obstacle_points, dx, x_min, y_min):
    """
    Rasterizes obstacles with a 'THICK BRUSH' to prevent diagonal leakage.
    Instead of marking 1 cell, we mark a 3x3 block around every point.
    """
    nx, ny = wall_mask.shape
    pts = np.array(obstacle_points)
    num_pts = len(pts)

    # Iterate over perimeter edges
    for i in range(num_pts):
        p1 = pts[i]
        p2 = pts[(i + 1) % num_pts]

        dist = np.linalg.norm(p2 - p1)
        if dist < 1e-9: continue

        # High-density sampling (Step size = 1/3 of a cell)
        steps = max(5, int(np.ceil(dist / (dx * 0.33))))

        xs = np.linspace(p1[0], p2[0], steps)
        ys = np.linspace(p1[1], p2[1], steps)

        # Convert to grid indices
        ixs = np.round((xs - x_min) / dx).astype(int)
        iys = np.round((ys - y_min) / dx).astype(int)

        # Clip to domain
        ixs = np.clip(ixs, 0, nx - 1)
        iys = np.clip(iys, 0, ny - 1)

        # --- THE THICKENING FIX ---
        # Mark the center cell AND neighbors (3x3 block)
        # This guarantees that the wall is 'watertight' for D2Q9
        for dx_i in [-1, 0, 1]:
            for dy_i in [-1, 0, 1]:
                # Shifted indices
                xi = np.clip(ixs + dx_i, 0, nx - 1)
                yi = np.clip(iys + dy_i, 0, ny - 1)
                wall_mask[xi, yi] = True

# --- 5. BOUNDARIES ---

def apply_wall_bounce_back(pdfs, mask):
    """ Reflects particles at walls (No-Slip). """
    if not np.any(mask): return
    wall_cells = pdfs[mask]
    pdfs[mask] = wall_cells[:, INV_D2Q9]

def apply_inlet(pdfs, slice_obj, u_target_x, u_target_y, current_step, ramp_steps):
    """ Soft start (Ramp-Up). """
    factor = 1.0
    if current_step < ramp_steps:
        factor = current_step / float(ramp_steps)

    feq = get_equilibrium(1.0, u_target_x * factor, u_target_y * factor)
    pdfs[slice_obj] = feq

def apply_outlet(pdfs, slice_obj, neighbor_slice):
    """ Zero-gradient outlet. """
    pdfs[slice_obj] = pdfs[neighbor_slice]

# --- 6. PLOTTING ---

def plot_lbm_results(X, Y, Vx, Vy, vel_mag, obstacles, wall_mask, path):
    """ Plots Grid/Geometry, Streamlines, and Vectors side-by-side. """
    x_min, x_max = np.min(X), np.max(X)
    y_min, y_max = np.min(Y), np.max(Y)

    # 2-Panel Plot
    domain_width = x_max - x_min
    domain_height = y_max - y_min
    aspect = domain_width / (domain_height + 1e-5)
    plot_height = 8
    plot_width = plot_height * aspect
    fig_width = plot_width * 2

    fig, axes = plt.subplots(1, 2,
                             figsize=(fig_width, plot_height),
                             sharex=True, sharey=True,
                             constrained_layout=True)
    ax_stream, ax_quiver = axes

    def draw_obstacles(ax):
        for obs in obstacles:
            obs_arr = np.array(obs)
            ax.fill(obs_arr[:, 0], obs_arr[:, 1], color='grey', alpha=1.0, zorder=10)

    # --- Plot 1: Streamlines ---
    levels = np.linspace(0, np.max(vel_mag), 50)
    cf1 = ax_stream.contourf(X, Y, vel_mag, levels=levels, cmap='viridis')

    # Fix for 'rows of x must be equal' -> Use standard meshgrid
    ax_stream.streamplot(X, Y, Vx, Vy, color='white', linewidth=0.5,
                         density=1.5, arrowsize=1, arrowstyle='->')

    draw_obstacles(ax_stream)
    # Overlay the actual LBM wall mask to debug leakage visually
    # We plot the mask as a faint red overlay
    ax_stream.imshow(wall_mask.T, origin='lower', extent=[x_min, x_max, y_min, y_max],
                     cmap='Reds', alpha=0.3, zorder=5)

    ax_stream.set_title("Streamlines (Red=LBM Wall)")
    cb1 = fig.colorbar(cf1, ax=ax_stream, location='bottom', fraction=0.05, pad=0.05)
    cb1.set_label('Velocity (m/s)')

    # --- Plot 2: Vectors ---
    cf2 = ax_quiver.contourf(X, Y, vel_mag, levels=levels, cmap='viridis')
    skip = max(1, int(len(X[0])/40))
    ax_quiver.quiver(X[::skip, ::skip], Y[::skip, ::skip],
                     Vx[::skip, ::skip], Vy[::skip, ::skip],
                     color='white', scale=None, width=0.003, alpha=0.8)
    draw_obstacles(ax_quiver)
    ax_quiver.set_title("Velocity Vectors")
    cb2 = fig.colorbar(cf2, ax=ax_quiver, location='bottom', fraction=0.05, pad=0.05)
    cb2.set_label('Velocity (m/s)')

    for ax in axes:
        ax.set_xlabel("x (m)")
        ax.set_ylabel("y (m)")
        ax.set_aspect('equal', adjustable='box')
        ax.set_xlim(x_min, x_max)
        ax.set_ylim(y_min, y_max)

    print(f"Saving plot to {path}")
    plt.savefig(path)
    plt.close()

# --- 7. SETUP ---

def physical_to_lattice(geom_data, target_u_lb=0.05):
    dx = geom_data['rect_grid_cell_size']
    u_phys = geom_data['inlet_velocity']
    nu_phys = geom_data['viscosity']

    L_x = geom_data['x_max'] - geom_data['x_min']
    nx = int(np.round(L_x / dx))
    ny = int(np.round((geom_data['y_max'] - geom_data['y_min']) / dx))

    dt = (target_u_lb * dx) / u_phys
    nu_lb = nu_phys * dt / (dx**2)
    omega = 1.0 / (3.0 * nu_lb + 0.5)

    print(f"\n--- Parameter Check ---")
    print(f"  Theoretical Omega: {omega:.4f}")

    MAX_OMEGA = 1.9
    if omega > MAX_OMEGA:
        print(f"WARNING: Omega {omega:.4f} is too high. Clamping to {MAX_OMEGA}.")
        omega = MAX_OMEGA

    print(f"  Final Parameters: nx={nx}, ny={ny}, omega={omega:.4f}")
    return nx, ny, omega, target_u_lb, dt

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('scenario')
    parser.add_argument('hash')
    args = parser.parse_args()

    geom_data = extract_attributes(args.scenario)
    nx, ny, omega, u_lb, dt = physical_to_lattice(geom_data, target_u_lb=0.05)

    # --- KERNEL ---
    stencil = get_d2q9_stencil()
    print("Configuring Solver...")
    try:
        lb_method = create_lb_method(method=Method.TRT, stencil=stencil,
                                     relaxation_rate=omega, compressible=True)
        print(" -> Method: TRT")
    except Exception:
        lb_method = create_lb_method(method=Method.SRT, stencil=stencil,
                                     relaxation_rate=omega, compressible=True)
        print(" -> Method: SRT")

    update_rule = create_lb_update_rule(lb_method=lb_method, optimization={'cse_global': True})
    kernel = ps.create_kernel(update_rule, target=ps.Target.CPU).compile()

    # --- ARRAYS ---
    pdfs = np.zeros((nx, ny, 9), dtype=np.float64)
    pdfs_tmp = np.zeros_like(pdfs)
    pdfs[:] = WEIGHTS

    # --- GEOMETRY (THICKENED) ---
    print("Rasterizing Obstacles (Thick Brush)...")
    wall_mask = np.zeros((nx, ny), dtype=bool)

    # 1. Domain
    wall_mask[0, :] = True; wall_mask[-1, :] = True
    wall_mask[:, 0] = True; wall_mask[:, -1] = True

    inlets, outlets = [], []

    def get_slice(side, start, end):
        dx = geom_data['rect_grid_cell_size']
        def idx(v): return max(0, min(nx, int(round((v - geom_data['x_min'])/dx))))
        def idy(v): return max(0, min(ny, int(round((v - geom_data['y_min'])/dx))))
        ix_s, ix_e = idx(start), idx(end)
        iy_s, iy_e = idy(start), idy(end)

        if side == 'left':   return (slice(0, 1), slice(iy_s, iy_e))
        if side == 'right':  return (slice(nx-1, nx), slice(iy_s, iy_e))
        if side == 'bottom': return (slice(ix_s, ix_e), slice(0, 1))
        if side == 'top':    return (slice(ix_s, ix_e), slice(ny-1, ny))
        return None

    # Inlets/Outlets
    for inlet in geom_data['inlets']:
        sl = get_slice(inlet['side'], inlet['coords'][0], inlet['coords'][1])
        if sl:
            wall_mask[sl] = False # Clear wall at inlet
            vx, vy = 0, 0
            if inlet['side'] == 'left': vx = u_lb
            elif inlet['side'] == 'right': vx = -u_lb
            elif inlet['side'] == 'bottom': vy = u_lb
            elif inlet['side'] == 'top': vy = -u_lb
            inlets.append({'slice': sl, 'u': (vx, vy)})

    for outlet in geom_data['outlets']:
        sl = get_slice(outlet['side'], outlet['coords'][0], outlet['coords'][1])
        if sl:
            wall_mask[sl] = False # Clear wall at outlet
            sx, sy = sl
            if outlet['side'] == 'left': nx_s, ny_s = slice(1, 2), sy
            elif outlet['side'] == 'right': nx_s, ny_s = slice(nx-2, nx-1), sy
            elif outlet['side'] == 'bottom': nx_s, ny_s = sx, slice(1, 2)
            elif outlet['side'] == 'top': nx_s, ny_s = sx, slice(ny-2, ny-1)
            outlets.append({'slice': sl, 'neighbor': (nx_s, ny_s)})

    # 2. OBSTACLES WITH THICK RASTERIZATION
    if geom_data['obstacles']:
        dx = geom_data['rect_grid_cell_size']
        x_min, y_min = geom_data['x_min'], geom_data['y_min']

        # A. Fill Interiors
        x_v = np.linspace(x_min, geom_data['x_max'], nx)
        y_v = np.linspace(y_min, geom_data['y_max'], ny)
        xv, yv = np.meshgrid(x_v, y_v, indexing='ij')
        points = np.vstack((xv.ravel(), yv.ravel())).T

        for obs in geom_data['obstacles']:
            # Fill Volume
            inside = Path(obs).contains_points(points).reshape((nx, ny))
            wall_mask = wall_mask | inside

            # Thick Perimeter Brush
            rasterize_obstacle_thick(wall_mask, obs, dx, x_min, y_min)

    # --- MAIN LOOP ---
    print("Starting Simulation...")
    max_steps = 60000
    check_interval = 2000
    tol = 1e-5
    last_energy = 0.0
    use_src_dst = False
    ramp_steps = 2000

    start_time = time.time()

    for i in range(1, max_steps + 1):
        try:
            if not use_src_dst: kernel(pdfs=pdfs, pdfs_tmp=pdfs_tmp)
            else: kernel(src=pdfs, dst=pdfs_tmp)
        except TypeError:
            use_src_dst = True
            kernel(src=pdfs, dst=pdfs_tmp)

        apply_wall_bounce_back(pdfs_tmp, wall_mask)

        for inl in inlets:
            apply_inlet(pdfs_tmp, inl['slice'], inl['u'][0], inl['u'][1],
                       current_step=i, ramp_steps=ramp_steps)
        for out in outlets:
            apply_outlet(pdfs_tmp, out['slice'], out['neighbor'])

        np.copyto(pdfs, pdfs_tmp)

        if i % check_interval == 0:
            rho, u = calc_macroscopic(pdfs)
            eng = np.sum(u**2)
            if np.isnan(eng) or eng > 1e15:
                print(f"CRITICAL: Instability at step {i}!")
                break
            diff = abs(eng - last_energy)
            rel_err = diff / (eng + 1e-10)
            print(f"Step {i}: Energy={eng:.4e}, Rel.Err={rel_err:.6e}")
            if rel_err < tol and i > ramp_steps:
                print(f"--> Converged (Step {i})")
                break
            last_energy = eng

    # --- EXPORT ---
    print("Exporting results...")
    rho, u = calc_macroscopic(pdfs)
    scale = geom_data['inlet_velocity'] / u_lb
    vx_phys = u[..., 0] * scale
    vy_phys = u[..., 1] * scale

    vx_export = vx_phys.T
    vy_export = vy_phys.T

    cache_dir, scenario_name = get_cache_dir(args.scenario)
    hdr = f'{ny}_{nx}_{get_parameter_string(geom_data)}'

    np.savetxt(f"{cache_dir}/{scenario_name}_{args.hash}_Vx.txt", vx_export, header=hdr)
    np.savetxt(f"{cache_dir}/{scenario_name}_{args.hash}_Vy.txt", vy_export, header=hdr)

    # --- PLOT ---
    print("Generating Plots...")
    # Use standard 'xy' indexing for Matplotlib
    x_rng = np.linspace(geom_data['x_min'], geom_data['x_max'], nx)
    y_rng = np.linspace(geom_data['y_min'], geom_data['y_max'], ny)
    X, Y = np.meshgrid(x_rng, y_rng)

    # Calculate Velocity Magnitude
    vel_mag_plot = np.sqrt(vx_export**2 + vy_export**2)

    # Mask Velocity inside obstacles for cleaner plot
    # We transpose wall_mask to match plot coords (ny, nx)
    wall_mask_plot = wall_mask.T
    # Set velocity to 0 where wall is present
    vel_mag_plot[wall_mask_plot] = 0.0
    vx_export[wall_mask_plot] = 0.0
    vy_export[wall_mask_plot] = 0.0

    out_img = f"{cache_dir}/{scenario_name}_{args.hash}_results.png"

    plot_lbm_results(X, Y, vx_export, vy_export, vel_mag_plot,
                     geom_data['obstacles'], wall_mask, out_img)

    print(f"Finished in {time.time()-start_time:.2f}s")

if __name__ == '__main__':
    main()