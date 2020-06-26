import urllib.request

from suqc import *

vadere_filename = "vadere1_4.jar"
download_link = (
    f"https://syncandshare.lrz.de/dl/fiSwg2d6GXkB1vzGB6XwhvTA/{vadere_filename}"
)

path2tutorial = os.path.dirname(os.path.realpath(__file__))
path2model = os.path.join(path2tutorial, vadere_filename)
path2scenario = os.path.join(path2tutorial, "example.scenario")

# Downloads a Vadere model used in this example, if not already here -- can be ignored.
if not os.path.exists(path2model):
    print(f"Downloading vadere-console {vadere_filename} model ...")
    # downloads file if it does not exist in this folder
    urllib.request.urlretrieve(download_link, path2model)
    print(
        f"Download of {vadere_filename} finished. The model (jar file) is located in tutorial folder."
    )
