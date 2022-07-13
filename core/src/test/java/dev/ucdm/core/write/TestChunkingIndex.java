/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.core.write;

import org.junit.jupiter.api.Test;
import dev.ucdm.array.Section;
import dev.ucdm.core.api.Dimension;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link ChunkingIndex} */
public class TestChunkingIndex {

  @Test
  public void testChunkingIndex() {
    testOne(new int[] {100, 100}, 500);
    testOne(new int[] {100, 100}, 499);
    testOne(new int[] {100, 100}, 100 * 100);
    testOne(new int[] {77, 3712, 2332}, 500 * 1000);
    testOne(new int[] {77, 3712, 2332}, 50 * 1000);
    testOne(new int[] {77, 3712, 2332}, 5 * 1000);
  }

  private void testOne(int[] shape, long maxChunkElems) {
    show("shape", shape);
    System.out.printf(" max = %d%n", maxChunkElems);
    ChunkingIndex index = new ChunkingIndex(shape);
    int[] result = index.computeChunkShape(maxChunkElems);
    show("chunk", result);
    long shapeSize = new Section(result).computeSize();
    System.out.printf(" size = %d%n%n", shapeSize);
    assertThat(shapeSize).isAtMost(maxChunkElems);
  }

  private void show(String what, int[] result) {
    System.out.printf("%s= (", what);
    for (int r : result)
      System.out.printf("%d,", r);
    System.out.printf(")%n");
  }

  private void show(String what, List<Dimension> dims) {
    System.out.printf("%s= (", what);
    for (Dimension r : dims) {
      System.out.printf("%d,", r.getLength());
    }
    System.out.printf(")%n");
  }
}
