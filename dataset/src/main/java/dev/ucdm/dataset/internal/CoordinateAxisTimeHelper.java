/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.dataset.internal;

import dev.ucdm.core.calendar.Calendar;
import dev.ucdm.core.calendar.CalendarDate;
import dev.ucdm.core.calendar.CalendarDateUnit;

/**
 * Helper class for time coordinates
 */
public class CoordinateAxisTimeHelper {
  private final Calendar calendar;
  private final CalendarDateUnit dateUnit;

  public CoordinateAxisTimeHelper(Calendar calendar, String unitString) {
    this.calendar = calendar;
    if (unitString == null) {
      this.dateUnit = null;
      return;
    }
    this.dateUnit = CalendarDateUnit.fromUdunitString(calendar, unitString).orElseThrow();
  }

  public CalendarDate makeCalendarDateFromOffset(int offset) {
    return dateUnit.makeCalendarDate(offset);
  }

  public CalendarDate makeCalendarDateFromOffset(String offset) {
    return CalendarDate.fromUdunitIsoDate(calendar.toString(), offset).orElseThrow();
  }

}
