/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.array;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.StreamSupport;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

/** Test {@link Array} */
public class TestPrintArray {

  private Array<Double> array3d;
  private Array<Double> array1d;

  @BeforeEach
  public void setup() {
    double[] data = new double[] {1, 2, 3, 4, 5, 6};
    array3d = Arrays.factory(ArrayType.DOUBLE, new int[] {1, 2, 3}, data);
    array1d = Arrays.factory(ArrayType.DOUBLE, new int[] {6}, data);
  }

  @Test
  public void testBasics() {
    assertThat(PrintArray.printArray(array1d))
            .isEqualTo("  [1.0, 2.0, 3.0, 4.0, 5.0, 6.0]");

    assertThat(PrintArray.printArray(null, array1d, new Indent(0)).toString())
            .isEqualTo("[1.0, 2.0, 3.0, 4.0, 5.0, 6.0]");

    assertThat(PrintArray.printArray(null, array1d, new Indent(2)).toString())
            .isEqualTo("  [1.0, 2.0, 3.0, 4.0, 5.0, 6.0]");

    assertThat(PrintArray.printArrayPlain(array3d)).isEqualTo("1.0 2.0 3.0 4.0 5.0 6.0 ");

    assertThat(PrintArray.printArray(array3d)).isEqualTo(
            "  [\n" +
            "    [\n" +
            "      [1.0, 2.0, 3.0], \n" +
            "      [4.0, 5.0, 6.0]\n" +
            "    ]\n" +
            "  ]");
  }

  @Test
  public void testScalarArray() {
    Array<?> test = Arrays.factory(ArrayType.INT, new int[] {1}, new int[] {123456});
    assertThat(PrintArray.printArray(test)).isEqualTo("  [123456]");
    assertThat(PrintArray.printArrayPlain(test)).isEqualTo("123456 ");
  }

  @Test
  public void testUnsignedArray() {
    Array<?> test = Arrays.factory(ArrayType.UBYTE, new int[] {3}, new byte[] {-1, 0, 1});
    assertThat(PrintArray.printArray(test)).isEqualTo("  [255, 0, 1]");
    assertThat(PrintArray.printArrayPlain(test)).isEqualTo("-1 0 1 ");
  }

  @Test
  public void testCharArray() {
    Array<?> test = Arrays.factory(ArrayType.CHAR, new int[] {6}, "123456".getBytes());
    assertThat(PrintArray.printArray(test)).isEqualTo("  \"123456\"");
    assertThat(PrintArray.printArrayPlain(test)).isEqualTo("49 50 51 52 53 54 ");

    Array<?> test2 = Arrays.factory(ArrayType.CHAR, new int[] {3, 2}, "123456".getBytes());
    assertThat(PrintArray.printArray(test2)).isEqualTo("  \"12\",   \"34\",   \"56\"");
    assertThat(PrintArray.printArrayPlain(test2)).isEqualTo("49 50 51 52 53 54 ");

    Array<?> test3 = Arrays.factory(ArrayType.CHAR, new int[] {3, 2, 2}, "123456123456".getBytes());
    assertThat(PrintArray.printArray(test3)).isEqualTo(
            "\n " +
                    " {  \"12\",   \"34\",  \"56\",   \"12\",  \"34\",   \"56\"\n" +
                    "  }");
    assertThat(PrintArray.printArrayPlain(test3)).isEqualTo("49 50 51 52 53 54 49 50 51 52 53 54 ");
  }

  @Test
  public void testStringArray() {
    Array<?> test = Arrays.factory(ArrayType.STRING, new int[] {3}, new String[] {"123", "456", "789"});
    assertThat(PrintArray.printArray(test)).isEqualTo("\"123\", \"456\", \"789\"");
    assertThat(PrintArray.printArrayPlain(test)).isEqualTo("123 456 789 ");

    Array<?> test2 = Arrays.factory(ArrayType.STRING, new int[] {2, 2}, new String[] {"123", "456", "789", "101112"});
    assertThat(PrintArray.printArray(test2)).isEqualTo(
            "\n" +
                    "  {\"123\", \"456\",\"789\", \"101112\"\n" +
                    "  }");
    assertThat(PrintArray.printArrayPlain(test2)).isEqualTo("123 456 789 101112 ");
  }

  // TODO
  @Test
  public void testStructureArray() {
    Array<StructureData> test = TestStructureData.makeStructureArray(0);
    System.out.printf("%s%n", test);
    System.out.printf("%s%n", PrintArray.printArray(test));

    for (StructureData sd : test) {
      System.out.printf("%s%n", PrintArray.printStructureData(sd));
    }
  }

  // TODO
  @Test
  public void testVlen() {
    int[] shape = new int[]{1, 2, -1};
    short[] arr1 = new short[]{1, 2, 3, 4, 5};
    short[] arr2 = new short[]{6, 7};
    short[][] ragged = new short[][]{arr1, arr2};
    ArrayVlen<Short> test = ArrayVlen.factory(ArrayType.SHORT, shape, ragged);
    assertThat(test.isVlen()).isTrue();

    System.out.printf("%s%n", test);
    System.out.printf("%s%n", PrintArray.printArray(test));
  }


}
