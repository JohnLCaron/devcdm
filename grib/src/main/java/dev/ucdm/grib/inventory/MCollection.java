/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.grib.inventory;

import dev.ucdm.core.calendar.CalendarDate;
import org.jetbrains.annotations.Nullable;

public interface MCollection {
  String getCollectionName();

  @Nullable
  CalendarDate getLastModified();

  /** Get common root directory of all MFiles in the collection - TODO may be null? */
  @Nullable
  String getRoot();

  /** The corresponding ncx4 filename for this collection. */
  String getIndexFilename(String suffix);

  //// iteration
  interface Visitor {
    void visit(MFile mfile);
  }

  void iterateOverMFiles(Visitor visitor);

  ////////////////////////////////////////////////////
  // ability to pass arbitrary information to users of the mcollection .

  Object getAuxInfo(String key);

  // return this for chaining
  MCollection setAuxInfo(String key, Object value);
}
