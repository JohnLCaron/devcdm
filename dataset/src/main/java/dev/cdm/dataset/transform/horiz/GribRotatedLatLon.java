/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.cdm.dataset.transform.horiz;

import dev.cdm.core.api.AttributeContainer;
import dev.cdm.core.constants.CDM;
import dev.cdm.dataset.geoloc.Projection;

/** Create a Grib Rotated LatLon Projection from the information in the Coordinate Transform Variable. */
public class GribRotatedLatLon extends AbstractProjectionCT implements ProjectionBuilder {

  public String getTransformName() {
    return CDM.GribRotatedLatLon;
  }

  public Projection makeProjection(AttributeContainer ctv, String geoCoordinateUnits) {
    double lon =
        ctv.findAttributeDouble(dev.cdm.dataset.geoloc.projection.RotatedLatLon.GRID_SOUTH_POLE_LONGITUDE, Double.NaN);
    double lat =
        ctv.findAttributeDouble(dev.cdm.dataset.geoloc.projection.RotatedLatLon.GRID_SOUTH_POLE_LATITUDE, Double.NaN);
    double angle =
        ctv.findAttributeDouble(dev.cdm.dataset.geoloc.projection.RotatedLatLon.GRID_SOUTH_POLE_ANGLE, Double.NaN);

    return new dev.cdm.dataset.geoloc.projection.RotatedLatLon(lat, lon, angle);
  }

}


