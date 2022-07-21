/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.grib.inventory;

import dev.ucdm.grib.collection.CollectionUpdateType;

import java.io.IOException;

/** Manages time partitions of MCollections */
public interface MPartition extends MCollection {

  Iterable<MCollection> makeCollections(CollectionUpdateType forceCollection) throws IOException;

  void removeCollection(MCollection child);

}
