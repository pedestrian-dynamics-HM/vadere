package org.vadere.util.importSumo.fileParsers.structures;

import org.locationtech.jts.geom.Polygon;
import org.vadere.util.importSumo.fileParsers.SumoObject;

import javax.annotation.Nullable;
import java.awt.*;

public class SumoStructure extends SumoObject {

    private final String id;
    private final Type type;

    public SumoStructure(int vadereId, String id, Type type, Polygon polygon, @Nullable Color color) {
        super(polygon, vadereId, color);
        this.id = id;
        this.type = type;
    }

    @Override
    public String getSumoId() {
        return id;
    }
    public String getTypedSumoId() { return "structure_" + id; }

    public Type getType() {
        return type;
    }

    enum Type{
        Building
    }
}
