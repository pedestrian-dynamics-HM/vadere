# -*- coding: utf-8 -*-
# Eclipse SUMO, Simulation of Urban MObility; see https://eclipse.org/sumo
# Copyright (C) 2011-2019 German Aerospace Center (DLR) and others.
# This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v2.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v20.html
# SPDX-License-Identifier: EPL-2.0

# @file    PythonClient.py
# @author  Philipp Schuegraf
# @date    2019-11-13
# @version $Id$

import sys, os
sys.path.append("/Users/Philipp/Repos/vadere/Tools/PyTraci")
import argparse
from IPython import embed

import pytraci as traci

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

    # ### Implemented features
    #traci.simulation_vadere.addTargetChanger("1", ["8.4", "6.4", "9.5", "6.4", "9.5", "6.9", "6.4", "6.4"], 1., 0, "3", 1.)
    traci.simulation_vadere.addWaitingArea("1", ["8.4", "6.4", "9.5", "6.4", "9.5", "6.9", "8.4", "6.9"])
    traci.simulationStep(1)
    # traci.person_vadere.setTargetList("2", ["2", "3"])
    # traci.simulationStep(1)
    # pos = (2., 3.)
    # traci.person_vadere.setPosition("1", *pos)
    # actual_pos = traci.person_vadere.getPosition("1")
    # assert(pos == actual_pos)
    #
    #
    # print(traci.person_vadere.getNextFreeID())
    #
    # print(traci.person_vadere.getPositionList())
    #
    # print(traci.person_vadere.getTargetList("-1"))
    # print(traci.person_vadere.getTargetList("1"))
    #
    # traci.person_vadere.add("5", (2.0, 3.0), "2")
    # traci.simulationStep(1)

    # interactive
    embed()