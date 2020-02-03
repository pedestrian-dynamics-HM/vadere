from ._api_wrapper import ApiWrapper


class PolygonapiWrapper(ApiWrapper):

    def getTopographyBounds(self):
        response = self._apiObject.getTopographyBounds()
        result = response.getResponseData()
        return result

    def getIDList(self):
        response = self._apiObject.getIDList()
        result = response.getResponseData()
        return result

    def getType(self, elementID):
        response = self._apiObject.getType(elementID)
        result = response.getResponseData()
        return result

    def getShape(self, elementID):
        response = self._apiObject.getShape(elementID)
        result = response.getResponseData()
        points = result.getPoints()
        shape = []
        for p in points:
            shape += [(p.getX(), p.getY())]
        return shape

    def getCentroid(self, elementID):
        response = self._apiObject.getCentroid(elementID)
        result = response.getResponseData()
        vpoint = (result.getX(), result.getY())
        return vpoint

    def getDistance(self, elementID, x, y):
        point = self._arraylistClass()
        point.add(self._stringClass(str(x)))
        point.add(self._stringClass(str(y)))
        response = self._apiObject.getDistance(elementID, point)
        result = response.getResponseData().get(0)
        return float(result)

    def getPosition2D(self, elementID):
        response = self._apiObject.getPosition2D(elementID)
        result = response.getResponseData()
        vpoint = (result.getX(), result.getY())
        return vpoint

    def getIDCount(self):
        response = self._apiObject.getIDCount()
        result = response.getResponseData()
        return result
