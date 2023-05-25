package org.vadere.meshing.mesh.gen;

import java.util.ArrayList;
import org.vadere.meshing.mesh.inter.IProperty;

public class GenProperty<T> implements IProperty {
  private ArrayList<T> data;

  T getProperty(int idx) {
    return data.get(idx);
  }

  @Override
  public void clear() {
    data.clear();
  }

  @Override
  public int size() {
    return data.size();
  }
}
