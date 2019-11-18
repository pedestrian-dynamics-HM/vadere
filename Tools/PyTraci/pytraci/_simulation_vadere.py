# -*- coding: utf-8 -*-
# Eclipse SUMO, Simulation of Urban MObility; see https://eclipse.org/sumo
# Copyright (C) 2011-2019 German Aerospace Center (DLR) and others.
# This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v2.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v20.html
# SPDX-License-Identifier: EPL-2.0

# @file    _simulation_vadere.py
# @author  Philipp Schuegraf
# @date    2019-11-15
# @version $Id$

from __future__ import absolute_import

import struct

from . import constants as tc
from ._simulation import SimulationDomain


class SimulationVadereDomain(SimulationDomain):

    def addTargetChanger(self, targetChangerID, points, reachDist, nextTargetIsPedestrian, nextTarget, prob):
        lenID = 1 + 4 + len(targetChangerID)
        lenPoints = 1 + 4 + 4 * len(points) + sum(map(len, points))
        lenReachDist = 1 + 8
        lenNextTargetIsPedestrian = 1 + 4
        lenNextTarget = 1 + 4 + len(nextTarget)
        lenProb = 1 + 8
        lenObj = lenID + lenPoints + lenReachDist + lenNextTargetIsPedestrian + lenNextTarget + lenProb

        self._connection._beginMessage(tc.CMD_SET_SIM_VARIABLE, tc.ADD_TARGET_CHANGER, targetChangerID, 1 + 4 + lenObj)
        self._connection._string += struct.pack("!Bi", tc.TYPE_COMPOUND, 6)
        self._connection._packString(targetChangerID)
        self._connection._packStringList(points)
        self._connection._string += struct.pack("!Bd", tc.TYPE_DOUBLE, reachDist)
        self._connection._string += struct.pack("!Bi", tc.TYPE_INTEGER, nextTargetIsPedestrian)
        self._connection._packString(nextTarget)
        self._connection._string += struct.pack("!Bd", tc.TYPE_DOUBLE, prob)
        self._connection._sendExact()

    def addWaitingArea(self, waitingAreaID, points):
        lenID = 1 + 4 + len(waitingAreaID)
        lenPoints = 1 + 4 + 4 * len(points) + sum(map(len, points))
        lenObj = lenID + lenPoints

        self._connection._beginMessage(tc.CMD_SET_SIM_VARIABLE, tc.ADD_WAITING_AREA, waitingAreaID, 1 + 4 + lenObj)
        self._connection._string += struct.pack("!Bi", tc.TYPE_COMPOUND, 2)
        self._connection._packString(waitingAreaID)
        self._connection._packStringList(points)
        self._connection._sendExact()

    def removeTargetChanger(self, targetChangerID):
        lenID = 1 + 4 + len(targetChangerID)

        self._connection._beginMessage(tc.CMD_SET_SIM_VARIABLE, tc.REMOVE_WAITING_AREA, targetChangerID, lenID)
        self._connection._packString(targetChangerID)
        self._connection._sendExact()

    def removeWaitingArea(self, waitingAreaID):
        lenID = 1 + 4 + len(waitingAreaID)

        self._connection._beginMessage(tc.CMD_SET_SIM_VARIABLE, tc.REMOVE_WAITING_AREA, waitingAreaID, lenID)
        self._connection._packString(waitingAreaID)
        self._connection._sendExact()
