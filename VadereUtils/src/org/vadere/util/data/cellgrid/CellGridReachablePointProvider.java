package org.vadere.util.data.cellgrid;

import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.random.ConstantIntegerPointProvider;
import org.vadere.util.random.IPointOffsetProvider;
import org.vadere.util.random.IReachablePointProvider;
import org.vadere.util.random.RandomIPointProvider;
import org.vadere.util.random.UniformIntegerIPointSupplier;
import org.vadere.util.random.UniformPointOffsetDouble;

import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class CellGridReachablePointProvider implements IReachablePointProvider {

    private final CellGrid cellGrid;
    private RandomIPointProvider randomPointProvider;
    private ConstantIntegerPointProvider constPointProvider;
    private IPointOffsetProvider offsetProvider;


    public static CellGridReachablePointProvider createUniform(final CellGrid cellGrid, final Random random){
        IPointOffsetProvider offset = new UniformPointOffsetDouble(random, 0.5);
        RandomIPointProvider pointSupplier = new UniformIntegerIPointSupplier(random,
                cellGrid.getNumPointsX()-1, cellGrid.getNumPointsY()-1);
        return new CellGridReachablePointProvider(cellGrid, pointSupplier, offset);
    }



    private CellGridReachablePointProvider(CellGrid cellGrid, RandomIPointProvider pointSupplier, IPointOffsetProvider offset) {
        this.cellGrid = cellGrid;
        this.randomPointProvider = pointSupplier;
        this.offsetProvider = offset;
        this.constPointProvider = new ConstantIntegerPointProvider(cellGrid.numPointsX -1, cellGrid.numPointsY-1);
    }

    private IPoint get(RandomIPointProvider provider, Predicate<Double> obstacleDistPredicate, IPointOffsetProvider offsetProvider){
        boolean legalState;
        CellState state;
        IPoint p;
        do {
            p = provider.nextPoint();
            state = cellGrid.getValue((int)p.getX(), (int)p.getY());
            legalState = state.tag == PathFindingTag.Reachable && obstacleDistPredicate.test(state.potential);
        } while (!legalState);
        return offsetProvider.applyOffset(p);
    }

    @Override
    public void setRandomIPointProvider(RandomIPointProvider provider) {
        this.randomPointProvider = provider;
    }

    @Override
    public RandomIPointProvider getRandomIPointProvider() {
        return randomPointProvider;
    }

    @Override
    public void setIPointOffsetProvider(IPointOffsetProvider provider) {
        this.offsetProvider = provider;
    }

    @Override
    public IPointOffsetProvider getIPointOffsetProvider() {
        return offsetProvider;
    }

    @Override
    public Stream<IPoint> stream(Predicate<Double> obstacleDistPredicate) {
        return Stream.generate(() -> get(constPointProvider, obstacleDistPredicate, offsetProvider));
    }

    @Override
    public Stream<IPoint> randomStream(Predicate<Double> obstacleDistPredicate) {
        return Stream.generate(() -> get(randomPointProvider, obstacleDistPredicate, offsetProvider));
    }
}
