/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.grib.inventory;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractMCollection implements MCollection {
  protected final String name;
  protected Map<String, Object> auxMap = new HashMap<>();

  public AbstractMCollection(String name) {
    this.name = name;
  }

  @Override
  public String getCollectionName() {
    return name;
  }

  @Override
  public String getIndexFilename(String suffix) {
    return getRoot() + "/" + getCollectionName() + suffix;
  }

  @Override
  public Object getAuxInfo(String key) {
    return auxMap.get(key);
  }

  @Override
  public MCollection setAuxInfo(String key, Object value) {
    auxMap.put(key, value);
    return this;
  }
}
