#!/usr/bin/env python3

import unittest


class TestExamples(unittest.TestCase):
    def test_first_example(self):
        import suqc.tutorial.first_example

        self.assertTrue(True)


if __name__ == "__main__":
    unittest.main()
