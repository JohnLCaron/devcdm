/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.dataset.transform.horiz;

import dev.ucdm.core.api.AttributeContainer;
import dev.ucdm.core.constants.CF;
import dev.ucdm.dataset.geoloc.Projection;

/** Create a LambertAzimuthal Projection from the information in the Coordinate Transform Variable. */
public class LambertAzimuthal extends AbstractProjectionCT implements ProjectionBuilder {

  public String getTransformName() {
    return CF.LAMBERT_AZIMUTHAL_EQUAL_AREA;
  }

  public Projection makeProjection(AttributeContainer ctv, String geoCoordinateUnits) {
    readStandardParams(ctv, geoCoordinateUnits);
    return new dev.ucdm.dataset.geoloc.projection.LambertAzimuthalEqualArea(lat0, lon0, false_easting, false_northing,
        earth_radius);
  }

  public Class<? extends Projection> getProjectionClass() {
    return dev.ucdm.dataset.geoloc.projection.LambertAzimuthalEqualArea.class;
  }
}

