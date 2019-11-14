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
from .storage import Storage
from . import constants as tc
from ._person import PersonDomain, _RETURN_VALUE_FUNC

_RETURN_VALUE_FUNC[tc.NEXT_FREE_ID] = Storage.readInt
_RETURN_VALUE_FUNC[tc.VAR_POSITION_LIST] = Storage.readPositionList
_RETURN_VALUE_FUNC[tc.VAR_TARGET_LIST] = Storage.readStringList


class PersonVadereDomain(PersonDomain):

    def getTargetList(self, personID):
        """Get possible targets

        """
        return self._getUniversal(tc.VAR_TARGET_LIST, personID)

    def setTargetList(self, personID, targetList):
        self._connection._beginMessage(tc.CMD_SET_PERSON_VARIABLE, tc.VAR_TARGET_LIST, personID, 1 + 4 + len(targetList))
        self._connection._packStringList(targetList)

    def getPositionList(self):
        return self._getUniversal(tc.VAR_POSITION_LIST)

    def add(self, personID, pos2D, *targets):
        """add(string, (double, double), stringlist)
        """
        self._connection._beginMessage(tc.CMD_SET_PERSON_VARIABLE, tc.ADD, personID,
                                       1 + 4 + 1 + 1 + 4 + 1 + 8 + 8 + 1 + 4 + len(targets) + 4 * len(targets))
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
