/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.dataset.transform.horiz;

import dev.ucdm.core.api.AttributeContainer;
import dev.ucdm.core.constants.CF;
import dev.ucdm.dataset.geoloc.Projection;

/**
 * Create a RotatedPole Projection from the information in the Coordinate Transform Variable.
 * This is from CF. Grib is RotatedLatLon
 */
public class RotatedPole extends AbstractProjectionCT implements ProjectionBuilder {

  public String getTransformName() {
    return CF.ROTATED_LATITUDE_LONGITUDE;
  }

  public Projection makeProjection(AttributeContainer ctv, String geoCoordinateUnits) {
    double lon = ctv.findAttributeDouble(CF.GRID_NORTH_POLE_LONGITUDE, Double.NaN);
    double lat = ctv.findAttributeDouble(CF.GRID_NORTH_POLE_LATITUDE, Double.NaN);

    return new dev.ucdm.dataset.geoloc.projection.RotatedPole(lat, lon);
  }

  public Class<? extends Projection> getProjectionClass() {
    return dev.ucdm.dataset.geoloc.projection.RotatedPole.class;
  }

}
