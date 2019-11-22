from py4j.java_gateway import java_import

from ._api_wrapper import ApiWrapper


class PersonapiWrapper(ApiWrapper):

    def __init__(self, apiObject, gateway):
        super(PersonapiWrapper, self).__init__(apiObject, gateway)

        # imports
        java_import(self._gateway.jvm, "org.vadere.manager.traci.compoundobjects.*")

        # java types
        self._stringClass = self._gateway.jvm.String

    def getIDList(self):
        response = self._apiObject.getIDList()
        result = response.getResponseData()
        return result

    def createNew(self, personID, x, y, targets):
        targetsJavaStringArray = self._gateway.new_array(self._stringClass, len(targets))
        for i, t in enumerate(targets):
            targetsJavaStringArray[i] = t
        compoundObjectBuidler = self._gateway.jvm.CompoundObjectBuilder()
        personCreateData = compoundObjectBuidler.createPerson(personID, x, y, targetsJavaStringArray)
        self._apiObject.createNew(personID, personCreateData)