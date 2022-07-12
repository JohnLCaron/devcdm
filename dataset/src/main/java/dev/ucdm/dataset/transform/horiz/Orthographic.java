/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.dataset.transform.horiz;

import dev.ucdm.core.api.AttributeContainer;
import dev.ucdm.core.constants.CF;
import dev.ucdm.dataset.geoloc.Projection;

/** Create a Orthographic Projection from the information in the Coordinate Transform Variable. */
public class Orthographic extends AbstractProjectionCT implements ProjectionBuilder {

  public String getTransformName() {
    return CF.ORTHOGRAPHIC;
  }

  public Projection makeProjection(AttributeContainer ctv, String geoCoordinateUnits) {
    double lon0 = ctv.findAttributeDouble(CF.LONGITUDE_OF_PROJECTION_ORIGIN, Double.NaN);
    double lat0 = ctv.findAttributeDouble(CF.LATITUDE_OF_PROJECTION_ORIGIN, Double.NaN);

    return new dev.ucdm.dataset.geoloc.projection.Orthographic(lat0, lon0);
  }

  public Class<? extends Projection> getProjectionClass() {
    return dev.ucdm.dataset.geoloc.projection.Orthographic.class;
  }
}
