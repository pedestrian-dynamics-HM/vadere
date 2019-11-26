from py4j.java_gateway import java_import

from ._api_wrapper import ApiWrapper


class PersonapiWrapper(ApiWrapper):

    def __init__(self, apiObject, gateway):
        super(PersonapiWrapper, self).__init__(apiObject, gateway)

        # imports
        java_import(self._gateway.jvm, "org.vadere.manager.traci.compoundobjects.*")
        java_import(self._gateway.jvm, "org.vadere.util.geometry.shapes")
        java_import(self._gateway.jvm, "java.util.*")

        # java types
        self._stringClass = self._gateway.jvm.String

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

    def getSpeed(self, personID):
        response = self._apiObject.getSpeed(personID)
        result = response.getResponseData()
        return result

    def setVelocity(self, personID, velocity):
        response = self._apiObject.setVelocity(personID, velocity)
        result = response.toString()
        return result

    def getPosition2D(self, personID):
        response = self._apiObject.getPosition2D(personID)
        result = response.getResponseData()
        return result

    def setPosition2D(self, personID, x, y):
        vpoint = self._gateway.jvm.VPoint(x, y)
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

        return position2DList

    def getTagetList(self, personID):
        response = self._apiObject.getTargetList(personID)
        result = response.getResponseData()
        return result

    def setTargetList(self, personID, targets):
        targetsJavaArrayList = self._gateway.jvm.ArrayList()
        for t in targets:
            targetsJavaArrayList.add(t)
        response = self._apiObject.setTargetList(personID, targetsJavaArrayList)
        result = response.toString()
        return result

    def createNew(self, personID, x, y, targets):
        targetsJavaStringArray = self._gateway.new_array(self._stringClass, len(targets))
        for i, t in enumerate(targets):
            targetsJavaStringArray[i] = t
        compoundObjectBuidler = self._gateway.jvm.CompoundObjectBuilder()
        personCreateData = compoundObjectBuidler.createPerson(personID, x, y, targetsJavaStringArray)
        response = self._apiObject.createNew(personID, personCreateData)
        result = response.toString()
        return result