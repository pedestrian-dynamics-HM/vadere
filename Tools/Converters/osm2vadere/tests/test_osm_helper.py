import os
import unittest

import numpy as np
from lxml import etree

from osm_helper import (Nd, Node, OsmData, OsmLookup, OsmLookupError,
                        PathToPolygon, PolyObjectWidthId, Tag, Way)


class TestNode(unittest.TestCase):
    def test_create_node(self):
        node = Node.create_node(1, 3, 5)
        self.assertEqual(node.id, 1)
        self.assertEqual(node.lat, 3)
        self.assertEqual(node.lon, 5)

    def test_equals(self):
        node1 = Node.create_node(1, 3, 5)
        node2 = Node.create_node(2, 4, 4)
        node3 = Node.create_node(1, 3, 5)
        self.assertEquals(node1, node3)
        self.assertNotEqual(node1, node2)

    def test_tags(self):
        node = Node.create_node(1, 3, 5)
        node.add_tag("foo", "bar")
        self.assertListEqual(node.tags, [Tag("rover:id", 1), Tag("foo", "bar")])

    def test_to_xml(self):
        node = Node.create_node(1, 3, 5)
        node.add_tag("foo", "bar")
        str_xml = b'<node id="1" action="modify" visible="true" lon="5" lat="3"><tag k="rover:id" v="1"/><tag k="foo" v="bar"/></node>'
        self.assertEquals(etree.tostring(node.to_xml()), str_xml)


class TestNd(unittest.TestCase):
    def test_Nd(self):
        nd = Nd(3344)
        self.assertEquals(etree.tostring(nd.to_xml()), b'<nd ref="3344"/>')

    def test_equal(self):
        nd1 = Nd(33)
        nd2 = Nd(44)
        nd3 = Nd(33)
        self.assertEqual(nd1, nd3)
        self.assertNotEqual(nd1, nd2)


class TestTag(unittest.TestCase):
    def test_Tag(self):
        tag = Tag("Foo", "Bar")
        self.assertEquals(etree.tostring(tag.to_xml()), b'<tag k="Foo" v="Bar"/>')

    def test_equal(self):
        tag1 = Tag("Foo", "Bar")
        tag2 = Tag("Bar", "Foo")
        tag3 = Tag("Foo", "Bar")
        self.assertEqual(tag1, tag3)
        self.assertNotEqual(tag1, tag2)


class TestWay(unittest.TestCase):
    def test_create_with_refs(self):
        way = Way.create_with_refs(-3, [1, 2, 3], att={"foo": "bar"}, tags={"baz": 42})
        self.assertEqual(way.id, -3)
        self.assertEqual(way.action, "modify")
        self.assertEqual(way.visible, "true")
        self.assertDictEqual(way.attr, {"foo": "bar"})
        self.assertListEqual(way.nds, [Nd(1), Nd(2), Nd(3)])
        self.assertListEqual(way.tags, [Tag("rover:id", 3), Tag("baz", 42)])

    def test_create(self):
        way = Way.create(
            -3,
            [
                Node.create_node(1, 1, 2),
                Node.create_node(2, 3, 4),
                Node.create_node(3, 5, 6),
            ],
            att={"foo": "bar"},
            tags={"baz": 42},
        )
        self.assertEqual(way.id, -3)
        self.assertEqual(way.action, "modify")
        self.assertEqual(way.visible, "true")
        self.assertDictEqual(way.attr, {"foo": "bar"})
        self.assertListEqual(way.nds, [Nd(1), Nd(2), Nd(3)])
        self.assertListEqual(way.tags, [Tag("rover:id", 3), Tag("baz", 42)])

    def test_add_tag(self):
        way = Way.create_with_refs(-3, [1, 2, 3])
        way.add_tag("Foo", "Bar")
        self.assertListEqual(way.tags, [Tag("rover:id", 3), Tag("Foo", "Bar")])

    def test_to_xml(self):
        way = Way.create_with_refs(-3, [1, 2, 3])
        way.add_tag("Foo", "Bar")
        way_str = b'<way id="-3" action="modify" visible="true"><nd ref="1"/><nd ref="2"/><nd ref="3"/><tag k="rover:id" v="3"/><tag k="Foo" v="Bar"/></way>'
        self.assertEqual(etree.tostring(way.to_xml()), way_str)


class TestPathToPolygon(unittest.TestCase):
    def test_strait_line(self):
        points = [(0.0, 0.0), (2.0, 0.0), (3.0, 0.0)]
        path = PathToPolygon(points, dist=0.1)
        self.assertEqual(len(path.polygon), 7)
        self.assertTrue((path.polygon[0] == [0.0, 0.1]).all())
        self.assertTrue((path.polygon[1] == [2.0, 0.1]).all())
        self.assertTrue((path.polygon[2] == [3.0, 0.1]).all())
        self.assertTrue((path.polygon[3] == [3.0, -0.1]).all())
        self.assertTrue((path.polygon[4] == [2.0, -0.1]).all())
        self.assertTrue((path.polygon[5] == [0.0, -0.1]).all())
        self.assertTrue((path.polygon[6] == [0.0, 0.1]).all())
        self.assertTrue((path.polygon[0] == path.polygon[-1]).all())


class TestPolyObjectWidthId(unittest.TestCase):
    def test_shift(self):
        obj = PolyObjectWidthId(3, [(3, 5), (4, 6), (5, 7)])
        obj.shift_points([1, 2])
        self.assertListEqual(obj.points, [(2, 3), (3, 4), (4, 5)])

    def test_closed(self):
        obj1 = PolyObjectWidthId(3, [(3, 5), (4, 6), (5, 7)])
        obj2 = PolyObjectWidthId(3, [(3, 5), (4, 6), (5, 7), (3, 5)])
        self.assertFalse(obj1.closed())
        self.assertTrue(obj2.closed())

    def test_create(self):
        closed1 = PolyObjectWidthId.create_closed(3, [(3, 5), (4, 6), (5, 7)])
        closed2 = PolyObjectWidthId.create_closed(3, [(3, 5), (4, 6), (5, 7), (3, 5)])
        self.assertTrue(closed1.closed())
        self.assertTrue(closed2.closed())
        self.assertEqual(closed1, closed2)

        open1 = PolyObjectWidthId.create_open(3, [(3, 5), (4, 6), (5, 7)])
        open2 = PolyObjectWidthId.create_open(3, [(3, 5), (4, 6), (5, 7), (3, 5)])
        self.assertFalse(open1.closed())
        self.assertFalse(open2.closed())
        self.assertEqual(open1, open2)

    def test_add_template_data(self):
        obj = PolyObjectWidthId.create_closed(3, [(3, 5), (4, 6), (5, 7)])
        obj.add_template_data("Foo", "Bar")
        self.assertDictEqual(obj.template_data, {"id": 3, "Foo": "Bar"})


class TestOsmLookup(unittest.TestCase):
    def test_load(self):
        nodes = [
            Node.create_node(2, 48.16256981067, 11.5868239213).to_xml(),
            Node.create_node(1, 48.16254537153, 11.58697933476).to_xml(),
            Node.create_node(-3, 48.16255438099, 11.58692204178).to_xml(),
        ]
        lookup = OsmLookup()
        lookup.load(nodes)

        self.assertDictEqual(lookup.latlon_to_node_errors, {})
        self.assertEqual(len(lookup.zone_map), 1)
        self.assertTupleEqual(list(lookup.zone_map.keys())[0], (32, "U"))

        self.assertEqual(len(lookup.latlon_to_node), 3)
        self.assertEqual(len(lookup.node_to_utm), 3)
        self.assertEqual(len(lookup.node_to_latlon), 3)

    def test_multiple_load(self):
        nodes = [
            Node.create_node(2, 48.16256981067, 11.5868239213).to_xml(),
            Node.create_node(1, 48.16254537153, 11.58697933476).to_xml(),
            Node.create_node(-3, 48.16255438099, 11.58692204178).to_xml(),
        ]
        lookup = OsmLookup()
        lookup.load(nodes)
        self.assertEqual(len(lookup.latlon_to_node), 3)
        self.assertEqual(len(lookup.node_to_utm), 3)
        self.assertEqual(len(lookup.node_to_latlon), 3)
        self.assertTupleEqual(list(lookup.zone_map.keys())[0], (32, "U"))
        self.assertDictEqual(lookup.latlon_to_node_errors, {})

        # should be cleaned
        lookup.load(nodes)
        self.assertEqual(len(lookup.latlon_to_node), 3)
        self.assertEqual(len(lookup.node_to_utm), 3)
        self.assertEqual(len(lookup.node_to_latlon), 3)
        self.assertTupleEqual(list(lookup.zone_map.keys())[0], (32, "U"))
        self.assertDictEqual(lookup.latlon_to_node_errors, {})

    def test_add(self):
        node1 = Node.create_node(2, 48.16256981067, 11.5868239213)
        node2 = Node.create_node(1, 48.16254537153, 11.58697933476)

        lookup = OsmLookup()
        lookup.add(2, (node1.lat, node1.lon), (3, 5))
        self.assertEqual(len(lookup.node_to_latlon), 1)
        self.assertEqual(len(lookup.node_to_utm), 1)
        self.assertEqual(len(lookup.latlon_to_node), 1)

        lookup.add(2, (node1.lat, node1.lon), (3, 5))
        self.assertEqual(len(lookup.node_to_latlon), 1)
        self.assertEqual(len(lookup.node_to_utm), 1)
        self.assertEqual(len(lookup.latlon_to_node), 1)

        lookup.add(1, (node2.lat, node2.lon), (3, 7))
        self.assertEqual(len(lookup.node_to_latlon), 2)
        self.assertEqual(len(lookup.node_to_utm), 2)
        self.assertEqual(len(lookup.latlon_to_node), 2)

    def test_remove(self):
        nodes = [
            Node.create_node(2, 48.16256981067, 11.5868239213).to_xml(),
            Node.create_node(1, 48.16254537153, 11.58697933476).to_xml(),
            Node.create_node(-3, 48.16255438099, 11.58692204178).to_xml(),
        ]
        lookup = OsmLookup()
        lookup.load(nodes)

        self.assertEqual(len(lookup.latlon_to_node), 3)
        self.assertEqual(len(lookup.node_to_utm), 3)
        self.assertEqual(len(lookup.node_to_latlon), 3)

        lookup.remove(2)
        self.assertEqual(len(lookup.latlon_to_node), 2)
        self.assertEqual(len(lookup.node_to_utm), 2)
        self.assertEqual(len(lookup.node_to_latlon), 2)

    def test_multiple_utm_zones(self):
        nodes = [
            Node.create_node(2, 48.16256981067, 11.5868239213).to_xml(),
            Node.create_node(1, 48.16254537153, 11.58697933476).to_xml(),
            Node.create_node(-3, 48.16255438099, 33.58692204178).to_xml(),
        ]  # wrong zone
        lookup = OsmLookup()
        self.assertRaises(RuntimeError, lookup.load, nodes)

    def test_duplicated_ids_negative(self):
        nodes = [
            Node.create_node(2, 48.16256981067, 11.5868239213).to_xml(),
            Node.create_node(-3, 48.16254537153, 11.58697933476).to_xml(),
            Node.create_node(-3, 48.16255438099, 11.58692204178).to_xml(),
        ]
        lookup = OsmLookup(strict=True)
        self.assertRaises(OsmLookupError, lookup.load, nodes)
        self.assertEqual(len(lookup.latlon_to_node_errors), 0)

    def test_duplicated_ids_positive(self):
        nodes = [
            Node.create_node(2, 48.16256981067, 11.5868239213).to_xml(),
            Node.create_node(3, 48.16254537153, 11.58697933476).to_xml(),
            Node.create_node(3, 48.16255438099, 11.58692204178).to_xml(),
        ]
        lookup = OsmLookup(strict=True)
        lookup.load(nodes)
        self.assertEqual(len(lookup.latlon_to_node_errors), 0)

    def test_duplicated_latlon(self):
        nodes = [
            Node.create_node(2, 48.16256981067, 11.5868239213).to_xml(),
            Node.create_node(1, 48.16254537153, 11.58697933476).to_xml(),
            Node.create_node(-3, 48.16254537153, 11.58697933476).to_xml(),
        ]
        lookup = OsmLookup(strict=True)
        self.assertRaises(OsmLookupError, lookup.load, nodes)
        self.assertEqual(len(lookup.latlon_to_node_errors), 1)


class TestOsmData(unittest.TestCase):
    def setUp(self):
        self.valid001 = OsmData(
            input_file=os.path.join(os.path.dirname(__file__), "./valid001.osm")
        )
        self.valid002 = OsmData(
            input_file=os.path.join(os.path.dirname(__file__), "./valid002.osm")
        )
        self.valid003 = OsmData(
            input_file=os.path.join(os.path.dirname(__file__), "./valid003.osm")
        )
        self.valid004 = OsmData(
            input_file=os.path.join(os.path.dirname(__file__), "./valid004.osm")
        )
        self.invalid001 = OsmData(
            input_file=os.path.join(os.path.dirname(__file__), "./invalid001.osm")
        )
        self.invalid002 = OsmData(
            input_file=os.path.join(os.path.dirname(__file__), "./invalid002.osm"),
            strict=False,
        )
        self.invalid003 = OsmData(
            input_file=os.path.join(os.path.dirname(__file__), "./invalid003.osm"),
            strict=False,
        )
        self.invalid004 = OsmData(
            input_file=os.path.join(os.path.dirname(__file__), "./invalid004.osm"),
            strict=False,
        )
        self.invalid005 = OsmData(
            input_file=os.path.join(os.path.dirname(__file__), "./invalid005.osm"),
            strict=False,
        )

    def test_base_point(self):
        self.assertTupleEqual(self.valid001.base_point, (48.16077, 11.58451))

    def test_nodes(self):
        nodes = self.valid001.nodes
        self.assertEqual(len(nodes), 6)
        self.assertEqual(nodes[0].get("id"), "-127405")
        self.assertEqual(nodes[1].get("id"), "-127407")
        self.assertEqual(nodes[2].get("id"), "-127425")
        self.assertEqual(nodes[3].get("id"), "1318876746")
        self.assertEqual(nodes[4].get("id"), "1324414637")
        self.assertEqual(nodes[5].get("id"), "1324414707")

    def test_utm_zone(self):
        self.assertTupleEqual(self.valid001.utm_zone, (32, "U"))

    def test_utm_zone_string(self):
        self.assertEqual(self.valid001.utm_zone_string, "UTM Zone 32U")

    def test__xpath_k_v_tags(self):
        self.assertEqual(
            OsmData._xpath_k_v_tags([("Foo", "Bar"), ("Baz", "Tar")]),
            "(./tag[@k='Foo' and @v='Bar'] and ./tag[@k='Baz' and @v='Tar'])",
        )

    def test__xpath_k_v_tag(self):
        self.assertEqual(
            OsmData._xpath_k_v_tag("Foo", "Bar"), "./tag[@k='Foo' and @v='Bar']"
        )

    def test__add_ignore(self):
        self.assertEqual(OsmData._add_ignore([]), "")

        self.assertEqual(
            OsmData._add_ignore([("Foo", "Bar"), ("Baz", "Tar")]),
            " and not (./tag[@k='Foo' and @v='Bar'] and ./tag[@k='Baz' and @v='Tar'])",
        )

    def test__tag_matcher(self):
        self.assertTupleEqual(
            OsmData._tag_matcher(
                [("Foo", "Bar"), ("Bazz",)], [], truth_operator=("and", "and")
            ),
            ("./tag[@k='Bazz'] and ./tag[@k='Foo' and @v='Bar']", ""),
        )
        self.assertTupleEqual(
            OsmData._tag_matcher(
                [("Foo", "Bar"), ("Bazz",)], [], truth_operator=("or", "and")
            ),
            ("./tag[@k='Bazz'] or ./tag[@k='Foo' and @v='Bar']", ""),
        )

        self.assertTupleEqual(
            OsmData._tag_matcher(
                [], [("Foo", "Bar"), ("Bazz",)], truth_operator=("and", "and")
            ),
            ("", "./tag[@k='Bazz'] and ./tag[@k='Foo' and @v='Bar']"),
        )
        self.assertTupleEqual(
            OsmData._tag_matcher(
                [], [("Foo", "Bar"), ("Bazz",)], truth_operator=("and", "or")
            ),
            ("", "./tag[@k='Bazz'] or ./tag[@k='Foo' and @v='Bar']"),
        )

        self.assertTupleEqual(
            OsmData._tag_matcher([], [], truth_operator=("and", "or")), ("", "")
        )

        self.assertTupleEqual(
            OsmData._tag_matcher(
                [("A", "A"), ("B", "B")],
                [("C", "C"), ("D", "D")],
                truth_operator=("or", "and"),
            ),
            (
                "./tag[@k='A' and @v='A'] or ./tag[@k='B' and @v='B']",
                "./tag[@k='C' and @v='C'] and ./tag[@k='D' and @v='D']",
            ),
        )

    def test_filter_tag(self):
        ret = self.valid001.filter_tag(include=[("Foo", "Bar")])
        self.assertEqual(len(ret), 0)

        ret = self.valid001.filter_tag(include=[("test", "1")])
        self.assertEqual(len(ret), 1)

        ret = self.valid001.filter_tag(include=[("test",)])
        self.assertEqual(len(ret), 2)

        ret = self.valid001.filter_tag(
            include=[("test",)], exclude=[("rover:id", "-133507")]
        )
        self.assertEqual(len(ret), 1)

        ret = self.valid001.filter_tag(
            include=[("test",)], exclude=[("rover:id",), ("building", "yes")]
        )
        self.assertEqual(len(ret), 2)

        ret = self.valid001.filter_tag(
            include=[("test",)],
            exclude=[("rover:id",), ("building", "yes")],
            truth_operator=("and", "or"),
        )
        self.assertEqual(len(ret), 0)

        ret = self.valid001.filter_tag(
            exclude=[("rover:id",), ("building", "yes")], truth_operator=("and", "or")
        )
        self.assertEqual(len(ret), 0)

        ret = self.valid001.filter_tag(
            include=[("rover:id",), ("building", "yes")], truth_operator=("and", "and")
        )
        self.assertEqual(len(ret), 0)

        ret = self.valid001.filter_tag(
            include=[("rover:id",), ("building", "yes")], truth_operator=("or", "and")
        )
        self.assertEqual(len(ret), 2)

        ret = self.valid001.filter_tag(None, None)
        self.assertEqual(len(ret), 2)

    def test_filter_for_buildings(self):
        ret = self.valid001.filter_for_buildings()
        self.assertEqual(len(ret), 1)
        self.assertEqual(ret[0].get("id"), "173168628")

    def test_filter_for_barrier(self):
        pass

    def test_filter_for_buildings_in_relations(self):
        pass

    def test_tags_to_dict(self):
        element = self.valid001.filter_tag([("rover:id", "-133507")])[0]
        self.assertDictEqual(OsmData.tags_to_dict(element), {})
        self.assertDictEqual(
            OsmData.tags_to_dict(element, ""),
            {
                "test": "1",
                "rover:id": "-133507",
                "rover:obstacle": "yes",
                "rover:obstacle:type": "polygon",
            },
        )
        self.assertDictEqual(
            OsmData.tags_to_dict(element, "rover:"),
            {"id": "-133507", "obstacle": "yes", "obstacle:type": "polygon"},
        )

    def test_utm_lookup(self):
        self.assertNotEqual(self.valid001.utm_lookup(1318876746), ())
        self.assertNotEqual(self.valid001.utm_lookup(-127425), ())
        self.assertRaises(OsmLookupError, self.valid001.utm_lookup, 666)

    def test_latlon_lookup(self):
        self.assertEqual(
            self.valid001.latlon_lookup(1318876746), (48.1608727, 11.5891088)
        )
        self.assertEqual(
            self.valid001.latlon_lookup(-127425), (48.16255438099, 11.58692204178)
        )
        self.assertRaises(OsmLookupError, self.valid001.latlon_lookup, 666)

    def test_tag_update_or_create(self):
        element = self.valid001.filter_tag([("rover:id", "-133507")])[0]
        self.assertEqual(element.xpath("./tag[@k='test']/@v")[0], "1")
        OsmData.tag_update_or_create(element, "test", "3")
        self.assertEqual(element.xpath("./tag[@k='test']/@v")[0], "3")

        OsmData.tag_update_or_create(element, "test5", 42)
        self.assertEqual(element.xpath("./tag[@k='test5']/@v")[0], "42")

        OsmData.tag_update_or_create(element, "test5", 69)
        self.assertEqual(element.xpath("./tag[@k='test5']/@v")[0], "69")

        self.assertEqual(len(element.xpath("./tag[@k='test2']")), 0)
        OsmData.tag_update_or_create(element, "test2", "Foo")
        self.assertEqual(element.xpath("./tag[@k='test2']/@v")[0], "Foo")

    def test_element_contains_tag(self):
        element = self.valid001.filter_tag([("rover:id", "-133507")])[0]
        self.assertTrue(OsmData.element_contains_tag(element, "rover:id", "-133507"))
        self.assertTrue(OsmData.element_contains_tag(element, "test", "1"))
        self.assertFalse(OsmData.element_contains_tag(element, "building", "yes"))

    def test_element(self):
        element = self.valid001.element("/osm/bounds")
        self.assertEqual(
            element.get("origin"), "CGImap 0.7.5 (31203 thorn-02.openstreetmap.org)"
        )
        self.assertRaises(OsmLookupError, self.valid001.element, "/foo/bar")
        self.assertRaises(OsmLookupError, self.valid001.element, "/osm/way")

    def test_elements(self):
        elements = self.valid001.elements("/osm/way")
        self.assertEqual(len(elements), 2)
        elements = self.valid001.elements("/osm/bounds")
        self.assertEqual(len(elements), 1)
        self.assertRaises(OsmLookupError, self.valid001.elements, "/foo/bar")

    def test_node(self):
        node = self.valid001.node(1318876746)
        self.assertEqual(node.get("user"), "KonB")
        self.assertRaises(OsmLookupError, self.valid001.node, 1318876746, rover_id=True)

        self.assertRaises(OsmLookupError, self.valid001.node, 666)
        node = self.valid001.node(333, rover_id=True)
        self.assertEqual(node.get("id"), "-127405")

    def test_node_exists(self):
        self.assertTrue(self.valid001.node_exists(1318876746))
        self.assertFalse(self.valid001.node_exists(1318876746, rover_id=True))
        self.assertTrue(self.valid001.node_exists(-127407))
        self.assertFalse(self.valid001.node_exists(-666))
        self.assertTrue(self.valid001.node(333, rover_id=True))
        self.assertTrue(self.valid001.node(-127405, rover_id=False))

    def test_node_add(self):

        self.assertEqual(len(self.valid001.nodes), 6)
        self.assertEqual(
            self.valid001.node_add((48.16256981067, 11.5868239213)), -127405
        )
        self.assertEqual(len(self.valid001.nodes), 6)

        self.assertRaises(RuntimeError, self.valid001.node_add, (45.66, 11.66))
        self.assertEqual(len(self.valid001.nodes), 6)

        node_id = self.valid001.node_add((48.16, 11.59))
        self.assertEqual(self.valid001.latlon_lookup(node_id), (48.16, 11.59))
        self.assertEqual(
            self.valid001.lookup.latlon_to_node.get((48.16, 11.59)), node_id
        )
        self.assertEqual(len(self.valid001.nodes), 7)

    def test_node_remove(self):
        node_id = 1318876746
        self.assertTrue(self.valid001.node_exists(node_id))
        self.valid001.utm_lookup(node_id)
        self.valid001.latlon_lookup(node_id)

        self.valid001.node_remove(node_id)
        self.assertFalse(self.valid001.node_exists(node_id))
        self.assertRaises(OsmLookupError, self.valid001.utm_lookup, node_id)
        self.assertRaises(OsmLookupError, self.valid001.latlon_lookup, node_id)

    def test_nodes_find_with_tags(self):
        self.assertEqual(len(self.valid001.nodes_find_with_tags([("rover:id",)])), 1)
        self.assertEqual(
            len(self.valid001.nodes_find_with_tags(exc_tag=[("rover:id",)])), 5
        )

    def test_node_add_tag(self):
        node_id = -127405
        node = self.valid001.node(node_id)
        self.assertListEqual(node.xpath("./tag[@k='foo']/@v"), [])
        self.assertListEqual(self.valid001.nodes_find_with_tags([("foo", "bar")]), [])
        self.valid001.node_add_tag(node_id, "foo", "bar")
        self.assertEqual(node.xpath("./tag[@k='foo']/@v"), ["bar"])

    def test_nodes_not_used(self):
        self.assertSetEqual(
            self.valid001.nodes_not_used([-127405, 1318876746, 1324414637]), set()
        )
        self.assertSetEqual(
            self.invalid001.nodes_not_used([-127433, -127405, 1318876746, 1324414637]),
            {-127433},
        )

    def test_nodes_to_latlon(self):
        self.assertListEqual(
            self.valid001.nodes_to_latlon([-127405, 1318876746]),
            [(48.16256981067, 11.5868239213), (48.1608727, 11.5891088)],
        )

    def test_nodes_to_utm(self):
        self.assertListEqual(
            self.valid001.nodes_to_utm([-127405, 1318876746]),
            [
                (692351.2680216449, 5337605.595388093),
                (692527.5088385276, 5337422.705304472),
            ],
        )

    def test_way(self):
        self.assertEqual(self.valid001.way(173168628).get("id"), "173168628")
        self.assertRaises(OsmLookupError, self.valid001.way, 555)

        self.assertRaises(OsmLookupError, self.valid001.way, 555, rover_id=True)
        self.assertEqual(
            self.valid001.way(-133507, rover_id=True).get("id"), "-999069167"
        )

    def test_way_exists(self):
        self.assertTrue(self.valid001.way_exists(173168628))
        self.assertFalse(self.valid001.way_exists(173168628, rover_id=True))

        self.assertFalse(self.valid001.way_exists(-133507, rover_id=False))
        self.assertTrue(self.valid001.way_exists(-133507, rover_id=True))

    def test_way_add_1(self):
        way = Way.create_with_refs(3, [-127405, 1318876746, 1324414637])
        self.valid001.way_add(way)
        self.assertTrue(self.valid001.way_exists(3))

    def test_way_add_2(self):
        # duplicated way id
        way = Way.create_with_refs(173168628, [-127405, 1318876746, 1324414637])
        self.assertRaises(OsmLookupError, self.valid001.way_add, way)

    def test_way_add_3(self):
        # missing reference
        way = Way.create_with_refs(3, [6, 1318876746, 1324414637])
        self.assertRaises(OsmLookupError, self.valid001.way_add, way)

    def test_way_create_from_polyline(self):
        utm_coords = self.valid002.lookup.node_to_utm.values()
        line = self.valid002.way(-999069173)
        self.assertEqual(line.xpath("./tag[@k='rover:id']/@v"), ["-133513"])
        self.assertEqual(line.xpath("./tag[@k='rover:obstacle:type']/@v"), ["line"])

        new_way, _, _ = self.valid002.way_create_from_polyline(utm_coords)
        self.assertTrue(self.valid002.way_exists(new_way.id))
        self.assertEqual(len(new_way.nds), 9)  # start == end

        self.assertEqual(len(self.valid002.nodes), 4 + 8)  # only

    def test_way_is_closed(self):
        self.assertTrue(self.valid001.way_is_closed(173168628))
        self.assertFalse(self.valid002.way_is_closed(-999069173))

    def test_way_node_refs(self):
        self.assertListEqual(
            self.valid001.way_node_refs(-999069167),
            [-127405, -127407, -127425, -127405],
        )
        self.assertRaises(OsmLookupError, self.valid001.way_node_refs, 666)

    def test_way_remove(self):
        self.assertEqual(len(self.valid001.nodes), 6)
        self.assertEqual(len(self.valid001.ways), 2)
        self.valid001.way_remove(-999069167)
        self.assertEqual(len(self.valid001.nodes), 3)
        self.assertEqual(len(self.valid001.ways), 1)

        self.assertFalse(self.valid001.way_exists(-999069167))
        self.assertRaises(OsmLookupError, self.valid001.utm_lookup, -127405)
        self.assertRaises(OsmLookupError, self.valid001.latlon_lookup, -127407)

    def test_way_add_tag(self):
        pass

    def test_ways_find_with_tags(self):
        pass

    def test_create_convex_hull(self):
        el = self.valid003.ways_find_with_tags(
            inc_tag=[("rover:obstacle:hull",), ("rover:obstacle:ignore", "yes")]
        )
        self.assertEqual(len(el), 0)
        hull_id = self.valid003.create_convex_hull([39057410, 36942797])
        el = self.valid003.ways_find_with_tags(
            inc_tag=[("rover:obstacle:hull",), ("rover:obstacle:ignore", "yes")]
        )
        self.assertEqual(len(el), 2)

        self.assertListEqual(
            self.valid003.way_node_refs(hull_id),
            [
                429583851,
                429583850,
                429583279,
                429583282,
                429583298,
                429583853,
                429583852,
                429583851,
            ],
        )
        self.assertEqual(
            len(
                self.valid003.ways_find_with_tags(
                    inc_tag=[
                        ("rover:id", str(np.abs(hull_id))),
                        ("rover:obstacle", "convex-hull"),
                        ("rover:obstacle:hull", "-1"),
                    ]
                )
            ),
            1,
        )

    def test_lint_add_ids(self):
        self.assertEqual(len(self.invalid002.xml.xpath("/osm/way[@id < 0]")), 1)
        self.assertEqual(
            len(
                self.invalid002.xml.xpath(
                    "/osm/way[@id < 0 and not (./tag[@k='rover:id'])]"
                )
            ),
            1,
        )

        self.assertEqual(len(self.invalid002.xml.xpath("/osm/node[@id < 0]")), 4)
        self.assertEqual(
            len(
                self.invalid002.xml.xpath(
                    "/osm/node[@id < 0 and not (./tag[@k='rover:id'])]"
                )
            ),
            3,
        )

        self.invalid002.lint_add_ids(dry_run=True)

        self.assertEqual(len(self.invalid002.xml.xpath("/osm/way[@id < 0]")), 1)
        self.assertEqual(
            len(
                self.invalid002.xml.xpath(
                    "/osm/way[@id < 0 and not (./tag[@k='rover:id'])]"
                )
            ),
            1,
        )

        self.assertEqual(len(self.invalid002.xml.xpath("/osm/node[@id < 0]")), 4)
        self.assertEqual(
            len(
                self.invalid002.xml.xpath(
                    "/osm/node[@id < 0 and not (./tag[@k='rover:id'])]"
                )
            ),
            3,
        )

        self.invalid002.lint_add_ids(dry_run=False)

        self.assertEqual(len(self.invalid002.xml.xpath("/osm/way[@id < 0]")), 1)
        self.assertEqual(
            len(
                self.invalid002.xml.xpath(
                    "/osm/way[@id < 0 and not (./tag[@k='rover:id'])]"
                )
            ),
            0,
        )

        self.assertEqual(len(self.invalid002.xml.xpath("/osm/node[@id < 0]")), 4)
        self.assertEqual(
            len(
                self.invalid002.xml.xpath(
                    "/osm/node[@id < 0 and not (./tag[@k='rover:id'])]"
                )
            ),
            0,
        )

    def test_lint_unique_ids(self):
        ways, nodes = self.invalid003.lint_unique_ids()
        self.assertSetEqual(ways, {"-999069167"})
        self.assertSetEqual(nodes, {"-127405"})

        ways, nodes = self.valid001.lint_unique_ids()
        self.assertSetEqual(ways, set())
        self.assertSetEqual(nodes, set())

    def test_lint_check_obstacles(self):
        invalid_polygons, multiple_used_nodes = self.invalid004.lint_check_obstacles()
        self.assertEqual(len(invalid_polygons), 2)
        self.assertEqual(len(multiple_used_nodes), 2)

        invalid_polygons, multiple_used_nodes = self.valid004.lint_check_obstacles()
        self.assertEqual(len(invalid_polygons), 0)
        self.assertEqual(len(multiple_used_nodes), 0)

    def test_lint_cleanup_unused_nodes(self):
        self.assertRaises(OsmLookupError, self.invalid003.lint_cleanup_unused_nodes)

        self.assertEqual(len(self.invalid005.xml.xpath("/osm/node")), 7)
        self.invalid005.lint_cleanup_unused_nodes(dry_run=True)
        self.assertEqual(len(self.invalid005.xml.xpath("/osm/node")), 7)
        self.invalid005.lint_cleanup_unused_nodes(dry_run=False)
        self.assertEqual(len(self.invalid005.xml.xpath("/osm/node")), 6)


if __name__ == "__main__":
    unittest.main()
