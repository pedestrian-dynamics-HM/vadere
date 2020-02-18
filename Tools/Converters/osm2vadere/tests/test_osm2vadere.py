import os
import unittest

import utm
from lxml import etree

import osm2vadere
from osm2vadere import OsmConverter, PolyObjectWidthId
from osm_helper import OsmData, PolyObjectWidthId

TEST_DATA_LON_LAT = os.path.join(os.path.dirname(__file__), "maps/map_for_testing.osm")
TEST_DATA_2 = os.path.join(os.path.dirname(__file__), "maps/map_mf_small.osm")
TEST_DATA = """{
    "shape" : {
        "type" : "POLYGON",
        "points" : [          { "x" : 143.92115343943564, "y" : 168.69565355275918 },
         { "x" : 143.8295708812843, "y" : 158.46319241869656 },
         { "x" : 147.9524496438901, "y" : 158.453088570111 },
         { "x" : 148.04042161765545, "y" : 168.57734735060853 },
         { "x" : 148.54040274306163, "y" : 168.57300290155786 },
         { "x" : 148.44811333818495, "y" : 157.95187235651085 },
         { "x" : 143.3250868078719, "y" : 157.96442724530158 },
         { "x" : 143.42117346474575, "y" : 168.70012847277175 },
         { "x" : 143.92115343943564, "y" : 168.69565355275918 },
         { "x" : 143.92115343943564, "y" : 168.69565355275918 },
         { "x" : 143.8295708812843, "y" : 158.46319241869656 },
         { "x" : 147.9524496438901, "y" : 158.453088570111 },
         { "x" : 148.04042161765545, "y" : 168.57734735060853 },
         { "x" : 148.54040274306163, "y" : 168.57300290155786 },
         { "x" : 148.44811333818495, "y" : 157.95187235651085 },
         { "x" : 143.3250868078719, "y" : 157.96442724530158 },
         { "x" : 143.42117346474575, "y" : 168.70012847277175 },
         { "x" : 143.92115343943564, "y" : 168.69565355275918 } ]
    },
    "id" : 258139209
}
"""


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
        # print(end_cartesian[0] - base_cartesian[0], end_cartesian[1] - base_cartesian[1])

        self.assertTrue(x_distance > 227 and x_distance < 235)
        self.assertTrue(y_distance > 258 and y_distance < 275)

    def test_extract_latitude_and_longitude_for_each_xml_node(self):
        xml_tree = etree.parse(TEST_DATA_LON_LAT)
        osm_xml = OsmData(TEST_DATA_LON_LAT)
        nodes_dictionary_with_lat_and_lon = osm_xml.node_latlon_lookup

        self.assertTrue(nodes_dictionary_with_lat_and_lon.get("1")[0] == "1.1")
        self.assertTrue(nodes_dictionary_with_lat_and_lon.get("1")[1] == "1.2")
        self.assertTrue(nodes_dictionary_with_lat_and_lon.get("2")[0] == "2.1")
        self.assertTrue(nodes_dictionary_with_lat_and_lon.get("2")[1] == "2.2")
        self.assertTrue(nodes_dictionary_with_lat_and_lon.get("3")[0] == "3.1")
        self.assertTrue(nodes_dictionary_with_lat_and_lon.get("3")[1] == "3.2")

    def test_find_width_and_height(self):
        building_normal = [(1, 1), (3, 1), (1, 3), (3, 3)]
        building_negative_coordinates = [(-1, 4), (-3, 2), (10, 2)]
        building_with_floating_points = [
            (2.3, 1.4),
            (-10.5, 7),
            (9.99, 3),
            (5, 7.1),
            (3, 4),
        ]
        building_points = [
            building_normal,
            building_negative_coordinates,
            building_with_floating_points,
        ]
        buildings = [PolyObjectWidthId(-1, building) for building in building_points]
        width, height = OsmConverter.find_width_and_height(buildings)

        self.assertTrue(width == 10)
        self.assertTrue(
            height == 8
        )  # 7.1 is the maximum but the function returns math.ceil

    def test_find_new_basepoint(self):
        building_normal = [(1, 1), (3, 1), (1, 3), (3, 3)]
        building_negative_coordinates = [(-1, 4), (-3, 2), (10, 2)]
        building_with_floating_points = [
            (2.3, 1.4),
            (-10.5, 7),
            (9.99, 3),
            (5, 7.1),
            (3, 4),
        ]
        building_points = [
            building_normal,
            building_negative_coordinates,
            building_with_floating_points,
        ]
        buildings = [PolyObjectWidthId(-1, building) for building in building_points]
        new_base_point = OsmConverter.find_new_base_point(buildings)

        self.assertTrue(new_base_point == (-10.5, 1))

        building_negative_coordinates.append((3, -5))
        new_base_point = OsmConverter.find_new_base_point(buildings)

        self.assertTrue(new_base_point == (-10.5, -5))

        buildings_cartesian_only_positive = [
            [(1, 3), (1, 2), (2, 2)],
            [(2, 4), (7, 7), (6, 6)],
        ]
        buildings = [
            PolyObjectWidthId(-1, building)
            for building in buildings_cartesian_only_positive
        ]
        new_base_point = OsmConverter.find_new_base_point(buildings)

        self.assertTrue(new_base_point == (1, 2))

    def test_get_wall(self):
        class Ns:
            def __init__(self, d, osm_file, way):
                self.d = d
                self.osm_file = osm_file
                self.way = way
                self.output = None
                self.use_osm_id = True

        o = osm2vadere.main_way_to_polygon(Ns(0.25, TEST_DATA_2, [258139209]))
        self.assertEqual(len(o), 1)
        self.assertEqual("".join(TEST_DATA.split()), "".join(o[0].split()))

    def test_shift_points(self):
        building_points = [[(1, 3), (-1, 2), (2, 2)], [(2, 4), (7, 7), (6, 6)]]
        buildings = [
            osm2vadere.PolyObjectWidthId(-1, points) for points in building_points
        ]
        for b in buildings:
            b.shift_points([1, 2])
        self.assertEqual(buildings[0].points, [(0, 1), (-2, 0), (1, 0)])
        self.assertEqual(buildings[1].points, [(1, 2), (6, 5), (5, 4)])


if __name__ == "__main__":
    unittest.main()
