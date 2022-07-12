/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.dataset.transform.horiz;

import dev.ucdm.core.api.AttributeContainer;
import dev.ucdm.core.constants.CF;
import dev.ucdm.dataset.geoloc.Projection;

/** Create a Sinusoidal Projection from the information in the Coordinate Transform Variable. */
public class Sinusoidal extends AbstractProjectionCT implements ProjectionBuilder {

  public String getTransformName() {
    return CF.SINUSOIDAL;
  }

  public Projection makeProjection(AttributeContainer ctv, String geoCoordinateUnits) {
    double centralMeridian = ctv.findAttributeDouble(CF.LONGITUDE_OF_CENTRAL_MERIDIAN, Double.NaN);
    double false_easting = ctv.findAttributeDouble(CF.FALSE_EASTING, 0.0);
    double false_northing = ctv.findAttributeDouble(CF.FALSE_NORTHING, 0.0);
    double earth_radius = ProjectionBuilders.getEarthRadiusInKm(ctv);

    if ((false_easting != 0.0) || (false_northing != 0.0)) {
      double scalef = ProjectionBuilders.getFalseEastingScaleFactor(geoCoordinateUnits);
      false_easting *= scalef;
      false_northing *= scalef;
    }

    return new dev.ucdm.dataset.geoloc.projection.Sinusoidal(centralMeridian, false_easting, false_northing, earth_radius);
  }

  public Class<? extends Projection> getProjectionClass() {
    return dev.ucdm.dataset.geoloc.projection.Sinusoidal.class;
  }
}
