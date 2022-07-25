/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.grib2.table;

import com.google.common.collect.ImmutableList;
import dev.ucdm.core.calendar.CalendarDate;
import dev.ucdm.core.calendar.CalendarPeriod;
import dev.ucdm.grib.collection.VariableIndex;
import dev.ucdm.grib.common.GribStatType;
import dev.ucdm.grib.common.GribTables;
import dev.ucdm.grib.coord.TimeCoordIntvDateValue;
import dev.ucdm.grib.coord.TimeCoordIntvValue;
import dev.ucdm.grib.coord.VertCoordUnit;
import dev.ucdm.grib.common.GribConfig;
import dev.ucdm.grib.grib2.record.Grib2Pds;
import dev.ucdm.grib.grib2.record.Grib2Record;
import dev.ucdm.grib.grib2.record.Grib2SectionIdentification;
import dev.ucdm.grib.grib2.iosp.Grib2Utils;
import dev.ucdm.grib.grib2.table.WmoCodeFlagTables.WmoTable;
import dev.ucdm.grib.common.util.GribNumbers;
import dev.ucdm.grib.common.util.GribUtils;
import dev.ucdm.grib.common.wmo.CommonCodeTable;

import org.jetbrains.annotations.Nullable;
import dev.ucdm.array.Immutable;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Grib 2 Tables - allows local overrides and augmentation of WMO tables.
 * This class serves the standard WMO tables, local tables are subclasses that override.
 * Methods are placed here because they may be overrided by local Tables.
 *
 * Tables include code, flag and parameter tables.
 *
 * @author caron
 * @since 4/3/11
 */
@Immutable
public class Grib2Tables implements GribTables, GribConfig.TimeUnitConverter {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib2Tables.class);
  private static final Map<Grib2TablesId, Grib2Tables> tables = new HashMap<>();
  private static Grib2Tables wmoStandardTable;

  public static Grib2Tables factory(Grib2Record gr) {
    Grib2SectionIdentification ids = gr.getId();
    Grib2Pds pds = gr.getPDS();
    return factory(ids.getCenter_id(), ids.getSubcenter_id(), ids.getMaster_table_version(),
        ids.getLocal_table_version(), pds.getGenProcessId());
  }

  // Lazy instantiation.
  public static Grib2Tables factory(int center, int subCenter, int masterVersion, int localVersion, int genProcessId) {
    Grib2TablesId id = new Grib2TablesId(center, subCenter, masterVersion, localVersion, genProcessId);
    Grib2Tables cust = tables.get(id);
    if (cust != null)
      return cust;

    // note that we match on id, so same Grib2Customizer may be mapped to multiple id's (eg match on -1)
    Grib2TableConfig config = Grib2TableConfig.matchTable(id);
    cust = build(config);

    tables.put(id, cust);
    return cust;
  }

  private static Grib2Tables build(Grib2TableConfig config) {
    switch (config.getType()) {
      case cfsr:
        return new CfsrLocalTables(config);
      case eccodes:
        return new EccodesLocalTables(config);
      case gempak:
        return new GempakLocalTables(config); // not used
      case gsd:
        return new FslHrrrLocalTables(config);
      case kma:
        return new KmaLocalTables(config);
      case ncep:
        return new NcepLocalTables(config);
      case ndfd:
        return new NdfdLocalTables(config);
      case mrms:
        return new MrmsLocalTables(config);
      case nwsDev:
        return new NwsMetDevTables(config);
      default:
        if (wmoStandardTable == null)
          wmoStandardTable = new Grib2Tables(config);
        return wmoStandardTable;
    }
  }

  public static int makeParamId(int discipline, int category, int number) {
    return (discipline << 16) + (category << 8) + number;
  }

  public static int[] unmakeParamId(int code) {
    int number = code & 255;
    code = code >> 8;
    int category = code & 255;
    int discipline = code >> 8;
    return new int[] {discipline, category, number};
  }

  public static String makeParamCode(int discipline, int category, int number) {
    return String.format("%d-%d-%d", discipline, category, number);
  }

  public static String makeParamCode(int code) {
    int number = code & 255;
    code = code >> 8;
    int category = code & 255;
    int discipline = code >> 8;
    return String.format("%d-%d-%d", discipline, category, number);
  }

  public static boolean isLocal(Parameter p) {
    return isLocal(p.getDiscipline(), p.getCategory(), p.getNumber());
  }

  public static boolean isLocal(int discipline, int category, int number) {
    return ((discipline <= 191) && (category <= 191) && (number <= 191));
  }

  public static boolean isLocal(int code) {
    int[] uncode = unmakeParamId(code);
    return isLocal(uncode[0], uncode[1], uncode[2]);
  }

  public static List<Grib2Tables> getAllRegisteredTables() {
    ImmutableList.Builder<Grib2Tables> builder = ImmutableList.builder();
    for (Grib2TableConfig config : Grib2TableConfig.getTables()) {
      builder.add(build(config));
    }
    return builder.build();
  }

  ///////////////////////////////////////////////////////////////
  protected final Grib2TableConfig config;
  private boolean timeUnitWarnWasSent;

  protected Grib2Tables(Grib2TableConfig config) {
    this.config = config;
  }

  public String getName() {
    return config.getName();
  }

  public int getCenterId() {
    return config.getConfigId().center();
  }

  public String getPath() {
    return config.getPath();
  }

  public Grib2TablesId getConfigId() {
    return config.getConfigId();
  }

  public Grib2TablesId.Type getType() {
    return config.getType();
  }

  public String getVariableName(Grib2Record gr) {
    return getVariableName(gr.getDiscipline(), gr.getPDS().getParameterCategory(), gr.getPDS().getParameterNumber());
  }

  /**
   * Make a IOSP Variable name, using the Parameter name is available, otherwise a synthesized name.
   */
  public String getVariableName(int discipline, int category, int parameter) {
    String s = WmoParamTable.getParameterName(discipline, category, parameter);
    if (s == null)
      s = "U" + discipline + "-" + category + "-" + parameter;
    return s;
  }

  /////////////////////////////////////////////////////////////////////////////////////
  // Parameter interface (table 4.2.x)

  @Nullable
  public GribTables.Parameter getParameter(Grib2Record gr) {
    return getParameter(gr.getDiscipline(), gr.getPDS().getParameterCategory(), gr.getPDS().getParameterNumber());
  }

  @Nullable
  public GribTables.Parameter getParameter(int discipline, Grib2Pds pds) {
    return getParameter(discipline, pds.getParameterCategory(), pds.getParameterNumber());
  }

  @Nullable
  GribTables.Parameter getParameter(int discipline, int category, int number) {
    return WmoParamTable.getParameter(discipline, category, number);
  }

  @Nullable
  public GribTables.Parameter getParameter(VariableIndex vindex) {
    return getParameter(vindex.getDiscipline(), vindex.getCategory(), vindex.getParameter());
  }

  /////////////////////////////////////////////////////////////////////////////////////
  // Code interface (tables other than 4.2.x)

  @Nullable
  public String getCodeTableValue(String tableName, int code) {
    WmoCodeTable codeTable = WmoCodeFlagTables.getInstance().getCodeTable(tableName);
    Grib2CodeTableInterface.Entry entry = (codeTable == null) ? null : codeTable.getEntry(code);
    return (entry == null) ? null : entry.getName();
  }

  @Override
  @Nullable
  public String getSubCenterName(int center_id, int subcenter_id) {
    return CommonCodeTable.getSubCenterName(center_id, subcenter_id);
  }

  @Nullable
  public String getGeneratingProcessName(int genProcess) {
    return null;
  }

  @Nullable
  public String getGeneratingProcessTypeName(int genProcess) {
    return getCodeTableValue("4.3", genProcess);
  }

  @Nullable
  public String getCategory(int discipline, int category) {
    WmoCodeTable catTable = WmoCodeFlagTables.getInstance().getCodeTable("4.1." + discipline);
    Grib2CodeTableInterface.Entry entry = (catTable == null) ? null : catTable.getEntry(category);
    return (entry == null) ? null : entry.getName();
  }

  public String getStatisticName(int id) {
    String result = getCodeTableValue("4.10", id); // WMO
    if (result == null)
      result = getStatisticNameShort(id);
    return result;
  }

  public String getStatisticNameShort(int id) {
    GribStatType stat = GribStatType.getStatTypeFromGrib2(id);
    return (stat == null) ? "UnknownStatType-" + id : stat.toString();
  }

  @Override
  @Nullable
  public GribStatType getStatType(int grib2StatCode) {
    return GribStatType.getStatTypeFromGrib2(grib2StatCode);
  }

  /*
   * Code Table Code table 4.7 - Derived forecast (4.7)
   * 0: Unweighted mean of all members
   * 1: Weighted mean of all members
   * 2: Standard deviation with respect to cluster mean
   * 3: Standard deviation with respect to cluster mean, normalized
   * 4: Spread of all members
   * 5: Large anomaly index of all members
   * 6: Unweighted mean of the cluster members
   * 7: Interquartile range (range between the 25th and 75th quantile)
   * 8: Minimum of all ensemble members
   * 9: Maximum of all ensemble members
   * -1: Reserved
   * -1: Reserved for local use
   * 255: Missing
   */
  public String getProbabilityNameShort(int id) {
    return switch (id) {
      case 0 -> "unweightedMean";
      case 1 -> "weightedMean";
      case 2 -> "stdDev";
      case 3 -> "stdDevNormalized";
      case 4 -> "spread";
      case 5 -> "largeAnomalyIndex";
      case 6 -> "unweightedMeanCluster";
      case 7 -> "interquartileRange";
      case 8 -> "minimumEnsemble";
      case 9 -> "maximumEnsemble";
      default -> "UnknownProbType" + id;
    };
  }

  ///////////////////////////////////////////////////////////////////////////////////////
  // Vertical Units

  /**
   * Unit of vertical coordinate.
   * from Grib2 code table 4.5.
   * Only levels with units get a dimension added
   *
   * @param code code from table 4.5
   * @return level unit, default is empty unit string
   */
  @Override
  public VertCoordUnit getVertUnit(int code) {
    // VertCoordType(int code, String desc, String abbrev, String units, String datum, boolean isPositiveUp, boolean
    // isLayer)
    return switch (code) {
      case 11, 12, 117, 237, 238 -> new VertCoordUnit(code, "m", null, true);
      case 20 -> new VertCoordUnit(code, "K", null, false);
      case 100, 119 -> new VertCoordUnit(code, "Pa", null, false);
      case 102 -> new VertCoordUnit(code, "m", "mean sea level", true);
      case 103 -> new VertCoordUnit(code, "m", "ground", true);
      case 104, 105 -> new VertCoordUnit(code, "sigma", null, false); // positive?

      case 106 -> new VertCoordUnit(code, "m", "land surface", false);
      case 107 -> new VertCoordUnit(code, "K", null, true); // positive?

      case 108 -> new VertCoordUnit(code, "Pa", "ground", true);
      case 109 -> new VertCoordUnit(code, "K m2 kg-1 s-1", null, true); // positive?

      case 111 -> new VertCoordUnit(code, "eta", null, false);
      case 114 -> new VertCoordUnit(code, "numeric", null, false);// ??

      case 160 -> new VertCoordUnit(code, "m", "sea level", false);
      case 161 -> new VertCoordUnit(code, "m", "water surface", false);

      // NCEP specific
      case 235 -> new VertCoordUnit(code, "0.1 C", null, true);
      default -> new VertCoordUnit(code, null, null, true);
    };
  }

  public boolean isLevelUsed(int code) {
    VertCoordUnit vunit = getVertUnit(code);
    return vunit.isVerticalCoordinate();
  }

  @Nullable
  public String getLevelName(int id) {
    return getCodeTableValue("4.5", id);
  }

  public boolean isLayer(Grib2Pds pds) {
    return !(pds.getLevelType2() == 255 || pds.getLevelType2() == 0);
  }

  // Table 4.5
  @Override
  public String getLevelNameShort(int id) {

    return switch (id) {
      case 1 -> "surface";
      case 2 -> "cloud_base";
      case 3 -> "cloud_tops";
      case 4 -> "zeroDegC_isotherm";
      case 5 -> "adiabatic_condensation_lifted";
      case 6 -> "maximum_wind";
      case 7 -> "tropopause";
      case 8 -> "atmosphere_top";
      case 9 -> "sea_bottom";
      case 10 -> "entire_atmosphere";
      case 11 -> "cumulonimbus_base";
      case 12 -> "cumulonimbus_top";
      case 20 -> "isotherm";
      case 100 -> "isobaric";
      case 101 -> "msl";
      case 102 -> "altitude_above_msl";
      case 103 -> "height_above_ground";
      case 104 -> "sigma";
      case 105 -> "hybrid";
      case 106 -> "depth_below_surface";
      case 107 -> "isentrope";
      case 108 -> "pressure_difference";
      case 109 -> "potential_vorticity_surface";
      case 111 -> "eta";
      case 113 -> "log_hybrid";
      case 117 -> "mixed_layer_depth";
      case 118 -> "hybrid_height";
      case 119 -> "hybrid_pressure";
      case 120 -> "pressure_thickness";
      case 160 -> "depth_below_sea";
      case GribNumbers.UNDEFINED -> "none";
      default -> "UnknownLevelType-" + id;
    };
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  // Time utilities

  private GribConfig.TimeUnitConverter timeUnitConverter;

  public void setTimeUnitConverter(GribConfig.TimeUnitConverter timeUnitConverter) {
    if (this.timeUnitConverter != null)
      throw new RuntimeException("Cant modify timeUnitConverter once its been set");
    this.timeUnitConverter = timeUnitConverter;
  }

  @Override
  public int convertTimeUnit(int timeUnit) {
    if (timeUnitConverter == null)
      return timeUnit;
    return timeUnitConverter.convertTimeUnit(timeUnit);
  }

  // The Forecast Date uses End when its a Interval
  @Nullable
  public CalendarDate getForecastDate(Grib2Record gr) {
    Grib2Pds pds = gr.getPDS();
    if (pds.isTimeInterval()) {
      TimeCoordIntvDateValue intv = getForecastTimeInterval(gr);
      return intv == null ? null : intv.end();

    } else {
      int val = pds.getForecastTime();
      CalendarPeriod period = Grib2Utils.getCalendarPeriod(convertTimeUnit(pds.getTimeUnit()));
      if (period == null)
        return null;
      return gr.getReferenceDate().add(val, period);
    }
  }

  // The Forecast Date always uses Beg: RefDate + forecastTime
  @Nullable
  public CalendarDate getForecastDateBeg(Grib2Record gr) {
    Grib2Pds pds = gr.getPDS();
    int val = pds.getForecastTime();
    CalendarPeriod period = Grib2Utils.getCalendarPeriod(convertTimeUnit(pds.getTimeUnit()));
    if (period == null)
      return null;
    return gr.getReferenceDate().add(val, period);
  }

  public TimeCoordIntvDateValue getForecastTimeInterval(Grib2Record gr) {
    if (!gr.getPDS().isTimeInterval())
      return null;
    Grib2Pds.PdsInterval pdsIntv = (Grib2Pds.PdsInterval) gr.getPDS();

    // the time "range" in units of pdsIntv timeUnits
    TimeIntervalAndUnits intvu = getForecastTimeInterval(pdsIntv);

    // convert time "range" to units of pds timeUnits
    int timeUnitOrg = gr.getPDS().getTimeUnit();
    CalendarPeriod wantPeriod = Grib2Utils.getCalendarPeriod(convertTimeUnit(timeUnitOrg));
    if (wantPeriod == null) {
      return null;
    }
    CalendarPeriod havePeriod = Grib2Utils.getCalendarPeriod(convertTimeUnit(intvu.timeUnitIntv));
    double fac2 = intvu.timeRange * GribUtils.getConvertFactor(havePeriod, wantPeriod);
    CalendarPeriod period = wantPeriod.withValue((int) fac2 * wantPeriod.getValue());

    // note from Arthur Taylor (degrib):
    /*
     * If there was a range I used:
     *
     * End of interval (EI) = (bytes 36-42 show an "end of overall time interval")
     * C1) End of Interval = EI;
     * Begin of Interval = EI - range
     *
     * and if there was no interval then I used:
     * C2) End of Interval = Begin of Interval = Ref + ForeT.
     */
    if (pdsIntv.hasUnknownIntervalEnd()) { // all values were set to zero: guessing!
      return new TimeCoordIntvDateValue(gr.getReferenceDate(), period);
    } else {
      return new TimeCoordIntvDateValue(period, pdsIntv.getIntervalTimeEnd());
    }
  }

  /**
   * Get interval size in units of hours.
   * Only use in GribVariable to decide on variable identity when intvMerge = false.
   * Move to GribVariable?
   * 
   * @param pds must be a Grib2Pds.PdsInterval
   * @return interval size in units of hours
   */
  public double getForecastTimeIntervalSizeInHours(Grib2Pds pds) {
    Grib2Pds.PdsInterval pdsIntv = (Grib2Pds.PdsInterval) pds;

    // calculate total "range"
    TimeIntervalAndUnits intvu = getForecastTimeInterval(pdsIntv);

    // convert that range to units of hours.
    CalendarPeriod timeUnitPeriod = Grib2Utils.getCalendarPeriod(convertTimeUnit(intvu.timeUnitIntv));
    if (timeUnitPeriod == null)
      return GribNumbers.UNDEFINEDD;
    if (timeUnitPeriod.equals(CalendarPeriod.Hour))
      return intvu.timeRange;

    double fac;
    if (timeUnitPeriod.getField() == CalendarPeriod.Field.Month) {
      fac = 30.0 * 24.0; // nominal hours in a month
    } else if (timeUnitPeriod.getField() == CalendarPeriod.Field.Year) {
      fac = 365.0 * 24.0; // nominal hours in a year
    } else {
      fac = GribUtils.getConvertFactor(CalendarPeriod.Hour, timeUnitPeriod); // CalendarPeriod.Hour.getConvertFactor(timeUnitPeriod);
    }
    return fac * intvu.timeRange;
  }

  private static class TimeIntervalAndUnits {
    final int timeUnitIntv;
    final int timeRange;

    TimeIntervalAndUnits(int timeUnitIntv, int timeRange) {
      this.timeUnitIntv = timeUnitIntv;
      this.timeRange = timeRange;
    }
  }

  private TimeIntervalAndUnits getForecastTimeInterval(Grib2Pds.PdsInterval pdsIntv) {
    int range = 0;
    int timeUnitIntv = -999;
    for (Grib2Pds.TimeInterval ti : pdsIntv.getTimeIntervals()) {
      if (ti.timeRangeUnit == 255) {
        continue;
      }
      if (timeUnitIntv < 0) {
        timeUnitIntv = ti.timeRangeUnit;
      } else if ((ti.timeRangeUnit != timeUnitIntv) // make sure it doesnt change
          || (ti.timeIncrementUnit != timeUnitIntv && ti.timeIncrementUnit != 255 && ti.timeIncrement != 0)) {
        logger.warn("TimeInterval(2) has different units timeUnit first=" + timeUnitIntv + " TimeInterval="
            + ti.timeIncrementUnit);
        throw new RuntimeException("TimeInterval(2) has different units");
      }

      range += ti.timeRangeLength;
      if (ti.timeIncrementUnit != 255) {
        range += ti.timeIncrement;
      }
    }
    return new TimeIntervalAndUnits(timeUnitIntv, range);
  }

  /**
   * If this has a time interval coordinate, get time interval in units of pds.getTimeUnit()
   * since the ReferenceTime.
   *
   * @param gr from this record
   * @return time interval in units of pds.getTimeUnit(), or null if not a time interval
   */
  @Nullable
  public int[] getForecastTimeIntervalOffset(Grib2Record gr) {
    TimeCoordIntvDateValue tinvd = getForecastTimeInterval(gr);
    if (tinvd == null)
      return null;

    Grib2Pds pds = gr.getPDS();
    int unit = convertTimeUnit(pds.getTimeUnit());
    TimeCoordIntvValue tinv = tinvd.convertReferenceDate(gr.getReferenceDate(), Grib2Utils.getCalendarPeriod(unit));
    if (tinv == null)
      return null;
    int[] result = new int[2];
    result[0] = tinv.bounds1();
    result[1] = tinv.bounds2();
    return result;
  }

  /////////////////////////////////////////////////////
  // debugging

  /**
   * Get the unprocessed parameter provided by this Grib2Table.
   */
  @Nullable
  public GribTables.Parameter getParameterRaw(int discipline, int category, int number) {
    return WmoParamTable.getParameter(discipline, category, number);
  }

  /**
   * Get the name of the parameter table that is being used for this parameter.
   */
  public String getParamTablePathUsedFor(int discipline, int category, int number) {
    return WmoCodeFlagTables.standard.getResourceName();
  }

  /**
   * Get the list of parameters provided by this Grib2Table.
   */
  public List<GribTables.Parameter> getParameters() {
    ImmutableList.Builder<GribTables.Parameter> allParams = ImmutableList.builder();
    for (WmoTable wmoTable : WmoCodeFlagTables.getInstance().getWmoTables()) {
      if (wmoTable.getType() == WmoCodeFlagTables.TableType.param) {
        WmoParamTable paramTable = new WmoParamTable(wmoTable);
        allParams.addAll(paramTable.getParameters());
      }
    }
    return allParams.build();
  }

  public void lookForProblems(Formatter f) {}

  public void showDetails(Formatter f) {}

  public void showEntryDetails(Formatter f, List<GribTables.Parameter> params) {}

  public void showSpecialPdsInfo(Grib2Record pds, Formatter f) {}

  @Override
  public String toString() {
    return String.format("Grib2Tables{ class=%s, config=%s}", getClass().getName(), config);
  }
}
