package org.vadere.util.random;

import org.vadere.util.geometry.shapes.IPoint;

public interface IPointOffsetProvider {

    static IPointOffsetProvider noOffset(){
        return new IPointOffsetProvider() {
            @Override
            public IPoint applyOffset(IPoint point) {
                return point;
            }

            @Override
            public IPoint applyOffset(IPoint point, double maxOffset) {
                return point;
            }
        };
    }

    IPoint applyOffset(IPoint point);

    IPoint applyOffset(IPoint point, double maxOffset);
}
