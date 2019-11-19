# -*- coding: utf-8 -*-
# Eclipse SUMO, Simulation of Urban MObility; see https://eclipse.org/sumo
# Copyright (C) 2011-2019 German Aerospace Center (DLR) and others.
# This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v2.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v20.html
# SPDX-License-Identifier: EPL-2.0

# @file    _polygon.py
# @author  Philipp Schuegraf
# @date    2019-11-19
# @version $Id$

from __future__ import absolute_import

import struct

from . import constants as tc
from ._polygon import PolygonDomain

class PolygonVadereDomain(PolygonDomain):

    def getIDList(self):
        return self._getUniversal(tc.TRACI_ID_LIST)
