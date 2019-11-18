# -*- coding: utf-8 -*-
# Eclipse SUMO, Simulation of Urban MObility; see https://eclipse.org/sumo
# Copyright (C) 2011-2019 German Aerospace Center (DLR) and others.
# This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v2.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v20.html
# SPDX-License-Identifier: EPL-2.0

# @file    _person_vadere.py
# @author  Philipp Schuegraf
# @date    2019-11-13
# @version $Id$

from __future__ import absolute_import

import struct

from . import constants as tc
from ._person import PersonDomain, _RETURN_VALUE_FUNC
from .storage import Storage

_RETURN_VALUE_FUNC[tc.NEXT_FREE_ID] = Storage.readInt
_RETURN_VALUE_FUNC[tc.VAR_POSITION_LIST] = Storage.readPositionList
_RETURN_VALUE_FUNC[tc.VAR_TARGET_LIST] = Storage.readStringList


class PersonVadereDomain(PersonDomain):

    def getTargetList(self, personID):
        """Get possible targets

        """
        return self._getUniversal(tc.VAR_TARGET_LIST, personID)

    def setTargetList(self, personID, targetList):
        self._connection._beginMessage(tc.CMD_SET_PERSON_VARIABLE, tc.VAR_TARGET_LIST, personID,
                                       1 + 4 + (4 + 1) * len(targetList))
        self._connection._packStringList(targetList)
        self._connection._sendExact()

    def getPositionList(self):
        return self._getUniversal(tc.VAR_POSITION_LIST)

    def getPositionListAscendingIds(self):
        listOfTuples = self._getUniversal(tc.VAR_POSITION_LIST)
        listOfPositions = [(0., 0.) for i in range(len(listOfTuples))]
        for t in listOfTuples:
            index = int(t[0]) - 1
            listOfPositions[index] = (t[1], t[2])
        return listOfPositions

    def add(self, personID, pos2D, *targets):
        """add(string, (double, double), stringlist)
        """
        lenID = 1 + 4 + 1
        lenPos = 1 + 8 + 8
        lenTargets = 1 + 4 + (1 + 4) * len(targets)
        lenObj = lenID + lenPos + lenTargets
        self._connection._beginMessage(tc.CMD_SET_PERSON_VARIABLE, tc.ADD, personID, 1 + 4 + lenObj)
        self._connection._string += struct.pack("!Bi", tc.TYPE_COMPOUND, 3)
        self._connection._packString(personID)
        self._connection._string += struct.pack("!Bdd", tc.POSITION_2D, *pos2D)
        self._connection._packStringList(targets)
        self._connection._sendExact()

    def setPosition(self, personID, x, y):
        self._connection._beginMessage(tc.CMD_SET_PERSON_VARIABLE, tc.VAR_POSITION, personID, 1 + 8 + 8)
        self._connection._string += struct.pack("!Bdd", tc.POSITION_2D, x, y)
        self._connection._sendExact()

    def setHeuristic(self, personID, heuristic):
        self._connection._sendStringCmd(
            tc.CMD_SET_PERSON_VARIABLE, tc.VAR_HEURISTIC, personID, heuristic
        )
