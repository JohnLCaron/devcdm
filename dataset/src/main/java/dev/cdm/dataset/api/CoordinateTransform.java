/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.dataset.api;

import dev.cdm.core.api.AttributeContainer;
import dev.cdm.core.constants.CDM;
import org.jetbrains.annotations.Nullable;

/**
 * A Coordinate Transform Variable has the metadata needed to construct a Projection or Vertical Transform.
 */
public record CoordinateTransform( String name, AttributeContainer metadata, boolean isProjection) {

  /** The expected units of the x, y axis. */
  @Nullable
  public String getXYunits() {
    return metadata.findAttributeString(CDM.GeoCoordinateUnits, "km");
  }

}
