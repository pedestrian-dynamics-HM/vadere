import unittest
from vadereanalysistool import ScenarioOutput


class ScenarioOutputTests(unittest.TestCase):

    def test_output_dir(self):
        out = ScenarioOutput.create_output_from_project_output("testData/s2ucre/output/b_2018-11-16_13-42-54.117")
        self.assertEqual(out.output_dir, "testData/s2ucre/output/b_2018-11-16_13-42-54.117")

    def test_scenario_dict(self):
        out = ScenarioOutput.create_output_from_project_output("testData/s2ucre/output/b_2018-11-16_13-42-54.117")
        self.assertEqual(out.scenario['name'], 'bridge_coordinates_kai')

    def test_invalid_scenario_dict(self):
        self.assertRaises(FileNotFoundError, ScenarioOutput.create_output_from_project_output, "testData/s2ucre/output/invalid")

    def test_scenario_output_dict(self):
        out = ScenarioOutput.create_output_from_project_output("testData/s2ucre/output/b_2018-11-16_13-42-54.117")
        self.assertEqual(len(out.files), 3)

    def test_settattr_works(self):
        out = ScenarioOutput.create_output_from_project_output("testData/s2ucre/output/b_2018-11-16_13-42-54.117")
        self.assertTrue('df_overlapCount_txt' in dir(out.named_files))
        self.assertTrue('df_overlaps_csv' in dir(out.named_files))
        self.assertTrue('df_postvis_trajectories' in dir(out.named_files))
        self.assertTrue('info_overlapCount_txt' in dir(out.named_files))
        self.assertTrue('info_overlaps_csv' in dir(out.named_files))
        self.assertTrue('info_postvis_trajectories' in dir(out.named_files))

    def test_df_wrapper(self):
        out = ScenarioOutput.create_output_from_project_output("testData/s2ucre/output/b_2018-11-16_13-42-54.117")
        self.assertTrue('df_postvis_trajectories' in dir(out.named_files))
        df = out.named_files.df_postvis_trajectories()
        self.assertEqual(len(df), 20)


    def test_offset(self):
        out = ScenarioOutput.create_output_from_project_output("testData/s2ucre/output/b_2018-11-16_13-42-54.117")
        self.assertEqual([564280.0,5933391.0], out.get_bound_offset())

    def test_get_scenario_name(self):
        out = ScenarioOutput.create_output_from_project_output("testData/s2ucre/output/b_2018-11-16_13-42-54.117")
        self.assertEqual('bridge_coordinates_kai', out.get_scenario_name())

    def test_scenario_md5sum(self):
        out_1 = ScenarioOutput.create_output_from_project_output("testData/s2ucre/output/b_2018-11-16_13-42-54.117")
        out_2 = ScenarioOutput.create_output_from_project_output("testData/s2ucre/output/b_2018-11-16_13-57-42.894")
        out_3 = ScenarioOutput.create_output_from_project_output("testData/s2ucre/output/b_2018-11-16_13-43-08.160")
        out_4 = ScenarioOutput.create_output_from_project_output("testData/s2ucre/output/b_2018-11-16_13-57-58.997")
        out_5 = ScenarioOutput.create_output_from_project_output(
            "testData/s2ucre/output/b_2018-11-16_13-46-13.488")
        out_6 = ScenarioOutput.create_output_from_project_output(
            "testData/s2ucre/output/b_2018-11-16_14-01-07.289")
        out_7 = ScenarioOutput.create_output_from_project_output("testData/s2ucre/output/b_2018-11-16_13-49-31.248")
        out_8 = ScenarioOutput.create_output_from_project_output("testData/s2ucre/output/b_2018-11-16_14-04-19.882")
        out_9 = ScenarioOutput.create_output_from_project_output(
            "testData/s2ucre/output/b_2018-11-16_13-49-48.269")
        out_11 = ScenarioOutput.create_output_from_project_output(
            "testData/s2ucre/output/b_2018-11-16_14-04-35.721")
        out_12 = ScenarioOutput.create_output_from_project_output(
            "testData/s2ucre/output/b_2018-11-16_13-53-04.555")
        out_13 = ScenarioOutput.create_output_from_project_output(
            "testData/s2ucre/output/b_2018-11-16_14-08-14.82")
        out_14 = ScenarioOutput.create_output_from_project_output(
            "testData/s2ucre/output/b_2018-11-16_13-56-18.210")
        out_15 = ScenarioOutput.create_output_from_project_output(
            "testData/s2ucre/output/b_2018-11-16_14-11-36.817")
        out_16 = ScenarioOutput.create_output_from_project_output("testData/s2ucre/output/b_2018-11-16_13-56-34.297")
        out_17 = ScenarioOutput.create_output_from_project_output("testData/s2ucre/output/b_2018-11-16_14-11-53.469")
        out_18 = ScenarioOutput.create_output_from_project_output("testData/s2ucre/output/empty_2018-11-16_13-56-50.397")
        out_19 = ScenarioOutput.create_output_from_project_output("testData/s2ucre/output/empty_2018-11-16_14-12-09.609")

        self.assertEqual('774fc8aaaf93ee003709626bb4c0db1f', out_1.scenario_hash)
        self.assertEqual('774fc8aaaf93ee003709626bb4c0db1f', out_2.scenario_hash)
        self.assertEqual('7be534d60a54f00afa1f9244466e8dcf', out_3.scenario_hash)
        self.assertEqual('7be534d60a54f00afa1f9244466e8dcf', out_4.scenario_hash)
        self.assertEqual('2b706a0ed12842f7ecfcc1b966b2edcc', out_5.scenario_hash)
        self.assertEqual('2b706a0ed12842f7ecfcc1b966b2edcc', out_6.scenario_hash)
        self.assertEqual('925007d0e16f698f404ede36528bbf49', out_7.scenario_hash)
        self.assertEqual('925007d0e16f698f404ede36528bbf49', out_8.scenario_hash)
        self.assertEqual('dce2cc64adbcd4520b5217afb64061e8', out_9.scenario_hash)
        self.assertEqual('dce2cc64adbcd4520b5217afb64061e8', out_11.scenario_hash)
        self.assertEqual('9bd23355bdc8e34e6766894bca2b469c', out_12.scenario_hash)
        self.assertEqual('9bd23355bdc8e34e6766894bca2b469c', out_13.scenario_hash)
        self.assertEqual('737fc0ed9b1ff0281043facf5d5a26da', out_14.scenario_hash)
        self.assertEqual('737fc0ed9b1ff0281043facf5d5a26da', out_15.scenario_hash)
        self.assertEqual('3405c30c7449d7b48d1ca21947433960', out_16.scenario_hash)
        self.assertEqual('3405c30c7449d7b48d1ca21947433960', out_17.scenario_hash)
        self.assertEqual('cd909753844587db821d1222e31d80d1', out_18.scenario_hash)
        self.assertEqual('cd909753844587db821d1222e31d80d1', out_19.scenario_hash)


    def test_trajectories_md5sum(self):
        out_1 = ScenarioOutput.create_output_from_project_output("testData/s2ucre/output/b_2018-11-16_13-42-54.117")
        out_2 = ScenarioOutput.create_output_from_project_output("testData/s2ucre/output/b_2018-11-16_13-57-42.894")
        out_3 = ScenarioOutput.create_output_from_project_output("testData/s2ucre/output/b_2018-11-16_13-43-08.160")
        out_4 = ScenarioOutput.create_output_from_project_output("testData/s2ucre/output/b_2018-11-16_13-57-58.997")
        out_5 = ScenarioOutput.create_output_from_project_output(
            "testData/s2ucre/output/b_2018-11-16_13-46-13.488")
        out_6 = ScenarioOutput.create_output_from_project_output(
            "testData/s2ucre/output/b_2018-11-16_14-01-07.289")
        out_7 = ScenarioOutput.create_output_from_project_output("testData/s2ucre/output/b_2018-11-16_13-49-31.248")
        out_8 = ScenarioOutput.create_output_from_project_output("testData/s2ucre/output/b_2018-11-16_14-04-19.882")
        out_9 = ScenarioOutput.create_output_from_project_output(
            "testData/s2ucre/output/b_2018-11-16_13-49-48.269")
        out_11 = ScenarioOutput.create_output_from_project_output(
            "testData/s2ucre/output/b_2018-11-16_14-04-35.721")
        out_12 = ScenarioOutput.create_output_from_project_output(
            "testData/s2ucre/output/b_2018-11-16_13-53-04.555")
        out_13 = ScenarioOutput.create_output_from_project_output(
            "testData/s2ucre/output/b_2018-11-16_14-08-14.82")
        out_14 = ScenarioOutput.create_output_from_project_output(
            "testData/s2ucre/output/b_2018-11-16_13-56-18.210")
        out_15 = ScenarioOutput.create_output_from_project_output(
            "testData/s2ucre/output/b_2018-11-16_14-11-36.817")
        out_16 = ScenarioOutput.create_output_from_project_output("testData/s2ucre/output/b_2018-11-16_13-56-34.297")
        out_17 = ScenarioOutput.create_output_from_project_output("testData/s2ucre/output/b_2018-11-16_14-11-53.469")
        out_18 = ScenarioOutput.create_output_from_project_output("testData/s2ucre/output/empty_2018-11-16_13-56-50.397")
        out_19 = ScenarioOutput.create_output_from_project_output("testData/s2ucre/output/empty_2018-11-16_14-12-09.609")

        self.assertEqual('fca32f3f98ac8c3ffb111cac28f9b9a4', out_1.trajectories_hash)
        self.assertEqual('fca32f3f98ac8c3ffb111cac28f9b9a4', out_2.trajectories_hash)
        self.assertEqual('7d128fe3cfe7f4a35709ce88cd86d58f', out_3.trajectories_hash)
        self.assertEqual('7d128fe3cfe7f4a35709ce88cd86d58f', out_4.trajectories_hash)
        self.assertEqual('592ad7b457fe7eca015093e04cddeafe', out_5.trajectories_hash)
        self.assertEqual('592ad7b457fe7eca015093e04cddeafe', out_6.trajectories_hash)
        self.assertEqual('f648ce3e4373f931bfc77131364e892c', out_7.trajectories_hash)
        self.assertEqual('f648ce3e4373f931bfc77131364e892c', out_8.trajectories_hash)
        self.assertEqual('a161272d2b5bd59d744048a60543e082', out_9.trajectories_hash)
        self.assertEqual('a161272d2b5bd59d744048a60543e082', out_11.trajectories_hash)
        self.assertEqual('16f901700235eb9954a217619c505065', out_12.trajectories_hash)
        self.assertEqual('16f901700235eb9954a217619c505065', out_13.trajectories_hash)
        self.assertEqual('fc770fb2b6d3656ce0a21a9745f8f6ea', out_14.trajectories_hash)
        self.assertEqual('fc770fb2b6d3656ce0a21a9745f8f6ea', out_15.trajectories_hash)
        self.assertEqual('c1322a79513671d2fd70b1dfbebf0247', out_16.trajectories_hash)
        self.assertEqual('c1322a79513671d2fd70b1dfbebf0247', out_17.trajectories_hash)
        self.assertEqual('6f077db2b6af4e022f970cbe4ff3b1f8', out_18.trajectories_hash)
        self.assertEqual('6f077db2b6af4e022f970cbe4ff3b1f8', out_19.trajectories_hash)
