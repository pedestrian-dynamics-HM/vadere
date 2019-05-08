import os
import shutil
import subprocess
import time

from vadereanalysistool import VadereProject
from vadereanalysistool.analysis import OriginDeviation


def run_scenario_files_with_vadere_console(scenario_files, vadere_console="VadereSimulator/target/vadere-console.jar",
                                           scenario_timeout_in_sec=60):
    output_dir = "Tools/ContinuousIntegration/run_orign_translation_test.d/output"

    if os.path.exists(output_dir):
        shutil.rmtree(output_dir)

    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    total_scenario_files = len(scenario_files)

    passed_scenarios = []
    failed_scenarios_with_exception = []

    for i, scenario_file in enumerate(scenario_files):
        try:
            print("Running scenario file ({}/{}): {}".format(i + 1, total_scenario_files, scenario_file))

            # Measure wall time and not CPU time simply because it is the simplest method.
            wall_time_start = time.time()

            out = os.path.join(output_dir, os.path.basename(scenario_file).split('.')[0])

            # Use timout feature, check return value and capture stdout/stderr to a PIPE (use
            # completed_process.stdout to get it).
            print("subprocess call: {}".format(' '.join(
                ["java", "-enableassertions", "-jar", vadere_console, "scenario-run", "-f", scenario_file, "-o", out])))
            completed_process = subprocess.run(
                args=["java", "-enableassertions", "-jar", vadere_console, "scenario-run", "-f", scenario_file, "-o",
                      out, "--override-timestep-setting"],
                timeout=scenario_timeout_in_sec,
                check=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE)

            wall_time_end = time.time()
            wall_time_delta = wall_time_end - wall_time_start

            print("Finished scenario file ({:.1f} s): {}".format(wall_time_delta, scenario_file))

            passed_scenarios.append(scenario_file)
        except subprocess.TimeoutExpired as exception:
            print("Scenario file failed: {}".format(scenario_file))
            print("->  Reason: timeout after {} s".format(exception.timeout))
            failed_scenarios_with_exception.append((scenario_file, exception))
        except subprocess.CalledProcessError as exception:
            print("Scenario file failed: {}".format(scenario_file))
            print("->  Reason: non-zero return value {}".format(exception.returncode))
            failed_scenarios_with_exception.append((scenario_file, exception))

    return {"passed": passed_scenarios, "failed": failed_scenarios_with_exception}


def get_output_pairs(path="Tools/ContinuousIntegration/run_orign_translation_test.d/output"):
    _output_pairs = []
    output_without_offset = [d for d in os.listdir(path) if d.endswith("without_offset")]
    for dir_without in output_without_offset:
        dir_with_offset = dir_without.replace("without_offset", "with_offset")
        name = dir_with_offset[:-len("without_offset")]
        pair = [os.path.join(path, dir_without), os.path.join(path, dir_with_offset)]
        _output_pairs.append(OriginDeviation(pair, name))

    return _output_pairs


def run_simulations(test_project_path):
    vadere_project = VadereProject(test_project_path)
    scenario_files = vadere_project.scenario_files
    passed_and_failed_scenarios = run_scenario_files_with_vadere_console(scenario_files)

    if len(passed_and_failed_scenarios['failed']) > 0:
        print("Error in simulation runs: " + passed_and_failed_scenarios['failed'])
        exit(1)


if __name__ == '__main__':
    test_project_path = "Tools/ContinuousIntegration/run_orign_translation_test.d"

    run_simulations(test_project_path)

    output_pairs = get_output_pairs()
    print("comparing {} output pairs".format(len(output_pairs)))
    # results = [pair.get_origin_deviation_result() for pair in output_pairs]
    return_val = 0
    space_offset = ''
    all_results_count = len(output_pairs)
    err_results_count = 0
    warn_results_count = 0
    ok_results_coutn = 0

    for pair in output_pairs:
        result = pair.get_origin_deviation_result()
        print("*** compare trajectories for {} ... ".format(result['output_name']), end='')
        if result['err_count'] > 0:
            err_results_count += 1
            print("ERROR")
            return_val = 1
            print("    Error: Found {} pedestrians with maximal diff bigger than {}.".format(result['err_count'],
                                                                                             pair.max_diff_warn))
            print("    First occurrence of error for pedestrian at timestep \n    [pedestrianId, timeStep, diff]")
            for i, (row_id, row) in enumerate(result['err_dict'].items()):
                if i > 2:
                    print("    ...")
                    break
                print("    [{}, {}, {}]".format(row['pedestrianId'], row['timeStep'], row['diff']))

        if result['warn_count'] > 0:
            if result['err_count'] == 0:
                warn_results_count += 1
                print("WARN")
            print("    Warning: Found {} pedestrians with maximal diff between {} and {}.".format(result['warn_count'],
                                                                                                  pair.max_diff_ok,
                                                                                                  pair.max_diff_warn))
            print("    First occurrence of warning for pedestrian at \n    [pedestrianId, timeStep, diff]")
            for i, (row_id, row) in enumerate(result['warn_dict'].items()):
                if i > 2:
                    print("    ...")
                    break
                print("    [{}, {}, {}]".format(row['pedestrianId'], row['timeStep'], row['diff']))

        if result['err_count'] == 0 and result['warn_count'] == 0:
            ok_results_coutn += 1
            print("OK")

        if result['err_count'] > 0 or result['warn_count'] > 0:
            stats_at_max_diff = result['stats_at_max_diff']
            print("    Max diff Stats      | mean: {} std: {}, median: {}, min: {}, max: {}".format(
                stats_at_max_diff['mean'], stats_at_max_diff['std'], stats_at_max_diff['50%'],
                stats_at_max_diff['min'], stats_at_max_diff['max']
            ))

            stats_at_start = result['stats_at_start']
            print("    Start position Stats| mean: {} std: {}, median: {}, min: {}, max: {}".format(
                stats_at_start['mean'], stats_at_start['std'], stats_at_start['50%'],
                stats_at_start['min'], stats_at_start['max']
            ))


    print("###########")
    print("# Summary #")
    print("###########")
    print("")
    print("Total scenario runs: {}".format(all_results_count))
    print("Passed: {}".format(ok_results_coutn))
    print("Warning: {}".format(warn_results_count))
    print("Failed: {}".format(err_results_count))
    exit(return_val)
