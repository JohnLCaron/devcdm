package dev.ucdm.core.util;


import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.core.util.Format.dfrac;
import static dev.ucdm.core.util.Format.formatDouble;

/** Test {@link Format} */
public class TestFormat {

  @Test
  public void testDfrac() {
    double num = 1.00003;
    assertThat(dfrac(num, 0)).isEqualTo("1");
    assertThat(dfrac(num, 1)).isEqualTo("1.0");
    assertThat(dfrac(num, 2)).isEqualTo("1.00");
    assertThat(dfrac(num, 3)).isEqualTo("1.000");
    assertThat(dfrac(num, 4)).isEqualTo("1.0000");
    assertThat(dfrac(num, 5)).isEqualTo("1.00003");
    assertThat(dfrac(num, 6)).isEqualTo("1.000030");
    assertThat(dfrac(-num, 6)).isEqualTo("-1.000030");
  }

  @Test
  public void testFormatDouble() {
    double num = 1.00003;
    assertThat(formatDouble(num, 0, -1)).isEqualTo("1");
    assertThat(formatDouble(num, 1, -1)).isEqualTo("1");
    assertThat(formatDouble(num, 2, -1)).isEqualTo("1.0");
    assertThat(formatDouble(num, 3, -1)).isEqualTo("1.00");
    assertThat(formatDouble(num, 4, -1)).isEqualTo("1.000");
    assertThat(formatDouble(num, 5, -1)).isEqualTo("1.0000");
    assertThat(formatDouble(num, 6, -1)).isEqualTo("1.00003");
    assertThat(formatDouble(num, 7, -1)).isEqualTo("1.00003");
    assertThat(formatDouble(num, 12, -1)).isEqualTo("1.00003");
    assertThat(formatDouble(-num, 7, -1)).isEqualTo("-1.00003");
  }

  @Test
  public void testD() {
    double num = 1.00003;
    assertThat(Format.d(num, 0)).isEqualTo("1");
    assertThat(Format.d(num, 1)).isEqualTo("1");
    assertThat(Format.d(num, 2)).isEqualTo("1.0");
    assertThat(Format.d(num, 3)).isEqualTo("1.00");
    assertThat(Format.d(num, 4)).isEqualTo("1.000");
    assertThat(Format.d(num, 5)).isEqualTo("1.0000");
    assertThat(Format.d(num, 6)).isEqualTo("1.00003");
    assertThat(Format.d(num, 7)).isEqualTo("1.00003");
  }

  @Test
  public void testDwidth() {
    double num = 1.2345;
    assertThat(Format.d(num, 3, 2)).isEqualTo("1.23");
    assertThat(Format.d(num, 3, 3)).isEqualTo("1.23");
    assertThat(Format.d(num, 3, 4)).isEqualTo("1.23");
    assertThat(Format.d(num, 3, 5)).isEqualTo(" 1.23");
    assertThat(Format.d(num, 3, 6)).isEqualTo("  1.23");
  }

  @Test
  public void testL() {
    long num = 12345L;
    assertThat(Format.l(num, 2)).isEqualTo("12345");
    assertThat(Format.l(num, 3)).isEqualTo("12345");
    assertThat(Format.l(num, 4)).isEqualTo("12345");
    assertThat(Format.l(num, 5)).isEqualTo("12345");
    assertThat(Format.l(num, 6)).isEqualTo(" 12345");
    assertThat(Format.l(num, 7)).isEqualTo("  12345");
  }

  @Test
  public void testI() {
    int num = 12345;
    assertThat(Format.i(num, 2)).isEqualTo("12345");
    assertThat(Format.i(num, 3)).isEqualTo("12345");
    assertThat(Format.i(num, 4)).isEqualTo("12345");
    assertThat(Format.i(num, 5)).isEqualTo("12345");
    assertThat(Format.i(num, 6)).isEqualTo(" 12345");
    assertThat(Format.i(num, 7)).isEqualTo("  12345");
  }

  @Test
  public void testPad() {
    assertThat(Format.pad("123", 2, true)).isEqualTo("123");
    assertThat(Format.pad("123", 2, false)).isEqualTo("123");
    assertThat(Format.pad("123", 3, true)).isEqualTo("123");
    assertThat(Format.pad("123", 3, false)).isEqualTo("123");
    assertThat(Format.pad("123", 4, true)).isEqualTo(" 123");
    assertThat(Format.pad("123", 4, false)).isEqualTo("123 ");
  }

  @Test
  public void testTab() {
    StringBuilder sb = new StringBuilder("123");
    Format.tab(sb, 2, false);
    assertThat(sb.toString()).isEqualTo("123");

    sb = new StringBuilder("123");
    Format.tab(sb, 2, true);
    assertThat(sb.toString()).isEqualTo("123 ");

    sb = new StringBuilder("123");
    Format.tab(sb, 6, false);
    assertThat(sb.toString()).isEqualTo("123   ");

    sb = new StringBuilder("123");
    Format.tab(sb, 6, true);
    assertThat(sb.toString()).isEqualTo("123   ");
  }

  @Test
  public void testFormatByteSize() {
    assertThat(Format.formatByteSize(123f)).isEqualTo("123.0 bytes");
    assertThat(Format.formatByteSize(123456f)).isEqualTo("123.4 Kbytes");
    assertThat(Format.formatByteSize(123456789f)).isEqualTo("123.4 Mbytes");
    assertThat(Format.formatByteSize(1.234e6f)).isEqualTo("1.234 Mbytes");
    assertThat(Format.formatByteSize(1.234e9f)).isEqualTo("1.234 Gbytes");
    assertThat(Format.formatByteSize(1.234e12f)).isEqualTo("1.234 Tbytes");
    assertThat(Format.formatByteSize(1.233e15f)).isEqualTo("1.233 Pbytes");
    assertThat(Format.formatByteSize(1234e15f)).isEqualTo("1234 Pbytes");
  }

}
