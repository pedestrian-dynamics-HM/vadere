# Extract line and branch coverage (in percentage) from HTML coverage reports
# which are created by Maven's JaCoCo plugin.
# Use top-level pom.xml to search in correct subdirectories.
#
# Wach out: call this script from root directory of project. E.g.
#
#   python Tools/my_script.py

import xml.etree.ElementTree as ET

import os
import re

# Use Unix path separators because this script is usually run in a Bash-context.
# And Bash fails with Windows path separators which are introduced by os.path.join(...)
path_separator = "/"

def get_modules_from_pom_file(filename="pom.xml"):
    """Return a list of submodules which where found in passed "pom.xml"."""

    xml_name_spaces = {
            "default": "http://maven.apache.org/POM/4.0.0"
    }
    
    xml_tree = ET.parse(filename)
    xml_root = xml_tree.getroot()
    
    exclude_list = ["./VadereAnnotation", "./VadereGui"]
    
    modules = xml_root.findall("default:modules/default:module", xml_name_spaces)
    module_names = [module.text for module in modules if module.text not in exclude_list]
    
    return module_names

def extract_line_and_branch_coverage(module_names):
    """Return a dictionary which maps module name to line and branch coverage tuple."""

    module_to_coverage = dict()

    default_coverage_file = path_separator.join(["target", "coverage-reports", "index.html"])

    for module in module_names:
        coverage_path = path_separator.join([module, default_coverage_file])

        with open(coverage_path, "r") as file:
            coverage_report = file.read()
            
            # TODO: Regex seems be be broken on Windows CI worker. Find out why!
            # This is a somewhat dirty fix for windows OS (I leave the todo open, if someone knows a cleaner solution. 
            # When parsing the html, there appear unicode(?) issues and in this case whitespaces are represented as Â\xa0 
            # I think this has something to do with it:
            # https://stackoverflow.com/questions/13865346/why-would-a-python-regex-compile-on-linux-but-not-windows
            coverage_report = coverage_report.replace('Â\xa0', ' ') 

            regex_pattern = re.compile(r"Total.*?([0-9]{1,3})\s?%.*?([0-9]{1,3})\s?%")

            match = regex_pattern.search(coverage_report)

            if match:
                line_coverage = float(match.group(1))
                branch_coverage = float(match.group(2))
                module_to_coverage[module] = (line_coverage, branch_coverage)
            else:
                print(coverage_report)
                print(f"match={match}")
                raise Exception("Coverage data not found for module: {}".format(module))

    return module_to_coverage

def print_averaged_line_coverage(coverage_data):
    """GitLab CI tools read out the stdout output of the build process. Therefore, print coverage info to stdout."""

    total_modules = len(coverage_data)
    line_coverage_data = [line_coverage for (line_coverage, branch_coverage) in coverage_data.values()]
    branch_coverage_data = [branch_coverage for (line_coverage, branch_coverage) in coverage_data.values()]

    summed_line_coverage_data = sum(line_coverage_data)
    summed_branch_coverage_data = sum(branch_coverage_data)

    # By default, GitLab CI parses only integers.
    averaged_line_coverage = int(round(summed_line_coverage_data / total_modules, 0))
    averaged_branch_coverage = int(round(summed_branch_coverage_data / total_modules, 0))

    print("Analyzed modules: {}".format(sorted(coverage_data.keys())))
    print("Line Coverage: Total {}%".format(averaged_line_coverage))
    print("Branch Coverage: Total {}%".format(averaged_branch_coverage))

if __name__ == "__main__":
    module_names = get_modules_from_pom_file()
    module_to_coverage = extract_line_and_branch_coverage(module_names)
    print_averaged_line_coverage(module_to_coverage)
