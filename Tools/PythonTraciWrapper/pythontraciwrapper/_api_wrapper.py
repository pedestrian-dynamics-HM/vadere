from py4j.java_gateway import java_import


class ApiWrapper(object):
    def __init__(self, apiObject, gateway, client):
        self._apiObject = apiObject
        self._gateway = gateway
        self._client = client

        # imports
        java_import(self._gateway.jvm, "org.vadere.manager.traci.compoundobjects.*")
        java_import(self._gateway.jvm, "org.vadere.util.geometry.shapes.*")
        java_import(self._gateway.jvm, "java.util.*")

        # java types
        self._stringClass = self._gateway.jvm.String
        self._vpointClass = self._gateway.jvm.VPoint
        self._arraylistClass = self._gateway.jvm.ArrayList
