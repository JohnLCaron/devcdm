/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.array;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

/** Test {@link Arrays} */
public class TestArrays {

  private Array<Double> array;

  @BeforeEach
  public void setup() {
    int[] shape = new int[] {1, 2, 3};
    double[] data = new double[] {1, 2, 3, 4, 5, 6};
    array = Arrays.factory(ArrayType.DOUBLE, shape, data);
  }

  @Test
  public void testFlip() {
    Array<Double> flip0 = Arrays.flip(array, 0);
    assertThat(flip0.get(0, 0, 0)).isEqualTo(1);
    assertThat(flip0.get(0, 0, 1)).isEqualTo(2);
    assertThat(flip0.get(0, 0, 2)).isEqualTo(3);
    assertThat(flip0.get(0, 1, 0)).isEqualTo(4);
    assertThat(flip0.get(0, 1, 1)).isEqualTo(5);
    assertThat(flip0.get(0, 1, 2)).isEqualTo(6);

    double[] expected = new double[] {1, 2, 3, 4, 5, 6};
    int count = 0;
    for (double val : flip0) {
      assertThat(val).isEqualTo(expected[count]);
      count++;
    }

    Array<Double> flip1 = Arrays.flip(array, 1);
    assertThat(flip1.get(0, 1, 0)).isEqualTo(1);
    assertThat(flip1.get(0, 1, 1)).isEqualTo(2);
    assertThat(flip1.get(0, 1, 2)).isEqualTo(3);
    assertThat(flip1.get(0, 0, 0)).isEqualTo(4);
    assertThat(flip1.get(0, 0, 1)).isEqualTo(5);
    assertThat(flip1.get(0, 0, 2)).isEqualTo(6);

    expected = new double[] {4, 5, 6, 1, 2, 3};
    count = 0;
    for (double val : flip1) {
      assertThat(val).isEqualTo(expected[count]);
      count++;
    }

    Array<Double> flip2 = Arrays.flip(array, 2);
    assertThat(flip2.get(0, 0, 2)).isEqualTo(1);
    assertThat(flip2.get(0, 0, 1)).isEqualTo(2);
    assertThat(flip2.get(0, 0, 0)).isEqualTo(3);
    assertThat(flip2.get(0, 1, 2)).isEqualTo(4);
    assertThat(flip2.get(0, 1, 1)).isEqualTo(5);
    assertThat(flip2.get(0, 1, 0)).isEqualTo(6);

    expected = new double[] {3, 2, 1, 6, 5, 4};
    count = 0;
    for (double val : flip2) {
      assertThat(val).isEqualTo(expected[count]);
      count++;
    }
  }

  @Test
  public void testPermute() {
    int[] permute = new int[] {0, 2, 1};
    Array<Double> pArray = Arrays.permute(array, permute);
    assertThat(pArray.get(0, 0, 0)).isEqualTo(1);
    assertThat(pArray.get(0, 1, 0)).isEqualTo(2);
    assertThat(pArray.get(0, 2, 0)).isEqualTo(3);
    assertThat(pArray.get(0, 0, 1)).isEqualTo(4);
    assertThat(pArray.get(0, 1, 1)).isEqualTo(5);
    assertThat(pArray.get(0, 2, 1)).isEqualTo(6);

    permute = new int[] {2, 1, 0};
    pArray = Arrays.permute(array, permute);
    assertThat(pArray.get(0, 0, 0)).isEqualTo(1);
    assertThat(pArray.get(1, 0, 0)).isEqualTo(2);
    assertThat(pArray.get(2, 0, 0)).isEqualTo(3);
    assertThat(pArray.get(0, 1, 0)).isEqualTo(4);
    assertThat(pArray.get(1, 1, 0)).isEqualTo(5);
    assertThat(pArray.get(2, 1, 0)).isEqualTo(6);

    permute = new int[] {2, 0, 1};
    pArray = Arrays.permute(array, permute);
    assertThat(pArray.get(0, 0, 0)).isEqualTo(1);
    assertThat(pArray.get(1, 0, 0)).isEqualTo(2);
    assertThat(pArray.get(2, 0, 0)).isEqualTo(3);
    assertThat(pArray.get(0, 0, 1)).isEqualTo(4);
    assertThat(pArray.get(1, 0, 1)).isEqualTo(5);
    assertThat(pArray.get(2, 0, 1)).isEqualTo(6);

    assertThrows(IllegalArgumentException.class, () -> {
      int[] permute2 = new int[] {0, 2, 3};
      Arrays.permute(array, permute2);
    });

    assertThrows(IllegalArgumentException.class, () -> {
      int[] permute2 = new int[] {0, 2, 2};
      Arrays.permute(array, permute2);
    });
  }

  @Test
  public void testReshape() {
    int[] reshape = new int[] {3, 2, 1};
    Array<Double> pArray = Arrays.reshape(array, reshape);
    assertThat(pArray.get(0, 0, 0)).isEqualTo(1);
    assertThat(pArray.get(0, 1, 0)).isEqualTo(2);
    assertThat(pArray.get(1, 0, 0)).isEqualTo(3);
    assertThat(pArray.get(1, 1, 0)).isEqualTo(4);
    assertThat(pArray.get(2, 0, 0)).isEqualTo(5);
    assertThat(pArray.get(2, 1, 0)).isEqualTo(6);

    reshape = new int[] {6, 1, 1};
    pArray = Arrays.reshape(array, reshape);
    assertThat(pArray.get(0, 0, 0)).isEqualTo(1);
    assertThat(pArray.get(1, 0, 0)).isEqualTo(2);
    assertThat(pArray.get(2, 0, 0)).isEqualTo(3);
    assertThat(pArray.get(3, 0, 0)).isEqualTo(4);
    assertThat(pArray.get(4, 0, 0)).isEqualTo(5);
    assertThat(pArray.get(5, 0, 0)).isEqualTo(6);

    reshape = new int[] {2, 3, 1};
    pArray = Arrays.reshape(array, reshape);
    assertThat(pArray.get(0, 0, 0)).isEqualTo(1);
    assertThat(pArray.get(0, 1, 0)).isEqualTo(2);
    assertThat(pArray.get(0, 2, 0)).isEqualTo(3);
    assertThat(pArray.get(1, 0, 0)).isEqualTo(4);
    assertThat(pArray.get(1, 1, 0)).isEqualTo(5);
    assertThat(pArray.get(1, 2, 0)).isEqualTo(6);

    assertThrows(IllegalArgumentException.class, () -> {
      int[] reshape2 = new int[] {2, 2, 2};
      Arrays.reshape(array, reshape2);
    });
  }

  @Test
  public void testReduceDim() {
    Array<Double> rArray = Arrays.reduce(array, 0);
    assertThat(rArray.getShape()).isEqualTo(new int[] {2, 3});

    assertThrows(IllegalArgumentException.class, () -> Arrays.reduce(array, 1));
    assertThrows(IllegalArgumentException.class, () -> Arrays.reduce(array, 3));

    int[] reshape = new int[] {3, 2, 1};
    Array<Double> pArray = Arrays.reshape(array, reshape);
    rArray = Arrays.reduce(pArray, 2);
    assertThat(rArray.getShape()).isEqualTo(new int[] {3, 2});
  }

  @Test
  public void testReduce() {
    Array<Double> rArray = Arrays.reduce(array);
    assertThat(rArray.getShape()).isEqualTo(new int[] {2, 3});

    int[] reshape = new int[] {3, 2, 1};
    Array<Double> pArray = Arrays.reshape(array, reshape);
    rArray = Arrays.reduce(pArray);
    assertThat(rArray.getShape()).isEqualTo(new int[] {3, 2});
  }

  @Test
  public void testReduceFirst() {
    Array<Double> rArray0 = Arrays.reduceFirst(array, 0);
    assertThat(rArray0.getShape()).isEqualTo(new int[] {1, 2, 3});

    Array<Double> rArray1 = Arrays.reduceFirst(array, 1);
    assertThat(rArray1.getShape()).isEqualTo(new int[] {2, 3});

    Array<Double> rArray2 = Arrays.reduceFirst(array, 2);
    assertThat(rArray2.getShape()).isEqualTo(new int[] {2, 3});
  }

  @Test
  public void testSection() throws InvalidRangeException {
    Section.Builder sb = Section.builder();
    sb.appendRange(null);
    sb.appendRange(null);
    sb.appendRange(new Range(2));
    Array<Double> pArray = Arrays.section(array, sb.build());
    assertThat(pArray.getShape()).isEqualTo(new int[] {1, 2, 2});

    assertThat(pArray.get(0, 0, 0)).isEqualTo(1);
    assertThat(pArray.get(0, 0, 1)).isEqualTo(2);
    assertThat(pArray.get(0, 1, 0)).isEqualTo(4);
    assertThat(pArray.get(0, 1, 1)).isEqualTo(5);

    assertThrows(IllegalArgumentException.class, () -> pArray.get(0, 0, 2));

    double total = 0.0;
    for (double val : pArray) {
      total += val;
    }
    assertThat(total).isEqualTo(12.0);

    Array<Double> rArray = Arrays.reduce(pArray);
    assertThat(rArray.getShape()).isEqualTo(new int[] {2, 2});

    assertThat(rArray.get(0, 0)).isEqualTo(1);
    assertThat(rArray.get(0, 1)).isEqualTo(2);
    assertThat(rArray.get(1, 0)).isEqualTo(4);
    assertThat(rArray.get(1, 1)).isEqualTo(5);

    assertThrows(IllegalArgumentException.class, () -> pArray.get(0, 2));
  }

  @Test
  public void testSectionStrided() throws InvalidRangeException {
    Section.Builder sb = Section.builder();
    sb.appendRange(null);
    sb.appendRange(null);
    sb.appendRange(new Range(0, 2, 2));
    Array<Double> pArray = Arrays.section(array, sb.build());
    assertThat(pArray.getShape()).isEqualTo(new int[] {1, 2, 2});

    assertThat(pArray.get(0, 0, 0)).isEqualTo(1);
    assertThat(pArray.get(0, 0, 1)).isEqualTo(3);
    assertThat(pArray.get(0, 1, 0)).isEqualTo(4);
    assertThat(pArray.get(0, 1, 1)).isEqualTo(6);

    try {
      assertThat(pArray.get(0, 0, 2)).isEqualTo(3);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }


    double total = 0.0;
    for (double val : pArray) {
      total += val;
    }
    assertThat(total).isEqualTo(14.0);

    Array<Double> rArray = Arrays.reduce(pArray);
    assertThat(rArray.getShape()).isEqualTo(new int[] {2, 2});

    assertThat(rArray.get(0, 0)).isEqualTo(1);
    assertThat(rArray.get(0, 1)).isEqualTo(3);
    assertThat(rArray.get(1, 0)).isEqualTo(4);
    assertThat(rArray.get(1, 1)).isEqualTo(6);

    try {
      assertThat(rArray.get(0, 2)).isEqualTo(3);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  public void testSlice() throws InvalidRangeException {
    Array<Double> pArray = Arrays.slice(array, 2, 2);
    assertThat(pArray.getShape()).isEqualTo(new int[] {1, 2});

    assertThat(pArray.get(0, 0)).isEqualTo(3);
    assertThat(pArray.get(0, 1)).isEqualTo(6);

    double total = 0.0;
    for (double val : pArray) {
      total += val;
    }
    assertThat(total).isEqualTo(9.0);
  }

  @Test
  public void testTranspose() {
    Array<Double> pArray = Arrays.transpose(array, 0, 2);
    assertThat(pArray.get(0, 0, 0)).isEqualTo(1);
    assertThat(pArray.get(1, 0, 0)).isEqualTo(2);
    assertThat(pArray.get(2, 0, 0)).isEqualTo(3);
    assertThat(pArray.get(0, 1, 0)).isEqualTo(4);
    assertThat(pArray.get(1, 1, 0)).isEqualTo(5);
    assertThat(pArray.get(2, 1, 0)).isEqualTo(6);

    pArray = Arrays.transpose(array, 0, 1);
    assertThat(pArray.get(0, 0, 0)).isEqualTo(1);
    assertThat(pArray.get(0, 0, 1)).isEqualTo(2);
    assertThat(pArray.get(0, 0, 2)).isEqualTo(3);
    assertThat(pArray.get(1, 0, 0)).isEqualTo(4);
    assertThat(pArray.get(1, 0, 1)).isEqualTo(5);
    assertThat(pArray.get(1, 0, 2)).isEqualTo(6);

    double total = 0.0;
    for (double val : pArray) {
      total += val;
    }
    assertThat(total).isEqualTo(21.0);
  }

  @Test
  public void testSums() {
    int[] shape = new int[] {1, 2, 3};
    int[] parray = new int[] {1, 2, 3, 4, 5, 6};
    Array<?> array = Arrays.factory(ArrayType.INT, shape, parray);

    assertThat(Arrays.sumDouble(array)).isEqualTo(21);
    Array<Double> darray = Arrays.toDouble(array);
    assertThat(Arrays.sumDouble(darray)).isEqualTo(21);
    Array<Double> darray2 = Arrays.toDouble(darray);
    assertThat(Arrays.sumDouble(darray2)).isEqualTo(21);
  }

  @Test
  public void testMinMaxSkipMissingDataDouble() {
    MinMax minmax = Arrays.getMinMaxSkipMissingData(array, null);
    assertThat(minmax.min()).isEqualTo(1.0);
    assertThat(minmax.max()).isEqualTo(6.0);

    MinMax minmax2 = Arrays.getMinMaxSkipMissingData(array, new IsMissingEvaluator() {
      public boolean hasMissing() {
        return false;
      }

      public boolean isMissing(double val) {
        return val == 1.0 || val == 6.0;
      }
    });
    assertThat(minmax2.min()).isEqualTo(1.0);
    assertThat(minmax2.max()).isEqualTo(6.0);

    MinMax minmax3 = Arrays.getMinMaxSkipMissingData(array, new IsMissingEvaluator() {
      public boolean hasMissing() {
        return true;
      }

      public boolean isMissing(double val) {
        return val == 1.0 || val == 6.0;
      }
    });
    assertThat(minmax3.min()).isEqualTo(2.0);
    assertThat(minmax3.max()).isEqualTo(5.0);
  }

  @Test
  public void testMinMaxSkipMissingDataNumber() {
    int[] shape = new int[] {1, 2, 3};
    int[] parray = new int[] {1, 2, 3, 4, 5, 6};
    Array<Number> narray = Arrays.factory(ArrayType.INT, shape, parray);

    MinMax minmax = Arrays.getMinMaxSkipMissingData(narray, null);
    assertThat(minmax.min()).isEqualTo(1.0);
    assertThat(minmax.max()).isEqualTo(6.0);
    assertThat(minmax.toString()).isEqualTo("MinMax[min=1.0, max=6.0]");

    MinMax minmax2 = Arrays.getMinMaxSkipMissingData(narray, new IsMissingEvaluator() {
      public boolean hasMissing() {
        return false;
      }

      public boolean isMissing(double val) {
        return val == 1.0 || val == 6.0;
      }
    });
    assertThat(minmax2.min()).isEqualTo(1.0);
    assertThat(minmax2.max()).isEqualTo(6.0);

    MinMax minmax3 = Arrays.getMinMaxSkipMissingData(narray, new IsMissingEvaluator() {
      public boolean hasMissing() {
        return true;
      }

      public boolean isMissing(double val) {
        return val == 1.0 || val == 6.0;
      }
    });
    assertThat(minmax3.min()).isEqualTo(2.0);
    assertThat(minmax3.max()).isEqualTo(5.0);
  }

  @Test
  public void testEqualNumbers() {
    int[] shape = new int[] {1, 2, 3};
    int[] parray = new int[] {1, 2, 3, 4, 5, 6};
    Array<Number> array = Arrays.factory(ArrayType.INT, shape, parray);

    Array darray = Arrays.toDouble(array);
    assertThat(Arrays.equalNumbers(array, (Array<Number>) darray)).isTrue();
  }

  @Test
  public void testEqualDoubles() {
    int[] shape = new int[] {1, 2, 3};
    double[] parray = new double[] {1, 2, 3, 4, 5, 6};
    Array<Double> array = Arrays.factory(ArrayType.DOUBLE, shape, parray);

    Array<Double> darray = Arrays.toDouble(array);
    assertThat(Arrays.equalDoubles(array, darray)).isTrue();
  }

  @Test
  public void testEqualFloats() {
    int[] shape = new int[] {1, 2, 3};
    float[] parray = new float[] {1, 2, 3, 4, 5, 6};
    Array<Float> array = Arrays.factory(ArrayType.FLOAT, shape, parray);

    float[] fparray = new float[] {1, 2, 3, 4, 5, 6.0001f};
    Array<Float> farray = Arrays.factory(ArrayType.FLOAT, shape, fparray);
    assertThat(Arrays.equalFloats(array, farray)).isFalse();
  }

  @Test
  public void testMakeStrings() {
    byte[] barray = "What?".getBytes(StandardCharsets.UTF_8);
    Array<Byte> array = Arrays.factory(ArrayType.BYTE, new int[] {barray.length}, barray);
    assertThat(Arrays.makeStringFromChar(array)).isEqualTo("What?");

    byte[] barray2 = "Whats?".getBytes(StandardCharsets.UTF_8);
    Array<Byte> array2 = Arrays.factory(ArrayType.BYTE, new int[] {2, 3}, barray2);
    Array<String> sarrays = Arrays.makeStringsFromChar(array2);
    assertThat(sarrays.get(0)).isEqualTo("Wha");
    assertThat(sarrays.get(1)).isEqualTo("ts?");
  }

  @Test
  public void testMakeArray() {
    assertThat(Arrays.makeArray(ArrayType.LONG, List.of("9", Long.toString(Long.MAX_VALUE), Long.toString(Long.MIN_VALUE))))
            .isEqualTo(Arrays.factory(ArrayType.LONG, new int[] {3}, new long[] {9, Long.MAX_VALUE, Long.MIN_VALUE}));
    assertThat(Arrays.makeArray(ArrayType.ULONG, List.of("9", Long.toString(Long.MAX_VALUE), Long.toString(Long.MIN_VALUE))))
            .isEqualTo(Arrays.factory(ArrayType.ULONG, new int[] {3}, new long[] {9, Long.MAX_VALUE, Long.MIN_VALUE}));

    assertThat(Arrays.makeArray(ArrayType.INT, List.of("-9", Integer.toString(Integer.MAX_VALUE), Integer.toString(Integer.MIN_VALUE))))
            .isEqualTo(Arrays.factory(ArrayType.INT, new int[] {3}, new int[] {-9, Integer.MAX_VALUE, Integer.MIN_VALUE}));
    assertThat(Arrays.makeArray(ArrayType.UINT, List.of("-9", Integer.toString(Integer.MAX_VALUE), Integer.toString(Integer.MIN_VALUE))))
            .isEqualTo(Arrays.factory(ArrayType.UINT, new int[] {3}, new int[] {-9, Integer.MAX_VALUE, Integer.MIN_VALUE}));

    assertThat(Arrays.makeArray(ArrayType.SHORT, List.of("-9", Short.toString(Short.MAX_VALUE), Short.toString(Short.MIN_VALUE))))
            .isEqualTo(Arrays.factory(ArrayType.SHORT, new int[] {3}, new short[] {-9, Short.MAX_VALUE, Short.MIN_VALUE}));
    assertThat(Arrays.makeArray(ArrayType.USHORT, List.of("-9", Short.toString(Short.MAX_VALUE), Short.toString(Short.MIN_VALUE))))
            .isEqualTo(Arrays.factory(ArrayType.USHORT, new int[] {3}, new short[] {-9, Short.MAX_VALUE, Short.MIN_VALUE}));

    assertThat(Arrays.makeArray(ArrayType.BYTE, List.of("-9", Byte.toString(Byte.MAX_VALUE), Byte.toString(Byte.MIN_VALUE))))
            .isEqualTo(Arrays.factory(ArrayType.BYTE, new int[] {3}, new byte[] {-9, Byte.MAX_VALUE, Byte.MIN_VALUE}));
    assertThat(Arrays.makeArray(ArrayType.UBYTE, List.of("-9", Byte.toString(Byte.MAX_VALUE), Byte.toString(Byte.MIN_VALUE))))
            .isEqualTo(Arrays.factory(ArrayType.UBYTE, new int[] {3}, new byte[] {-9, Byte.MAX_VALUE, Byte.MIN_VALUE}));

    assertThat(Arrays.makeArray(ArrayType.DOUBLE, List.of(".1", Long.toString(Long.MAX_VALUE), Long.toString(Long.MIN_VALUE))))
            .isEqualTo(Arrays.factory(ArrayType.DOUBLE, new int[] {3}, new double[] {.1, Long.MAX_VALUE, Long.MIN_VALUE}));
    assertThat(Arrays.makeArray(ArrayType.FLOAT, List.of(".1", Long.toString(Long.MAX_VALUE), Long.toString(Long.MIN_VALUE))))
            .isEqualTo(Arrays.factory(ArrayType.FLOAT, new int[] {3}, new float[] {.1f, Long.MAX_VALUE, Long.MIN_VALUE}));
  }

}
