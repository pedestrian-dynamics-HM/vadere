# -----------------------------------------------------------
# CovidSim - TransmissionModel validation
#
# This script generates a parametrized scenario file from a template (hamner-2020-life-template.scenario). The scenario
# models a Covid-19 superspreading event recorded in hamner-2020-life. It is used for the validation of Vadere's
# TransmissionModel.
#
# Author: Simon Rahn, simon.rahn@hm.edu
# Date: 2021-08-27
# -----------------------------------------------------------

import numpy as np
import random as rd
import json


def generate_scenario_elements(nRows, nColumns, xStart, yStart, xSpace, ySpace, elementIdStart, elementWidth,
                               elementHeight, elementDict, nMaxElements, waitingTime=0):
    # start at upper left corner, complete a row, continue with the next row below the previous one

    global TARGET_LISTS

    scenarioElementDictList = list()
    elementId = elementIdStart
    elementPosY = yStart + ySpace  # in the first loop ySpace is subtracted again
    idxElements = 0
    idxTargetId = 0

    for rows in range(nRows):
        elementPosX = xStart  # reset x position for each new row
        elementPosY = elementPosY - ySpace

        for column in range(nColumns):
            shapeDict = {"x": elementPosX,
                         "y": elementPosY,
                         "width": elementWidth,
                         "height": elementHeight,
                         "type": "RECTANGLE"}

            elementDict['id'] = elementId
            elementDict['shape'] = shapeDict

            if 'waitingTime' in elementDict:
                elementDict['waitingTime'] = waitingTime

            if 'targetIds' in elementDict:
                targets = []
                for i in range(len(TARGET_LISTS)):
                    targets.append(TARGET_LISTS[i][idxTargetId])

                idxTargetId = idxTargetId + 1

                elementDict['targetIds'] = targets

            scenarioElementDictList.append(elementDict.copy())

            idxElements = idxElements + 1
            if idxElements == nMaxElements:
                return scenarioElementDictList

            elementPosX = elementPosX + xSpace
            elementId = elementId + 1

    return scenarioElementDictList


if __name__ == '__main__':
    # coordinates:
    # X -> horizontal / width / rows
    # Y -> vertical / height / depth / columns
    #
    # units: all values in SI units (m, s)
    #
    # assumptions are commented with [assumption]
    # information taken from hamner-2020-life are commented with [hamner-2020-life]

    # total number of agents
    nAgents = 61  # [hamner-2020-life]

    # generate random number to assure reproducibility
    seed = 1
    rd.seed(seed)

    # topography
    boundingBoxWidth = 0.5

    # sources area
    nSources = nAgents
    nSourcesColumns = 5
    nSourcesRows = int(np.ceil(nSources / nSourcesColumns))
    sourceWidth = 0.41
    sourceHeight = 0.41
    sourceSpacingX = 0.4
    sourceSpacingY = 0.4
    sourcesAreaBoundingBoxWidth = 1
    sourceIdCounter = 1         # start source ids with sourceIdCounter

    sourcesAreaWidth = sourcesAreaBoundingBoxWidth * 2 + nSourcesColumns * sourceWidth + (
            nSourcesColumns - 1) * sourceSpacingX
    sourcesAreaHeight = sourcesAreaBoundingBoxWidth * 2 + nSourcesRows * sourceHeight + (
            nSourcesRows - 1) * sourceSpacingY

    # wall thickness
    wallThickness = 0.5

    # targets
    targetIdCounter = 1001      # start target ids with targetIdCounter

    # chairs
    seatWidthX = 0.4            # [assumption]
    seatDepthY = 0.4            # [assumption]

    targetWidth = 0.4
    targetHeight = 0.4
    relativeTargetPositionX = seatWidthX / 2 - targetWidth / 2
    relativeTargetPositionY = - targetHeight

    # stage
    nChairRows = 6              # [hamner-2020-life]
    chairRowSpacing = 0.6       # [assumption] vertical spacing between chairs

    nChairColumns = 10
    chairColumnSpacing = 0.2    # horizontal spacing between chairs; 6 to 10 inches (~0.15 to 0.25 m)

    stagesSpacingX = 1.5        # [assumption] horizontal spacing between left and right stage / width of aisle

    stageWallSpacingX = 3       # [assumption] horizontal spacing between stages and walls
    stageWallSpacingY = 6       # [assumption] vertical spacing between stages and walls

    stagesWidth = nChairColumns * seatWidthX + (nChairColumns - 1) * chairColumnSpacing
    stagesHeight = nChairRows * seatDepthY + (nChairRows - 1) * chairRowSpacing

    # first chair within a stage is in the upper left corner, chair id increases column-wise then row-wise
    # X, Y position defines the upper left corner of the chair -> in Vadere, the position would be X, (Y - seatDepth)
    stageLeftX = boundingBoxWidth + sourcesAreaWidth + wallThickness + stageWallSpacingX
    stageLeftY = boundingBoxWidth + wallThickness + stagesHeight + stageWallSpacingY
    stageLeftFirstId = targetIdCounter

    stageRightX = stageLeftX + stagesWidth + stagesSpacingX
    stageRightY = stageLeftY
    stageRightFirstId = stageLeftFirstId + nChairRows * nChairColumns

    # benches
    nBenchesOccupants = 30      # [assumption] approx. half of choir practice attendees
    nBenchesColumns = 10        # [assumption]
    benchSeatColumnSpacing = chairColumnSpacing

    nBenchesRows = int(np.ceil(nBenchesOccupants / nBenchesColumns))
    benchSeatRowSpacing = chairRowSpacing

    benchesWallSpacingX = 3     # [assumption]
    benchesWallSpacingY = 4     # [assumption]

    benchesWidth = nBenchesColumns * seatWidthX + (nBenchesColumns - 1) * benchSeatColumnSpacing
    benchesHeight = nBenchesRows * seatDepthY + (nBenchesRows - 1) * benchSeatRowSpacing

    benchesX = stageRightX + stagesWidth + stageWallSpacingX + wallThickness + benchesWallSpacingX
    benchesY = boundingBoxWidth + wallThickness + benchesWallSpacingY + benchesHeight
    benchesFirstId = stageRightFirstId + nChairRows * nChairColumns

    # room dimensions
    largeRoomWidth = 2 * stageWallSpacingX + 2 * stagesWidth + stagesSpacingX
    largeRoomHeight = 2 * stageWallSpacingY + stagesHeight
    largeRoomX = boundingBoxWidth + sourcesAreaWidth + wallThickness    # X position of bottom left corner of large room
    largeRoomY = boundingBoxWidth + wallThickness

    smallRoomWidth = benchesWidth + 2 * benchesWallSpacingX
    smallRoomHeight = benchesHeight + 2 * benchesWallSpacingY

    # practice phases: in large and small rooms
    practiceTime = 5 * 60       # [hamner-2020-life] but simplified to 45 min instead of practice 40 min, 50min, 45 min

    # break targets
    nBreakTargets = 13          # [assumption] -> distribute agents in groups across the room during break
    nBreakTargetColumns = 6
    nBreakTargetRows = int(np.ceil(nBreakTargets / nBreakTargetColumns))
    breakTargetWidth = 1
    breakTargetHeight = 1
    breakTargetSpacingX = (largeRoomWidth - breakTargetWidth * nBreakTargetColumns) / (nBreakTargetColumns + 1)
    breakTargetSpacingY = 1

    # break phase: intermediate targets during break (groups distributed in large room)
    nMaxAgentsPerBreakTarget = 5    # [assumption]
    breakTime = 5 * 60              # [hamner-2020-life] 15 min period of break
    nPassedBreakTargets = 1         # [assumption] increase nTargetsDuringBreak to introduce more movement;
                                    # number of targets that an agent passes during the break
    breakTimePerTarget = breakTime / nPassedBreakTargets
    breakTargetsFirstId = benchesFirstId + nBenchesRows * nBenchesColumns

    # clearing phase: chair racks
    nRackTargets = 1
    rackTargetFirstId = breakTargetsFirstId + nBreakTargets
    clearingTime = 0            # [assumption] period of returning the chairs to the racks

    # final target
    finalTargetId = rackTargetFirstId + nRackTargets

    # topography
    boundsX = 2 * boundingBoxWidth + sourcesAreaWidth + 3 * wallThickness + largeRoomWidth + smallRoomWidth
    boundsY = 2 * boundingBoxWidth + largeRoomHeight + 2 * wallThickness

    # generate scenario elements
    # targetIds for each source are drawn from lists of random numbers; targetIds at index 0 of each list belong to the
    # source which spawns an infectious agent
    nSkippedRows = 2            # [assumption] last two rows of chairs are not occupied
    leftStageIds = range(stageLeftFirstId + nSkippedRows * nChairColumns, stageRightFirstId)
    rightStageIdsPracticeTime1 = range(stageRightFirstId + nSkippedRows * nChairColumns, benchesFirstId)
    # [hamner-2020-life] agents move close together -> no free seats between agents (limited number of ids in
    # rightStageIdsPracticeTime2, benchesIds)
    rightStageIdsPracticeTime2 = range(benchesFirstId - (nAgents - nBenchesOccupants),
                                       benchesFirstId)
    benchesIds = range(benchesFirstId, breakTargetsFirstId)

    breakIds = range(breakTargetsFirstId, 1164)  # [assumption]
    # generate lists
    LIST_1 = list(leftStageIds) + list(rightStageIdsPracticeTime1)
    LIST_2 = list(rightStageIdsPracticeTime2) + list(benchesIds)
    LIST_3 = list(breakIds) * nMaxAgentsPerBreakTarget
    LIST_5 = [1164] * nAgents
    LIST_6 = [1165] * nAgents
    # randomize agents' seating positions
    rd.shuffle(LIST_1)
    rd.shuffle(LIST_2)
    # [assumption] make sure that the infectious agent is placed in stageRight (practice time 1 and 3) and benches
    # (practice time 2), i.e. move a random number from the interval [a, b] (-> randint(a, b)) to index
    # infectiousSourceIdx in LIST_1 and LIST_2
    infectiousSourceIdx = 0
    LIST_1.insert(infectiousSourceIdx,
                  LIST_1.pop(LIST_1.index(rd.randint(rightStageIdsPracticeTime1[0], rightStageIdsPracticeTime1[-1]))))
    LIST_2.insert(infectiousSourceIdx, LIST_2.pop(LIST_2.index(rd.randint(benchesIds[0], benchesIds[-1]))))
    # merge all target lists to common list
    TARGET_LISTS = [LIST_1, LIST_2]
    for idx in range(nPassedBreakTargets):
        rd.shuffle(LIST_3)
        TARGET_LISTS.append(LIST_3.copy())
    TARGET_LISTS.append(LIST_1)     # add LIST_1 once again, because agents return to their original seats during practice phase 3 (after the break)
    TARGET_LISTS.append(LIST_5)
    TARGET_LISTS.append(LIST_6)

    # iterate through sources
    sourceDict = {"id": 0,
                  "shape": "",
                  "interSpawnTimeDistribution": "org.vadere.state.scenario.ConstantDistribution",
                  "distributionParameters": [1.0],
                  "spawnNumber": 1,
                  "maxSpawnNumberTotal": -1,
                  "startTime": 0,
                  "endTime": 0,
                  "spawnAtRandomPositions": False,
                  "spawnAtGridPositionsCA": False,
                  "useFreeSpaceOnly": True,
                  "targetIds": [],
                  "groupSizeDistribution": [1.0],
                  "dynamicElementType": "PEDESTRIAN",
                  "attributesPedestrian": None
                  }

    sourceList = generate_scenario_elements(nSourcesRows,
                                            nSourcesColumns,
                                            boundingBoxWidth + sourcesAreaBoundingBoxWidth,
                                            boundsY - boundingBoxWidth - sourcesAreaBoundingBoxWidth - sourceHeight,
                                            sourceSpacingX + sourceWidth,
                                            sourceSpacingY + sourceHeight,
                                            sourceIdCounter,
                                            sourceWidth,
                                            sourceHeight,
                                            sourceDict,
                                            nSources)

    # iterate through targets
    targetList = list()
    targetDict = {"id": 0,
                  "absorbing": False,
                  "shape": "",
                  "waitingTime": 0,
                  "waitingTimeYellowPhase": 0.0,
                  "parallelWaiters": 0,
                  "individualWaiting": True,
                  "deletionDistance": 0.1,
                  "startingWithRedLight": False,
                  "nextSpeed": -1.0}

    stageLeftTargets = generate_scenario_elements(nChairRows,
                                                  nChairColumns,
                                                  stageLeftX + relativeTargetPositionX,
                                                  stageLeftY + relativeTargetPositionY,
                                                  chairColumnSpacing + seatWidthX,
                                                  chairRowSpacing + seatDepthY,
                                                  stageLeftFirstId,
                                                  targetWidth,
                                                  targetHeight,
                                                  targetDict,
                                                  nChairRows * nChairColumns,
                                                  practiceTime)

    stageRightTargets = generate_scenario_elements(nChairRows,
                                                   nChairColumns,
                                                   stageRightX + relativeTargetPositionX,
                                                   stageRightY + relativeTargetPositionY,
                                                   chairColumnSpacing + seatWidthX,
                                                   chairRowSpacing + seatDepthY,
                                                   stageRightFirstId,
                                                   targetWidth,
                                                   targetHeight,
                                                   targetDict,
                                                   nChairRows * nChairColumns,
                                                   practiceTime)

    benchesTargets = generate_scenario_elements(nBenchesRows,
                                                nBenchesColumns,
                                                benchesX + relativeTargetPositionX,
                                                benchesY + relativeTargetPositionY,
                                                benchSeatColumnSpacing + seatWidthX,
                                                benchSeatRowSpacing + seatDepthY,
                                                benchesFirstId,
                                                targetWidth,
                                                targetHeight,
                                                targetDict,
                                                nBenchesRows * nBenchesColumns,
                                                practiceTime)

    breakTargets = generate_scenario_elements(nBreakTargetRows,
                                              nBreakTargetColumns,
                                              largeRoomX + breakTargetSpacingX,
                                              largeRoomY + largeRoomHeight - 2.5,
                                              breakTargetSpacingX + breakTargetWidth,
                                              breakTargetSpacingY + breakTargetHeight,
                                              breakTargetsFirstId,
                                              breakTargetWidth,
                                              breakTargetHeight,
                                              targetDict,
                                              nBreakTargets,
                                              breakTimePerTarget)

    rackTarget = [{"id": rackTargetFirstId,
                   "absorbing": False,
                   "shape": {"x": stageLeftX - stageWallSpacingX,
                             "y": stageLeftY + (stageWallSpacingY - 1),
                             "width": largeRoomWidth / 4,
                             "height": 1,
                             "type": "RECTANGLE"},
                   "waitingTime": clearingTime,
                   "waitingTimeYellowPhase": 0.0,
                   "parallelWaiters": 0,
                   "individualWaiting": True,
                   "deletionDistance": 0.1,
                   "startingWithRedLight": False,
                   "nextSpeed": -1.0}]

    finalTarget = [{"id": finalTargetId,
                    "absorbing": True,
                    "shape": {"x": boundingBoxWidth + wallThickness,
                              "y": boundingBoxWidth + wallThickness,
                              "width": 0.5,
                              "height": 2,
                              "type": "RECTANGLE"},
                    "waitingTime": 0,
                    "waitingTimeYellowPhase": 0.0,
                    "parallelWaiters": 0,
                    "individualWaiting": True,
                    "deletionDistance": 0.1,
                    "startingWithRedLight": False,
                    "nextSpeed": -1.0}]

    targetList = stageLeftTargets + stageRightTargets + benchesTargets + breakTargets + rackTarget + finalTarget

    # obstacles
    obstacleIdCounter = 2001
    entryWidth = 2
    obstacle1 = {
        "shape": {
            "x": boundingBoxWidth + sourcesAreaWidth,
            "y": boundingBoxWidth,
            "width": largeRoomWidth + smallRoomWidth + 3 * wallThickness,
            "height": wallThickness,
            "type": "RECTANGLE"
        },
        "id": obstacleIdCounter
    }

    obstacleIdCounter = obstacleIdCounter + 1

    obstacle2 = {
        "shape": {
            "x": boundingBoxWidth + sourcesAreaWidth,
            "y": boundingBoxWidth + entryWidth + wallThickness,
            "width": wallThickness,
            "height": largeRoomHeight - entryWidth + wallThickness,
            "type": "RECTANGLE"
        },
        "id": obstacleIdCounter
    }
    obstacleIdCounter = obstacleIdCounter + 1

    obstacle3 = {
        "shape": {
            "x": boundingBoxWidth + sourcesAreaWidth + wallThickness,
            "y": boundingBoxWidth + largeRoomHeight + wallThickness,
            "width": largeRoomWidth + wallThickness + smallRoomWidth,
            "height": wallThickness,
            "type": "RECTANGLE"
        },
        "id": obstacleIdCounter
    }
    obstacleIdCounter = obstacleIdCounter + 1

    obstacle4 = {
        "shape": {
            "x": boundingBoxWidth + sourcesAreaWidth + wallThickness + largeRoomWidth,
            "y": boundingBoxWidth + entryWidth + wallThickness,
            "width": wallThickness,
            "height": largeRoomHeight - entryWidth,
            "type": "RECTANGLE"
        },
        "id": obstacleIdCounter
    }
    obstacleIdCounter = obstacleIdCounter + 1

    obstacle5 = {
        "shape": {
            "x": boundingBoxWidth + sourcesAreaWidth + wallThickness + largeRoomWidth + wallThickness + smallRoomWidth,
            "y": boundingBoxWidth + wallThickness,
            "width": wallThickness,
            "height": largeRoomHeight + wallThickness,
            "type": "RECTANGLE"
        },
        "id": obstacleIdCounter
    }

    obstacleList = [obstacle1, obstacle2, obstacle3, obstacle4, obstacle5]

    # create new scenario file
    newFilename = f"hamner-2020-life_seed_{seed}"
    templateFilename = "../../scenarios/hamner-2020-life-template.scenario"

    with open(templateFilename) as f:
        scenarioFile = json.load(f)

    scenarioFile['scenario']['topography']['attributes']['boundingBoxWidth'] = boundingBoxWidth
    scenarioFile['scenario']['topography']['attributes']['bounds']['width'] = boundsX
    scenarioFile['scenario']['topography']['attributes']['bounds']['height'] = boundsY
    scenarioFile['scenario']['topography']['sources'] = sourceList
    scenarioFile['scenario']['topography']['targets'] = targetList
    scenarioFile['scenario']['topography']['obstacles'] = obstacleList
    scenarioFile['name'] = newFilename

    # update file
    updatedFile = "../../scenarios/" + newFilename + ".scenario"
    with open(updatedFile, 'w') as f:
        json.dump(scenarioFile, f, indent=2)
