from ._api_wrapper import ApiWrapper


class PersonapiWrapper(ApiWrapper):

    def getIDList(self):
        response = self._apiObject.getIDList()
        result = response.getResponseData()
        return result

    def getNextFreeId(self):
        response = self._apiObject.getNextFreeId()
        result = response.getResponseData()
        return result

    def getIDCount(self):
        response = self._apiObject.getIDCount()
        result = response.getResponseData()
        return result

    def getFreeFlowSpeed(self, personID):
        response = self._apiObject.getFreeFlowSpeed(personID)
        result = response.getResponseData()
        return result

    def setFreeFlowSpeed(self, personID, speed):
        response = self._apiObject.setVelocity(personID, speed)
        result = response.toString()
        return result

    def getPosition2D(self, personID):
        response = self._apiObject.getPosition2D(personID)
        result = response.getResponseData()
        return (float(result.getX()), float(result.getY()))

    def getVelocity(self, personID):
        response = self._apiObject.getVelocity(personID)
        result = response.getResponseData()
        return (float(result.getX()), float(result.getY()))

    def getMaximumSpeed(self, personID):
        response = self._apiObject.getMaximumSpeed(personID)
        result = response.getResponseData()
        return float(result)

    def setPosition2D(self, personID, x, y):
        vpoint = self._vpointClass(float(x), float(y))
        response = self._apiObject.setPosition2D(personID, vpoint)
        result = response.toString()
        return result

    def getPosition2DList(self):
        response = self._apiObject.getPosition2DList()
        result = response.getResponseData()

        ids = self.getIDList()

        position2DList = {}
        for id in ids:
            vpoint = result[id]
            position2DList[id] = [vpoint.getX(), vpoint.getY()]

        return dict(position2DList)

    def getTargetList(self, personID):
        response = self._apiObject.getTargetList(personID)
        result = response.getResponseData()
        return result

    def setTargetList(self, personID, targets):
        targetsJavaArrayList = self._arraylistClass()
        for t in targets:
            targetsJavaArrayList.add(t)
        response = self._apiObject.setTargetList(personID, targetsJavaArrayList)
        result = response.toString()
        return result

    def setNextTargetListIndex(self, personID, nextTargetListIndex):
        response = self._apiObject.setNextTargetListIndex(personID, nextTargetListIndex)
        result = response.toString()
        return result

    def getNextTagetListIndex(self, personID):
        response = self._apiObject.getNextTargetListIndex(personID)
        result = response.toString()
        return result

    def createNew(self, jsonFilePath):
        response = self._apiObject.createNew(jsonFilePath)
        result = response.toString()
        return result
