import os
from vadereanalysistool import helper
import pandas as pd
import json
import hashlib


class NamedFiles:

    def __init__(self):
        pass


class ScenarioOutput:
    """
    attributes:
    output_dir:         path to output directory (may be relative or absolute
    output_dir_name:    name of output directory
    scenario_path:      path to scenario file within output directory
    scenario:           dict representing the whole scenario file
    scenario_hash:      hash of scenario file based on string.strip(' \s\r\n') to ensure cross platform comparison
    trajectories_hash:  hash of trajectory file base ond string.strip(' \s\r\n') to ensure cross platform comparison
    file:               dict containing lambdas to create DataFrames for each output file.
                        The Key is the file name with '.' and '-' replaced with '_'
    named_files:        dummpy object containing attributes for each file in *file* with '.' and '-' replaced with '_'
                        These attributes are also lambdas
    """

    def __init__(self, output_dir):

        if os.path.exists(output_dir):
            self.output_dir = output_dir
            self.output_dir_name = os.path.basename(output_dir)
        else:
            raise FileNotFoundError("Directory at {} does not exist.".format(output_dir))

        output_files = os.listdir(output_dir)
        scenario_files = [f for f in output_files if f.endswith(".scenario")]

        if len(scenario_files) < 1:
            raise FileNotFoundError("Output directory has no scenario")
        self.scenario_path = os.path.join(output_dir, scenario_files[0])
        self.scenario = helper.read_json_to_dict(self.scenario_path)
        self.scenario_hash = self._get_md5_sum(self.scenario_path)
        if os.path.exists(os.path.join(self.output_dir, 'postvis.trajectories')):
            self.trajectories_hash = self._get_md5_sum(os.path.join(self.output_dir, 'postvis.trajectories'))
        else:
            raise FileNotFoundError("postvis.trajectories not found in {}.".format(output_dir))

        # add attributes for output files programmatically to the ScenarioOutput object. These attributes
        # are recognized by the code completion tool of jupyter-notebook and allow easy access to the each
        # output file in one simulation run.
        self.files = dict()
        self.named_files = NamedFiles()

        for file in self.scenario['processWriters']['files']:
            f_name = file['filename']
            f_path = os.path.join(self.output_dir, f_name)
            if os.path.exists(f_path):
                attr_df = helper.clean_dir_name(f_name)
                setattr(self.named_files, "df_" + attr_df, self._load_df_(f_path))
                self.files[f_name] = self._load_df_(f_path)

                attr_info = "info_{}".format(attr_df)
                attr_info_dict = dict()
                attr_info_dict['keyType'] = file['type'].split('.')[-1]
                attr_info_dict['dataprocessors'] = self._get_used_processors_(file['processors'])
                attr_info_dict['path'] = os.path.abspath(f_path)
                setattr(self.named_files, attr_info, attr_info_dict)

    def _get_used_processors_(self, ids):
        """
        :param ids: list of processor ids used in one output file
        :return:    the names of DataProcessors corresponding to the given ids. Only the last component of the
                    dataProcessor name is returned
        """
        processor_list = [p for p in self.scenario['processWriters']['processors'] if p['id'] in ids]
        return [p['type'].split('.')[-1] for p in processor_list]

    def _load_df_(self, path):
        """
        :return: lambda function to lazy load pandas DataFrame. This reduces the load time of a vadere_analysis_tool
        project in a jupyter-notebook because the DataFrames of an output file is loaded only when needed.
        """
        return lambda: self._get_dataframe_(path)

    def info(self):
        """
        :return: print important scenario settings based on scenario file from the output directory
        """
        print("mainModel:", self.scenario['scenario']['mainModel'])
        print("attributesSimulation:")
        print(json.dumps(self.scenario['scenario']['attributesSimulation'], indent=2))
        print("attributesModel:")
        print(json.dumps(self.scenario['scenario']['attributesModel'], indent=2))

    def get_scenario_name(self):
        try:
            name = self.scenario['name']
        except KeyError as err:
            raise KeyError("The scenario file in output {} is corrupt. Scenario file not found. Err:{}".format(
                self.output_dir_name, err))

        return name

    def get_bound_offset(self):
        try:
            offset = [self.scenario['scenario']['topography']['attributes']['bounds']['x'],
                      self.scenario['scenario']['topography']['attributes']['bounds']['y']]
        except KeyError as err:
            raise KeyError("The scenario file in output {} is corrupt. topograhpy bound not found. Err:{}".format(
                self.output_dir_name, err))

        return offset

    @staticmethod
    def _get_md5_sum(path):

        with open(path, "r", encoding='utf-8') as f:
            text = f.read()
            text = text.strip(' \t\n\r')
            hash_md5 = hashlib.md5(text.encode('utf-8'))
        return hash_md5.hexdigest()

    @staticmethod
    def _get_dataframe_(path):
        df = pd.read_csv(filepath_or_buffer=path, sep=" ", header=0, decimal=".", index_col=False, encoding="utf-8")
        return df
