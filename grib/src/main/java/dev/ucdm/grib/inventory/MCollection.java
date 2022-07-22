/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.grib.inventory;

import dev.ucdm.core.calendar.CalendarDate;
import org.jetbrains.annotations.Nullable;

/** Manages collections of MFiles */
public interface MCollection {
  String getCollectionName();

  CalendarDate getLastModified();

  /** Get common root directory of all MFiles in the collection */
  String getRoot();

  /** The corresponding ncx4 filename for this collection. */
  String getIndexFilename();

  //// iteration
  interface Visitor {
    void visit(MFile mfile);
  }

  void iterateOverMFiles(Visitor visitor);

  ////////////////////////////////////////////////////
  // ability to pass arbitrary information to users of the MCollection.
  Object getAuxInfo(String key);
}
