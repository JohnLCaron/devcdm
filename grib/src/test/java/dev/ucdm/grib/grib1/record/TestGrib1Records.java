package dev.ucdm.grib.grib1.record;

import dev.ucdm.core.calendar.CalendarDate;
import dev.ucdm.core.io.RandomAccessFile;
import dev.ucdm.grib.grib1.table.Grib1Customizer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.Formatter;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;

public class TestGrib1Records {
  interface Callback {
    boolean call(RandomAccessFile raf, Grib1Record gr) throws IOException;
  }

  public static Stream<Arguments> params() {
    return Stream.of(
            Arguments.of("afwa.grib1", 0, 61, 16899,
                    CalendarDate.fromUdunitIsoDate("ISO8601", "2005-05-04T00:00:00Z").orElseThrow()),
            Arguments.of("ECMWF.grib1", 0, 129, 1681,
                    CalendarDate.fromUdunitIsoDate("ISO8601", "2006-12-25T12:00:00Z").orElseThrow()),
            Arguments.of("HPPI89_KWBC.grib1", 0, 2, 5329,
                    CalendarDate.fromUdunitIsoDate("ISO8601", "2010-03-31T18:00:00Z").orElseThrow()),
            Arguments.of("thinGrid.grib1", 0, 11, 5329,
                    CalendarDate.fromUdunitIsoDate("ISO8601", "2005-07-27T12:00:00Z").orElseThrow()),
            Arguments.of("airtmp_zht_000002_000000_1a0061x0061_2010011200_00240000_fcstfld.grib1", 1, 11, 3721,
                    CalendarDate.fromUdunitIsoDate("ISO8601", "2010-01-12T00:00:00Z").orElseThrow()),
            Arguments.of("radar_national.grib1", 3, 201, 400000,
                    CalendarDate.fromUdunitIsoDate("ISO8601", "2005-01-20T02:15:00Z").orElseThrow()),
            Arguments.of("jma.grib1", 3, 2, 83525,
                    CalendarDate.fromUdunitIsoDate("ISO8601", "2006-08-14T00:00:00Z").orElseThrow()),
            Arguments.of("complex_packing.grib1", 4, 122, 131072,
                    CalendarDate.fromUdunitIsoDate("ISO8601", "2015-11-09T00:00:00Z").orElseThrow()),
            Arguments.of("complex_packing2.grib1", 4, 144, 131072,
                    CalendarDate.fromUdunitIsoDate("ISO8601", "2015-11-09T00:00:00Z").orElseThrow()),
            Arguments.of("D2.2006091400.F012.002M.CLWMR.grib1", 5, 153, 102960,
                    CalendarDate.fromUdunitIsoDate("ISO8601", "2006-09-14T00:00:00Z").orElseThrow()),
            Arguments.of("noaaRFC-QPE.grib1", 5, 237, 157500,
                    CalendarDate.fromUdunitIsoDate("ISO8601", "2010-10-05T00:00:00Z").orElseThrow()),
            Arguments.of("rotatedlatlon.grib1", 10, 7, 176904,
                    CalendarDate.fromUdunitIsoDate("ISO8601", "2003-12-15T18:00:00Z").orElseThrow()),
            Arguments.of("ncepPredefinedGds.grib1", 21000, 100, 37 * 36,
                    CalendarDate.fromUdunitIsoDate("ISO8601", "2004-11-19T00:00:00Z").orElseThrow()));
  }

  @ParameterizedTest
  @MethodSource("params")
  public void testRead(String ds, int gdsTemplate, int param, int datalen, CalendarDate refdate) throws IOException {
    String filename = "../grib/src/test/data/" + ds;

    readFile(filename, (raf, gr) -> {
      Grib1Gds gds = gr.getGDS();
      assertThat(gdsTemplate).isEqualTo(gds.template);
      assertThat(gds.toString().contains("template=" + gdsTemplate)).isTrue();
      gds.testHorizCoordSys(new Formatter());

      Grib1SectionProductDefinition pds = gr.getPDSsection();
      assertThat(param).isEqualTo(pds.getParameterNumber());
      Formatter f = new Formatter();
      pds.showPds(Grib1Customizer.factory(gr, null), f);
      assertThat(f.toString().contains(String.format("Parameter Name : (%d)", param))
              || f.toString().contains(String.format("Parameter %d not found", param))).isTrue();
      assertThat(refdate).isEqualTo(pds.getReferenceDate());

      float[] data = gr.readData(raf);
      assertThat(datalen).isEqualTo(data.length);
      System.out.printf("%s: template,param,len=  %d, %d, %d, %s %n", filename, gds.template, pds.getParameterNumber(),
              data.length, pds.getReferenceDate());
      return true;
    });
  }

  private void readFile(String path, Callback callback) throws IOException {
    try (RandomAccessFile raf = new RandomAccessFile(path, "r")) {
      raf.order(RandomAccessFile.BIG_ENDIAN);
      raf.seek(0);

      Grib1RecordScanner reader = new Grib1RecordScanner(raf);
      while (reader.hasNext()) {
        Grib1Record gr = reader.next();
        if (gr == null)
          break;
        callback.call(raf, gr);
      }

    }
  }


}
