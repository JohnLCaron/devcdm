package dev.cdm.core.hdfeos5;

import dev.cdm.core.api.CdmFile;
import dev.cdm.core.api.CdmFiles;
import dev.cdm.core.api.Variable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;

public class TestReadAll {
  private static final String topdir = "src/test/data/hdfeos5/";

  public static Stream<Arguments> params() {
    File allDir = new File(topdir);
    File[] allFiles = allDir.listFiles();
    assertThat(allFiles).isNotNull();
    return Arrays.stream(allFiles).map( f -> Arguments.of(f.getAbsolutePath()));
  }

  @ParameterizedTest
  @MethodSource("params")
  public void testReadAll(String filename) throws IOException {
    assertThat(CdmFiles.canOpen(filename)).isTrue();
    try (CdmFile cdmFile = CdmFiles.open(filename)) {
      System.out.println("Test CdmFile: " + cdmFile.getLocation());
      assertThat(cdmFile.getCdmFileTypeDescription()).isEqualTo("Hierarchical Data Format 5");
      assertThat(cdmFile.getCdmFileTypeId()).isEqualTo("HDF-EOS5");
      System.out.printf("   CdmFileType %s version= '%s'%n", cdmFile.getCdmFileTypeId(), cdmFile.getCdmFileTypeVersion());

      for (Variable v : cdmFile.getVariables()) {
        assertThat(v.readArray()).isNotNull();
      }
    }
  }
}
