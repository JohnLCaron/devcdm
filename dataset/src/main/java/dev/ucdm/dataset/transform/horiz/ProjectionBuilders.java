/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.dataset.transform.horiz;

import dev.ucdm.core.api.AttributeContainer;
import dev.ucdm.core.api.Variable;
import dev.ucdm.core.constants.AxisType;
import dev.ucdm.core.constants.CDM;
import dev.ucdm.core.constants.CF;
import dev.ucdm.core.constants._Coordinate;
import dev.ucdm.dataset.api.CdmDatasetCS;
import dev.ucdm.dataset.api.SimpleUnit;
import dev.ucdm.dataset.api.CoordinateAxis;
import dev.ucdm.dataset.geoloc.Earth;

import java.util.List;

/** Static helper class for ProjectionBuilder's */
class ProjectionBuilders {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProjectionBuilders.class);

  /*
   * from CF: false_easting(false_northing):
   * The value added to all abscissa(ordinate) values in the rectangular coordinates for a map projection.
   * This value frequently is assigned to eliminate negative numbers.
   * Expressed in the unit of the coordinate variable identified by the standard name projection_x_coordinate
   * (projection_y_coordinate).
   */
  public static double getFalseEastingScaleFactor(CdmDatasetCS ds, AttributeContainer ctv) {
    String units = getGeoCoordinateUnits(ds, ctv);
    return getFalseEastingScaleFactor(units);
  }

  public static String getGeoCoordinateUnits(CdmDatasetCS ds, AttributeContainer ctv) {
    String units = ctv.findAttributeString(CDM.UNITS, null);
    if (units == null) {
      List<CoordinateAxis> axes = ds.getCoordinateAxes();
      for (CoordinateAxis axis : axes) {
        if (axis.getAxisType() == AxisType.GeoX) { // kludge - what if there's multiple ones?
          Variable v = axis.getOriginalVariable(); // why original variable ?
          units = (v == null) ? axis.getUnitsString() : v.getUnitsString();
          break;
        }
      }
      if (units == null) {
        Variable xvar = ds.getRootGroup().findVariableByAttribute(_Coordinate.AxisType, AxisType.GeoX.toString());
        if (xvar != null) {
          units = xvar.getUnitsString();
        }
      }
    }
    return units;
  }

  public static double getFalseEastingScaleFactor(String geoCoordinateUnits) {
    if (geoCoordinateUnits != null) {
      try {
        SimpleUnit unit = SimpleUnit.factoryWithExceptions(geoCoordinateUnits);
        return unit.convertTo(1.0, SimpleUnit.kmUnit);
      } catch (Exception e) {
        log.warn(geoCoordinateUnits + " not convertible to km");
      }
    }
    return 1.0;
  }

  /**
   * Get the earth radius in km from the attribute "earth_radius".
   * Normally this is in meters, convert to km if its > 10,000.
   * Use Earth.WGS84_EARTH_RADIUS_METERS as default.
   *
   * @param ctv coord transform variable
   * @return earth radius in km
   */
  static double getEarthRadiusInKm(AttributeContainer ctv) {
    double earth_radius = ctv.findAttributeDouble(CF.EARTH_RADIUS, Earth.WGS84_EARTH_RADIUS_METERS);
    if (earth_radius > 10000.0) {
      earth_radius *= .001;
    }
    return earth_radius;
  }

}
