package org.vadere.util.importSumo.fileParsers;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.GeometryFixer;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.Objects;


public abstract class SumoObject {

    protected Polygon polygon;
    private final int vadereId;
    @Nullable private final Color colorOverride;

    public SumoObject(Polygon polygon, int vadereId, @Nullable Color colorOverride) {
        this.vadereId = vadereId;
        this.colorOverride = colorOverride;
        setPolygon(polygon);
    }

    @Nullable
    public Color getColorOverride() {
        return colorOverride;
    }

    public abstract String getSumoId();
    public abstract String getTypedSumoId();

    public int getVadereId() {
        return vadereId;
    }

    public Polygon getPolygon() {
        return polygon;
    }

    public void setPolygon(Polygon polygon) {
        this.polygon = FixInvalidPolygons(polygon);
    }

    public void setPolygonUnsafe(Polygon polygon) {
        this.polygon = polygon;
    }

    private static Polygon FixInvalidPolygons(Polygon polygon) {
        if(polygon==null){
            return null;
        }

        if (polygon.isValid() || polygon.isEmpty()) {
            return polygon;
        }

        Geometry fix = GeometryFixer.fix(polygon);
        if (fix.isValid() && !fix.isEmpty() && fix instanceof Polygon fixedPolygon) {
            return fixedPolygon;
        }

        return polygon; // failed
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SumoObject sumoObject = (SumoObject) o;
        return vadereId == sumoObject.vadereId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(vadereId);
    }
}
