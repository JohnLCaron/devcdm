/* Copyright Unidata */
package dev.ucdm.dataset.geoloc;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.ucdm.array.NumericCompare;

import java.lang.invoke.MethodHandles;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Test that the algorithm for longitude normalization works
 */
public class TestLongitudeNormalization {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static Stream<Arguments> params() {
    return Stream.of(
            Arguments.of(100.0, -100.0, 0.0),
            Arguments.of(-100.0, 100.0, 360.0),
            Arguments.of(-100.0, -180.0, 0.0),
            Arguments.of(-180.0, -100.0, 360.0),
            Arguments.of(-180.0, 180.0, 360.0),
            Arguments.of(181.0, -180.0, -360.0),
            Arguments.of(181.0, -200.0, -360.0),
            Arguments.of(-200.0, 200.0, 720.0),
            Arguments.of(-179.0, 180.0, 360.0)
    );
  }

  @ParameterizedTest
  @MethodSource("params")
  public void testLongitudeNormalization(double lon, double from, Double expectedDiff) {
    double compute = lonNormalFrom(lon, from);

    if (expectedDiff != null) {
      logger.debug("({} from {}) = {}, diff = {} expectedDiff {}", lon, from, compute, compute - lon, expectedDiff);
      assertThat(NumericCompare.nearlyEquals(expectedDiff, compute - lon)).isTrue();
    } else {
      logger.debug("({} from {}) = {}, diff = {}", lon, from, compute, compute - lon);
    }

    String msg = String.format("(%f from %f) = %f%n", lon, from, compute);
    assertWithMessage(msg).that(compute).isAtLeast(from);
    assertWithMessage(msg).that(compute).isAtMost(from + 360);
  }

  /**
   * put longitude into the range [start, start+360] deg
   *
   * @param lon   lon to normalize
   * @param start starting point
   * @return longitude into the range [center +/- 180] deg
   */
  static public double lonNormalFrom(double lon, double start) {
    while (lon < start)
      lon += 360;
    while (lon > start + 360)
      lon -= 360;
    return lon;
  }
}
