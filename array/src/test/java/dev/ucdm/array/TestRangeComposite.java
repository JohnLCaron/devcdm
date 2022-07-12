/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.array;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link RangeComposite} */
public class TestRangeComposite {
  Random random = new Random(System.currentTimeMillis());

  @Test
  public void testBasics() throws InvalidRangeException {
    Range r1 = new Range("r1", 1, 5);
    Range r2 = new Range("r2", 42, 47, 3);
    RangeComposite rc = new RangeComposite("test", List.of(r1, r2));

    int[] expected = new int[] {1,2,3,4,5,42,45};
    assertThat(rc.length()).isEqualTo(expected.length);
    assertThat(rc.name()).isEqualTo("test");

    int count = 0;
    for (int idx : rc) {
      assertThat(idx).isEqualTo(expected[count]);
      count++;
    }

    RangeIterator copy = rc.copyWithName("another");
    assertThat(copy.name()).isEqualTo("another");
    assertThat(copy.length()).isEqualTo(expected.length);
    count = 0;
    for (int idx : copy) {
      assertThat(idx).isEqualTo(expected[count]);
      count++;
    }
  }
}
