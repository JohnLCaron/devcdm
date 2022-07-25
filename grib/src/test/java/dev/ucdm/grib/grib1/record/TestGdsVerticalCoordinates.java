package dev.ucdm.grib.grib1.record;

import dev.ucdm.core.io.RandomAccessFile;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;

// Note: not using gdss.getVerticalCoordinateParameters() anywhere. */
public class TestGdsVerticalCoordinates {
  interface Callback {
    void call(RandomAccessFile raf, Grib1Record gr) throws IOException;
  }

  public static Stream<Arguments> params() {
    return Stream.of(
            Arguments.of("ECMWF.grib1", 0, 129, 184),
            Arguments.of("rotatedlatlon.grib1", 10, 7, 1)
    );
  }

  @ParameterizedTest
  @MethodSource("params")
  public void testRead(String ds, int gdsTemplate, int param, int ncoords) throws IOException {
    String filename = "../grib/src/test/data/" + ds;

    readFile(filename, (raf, gr) -> {
      Grib1SectionGridDefinition gdss = gr.getGDSsection();
      assertThat(gdss.hasVerticalCoordinateParameters()).isEqualTo(true);
      double[] vertCoords = gdss.getVerticalCoordinateParameters();
      assertThat(vertCoords).isNotNull();
      assertThat(vertCoords.length).isEqualTo(ncoords);
      System.out.printf("%n%s, %d, %d, %d, %s%n", filename, gdsTemplate, param,
              vertCoords.length, java.util.Arrays.toString(vertCoords));
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
