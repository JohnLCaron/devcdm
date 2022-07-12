/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.dataset.transform.horiz;

import dev.ucdm.core.api.AttributeContainer;
import dev.ucdm.core.constants.CDM;
import dev.ucdm.dataset.geoloc.Projection;

/** Create a Grib Rotated LatLon Projection from the information in the Coordinate Transform Variable. */
public class GribRotatedLatLon extends AbstractProjectionCT implements ProjectionBuilder {

  public String getTransformName() {
    return CDM.GribRotatedLatLon;
  }

  public Projection makeProjection(AttributeContainer ctv, String geoCoordinateUnits) {
    double lon =
        ctv.findAttributeDouble(dev.ucdm.dataset.geoloc.projection.GribRotatedLatLon.GRID_SOUTH_POLE_LONGITUDE, Double.NaN);
    double lat =
        ctv.findAttributeDouble(dev.ucdm.dataset.geoloc.projection.GribRotatedLatLon.GRID_SOUTH_POLE_LATITUDE, Double.NaN);
    double angle =
        ctv.findAttributeDouble(dev.ucdm.dataset.geoloc.projection.GribRotatedLatLon.GRID_SOUTH_POLE_ANGLE, Double.NaN);

    return new dev.ucdm.dataset.geoloc.projection.GribRotatedLatLon(lat, lon, angle);
  }

  public Class<? extends Projection> getProjectionClass() {
    return dev.ucdm.dataset.geoloc.projection.GribRotatedLatLon.class;
  }

}


