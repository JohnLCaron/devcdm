/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.cdm.dataset.transform.horiz;

import dev.cdm.core.api.AttributeContainer;
import dev.cdm.core.constants.CF;
import dev.cdm.dataset.geoloc.Projection;
import dev.cdm.dataset.geoloc.projection.sat.VerticalPerspectiveView;

/** VerticalPerspectiveView projection. */
public class VerticalPerspective extends AbstractProjectionCT implements ProjectionBuilder {

  public String getTransformName() {
    return CF.VERTICAL_PERSPECTIVE;
  }

  public Projection makeProjection(AttributeContainer ctv, String geoCoordinateUnits) {

    readStandardParams(ctv, geoCoordinateUnits);

    double distance = ctv.findAttributeDouble(CF.PERSPECTIVE_POINT_HEIGHT, Double.NaN);
    if (Double.isNaN(distance)) {
      distance = ctv.findAttributeDouble("height_above_earth", Double.NaN);
    }
    if (Double.isNaN(lon0) || Double.isNaN(lat0) || Double.isNaN(distance))
      throw new IllegalArgumentException("Vertical Perspective must have: " + CF.LONGITUDE_OF_PROJECTION_ORIGIN + ", "
          + CF.LATITUDE_OF_PROJECTION_ORIGIN + ", and " + CF.PERSPECTIVE_POINT_HEIGHT + "(or height_above_earth) "
          + "attributes");

    // We assume distance comes in 'm' (CF-compliant) and we pass in as 'km'
    return new VerticalPerspectiveView(lat0, lon0, earth_radius, distance / 1000., false_easting, false_northing);
  }

  public Class<? extends Projection> getProjectionClass() {
    return VerticalPerspectiveView.class;
  }
}
