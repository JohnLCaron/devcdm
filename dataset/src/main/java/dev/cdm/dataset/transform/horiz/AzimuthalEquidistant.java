/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.cdm.dataset.transform.horiz;

import dev.cdm.core.api.AttributeContainer;
import dev.cdm.core.constants.CF;
import dev.cdm.dataset.geoloc.Earth;
import dev.cdm.dataset.geoloc.Projection;
import dev.cdm.dataset.geoloc.projection.proj4.EquidistantAzimuthalProjection;

/** AzimuthalEquidistant Projection. */
public class AzimuthalEquidistant extends AbstractProjectionCT implements ProjectionBuilder {

  public String getTransformName() {
    return CF.AZIMUTHAL_EQUIDISTANT;
  }

  public Projection makeProjection(AttributeContainer ctv, String geoCoordinateUnits) {
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

    return new EquidistantAzimuthalProjection(lat0, lon0, false_easting, false_northing, earth);
  }

  public Class<? extends Projection> getProjectionClass() {
    return EquidistantAzimuthalProjection.class;
  }
}
