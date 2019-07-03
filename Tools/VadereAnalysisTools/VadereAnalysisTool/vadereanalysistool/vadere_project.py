import os
import re
import fnmatch
import shutil
from vadereanalysistool import helper, scenario_output


class NamedOutput:
    """ A Placeholder for dynamically created attributes representing a ScenarioOutput object. Each attribute in this
        object is a lambda function call to access the VadereProject.output_dirs dict to retrive the corresponding
        ScenarioOutput object for this directory. The name of the attributes are the output directory names with
        each '.' and '-' replaced with a '_'.
    """

    def __init__(self):
        pass


class VadereProject():
    """ Represents a vadere_analysis_tool project with all scenarios and all valid outputs.

        Under the attribute named_output all valid ScenarioOutput objects are stored with a name generated from the
        output directory name where each '.' and '-' is replaced with a '_'.

        In the example below the output `bridge_coordinates_kai_2018-10-11_17-50-48.43` can be accessed
        either throug `project.output_dirs['bridge_coordinates_kai_2018-10-11_17-50-48.43']
        or using      `proj.named_output.bridge_coordinates_kai_2018_10_11_17_50_48_43()`
        Note the removed '.' and '-' in the second call.
        Example:
        ├── s2ucre  <---- Vadere Project
        │   ├── output  <---- Folder containing output
        │   │   ├── bridge_coordinates_kai_2018-10-11_17-50-48.43  <---- ScenarioOutput(1)
        │   │   │   ├── bridge_coordinates_kai.scenario
        │   │   │   ├── overlapCount.txt
        │   │   │   ├── overlaps.csv
        │   │   │   └── postvis.trajectories
        │   │   ├── bridge_coordinates_kai_2018-10-15_10-34-43.80  <---- ScenarioOutput(2)
        │   │   │   ├── bridge_coordinates_kai.scenario
        │   │   │   ├── overlapCount.txt
        │   │   │   ├── overlaps.csv
        │   │   │   └── postvis.trajectories
        │   ├── scenarios
        │   │   ├── bridge_coordinates_kai_navigation_random_pos.scenario
        │   │   ├── bridge_coordinates_kai_navigation.scenario
        │   │   ├── bridge_coordinates_kai_random_pos.scenario
        │   │   ├── bridge_coordinates_kai.scenario
        │   │   └── empty.scenario
        │   └── vadere_analysis_tool.project

        Within each ScenarioOutput the Pandas DataFrame for each file can be accessed using the files dictionary or
        again the named_files attribute. For the output `bridge_coordinates_kai_2018-10-11_17-50-48.43` the possible
        calls would be:
        ```
        out = proj.named_output.bridge_coordinates_kai_2018_10_11_17_50_48_43
        out.files['overlapCount.txt']()
        out.files['overlapCount.txt']()
        out.files['postvis.trajectories']()
        # or
        out.named_files.df_overlapCount_txt()
        out.named_files.df_overlaps_csv()
        out.named_files.df_postvis_trajectories()
        ```
        The () are important because the dict only holds lambda to only load the DataFrame if needed. This
        reduces the load time for big VadereProjects.
    """

    def __init__(self, project_dir, expect_all_outputs: bool = True):

        if os.path.exists(project_dir):
            self.project_path = project_dir
        else:
            raise FileNotFoundError("Directory at {} does not exist.".format(project_dir))

        self.project_name = str.strip(helper.read_lines(os.path.join(project_dir, 'vadere.project'))[0])

        self.output_path = os.path.join(self.project_path, 'output')
        if os.path.exists(os.path.join(self.project_path, 'output')):
            self._load_output_directories(expect_all_outputs)
        else:
            print("Warn: project {} has no output folder, an empty folder will be created.".format(self.project_name))
            os.makedirs(self.output_path, mode=0o755)

        if os.path.exists(os.path.join(self.project_path, 'scenarios')):
            self._load_scenario_files()
        else:
            raise FileNotFoundError("Project does not have scenario folder.")

    def _load_scenario_files(self, path='scenarios', scenario_search_pattern = "*.scenario", exclude_patterns=[]):

        scenario_files = []
        for root, dirnames, filenames in os.walk(os.path.join(self.project_path, path)):
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

        self.scenario_files = sorted(scenario_files)

    def clear_output_dir(self):
        if os.path.exists(self.output_path):
            shutil.rmtree(self.output_path)
            os.makedirs(self.output_path, mode=0o755)

    def _load_output_directories(self, expect_all_outputs):
        ret_msg = "loaded {} out of {} output directories. {}"
        err_dir = list()

        all_dirs = [name for name in os.listdir(self.output_path) if os.path.isdir(os.path.join(self.output_path, name))]
        out_dirs = [dir for dir in all_dirs if dir != 'corrupt' and dir != 'legacy' and not dir.startswith('.')]
        self.output_dirs = dict()
        self.named_output = NamedOutput()

        for dir in out_dirs:
            try:
                output = scenario_output.ScenarioOutput.create_output_from_project_output(
                    os.path.join(self.output_path, dir), expect_all_outputs=expect_all_outputs)
                self.output_dirs[dir] = output
                setattr(self.named_output, helper.clean_dir_name(dir), output)
            except FileNotFoundError:
                err_dir.append(dir)

        if len(err_dir) > 0:
            print(ret_msg.format(len(self.output_dirs), len(out_dirs),
                                 'call err_info() function to see invalid output directories'))
            self._err_dirs = err_dir
        else:
            print(ret_msg.format(len(self.output_dirs), len(out_dirs), ''))
            self._err_dirs = list()

    def err_info(self):
        return (self._err_dirs)
