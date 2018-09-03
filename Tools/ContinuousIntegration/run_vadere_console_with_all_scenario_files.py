# Use "vadere-console.jar", which is created by "mvn package", to run all
# scenario files under "VadereModelTests" subdirectory.
#
# Note: script contains some print statements so that progress can be tracked
# a little bit

# Wach out: call this script from root directory of project. E.g.
#
#   python Tools/my_script.py

import fnmatch
import os
import re
import shutil
import subprocess
import time

def find_scenario_files(path="VadereModelTests", scenario_search_pattern = "*.scenario", exclude_patterns = ["TESTOVM", "output","legacy"]):
    scenario_files = []

    for root, dirnames, filenames in os.walk(path):
        for filename in fnmatch.filter(filenames, scenario_search_pattern):
            scenario_path = os.path.join(root, filename)
            scenario_path_excluded = False

            for exclude_pattern in exclude_patterns:
                regex_pattern = re.compile(exclude_pattern)
                match = regex_pattern.search(scenario_path)
                if match:
                    scenario_path_excluded = True

            if scenario_path_excluded == False:
                scenario_files.append(scenario_path)

    print("Total scenario files: {}".format(len(scenario_files)))
    # print("Exclude patterns: {}".format(exclude_patterns))

    return sorted(scenario_files)

def run_scenario_files_with_vadere_console(scenario_files, vadere_console="VadereGui/target/vadere-console.jar", scenario_timeout_in_sec=60):
    output_dir = "output"

    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    total_scenario_files = len(scenario_files)

    passed_scenarios = []
    failed_scenarios_with_exception = []

    for i, scenario_file in enumerate(scenario_files):
        try:
            # Surpress this output to print only failed scenarios.
            # print("Running scenario file ({}/{}): {}".format(i + 1, total_scenario_files, scenario_file))

            # Measure wall time and not cpu because it is the easiest.
            wall_time_start = time.time()


            # Use timout feature, check return value and capture stdout/stderr to a PIPE (use completed_process.stdout to get it).
            completed_process = subprocess.run(args=["java", "-enableassertions", "-jar", vadere_console, "scenario-run", "-f", scenario_file, "-o", output_dir],
                                           timeout=scenario_timeout_in_sec,
                                           check=True,
                                           stdout=subprocess.PIPE,
                                           stderr=subprocess.PIPE)

            wall_time_end = time.time()
            wall_time_delta = wall_time_end - wall_time_start

            # Surpress this output to print only failed scenarios.
            # print("Finished scenario file ({:.1f} s): {}".format(wall_time_delta, scenario_file))

            passed_scenarios.append(scenario_file)
        except subprocess.TimeoutExpired as exception:
            prefix = ""
            if "TestOSM" in scenario_file:
                prefix = " * OSM * "

            print(prefix + "Scenario file failed: {}".format(scenario_file))
            print("->  Reason: timeout after {} s".format(exception.timeout))
            failed_scenarios_with_exception.append((scenario_file, exception))
        except subprocess.CalledProcessError as exception:
            prefix = ""
            if "TestOSM" in scenario_file:
                prefix = " * OSM * "
            print(prefix + "Scenario file failed: {}".format(scenario_file))
            print("->  Reason: non-zero return value {}".format(exception.returncode))
            failed_scenarios_with_exception.append((scenario_file, exception))


    if os.path.exists(output_dir):
        shutil.rmtree(output_dir)

    return {"passed": passed_scenarios, "failed": failed_scenarios_with_exception}

if __name__ == "__main__":
    print("Output is only shown for failed scenarios!")

    long_running_scenarios = ["TestSFM", "rimea_09", "rimea_11", "queueing"]
    excluded_scenarios = ["TESTOVM", "output", "legacy"]
    excluded_scenarios.extend(long_running_scenarios)

    scenario_files_regular_length = find_scenario_files(exclude_patterns=excluded_scenarios)
    passed_and_failed_scenarios = run_scenario_files_with_vadere_console(scenario_files_regular_length)

    for scenario in long_running_scenarios:
        search_pattern = "*" + scenario + "*.scenario"
        scenario_files_long = find_scenario_files(scenario_search_pattern=search_pattern)
        tmp_passed_and_failed_scenarios = run_scenario_files_with_vadere_console(scenario_files_long, scenario_timeout_in_sec=480)
        passed_and_failed_scenarios["passed"].extend(tmp_passed_and_failed_scenarios["passed"])
        passed_and_failed_scenarios["failed"].extend(tmp_passed_and_failed_scenarios["failed"])


    if len(passed_and_failed_scenarios["failed"]) > 0:
        exit(1)
    else:
        exit(0)
