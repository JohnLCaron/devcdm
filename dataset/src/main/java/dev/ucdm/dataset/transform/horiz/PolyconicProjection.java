/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.ucdm.dataset.transform.horiz;

import dev.ucdm.core.api.AttributeContainer;
import dev.ucdm.dataset.geoloc.Earth;
import dev.ucdm.dataset.geoloc.Projection;

/**
 * Polyconic Projection.
 * 
 * @author ghansham@sac.isro.gov.in 1/8/2012
 */
public class PolyconicProjection extends AbstractProjectionCT implements ProjectionBuilder {
  public static final String GRID_MAPPING_NAME = dev.ucdm.dataset.geoloc.projection.proj4.PolyconicProjection.GRID_MAPPING_NAME;

  public Projection makeProjection(AttributeContainer ctv, String geoCoordinateUnits) {

    double lon0 = ctv.findAttributeDouble("longitude_of_central_meridian", Double.NaN);
    double lat0 = ctv.findAttributeDouble("latitude_of_projection_origin", Double.NaN);

    double semi_major_axis = ctv.findAttributeDouble("semi_major_axis", Double.NaN);
    double semi_minor_axis = ctv.findAttributeDouble("semi_minor_axis", Double.NaN);
    double inverse_flattening = ctv.findAttributeDouble("inverse_flattening", 0.0);

    Projection proj;

    // check for ellipsoidal earth
    if (!Double.isNaN(semi_major_axis) && (!Double.isNaN(semi_minor_axis) || inverse_flattening != 0.0)) {
      Earth earth = new Earth(semi_major_axis, semi_minor_axis, inverse_flattening);
      proj = new dev.ucdm.dataset.geoloc.projection.proj4.PolyconicProjection(lat0, lon0, earth);
    } else {
      proj = new dev.ucdm.dataset.geoloc.projection.proj4.PolyconicProjection(lat0, lon0);
    }

    return proj;
  }

  public String getTransformName() {
    return GRID_MAPPING_NAME;
  }

  public Class<? extends Projection> getProjectionClass() {
    return dev.ucdm.dataset.geoloc.projection.proj4.PolyconicProjection.class;
  }
}
