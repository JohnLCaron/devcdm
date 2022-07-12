package dev.ucdm.grib.grib1.record;

import dev.ucdm.core.io.RandomAccessFile;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.Formatter;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.test.util.TestFilesKt.testFilesIn;

public class TestGrib1RecordScanner {
  interface Callback {
    boolean call(RandomAccessFile raf, Grib1Record gr) throws IOException;
  }

  public static Stream<Arguments> params() {
    return testFilesIn("src/test/data/")
            .addNameFilter(it -> it.endsWith("grib1"))
            .build();
  }

  @ParameterizedTest
  @MethodSource("params")
  public void testRead(String filename) throws IOException {

    readFile(filename, (raf, gr) -> {
      assertThat(gr.getGDS()).isNotNull();
      assertThat(gr.getPDSsection()).isNotNull();
      gr.getGDS().testHorizCoordSys(new Formatter());

      float[] data = gr.readData(raf);

      System.out.printf("%s: template,param,len=  %d, %d, %d, \"%s\" %n", filename, gr.getGDS().template,
              gr.getPDSsection().getParameterNumber(), data.length, gr.getReferenceDate());
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
