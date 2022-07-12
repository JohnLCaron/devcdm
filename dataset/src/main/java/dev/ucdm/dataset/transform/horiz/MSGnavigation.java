/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.dataset.transform.horiz;

import dev.ucdm.core.api.AttributeContainer;
import dev.ucdm.core.constants.CF;
import dev.ucdm.dataset.geoloc.Projection;

/** MSGnavigation projection */
public class MSGnavigation extends AbstractProjectionCT implements ProjectionBuilder {
  public static final String GRID_MAPPING_NAME = dev.ucdm.dataset.geoloc.projection.sat.MSGnavigation.GRID_MAPPING_NAME;

  public String getTransformName() {
    return GRID_MAPPING_NAME;
  }

  public Projection makeProjection(AttributeContainer ctv, String geoCoordinateUnits) {

    double lon0 = ctv.findAttributeDouble(CF.LONGITUDE_OF_PROJECTION_ORIGIN, Double.NaN);
    double lat0 = ctv.findAttributeDouble(CF.LATITUDE_OF_PROJECTION_ORIGIN, Double.NaN);
    double minor_axis = ctv.findAttributeDouble(CF.SEMI_MINOR_AXIS, Double.NaN);
    double major_axis = ctv.findAttributeDouble(CF.SEMI_MAJOR_AXIS, Double.NaN);
    double height =
        ctv.findAttributeDouble(dev.ucdm.dataset.geoloc.projection.sat.MSGnavigation.HEIGHT_FROM_EARTH_CENTER, Double.NaN);
    double scale_x = ctv.findAttributeDouble(dev.ucdm.dataset.geoloc.projection.sat.MSGnavigation.SCALE_X, Double.NaN);
    double scale_y = ctv.findAttributeDouble(dev.ucdm.dataset.geoloc.projection.sat.MSGnavigation.SCALE_Y, Double.NaN);

    return new dev.ucdm.dataset.geoloc.projection.sat.MSGnavigation(lat0, lon0, major_axis, minor_axis, height, scale_x,
        scale_y);
  }

  public Class<? extends Projection> getProjectionClass() {
    return dev.ucdm.dataset.geoloc.projection.sat.MSGnavigation.class;
  }

}
