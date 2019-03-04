
from vadereanalysistool import ScenarioOutput
from vadereanalysistool import VadereProject


class SameSeedTrajectory:
    """ Compare postvis.trajectory files from different runs but with same seed."""

    def __init__(self, project_dir):
        self.project = VadereProject(project_dir)

    def find_same_scenarios(self):
        """
        Search through the output directory of the vadere project and group all output together, which have the same
        scenario file (md5-Hash compare after removing trailing ' \n\t\r')
        :return: Dictionary with ScenarioOutputs grouped by the scenario file hash.
        """

        same_scenarios = {}
        for k , v in self.project.output_dirs.items():
            list_of_same_outputs = same_scenarios.get(v.scenario_hash, [])
            if v not in list_of_same_outputs:
                list_of_same_outputs.append(v)
                same_scenarios[v.scenario_hash] = list_of_same_outputs

        return same_scenarios

    def get_trajectory_comparison_result(self):
        """
        Calculate the differences in the trajectory files for outputs with the same scenario file.
        For each comparison the md5-Hash is used (md5-Hash compare after removing trailing ' \n\t\r')
        :return:
        dict-keys:
            *scenario_hash:*    The scenario file hash value representing a group of outputs which should have the same
                                trajectory files.
            *scenario_name:*    Name of the scenario in this group. Must be the same because the name ist included in
                                the scenario file
            *scenario_outputs:* List of tuples containing the output directory name and the corresponding trajectory
                                hash.
            *has_is_equal:*     Boolean indicating if all trajectory files are equal
        """
        ret_list = []
        same_scenarios = self.find_same_scenarios()
        for scenario_hash in same_scenarios:
            scenario_list = same_scenarios[scenario_hash]
            ret = dict()
            trajectory_hash = [out.trajectories_hash for out in scenario_list]
            hash_same = self.compare_hash_list(trajectory_hash)

            ret['scenario_hash'] = scenario_hash
            ret['scenario_name'] = scenario_list[0].get_scenario_name()
            ret['scenario_outputs'] = [(scenario.output_dir_name, scenario.trajectories_hash) for scenario in scenario_list]
            ret['hash_is_equal'] = hash_same
            ret_list.append(ret)

        return ret_list



    @staticmethod
    def compare_hash_list(hash_list):
        iterator = iter(hash_list)
        try:
            first = next(iterator)
        except StopIteration:
            return True
        return all(first == rest for rest in iterator)
