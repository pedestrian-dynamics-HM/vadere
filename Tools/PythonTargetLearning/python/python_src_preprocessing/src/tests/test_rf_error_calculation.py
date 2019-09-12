import unittest
import math
import numpy as np

from rf.regression import calculate_errors

class TestErrorCalculation(unittest.TestCase):

    def test_max_error(self):
        errors = calculate_errors(
            np.array([[1, 0]]), 
            np.array([[0, 1]]))

        self.assertEqual(errors['euclid']['mean'], math.sqrt(2))
        self.assertEqual(errors['euclid']['std'], 0.0)
        self.assertListEqual(list(errors['mean_abs_error']), [1.0, 1.0])
        self.assertListEqual(list(errors['mean_error']), [1.0, -1.0])
        self.assertListEqual(list(errors['rmse']), [1.0, 1.0])


    def test_no_error(self):
        errors = calculate_errors(
            np.array([[0, 0], 
             [1, 0],
             [0, 1],
             [1, 1],
             [0.5, 0.5]]),
            np.array([[0, 0], 
             [1, 0],
             [0, 1],
             [1, 1],
             [0.5, 0.5]]))

        self.assertEqual(errors['euclid']['mean'], 0.0)
        self.assertEqual(errors['euclid']['std'], 0.0)
        self.assertListEqual(list(errors['mean_abs_error']), [0.0, 0.0])
        self.assertListEqual(list(errors['mean_error']), [0.0, 0.0])
        self.assertListEqual(list(errors['rmse']), [0.0, 0.0])