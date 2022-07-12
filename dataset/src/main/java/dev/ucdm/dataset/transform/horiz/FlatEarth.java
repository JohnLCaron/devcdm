/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.dataset.transform.horiz;

import dev.ucdm.core.api.AttributeContainer;
import dev.ucdm.core.constants.CDM;
import dev.ucdm.core.constants.CF;
import dev.ucdm.dataset.geoloc.Projection;

/** Create a "FlatEarth" Projection from the information in the Coordinate Transform Variable. */
public class FlatEarth extends AbstractProjectionCT implements ProjectionBuilder {

  public String getTransformName() {
    return CDM.FlatEarth;
  }

  public Projection makeProjection(AttributeContainer ctv, String geoCoordinateUnits) {
    double lon0 = ctv.findAttributeDouble(CF.LONGITUDE_OF_PROJECTION_ORIGIN, Double.NaN);
    double lat0 = ctv.findAttributeDouble(CF.LATITUDE_OF_PROJECTION_ORIGIN, Double.NaN);
    double rot = ctv.findAttributeDouble(dev.ucdm.dataset.geoloc.projection.FlatEarth.ROTATIONANGLE, 0.0);
    double earth_radius = ProjectionBuilders.getEarthRadiusInKm(ctv);

    return new dev.ucdm.dataset.geoloc.projection.FlatEarth(lat0, lon0, rot, earth_radius);
  }

  public Class<? extends Projection> getProjectionClass() {
    return dev.ucdm.dataset.geoloc.projection.FlatEarth.class;
  }
}
