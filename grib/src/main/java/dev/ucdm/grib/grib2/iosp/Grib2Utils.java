/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.grib2.iosp;

import dev.cdm.core.calendar.CalendarPeriod;
import dev.cdm.core.constants.AxisType;
import dev.cdm.core.util.StringUtil2;
import dev.ucdm.grib.grib2.record.Grib2Pds;
import dev.ucdm.grib.grib2.record.Grib2Record;
import dev.ucdm.grib.grib2.table.WmoParamTable;
import dev.ucdm.grib.common.GribTables;

import javax.annotation.Nullable;

/**
 * Static utilities for Grib-2
 *
 * @author caron
 * @since 3/29/11
 */
public class Grib2Utils {

  public static String clean(String s) {
    StringBuilder sb = new StringBuilder(s);
    StringUtil2.replace(sb, "/. ", "-p_");
    StringUtil2.removeAll(sb, "(),;");
    char c = sb.charAt(0);
    if (Character.isLetter(c)) {
      if (Character.isLowerCase(c))
        sb.setCharAt(0, Character.toUpperCase(c));
    } else {
      sb.insert(0, 'N');
    }

    return sb.toString().trim();
  }

  public static String cleanupHeader(byte[] raw) {
    String result = StringUtil2.cleanup(raw);
    int pos = result.indexOf("data");
    if (pos > 0)
      result = result.substring(pos);
    return result;
  }

  public static String getVariableName(Grib2Record gr) {
    GribTables.Parameter p = WmoParamTable.getParameter(gr.getDiscipline(), gr.getPDS().getParameterCategory(),
        gr.getPDS().getParameterNumber());
    String s = (p == null) ? null : p.getName();
    if (s == null)
      s = "U" + gr.getDiscipline() + "-" + gr.getPDS().getParameterCategory() + "-" + gr.getPDS().getParameterNumber();
    return s;
  }

  @Nullable
  public static CalendarPeriod getCalendarPeriod(int timeUnit) {

    return switch (timeUnit) { // code table 4.4
      case 0 -> CalendarPeriod.of(1, CalendarPeriod.Field.Minute);
      case 1 -> CalendarPeriod.of(1, CalendarPeriod.Field.Hour);
      case 2 -> CalendarPeriod.of(1, CalendarPeriod.Field.Day);
      case 3 -> CalendarPeriod.of(1, CalendarPeriod.Field.Month);
      case 4 -> CalendarPeriod.of(1, CalendarPeriod.Field.Year);
      case 5 -> CalendarPeriod.of(10, CalendarPeriod.Field.Year);
      case 6 -> CalendarPeriod.of(30, CalendarPeriod.Field.Year);
      case 7 -> CalendarPeriod.of(100, CalendarPeriod.Field.Year);
      case 10 -> CalendarPeriod.of(3, CalendarPeriod.Field.Hour);
      case 11 -> CalendarPeriod.of(6, CalendarPeriod.Field.Hour);
      case 12 -> CalendarPeriod.of(12, CalendarPeriod.Field.Hour);
      case 13 -> CalendarPeriod.of(1, CalendarPeriod.Field.Second);
      default -> null;
    };
  }

  /**
   * Check to see if this pds is a layer variable
   * 
   * @param pds record to check
   * @return true if a layer
   */
  public static boolean isLayer(Grib2Pds pds) {
    return pds.getLevelType2() != 255 && pds.getLevelType2() != 0;
  }

  public static boolean isLatLon(int gridTemplate, int center) {
    return ((gridTemplate < 4) || ((gridTemplate >= 40) && (gridTemplate < 44)));
  }

  //////////////////////////////////////////////////////////////////////////////////
  // ad-hoc Conventions for identifying curvilinear coordinates.
  // TODO possibly move to Customizer

  // check if grid template is "Curvilinear Orthogonal", (NCEP 204) methods below only used when thats true
  public static boolean isCurvilinearOrthogonal(int gridTemplate, int center) {
    return ((center == 7) && (gridTemplate == 204));
  }

  // isLatLon2D is true, check parameter to see if its a 2D lat/lon coordinate
  @Nullable
  public static LatLon2DCoord getLatLon2DcoordType(int discipline, int category, int parameter) {
    if ((discipline != 0) || (category != 2) || (parameter < 198 || parameter > 203))
      return null;
    return switch (parameter) {
      case 198 -> LatLon2DCoord.U_Latitude;
      case 199 -> LatLon2DCoord.U_Longitude;
      case 200 -> LatLon2DCoord.V_Latitude;
      case 201 -> LatLon2DCoord.V_Longitude;
      case 202 -> LatLon2DCoord.P_Latitude;
      case 203 -> LatLon2DCoord.P_Longitude;
      default -> null;
    };
  }

  public enum LatLonCoordType {
    U, V, P
  }
  public enum LatLon2DCoord {
    U_Latitude, U_Longitude, V_Latitude, V_Longitude, P_Latitude, P_Longitude;

    public LatLonCoordType getCoordType() {
      return switch (this) {
        case U_Latitude, U_Longitude -> LatLonCoordType.U;
        case V_Latitude, V_Longitude -> LatLonCoordType.V;
        case P_Latitude, P_Longitude -> LatLonCoordType.P;
      };
    }

    public AxisType getAxisType() {
      return this.name().contains("Latitude") ? AxisType.Lat : AxisType.Lon;
    }
  }

  /**
   * This looks for snippets in the variable name/desc as to whether it wants U, V, or P 2D coordinates.
   *
   * @param desc variable name/desc
   * @return U, V, or P for normal variables, null for the coordinates themselves
   */
  public static LatLonCoordType getLatLon2DcoordType(String desc) {
    LatLonCoordType type;
    if (desc.contains("u-component"))
      type = LatLonCoordType.U;
    else if (desc.contains("v-component"))
      type = LatLonCoordType.V;
    else if (desc.contains("Latitude of") || desc.contains("Longitude of"))
      type = null;
    else
      type = LatLonCoordType.P;
    return type;
  }

}
