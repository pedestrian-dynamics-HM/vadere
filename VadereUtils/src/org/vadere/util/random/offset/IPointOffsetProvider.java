package org.vadere.util.random.offset;

import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;

public interface IPointOffsetProvider {

    static IPointOffsetProvider noOffset(){
        return VPoint::new;
    }

    IPoint applyOffset(IPoint point);

}
