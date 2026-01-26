import numpy as np
import matplotlib.pyplot as plt
import argparse
import sys
import os

def load_velocity_field(cache_path, scenario_name, scenario_hash):
    vx_file = f"{cache_path}/{scenario_name}_{scenario_hash}_Vx.txt"
    vy_file = f"{cache_path}/{scenario_name}_{scenario_hash}_Vy.txt"

    if not os.path.exists(vx_file) or not os.path.exists(vy_file):
        print(vx_file)
        print(f"Error: Could not find files for hash {scenario_hash}")
        print(f"Looking for: {vx_file}")
        sys.exit(1)

    Vx = np.loadtxt(vx_file)
    Vy = np.loadtxt(vy_file)

    return Vx, Vy

def compute_l2_error(Vx_test, Vy_test, Vx_ref, Vy_ref):
    """
    Error = || u_test - u_ref || / || u_ref ||
    """
    diff_x = Vx_test - Vx_ref
    diff_y = Vy_test - Vy_ref
    sum_diff_sq = np.sum(diff_x**2 + diff_y**2)
    sum_ref_sq = np.sum(Vx_ref**2 + Vy_ref**2)

    if sum_ref_sq == 0:
        return 0.0

    return np.sqrt(sum_diff_sq / sum_ref_sq)

def main():
    parser = argparse.ArgumentParser(description='Compare convergence of multiple FEM runs.')
    parser.add_argument('--cache_path', required=True, help='Path to cache folder')
    parser.add_argument('--scenario_name', required=True, help='Scenario name (without extension)')
    parser.add_argument('--hashes', required=True, nargs='+',
                        help='List of hashes ordered from COARSE to FINE. '
                             'The last hash is assumed to be the Ground Truth.')
    parser.add_argument('--edgelens', required=True, nargs='+', type=float,
                        help='List of max_triangle_edge_lens corresponding to the hashes.')
    args = parser.parse_args()
    if len(args.hashes) != len(args.edgelens):
        print("Error: Number of hashes must match number of area thresholds.")
        sys.exit(1)

    # load the reference solution (the finest mesh)
    ref_hash = args.hashes[-1]
    ref_edgelen = args.edgelens[-1]
    Vx_ref, Vy_ref = load_velocity_field(args.cache_path, args.scenario_name, ref_hash)
    rows, cols = Vx_ref.shape
    print(f"Grid dimensions: {rows}x{cols}")

    # compare coarser meshes to reference
    results = []
    for i in range(len(args.hashes) - 1):
        curr_hash = args.hashes[i]
        curr_edgelen = args.edgelens[i]
        Vx_test, Vy_test = load_velocity_field(args.cache_path, args.scenario_name, curr_hash)

        # check: grids must be identical
        if Vx_test.shape != Vx_ref.shape:
            print(f"CRITICAL ERROR: Grid mismatch!")
            print(f"Ref: {Vx_ref.shape}, Test: {Vx_test.shape}")
            print("Ensure 'rect_grid_cell_size' was identical for all runs.")
            sys.exit(1)

        error_l2 = compute_l2_error(Vx_test, Vy_test, Vx_ref, Vy_ref)
        #h = np.sqrt(curr_area) # approximate element size h
        results.append({
            #'area': curr_area,
            'h': curr_edgelen,
            'error': error_l2
        })

    print(f"{'triangle edge length':<12} | {'rel L2 error':<15} | {'convergence rate':<12}")
    print("-" * 50)

    # analyze convergence rates
    previous_h = None
    previous_err = None
    for res in results:
        h = res['h']
        err = res['error']

        rate_str = "-"
        if previous_h is not None:
            # convergence rate p = log(E1/E2) / log(h1/h2)
            if err > 1e-12:
                rate = np.log(previous_err / err) / np.log(previous_h / h)
                rate_str = f"{rate:.4f}"
        print(f"{h:<12.4f} | {err:<15.6e} | {rate_str:<12}")

        previous_h = h
        previous_err = err

    # plot error map of the coarsest mesh vs finest
    coarsest_hash = args.hashes[0]
    Vx_c, Vy_c = load_velocity_field(args.scenario, coarsest_hash)
    diff_mag = np.sqrt((Vx_c - Vx_ref)**2 + (Vy_c - Vy_ref)**2)
    #plt.figure(figsize=(10, 5))
    #plt.imshow(diff_mag, origin='lower', cmap='hot', interpolation='nearest')
    #plt.colorbar(label='Velocity Difference magnitude (m/s)')
    #plt.title(f"Error Distribution: Coarsest ({args.areas[0]}) vs Finest ({ref_area})")
    #plt.tight_layout()
    #plt.show()

if __name__ == "__main__":
    main()