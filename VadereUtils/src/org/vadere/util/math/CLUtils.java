package org.vadere.util.math;


import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryUtil;
import java.nio.FloatBuffer;


public class CLUtils {

    public static FloatBuffer toFloatBuffer(@NotNull final float[] floats) {
        FloatBuffer floatBuffer = MemoryUtil.memAllocFloat(floats.length);

        for(int i = 0; i < floats.length; i++) {
            floatBuffer.put(i, floats[i]);
        }
        return floatBuffer;
    }

    public static float[] toFloatArray(@NotNull FloatBuffer floatBuffer, final int size) {
	    float[] result = new float[size];
	    for(int i = 0; i < size; i++) {
	        result[i] = floatBuffer.get(i);
        }
        return result;
    }
}
