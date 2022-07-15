/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.gcdm.client;

import dev.ucdm.array.Array;
import dev.ucdm.dataset.transform.vertical.VerticalTransform;
import org.jetbrains.annotations.Nullable;

/**
 * Implementation of VerticalTransform that used Gcdm to get the VerticalTransform 3D array.
 * This allows us to not have to implement the VerticalTransform on the client.
 * Not immutable because we need to set the GcdmGridDataset after construction.
 */
public class GcdmVerticalTransform implements VerticalTransform {
  private GcdmGridDataset gridDataset;
  private final int id;
  private final String name;
  private final String units;

  public GcdmVerticalTransform(int id, String name, String units) {
    this.id = id;
    this.name = name;
    this.units = units;
  }

  void setDataset(GcdmGridDataset gridDataset) {
    this.gridDataset = gridDataset;
  }

  public int getId() {
    return id;
  }

  @Override
  public String getName() {
    return name;
  }

  @Nullable
  @Override
  public String getUnitString() {
    return units;
  }

  @Override
  public Array<Number> getCoordinateArray3D(int timeIndex) {
    return gridDataset.getVerticalTransform(this.name, timeIndex);
  }
}
