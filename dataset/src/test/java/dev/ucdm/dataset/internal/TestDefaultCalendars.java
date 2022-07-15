/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.dataset.internal;

import dev.ucdm.core.api.Attribute;
import dev.ucdm.dataset.api.CdmDatasetCS;
import org.junit.jupiter.api.Test;
import dev.ucdm.array.Array;
import dev.ucdm.core.calendar.Calendar;
import dev.ucdm.core.calendar.CalendarDate;
import dev.ucdm.core.constants.AxisType;
import dev.ucdm.core.constants.CF;
import dev.ucdm.dataset.api.CoordinateAxis;
import dev.ucdm.dataset.api.CdmDatasets;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static dev.ucdm.test.util.TestFilesKt.datasetLocalDir;
import static java.lang.String.format;

public class TestDefaultCalendars {
  private final Calendar defaultCoardsCalendar = Calendar.gregorian;
  private final Calendar defaultCFCalendar = Calendar.gregorian;

  private final String coardsConvention = "COARDS";
  private final String cfConvention = "CF-1.X";

  @Test
  public void testCfDefaultCalendar() throws IOException {
    String failMessage, found, expected;
    boolean testCond;

    String tstFile = datasetLocalDir + "dataset/cfMissingCalendarAttr.nc";

    // open the test file
    try (CdmDatasetCS ncd = CdmDatasets.openDatasetCS(tstFile, true)) {
      assertThat(ncd.getConventionBuilder()).isEqualTo("CFConventions");

      // get the Time Coordinate Axis and read the values
      CoordinateAxis tca = ncd.findCoordinateAxis(AxisType.Time);
      Array<Number> times = (Array<Number>) tca.readArray();

      // first date in this file is 90 [hours since 2015-12-18T06:00:00],
      // which is 2015-12-22 00:00:00\
      expected = "90";
      found = Integer.toString(times.getScalar().intValue());
      testCond = found.equals(expected);
      failMessage = format("The first value in the times array should be %s. I got %s instead.", expected, found);
      assertWithMessage(failMessage).that(testCond).isTrue();

      // look for the calendar attached to the time variable...if there isn't one,
      // then a default was not set and the test will fail.
      Attribute att = tca.attributes().findAttributeIgnoreCase(CF.CALENDAR);
      assertThat(att).isNotNull();
      Calendar cal = Calendar.get(att.getStringValue()).orElseThrow();
      expected = defaultCFCalendar.toString();
      found = cal.toString();
      testCond = found.equals(expected);
      failMessage = format("The calendar should equal %s, but got %s instead. Failed to set a default calendar.",
              expected, found);
      assertWithMessage(failMessage).that(testCond).isTrue();

      // convert the time value to a CalendarDate
      CoordinateAxisTimeHelper coordAxisTimeHelper =
              new CoordinateAxisTimeHelper(cal, tca.attributes().findAttributeIgnoreCase("units").getStringValue());
      CalendarDate date = coordAxisTimeHelper.makeCalendarDateFromOffset(times.getScalar().intValue());

      // create the correct date as requested from NCSS
      String correctIsoDateTimeString = "2015-12-22T00:00:00Z";
      CalendarDate correctDate =
              CalendarDate.fromUdunitIsoDate(defaultCFCalendar.toString(), correctIsoDateTimeString).orElseThrow();

      // If everything is correct, then the date and correct date should be the same
      expected = correctDate.toString();
      found = date.toString();
      testCond = date.equals(correctDate);
      failMessage = format("The correct date is %s, but I got %s instead.", expected, found);
      assertWithMessage(failMessage).that(testCond).isTrue();
    }
  }

  @Test
  public void testCoardsDefaultCalendar() throws IOException {
    String failMessage, found, expected;
    boolean testCond;

    String tstFile = datasetLocalDir + "dataset/coardsMissingCalendarAttr.nc";

    // open the test file
    try (CdmDatasetCS ncd = CdmDatasets.openDatasetWithCS(tstFile, true)) {
      System.out.printf("testCoardsDefaultCalendar %s%n", ncd.getLocation());
      assertThat(ncd.getConventionBuilder()).isEqualTo("CFConventions");

      // get the Time Coordinate Axis and read the values
      CoordinateAxis tca = ncd.findCoordinateAxis(AxisType.Time);
      assertThat(tca).isNotNull();
      Array<Double> times = (Array<Double>) tca.readArray();
      Double firstTime = times.getScalar();
      assertThat(firstTime.intValue()).isEqualTo(17662920);

      String calendar = tca.attributes().findAttributeString(CF.CALENDAR, null);
      assertThat(calendar).isNotNull();
      Calendar cal = Calendar.get(calendar).orElseThrow();

      // convert the time value to a CalendarDate
      CoordinateAxisTimeHelper coordAxisTimeHelper =
              new CoordinateAxisTimeHelper(cal, tca.attributes().findAttributeString("units", null));
      CalendarDate date = coordAxisTimeHelper.makeCalendarDateFromOffset(firstTime.intValue());

      // read the correct date from the time attribute and turn it into a CalendarDate
      String correctIsoDateTimeString =
              tca.attributes().findAttributeString("correct_iso_time_value_str", null);
      CalendarDate correctDate =
              CalendarDate.fromUdunitIsoDate(defaultCoardsCalendar.toString(), correctIsoDateTimeString).orElseThrow();

      // If everything is correct, then the date and correct date should be the same
      assertThat(date).isEqualTo(correctDate);
      found = date.toString();
      expected = correctDate.toString();
      testCond = found.equals(expected);
      failMessage = format("The correct date is %s, but I got %s instead.", expected, found);
      assertWithMessage(failMessage).that(testCond).isTrue();
    }
  }
}

