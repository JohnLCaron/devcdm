/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.protoconvert;

import dev.ucdm.grib.common.GdsHorizCoordSys;

import dev.ucdm.array.Immutable;

/**
 * Encapsolates the GdsHorizCoordSys; shared by the GribCollection.GroupGC
 * TODO: review, is this needed?
 */
@Immutable
public class GribHorizCoordSystem {
  private final GdsHorizCoordSys hcs;
  private final byte[] rawGds; // raw gds: Grib1SectionGridDefinition or Grib2SectionGridDefinition
  private final Object gdsHash;
  private final String id, description;
  private final int predefinedGridDefinition; // grib1

  public GribHorizCoordSystem(GdsHorizCoordSys hcs, byte[] rawGds, Object gdsHash, String id, String description,
                              int predefinedGridDefinition) {
    this.hcs = hcs;
    this.rawGds = rawGds;
    this.gdsHash = gdsHash;
    this.predefinedGridDefinition = predefinedGridDefinition;

    this.id = id;
    this.description = description;
  }

  public GdsHorizCoordSys getHcs() {
    return hcs;
  }

  public byte[] getRawGds() {
    return rawGds;
  }

  // use this object for hashmaps
  public Object getGdsHash() {
    return gdsHash;
  }

  // unique name for Group
  public String getId() {
    return id;
  }

  // human readable
  public String getDescription() {
    return description;
  }

  public int getPredefinedGridDefinition() {
    return predefinedGridDefinition;
  }

}
