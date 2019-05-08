import unittest

import filter_vadere_obstacles as under_test

class TestFilterVadereObstacles(unittest.TestCase):

    def test_raise_exception_on_invalid_boundary_box_raises_value_error_on_empty_boundary_box(self):
        with self.assertRaises(ValueError):
            boundary_box = {}
            under_test.raise_exception_on_invalid_boundary_box(boundary_box)
    
    def test_raise_exception_on_invalid_boundary_box_raises_value_error_if_boundary_box_contains_wrong_keys(self):
        with self.assertRaises(ValueError):
            boundary_box = { "x1": 0.0, "y1": 0.0, "x2": 1.0, "z": 1.0 }
            under_test.raise_exception_on_invalid_boundary_box(boundary_box)
    
    def test_raise_exception_on_invalid_boundary_box_raises_value_error_if_boundary_box_contains_too_less_keys(self):
        with self.assertRaises(ValueError):
            boundary_box = { "x1": 0.0, "y1": 0.0, "x2": 1.0 }
            under_test.raise_exception_on_invalid_boundary_box(boundary_box)

    def test_raise_exception_on_invalid_boundary_box_raises_value_error_if_boundary_box_contains_too_much_keys(self):
        with self.assertRaises(ValueError):
            boundary_box = { "x1": 0.0, "y1": 0.0, "x2": 1.0 , "y2": 1.0, "z": 0.0 }
            under_test.raise_exception_on_invalid_boundary_box(boundary_box)
            
    def test_raise_exception_on_invalid_boundary_box_raises_error_if_x2_less_than_x1(self):
        with self.assertRaises(ValueError):
            boundary_box = { "x1": 0.0, "y1": 0.0, "x2": -1.0 , "y2": 1.0 }
            under_test.raise_exception_on_invalid_boundary_box(boundary_box)

    def test_raise_exception_on_invalid_boundary_box_raises_error_if_x2_equals_x1(self):
        with self.assertRaises(ValueError):
            boundary_box = { "x1": 0.0, "y1": 0.0, "x2": 0.0 , "y2": 1.0 }
            under_test.raise_exception_on_invalid_boundary_box(boundary_box)

    def test_raise_exception_on_invalid_boundary_box_raises_error_if_y2_less_than_y1(self):
        with self.assertRaises(ValueError):
            boundary_box = { "x1": 0.0, "y1": 0.0, "x2":1.0 , "y2": -1.0 }
            under_test.raise_exception_on_invalid_boundary_box(boundary_box)

    def test_raise_exception_on_invalid_boundary_box_raises_error_if_y2_equals_y1(self):
        with self.assertRaises(ValueError):
            boundary_box = { "x1": 0.0, "y1": 0.0, "x2":1.0 , "y2": 0.0 }
            under_test.raise_exception_on_invalid_boundary_box(boundary_box)

    def test_raise_exception_on_invalid_boundary_box_returns_none_if_everything_alright(self):
        boundary_box = { "x1": 0.0, "y1": 0.0, "x2":1.0 , "y2": 1.0 }
        
        expected_return_value = None
        actual_return_value = under_test.raise_exception_on_invalid_boundary_box(boundary_box)
        
        self.assertEqual(expected_return_value, actual_return_value)

if __name__ == '__main__':
    unittest.main()
