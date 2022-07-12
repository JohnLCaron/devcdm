/*
 * (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 */
package dev.ucdm.grib.grib2.record;

import dev.ucdm.core.calendar.CalendarDate;
import dev.ucdm.grib.collection.MFile;
import dev.ucdm.grib.collection.MFileOS;
import dev.ucdm.grib.collection.CollectionUpdateType;
import dev.ucdm.grib.common.GribIndex;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test data provided by github issue https://github.com/Unidata/thredds/issues/834
 * Only the first record was used to create a .gbx9 file.
 * <p>
 * Full output from ecCodes v2.6.0 grib_dump at the end of the file, with data values
 * removed (as required by ECMWF data use policy for redistribution without the need
 * for netCDF-java / TDS users to register with them). Note also because of the data
 * use policy, we are only testing the gbx9 file (which is the GRIB record, minus the
 * data block)
 */
public class TestPds61 {

  private static Grib2Pds pds;

  @BeforeAll
  public static void openTestFile() throws IOException {
    String testfile = "/home/snake/tmp/example_pds_61.grib2.gbx9";

    MFile mfile = MFileOS.getExistingFile(testfile);
    assertThat(mfile).isNotNull();

    dev.ucdm.grib.protoconvert.Grib2Index gi = GribIndex.readOrCreateIndex2(mfile, CollectionUpdateType.nocheck, new Formatter());

    assertThat(gi).isNotNull();
    List<Grib2Record> records = gi.getRecords();
    Grib2Record record = records.get(0);
    pds = record.getPDS();
  }

  @Test
  public void checkPdsBasic() {
    assertThat(pds.getRawLength()).isEqualTo(68);
    assertThat(pds.getTemplateNumber()).isEqualTo(61);
  }

  @Test
  public void checkModelVersionDate() {
    assertThat(pds.calcTime(38))
        .isEqualTo(CalendarDate.fromUdunitIsoDate("proleptic_gregorian", "2011-03-01T00:00:00").orElseThrow());
  }

  @Test
  public void checkEndOfOverallIntervalDate() {
    assertThat(pds.calcTime(45))
        .isEqualTo(CalendarDate.fromUdunitIsoDate("proleptic_gregorian", "2010-12-29T06:00:00").orElseThrow());
  }

  @Test
  public void checkTypeOfGeneratingProcess() {
    assertThat(pds.getGenProcessType()).isEqualTo(4);
  }

  @Test
  public void checkNumberOfTimeRanges() {
    assertThat(pds.getOctet(52)).isEqualTo(1);
  }

}
