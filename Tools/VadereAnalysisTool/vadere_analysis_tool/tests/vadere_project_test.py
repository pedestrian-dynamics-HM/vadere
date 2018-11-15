import unittest
from vadere_analysis_tool import vadere_project


class VadereProjectTests(unittest.TestCase):

    def test_project_name(self):
        project = vadere_project.VadereProject("testData/s2ucre")
        self.assertEqual(project.project_name, 's2ucre_scenarios')

    def test_wrong_project_dir(self):
        self.assertRaises(FileNotFoundError, vadere_project.VadereProject, "testData/s2uSSScre")

    def test_no_output_dir(self):
        self.assertRaises(FileNotFoundError, vadere_project.VadereProject, "testData/s2ucreInvalid")

    def test_load_output_dir(self):
        project = vadere_project.VadereProject("testData/s2ucre")
        self.assertEqual(len(project.err_info()), 2)
        self.assertEqual(len(project.output_dirs), 6)

    def test_scenaio_files(self):
        project = vadere_project.VadereProject("testData/s2ucre")
        self.assertEqual(['testData/s2ucre/scenarios/bridge_coordinates_kai.scenario',
         'testData/s2ucre/scenarios/bridge_coordinates_kai_navigation.scenario',
         'testData/s2ucre/scenarios/bridge_coordinates_kai_navigation_random_pos.scenario',
         'testData/s2ucre/scenarios/bridge_coordinates_kai_origin_0.scenario',
         'testData/s2ucre/scenarios/bridge_coordinates_kai_origin_0_navigation.scenario',
         'testData/s2ucre/scenarios/bridge_coordinates_kai_origin_0_navigation_random_pos.scenario',
         'testData/s2ucre/scenarios/bridge_coordinates_kai_origin_0_random_pos.scenario',
         'testData/s2ucre/scenarios/bridge_coordinates_kai_random_pos.scenario',
         'testData/s2ucre/scenarios/empty.scenario'], project.scenario_files)
