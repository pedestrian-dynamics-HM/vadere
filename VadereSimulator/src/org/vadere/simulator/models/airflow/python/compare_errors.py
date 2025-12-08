import numpy as np
import matplotlib.pyplot as plt
import argparse
import sys
import os

def load_velocity_field(scenario_path, scenario_hash):
    """Loads Vx and Vy from the generated text files."""
    vx_file = f"{scenario_path}_{scenario_hash}_Vx.txt"
    vy_file = f"{scenario_path}_{scenario_hash}_Vy.txt"

    if not os.path.exists(vx_file) or not os.path.exists(vy_file):
        print(f"Error: Could not find files for hash {scenario_hash}")
        print(f"Looking for: {vx_file}")
        sys.exit(1)

    # Load data (skipping header automatically handles the '#' issue)
    Vx = np.loadtxt(vx_file)
    Vy = np.loadtxt(vy_file)

    return Vx, Vy

def compute_l2_error(Vx_test, Vy_test, Vx_ref, Vy_ref):
    """
    Computes the relative L2 error norm between a test field and a reference field.
    Error = || u_test - u_ref || / || u_ref ||
    """
    # Difference vector
    diff_x = Vx_test - Vx_ref
    diff_y = Vy_test - Vy_ref

    # Square difference sum (Integrate over domain - approx by sum since dx is const)
    sum_diff_sq = np.sum(diff_x**2 + diff_y**2)

    # Reference sum
    sum_ref_sq = np.sum(Vx_ref**2 + Vy_ref**2)

    # Avoid division by zero
    if sum_ref_sq == 0:
        return 0.0

    return np.sqrt(sum_diff_sq / sum_ref_sq)

def main():
    parser = argparse.ArgumentParser(description='Compare convergence of multiple FEM runs.')
    parser.add_argument('--scenario', required=True, help='Path to scenario (without extension)')
    parser.add_argument('--hashes', required=True, nargs='+',
                        help='List of hashes ordered from COARSE to FINE. '
                             'The last hash is assumed to be the Ground Truth.')
    parser.add_argument('--areas', required=True, nargs='+', type=float,
                        help='List of area_thresholds corresponding to the hashes.')

    args = parser.parse_args()

    if len(args.hashes) != len(args.areas):
        print("Error: Number of hashes must match number of area thresholds.")
        sys.exit(1)

    # 1. Load the Reference Solution (The finest mesh)
    ref_hash = args.hashes[-1]
    ref_area = args.areas[-1]
    Vx_ref, Vy_ref = load_velocity_field(args.scenario, ref_hash)
    rows, cols = Vx_ref.shape
    print(f"Grid dimensions: {rows}x{cols}")

    results = []

    # 2. Compare Coarser meshes to Reference
    # We exclude the last one (reference) from the loop
    for i in range(len(args.hashes) - 1):
        curr_hash = args.hashes[i]
        curr_area = args.areas[i]
        Vx_test, Vy_test = load_velocity_field(args.scenario, curr_hash)

        # Sanity Check: Grids MUST be identical
        if Vx_test.shape != Vx_ref.shape:
            print(f"CRITICAL ERROR: Grid mismatch!")
            print(f"Ref: {Vx_ref.shape}, Test: {Vx_test.shape}")
            print("Ensure 'grid_size' was identical for all runs.")
            sys.exit(1)

        # Compute Error
        error_l2 = compute_l2_error(Vx_test, Vy_test, Vx_ref, Vy_ref)

        # Calculate approximate element size h ~ sqrt(Area)
        h = np.sqrt(curr_area)

        results.append({
            'area': curr_area,
            'h': h,
            'error': error_l2
        })

    # 3. Analyze Convergence Rates
    print(f"{'Area':<10} | {'h (approx)':<12} | {'Rel L2 Error':<15} | {'Rate (Order)':<12}")
    print("-" * 50)

    # Add the reference point for plotting purposes (Error = 0)
    # But for the table, we calculate rates between steps

    previous_h = None
    previous_err = None

    for res in results:
        h = res['h']
        err = res['error']

        rate_str = "-"
        if previous_h is not None:
            # Convergence Rate p = log(E1/E2) / log(h1/h2)
            # Avoid log(0) if error is super small
            if err > 1e-12:
                rate = np.log(previous_err / err) / np.log(previous_h / h)
                rate_str = f"{rate:.4f}"

        print(f"{res['area']:<10.4f} | {h:<12.4f} | {err:<15.6e} | {rate_str:<12}")

        previous_h = h
        previous_err = err

    # 4. Plot Error Map of the coarsest mesh vs finest
    # This helps identify WHERE the error is (usually boundaries/corners)
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