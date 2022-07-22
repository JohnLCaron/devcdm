/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.grib.inventory;

import dev.ucdm.core.calendar.CalendarDate;

/** Manages time partitions of MCollections */
public interface MPartition {
  String getCollectionName();

  CalendarDate getLastModified();

  /** Get common root directory of all MFiles in the collection */
  String getRoot();

  /** The corresponding ncx4 filename for this collection. */
  String getIndexFilename();

  boolean isPartitionOfPartition();

  // use if isPartitionOfPartition is false
  void iterateOverMCollections(MPartition.CVisitor visitor);
  interface CVisitor {
    void visit(MCollection mfile);
  }

  // use if isPartitionOfPartition is true
  void iterateOverMPartitions(MPartition.PVisitor visitor);
  interface PVisitor {
    void visit(MPartition mfile);
  }

  Object getAuxInfo(String key);
}
