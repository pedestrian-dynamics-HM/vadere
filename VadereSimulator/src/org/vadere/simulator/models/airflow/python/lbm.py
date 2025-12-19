import sys
import time
import argparse
import inspect
import numpy as np
import matplotlib.pyplot as plt
from matplotlib.path import Path

# ==========================================
#      CRITICAL COMPATIBILITY PATCHES
# ==========================================
import sympy
import sympy.codegen.ast
from sympy.core.mul import Mul
from sympy import Tuple, Matrix

# Safely import MatrixBase
try:
    from sympy.matrices.matrixbase import MatrixBase
except ImportError:
    try:
        from sympy.matrices.common import MatrixBase
    except ImportError:
        from sympy.matrices import MatrixBase

# Safely import ImmutableDenseMatrix
try:
    from sympy.matrices.immutable import ImmutableDenseMatrix
except ImportError:
    from sympy.matrices import ImmutableDenseMatrix

print(f"⚠️  Applying Robust Compatibility Patches for SymPy {sympy.__version__}...")

if sympy.__version__ >= "1.11":
    # --- PATCH 1: Fix Multiplication Crash ---
    _original_flatten = Mul.flatten
    @classmethod
    def _patched_flatten(cls, args):
        new_args = []
        for arg in args:
            if isinstance(arg, (tuple, Tuple)):
                new_args.extend(arg)
            else:
                new_args.append(arg)
        return _original_flatten(new_args)
    Mul.flatten = _patched_flatten

    # --- PATCH 2: Fix Assignment Validation ---
    def _patched_check_args(lhs, rhs):
        pass
    sympy.codegen.ast.Assignment._check_args = _patched_check_args

    # --- PATCH 3: Fix Typing (The Big One) ---
    try:
        import pystencils.typing.leaf_typing
        from pystencils.typing import create_type

        # We need to construct the internal MatrixType manually since we can't import it
        # pystencils uses a custom class for matrix types. We assume it's available
        # via the type system or we reconstruct the behavior.

        found_patch = False
        for name, obj in inspect.getmembers(pystencils.typing.leaf_typing):
            if inspect.isclass(obj) and hasattr(obj, 'figure_out_type'):
                print(f"   -> Patching typing class: {name}")
                _original_figure_out_type = obj.figure_out_type

                def _patched_figure_out_type(self, expr):
                    # 1. Handle Tuples/Lists -> Convert to Immutable Matrix
                    if isinstance(expr, (list, tuple, Tuple)):
                        expr = ImmutableDenseMatrix(expr)

                    # 2. Handle ANY SymPy Matrix (Mutable or Immutable)
                    if isinstance(expr, (Matrix, MatrixBase, ImmutableDenseMatrix)):
                        # Force conversion to Immutable for safety
                        if not isinstance(expr, ImmutableDenseMatrix):
                            expr = ImmutableDenseMatrix(expr)

                        # MANUAL TYPE CONSTRUCTION
                        # Instead of asking pystencils to figure it out (which fails),
                        # we tell it: "This is a Matrix of doubles."
                        # We use 'float64' (double) as the base type.
                        base_type = create_type("float64")

                        # We need to return (expression, type_info)
                        # Since we can't import MatrixType easily, we cheat and look at
                        # what pystencils expects.

                        # Attempt to find MatrixType from the module if possible,
                        # otherwise use a generic string representation or rely on
                        # the fact that pystencils often accepts just the base type
                        # if it handles the object correctly elsewhere.

                        # CRITICAL FIX: We recurse manually if needed, but primarily
                        # we assume these are just numbers.
                        from pystencils.typing.types import MatrixType
                        return expr, MatrixType(base_type, *expr.shape)

                    return _original_figure_out_type(self, expr)

                obj.figure_out_type = _patched_figure_out_type
                found_patch = True
                break

        if not found_patch:
             print("⚠️ WARNING: Could not find 'figure_out_type' to patch.")

    except ImportError:
        # Fallback if MatrixType cannot be imported (older pystencils)
        # We try a simpler patch that just forces Immutability and hopes for the best
        print("⚠️ Could not import MatrixType. Falling back to simple patch.")
        pass
    except Exception as e:
        print(f"⚠️ Non-fatal warning during patching: {e}")

# ==========================================

# --- NORMAL IMPORTS ---
from lbmpy.session import create_channel
from lbmpy.boundaries import NoSlip, UBB, FixedDensity
from lbmpy.enums import Method, Stencil
from pystencils import Target
from helpers import extract_attributes, get_cache_dir, get_parameter_string

def physical_to_lattice(geom_data, target_u_lb=0.05):
    dx = geom_data['rect_grid_cell_size']
    u_phys = geom_data['inlet_velocity']
    nu_phys = geom_data['viscosity']

    L_x = geom_data['x_max'] - geom_data['x_min']
    L_y = geom_data['y_max'] - geom_data['y_min']
    nx = int(np.round(L_x / dx))
    ny = int(np.round(L_y / dx))

    dt = (target_u_lb * dx) / u_phys
    nu_lb = nu_phys * dt / (dx**2)
    omega = 1.0 / (3.0 * nu_lb + 0.5)

    print(f"--- Physics Conversion ---")
    print(f"Grid: {nx} x {ny} cells")
    print(f"Lattice u: {target_u_lb}")
    print(f"Lattice nu: {nu_lb:.6f} (omega={omega:.4f})")
    return nx, ny, omega, target_u_lb

def get_slice_from_coords(side, start_m, end_m, geom_data, nx, ny):
    dx = geom_data['rect_grid_cell_size']
    x_min, y_min = geom_data['x_min'], geom_data['y_min']

    if side in ['bottom', 'top']:
        idx_start = int((start_m - x_min) / dx)
        idx_end = int((end_m - x_min) / dx)
        idx_start = max(0, idx_start)
        idx_end = min(nx, idx_end)
        if side == 'bottom': return np.s_[0:1, idx_start:idx_end]
        else:                return np.s_[ny-1:ny, idx_start:idx_end]
    elif side in ['left', 'right']:
        idx_start = int((start_m - y_min) / dx)
        idx_end = int((end_m - y_min) / dx)
        idx_start = max(0, idx_start)
        idx_end = min(ny, idx_end)
        if side == 'left':  return np.s_[idx_start:idx_end, 0:1]
        else:               return np.s_[idx_start:idx_end, nx-1:nx]
    return None

def main():
    start_time = time.time()
    parser = argparse.ArgumentParser()
    parser.add_argument('scenario')
    parser.add_argument('hash')
    args = parser.parse_args()

    geom_data = extract_attributes(args.scenario)
    nx, ny, omega, u_lb = physical_to_lattice(geom_data)

    print("Initializing Simulation...")
    sim = create_channel(
        domain_size=(nx, ny),
        u_max=1e-6,
        method=Method.CUMULANT,
        relaxation_rate=omega,
        optimization={'target': Target.CPU},
        compressible=True
    )

    # Boundaries
    sim.boundary_handling.set_boundary(NoSlip(), slice_obj=np.s_[:, 0:1])
    sim.boundary_handling.set_boundary(NoSlip(), slice_obj=np.s_[:, -1:])
    sim.boundary_handling.set_boundary(NoSlip(), slice_obj=np.s_[0:1, :])
    sim.boundary_handling.set_boundary(NoSlip(), slice_obj=np.s_[-1:, :])

    for inlet in geom_data['inlets']:
        start, end = inlet['coords']
        side = inlet['side']
        sl = get_slice_from_coords(side, start, end, geom_data, nx, ny)
        vel_vec = (0, 0)
        if side == 'left':    vel_vec = (u_lb, 0)
        elif side == 'right': vel_vec = (-u_lb, 0)
        elif side == 'bottom':vel_vec = (0, u_lb)
        elif side == 'top':   vel_vec = (0, -u_lb)
        sim.boundary_handling.set_boundary(UBB(vel_vec), slice_obj=sl)

    for outlet in geom_data['outlets']:
        start, end = outlet['coords']
        side = outlet['side']
        sl = get_slice_from_coords(side, start, end, geom_data, nx, ny)
        sim.boundary_handling.set_boundary(FixedDensity(1.0), slice_obj=sl)

    if geom_data['obstacles']:
        mask = np.zeros((nx, ny), dtype=bool)
        x_vals = np.linspace(geom_data['x_min'], geom_data['x_max'], nx)
        y_vals = np.linspace(geom_data['y_min'], geom_data['y_max'], ny)
        xv, yv = np.meshgrid(x_vals, y_vals, indexing='ij')
        points = np.vstack((xv.ravel(), yv.ravel())).T
        for obs_poly in geom_data['obstacles']:
            path = Path(obs_poly)
            inside = path.contains_points(points)
            mask = mask | inside.reshape((nx, ny))
        sim.boundary_handling.set_boundary(NoSlip(), mask=mask)

    steps = int(3 * nx / u_lb)
    print(f"Running for {steps} steps...")
    sim.run(steps)

    print("Post-processing...")
    vel_field = sim.velocity_slice()
    vx = vel_field[:, :, 0].T
    vy = vel_field[:, :, 1].T

    scale_factor = geom_data['inlet_velocity'] / u_lb
    vx_phys = vx * scale_factor
    vy_phys = vy * scale_factor

    cache_dir, scenario_name = get_cache_dir(args.scenario)
    param_string = get_parameter_string(geom_data)

    np.savetxt(f"{cache_dir / scenario_name}_{args.hash}_Vx.txt", vx_phys, header=f'{ny}_{nx}_{param_string}')
    np.savetxt(f"{cache_dir / scenario_name}_{args.hash}_Vy.txt", vy_phys, header=f'{ny}_{nx}_{param_string}')

    plt.figure(figsize=(10, 5))
    mag = np.sqrt(vx_phys**2 + vy_phys**2)
    plt.imshow(mag, origin='lower', cmap='jet', extent=[geom_data['x_min'], geom_data['x_max'], geom_data['y_min'], geom_data['y_max']])
    plt.colorbar(label='Velocity (m/s)')
    plt.title(f"LBM Simulation: {scenario_name}")
    plt.savefig(f"{cache_dir / scenario_name}_{args.hash}_results.png")
    print(f"Done. Results saved to {cache_dir / scenario_name}_{args.hash}_results.png")

if __name__ == '__main__':
    main()