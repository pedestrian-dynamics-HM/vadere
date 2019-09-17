import os
from sklearn.tree import export_graphviz

def visulize_rf(regressor):
    for i, tree in enumerate(regressor.estimators_):
        export_graphviz(tree, out_file="tree-{0}.dot".format(i))

    os.system('dot -Tpng tree.dot -o tree.png')