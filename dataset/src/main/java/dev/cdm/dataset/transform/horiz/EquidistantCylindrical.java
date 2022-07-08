/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.cdm.dataset.transform.horiz;

import dev.cdm.core.api.AttributeContainer;
import dev.cdm.core.constants.CDM;
import dev.cdm.core.constants.CF;
import dev.cdm.dataset.geoloc.Projection;
import dev.cdm.dataset.geoloc.projection.proj4.EquidistantAzimuthalProjection;

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

    return new dev.cdm.dataset.geoloc.projection.EquidistantCylindrical(centralLat, centralLon, standardParellel, false_easting, false_northing, earth_radius);
  }

  public Class<? extends Projection> getProjectionClass() {
    return dev.cdm.dataset.geoloc.projection.EquidistantCylindrical.class;
  }
}
