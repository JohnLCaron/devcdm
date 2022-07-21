/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.grib.inventory;

import dev.ucdm.core.calendar.CalendarDate;
import dev.ucdm.grib.collection.CollectionUpdateType;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/** Manages time partitions of MCollections */
public interface MPartition {
  String getCollectionName();

  @Nullable
  CalendarDate getLastModified();

  /** Get common root directory of all MFiles in the collection */
  String getRoot();

  /** The corresponding ncx4 filename for this collection. */
  String getIndexFilename(String suffix);

  //// iteration
  interface Visitor {
    void visit(MCollection mfile);
  }

  void iterateOverMCollections(MPartition.Visitor visitor);

  void removeCollection(MCollection child);

}
