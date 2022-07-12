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

/** Test that the algorithm for longitude wrapping works */
public class TestLongitudeWrap {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static Stream<Arguments> params() {
    return Stream.of(
            Arguments.of(100, -100, 200)
    );
  }

  @ParameterizedTest
  @MethodSource("params")
  public void testLongitudeWrap(double lat1, double lat2, double expected) {
    double compute = lonDiff(lat1, lat2);
    logger.debug("({} - {}) = {}, expect {}", lat1, lat2, compute, expected);
    assertThat(NumericCompare.nearlyEquals(expected, compute)).isTrue();
    assertThat(Math.abs(compute)).isLessThan(360.0);
  }

  static public double lonDiff(double lon1, double lon2) {
    return Math.IEEEremainder(lon1 - lon2, 720.0);
  }
}
