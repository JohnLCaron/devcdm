/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.cdm.dataset.transform.horiz;

import dev.cdm.core.api.AttributeContainer;
import dev.cdm.core.constants.CF;
import dev.cdm.dataset.geoloc.Earth;
import dev.cdm.dataset.geoloc.Projection;
import dev.cdm.dataset.geoloc.projection.proj4.CylindricalEqualAreaProjection;

/** Lambert Cylindrical Equal Area Projection */
public class LambertCylindricalEqualArea extends AbstractProjectionCT implements ProjectionBuilder {

  public String getTransformName() {
    return CF.LAMBERT_CYLINDRICAL_EQUAL_AREA;
  }

  public Projection makeProjection(AttributeContainer ctv, String geoCoordinateUnits) {
    double par = ctv.findAttributeDouble(CF.STANDARD_PARALLEL, Double.NaN);

    readStandardParams(ctv, geoCoordinateUnits);

    // create spherical Earth obj if not created by readStandardParams w radii, flattening
    if (earth == null) {
      if (earth_radius > 0.) {
        // Earth radius obtained in readStandardParams is in km, but Earth object wants m
        earth = new Earth(earth_radius * 1000.);
      } else {
        earth = new Earth();
      }
    }

    return new CylindricalEqualAreaProjection(lon0, par, false_easting, false_northing, earth);
  }
}
