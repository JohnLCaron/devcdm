/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.cdm.dataset.transform.horiz;

import dev.cdm.core.api.AttributeContainer;
import dev.cdm.core.constants.CF;
import dev.cdm.dataset.geoloc.Projection;
import dev.cdm.dataset.geoloc.projection.proj4.AlbersEqualAreaEllipse;

/** Create a AlbersEqualArea Projection from the information in the Coordinate Transform Variable. */
public class AlbersEqualArea extends AbstractProjectionCT implements ProjectionBuilder {

  public String getTransformName() {
    return CF.ALBERS_CONICAL_EQUAL_AREA;
  }

  public Projection makeProjection(AttributeContainer ctv, String geoCoordinateUnits) {
    double[] pars = readAttributeDouble2(ctv.findAttribute(CF.STANDARD_PARALLEL));
    if (pars == null)
      return null;

    readStandardParams(ctv, geoCoordinateUnits);

    Projection proj;

    if (earth != null) {
      proj = new AlbersEqualAreaEllipse(lat0, lon0, pars[0], pars[1], false_easting, false_northing, earth);

    } else {
      proj = new dev.cdm.dataset.geoloc.projection.AlbersEqualArea(lat0, lon0, pars[0], pars[1], false_easting,
          false_northing, earth_radius);
    }

    return proj;
  }
}