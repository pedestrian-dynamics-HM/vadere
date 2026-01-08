import time
import argparse
import sys
import numpy as np
import matplotlib.pyplot as plt
import pystencils as ps
import lbmpy

# --- 1. SETUP ---
from lbmpy.enums import Method, Stencil
from lbmpy.stencils import LBStencil

def get_d2q9_stencil():
    return LBStencil(Stencil.D2Q9)

from lbmpy.creationfunctions import create_lb_update_rule, create_lb_method
from helpers import extract_attributes, get_cache_dir, get_parameter_string

# --- 2. PHYSICS HELPERS ---
def extract_stencil_info(lb_method):
    """
    Extracts directions and creates the Bounce-Back Index Map.
    """
    stencil = lb_method.stencil
    dirs = np.array(stencil, dtype=np.int32)
    weights = np.array(lb_method.weights, dtype=np.float64)

    # Create Inverse Map: inv_indices[i] is the index of the direction opposite to i
    inv_indices = np.zeros(len(dirs), dtype=np.int32)
    for i, d in enumerate(dirs):
        inv_d = tuple(-1 * x for x in d)
        try:
            inv_indices[i] = stencil.index(inv_d)
        except ValueError:
            raise ValueError(f"Stencil Error: Direction {d} has no inverse in {stencil}")

    return dirs, weights, inv_indices

def get_equilibrium(rho, u, dirs, weights):
    """
    Robust Equilibrium (Handles Scalar Inlet & Field Simulation).
    """
    # u_sq: (nx, ny) or scalar
    u_sq = np.sum(u**2, axis=-1)

    # c_u: (nx, ny, 9) or (9,)
    c_u = np.dot(u, dirs.T)

    t1 = 3.0 * c_u
    t2 = 4.5 * c_u**2

    # Broadcast correction
    if u_sq.ndim > 0:
        t3 = 1.5 * u_sq[..., np.newaxis]
    else:
        t3 = 1.5 * u_sq

    if not np.isscalar(rho) and rho.ndim > 0:
        rho_term = rho[..., np.newaxis]
    else:
        rho_term = rho

    return weights * rho_term * (1.0 + t1 + t2 - t3)

def calc_macroscopic(pdfs, dirs):
    rho = np.sum(pdfs, axis=2)
    momentum = np.tensordot(pdfs, dirs, axes=(2, 0))
    rho_safe = np.maximum(rho, 1e-5)
    u = momentum / rho_safe[..., np.newaxis]
    return rho, u

# --- 3. ROBUST GEOMETRY ---
def rasterize_thick_line(mask, p1, p2, bounds, thickness=3.0):
    """
    Draws watertight walls [x, y].
    """
    nx, ny = mask.shape
    x_min, y_min, dx = bounds

    # Physical -> Grid
    gx1 = (p1[0] - x_min) / dx
    gy1 = (p1[1] - y_min) / dx
    gx2 = (p2[0] - x_min) / dx
    gy2 = (p2[1] - y_min) / dx

    # Bounding Box
    ix_min = max(0, int(min(gx1, gx2) - thickness))
    ix_max = min(nx, int(max(gx1, gx2) + thickness + 1))
    iy_min = max(0, int(min(gy1, gy2) - thickness))
    iy_max = min(ny, int(max(gy1, gy2) + thickness + 1))

    for x in range(ix_min, ix_max):
        for y in range(iy_min, iy_max):
            l2 = (gx2-gx1)**2 + (gy2-gy1)**2
            if l2 == 0:
                dist = np.hypot(x-gx1, y-gy1)
            else:
                t = ((x-gx1)*(gx2-gx1) + (y-gy1)*(gy2-gy1)) / l2
                t = max(0, min(1, t))
                proj_x = gx1 + t * (gx2-gx1)
                proj_y = gy1 + t * (gy2-gy1)
                dist = np.hypot(x-proj_x, y-proj_y)

            if dist <= thickness:
                mask[x, y] = True

# --- 4. MAIN ---
def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('scenario')
    parser.add_argument('hash')
    args = parser.parse_args()

    # --- GEOMETRY ---
    geom_data = extract_attributes(args.scenario)
    dx = geom_data['rect_grid_cell_size']
    x_min, x_max = geom_data['x_min'], geom_data['x_max']
    y_min, y_max = geom_data['y_min'], geom_data['y_max']

    # Layout: (nx, ny) -> (Width, Height)
    nx = int(np.round((x_max - x_min) / dx))
    ny = int(np.round((y_max - y_min) / dx))

    print(f"--- Initialization ---")
    print(f"Grid: {nx} x {ny} (Compatible Layout)")

    # Parameters
    u_phys = geom_data['inlet_velocity']
    nu_phys = geom_data['viscosity']

    u_lb = 0.04
    dt = (u_lb * dx) / u_phys
    nu_lb = nu_phys * dt / (dx**2)
    omega = 1.0 / (3.0 * nu_lb + 0.5)
    omega = min(omega, 1.95)

    print(f"Omega: {omega:.4f}")

    # --- KERNEL ---
    stencil = get_d2q9_stencil()
    # TRT is stable for walls
    try:
        lb_method = create_lb_method(method=Method.TRT, stencil=stencil, relaxation_rate=omega)
    except:
        lb_method = create_lb_method(method=Method.SRT, stencil=stencil, relaxation_rate=omega)

    # Compile
    update_rule = create_lb_update_rule(lb_method=lb_method, optimization={'cse_global': True})
    kernel = ps.create_kernel(update_rule, target=ps.Target.CPU).compile()

    # Get Stencil Info
    DIRS, WEIGHTS, INV_INDICES = extract_stencil_info(lb_method)

    # --- ARRAYS ---
    pdfs = np.zeros((nx, ny, 9), dtype=np.float64)
    pdfs_tmp = np.zeros_like(pdfs)

    for i, w in enumerate(WEIGHTS):
        pdfs[:, :, i] = w

    # --- WALL MASK ---
    wall_mask = np.zeros((nx, ny), dtype=bool)
    bounds = (x_min, y_min, dx)

    # 1. Obstacles (Thick)
    if geom_data['obstacles']:
        for obs in geom_data['obstacles']:
            pts = obs
            for i in range(len(pts)):
                p1 = pts[i]
                p2 = pts[(i+1)%len(pts)]
                rasterize_thick_line(wall_mask, p1, p2, bounds, thickness=2.5)

    # 2. Border (Watertight - 3 Cells Thick)
    # This prevents tunneling and periodic wrapping
    wall_mask[0:3, :] = True   # Left
    wall_mask[-3:, :] = True   # Right
    wall_mask[:, 0:3] = True   # Bottom
    wall_mask[:, -3:] = True   # Top

    # 3. Inlets/Outlets (Punch holes)
    inlets = []
    outlets = []

    def get_idx(val, start, limit):
        idx = int(round((val - start)/dx))
        return max(0, min(limit, idx))

    for feat in geom_data['inlets'] + geom_data['outlets']:
        is_inlet = (feat in geom_data['inlets'])
        side = feat['side']
        c_start, c_end = feat['coords']

        sl = None
        u_target = np.array([0.0, 0.0])

        # Slicing [x, y]
        if side == 'left': # x=0
            y_s, y_e = get_idx(c_start, y_min, ny), get_idx(c_end, y_min, ny)
            sl = (slice(0, 3), slice(y_s, y_e)) # Cut through thick wall
            u_target = np.array([u_lb, 0])
            neighbor = (slice(3, 4), slice(y_s, y_e))

        elif side == 'right': # x=max
            y_s, y_e = get_idx(c_start, y_min, ny), get_idx(c_end, y_min, ny)
            sl = (slice(nx-3, nx), slice(y_s, y_e))
            u_target = np.array([-u_lb, 0])
            neighbor = (slice(nx-4, nx-3), slice(y_s, y_e))

        elif side == 'bottom': # y=0
            x_s, x_e = get_idx(c_start, x_min, nx), get_idx(c_end, x_min, nx)
            sl = (slice(x_s, x_e), slice(0, 3))
            u_target = np.array([0, u_lb])
            neighbor = (slice(x_s, x_e), slice(3, 4))

        elif side == 'top': # y=max
            x_s, x_e = get_idx(c_start, x_min, nx), get_idx(c_end, x_min, nx)
            sl = (slice(x_s, x_e), slice(ny-3, ny))
            u_target = np.array([0, -u_lb])
            neighbor = (slice(x_s, x_e), slice(ny-4, ny-3))

        if sl:
            wall_mask[sl] = False
            if is_inlet:
                inlets.append({'slice': sl, 'u': u_target})
            else:
                outlets.append({'slice': sl, 'neighbor': neighbor})

    # --- SIMULATION ---
    print("Running Simulation...")
    max_steps = 60000
    ramp_steps = 2000

    for i in range(1, max_steps+1):
        # 1. Stream & Collide (Src -> Dst)
        kernel(src=pdfs, dst=pdfs_tmp)

        # 2. BOUNDARY CONDITIONS (On Dst)

        # A. BOUNCE BACK (The Physics Fix)
        # Any fluid that streamed into a wall node is reflected back.
        if np.any(wall_mask):
            # Optimized numpy slice flip
            # Takes particles at wall, flips them to inverse directions
            pdfs_tmp[wall_mask] = pdfs_tmp[wall_mask][:, INV_INDICES]

        # B. INLETS
        for inl in inlets:
            ramp = min(1.0, i/ramp_steps)
            u_cur = inl['u'] * ramp
            feq = get_equilibrium(1.0, u_cur, DIRS, WEIGHTS)
            pdfs_tmp[inl['slice']] = feq

        # C. OUTLETS
        for out in outlets:
            pdfs_tmp[out['slice']] = pdfs_tmp[out['neighbor']]

        # 3. Swap
        np.copyto(pdfs, pdfs_tmp)

        if i % 5000 == 0:
            print(f"Step {i}/{max_steps}...")

    # --- OUTPUT ---
    rho, u = calc_macroscopic(pdfs, DIRS)
    scale = u_phys / u_lb
    vx = u[..., 0] * scale
    vy = u[..., 1] * scale

    # Mask walls for clean plot (visual only)
    vx[wall_mask] = 0
    vy[wall_mask] = 0

    # Transpose for Plotting (nx, ny) -> (ny, nx)
    vx_plot = vx.T
    vy_plot = vy.T
    mask_plot = wall_mask.T

    cache_dir, scenario_name = get_cache_dir(args.scenario)
    hdr = f'{ny}_{nx}_{get_parameter_string(geom_data)}'

    np.savetxt(f"{cache_dir}/{scenario_name}_{args.hash}_Vx.txt", vx_plot, header=hdr)
    np.savetxt(f"{cache_dir}/{scenario_name}_{args.hash}_Vy.txt", vy_plot, header=hdr)

    # Plot
    x_range = np.linspace(x_min, x_max, nx)
    y_range = np.linspace(y_min, y_max, ny)
    X, Y = np.meshgrid(x_range, y_range)

    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(16, 6))

    vel_mag = np.sqrt(vx_plot**2 + vy_plot**2)
    vel_masked = np.ma.masked_where(mask_plot, vel_mag)

    # Streamlines     ax1.set_title("Streamlines")
    ax1.contourf(X, Y, vel_masked, levels=50, cmap='viridis', origin='lower')
    # Overlay Wall Mask to prove alignment
    ax1.imshow(mask_plot, extent=[x_min, x_max, y_min, y_max], origin='lower', cmap='binary', alpha=0.2)
    ax1.streamplot(X, Y, vx_plot, vy_plot, color='white', density=1.5, linewidth=0.6)

    # Vectors
    ax2.set_title("Vectors")
    ax2.contourf(X, Y, vel_masked, levels=50, cmap='viridis', origin='lower')
    skip = max(1, int(nx/35))
    ax2.quiver(X[::skip, ::skip], Y[::skip, ::skip],
               vx_plot[::skip, ::skip], vy_plot[::skip, ::skip],
               color='white', scale=None, alpha=0.7)

    for ax in [ax1, ax2]:
        for obs in geom_data['obstacles']:
            ax.fill(np.array(obs)[:,0], np.array(obs)[:,1], color='gray', alpha=0.5)
        ax.set_aspect('equal')
        ax.set_xlim(x_min, x_max)
        ax.set_ylim(y_min, y_max)

    out_img = f"{cache_dir}/{scenario_name}_{args.hash}_results.png"
    plt.savefig(out_img, dpi=150, bbox_inches='tight')
    print("Done.")

if __name__ == '__main__':
    main()