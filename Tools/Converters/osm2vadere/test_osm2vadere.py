from lxml import etree

import osm2vadere
import unittest
import utm

class TestOsm2vadere(unittest.TestCase):

    def test_utm_conversion(self):
        # tests if the distances of the Allianz Arena match up
        # from the website https://allianz-arena.com/de/die-arena/fakten/allgemeine-informationen: Dimension Stadion: 258 m x 227 m x 52 m (Bruttorauminhalt)
        # the stadion isn't alligned horizontal, therefore the calculated distances are bigger.
        base_allianz_arena = (48.21762, 11.62305)
        end_allianz_arena = (48.22001, 11.62628)

        base_cartesian = utm.from_latlon(base_allianz_arena[0], base_allianz_arena[1])
        end_cartesian = utm.from_latlon(end_allianz_arena[0], end_allianz_arena[1])

        x_distance = end_cartesian[0] - base_cartesian[0]
        y_distance = end_cartesian[1] - base_cartesian[1]
        #print(end_cartesian[0] - base_cartesian[0], end_cartesian[1] - base_cartesian[1])

        self.assertTrue(x_distance > 227 and x_distance < 235)
        self.assertTrue(y_distance > 258 and y_distance < 275)

    def test_extract_latitude_and_longitude_for_each_xml_node(self):
        xml_tree = etree.parse("maps/unit_test_map.osm")
        nodes_dictionary_with_lat_and_lon = osm2vadere.extract_latitude_and_longitude_for_each_xml_node(xml_tree)

        self.assertTrue(nodes_dictionary_with_lat_and_lon.get("1")[0] == "1.1")
        self.assertTrue (nodes_dictionary_with_lat_and_lon.get("1")[1] == "1.2")
        self.assertTrue (nodes_dictionary_with_lat_and_lon.get("2")[0] == "2.1")
        self.assertTrue (nodes_dictionary_with_lat_and_lon.get("2")[1] == "2.2")
        self.assertTrue (nodes_dictionary_with_lat_and_lon.get("3")[0] == "3.1")
        self.assertTrue (nodes_dictionary_with_lat_and_lon.get("3")[1] == "3.2")

    def test_find_width_and_height(self):
        building_normal = [(1, 1), (3, 1), (1, 3), (3, 3)]
        building_negative_coordinates = [(-1, 4), (-3, 2), (10, 2)]
        building_with_floating_points = [(2.3, 1.4), (-10.5, 7), (9.99, 3), (5, 7.1), (3, 4)]
        buildings_cartesian = [building_normal, building_negative_coordinates, building_with_floating_points]
        width, height = osm2vadere.find_width_and_height(buildings_cartesian)

        self.assertTrue(width == 10)
        self.assertTrue(height == 8) # 7.1 is the maximum but the function returns math.ceil

    def test_find_new_basepoint(self):
        building_normal = [(1, 1), (3, 1), (1, 3), (3, 3)]
        building_negative_coordinates = [(-1, 4), (-3, 2), (10, 2)]
        building_with_floating_points = [(2.3, 1.4), (-10.5, 7), (9.99, 3), (5, 7.1), (3, 4)]
        
        buildings_cartesian = [building_normal, building_negative_coordinates, building_with_floating_points]
        
        new_base_point = osm2vadere.find_new_basepoint(buildings_cartesian)
        self.assertTrue(new_base_point == (-10.5, 1))

        building_negative_coordinates.append((3, -5))
        new_base_point = osm2vadere.find_new_basepoint(buildings_cartesian)
        self.assertTrue(new_base_point == (-10.5, -5))

        buildings_cartesian_only_positive = [[(1, 3), (1, 2), (2, 2)], [(2, 4), (7, 7), (6, 6)]]
        new_base_point = osm2vadere.find_new_basepoint(buildings_cartesian_only_positive)
        self.assertTrue(new_base_point == (1, 2))

    def test_shift_points(self):
        buildings_cartesian = [[(1, 3), (-1, 2), (2.2, 2)], [(2, 4), (7, 7), (6, 6)]]
        buildings_cartesian_shifted_by_one_and_two = osm2vadere.shift_points(buildings_cartesian, 1, 2)

        self.assertTrue(buildings_cartesian_shifted_by_one_and_two == [[(2, 5), (0, 4), (3.2, 4)], [(3, 6), (8, 9), (7, 8)]])

if __name__ == "__main__":
    unittest.main()
