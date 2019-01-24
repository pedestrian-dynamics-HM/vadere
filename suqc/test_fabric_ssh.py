#!/usr/bin/env python3

# TODO: """ << INCLUDE DOCSTRING (one-line or multi-line) >> """

from fabric import Connection

# --------------------------------------------------
# people who contributed code
__authors__ = "Daniel Lehmberg"
# people who made suggestions or reported bugs but didn't contribute code
__credits__ = ["n/a"]
# --------------------------------------------------


c = Connection("minimuc.cs.hm.edu", user="dlehmberg", port=5022)

result = c.run("""python3 -c 'import suqc.configuration
print(suqc.configuration.get_con_path())
'""")
print(result)

c.close()
