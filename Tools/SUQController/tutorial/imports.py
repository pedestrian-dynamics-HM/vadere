import urllib

from suqc import *

# Downloads a Vadere model used in this example, if not already here -- can be ignored.
if not os.path.exists("vadere0_7rc.jar"):
    print("Downloading the vadere-console model....")
    # downloads file if it does not exist in this folder
    urllib.request.urlretrieve("https://syncandshare.lrz.de/dl/fi6svHdgohH5np2ErsjYGMoy",
                               "vadere0_7rc.jar")
    print("Download finished. The model (jar file) is located in tutorial folder.")


path2tutorial = os.path.dirname(os.path.realpath(__file__))
path2model = os.path.join(path2tutorial, "vadere0_7rc.jar")
path2scenario = os.path.join(path2tutorial, "example.scenario")