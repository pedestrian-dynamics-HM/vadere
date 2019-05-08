import os
import shutil
import subprocess
import time

from vadereanalysistool import VadereProject
from vadereanalysistool import SameSeedTrajectory


def run_scenario_files_with_vadere_console(project, number_of_runs=3, vadere_console="VadereSimulator/target/vadere-console.jar",
                                           scenario_timeout_in_sec=12*60):

    if not os.path.exists(vadere_console):
        raise ValueError("vadere console could not be found at path: {}".format(os.path.abspath(vadere_console)))

    if not os.path.exists(vadere_console):
        raise ValueError("vadere console could not be found at path: {}".format(os.path.abspath(vadere_console)))

    scenario_files = project.scenario_files
    total_scenario_files = len(scenario_files)
    output_dir = project.output_path

    passed_scenarios = []
    failed_scenarios_with_exception = []
    for run in range(number_of_runs):
        for i, scenario_file in enumerate(scenario_files):
            try:
                scenario_name = os.path.basename(scenario_file)
                print("Running scenario file ({}/{}) of run {}/{}: {}".format(i + 1, total_scenario_files, run + 1, number_of_runs, scenario_name))

                # Measure wall time and not CPU time simply because it is the simplest method.
                wall_time_start = time.time()

                # Use timout feature, check return value and capture stdout/stderr to a PIPE (use
                # completed_process.stdout to get it).
                # print("subprocess call: {}".format(' '.join(
                #     ["java", "-enableassertions", "-jar", vadere_console, "scenario-run", "-f", scenario_file, "-o", output_dir])))
                completed_process = subprocess.run(
                    args=["java", "-enableassertions", "-jar", vadere_console, "scenario-run", "-f", scenario_file, "-o",
                          output_dir],
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


if __name__ == '__main__':
    # Note: the script is intended to run from [vadere_root_path]
    project = VadereProject('Tools/ContinuousIntegration/run_seed_comparison_test.d')
    project.clear_output_dir()
    scenario_return = run_scenario_files_with_vadere_console(project)
  
    if len(scenario_return["failed"]) > 0:
        raise RuntimeError("All scenarios have to pass for the seed test.")

    # this will reload project
    seed_test = SameSeedTrajectory('Tools/ContinuousIntegration/run_seed_comparison_test.d')
    seed_results = seed_test.get_trajectory_comparison_result()

    seed_err = [res for res in seed_results if not res['hash_is_equal']]
    seed_ok = [res for res in seed_results if  res['hash_is_equal']]

    if seed_err:
        for res in seed_err:
            print("Scenario {} with scenario-hash {} - ERROR".format(res['scenario_name'], res['scenario_hash']))
            for res_out in res['scenario_outputs']:
                print("   {}--{}".format(res_out[1], res_out[0]))

    if seed_ok:
        for res in seed_ok:
            print("Scenario {} with scenario-hash {} - OK".format(res['scenario_name'], res['scenario_hash']))


    print("###########")
    print("# Summary #")
    print("###########")
    print("")
    print("Total scenario runs: {}".format(len(seed_results)))
    print("Passed: {}".format(len(seed_ok)))
    print("Failed: {}".format(len(seed_err)))

    if len(seed_err) > 0:
        exit(1)
    else:
        exit(0)
