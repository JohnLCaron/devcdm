/* Copyright Unidata */
package dev.ucdm.grib.protoconvert;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test can read both proto2 and proto3 gbx9.
 * Also test failure mode for invalid proto.
 */
public class TestGribIndexProto {
  private static final String gridTestDir = "../grib/src/test/data/index/";

  public static Stream<Arguments> params() {
    return Stream.of(
            Arguments.of(gridTestDir + "grib1.proto2.gbx9", true, false),
            Arguments.of(gridTestDir + "grib1.proto3.gbx9", true, false), // TODO ver8 failed
            Arguments.of(gridTestDir + "grib1.proto3.syntax2.gbx9", false, true), // TODO ver8 success
            Arguments.of(gridTestDir + "grib2.proto2.gbx9", true, true), // fails : grib2
            Arguments.of(gridTestDir + "../grib/src/test/data/afwa.grib1", true, true), // fails : not gbx
            Arguments.of(gridTestDir + "grib2.proto2.gbx9", false, false),
            Arguments.of(gridTestDir + "grib2.proto3.gbx9", false, false), // fails
            Arguments.of(gridTestDir + "grib2.proto3.syntax2.gbx9", false, false),
            Arguments.of(gridTestDir + "bad.gbx9", false, true), // fails : doesnt exist
            Arguments.of(gridTestDir + "../grib/src/test/data/afwa.grib1", false, true), // fails : not gbx
            Arguments.of(gridTestDir + "grib1.proto3.syntax2.gbx9", false, true)); // fails : grib1
  }

  @ParameterizedTest
  @MethodSource("params")
  public void testOpen(String filename, boolean isGrib1, boolean fail) {
    if (isGrib1) {
      Grib1Index index = Grib1IndexProto.readGrib1Index(filename);
      assertThat(index == null).isEqualTo(fail);
    } else {
      Grib2Index index = Grib2IndexProto.readGrib2Index(filename);
      assertThat(index == null).isEqualTo(fail);
    }
  }
}

