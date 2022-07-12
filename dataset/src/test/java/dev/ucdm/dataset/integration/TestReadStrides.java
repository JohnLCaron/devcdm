/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.dataset.integration;

import dev.ucdm.core.api.CdmFile;
import dev.ucdm.core.api.Variable;
import dev.ucdm.dataset.api.CdmDatasets;
import org.junit.jupiter.api.Test;
import dev.ucdm.array.Array;
import dev.ucdm.array.Arrays;
import dev.ucdm.array.Index;
import dev.ucdm.array.InvalidRangeException;
import dev.ucdm.array.Range;
import dev.ucdm.array.Section;

import java.io.IOException;
import java.util.ArrayList;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.test.util.TestFilesKt.datasetLocalNcmlDir;

/** Test reading variable data */
public class TestReadStrides {

  @Test
  public void testReadStridesCached() throws Exception {
    String filename = datasetLocalNcmlDir + "nc/time0.nc";
    try (CdmFile ncfile = CdmDatasets.openDataset(filename)) {
      Variable temp = ncfile.findVariable("T");
      assertThat(temp).isNotNull();

      // read entire array
      Array<Double> A = (Array<Double>) temp.readArray(new Section("0:2,0:3"));
      assertThat(A.getRank()).isEqualTo(2);

      Index ima = A.getIndex();
      int[] shape = A.getShape();
      assertThat(shape[0]).isEqualTo(3);
      assertThat(shape[1]).isEqualTo(4);

      for (int i = 0; i < shape[0]; i++) {
        for (int j = 0; j < shape[1]; j++) {
          double dval = A.get(ima.set(i, j));
          assertThat(dval).isEqualTo((double) (i * 10 + j));
        }
      }

      A = (Array<Double>) temp.readArray(new Section("0:2:1,0:3:1"));
      assertThat(A.getRank()).isEqualTo(2);

      ima = A.getIndex();
      shape = A.getShape();
      assertThat(shape[0]).isEqualTo(3);
      assertThat(shape[1]).isEqualTo(4);

      for (int i = 0; i < shape[0]; i++) {
        for (int j = 0; j < shape[1]; j++) {
          double dval = A.get(ima.set(i, j));
          assertThat(dval).isEqualTo((double) (i * 10 + j));
        }
      }

      A = (Array<Double>) temp.readArray(new Section("0:2:2,0:3:2"));
      assertThat(A.getRank()).isEqualTo(2);

      ima = A.getIndex();
      shape = A.getShape();
      assertThat(shape[0]).isEqualTo(2);
      assertThat(shape[1]).isEqualTo(2);

      for (int i = 0; i < shape[0]; i++) {
        for (int j = 0; j < shape[1]; j++) {
          double dval = A.get(ima.set(i, j));
          assertThat(dval).isEqualTo((double) (i * 20 + j * 2));
        }
      }

      A = (Array<Double>) temp.readArray(new Section(":,0:3:2"));
      assertThat(A.getRank()).isEqualTo(2);

      ima = A.getIndex();
      shape = A.getShape();
      assertThat(shape[0]).isEqualTo(3);
      assertThat(shape[1]).isEqualTo(2);

      for (int i = 0; i < shape[0]; i++) {
        for (int j = 0; j < shape[1]; j++) {
          double dval = A.get(ima.set(i, j));
          assertThat(dval).isEqualTo((double) (i * 10 + j * 2));
        }
      }

      A = (Array<Double>) temp.readArray(new Section("0:2:2,:"));
      assertThat(A.getRank()).isEqualTo(2);

      ima = A.getIndex();
      shape = A.getShape();
      assertThat(shape[0]).isEqualTo(2);
      assertThat(shape[1]).isEqualTo(4);

      for (int i = 0; i < shape[0]; i++) {
        for (int j = 0; j < shape[1]; j++) {
          double dval = A.get(ima.set(i, j));
          assertThat(dval).isEqualTo((double) (i * 20 + j));
        }
      }
    }
  }

  @Test
  public void testReadStridesNoCache() throws Exception {
    String filename = datasetLocalNcmlDir + "nc/time0.nc";
    try (CdmFile ncfile = CdmDatasets.openDataset(filename)) {
      Variable temp = ncfile.findVariable("T");
      assertThat(temp).isNotNull();
      temp.setCaching(false);

      Array<Double> A = (Array<Double>) temp.readArray(new Section("0:2:1,0:3:1"));
      assertThat(A.getRank()).isEqualTo(2);

      Index ima = A.getIndex();
      int[] shape = A.getShape();
      assertThat(shape[0]).isEqualTo(3);
      assertThat(shape[1]).isEqualTo(4);

      for (int i = 0; i < shape[0]; i++) {
        for (int j = 0; j < shape[1]; j++) {
          double dval = A.get(ima.set(i, j));
          assertThat(dval).isEqualTo((double) (i * 10 + j));
        }
      }

      A = (Array<Double>) temp.readArray(new Section("0:2:2,0:3:2"));
      assertThat(A.getRank()).isEqualTo(2);

      ima = A.getIndex();
      shape = A.getShape();
      assertThat(shape[0]).isEqualTo(2);
      assertThat(shape[1]).isEqualTo(2);

      for (int i = 0; i < shape[0]; i++) {
        for (int j = 0; j < shape[1]; j++) {
          double dval = A.get(ima.set(i, j));
          assertThat(dval).isEqualTo((double) (i * 20 + j * 2));
        }
      }

      A = (Array<Double>) temp.readArray(new Section(":,0:3:2"));
      assertThat(A.getRank()).isEqualTo(2);

      ima = A.getIndex();
      shape = A.getShape();
      assertThat(shape[0]).isEqualTo(3);
      assertThat(shape[1]).isEqualTo(2);

      for (int i = 0; i < shape[0]; i++) {
        for (int j = 0; j < shape[1]; j++) {
          double dval = A.get(ima.set(i, j));
          assertThat(dval).isEqualTo((double) (i * 10 + j * 2));
        }
      }

      A = (Array<Double>) temp.readArray(new Section("0:2:2,:"));
      assertThat(A.getRank()).isEqualTo(2);

      ima = A.getIndex();
      shape = A.getShape();
      assertThat(shape[0]).isEqualTo(2);
      assertThat(shape[1]).isEqualTo(4);

      for (int i = 0; i < shape[0]; i++) {
        for (int j = 0; j < shape[1]; j++) {
          double dval = A.get(ima.set(i, j));
          assertThat(dval).isEqualTo((double) (i * 20 + j));
        }
      }
    }
  }

  @Test
  public void testReadStridesAll() throws Exception {
    String filename = datasetLocalNcmlDir + "nc/time0.nc";

      testReadStrides(filename);
  }

  private void testReadStrides(String filename) throws IOException, InvalidRangeException {
    try (CdmFile ncfile = CdmDatasets.openDataset(filename)) {
      for (Variable v : ncfile.getVariables()) {
        if (v.getRank() == 0) {
          continue;
        }
        if (!v.hasCachedData())
          v.setCaching(false);
        testVariableReadStrides(v);
      }
    }
  }

  private void testVariableReadStrides(Variable v) throws IOException, InvalidRangeException {
    Array<Double> allData = (Array<Double>) v.readArray();

    int[] shape = v.getShape();
    if (shape.length < 5) {
      return;
    }
    for (int first = 0; first < 3; first++) {
      for (int stride = 2; stride < 5; stride++) {
        ArrayList<Range> ranges = new ArrayList<>();
        for (int value : shape) {
          int last = value - 1;
          Range r = new Range(first, last, stride);
          ranges.add(r);
        }

        System.out.println(v.getFullName() + " test range= " + new Section(ranges));
        Array<Double> sectionRead = (Array<Double>) v.readArray(new Section(ranges));
        Array<Double> sectionMake = Arrays.section(allData, new Section(ranges));
        assertThat(Arrays.equalDoubles(sectionRead, sectionMake)).isTrue();
      }
    }
  }

}
