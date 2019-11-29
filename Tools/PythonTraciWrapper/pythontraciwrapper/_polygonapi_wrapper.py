from ._api_wrapper import ApiWrapper

class PolygonapiWrapper(ApiWrapper):

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

    def getPosition2D(self, elementID):
        response = self._apiObject.getPosition2D(elementID)
        result = response.getResponseData()
        vpoint = (result.getX(), result.getY())
        return vpoint

    def getIDCount(self):
        response = self._apiObject.getIDCount()
        result = response.getResponseData()
        return result
