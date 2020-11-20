#!/usr/bin/env python3

from fabric import Connection

c = Connection("minimuc.cs.hm.edu", user="dlehmberg", port=5022)

result = c.run(
    """python3 -c 'import suqc.configuration
print(suqc.configuration.get_con_path())
'"""
)
print(result)

c.close()
