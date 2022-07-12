/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.array;

import org.junit.jupiter.api.Test;

import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link dev.ucdm.array.NumericCompare} */
public class TestNumericCompare {

  @Test
  public void testCompareBytes() {
    byte[] raw1 = new byte[] {1, 2, 3};
    byte[] raw2 = new byte[] {1, 2, 2};
    byte[] raw3 = new byte[] {1, 2};

    Formatter f;

    assertThat(NumericCompare.compare(raw1, null, null)).isFalse();

    f = new Formatter();
    assertThat(NumericCompare.compare(raw1, raw1, f)).isTrue();
    assertThat(f.toString()).isEmpty();

    f = new Formatter();
    assertThat(NumericCompare.compare(raw1, raw2, f)).isFalse();
    assertThat(f.toString())
        .isEqualTo(String.format("   2 :   3 !=   2%n" + "NumericCompare.compare 3 bytes, 1 are different%n"));

    f = new Formatter();
    assertThat(NumericCompare.compare(raw1, raw3, f)).isFalse();
    assertThat(f.toString()).isEqualTo(String.format("length 1=   3 != length 2=  2%n"));
  }

  @Test
  public void testCompareInt() {
    int[] raw1 = new int[] {1, 2, 3};
    int[] raw2 = new int[] {1, 2, 2};
    int[] raw3 = new int[] {1, 2};

    Formatter f;

    assertThat(NumericCompare.compare(raw1, null, null)).isFalse();

    f = new Formatter();
    assertThat(NumericCompare.compare(raw1, raw1, f)).isTrue();
    assertThat(f.toString()).isEmpty();

    f = new Formatter();
    assertThat(NumericCompare.compare(raw1, raw2, f)).isFalse();
    assertThat(f.toString()).isEqualTo(String.format("   2 :   3 !=   2%n" + "NumericCompare.compare 3 ints, 1 are different%n"));

    f = new Formatter();
    assertThat(NumericCompare.compare(raw1, raw3, f)).isFalse();
    assertThat(f.toString()).endsWith(String.format("length 1=   3 != length 2=  2%n"));
  }

  @Test
  public void testCompareFloat() {
    float[] raw1 = new float[] {1, 2, 3};
    float[] raw2 = new float[] {1, 2, 2};
    float[] raw3 = new float[] {1, 2};

    Formatter f;

    assertThat(NumericCompare.compare(raw1, null, null)).isFalse();

    f = new Formatter();
    assertThat(NumericCompare.compare(raw1, raw1, f)).isTrue();
    assertThat(f.toString()).isEmpty();

    f = new Formatter();
    assertThat(NumericCompare.compare(raw1, raw2, f)).isFalse();
    assertThat(f.toString())
        .isEqualTo(String.format("     2 : 3.000000 != 2.000000%n" + "NumericCompare.compare 3 floats, 1 are different%n"));

    f = new Formatter();
    assertThat(NumericCompare.compare(raw1, raw3, f)).isFalse();
    assertThat(f.toString()).isEqualTo(String.format("compareFloat: length 1=   3 != length 2=  2%n"));
  }

  @Test
  public void testAbsoluteDifference() {
    assertThat(NumericCompare.absoluteDifference(100, 100.01)).isWithin(1.0e-8).of(.01);
    assertThat(NumericCompare.absoluteDifference(100, Double.NaN)).isEqualTo(Double.NaN);
  }

  @Test
  public void testRelativeDifference() {
    assertThat(NumericCompare.relativeDifference(100, 100.01)).isWithin(1.0e-8).of(.01/100.01);
    assertThat(NumericCompare.relativeDifference(0, 0)).isEqualTo(0);
    assertThat(NumericCompare.relativeDifference(0, 100)).isEqualTo(1);
    assertThat(NumericCompare.relativeDifference(100, 0)).isEqualTo(1);
  }

  @Test
  public void testRelativeDifferenceFloat() {
    assertThat(NumericCompare.relativeDifference(0f, 0f)).isEqualTo(0f);
    assertThat(NumericCompare.relativeDifference(0f, 100f)).isEqualTo(1f);
    assertThat(NumericCompare.relativeDifference(100f, 0f)).isEqualTo(1f);
    assertThat(NumericCompare.relativeDifference(100f, 100.01f)).isWithin(1.0e-5f).of((float) (.01/100.01));
  }

   // TODO more tests

}
