#!/usr/bin/env python3 

import unittest

# --------------------------------------------------
# people who contributed code
__authors__ = "Daniel Lehmberg"
# people who made suggestions or reported bugs but didn't contribute code
__credits__ = ["n/a"]
# --------------------------------------------------


class TestExamples(unittest.TestCase):
    
    def test_first_example(self):
        import suqc.tutorial.first_example
        self.assertTrue(True)


if __name__ == "__main__":
    unittest.main()
