package dev.ucdm.grib.grib2.record;

import dev.cdm.core.io.RandomAccessFile;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.Formatter;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static dev.cdm.test.util.TestFilesKt.testFilesIn;

public class TestGrib2RecordScanner {
  interface Callback {
    boolean call(RandomAccessFile raf, Grib2Record gr) throws IOException;
  }

  public static Stream<Arguments> params() {
    return testFilesIn("src/test/data/")
            .addNameFilter(it -> it.endsWith("grib2"))
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
              gr.getPDSsection().getPDSTemplateNumber(), data.length, gr.getReferenceDate());
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
