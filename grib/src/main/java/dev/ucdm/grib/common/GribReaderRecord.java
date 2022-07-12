/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.grib.common;

import javax.annotation.Nonnull;
import dev.cdm.grid.api.GridSubset;
import dev.ucdm.grib.collection.GribCollection;

class GribReaderRecord implements Comparable<GribReaderRecord> {
  int resultIndex; // index into the result array
  final GribCollection.ReadRecord record;
  final GdsHorizCoordSys hcs;
  GridSubset validation; // debugging I think

  GribReaderRecord(int resultIndex, GribCollection.ReadRecord record, GdsHorizCoordSys hcs) {
    this.resultIndex = resultIndex;
    this.record = record;
    this.hcs = hcs;
  }

  public void setResultIndex(int resultIndex) {
    this.resultIndex = resultIndex;
  }

  @Override
  public int compareTo(@Nonnull GribReaderRecord o) {
    int r = Integer.compare(record.fileno(), o.record.fileno());
    if (r != 0) {
      return r;
    }
    return Long.compare(record.pos(), o.record.pos());
  }

  // debugging
  public void show(GribCollection gribCollection) {
    String dataFilename = gribCollection.getFilename(record.fileno());
    System.out.printf(" fileno=%d filename=%s startPos=%d%n", record.fileno(), dataFilename, record.pos());
  }
}
