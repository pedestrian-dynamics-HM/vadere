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
        self.assertEqual(len(project.err_info()), 1)
        self.assertEqual(len(project.output_dirs), 30)
