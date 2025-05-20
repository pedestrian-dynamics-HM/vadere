def extract_attributes(topography, attributes_model, parameter_string):
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
    print(x_min, x_max)
    print(y_min, y_max)

    grid_size = float(attributes_model['gridSize'])
    area_threshold = float(attributes_model['areaThreshold'])
    inlet_velocity = float(attributes_model['inletVelocity'])
    blocking_obstacles = attributes_model['blockingObstacles']

    parameter_string = parameter_string + str(attributes_model['gridSize']) + "-" + str(attributes_model['areaThreshold']) + "-" + str(attributes_model['inletVelocity']) + "-"


    inlets = []
    outlets = []
    for i, ins in enumerate(attributes_model['inlets']):
        parameter_string = parameter_string + ins['side'] + "[" + str(ins['start']) +","+ str(ins['start']+ins['width']) + "]"
        inlets.append({"id": i, "side": ins['side'], "coords": [float(ins['start']), float(ins['start']+ins['width'])]})
    parameter_string = parameter_string + "-"
    for i, outs in enumerate(attributes_model['outlets']):
        parameter_string = parameter_string + outs['side'] + "[" + str(outs['start'])+","+  str(outs['start']+outs['width']) + "]"
        outlets.append({"id": i, "side": outs['side'], "coords": [float(outs['start']), float(outs['start']+outs['width'])]})

    parameter_string = parameter_string + "-" + str(blocking_obstacles) + f"-xmin[{x_min}]-xmax[{x_max}]-ymin[{y_min}]-ymax[{y_max}]"

    obstacles = []
    for obstacle in topography['obstacles']:
        if obstacle['id'] in blocking_obstacles:
            if obstacle['shape']['type'] == 'RECTANGLE':
                ob_x_min = obstacle['shape']['x']
                ob_y_min = obstacle['shape']['y']
                ob_x_max = ob_x_min + obstacle['shape']['width']
                ob_y_max = ob_y_min + obstacle['shape']['height']
                obstacles.append([(ob_x_min, ob_y_min), (ob_x_min, ob_y_max), (ob_x_max, ob_y_max), (ob_x_max, ob_y_min)])

            if obstacle['shape']['type'] == 'POLYGON':
                obstacles.append([(point['x'], point['y']) for point in obstacle['shape']['points']])

    return grid_size, area_threshold, x_min, x_max, y_min, y_max, inlet_velocity, inlets, outlets, obstacles, parameter_string



