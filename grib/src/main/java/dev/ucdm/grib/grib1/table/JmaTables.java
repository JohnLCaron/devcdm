/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.grib.grib1.table;

import org.jetbrains.annotations.Nullable;

import dev.ucdm.grib.common.GribStatType;
import dev.ucdm.grib.common.util.GribNumbers;
import dev.ucdm.grib.coord.VertCoordUnit;
import dev.ucdm.grib.grib1.record.Grib1ParamLevel;
import dev.ucdm.grib.grib1.record.Grib1ParamTime;
import dev.ucdm.grib.grib1.record.Grib1SectionProductDefinition;

import java.util.HashMap;
import java.util.Map;

/**
 * Local tables for JMA (center 34)
 *
 * @author caron
 * @see "http://rda.ucar.edu/datasets/ds628.0/docs/JRA-55.grib_20130319.xlsx"
 * @since 1/27/2015
 */
public class JmaTables extends Grib1Customizer {
  private static Map<Integer, VertCoordUnit> levelTypesMap; // shared by all instances

  JmaTables(Grib1ParamLookup tables) {
    super(34, tables);
  }

  /////////////////// local time types
  @Override
  public Grib1ParamTime getParamTime(Grib1SectionProductDefinition pds) {
    int p1 = pds.getTimeValue1(); // octet 19
    int p2 = pds.getTimeValue2(); // octet 20
    int timeRangeIndicator = pds.getTimeRangeIndicator(); // octet 21
    int n = pds.getNincluded();

    int start;
    int end;
    int forecastTime = 0;
    boolean isInterval;

    switch (timeRangeIndicator) {
      /*
       * Monthly-diurnal (ds628.5/fcst_phy2m125_diurnal)
       * Average over the days of the month.
       * 128: Average of N forecast products with a valid time ranging between reference time + P1 and reference time +
       * P2;
       * products have reference times at Intervals of 24 hours, beginning at the given reference time.
       */
      case 128:
        isInterval = true;
        start = p1;
        end = p2;
        break;

      /*
       * 129: Temporal variance of N forecasts; each product has valid time ranging between reference time + P1 and
       * reference time + P2;
       * products have reference times at intervals of 24 hours, beginning at the given reference time;
       * unit of measurement is square of that in Code Table 2
       */

      /*
       * 131: Temporal variance of N forecasts; valid time of the first product ranges between R + P1 and R + P2,
       * where R is reference time given in octets 13 to 17, then subsequent products have valid time range at interval
       * of P2 - P1;
       * thus all N products cover continuous time span; products have reference times at intervals of P2 - P1,
       * beginning at the given reference time;
       * unit of measurement is square of that in Code Table 2
       */

      case 129:
      case 131:
        isInterval = true;
        start = p1;
        end = p2;
        break;

      /*
       * 130: Average of N forecast products; valid time of the first product ranges between R + P1 and R + P2, where R
       * is reference time given in octets 13 to 17,
       * then subsequent products have valid time range at interval of P2 - P1; thus all N products cover continuous
       * time span; products have reference times at
       * intervals of P2 - P1, beginning at the given reference time
       */
      case 130:
        isInterval = true;
        start = p1;
        end = (n > 0) ? p1 + n * (p2 - p1) : p2; // prob n >= 1
        break;

      /*
       * Temporal variance of N uninitialized analyses (P1 = 0) or instantaneous forecasts (P1 > 0);
       * each product has valid time at the reference time + P1;
       * products have reference times at intervals of P2, beginning at the given reference time;
       * unit of measurement is square of that in Code Table 2
       */
      case 132:
        forecastTime = p1;
        start = p1;
        end = (n > 0) ? p1 + (n - 1) * p2 : p1;
        isInterval = (n > 0);
        break;

      default:
        return super.getParamTime(pds);
    }

    return new Grib1ParamTime(this, timeRangeIndicator, isInterval, start, end, forecastTime);
  }

  @Override
  public String getTimeTypeName(int timeRangeIndicator) {
    return switch (timeRangeIndicator) {
      case 128 -> "Average over the days in the month";
      case 129 -> "Temporal variance of N forecasts at 24 hour intervals";
      case 130 -> "Forecast, 6-hour averaged, then one-month averaged";
      case 131 -> "Temporal variance of N forecasts at intervals of P1 - P2";
      case 132 -> "Temporal variance of N uninitialized analyses (P1 = 0) or instantaneous forecasts (P1 > 0)";
      default -> super.getTimeTypeName(timeRangeIndicator);
    };
  }

  @Override
  public GribStatType getStatType(int timeRangeIndicator) {
    return switch (timeRangeIndicator) {
      case 128, 130 -> GribStatType.Average;
      case 129, 131, 132 -> GribStatType.Variance;
      default -> super.getStatType(timeRangeIndicator);
    };
  }


  ///////////////////// levels
  @Override
  public Grib1ParamLevel getParamLevel(Grib1SectionProductDefinition pds) {
    int levelType = pds.getLevelType();
    int pds11 = pds.getLevelValue1();
    int pds12 = pds.getLevelValue2();
    int pds1112 = pds11 << 8 | pds12;

    return switch (levelType) {
      case 211, 212 -> new Grib1ParamLevel(this, levelType, GribNumbers.MISSING, GribNumbers.MISSING);
      case 100, 213 -> new Grib1ParamLevel(this, levelType, pds1112, GribNumbers.MISSING);
      default -> Grib1ParamLevel.factory(this, pds);
    };
  }

  protected VertCoordUnit getLevelType(int code) {
    if (levelTypesMap == null)
      makeLevelTypesMap();
    VertCoordUnit levelType = levelTypesMap.get(code);
    if (levelType != null)
      return levelType;
    return super.getLevelType(code);
  }

  private static void makeLevelTypesMap() {
    levelTypesMap = new HashMap<>(10);
    // (int code, String desc, String abbrev, String units, String datum, boolean isPositiveUp, boolean isLayer)
    levelTypesMap.put(100,
        new VertCoordUnit(100, "Isobaric Surface", "isobaric_surface_low", "hPa", null, false, false)); // 3D
    levelTypesMap.put(211, new VertCoordUnit(211, "Entire soil", "entire_soil", "", null, false, false));
    levelTypesMap.put(212,
        new VertCoordUnit(212, "The bottom of land surface model", "bottom_of_model", "", null, false, false));
    levelTypesMap.put(213, new VertCoordUnit(213, "Underground layer number of land surface model", "underground_layer",
        "layer", null, false, false)); // 3D
  }

  //////////////////// gen process
  private static Map<Integer, String> genProcessMap;

  @Override
  @Nullable
  public String getGeneratingProcessName(int genProcess) {
    if (genProcessMap == null)
      makeGenProcessMap();
    return genProcessMap.get(genProcess);
  }

  private static void makeGenProcessMap() {
    genProcessMap = new HashMap<>(100);
    genProcessMap.put(0, "Undefined (not to specify generating process)");
    genProcessMap.put(1, "Global Spectral Model (GSM8803_T63L16)");
    genProcessMap.put(2, "Global Spectral Model (GSM8903_T106L21)");
    genProcessMap.put(3, "Global Spectral Model (GSM9603_T213L30)");
    genProcessMap.put(4, "Global Spectral Model (GSM0103_T213L40) ");
    genProcessMap.put(21, "One-week EPS (GSM0103_T106L40)");
    genProcessMap.put(31, "Regional Spectral Model (RSM0103)");
    genProcessMap.put(32, "Mesoscale Model (MSM0103)");
    genProcessMap.put(51, "One-month EPS (GSM9603_T63L30)");
    genProcessMap.put(52, "One-month EPS (GSM9603_T106L40)");
    genProcessMap.put(53, "One-month EPS (GSM0603C_TL159L40)");
    genProcessMap.put(70, "Seasonal EPS (GSM0103_T63L40)");
    genProcessMap.put(71, "Seasonal EPS (GSM0502C_TL95L40)");
    genProcessMap.put(90, "Sea surface wind correction");
    genProcessMap.put(101, "NOAA-AVHRR analysis data");
    genProcessMap.put(102, "VISSR grid point data (for cloudiness, TBB etc.)");
    genProcessMap.put(103, "Long-wave radiation data");
    genProcessMap.put(104, "GMS data (sea surface temperature)");
    genProcessMap.put(105, "Snow/ice area data");
    genProcessMap.put(106, "Global solar radiation");
    genProcessMap.put(141, "Sea surface temperature analysis (average for dekad or ten days)");
    genProcessMap.put(142, "Sea surface temperature analysis");
    genProcessMap.put(143, "Ocean current analysis");
    genProcessMap.put(144, "Global ocean wave model");
    genProcessMap.put(150, "Regional ocean wave model");
    genProcessMap.put(200, "Volcaninc ash prediction");
    genProcessMap.put(201, "Japanese 55-year Reanalysis (JRA-55)");
  }

}
