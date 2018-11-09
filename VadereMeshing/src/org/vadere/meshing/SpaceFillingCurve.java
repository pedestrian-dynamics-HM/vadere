package org.vadere.meshing;

import org.apache.commons.lang3.tuple.Pair;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

/**
 * @author Benedikt Zoennchen
 */
public class SpaceFillingCurve {

    private final VRectangle bbox;
    private static final int precision = 10;

    private enum State {
        TopLeft, TopRight, BottomLeft, BottomRight;
    }

    public SpaceFillingCurve(final VRectangle bbox) {
        this.bbox = bbox;
    }

    public double compute(final IPoint point) {
        // normalize to [0,1] x [0,1]
        double x = (point.getX() - bbox.getMinX()) / bbox.getWidth();
        double y = (point.getY() - bbox.getMinY()) / bbox.getHeight();

        String xCode = toBinary(x);
        String yCode = toBinary(y);

        // run the automaton
        assert xCode.length() == yCode.length();

        State currentState = State.TopLeft;
        byte[] result = new byte[xCode.length()];
        for(int i = 0; i < xCode.length(); i++) {
            // TODO improve the conversion
            Pair<State, Byte> pair = nextState(currentState, Byte.parseByte(xCode.charAt(i)+""), Byte.parseByte(yCode.charAt(i)+""));
            result[i] = pair.getRight();
            currentState = pair.getLeft();
        }

        return byteCodeToDouble(result);
    }

  /*  public <T> void sort(final List<T> list, Function<T, IPoint> f) {
        List<State> bucketStates = new ArrayList<>();
        List<String> xStrings = list.stream().map(object -> toBinary(f.apply(object).getX())).collect(Collectors.);
        List<String> yStrings = list.stream().map(object -> toBinary(f.apply(object).getY())).collect(Collectors.toList());

        List<Integer> bucketSizes = new ArrayList<>();
        bucketSizes.add(list.size());

        int nBuckets = 1;
        for(Integer bSize : bucketSizes) {

        }

    }
*/

    private double byteCodeToDouble(final byte[] code) {
        double sum = 0.0;
        for(int i = 0; i < code.length; i++) {
            sum += code[i] * (1.0/(4<<(2*i)));
        }
        return sum;
    }

    public String toBinary(double decimal) {
        double tmp = decimal;
        for (int i = 0; i < precision; i++) {
            tmp *= 2;
        }

        String binaryString =  Long.toBinaryString((long)tmp);

        for(int i = binaryString.length(); i < precision; i++) {
            binaryString = "0"+binaryString;
        }

        return binaryString.substring(0, precision);
    }

    public Pair<State, Byte> nextState(final State current, Byte x, Byte y) {
        byte zero = 0;
        byte one = 1;
        byte two = 2;
        byte three = 3;

        switch (current) {
            case TopLeft:
                if(x == 0 && y == 1)
                    return Pair.of(State.TopLeft, one);
                else if (x == 1 && y == 1)
                    return Pair.of(State.TopLeft, two);
                else if (x == 0 && y == 0)
                    return Pair.of(State.TopRight, zero);
                else
                    return Pair.of(State.BottomLeft, three);
            case TopRight:
                if(x == 0 && y == 1)
                    return Pair.of(State.BottomRight, three);
                else if (x == 1 && y == 1)
                    return Pair.of(State.TopRight, one);
                else if (x == 0 && y == 0)
                    return Pair.of(State.TopLeft, zero);
                else
                    return Pair.of(State.TopRight, two);
            case BottomLeft:
                if(x == 0 && y == 1)
                    return Pair.of(State.BottomLeft, one);
                else if (x == 1 && y == 1)
                    return Pair.of(State.BottomRight, zero);
                else if (x == 0 && y == 0)
                    return Pair.of(State.BottomLeft, two);
                else
                    return Pair.of(State.TopLeft, three);
            case BottomRight:
                if(x == 0 && y == 1)
                    return Pair.of(State.TopRight, three);
                else if (x == 1 && y == 1)
                    return Pair.of(State.BottomLeft, zero);
                else if (x == 0 && y == 0)
                    return Pair.of(State.BottomRight, two);
                else
                    return Pair.of(State.BottomRight, one);
        }

        throw new IllegalArgumentException("illegal state (" + current + "," + x + "," + y + ")");
    }


    public static void main(String... args) {
        SpaceFillingCurve curve = new SpaceFillingCurve(new VRectangle(0,0,1,1));
        double positionOnCurve = curve.compute(new VPoint(13.0/16.0, 7.0 / 16.0));
        System.out.println(positionOnCurve);
    }


}
