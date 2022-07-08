/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.grib.grib2.table;

import dev.cdm.core.calendar.CalendarDate;
import dev.cdm.core.calendar.CalendarPeriod;
import dev.ucdm.grib.common.TimeCoordIntvDateValue;
import dev.ucdm.grib.grib2.record.Grib2Pds;
import dev.ucdm.grib.grib2.record.Grib2Record;
import dev.ucdm.grib.grib2.iosp.Grib2Utils;

import javax.annotation.Nullable;

/**
 * Center = (7) US National Weather Service, National Centres for Environmental Prediction (NCEP)
 * SubCenter = (14) NWS Meteorological Development Laboratory
 * Master Table = 1
 * Local Table = 0
 *
 * @author caron
 * @since 1/28/2016.
 */
public class NwsMetDevTables extends NcepLocalTables {

  NwsMetDevTables(Grib2TableConfig grib2Table) {
    super(grib2Table);
  }

  @Override
  @Nullable
  public TimeCoordIntvDateValue getForecastTimeInterval(Grib2Record gr) {
    Grib2Pds pds = gr.getPDS();
    if (!pds.isTimeInterval())
      return null;
    Grib2Pds.PdsInterval pdsIntv = (Grib2Pds.PdsInterval) gr.getPDS();

    // override here only if timeRangeUnit = 255
    boolean needOverride = false;
    for (Grib2Pds.TimeInterval ti : pdsIntv.getTimeIntervals()) {
      needOverride = (ti.timeRangeUnit == 255);
    }
    if (!needOverride)
      return super.getForecastTimeInterval(gr);

    CalendarDate intvEnd = pdsIntv.getIntervalTimeEnd();
    int ftime = pdsIntv.getForecastTime();
    int timeUnitOrg = pds.getTimeUnit();
    int timeUnitConvert = convertTimeUnit(timeUnitOrg);
    CalendarPeriod unitPeriod = Grib2Utils.getCalendarPeriod(timeUnitConvert);
    if (unitPeriod == null)
      throw new IllegalArgumentException("unknown CalendarPeriod " + timeUnitConvert + " org=" + timeUnitOrg);

    CalendarPeriod.Field fld = unitPeriod.getField();

    CalendarDate referenceDate = gr.getReferenceDate();
    CalendarDate intvStart = referenceDate.add(ftime, fld);

    return new TimeCoordIntvDateValue(intvStart, intvEnd);
  }

  /**
   * Only use in GribVariable to decide on variable identity when intvMerge = false.
   * By returning a constant, we dont intvMerge = false.
   * Problem is we cant reconstruct interval length without reference time, which is not in the pds.
   */
  @Override
  public double getForecastTimeIntervalSizeInHours(Grib2Pds pds) {
    Grib2Pds.PdsInterval pdsIntv = (Grib2Pds.PdsInterval) pds;

    // override here only if timeRangeUnit = 255
    boolean needOverride = false;
    for (Grib2Pds.TimeInterval ti : pdsIntv.getTimeIntervals()) {
      needOverride = (ti.timeRangeUnit == 255);
    }
    if (!needOverride) {
      return super.getForecastTimeIntervalSizeInHours(pds);
    }
    // this is some kind of hack for Center 7 subcenter 14.
    return 12.0;
  }

}
