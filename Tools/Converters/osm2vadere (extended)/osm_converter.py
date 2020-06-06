# Convert an OpenStreetMap XML file exported from https://www.openstreetmap.org/
# to a Vadere topology (in Cartesian coordinates).
#
# Steps to run this script:
#
# 1. Go to https://www.openstreetmap.org/.
# 2. Click "Export" and adjust region of interest and zoom level.
# 3. Call script and pass exported file from (2):
#
#      python3 <script> <exported_file>
#
# 4. Insert output into "topography" tab of Vadere.
#
# Note: currently, the scripts converts only buildings to Vadere obstacles.
#
# Watch out: before running this script, install its dependencies using pip:
#
#   pip install -r requirements.txt
#
# An OpenStreetMap XML file has following structure:
#
# 1. The boundary box of the exported tile.
# 2. List of all nodes in the tile containing latitude and longitude.
# 3. Paths are formed using references to (2).
#
# Example OSM file:
#
#   <?xml version="1.0" encoding="UTF-8"?>
#   <osm version="0.6" ...>
#    <bounds minlat="47.8480100" minlon="11.8207100" maxlat="47.8495600" maxlon="11.8249200"/>
#    <node id="31413334" ... lat="47.8563764" lon="11.8186396"/>
#    ...
#    <way id="192686406" ...>
#     <nd ref="31413334"/>
#     <nd ref="3578052113"/>
#     <nd ref="394769402"/>
#     <nd ref="3828186058"/>
#     <tag k="highway" v="unclassified"/>
#    </way>
#   ...
#
# A Vadere obstacle looks like this:
#
#   {
#   "shape" : {
#       "type" : "POLYGON",
#       "points" : [ { "x" : 43.7, "y" : 3.4 }, ... ]
#     },
#    "id" : -1
#   }


import math
import os
from string import Template
from typing import List

from git import InvalidGitRepositoryError, Repo
from lxml.etree import Element

import osm_helper
from osm_helper import OsmData, PolyObjectWidthId

vadere_simple_topography_element_string = """{
    "id" : $id,
    "shape" : {
        "type" : "POLYGON",
        "points" : [ 
$points
        ]
    }
}"""

source_defaults = {
    "interSpawnTimeDistribution": "org.vadere.state.scenario.ConstantDistribution",
    "distributionParameters": "[1.0]",
    "spawnNumber": "1",
    "maxSpawnNumberTotal": "-1",
    "startTime": "0.0",
    "endTime": "0.0",
    "spawnAtRandomPositions": "false",
    "useFreeSpaceOnly": "true",
    "targetIds": "[ ]",
    "groupSizeDistribution": "[1.0]",
    "dynamicElementType": "PEDESTRIAN",
}

target_defaults = {
    "absorbing": "true",
    "waitingTime": "0.0",
    "waitingTimeYellowPhase": "0.0",
    "parallelWaiters": "0",
    "individualWaiting": "true",
    "deletionDistance": "0.1",
    "startingWithRedLight": "false",
    "nextSpeed": "-1.0",
}


class OsmConverter:
    """
    Create Vadere Topography elements based on open street map xml input files.
    """

    def __init__(self, osm_file, use_osm_id, wall_thickness=0.25):
        self.osm_file = osm_file
        self.osm = OsmData(self.osm_file)
        self.use_osm_id = use_osm_id
        self.wall_thickness = wall_thickness
        self.aoi = None

        # self.xml_tree = etree.parse(self.osm_file)
        # self.node_dict = OsmConverter.extract_latitude_and_longitude_for_each_xml_node(self.xml_tree)
        # self.simple_buildings = OsmConverter.filter_for_buildings(self.xml_tree)
        # self.complex_buildings = OsmConverter.filter_for_buildings_in_relations(self.xml_tree)

        self.base_point_lon_lat = self.osm.base_point
        self.base_point_utm = [0.0, 0.0]
        self.zone_map = {}
        self.obstacles: List[Element] = []
        self.sources: List[Element] = []
        self.targets: List[Element] = []
        self.measurement: List[Element] = []

    def filter(self):
        for f in self.osm.obstacle_selectors:
            self.obstacles.extend(f())

        for f in self.osm.target_selectors:
            self.targets.extend(f())

        for f in self.osm.source_selectors:
            self.sources.extend(f())

        for f in self.osm.measurement_selectors:
            self.measurement.extend(f())

    def filter_area_of_interest(self):
        aoi = self.osm.get_area_of_intrest()

        if aoi:
            self.aoi = aoi
            self.obstacles = [
                o
                for o in self.obstacles
                if self.osm.contained_in_area_of_intrest(aoi, o)
            ]
            self.targets = [
                o for o in self.targets if self.osm.contained_in_area_of_intrest(aoi, o)
            ]
            self.sources = [
                o for o in self.sources if self.osm.contained_in_area_of_intrest(aoi, o)
            ]
            self.measurement = [
                o
                for o in self.measurement
                if self.osm.contained_in_area_of_intrest(aoi, o)
            ]

    def get_base_point_from_aoi(self):
        assert self.aoi is not None
        lonlat = (min(self.aoi[0]), min(self.aoi[1]))
        return self.osm.lookup.convert_latlon_to_utm(lonlat)

    def find_width_height_from_aoi(self, base):
        assert self.aoi is not None

        max_point = self.osm.lookup.convert_latlon_to_utm(
            (max(self.aoi[0]), max(self.aoi[1]))
        )

        shift_in_x = -base[0]
        shift_in_y = -base[1]
        max_point_shifted = (max_point[0] + shift_in_x, max_point[1] + shift_in_y)
        return max_point_shifted  # width and height

    @classmethod
    def from_args(cls, arg):
        c = cls(arg.input, arg.use_osm_id)
        c.filter()
        if arg.use_aoi:
            c.filter_area_of_interest()
        return c

    @staticmethod
    def get_git_hash():
        """
        :return: name of file with commit hash of current version. If the file contains uncommitted changes a
        'dirty' commit hash is returned.
        """
        try:
            repo_base = "../../.."
            repo = Repo(repo_base)
            current_file = os.path.relpath(__file__, repo_base)
            osm_helper_file = os.path.relpath(osm_helper.__file__, repo_base)
            file_name = os.path.basename(current_file)
            if current_file in repo.untracked_files:
                print(
                    f"{__file__} is not tracked by git. This is not good! You will not be able to reproduce the output"
                    f" later on."
                )
                return "not-tracked"

            if repo.is_dirty(path=current_file) or repo.is_dirty(path=osm_helper_file):
                print(
                    f"warning: Converted output is based on a not committed script. Reproducing the result might not"
                    f" work. Commit changes first and rerun."
                )
                return f"{file_name}-{repo.commit().hexsha}-dirty"
            else:
                return f"{file_name}-{repo.commit().hexsha}"

        except InvalidGitRepositoryError:
            print(f"cannot find git repository at {os.path.abspath('../../..')}")
            return "no-repo-found"

    # @staticmethod
    # def xpath_k_v_tags(key_value_list: List):
    #     xpath = [OsmConverter.xpath_k_v_tag(*i) for i in key_value_list]
    #     return f"({' and '.join(xpath)})"
    #
    # @staticmethod
    # def xpath_k_v_tag(k: str, v: str):
    #     return f"./tag[@k='{k}' and @v='{v}']"
    #
    # @staticmethod
    # def add_ignore(ignore: List):
    #     if ignore is None:
    #         return ""
    #     else:
    #         return f" and not({OsmConverter.xpath_k_v_tags(ignore)})"
    #
    # @staticmethod
    # def filter_tag(xml_tree, include: list, exclude: list = None) -> List[Element]:
    #     """
    #     creates xpath string which will return all elements contains tags with a specific key (e.g ('key2') without
    #     checking the value or in the case of ('key1', 'val2') the value will be checked. All elements in the include
    #     or
    #     exclude list are concatenated with an 'and' operator
    #     :param xml_tree:
    #     :param include: list of the form [('key1', 'val1'), ('key1', 'val2'), ('key2'), ...]
    #     :param exclude: list of the form [('key1', 'val1'), ('key1', 'val2'), ('key2'), ...]
    #     :return:
    #     """
    #     include_tag_key = [element[0] for element in include if len(element) == 1]
    #     inc_1 = [f"./tag[@k='{k}']" for k in include_tag_key]
    #     include_tag_key_value_pair = [element for element in include if len(element) == 2]
    #     inc_2 = [f"./tag[@k='{e[0]}' and @v='{e[1]}']" for e in include_tag_key_value_pair]
    #
    #     if exclude is not None:
    #         exclude_tag_key = [element for element in exclude if len(element) == 1]
    #         exc_1 = [f"./tag[@k='{k}']" for k in exclude_tag_key]
    #         exclude_tag_key_value_pair = [element for element in exclude if len(element) == 2]
    #         exc_2 = [f"./tag[@k='{e[0]}' and @v='{e[1]}']" for e in exclude_tag_key_value_pair]
    #         exclude_xpath = f"and not ({' and '.join(exc_1 +  exc_2)})"
    #     else:
    #         exclude_xpath = ""
    #
    #     xpath = f"/osm/way[({' and '.join(inc_1 + inc_2)}) {exclude_xpath} ]"
    #     print(xpath)
    #     return xml_tree.xpath(xpath)
    #
    # @staticmethod
    # def filter_for_buildings(xml_tree, ignore: list = None) -> List[Element]:
    #     xpath = f"/osm/way[./tag/@k='building' {OsmConverter.add_ignore(ignore)}]"
    #     return xml_tree.xpath(xpath)
    #
    # @staticmethod
    # def filter_for_barrier(xml_tree, ignore: list = None) -> List[Element]:
    #     xpath = f"/osm/way[./tag/@k='barrier' {OsmConverter.add_ignore(ignore)}]"
    #     return xml_tree.xpath(xpath)
    #
    # @staticmethod
    # def filter_for_buildings_in_relations(xml_tree, ignore: list = None):
    #     # Note: A relation describes a shape with "cutting holes".
    #
    #     # Select "relation" nodes with a child node "tag" annotated with attribute "k='building'".
    #     xpath = f"/osm/relation[./tag/@k='building' {OsmConverter.add_ignore(ignore)}]"
    #     buildings = xml_tree.xpath(xpath)
    #
    #     # We only want the shapes and only the outer parts. role='inner' is for "cutting holes" in the shape.
    #     members_in_the_relations = [building.xpath("./member[./@type='way' and ./@role='outer']") for building in
    #                                 buildings]
    #     way_ids = []
    #     for element in members_in_the_relations:
    #         for way in element:
    #             way_ids.append(way.get("ref"))
    #     ways = xml_tree.xpath("/osm/way")
    #     ways_as_dict_with_id_key = {way.get("id"): way for way in ways}
    #     buildings_from_relations = [ways_as_dict_with_id_key[way_id] for way_id in way_ids]
    #     return buildings_from_relations

    @staticmethod
    def find_new_base_point(buildings: List[PolyObjectWidthId]):
        """
        The base point will be the smallest coordinate taken for all buildings of the current map boundary.
        This point will most likely not correspond with the Base Point which is the lower left corner of the
        map bound chosen at export time of the open street map xml file.
        :param buildings:
        :return: smallest coordinate (in utm)
        """
        # "buildings_cartesian" is a list of lists. The inner list contains the (x,y) tuples.
        # search for the lowest x- and y-coordinates within the points
        all_points = [point for building in buildings for point in building.points]

        tuple_with_min_x = min(all_points, key=lambda point: point[0])
        tuple_with_min_y = min(all_points, key=lambda point: point[1])

        return tuple_with_min_x[0], tuple_with_min_y[1]

    @staticmethod
    def find_width_and_height(buildings: List[PolyObjectWidthId]):
        """
        :param buildings:
        :return: utm coordinates used to bound all buildings
        """
        width = 0
        height = 0
        for cartesian_points in buildings:
            for point in cartesian_points.points:
                width = max(width, point[0])
                height = max(height, point[1])
        return math.ceil(width), math.ceil(height)

    @staticmethod
    def to_vadere_topography(
        width,
        height,
        translation,
        zone_string,
        obstacles=None,
        sources=None,
        targets=None,
        measurement_areas=None,
    ):
        """

        :param measurement_areas:
        :param targets:
        :param sources:
        :param obstacles: list of Vadere obstacles (json string)
        :param width: of the topography bound
        :param height: of the topography bound
        :param translation: offset used to translate the topography to (0,0). This is needed to reverse the translation
               if needed
        :param zone_string: epgs or UTM string encoding the coordinates system
        :return:
        """
        with open("templates/vadere_topography_template.txt", "r") as f:
            vadere_topography_input = f.read()  # .replace('\n', '')

        epsg_description = f"OpenStreetMap export {OsmConverter.get_git_hash()}"
        vadere_topography_output = Template(vadere_topography_input).substitute(
            width=width,
            height=height,
            obstacles=obstacles,
            translate_x=translation[0],
            translate_y=translation[1],
            epsg=zone_string,
            epsg_description=epsg_description,
            sources=sources,
            targets=targets,
            measurement_areas=measurement_areas,
        )
        return vadere_topography_output

    @staticmethod
    def apply_template(
        poly_object: PolyObjectWidthId,
        template_string,
        default_template_data=None,
        indent_level=3,
    ):
        """
        :param indent_level:
        :param default_template_data:
        :param template_string:
        :param poly_object:
        :return: Vadere json representation of an obstacle
        """
        vadere_point_string = f"{'    ' * indent_level}" + '  { "x" : $x, "y" : $y }'

        obstacle_string_template = Template(template_string)
        point_string_template = Template(vadere_point_string)

        points_as_string = [
            point_string_template.substitute(x=x, y=y) for x, y in poly_object.points
        ]
        points_as_string_concatenated = ",\n".join(points_as_string)

        template_data = {}
        if default_template_data is not None:
            template_data.update(default_template_data)
        template_data.update(poly_object.template_data)
        template_data.setdefault("points", points_as_string_concatenated)

        vadere_obstacle_as_string = obstacle_string_template.substitute(template_data)

        return vadere_obstacle_as_string

    @staticmethod
    def to_vadere_obstacles(buildings: List[PolyObjectWidthId]):
        list_of_vadere_obstacles_as_strings = []
        for building in buildings:
            vadere_obstacles_as_strings = OsmConverter.apply_template(
                building, template_string=vadere_simple_topography_element_string
            )
            list_of_vadere_obstacles_as_strings.append(vadere_obstacles_as_strings)
        return list_of_vadere_obstacles_as_strings

    @staticmethod
    def to_vadere_measurement_area(buildings: List[PolyObjectWidthId]):
        list_of_vadere_measurement_area_as_strings = []
        for building in buildings:
            vadere_measurement_area_as_strings = OsmConverter.apply_template(
                building, template_string=vadere_simple_topography_element_string
            )
            list_of_vadere_measurement_area_as_strings.append(
                vadere_measurement_area_as_strings
            )
        return list_of_vadere_measurement_area_as_strings

    @staticmethod
    def to_vadere_sources(sources: List[PolyObjectWidthId]):
        list_of_vadere_sources_as_strings = []
        with open("templates/vadere_source_template.txt", "r") as f:
            vadere_source_template = f.read()
        for source in sources:
            source_string = OsmConverter.apply_template(
                source,
                template_string=vadere_source_template,
                default_template_data=source_defaults,
            )
            list_of_vadere_sources_as_strings.append(source_string)
        return list_of_vadere_sources_as_strings

    @staticmethod
    def to_vadere_targets(targets: List[PolyObjectWidthId]):
        list_of_vadere_target_as_strings = []
        with open("templates/vadere_target_template.txt", "r") as f:
            vadere_target_template = f.read()
        for target in targets:
            target_string = OsmConverter.apply_template(
                target,
                template_string=vadere_target_template,
                default_template_data=target_defaults,
            )
            list_of_vadere_target_as_strings.append(target_string)
        return list_of_vadere_target_as_strings

    @staticmethod
    def print_output(output_file, output):
        if output_file is None:
            print(output)
        else:
            with open(output_file, "w") as text_file:
                print(output, file=text_file)

    def convert_way_to_utm(self, way: Element):
        way_id = way.get("id")
        node_ids = self.osm.way_node_refs(way_id)
        converted_way_points = self.osm.nodes_to_utm(node_ids)

        # if way is closed remove last element (it's the same as the first)
        if self.osm.way_is_closed(way_id):
            converted_way_points = converted_way_points[:-1]

        return converted_way_points

    def print_xml_parsing_statistics(self):
        print(f"File: {self.osm_file}")
        print(f"  Nodes: {len(self.osm.nodes)}")
        print(f"  Polygons: {len(self.obstacles)}")
        print(
            f"  Base point: {self.base_point_lon_lat} (not to be confused with utm based point which!"
        )

    def convert_to_utm_poly_object(
        self,
        data,
        tag_name_space=None,
        shift_nodes=True,
        base_point=None,
        remove_duplicates=True,
    ):
        polygons_in_utm = []
        # ways which already are 'polygons'
        for way in data:
            # Collect nodes that belong to the current building.
            utm_points = self.convert_way_to_utm(way)

            if self.use_osm_id:
                element = PolyObjectWidthId(way.get("id"), utm_points)
                element.template_data.update(OsmData.tags_to_dict(way, tag_name_space))
                polygons_in_utm.append(element)
            else:
                element = PolyObjectWidthId(-1, utm_points)
                element.template_data.update(OsmData.tags_to_dict(way, tag_name_space))
                polygons_in_utm.append(element)

        utm_topography_elements = polygons_in_utm
        if base_point is None:
            self.base_point_utm = OsmConverter.find_new_base_point(polygons_in_utm)
        else:
            self.base_point_utm = list(base_point)

        if shift_nodes:
            for b in utm_topography_elements:
                b.shift_points(self.base_point_utm)

        if remove_duplicates:
            ret = []
            id_set = set()
            for element in utm_topography_elements:
                if element.id not in id_set:
                    ret.append(element)
                    id_set.add(element.id)
            utm_topography_elements = ret

        return utm_topography_elements, self.base_point_utm, self.osm.utm_zone_string
