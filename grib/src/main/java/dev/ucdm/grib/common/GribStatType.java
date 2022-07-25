/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.common;

import dev.ucdm.core.constants.CF;

import org.jetbrains.annotations.Nullable;

/**
 * Grib1 derived from Code table 5 : Time range indicator
 * Grib2 code table 4.10: Statistical process used to calculate the processed field from the field at each time
 * increment during the time range
 * These are the standard WMO tables only.
 *
 * @author caron
 * @since 1/17/12
 */
public enum GribStatType {
  Average, Accumulation, Maximum, Minimum, DifferenceFromEnd, RootMeanSquare, StandardDeviation, Covariance, DifferenceFromStart, Ratio, Variance;

  // (code table 4.10) Statistical process used to calculate the processed field from the field at each time increment
  // during the time range
  @Nullable
  public static GribStatType getStatTypeFromGrib2(int grib2StatCode) {
    return switch (grib2StatCode) {
      case 0 -> GribStatType.Average;
      case 1 -> GribStatType.Accumulation;
      case 2 -> GribStatType.Maximum;
      case 3 -> GribStatType.Minimum;
      case 4 -> GribStatType.DifferenceFromEnd;
      case 5 -> GribStatType.RootMeanSquare;
      case 6 -> GribStatType.StandardDeviation;
      case 7 -> GribStatType.Covariance;
      case 8 -> GribStatType.DifferenceFromStart;
      case 9 -> GribStatType.Ratio;
      default -> null;
    };
  }

  // what are these names ??
  public static int getStatTypeNumber(String name) {
    if (name.startsWith("Average"))
      return 0;
    if (name.startsWith("Accumulation"))
      return 1;
    if (name.startsWith("Maximum"))
      return 2;
    if (name.startsWith("Minimum"))
      return 3;
    if (name.startsWith("DifferenceFromEnd") || name.startsWith("Difference (Value at the end"))
      return 4;
    if (name.startsWith("Root"))
      return 5;
    if (name.startsWith("Standard"))
      return 6;
    if (name.startsWith("Covariance"))
      return 7;
    if (name.startsWith("DifferenceFromStart") || name.startsWith("Difference (Value at the start"))
      return 8;
    if (name.startsWith("Ratio"))
      return 9;
    if (name.startsWith("Variance"))
      return 10;
    return -1;
  }

  /**
   * Convert StatType to CF.CellMethods
   * 
   * @param stat the GRIB1 statistical type
   * @return equivalent CF, or null
   */
  @Nullable
  public static CF.CellMethods getCFCellMethod(GribStatType stat) {
    return switch (stat) {
      case Average -> CF.CellMethods.mean;
      case Accumulation -> CF.CellMethods.sum;
      case Covariance -> CF.CellMethods.variance;
      case Minimum -> CF.CellMethods.minimum;
      case Maximum -> CF.CellMethods.maximum;
      case StandardDeviation -> CF.CellMethods.standard_deviation;
      case Variance -> CF.CellMethods.variance;
      default -> null;
    };
  }

}
