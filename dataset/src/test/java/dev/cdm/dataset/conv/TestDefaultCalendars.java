/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.cdm.dataset.conv;

import dev.cdm.core.api.Attribute;
import dev.cdm.dataset.api.CdmDatasetCS;
import dev.cdm.dataset.api.TestCdmDatasets;
import org.junit.jupiter.api.Test;
import dev.cdm.array.Array;
import dev.cdm.core.calendar.Calendar;
import dev.cdm.core.calendar.CalendarDate;
import dev.cdm.core.constants.AxisType;
import dev.cdm.core.constants.CF;
import dev.cdm.dataset.api.CoordinateAxis;
import dev.cdm.dataset.api.CdmDatasets;
import dev.cdm.dataset.internal.CoordinateAxisTimeHelper;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
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

    String tstFile = TestCdmDatasets.datasetLocalDir + "dataset/cfMissingCalendarAttr.nc";

    // open the test file
    try (CdmDatasetCS ncd = CdmDatasets.openDatasetCS(tstFile, true)) {

      // make sure this dataset used the cfConvention
      expected = cfConvention;
      found = ncd.getConventionBuilder();
      testCond = found.equals(expected);
      failMessage =
              format("This dataset used the %s convention, but should have used the %s convention.", found, expected);
      assertWithMessage(failMessage).that(testCond).isTrue();

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

  // TODO rewrite this
  @Test
  public void testCoardsDefaultCalendar() throws IOException {
    String failMessage, found, expected;
    boolean testCond;

    String tstFile = TestCdmDatasets.datasetLocalDir + "dataset/coardsMissingCalendarAttr.nc";

    // open the test file
    try (CdmDatasetCS ncd = CdmDatasets.openDatasetCS(tstFile, true)) {
      System.out.printf("testCoardsDefaultCalendar %s%n", ncd.getLocation());
      // make sure this dataset used the coardsConvention
      found = ncd.getConventionBuilder();
      expected = coardsConvention;
      testCond = found.equals(expected);
      failMessage =
              format("This dataset used the %s convention, but should have used the %s convention.", found, expected);
      assertWithMessage(failMessage).that(testCond).isTrue();

      // get the Time Coordinate Axis and read the values
      CoordinateAxis tca = ncd.findCoordinateAxis(AxisType.Time);
      assertThat(tca).isNotNull();
      Array<Integer> times = (Array<Integer>) tca.readArray();

      // first date in this file is 1.766292E7 [hours since 0001-01-01T00:00:00],
      // which is 2008-06-27 00:00:00
      found = Integer.toString(times.getScalar());
      expected = "17662920";
      testCond = found.equals(expected);
      failMessage = format("The first value in the times array should be %s. I got %s instead.", expected, found);
      assertWithMessage(failMessage).that(testCond).isTrue();

      // look for the calendar attached to the time variable...if there isn't one,
      // then a default was not set and the test will fail.
      Calendar cal = Calendar.get(tca.attributes().findAttributeIgnoreCase(CF.CALENDAR).getStringValue()).orElseThrow();
      found = cal.toString();
      expected = defaultCoardsCalendar.toString();
      testCond = found.equals(expected);
      failMessage = format("The calendar should equal %s, but got %s instead. Failed to add a default calendar.",
              expected, found);
      assertWithMessage(failMessage).that(testCond).isTrue();

      // convert the time value to a CalendarDate
      CoordinateAxisTimeHelper coordAxisTimeHelper =
              new CoordinateAxisTimeHelper(cal, tca.attributes().findAttributeIgnoreCase("units").getStringValue());
      CalendarDate date = coordAxisTimeHelper.makeCalendarDateFromOffset(times.getScalar());

      // read the correct date from the time attribute and turn it into a CalendarDate
      String correctIsoDateTimeString =
              tca.attributes().findAttributeIgnoreCase("correct_iso_time_value_str").getStringValue();
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

