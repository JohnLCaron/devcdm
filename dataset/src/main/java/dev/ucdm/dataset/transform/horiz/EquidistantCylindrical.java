/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.dataset.transform.horiz;

import dev.ucdm.core.api.AttributeContainer;
import dev.ucdm.core.constants.CDM;
import dev.ucdm.core.constants.CF;
import dev.ucdm.dataset.geoloc.Projection;

/** Create a Mercator Projection from the information in the Coordinate Transform Variable. */
public class EquidistantCylindrical extends AbstractProjectionCT implements ProjectionBuilder {

  @Override
  public String getTransformName() {
    return CDM.EquidistantCylindrical;
  }

  @Override
  public Projection makeProjection(AttributeContainer ctv, String geoCoordinateUnits) {
    double centralLat = ctv.findAttributeDouble(CDM.LatitudeOfCentralMeridian, Double.NaN);
    double centralLon = ctv.findAttributeDouble(CF.LONGITUDE_OF_CENTRAL_MERIDIAN, Double.NaN);
    double standardParellel = ctv.findAttributeDouble(CF.STANDARD_PARALLEL, Double.NaN);

    readStandardParams(ctv, geoCoordinateUnits);

    return new dev.ucdm.dataset.geoloc.projection.EquidistantCylindrical(centralLat, centralLon, standardParellel, false_easting, false_northing, earth_radius);
  }

  public Class<? extends Projection> getProjectionClass() {
    return dev.ucdm.dataset.geoloc.projection.EquidistantCylindrical.class;
  }
}
