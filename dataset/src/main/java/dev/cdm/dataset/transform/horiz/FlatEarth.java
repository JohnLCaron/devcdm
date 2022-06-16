/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.cdm.dataset.transform.horiz;

import dev.cdm.core.api.AttributeContainer;
import dev.cdm.core.constants.CF;
import dev.cdm.dataset.geoloc.Projection;

/** Create a "FlatEarth" Projection from the information in the Coordinate Transform Variable. */
public class FlatEarth extends AbstractProjectionCT implements ProjectionBuilder {

  public String getTransformName() {
    return "flat_earth";
  }

  public Projection makeProjection(AttributeContainer ctv, String geoCoordinateUnits) {
    double lon0 = ctv.findAttributeDouble(CF.LONGITUDE_OF_PROJECTION_ORIGIN, Double.NaN);
    double lat0 = ctv.findAttributeDouble(CF.LATITUDE_OF_PROJECTION_ORIGIN, Double.NaN);
    double rot = ctv.findAttributeDouble(dev.cdm.dataset.geoloc.projection.FlatEarth.ROTATIONANGLE, 0.0);
    double earth_radius = TransformBuilders.getEarthRadiusInKm(ctv);

    return new dev.cdm.dataset.geoloc.projection.FlatEarth(lat0, lon0, rot, earth_radius);
  }
}
