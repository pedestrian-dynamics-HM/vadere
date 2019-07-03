package org.vadere.util.geometry.shapes;

import org.junit.Test;

import static org.junit.Assert.*;

public class IPointTest {

    private static double ALLOWED_DOUBLE_TOLERANCE = 10e-3;

    @Test
    public void dotProductReturnsNullIfTwoNullVectorsAreCombined() {
        VPoint nullVector1 = new VPoint(0, 0);
        VPoint nullVector2 = new VPoint(0, 0);

        double expectedResult = 0;
        double actualResult = nullVector1.dotProduct(nullVector2);

        assertEquals(expectedResult, actualResult, ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test
    public void dotProductReturnsNullIfFirstVectorIsNullVector() {
        VPoint nullVector = new VPoint(0, 0);
        VPoint vector = new VPoint(1, 2);

        double expectedResult = 0;
        double actualResult = nullVector.dotProduct(vector);

        assertEquals(expectedResult, actualResult, ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test
    public void dotProductReturnsNullIfSecondVectorIsNullVector() {
        VPoint nullVector = new VPoint(0, 0);
        VPoint vector = new VPoint(1, 2);

        double expectedResult = 0;
        double actualResult = vector.dotProduct(nullVector);

        assertEquals(expectedResult, actualResult, ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test
    public void dotProductCombinesTwoPositiveVectorsProperly() {
        VPoint vector1 = new VPoint(1, 2);
        VPoint vector2 = new VPoint(3, 4);

        double expectedResult = (vector1.x * vector2.x) + (vector1.y * vector2.y);
        double actualResult = vector2.dotProduct(vector1);

        assertEquals(expectedResult, actualResult, ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test
    public void dotProductCombinesOnePositiveAndOneNegativeVectorProperly() {
        VPoint vector1 = new VPoint(1, 2);
        VPoint vector2 = new VPoint(-3, -4);

        double expectedResult = (vector1.x * vector2.x) + (vector1.y * vector2.y);
        double actualResult = vector2.dotProduct(vector1);

        assertEquals(expectedResult, actualResult, ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test
    public void dotProductCombinesOnePositiveAndOneNegativeVectorProperlyIfFirstVectorContainsASingleZero() {
        VPoint vector1 = new VPoint(1, 0);
        VPoint vector2 = new VPoint(-3, -4);

        double expectedResult = (vector1.x * vector2.x) + (vector1.y * vector2.y);
        double actualResult = vector2.dotProduct(vector1);

        assertEquals(expectedResult, actualResult, ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test
    public void dotProductCombinesOnePositiveAndOneNegativeVectorProperlyIfSecondVectorContainsASingleZero() {
        VPoint vector1 = new VPoint(1, 2);
        VPoint vector2 = new VPoint(0, -4);

        double expectedResult = (vector1.x * vector2.x) + (vector1.y * vector2.y);
        double actualResult = vector2.dotProduct(vector1);

        assertEquals(expectedResult, actualResult, ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test
    public void dotProductReturnsZeroIfBothVectorsAreOrthogonal() {
        VPoint vector1 = new VPoint(2, 1);
        VPoint vector2 = new VPoint(-1, 2);

        double expectedResult = 0;
        double actualResult = vector2.dotProduct(vector1);

        assertEquals(expectedResult, actualResult, ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test
    public void dotProductDoesNotChangeUsedNullVectors() {
        VPoint nullVector1 = new VPoint(0, 0);
        VPoint nullVector2 = new VPoint(0, 0);

        double expectedResult = 0;

        assertEquals(expectedResult, nullVector1.x, ALLOWED_DOUBLE_TOLERANCE);
        assertEquals(expectedResult, nullVector1.y, ALLOWED_DOUBLE_TOLERANCE);

        assertEquals(expectedResult, nullVector2.x, ALLOWED_DOUBLE_TOLERANCE);
        assertEquals(expectedResult, nullVector2.y, ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test
    public void dotProductDoesNotChangeUsedVectors() {
        VPoint vector1 = new VPoint(1, 2);
        VPoint vector2 = new VPoint(3, 4);

        assertEquals(1, vector1.x, ALLOWED_DOUBLE_TOLERANCE);
        assertEquals(2, vector1.y, ALLOWED_DOUBLE_TOLERANCE);

        assertEquals(3, vector2.x, ALLOWED_DOUBLE_TOLERANCE);
        assertEquals(4, vector2.y, ALLOWED_DOUBLE_TOLERANCE);
    }
}