/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.dataset.transform.horiz;

import dev.ucdm.core.api.AttributeContainer;
import dev.ucdm.core.constants.CF;
import dev.ucdm.dataset.geoloc.Earth;
import dev.ucdm.dataset.geoloc.Projection;

/** Create a LambertConformalConic Projection from the information in the Coordinate Transform Variable. */
public class LambertConformalConic extends AbstractProjectionCT implements ProjectionBuilder {

  public String getTransformName() {
    return CF.LAMBERT_CONFORMAL_CONIC;
  }

  public Projection makeProjection(AttributeContainer ctv, String geoCoordinateUnits) {
    double[] pars = readAttributeDouble2(ctv.findAttribute(CF.STANDARD_PARALLEL));
    if (pars == null)
      return null;

    double lon0 = ctv.findAttributeDouble(CF.LONGITUDE_OF_CENTRAL_MERIDIAN, Double.NaN);
    double lat0 = ctv.findAttributeDouble(CF.LATITUDE_OF_PROJECTION_ORIGIN, Double.NaN);
    double false_easting = ctv.findAttributeDouble(CF.FALSE_EASTING, 0.0);
    double false_northing = ctv.findAttributeDouble(CF.FALSE_NORTHING, 0.0);

    if ((false_easting != 0.0) || (false_northing != 0.0)) {
      double scalef = ProjectionBuilders.getFalseEastingScaleFactor(geoCoordinateUnits);
      false_easting *= scalef;
      false_northing *= scalef;
    }

    double earth_radius = ProjectionBuilders.getEarthRadiusInKm(ctv);
    double semi_major_axis = ctv.findAttributeDouble(CF.SEMI_MAJOR_AXIS, Double.NaN);
    double semi_minor_axis = ctv.findAttributeDouble(CF.SEMI_MINOR_AXIS, Double.NaN);
    double inverse_flattening = ctv.findAttributeDouble(CF.INVERSE_FLATTENING, 0.0);

    Projection proj;

    // check for ellipsoidal earth
    if (!Double.isNaN(semi_major_axis) && (!Double.isNaN(semi_minor_axis) || inverse_flattening != 0.0)) {
      Earth earth = new Earth(semi_major_axis, semi_minor_axis, inverse_flattening);
      proj = new dev.ucdm.dataset.geoloc.projection.proj4.LambertConformalConicEllipse(lat0, lon0, pars[0], pars[1],
          false_easting, false_northing, earth);

    } else {
      proj = new dev.ucdm.dataset.geoloc.projection.LambertConformal(lat0, lon0, pars[0], pars[1], false_easting,
          false_northing, earth_radius);
    }

    return proj;
  }

  public Class<? extends Projection> getProjectionClass() {
    return dev.ucdm.dataset.geoloc.projection.LambertConformal.class;
  }
}
