import itertools
import logging
import random
from typing import Any, List, Set, Tuple

import numpy as np
import utm as utm_latlog_converter
from lxml import etree
from lxml.etree import Element, ElementTree
from scipy.spatial import ConvexHull


"""
rover:id  (will mimic osm id)
rover:obstacle:hull = [id of hull this element belongs to or -1 if this is one]
rover:obstacle:line:hull = [id of line-hull this element belongs to or -1 if this is a line hull] 
rover:obstacle.ignore = [yes | no] (should converter ignore this?)
rover:obstacle:type = [polygon, line] (manually added obstacles of given type)

rover:obstacle = [yes, wall, building, ...] (informative, must be polygons)
-------
osm-Tags

tag(barrier, wall) -> lines which must be made to line-polygons ==> obstacles
tag(building, *) -> this are polygons ==> obstacles 
[complex buildings] -> this are polygons ==> obstacles 

"""


class Node:
    def __init__(self):
        self.id = -1
        self.action = "modify"
        self.visible = "true"
        self.lat = 0.0
        self.lon = 0.0
        self.tags: List[Tag] = []

    @classmethod
    def create_node(cls, node_id, lat, lon):
        n = cls()
        n.id = node_id
        n.add_tag("rover:id", np.abs(n.id))
        n.lat = lat
        n.lon = lon
        return n

    def add_tag(self, key, value):
        self.tags.append(Tag(key, value))

    def to_xml(self) -> Element:
        element = Element("node")
        element.set("id", str(self.id))
        element.set("action", str(self.action))
        element.set("visible", str(self.visible))
        element.set("lon", str(self.lon))
        element.set("lat", str(self.lat))

        for tag in self.tags:
            element.append(tag.to_xml())
        return element

    def __eq__(self, other):
        return (
            self.id == other.id
            and self.action == other.action
            and self.visible == other.visible
            and self.lat == other.lat
            and self.lon == other.lon
            and sorted(self.tags) == sorted(other.tags)
        )


class Nd:
    def __init__(self, ref_id):
        self.id = ref_id

    def to_xml(self):
        element = Element("nd")
        element.set("ref", str(self.id))
        return element

    def __eq__(self, other):
        return self.id == other.id


class Tag:
    def __init__(self, k, v):
        self.k = k
        self.v = v

    def to_xml(self):
        element = Element("tag")
        for k, v in self.__dict__.items():
            element.set(k, str(v))
        return element

    def __eq__(self, other):
        return self.k == other.k and self.v == other.v


class Way:
    def __init__(self):
        self.id = -1
        self.action = "modify"
        self.visible = "true"
        self.attr = {}
        self.nds = []
        self.tags = []

    @classmethod
    def create_with_refs(
        cls, way_id, refs: List[int], att: dict = None, tags: dict = None
    ):
        w = cls()
        w.id = way_id
        w.add_tag("rover:id", np.abs(w.id))
        for ref_id in refs:
            w.nds.append(Nd(ref_id))
        if tags is not None:
            for k, v in tags.items():
                w.add_tag(k, v)
        if att is not None:
            w.attr.update(att)
        return w

    @classmethod
    def create(cls, way_id, nodes: List[Node], att: dict = None, tags: dict = None):
        w = cls()
        w.id = way_id
        w.add_tag("rover:id", np.abs(w.id))
        for n in nodes:
            w.nds.append(Nd(n.id))
        if tags is not None:
            for k, v in tags.items():
                w.add_tag(k, v)
        if att is not None:
            w.attr.update(att)
        return w

    def add_tag(self, key, value):
        self.tags.append(Tag(key, value))

    def to_xml(self) -> Element:
        element = Element("way")
        element.set("id", str(self.id))
        element.set("action", str(self.action))
        element.set("visible", str(self.visible))

        for k, v in self.attr.items():
            element.set(k, str(v))

        for nd in self.nds:
            element.append(nd.to_xml())

        for tag in self.tags:
            element.append(tag.to_xml())

        return element


class PathToPolygon:
    def __init__(self, list_of_tuples, dist):
        self.points = [np.array([p[0], p[1]]) for p in list_of_tuples]
        self.dist = dist
        self.rotPositive = np.array(((0, -1), (1, 0)))
        self.rotNegative = np.array(((0, 1), (-1, 0)))
        self.polygon = []
        self._create_poly_points()

    @classmethod
    def get_poly_object(cls, list_of_tuples, dist, _id):
        ptp = cls(list_of_tuples, dist)
        return PolyObjectWidthId(_id, ptp.polygon)

    def _create_poly_points(self):
        lines_o1 = []
        lines_o2 = []
        for a, b in self._pairwise(self.points):
            lines = self._parallel_lines([a, b], self.dist)
            lines_o1.append(lines[0])
            lines_o2.append(lines[1])

        path_o1 = self._offset_line(lines_o1)
        path_o2 = self._offset_line(lines_o2)

        self.polygon.extend(path_o1)
        path_o2.reverse()
        self.polygon.extend(path_o2)

        if not self._polygon_closed():
            self.polygon.append(self.polygon[0])

    def _polygon_closed(self):
        if len(self.polygon) < 2:
            return False

        return np.array_equal(self.polygon[0], self.polygon[-1])

    def _offset_line(self, lines):
        points = [lines[0][0]]
        for l1, l2 in self._pairwise(lines):
            points.append(self._line_intersection(l1, l2))
        points.append(lines[-1][-1])  # last line last point
        return points

    @staticmethod
    def _pairwise(iterable):
        """s -> (s0,s1), (s1,s2), (s2, s3), ..."""
        a, b = itertools.tee(iterable)
        next(b, None)
        return zip(a, b)

    @staticmethod
    def _line_intersection(line1, line2):

        s = np.vstack([*line1, *line2])  # s for stacked
        h = np.hstack((s, np.ones((4, 1))))  # h for homogeneous
        l1 = np.cross(h[0], h[1])  # get first line
        l2 = np.cross(h[2], h[3])  # get second line
        x, y, z = np.cross(l1, l2)  # point of intersection
        if np.abs(z) < 1e-3:  # lines are parallel
            print(f"z: {z}")
            return np.array(line1[1])
        return np.array([x / z, y / z])

    def _parallel_lines(self, line, dist):
        """
        create parallel lines moved dist amount away in +-90 deg.
        :param dist:
        :param line: [p1, p2] with p1 = [x1, y1]
        :return: (line_pos, line_neg) at dist from line
        """
        p1, p2 = line[0], line[1]
        v = p2 - p1
        v_normalized = v / np.linalg.norm(v)

        # +90 and stretch by dist
        o1 = dist * np.matmul(self.rotPositive, v_normalized)
        line_o1 = [o1 + p1, o1 + p2]
        # -90 and stretch by dist
        o2 = dist * np.matmul(self.rotNegative, v_normalized)
        line_o2 = [o2 + p1, o2 + p2]

        return [line_o1, line_o2]


class PolyObjectWidthId:
    """
    Simple wrapper class around a list of points (order is important!) within an identifier field (id)
    """

    def __init__(self, poly_id, utm_points: List[Tuple[float, float]]):
        self.id = poly_id
        self.points = utm_points  # [ (x, y), (x, y), ...]
        self.base = None
        self.template_data = {}
        self.template_data.setdefault("id", self.id)

    @classmethod
    def create_closed(cls, poly_id, utm_points: List[Tuple[float, float]]):
        obj = cls(poly_id, utm_points)
        if not obj.closed():
            obj.points.append(obj.points[0])
        return obj

    @classmethod
    def create_open(cls, poly_id, utm_points: List[Tuple[float, float]]):
        obj = cls(poly_id, utm_points)
        if obj.closed():
            obj.points = obj.points[:-1]
        return obj

    @classmethod
    def to_latlon(cls, poly_id, utm_points, zone_number=32, zone_letter="U"):
        latlon_points = []
        for p in utm_points:
            lat, lon = utm_latlog_converter.to_latlon(
                easting=p[0],
                northing=p[1],
                zone_number=zone_number,
                zone_letter=zone_letter,
            )
            latlon_points.append((lat, lon))
        return cls(poly_id, latlon_points)

    def shift_points(self, base: list):
        shift_in_x = -base[0]
        shift_in_y = -base[1]
        self.points = [
            (point[0] + shift_in_x, point[1] + shift_in_y) for point in self.points
        ]
        self.base = base

    def closed(self) -> bool:
        return self.points[0] == self.points[-1]

    def add_template_data(self, key: str, value):
        self.template_data.setdefault(key, str(value))

    def __eq__(self, other):
        return (
            self.id == other.id
            and self.points == other.points
            and self.base == other.base
            and self.template_data == other.template_data
        )


class OsmLookup:
    def __init__(self, strict=True):
        self.strict = strict
        self.node_to_latlon = {}
        self.node_to_utm = {}
        self.latlon_to_node = {}
        self.zone_map = {}
        self.latlon_to_node_errors = {}

    def load(self, nodes: Element):
        self.node_to_latlon = {}
        self.node_to_utm = {}
        self.latlon_to_node = {}
        self.zone_map = {}
        self.latlon_to_node_errors = {}

        for node in nodes:
            latlon_point = (float(node.get("lat")), float(node.get("lon")))
            node_id = int(node.get("id"))
            x, y, zone_number, zone_letter = utm_latlog_converter.from_latlon(
                float(latlon_point[0]), float(latlon_point[1])
            )
            utm_point = (x, y)
            self.zone_map.setdefault((zone_number, zone_letter), None)
            if len(self.zone_map) > 1:
                raise RuntimeError(
                    f"OSM map contains multiple UTM-Zones. This does not work here. {self.zone_map}"
                )

            if node_id in self.node_to_latlon:
                msg = f"node[id: {node_id} lonlat:[{latlon_point} utm:[{utm_point}] already exist."
                if self.strict and node_id < 0:
                    raise OsmLookupError(msg)
                else:
                    print(f"{msg} continue ...")
            self.node_to_latlon.setdefault(node_id, latlon_point)
            self.node_to_utm.setdefault(node_id, utm_point)

            if latlon_point in self.latlon_to_node:
                error_set = self.latlon_to_node_errors.get(latlon_point, set())
                error_set.add(node_id)
                error_set.add(self.latlon_to_node.get(latlon_point))
                self.latlon_to_node_errors.setdefault(latlon_point, error_set)
                msg = (
                    f"node[id: {node_id} lonlat:[{latlon_point} utm:[{utm_point}] already exist in "
                    f"latlon->node: {error_set}"
                )
                if self.strict:
                    if node_id > 0:
                        print(
                            f"found node ids [{error_set}] which point to the same lonlat coordinates. continue because these points belong to osm ... "
                        )
                    else:
                        raise OsmLookupError(msg)
                else:
                    print(f"{msg} continue ...")
            self.latlon_to_node.setdefault(latlon_point, node_id)

    def convert_latlon_to_utm(self, latlon):
        latlon_point = (float(latlon[0]), float(latlon[1]))
        x, y, zone_number, zone_letter = utm_latlog_converter.from_latlon(
            float(latlon_point[0]), float(latlon_point[1])
        )
        utm_point = (x, y)
        self.zone_map.setdefault((zone_number, zone_letter), None)
        if len(self.zone_map) > 1:
            raise RuntimeError(
                f"OSM map contains multiple UTM-Zones. This does not work here. {self.zone_map}"
            )
        return utm_point

    def add(self, node_id, latlon, utm):
        # todo add test before adding
        self.node_to_latlon.setdefault(node_id, latlon)
        self.node_to_utm.setdefault(node_id, utm)

        self.latlon_to_node.setdefault(latlon, node_id)

    def remove(self, node_id):
        # todo add test before removing
        latlon = self.node_to_latlon.get(node_id, None)
        self.node_to_latlon.pop(node_id)
        self.node_to_utm.pop(node_id)

        if latlon is not None:
            self.latlon_to_node.pop(latlon)


class OsmLookupError(LookupError):
    def __init__(self, msg):
        super(OsmLookupError, self).__init__(msg)


class OsmData:
    def __init__(self, input_file, output_file=None, strict=True):
        parser = etree.XMLParser(remove_blank_text=True)
        self.input_file = input_file
        self.output_file = output_file if output_file is not None else input_file
        self.xml: ElementTree = etree.parse(self.input_file, parser).getroot()
        self.osm_root: Element = self.xml.xpath("/osm")[0]

        nodes = self.osm_root.xpath("/osm/node")
        self.lookup = OsmLookup(strict=strict)
        self.lookup.load(nodes)

        self.obstacle_selectors = [
            lambda: self.filter_for_buildings(
                ignore=[("rover:obstacle:ignore", "yes")]
            ),
            lambda: self.filter_for_buildings_in_relations(
                ignore=[("rover:obstacle:ignore", "yes")]
            ),
            lambda: self.filter_tag(
                include=[("rover:obstacle",)],
                exclude=[("rover:obstacle:ignore", "yes")],
            ),
        ]
        self.source_selectors = [
            lambda: self.filter_tag(
                include=[("rover:source",)], exclude=[("rover:source:ignore", "yes")]
            )
        ]
        self.target_selectors = [
            lambda: self.filter_tag(
                include=[("rover:target",)], exclude=[("rover:target:ignore", "yes")]
            )
        ]

        self.measurement_selectors = [
            lambda: self.filter_tag(
                include=[("rover:measurementArea",)],
                exclude=[("rover:measurementArea:ignore", "yes")],
            )
        ]

        print(
            f"loaded {input_file} with {len(self.nodes)} nodes and {len(self.ways)} ways."
        )

    @property
    def base_point(self):
        element = self.element("/osm/bounds")
        return float(element.get("minlat")), float(element.get("minlon"))

    @property
    def nodes(self):
        return self.xml.xpath("/osm/node")

    @property
    def ways(self):
        return self.xml.xpath("/osm/way")

    @property
    def utm_zone(self) -> Tuple[int, str]:
        return list(self.lookup.zone_map.keys())[0]

    @property
    def utm_zone_string(self):
        zone_number, zone_letter = self.utm_zone
        return f"UTM Zone {zone_number}{zone_letter}"

    @staticmethod
    def _xpath_k_v_tags(key_value_list: List[Tuple[str, str]]) -> str:
        xpath = [OsmData._xpath_k_v_tag(*i) for i in key_value_list]
        return f"({' and '.join(xpath)})"

    @staticmethod
    def _xpath_k_v_tag(k: str, v: str):
        return f"./tag[@k='{k}' and @v='{v}']"

    @staticmethod
    def _add_ignore(ignore: List[Tuple[str, str]]):
        if ignore is None or len(ignore) < 1:
            return ""
        else:
            return f" and not {OsmData._xpath_k_v_tags(ignore)}"

    @staticmethod
    def _tag_matcher(
        inc: List[Tuple], exc: List[Tuple], truth_operator: Tuple[str, str]
    ) -> Tuple[str, str]:

        include = []
        if inc is not None:
            include_tag_key = [element[0] for element in inc if len(element) == 1]
            inc_1 = [f"./tag[@k='{k}']" for k in include_tag_key]
            include_tag_key_value_pair = [
                element for element in inc if len(element) == 2
            ]
            inc_2 = [
                f"./tag[@k='{e[0]}' and @v='{e[1]}']"
                for e in include_tag_key_value_pair
            ]
            include.extend(inc_1)
            include.extend(inc_2)

        exclude = []
        if exc is not None:
            exclude_tag_key = [element[0] for element in exc if len(element) == 1]
            exc_1 = [f"./tag[@k='{k}']" for k in exclude_tag_key]
            exclude_tag_key_value_pair = [
                element for element in exc if len(element) == 2
            ]
            exc_2 = [
                f"./tag[@k='{e[0]}' and @v='{e[1]}']"
                for e in exclude_tag_key_value_pair
            ]
            exclude.extend(exc_1)
            exclude.extend(exc_2)

        return (
            f" {truth_operator[0]} ".join(include),
            f" {truth_operator[1]} ".join(exclude),
        )

    def filter_tag(
        self,
        include: List[Tuple] = None,
        exclude: List[Tuple] = None,
        truth_operator: Tuple[str, str] = ("and", "and"),
    ) -> List[Element]:
        """
        creates xpath string which will return all elements contains tags with a specific key (e.g ('key2') without
        checking the value or in the case of ('key1', 'val2') the value will be checked. All elements in the include or
        exclude list are concatenated with an 'and' operator
        :param truth_operator:
        :param include: list of the form [('key1', 'val1'), ('key1', 'val2'), ('key2'), ...]
        :param exclude: list of the form [('key1', 'val1'), ('key1', 'val2'), ('key2'), ...]
        :return:
        """

        include_xpath, exclude_xpath = OsmData._tag_matcher(
            include, exclude, truth_operator
        )

        if include_xpath == "" and exclude_xpath != "":
            xpath = f"/osm/way[not ({exclude_xpath}) ]"
        elif include_xpath != "" and exclude_xpath == "":
            xpath = f"/osm/way[ {include_xpath} ]"
        elif include_xpath != "" and exclude_xpath != "":
            xpath = f"/osm/way[({include_xpath}) and not ({exclude_xpath}) ]"
        else:
            xpath = f"/osm/way"

        return self.xml.xpath(xpath)

    def filter_for_buildings(self, ignore: list = None) -> List[Element]:
        xpath = f"/osm/way[./tag/@k='building' {OsmData._add_ignore(ignore)}]"
        return self.xml.xpath(xpath)

    def filter_for_barrier(self, ignore: list = None) -> List[Element]:
        xpath = f"/osm/way[./tag/@k='barrier' {OsmData._add_ignore(ignore)}]"
        return self.xml.xpath(xpath)

    def filter_for_buildings_in_relations(self, ignore: list = None):
        # Note: A relation describes a shape with "cutting holes".

        # Select "relation" nodes with a child node "tag" annotated with attribute "k='building'".
        xpath = f"/osm/relation[./tag/@k='building' {OsmData._add_ignore(ignore)}]"
        buildings = self.xml.xpath(xpath)

        # We only want the shapes and only the outer parts. role='inner' is for "cutting holes" in the shape.
        members_in_the_relations = [
            building.xpath("./member[./@type='way' and ./@role='outer']")
            for building in buildings
        ]
        way_ids = []
        for element in members_in_the_relations:
            for way in element:
                way_ids.append(way.get("ref"))
        ways = self.xml.xpath("/osm/way")
        ways_as_dict_with_id_key = {way.get("id"): way for way in ways}
        buildings_from_relations = [
            ways_as_dict_with_id_key[way_id] for way_id in way_ids
        ]
        return buildings_from_relations

    @staticmethod
    def tags_to_dict(element: Element, namespace: str = None) -> dict:
        tags = element.xpath("./tag")
        tag_dic = {}
        if (len(tags)) == 0 or namespace is None:
            return tag_dic
        else:
            for tag in tags:
                key_value = tag.xpath("./@*")
                if namespace == "*":
                    tag_dic.setdefault(key_value[0], key_value[1])
                else:
                    if key_value[0].startswith(namespace):
                        key = key_value[0][len(namespace) :]
                        tag_dic.setdefault(key, key_value[1])

        return tag_dic

    def utm_lookup(self, node_id: int) -> ():
        if node_id in self.lookup.node_to_utm:
            return self.lookup.node_to_utm.get(node_id)
        else:
            raise OsmLookupError(f"no utm coordinate found for node id: {node_id}")

    def latlon_lookup(self, node_id: int) -> ():
        if node_id in self.lookup.node_to_latlon:
            return self.lookup.node_to_latlon.get(node_id)
        else:
            raise OsmLookupError(f"no latlon coordinate found for node id: {node_id}")

    @staticmethod
    def tag_update_or_create(element: Element, key: str, value):
        ret_list = element.xpath(f"./tag[@k='{key}']")
        if len(ret_list) == 1:
            # found --> update
            tag = ret_list[0]
            tag.set("v", str(value))
        else:
            # not found --> create and add child
            tag = Tag(key, value)
            element.append(tag.to_xml())

    @staticmethod
    def element_contains_tag(element: Element, key: str, value=None) -> bool:
        if value is None:
            xpath = f"./tag[@k='{key}']"
        else:
            xpath = f"./tag[@k='{key}' and @v='{value}']"
        ret_list = element.xpath(xpath)
        return len(ret_list) == 1

    def element(self, xpath: str) -> Element:
        ret_list = self.xml.xpath(xpath)
        if len(ret_list) == 1:
            return ret_list[0]
        else:
            raise OsmLookupError(
                f"no or to many matches for xpath '{xpath}' (found '{len(ret_list)}' matches)"
            )

    def elements(self, xpath: str) -> List[Element]:
        ret_list = self.xml.xpath(xpath)
        if len(ret_list) > 0:
            return ret_list
        else:
            raise OsmLookupError(f"no matches for xpath '{xpath}'")

    def next_id(self):
        ids = set(self.osm_root.xpath("//*[@id<0]/@id"))
        next_int = random.randint(-2_147_483_648, -1)
        while next_int in ids:
            next_int = random.randint(-2_147_483_648, -1)
        return next_int

    def node(self, node_id, rover_id: bool = False) -> Element:
        if rover_id:
            ret_list = self.nodes_find_with_tags(inc_tag=[("rover:id", node_id)])
            msg = f"cannot find node element for rover:id = '{node_id}'"
        else:
            ret_list = self.osm_root.xpath(f"/osm/node[@id={node_id}]")
            msg = f"cannot find node element for id = {node_id}"
        if len(ret_list) == 1:
            return ret_list[0]
        else:
            raise OsmLookupError(msg)

    def node_exists(self, node_id, rover_id: bool = False) -> bool:
        try:
            _ = self.node(node_id, rover_id)
            return True
        except OsmLookupError:
            return False

    def node_add(self, latlon_point):

        if latlon_point in self.lookup.latlon_to_node:
            return self.lookup.latlon_to_node.get(latlon_point)

        next_id = self.next_id()
        node = Node.create_node(next_id, latlon_point[0], latlon_point[1])
        x, y, z_number, z_letter = utm_latlog_converter.from_latlon(
            float(node.lat), float(node.lon)
        )

        if self.utm_zone != (z_number, z_letter):
            raise RuntimeError(
                f"given latlon coordinate {latlon_point} is in wrong utm zone {(z_number, z_letter)} != {self.utm_zone} "
            )

        self.osm_root.append(node.to_xml())
        try:
            _ = self.node(node.id)
        except OsmLookupError:
            raise OsmLookupError(
                f"error finding added node element {etree.tostring(node.to_xml())}"
            )
        # add new node to lookup
        self.lookup.add(node.id, latlon=(node.lat, node.lon), utm=(x, y))
        return next_id

    def node_remove(self, node_id):
        element: Element = self.node(node_id)
        element.getparent().remove(element)
        self.lookup.remove(node_id)

    def nodes_find_with_tags(
        self,
        inc_tag: List[Tuple] = None,
        exc_tag: List[Tuple] = None,
        truth_operator=("and", "or"),
    ) -> List[Element]:
        inc, exc = OsmData._tag_matcher(inc_tag, exc_tag, truth_operator)
        if len(inc) > 0 and len(exc) > 0:
            xpath = f"/osm/node[{inc} and not ({exc})]"
        elif len(inc) > 0 and len(exc) == 0:
            xpath = f"/osm/node[{inc}]"
        else:
            xpath = f"/osm/node[not({exc})]"
        return self.xml.xpath(xpath)

    def node_add_tag(self, node_id, key: str, value):
        element = self.node(node_id)
        OsmData.tag_update_or_create(element, key, value)

    def nodes_not_used(self, node_ids: List[int]) -> Set[int]:
        all_refs = set([int(i) for i in self.xml.xpath("//*/nd/@ref")])
        return set(node_ids).difference(all_refs)

    def nodes_to_latlon(self, node_ids: List[int]) -> List[Tuple[Any, Any]]:
        return [self.latlon_lookup(node_id) for node_id in node_ids]

    def nodes_to_utm(self, node_ids: List[int]) -> List[Tuple[Any, Any]]:
        return [self.utm_lookup(node_id) for node_id in node_ids]

    def way(self, way_id: int, rover_id: bool = False) -> Element:
        if rover_id:
            ret_list = self.ways_find_with_tags(inc_tag=[("rover:id", way_id)])
            msg = f"cannot find way element for rover:id = '{way_id}'"
        else:
            ret_list = self.osm_root.xpath(f"/osm/way[@id='{way_id}']")
            msg = f"cannot find way element for id = '{way_id}'"

        if len(ret_list) == 1:
            return ret_list[0]
        else:
            raise OsmLookupError(msg)

    def way_exists(self, way_id, rover_id: bool = False):
        try:
            _ = self.way(way_id, rover_id)
            return True
        except OsmLookupError:
            return False

    def way_add(self, new_way: Way) -> int:

        if self.way_exists(new_way.id):
            raise OsmLookupError(f"way id '{new_way.id}' alrady exists")

        for n in new_way.nds:
            if not self.node_exists(n.id):
                raise OsmLookupError(
                    f"way contains reference to non existing nodes ref:{n.id}"
                )

        self.osm_root.append(new_way.to_xml())
        try:
            _ = self.way(new_way.id)
            return new_way.id
        except OsmLookupError as e:
            raise OsmLookupError(
                f"error finding added way element {etree.tostring(new_way.to_xml())} source:{str(e)}"
            )

    def way_create_from_polyline(self, line_utm, dist=0.25):
        """
        Create way element (closed polygon) from the given polyline in utm reference coordinate system.
        This function will first translate the utm coordinates to latlon before adding the new nodes and way elements
        :param line_utm: utm based list of point building a line
        :param dist: halve the width of the polygon created from the line.
        """
        next_id = self.next_id()
        polygon_utm = PathToPolygon.get_poly_object(line_utm, dist, next_id)
        polygon_latlon = PolyObjectWidthId.to_latlon(next_id, polygon_utm.points)

        # create and add nodes to xml
        node_ids = [self.node_add(p) for p in polygon_latlon.points]

        # create way from nodes
        new_way = Way.create_with_refs(self.next_id(), node_ids)

        self.way_add(new_way)

        return new_way, polygon_utm, polygon_latlon

    def way_is_closed(self, way_id):
        """
        If first an last point are the same then the way is closed.
        :return: True if way is closed, otherwise False
        """
        points = self.way_node_refs(way_id)
        return points[0] == points[-1]

    def way_node_refs(self, way_id) -> list:
        ret_list = self.osm_root.xpath(f"/osm/way[@id={way_id}]/nd/@ref")
        if len(ret_list) > 1:
            return [int(i) for i in ret_list]
        else:
            raise OsmLookupError(
                f"way needs at least two referenced nodes id: {way_id}"
            )

    def way_remove(self, way_id):
        element: Element = self.way(way_id)
        refs = self.way_node_refs(way_id)
        element.getparent().remove(element)
        # remove nodes no longer used by anything else
        not_used_node_ids = self.nodes_not_used(refs)
        for node_id in not_used_node_ids:
            self.node_remove(node_id)

    def way_add_tag(self, way_id, key: str, value):
        element = self.way(way_id)
        OsmData.tag_update_or_create(element, key, value)

    def ways_find_with_tags(
        self,
        inc_tag: List[Tuple] = None,
        exc_tag: list = None,
        truth_operator=("and", "or"),
    ):
        inc, exc = OsmData._tag_matcher(inc_tag, exc_tag, truth_operator)
        if len(inc) > 0 and len(exc) > 0:
            xpath = f"/osm/way[{inc} and not ({exc})]"
        elif len(inc) > 0 and len(exc) == 0:
            xpath = f"/osm/way[{inc}]"
        else:
            xpath = f"/osm/way[not({exc})]"
        return self.xml.xpath(xpath)

    def create_convex_hull(self, way_ids: List[int]):
        node_ids = []
        for way_id in way_ids:
            node_ids.extend(self.way_node_refs(way_id))

        # find points on convex hull
        _utm_points = [self.utm_lookup(node_id) for node_id in node_ids]
        id_array = np.array(node_ids)
        hull = ConvexHull(np.array(_utm_points))
        hull_point_ids = list(id_array[hull.vertices])
        hull_point_ids.append(hull_point_ids[0])

        # create way with convex hull
        hull_way = Way.create_with_refs(
            self.next_id(),
            hull_point_ids,
            tags={"rover:obstacle:hull": "-1", "rover:obstacle": "convex-hull"},
        )
        self.way_add(hull_way)

        # ignore ways incorporated in hull_way
        for way_id in way_ids:
            self.way_add_tag(way_id, "rover:obstacle:hull", hull_way.id)
            self.way_add_tag(way_id, "rover:obstacle:ignore", "yes")

        print(f"created new hull-way (id:{hull_way.id}) containing {way_ids}")
        return hull_way.id

    def contained_in_area_of_intrest(self, aoi, way: Element):
        min_lat = aoi[0][0]
        max_lat = aoi[0][1]
        min_lon = aoi[1][0]
        max_lon = aoi[1][1]
        nodes = [self.lookup.node_to_latlon[int(id)] for id in way.xpath("nd/@ref")]

        for n in nodes:
            if n[0] < min_lat or n[0] > max_lat or n[1] < min_lon or n[1] > max_lon:
                return False

        return True

    def get_area_of_intrest(self):
        """
        Return lat/lon min/max values for element within area of intrest
        :return:
        """
        self.xml.xpath("/osm/way/tag[@k='vadere:area-of-intrest']")

        lat = [0, 0]
        lon = [0, 0]
        nodes = [
            self.lookup.node_to_latlon[int(id)]
            for id in self.xml.xpath(
                "/osm/way[./tag/@k='vadere:area-of-intrest']/nd/@ref"
            )
        ][:-1]

        if len(nodes) == 0:
            raise RuntimeError(
                f"No area of interst found. Map does not contain a way with the tag 'vadere:area-of-intrest'"
            )
        elif len(nodes) < 2:
            raise RuntimeError(
                f"area-of-intrest path contains only '{len(nodes)}' nodes. Need at least 3 nodes to "
                f"create area of interest."
            )

        node_lat = [n[0] for n in nodes]
        node_lon = [n[1] for n in nodes]

        return ([min(node_lat), max(node_lat)], [min(node_lon), max(node_lon)])

    def lint_add_ids(self, dry_run=False):
        ways_without_id: List[Element] = self.xml.xpath(
            "/osm/way[@id < 0 and not (./tag[@k='rover:id'])]"
        )
        print(f"found {len(ways_without_id)} way elements without id")
        for idx, e in enumerate(ways_without_id):
            way_id = e.get("id")
            print(
                f" *** ({idx}/{len(ways_without_id)}) add tag: <tag k='rover:id' v='{np.abs(int(way_id))}'/> to way {way_id} "
            )
            if not dry_run:
                self.way_add_tag(way_id, "rover:id", np.abs(int(way_id)))

        nodes_without_id: List[Element] = self.xml.xpath(
            "/osm/node[@id < 0 and not (./tag[@k='rover:id'])]"
        )
        print(f"found {len(nodes_without_id)} node elements without id")
        for idx, n in enumerate(nodes_without_id):
            node_id = n.get("id")
            print(
                f" *** ({idx}/{len(nodes_without_id)}) add tag: <tag k='rover:id' v='{np.abs(int(node_id))}'/> to node {node_id} "
            )
            if not dry_run:
                self.node_add_tag(node_id, "rover:id", np.abs(int(node_id)))

    def lint_unique_ids(self):
        way_ids = self.xml.xpath("/osm/way/@id")
        way_id_set = set()
        duplicate_ways = set()
        for w in way_ids:
            if w in way_id_set:
                duplicate_ways.add(w)
            else:
                way_id_set.add(w)
        node_ids = self.xml.xpath("/osm/node/@id")
        node_id_set = set()
        duplicate_nodes = set()
        for n in node_ids:
            if n in node_id_set:
                duplicate_nodes.add(n)
            else:
                node_id_set.add(n)

        if len(duplicate_ways) == len(duplicate_nodes) == 0:
            print("no duplicate ways or nodes found")
        else:
            print(
                f"duplicate ways: {duplicate_ways}, duplicate nodes: {duplicate_nodes}"
            )

        return duplicate_ways, duplicate_nodes

    def lint_check_obstacles(self):
        obstacle_elements = []
        invalid_obstacles = dict()
        for selector in self.obstacle_selectors:
            obstacle_elements.extend(selector())
        node_refs = {
            way.get("id"): self.way_node_refs(way.get("id"))
            for way in obstacle_elements
        }

        for way_id, nodes in node_refs.items():
            if len(nodes) < 3:
                invalid_obstacles.setdefault(way_id, nodes)
                print(
                    f"invalid polygons found. Way contains less than 3 nodes. way-id: {way_id}"
                )
            elif nodes[0] == nodes[-1]:
                node_refs[way_id] = nodes[:-1]
            else:
                invalid_obstacles.setdefault(way_id, nodes)
                print(
                    f"invalid polygon found. First and last node ref do not match. way-id: {way_id}"
                )

        way_refs = {}
        for way_key, node_list in node_refs.items():
            for node in node_list:
                way_keys = way_refs.get(node, [])
                way_keys.append(way_key)
                way_refs.setdefault(node, way_keys)

        multiple_used_nodes = {k: v for k, v in way_refs.items() if len(v) > 1}
        if len(multiple_used_nodes) > 0:
            print(
                f"some nodes ware used by multiple polygons. This is a problem for mesching..."
            )
            print(f"   {multiple_used_nodes}")
        else:
            print(f"no multiple used node found")

        return invalid_obstacles, multiple_used_nodes

    def lint_cleanup_unused_nodes(self, dry_run=False):
        ref_ids = self.xml.xpath("//*/nd[@ref < 0]/@ref")
        node_ids = self.xml.xpath("/osm/node[@id < 0]/@id")
        ref_ids_set = set(ref_ids)
        node_ids_set = set(node_ids)
        if len(node_ids) != len(node_ids_set):
            raise OsmLookupError("Duplicate Ids exist")

        unused_nodes = node_ids_set.difference(ref_ids_set)
        print(f"{len(unused_nodes)} unused nodes found")
        for node_id in unused_nodes:
            print(f"remove node {node_id}")
            if not dry_run:
                self.node_remove(int(node_id))

    def save(self):
        with open(self.output_file, "wb") as f:
            f.write(
                etree.tostring(
                    self.xml, encoding="utf8", pretty_print=True, xml_declaration=True
                )
            )

        print(f"saved in {self.output_file}")


class OsmCfgParser:
    def __init__(self, data: dict):
        self.data = data

    def pre_group(self, group: str, curr_line: int):
        return

    def post_group(self, group: str, curr_line: int):
        return

    def parse_line(self, active_group: str, striped_line: str, curr_line: int):
        if striped_line == "":
            return

        if self._is_key_value_pair(striped_line):
            kv_dict = self._get_key_value_pair(striped_line)
            self.data[active_group].update(kv_dict)
        else:
            logging.warning(
                f"line{curr_line} not an key-value pair. (ignore) '{striped_line}' "
            )

    def _add_to_group(self, group_name, key_value_pair: dict):
        self.data[group_name].update(key_value_pair)

    @staticmethod
    def _parse_scalar(value: str):
        try:
            return float(value)
        except ValueError:
            if value in ["true", "True"]:
                return True
            if value in ["false", "False"]:
                return False
            return value

    @staticmethod
    def _get_key_value_pair(line: str, default: dict = None):
        if OsmCfgParser._is_key_value_pair(line):
            ret = line.strip().split("=")
            if len(ret) != 2:
                raise ValueError("A key value pair must contain only one '=' ")

            key = ret[0].strip()
            val = ret[1].strip()
            # scalar or list
            if OsmCfgParser._is_scalar_value(val):
                kv_pair = {key: OsmCfgParser._parse_scalar(val)}
            else:
                # list value
                val = [
                    item.strip() for item in val.split(";")
                ]  # remove all whitespaces and split on ";"
                if "" in val:
                    val.remove("")  # remove empty elements
                kv_pair = {key: val}

            return kv_pair
        elif default is not None:
            return default
        else:
            raise ValueError(f"Line does not contain a key value pair: '{line}'")

    @staticmethod
    def _is_key_value_pair(line: str):
        return "=" in line

    @staticmethod
    def _is_scalar_value(val: str):
        return ";" not in val

    @staticmethod
    def _is_list_value(val: str):
        return ";" in val


class OsmConfigTextGroupParser(OsmCfgParser):
    def __init__(self, data: dict):
        super().__init__(data)
        self._simple_group_id = 0
        self._simple_group = []

    def pre_group(self, group: str, curr_line: int):
        """
        Ensure clean startup state.
        """
        self._simple_group_id = 0
        self._simple_group = []

    def post_group(self, group: str, curr_line: int):
        """
        In the case  the last group of way id's did not end with a new line write the data in #_simple_group to data.
        """
        if len(self._simple_group) > 0:
            self._add_to_group(group, {str(self._simple_group_id): self._simple_group})
            self._simple_group_id += 1

    def parse_line(self, active_group: str, striped_line: str, curr_line: int):
        if striped_line == "":
            # group ended (or first group)
            self._write_simple_group_as_list(active_group)
            self._simple_group = []
            if self._key_used(active_group, str(self._simple_group_id)):
                self._simple_group_id += 1
        else:
            if striped_line.startswith("way "):
                self._simple_group.append(striped_line[4:].strip())
            else:
                self._simple_group.append(striped_line)

    def _write_simple_group_as_list(self, group_name):
        if (len(self._simple_group)) == 0:
            return

        self._add_to_group(group_name, {str(self._simple_group_id): self._simple_group})

    def _key_used(self, group_key, key: str):
        return key in self.data[group_key]


class OsmConfig(dict):
    def __init__(self, cfg_path, **kwargs):
        super().__init__(**kwargs)
        self.cfg_path = cfg_path
        # self.configGroups = dict()
        self.parser_map = {
            "convex-hull:way-list": OsmConfigTextGroupParser(self),
            "wall:way-list": OsmConfigTextGroupParser(self),
            "default": OsmCfgParser(self),
        }
        self._current_line_num = 0
        self._previous_group = ""
        self._active_group = ""

        self._parse_config()

    def _parse_config(self):

        with open(self.cfg_path, "r") as cfg_f:
            for line in cfg_f:
                self._current_line_num += 1
                if line.strip().startswith("#"):
                    line = ""

                # check for new group. If yes handle post and pre calls to parser
                # if not do nothing. As long as we dont have any group (start of parsing)
                # continue reading lines until we have a group.
                new_group = self._new_group(line)
                if self._active_group == "":
                    continue

                # handle pre and post parse_line calls.
                if new_group:
                    if self._previous_group != "":
                        old_parser = self._select_parser(self._previous_group)
                        old_parser.post_group(
                            self._previous_group, self._current_line_num
                        )
                    new_parser = self._select_parser(self._active_group)
                    new_parser.pre_group(self._active_group, self._current_line_num)
                    continue

                # select parser based on active group.
                parser = self._select_parser(self._active_group)

                parser.parse_line(
                    self._active_group, line.strip(), self._current_line_num
                )

        # EOF reached. Call post parse_line on last active parser
        last_parser = self._select_parser(self._active_group)
        last_parser.post_group(self._active_group, self._current_line_num)

    def _select_parser(self, configGroup: str):
        if configGroup in self.parser_map:
            parser = self.parser_map[configGroup]
        else:
            parser = self.parser_map["default"]

        return parser

    def _new_group(self, val: str):
        g = val.strip()
        if g.startswith("[") and g.endswith("]") and len(g) > 2:
            self._previous_group = self._active_group
            self._active_group = g[1:-1]
            self[self._active_group] = {}
            return True
        else:
            return False

    def print_config(self):
        for k, v in self.items():
            print(k)
            for kk, vv in v.items():
                print(f"\t{kk}: {vv}")

    def get_way_list(self, cmd):
        if f"{cmd}:way-list" not in self:
            return []
        else:
            return list(self[f"{cmd}:way-list"].values())

    def get_convex_hull_list(self):
        if "convex-hull:way-list" not in self:
            return []
        else:
            return list(self["convex-hull:way-list"].values())

    def get_wall_list(self):
        if "wall:way-list" not in self:
            return []
        else:
            return list(self["convex-hull:way-list"].values())


def convert_walls(osm: OsmData):
    way_elements = osm.ways_find_with_tags(
        inc_tag=[("barrier", "wall")],  # select line based ways.
        exc_tag=[("rover:obstacle:line:hull",), ("rover:obstacle.ignore", "yes")],
        truth_operator=("and", "or"),
    )  # ignore already transformed ones.

    way_elements.extend(
        osm.ways_find_with_tags(
            inc_tag=[("rover:obstacle:type", "line")],
            exc_tag=[("rover:obstacle:line:hull",), ("rover:obstacle.ignore", "yes")],
            truth_operator=("and", "or"),
        )
    )

    print(f"found {len(way_elements)} lines. Start converting ...")
    for line_element in way_elements:
        line_id = line_element.get("id")
        if line_id is not None:
            utm_points = osm.nodes_to_utm(osm.way_node_refs(line_id))
            new_way, _, _ = osm.way_create_from_polyline(utm_points)

            # add tags to new polygon
            new_way_id = new_way.id
            osm.way_add_tag(new_way_id, "rover:obstacle", "wall")
            osm.way_add_tag(new_way_id, "rover:obstacle:line:ref", line_id)
            osm.way_add_tag(new_way_id, "rover:obstacle:line:hull", "-1")

            # add tags to existing line
            osm.way_add_tag(line_id, "rover:obstacle:line:hull", new_way_id)
            osm.way_add_tag(line_id, "rover:obstacle.ignore", "yes")
            print(
                f"Created new way (id: {new_way_id}) ignore id: {line_id} from now on."
            )


class OsmArg(dict):
    def __init__(self, data):
        super().__init__(data)

    def __setattr__(self, name: str, value: Any) -> None:
        try:
            self[name.replace("_", "-")] = value
        except KeyError:
            raise AttributeError(f"class has no Attribute {name.replace('_','-')}")

    def __getattribute__(self, name: str) -> Any:
        if name.replace("_", "-") not in self:
            raise AttributeError(f"class has no Attribute {name.replace('_','-')}")
        return self[name.replace("_", "-")]
