import os


def run_vadere(scenario_file_path, output_path):
    systemCommand = "java -jar vadere-console.jar " + scenario_file_path + " " + output_path + " -suq"
    status = os.system(systemCommand)

    if status != 0:
        print("Error: VadereConsole exited with return code: %d" % status)
        exit(1)

    return status