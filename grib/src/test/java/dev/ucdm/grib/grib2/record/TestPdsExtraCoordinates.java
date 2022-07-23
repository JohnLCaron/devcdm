/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.grib2.record;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.grib.grib2.record.TestGrib2Records.readFile;

public class TestPdsExtraCoordinates {
  public static Stream<Arguments> params() {
    return Stream.of(
            Arguments.of("/media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/grib2/COSMO_EU.grib2"),
            Arguments.of("/media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/grib2/cosmo_de_eps_m001_2009051100.grib2"),
            Arguments.of("/media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/grib2/thinGrid.grib2")
    );
  }

  private float[] firstExtra = null;
  private Grib2Pds firstPds = null;
  private Grib2Gds firstGds = null;

  @ParameterizedTest
  @MethodSource("params")
  public void testRead(String filename) throws IOException {
    System.out.printf("%n***** filename %s%n", filename);
    readFile(filename, (raf, gr) -> {
      Grib2SectionGridDefinition gdss = gr.getGDSsection();
      Grib2Gds gds = gr.getGDS();
      if (gds.getNptsInLine() != null && firstGds == null) {
        System.out.printf("getNptsInLine = %s%n", java.util.Arrays.toString(gds.getNptsInLine()));
        firstGds = gds;
      }

      Grib2SectionProductDefinition pdss = gr.getPDSsection();
      Grib2Pds pds = pdss.getPDS();
      assertThat(pds.getExtraCoordinatesCount()).isGreaterThan(0);
      if (firstExtra == null) {
        firstExtra = pds.getExtraCoordinates();
        firstPds = pds;
        System.out.printf("extra = %s%n", java.util.Arrays.toString(pds.getExtraCoordinates()));
        System.out.printf("pds = %s%n", pds);
      } else {
        assertThat(pds.getExtraCoordinates()).isEqualTo(firstExtra);
        // assertThat(pds).isEqualTo(firstPds);
      }

    });
  }

}
