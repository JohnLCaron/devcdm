/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.dataset.transform.horiz;

import dev.ucdm.core.api.AttributeContainer;
import dev.ucdm.core.constants.CDM;
import dev.ucdm.dataset.geoloc.Projection;
import dev.ucdm.dataset.geoloc.projection.UtmProjection;

/** Create a UTM Projection from the information in the Coordinate Transform Variable. */
public class UTM extends AbstractProjectionCT implements ProjectionBuilder {

  public String getTransformName() {
    return CDM.UniversalTransverseMercator;
  }

  public Projection makeProjection(AttributeContainer ctv, String geoCoordinateUnits) {
    double zoned = ctv.findAttributeDouble(UtmProjection.UTM_ZONE1, Double.NaN);
    if (Double.isNaN(zoned))
      zoned = ctv.findAttributeDouble(UtmProjection.UTM_ZONE2, Double.NaN);
    if (Double.isNaN(zoned))
      throw new IllegalArgumentException("No zone was specified");

    int zone = (int) zoned;
    boolean isNorth = zone > 0;
    zone = Math.abs(zone);

    double axis = ctv.findAttributeDouble("semimajor_axis", 0.0);
    double f = ctv.findAttributeDouble("inverse_flattening", 0.0);

    // double a, double f, int zone, boolean isNorth
    return (axis != 0.0) ? new UtmProjection(axis, f, zone, isNorth) : new UtmProjection(zone, isNorth);
  }

  public Class<? extends Projection> getProjectionClass() {
    return dev.ucdm.dataset.geoloc.projection.UtmProjection.class;
  }
}
