package org.vadere.util.random;

import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.function.Supplier;

public class ConstantIntegerPointProvider implements IPointProvider {

    private Supplier<IPoint> iPointSupplier;

    private  int xUpperBound;
    private  int yUpperBound;


    public ConstantIntegerPointProvider(int xUpperBound, int yUpperBound){
        this.xUpperBound = xUpperBound;
        this.yUpperBound = yUpperBound;
        this.iPointSupplier = new Supplier<IPoint>() {
            int x;
            int y;

            @Override
            public IPoint get() {
                if (x == xUpperBound){
                    x = 0;
                    y = y == yUpperBound ? 0 : ++y;
                } else {
                    x++;
                }
                return new VPoint(x, y);
            }
        };
    }

    @Override
    public double getSupportUpperBoundX() {
        return xUpperBound;
    }

    @Override
    public double getSupportLowerBoundX() {
        return 0;
    }

    @Override
    public double getSupportUpperBoundY() {
        return yUpperBound;
    }

    @Override
    public double getSupportLowerBoundY() {
        return 0;
    }

    @Override
    public IPoint nextPoint() {
        return iPointSupplier.get();
    }
}
