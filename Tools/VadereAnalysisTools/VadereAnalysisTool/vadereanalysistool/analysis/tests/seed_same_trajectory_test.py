import unittest
import pprint
from vadereanalysistool import SameSeedTrajectory


class SameSeedTrajectoryTest(unittest.TestCase):

    def test_find_same_scenarios(self):
        test = SameSeedTrajectory("../../tests/testData/s2ucre")

        same_scenarios = test.find_same_scenarios()
        self.assertEqual(3, len(same_scenarios['774fc8aaaf93ee003709626bb4c0db1f']))
        self.assertEqual(2, len(same_scenarios['7be534d60a54f00afa1f9244466e8dcf']))
        self.assertEqual(2, len(same_scenarios['2b706a0ed12842f7ecfcc1b966b2edcc']))
        self.assertEqual(2, len(same_scenarios['925007d0e16f698f404ede36528bbf49']))

    def test_trajectory_seed(self):
        test = SameSeedTrajectory("../../tests/testData/s2ucre")
        out = test.get_trajectory_comparison_result()
        self.assertEqual(9, len(out))
        pprint.pprint(out)

        bridge_coordinates_kai = out[0]
        self.assertEqual(False, bridge_coordinates_kai['hash_is_equal'])
        self.assertEqual(3, len(bridge_coordinates_kai['scenario_outputs']))

        bridge_coordinates_kai_navigation = out[1]
        self.assertEqual(True, bridge_coordinates_kai_navigation['hash_is_equal'])
        self.assertEqual(2, len(bridge_coordinates_kai_navigation['scenario_outputs']))

        bridge_coordinates_kai_navigation_random_pos = out[2]
        self.assertEqual(True, bridge_coordinates_kai_navigation_random_pos['hash_is_equal'])
        self.assertEqual(2, len(bridge_coordinates_kai_navigation_random_pos['scenario_outputs']))

        bridge_coordinates_kai_origin_0 = out[3]
        self.assertEqual(True, bridge_coordinates_kai_origin_0['hash_is_equal'])
        self.assertEqual(2, len(bridge_coordinates_kai_origin_0['scenario_outputs']))


        bridge_coordinates_kai_origin_0_navigation = out[4]
        self.assertEqual(True, bridge_coordinates_kai_origin_0_navigation['hash_is_equal'])
        self.assertEqual(2, len(bridge_coordinates_kai_origin_0_navigation['scenario_outputs']))


        bridge_coordinates_kai_origin_0_navigation_random_pos = out[5]
        self.assertEqual(True, bridge_coordinates_kai_origin_0_navigation_random_pos['hash_is_equal'])
        self.assertEqual(2, len(bridge_coordinates_kai_origin_0_navigation_random_pos['scenario_outputs']))


        bridge_coordinates_kai_origin_0_random_pos = out[6]
        self.assertEqual(True, bridge_coordinates_kai_origin_0_random_pos['hash_is_equal'])
        self.assertEqual(2, len(bridge_coordinates_kai_origin_0_random_pos['scenario_outputs']))


        bridge_coordinates_kai_random_pos = out[7]
        self.assertEqual(True, bridge_coordinates_kai_random_pos['hash_is_equal'])
        self.assertEqual(2, len(bridge_coordinates_kai_random_pos['scenario_outputs']))


        empty = out[8]
        self.assertEqual(True, empty['hash_is_equal'])
        self.assertEqual(2, len(empty['scenario_outputs']))


