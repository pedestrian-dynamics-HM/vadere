# Use "vadere-console.jar", which is created by "mvn package", to run all
# scenario files under "VadereModelTests" subdirectory.
#
# Note: script contains some print statements so that progress can be tracked
# a little bit while script is running in continuous integration pipeline.

# Watch out: call this script from root directory of project. E.g.
#
#   python Tools/my_script.py

import fnmatch
import os
import re
import shutil
import subprocess
import time
import argparse


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

    return sorted(scenario_files)

def run_scenario_files_with_vadere_console(scenario_files, vadere_console="VadereSimulator/target/vadere-console.jar", scenario_timeout_in_sec=60):
    output_dir = "output"
    log_dir = ""

    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    total_scenario_files = len(scenario_files)

    passed_scenarios = []
    failed_scenarios_with_exception = []
    failed_summary = []

    for i, scenario_file in enumerate(scenario_files):

        try:
            print("Running scenario file ({}/{}): {}".format(i + 1, total_scenario_files, scenario_file))
            scenario_name = os.path.basename(scenario_file).split('.')[0]
            log_file = os.path.join(log_dir, "log_" + scenario_name + ".out")

            # Measure wall time and not CPU time simply because it is the simplest method.
            wall_time_start = time.time()

            # Use timout feature, check return value and capture stdout/stderr to a PIPE (use completed_process.stdout to get it).
            completed_process = subprocess.run(args=["java", "-enableassertions", "-jar", vadere_console,
                                                     "--logname", log_file, "scenario-run", "-f",
                                                     scenario_file, "-o", output_dir],
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
            failed_summary.append("Scenario file failed: {}".format(scenario_file))
            failed_summary.append("->  Reason: timeout after {} s".format(exception.timeout))
            failed_scenarios_with_exception.append((scenario_file, exception))
        except subprocess.CalledProcessError as exception:
            prefix = ""
            if "TestOSM" in scenario_file:
                prefix = " * OSM * "
            print(prefix + "Scenario file failed: {}".format(scenario_file))
            print("->  Reason: non-zero return value {}".format(exception.returncode))
            print("            {}".format(read_first_error_linies(exception.stderr)))
            failed_summary.append(prefix + "Scenario file failed: {}".format(scenario_file))
            failed_summary.append("->  Reason: non-zero return value {}".format(exception.returncode))
            failed_summary.append("            {}".format(read_first_error_linies(exception.stderr)))

            failed_scenarios_with_exception.append((scenario_file, exception))

    if os.path.exists(output_dir):
        shutil.rmtree(output_dir)

    return {"passed": passed_scenarios, "failed": failed_scenarios_with_exception, "failed_summary": failed_summary}

def read_first_error_linies(error_byte_string):
    err_string_lines = error_byte_string.decode('utf-8').split('\n')
    if len(err_string_lines) >= 2:
        return re.sub('\s+', ' ', ' '.join(err_string_lines[:2]))
    else:
        return 'unknown error see vadere log file.'


def print_summary(passed_and_failed_scenarios):
    total_passed_scenarios = len(passed_and_failed_scenarios["passed"])
    total_failed_scenarios = + len(passed_and_failed_scenarios["failed"])
    total_scenarios = total_passed_scenarios + total_failed_scenarios
    faild_summary = passed_and_failed_scenarios["failed_summary"]

    if len(faild_summary) > 0:
        print("#################")
        print("# Failed Summary #")
        print("#################")
        for line in faild_summary:
            print(line)


    print("###########")
    print("# Summary #")
    print("###########")
    print("")
    print("Total scenario runs: {}".format(total_scenarios))
    print("Passed: {}".format(total_passed_scenarios))
    print("Failed: {}".format(total_failed_scenarios))


def run_all():
    long_running_scenarios = [
        "basic_4_1_wall_gnm1",
        "queueing",
        "rimea_09",
        "rimea_11",
        "TestSFM",
        "thin_wall_and_closer_source_nelder_mead_ok",
        "thin_wall_and_closer_source_pso_could_fail",
        "rimea_04_flow_osm1_550_up",
        "stairs_diagonal_both_1_2_+1.scenario",
    ]

    excluded_scenarios = ["TESTOVM", "output", "legacy"]
    excluded_scenarios.extend(long_running_scenarios)

    scenario_files_regular_length = find_scenario_files(exclude_patterns=excluded_scenarios)
    passed_and_failed_scenarios = run_scenario_files_with_vadere_console(scenario_files_regular_length)

    for scenario in long_running_scenarios:
        search_pattern = "*" + scenario + "*.scenario"
        scenario_files_long = find_scenario_files(scenario_search_pattern=search_pattern)
        tmp_passed_and_failed_scenarios = run_scenario_files_with_vadere_console(scenario_files_long,
                                                                                 scenario_timeout_in_sec=480)
        passed_and_failed_scenarios["passed"].extend(tmp_passed_and_failed_scenarios["passed"])
        passed_and_failed_scenarios["failed"].extend(tmp_passed_and_failed_scenarios["failed"])
        passed_and_failed_scenarios["failed_summary"].extend(tmp_passed_and_failed_scenarios["failed_summary"])

    return passed_and_failed_scenarios


if __name__ == "__main__":

    parser = argparse.ArgumentParser(description='Run ModelTest')
    parser.add_argument('--scenario', '-s', nargs='?', default='', metavar='S', help='only run one selected scenario')
    args = vars(parser.parse_args())
    if args['scenario'] == '':
        passed_and_failed_scenarios = run_all()
    else:
        passed_and_failed_scenarios = run_scenario_files_with_vadere_console([args['scenario']])

    print_summary(passed_and_failed_scenarios)

    if len(passed_and_failed_scenarios["failed"]) > 0:
        exit(1)
    else:
        exit(0)
