import sys, os
import argparse
from IPython import embed

import org.vadere.manager.client.pyTraci as traci

def readScenario(scenPath):
    with open(scenPath, 'r') as scenFile:
        scenario = scenFile.read()
    return scenario


if __name__ == '__main__':

    # args
    parser = argparse.ArgumentParser(description="Python client to vadere via traci")
    parser.add_argument('-scenPath', type=str, help='Path to the scenario')
    parser.add_argument('-scenFName', type=str, help='Filename of the scenario. E.g. scenario002.scenario')
    args = parser.parse_args()

    # go
    scenPath = os.path.join(args.scenPath, args.scenFName)
    scenName = "Test"
    scenario = readScenario(scenPath)

    traci.init(port=9999)
    traci.sendFile(["Test", scenario])

    ### Implemented features
    pos = (2., 3.)
    traci.person.setPosition("1", *pos)
    actual_pos = traci.person.getPosition("1")
    assert(pos == actual_pos)


    #print(traci.person.getNextFreeID())

    #print(traci.person.getPositionList())

    # print(traci.person.getTargetList("-1"))
    # print(traci.person.getTargetList("1"))

    # traci.person.add("5", (2.0, 3.0), "2")
    # traci.simulationStep(1)

    # interactive
    embed()