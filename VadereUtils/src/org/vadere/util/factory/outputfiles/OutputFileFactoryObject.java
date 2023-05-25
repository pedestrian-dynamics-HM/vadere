package org.vadere.util.factory.outputfiles;

import java.util.function.Supplier;
import org.vadere.util.factory.FactoryObject;

public class OutputFileFactoryObject<T> extends FactoryObject<T> {

  private final String keyName;
  private final String label;
  private final String description;

  public OutputFileFactoryObject(
      Class<? extends T> clazz,
      Supplier<T> supplier,
      String label,
      String description,
      String keyName) {
    super(clazz, supplier);
    this.label = label;
    this.description = description;
    this.keyName = keyName;
  }

  public String getKeyName() {
    return keyName;
  }

  public String getLabel() {
    return label;
  }

  public String getDescription() {
    return description;
  }
}
