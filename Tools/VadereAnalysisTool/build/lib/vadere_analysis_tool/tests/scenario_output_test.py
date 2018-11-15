import unittest
from vadere_analysis_tool import ScenarioOutput


class ScenarioOutputTests(unittest.TestCase):

    def test_output_dir(self):
        out = ScenarioOutput("testData/s2ucre/output/bridge_coordinates_kai_2018-10-11_17-50-48.43")
        self.assertEqual(out.output_dir, "testData/s2ucre/output/bridge_coordinates_kai_2018-10-11_17-50-48.43")

    def test_scenario_dict(self):
        out = ScenarioOutput("testData/s2ucre/output/bridge_coordinates_kai_2018-10-11_17-50-48.43")
        self.assertEqual(out.scenario['name'], 'bridge_coordinates_kai')

    def test_invalid_scenario_dict(self):
        self.assertRaises(FileNotFoundError, ScenarioOutput, "testData/s2ucre/output/invalid")

    def test_scenario_output_dict(self):
        out = ScenarioOutput("testData/s2ucre/output/bridge_coordinates_kai_2018-10-11_17-50-48.43")
        self.assertEqual(len(out.files), 3)

    def test_settattr_works(self):
        out = ScenarioOutput("testData/s2ucre/output/bridge_coordinates_kai_2018-10-11_17-50-48.43")
        self.assertTrue('df_overlapCount_txt' in dir(out.named_files))
        self.assertTrue('df_overlaps_csv' in dir(out.named_files))
        self.assertTrue('df_postvis_trajectories' in dir(out.named_files))
        self.assertTrue('info_overlapCount_txt' in dir(out.named_files))
        self.assertTrue('info_overlaps_csv' in dir(out.named_files))
        self.assertTrue('info_postvis_trajectories' in dir(out.named_files))

    def test_offset(self):
        out = ScenarioOutput("testData/s2ucre/output/bridge_coordinates_kai_2018-10-11_17-50-48.43")
        self.assertEqual([564280.0,5933391.0], out.get_bound_offset())
