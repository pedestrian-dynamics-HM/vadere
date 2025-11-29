import json

def extract_attributes(scenario_file_path):
    """
    Parses the JSON scenario file and returns geometry parameters.
    """
    with open(scenario_file_path) as file:
        data = json.load(file)

    topography = data['scenario']['topography']
    attributes_model = data['scenario']['attributesModel']['org.vadere.state.attributes.models.airflow.AttributesAirFlowModel']

    # 1. Bounds Calculation
    topo_xmin = topography['attributes']['bounds']['x'] + topography['attributes']['boundingBoxWidth']
    topo_ymin = topography['attributes']['bounds']['y'] + topography['attributes']['boundingBoxWidth']
    topo_xmax = topography['attributes']['bounds']['x'] + topography['attributes']['bounds']['width']
    topo_ymax = topography['attributes']['bounds']['y'] + topography['attributes']['bounds']['height']

    airflow_xmin = attributes_model['bounds']['xmin']
    airflow_xmax = attributes_model['bounds']['xmax']
    airflow_ymin = attributes_model['bounds']['ymin']
    airflow_ymax = attributes_model['bounds']['ymax']

    x_min = max(topo_xmin, airflow_xmin)
    x_max = min(topo_xmax, airflow_xmax)
    y_min = max(topo_ymin, airflow_ymin)
    y_max = min(topo_ymax, airflow_ymax)

    # 2. Parameters
    grid_size = float(attributes_model['gridSize'])
    area_threshold = float(attributes_model['areaThreshold'])
    inlet_velocity = float(attributes_model['inletVelocity'])
    blocking_obstacles = attributes_model['blockingObstacles']

    # 3. Inlets / Outlets
    inlets = []
    for i, ins in enumerate(attributes_model['inlets']):
        inlets.append({
            "id": i,
            "side": ins['side'],
            "coords": [float(ins['start']), float(ins['start'] + ins['width'])]
        })

    outlets = []
    for i, outs in enumerate(attributes_model['outlets']):
        outlets.append({
            "id": i,
            "side": outs['side'],
            "coords": [float(outs['start']), float(outs['start'] + outs['width'])]
        })

    # 4. Obstacles
    obstacles = []
    for obstacle in topography['obstacles']:
        if obstacle['id'] in blocking_obstacles:
            if obstacle['shape']['type'] == 'RECTANGLE':
                ob_x_min = obstacle['shape']['x']
                ob_y_min = obstacle['shape']['y']
                ob_x_max = ob_x_min + obstacle['shape']['width']
                ob_y_max = ob_y_min + obstacle['shape']['height']
                # Store as list of points
                obstacles.append([(ob_x_min, ob_y_min), (ob_x_min, ob_y_max),
                                  (ob_x_max, ob_y_max), (ob_x_max, ob_y_min)])

            elif obstacle['shape']['type'] == 'POLYGON':
                obstacles.append([(point['x'], point['y']) for point in obstacle['shape']['points']])

    return {
        'grid_size': grid_size,
        'area_threshold': area_threshold,
        'x_min': x_min, 'x_max': x_max,
        'y_min': y_min, 'y_max': y_max,
        'inlet_velocity': inlet_velocity,
        'inlets': inlets,
        'outlets': outlets,
        'obstacles': obstacles
    }


def get_parameter_string(geom_data):
    parameter_string = f"{geom_data['grid_size']}-{geom_data['area_threshold']}-{geom_data['inlet_velocity']}-"

    for item in geom_data['inlets']:
        parameter_string += f"{item['side']}[{item['coords'][0]},{item['coords'][1]}]"
    parameter_string += "-"

    for item in geom_data['outlets']:
        parameter_string += f"{item['side']}[{item['coords'][0]},{item['coords'][1]}]"

    parameter_string += f"-xmin[{geom_data['x_min']}]-xmax[{geom_data['x_max']}]-ymin[{geom_data['y_min']}]-ymax[{geom_data['y_max']}]"

    return parameter_string