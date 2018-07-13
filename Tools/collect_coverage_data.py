# Collect HTML coverage reports (created by Maven's jacoco plugin) and average the result of all moduls.
# Use top-level pom.xml to look into correct subdirectories.
#
# Wach out: call this script from root directory of project. E.g.
#
#   python Tools/my_script.py

import xml.etree.ElementTree as ET

import os
import re

def get_modules_from_pom_file(filename="pom.xml"):
    xml_name_spaces = {
            "default": "http://maven.apache.org/POM/4.0.0"
    }
    
    xml_tree = ET.parse(filename)
    xml_root = xml_tree.getroot()
    
    exclude_list = ["VadereAnnotation"]
    
    modules = xml_root.findall("default:modules/default:module", xml_name_spaces)
    module_names = [module.text for module in modules if module.text not in exclude_list]
    
    return module_names

def read_coverage_data_for_modules(module_names):
        module_to_coverage = dict()

        default_coverage_file = "target/site/coverage-reports/index.html"

        for module in module_names:
            coverage_path = os.path.join(module, default_coverage_file)

            with open(coverage_path, "r") as file:
                coverage_report = file.read()

                regex_pattern = re.compile(r"Total.*?([0-9]{1,3})%")
                match = regex_pattern.search(coverage_report)

                if match:
                    module_to_coverage[module] = match.group(1)
                else:
                    raise Exception("Coverage data not found for module: {}".format(module))

        return module_to_coverage

if __name__ == "__main__":
    module_names = get_modules_from_pom_file()
    module_to_coverage = read_coverage_data_for_modules(module_names)

    print(module_to_coverage)
