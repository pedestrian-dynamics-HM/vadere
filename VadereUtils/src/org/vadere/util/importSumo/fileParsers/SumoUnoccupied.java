package org.vadere.util.importSumo.fileParsers;

import org.jetbrains.annotations.Nullable;
import org.locationtech.jts.geom.Polygon;

import java.awt.*;

public class SumoUnoccupied extends SumoObject{
    public SumoUnoccupied(Polygon polygon, int vadereId, @Nullable Color colorOverride) {
        super(polygon, vadereId, colorOverride);
    }

    @Override
    public String getSumoId() {
        return  getTypedSumoId();
    }

    @Override
    public String getTypedSumoId() {
        return "unoccupied" +getSumoId();
    }
}
