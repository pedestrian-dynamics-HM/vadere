
def set_spawn_number(scenario, value):
    scenario["scenario"]["topography"]["sources"][0]["spawnNumber"] = int(value)
    return scenario

def set_name(scenario, scenario_name):
    scenario["name"] = "evacuation"
    return scenario

def get_timestep(scenario):
    float(scenario["scenario"]["attributesSimulation"]["simTimeStepLength"])