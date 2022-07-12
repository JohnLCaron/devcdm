package dev.ucdm.grib.grib2.record;

import dev.ucdm.core.calendar.CalendarDate;
import dev.ucdm.core.io.RandomAccessFile;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.Formatter;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;

public class TestGrib2Records {
  interface Callback {
    boolean call(RandomAccessFile raf, Grib2Record gr) throws IOException;
  }

  private static String testdir = "src/test/data/";

  public static Stream<Arguments> params() {
    return Stream.of(
            Arguments.of(testdir + "HLYA10.grib2", 0, 0, 259920, "2014-01-15T00:00:00Z"),
            Arguments.of(testdir + "MRMS_LowLevelCompositeReflectivity_00.50_20141207-072038.grib2", 0, 0, 24500000,
                    "2014-12-07T07:20:38Z"),
            Arguments.of(testdir + "cosmo-eu.grib2", 1, 0, 25, "2010-03-29T00:00:00Z"),
            Arguments.of(testdir + "ds.sky.grib2", 10, 0, 22833, "2005-09-01T15:00:00Z"),
            Arguments.of(testdir + "sref_eta.grib2", 20, 0, 14873, "2009-03-16T21:00:00Z"),
            Arguments.of(testdir + "ds.snow.grib2", 30, 8, 739297, "2005-09-01T15:00:00Z"),
            Arguments.of(testdir + "Albers_viirs_s.grib2", 31, 0, 3255148, "2014-02-14T12:00:00Z"),
            Arguments.of(testdir + "thinGrid.grib2", 40, 0, 2097152, "2012-01-01T00:00:00Z"),
            Arguments.of(testdir + "Eumetsat.VerticalPerspective.grib2", 90, 30, 1530169, "2007-05-22T11:45:00Z"),
            Arguments.of(testdir + "ofs_atl.grib2", 204, 0, 2020800, "2007-08-07T00:00:00Z"),
            Arguments.of(testdir + "rap-native.grib2", 32769, 0, 35, "2016-04-25T22:00:00Z"),

            // Example of each PDS that we have
            Arguments.of(testdir + "pdsScale.pds1.grib2", 0, 1, 65160, "2010-11-06T00:00:00Z"),
            Arguments.of(testdir + "sref.pds2.grib2", 30, 2, 23865, "2009-01-25T21:00:00Z"),
            Arguments.of(testdir + "problem.pds9.grib2", 30, 9, 739297, "2010-09-13T00:00:00Z"),
            Arguments.of(testdir + "cosmo.pds11.grib2", 1, 11, 194081, "2009-05-11T00:00:00Z"),
            Arguments.of(testdir + "sref.pds12.grib2", 30, 12, 23865, "2009-01-25T21:00:00Z"),
            Arguments.of(testdir + "rugley.pds15.grib2", 0, 15, 41760, "2010-10-13T18:00:00Z"),
            Arguments.of(testdir + "Lannion.pds31.grib2", 0, 31, 5760000, "2013-11-18T02:00:00Z"),
            Arguments.of(testdir + "BOM_synsat.pds32.grib2", 0, 32, 9690, "2020-05-06T12:00:00Z"));
  }

  @ParameterizedTest
  @MethodSource("params")
  public void testRead(String filename, int gdsTemplate, int pdsTemplate, long datalen, String refdateIso) throws IOException {
    CalendarDate refdate = CalendarDate.fromUdunitIsoDate("ISO8601", refdateIso).orElseThrow();

    readFile(filename, (raf, gr) -> {
      Grib2Gds gds = gr.getGDS();
      assertThat(gdsTemplate).isEqualTo(gds.template);
      gds.testHorizCoordSys(new Formatter());

      Grib2SectionProductDefinition pdss = gr.getPDSsection();
      Grib2Pds pds = pdss.getPDS();
      assertThat(pdsTemplate).isEqualTo(pdss.getPDSTemplateNumber());
      Formatter f = new Formatter();
      pds.show(f);
      assertThat(f.toString()).contains(String.format("template=%d", pdsTemplate));
      assertThat(refdate).isEqualTo(gr.getReferenceDate());

      float[] data = gr.readData(raf);
      assertThat(datalen).isEqualTo(data.length);
      System.out.printf("%s: template,param,len=  %d, %d, %d, \"%s\" %n", filename, gds.template,
              pdss.getPDSTemplateNumber(), data.length, gr.getReferenceDate());

      if (pds.isSatellite()) {
        Grib2Pds.PdsSatellite sat = (Grib2Pds.PdsSatellite) pds;
        int numBands = sat.getNumSatelliteBands();
        assertThat(numBands).isNotEqualTo(0);
        assertThat(numBands).isEqualTo(sat.getSatelliteBands().length);
      }
      return true;
    });
  }

  private void readFile(String path, Callback callback) throws IOException {
    try (RandomAccessFile raf = new RandomAccessFile(path, "r")) {
      raf.order(RandomAccessFile.BIG_ENDIAN);
      raf.seek(0);

      Grib2RecordScanner reader = new Grib2RecordScanner(raf);
      while (reader.hasNext()) {
        Grib2Record gr = reader.next();
        if (gr == null)
          break;
        callback.call(raf, gr);
      }
    }
  }


}
