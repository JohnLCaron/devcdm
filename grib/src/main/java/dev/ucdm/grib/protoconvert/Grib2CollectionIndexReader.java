/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.protoconvert;

import dev.ucdm.grib.collection.GribCollection;
import dev.ucdm.grib.common.GdsHorizCoordSys;
import dev.ucdm.grib.common.GribTables;
import dev.ucdm.grib.common.GribConfig;
import dev.ucdm.grib.grib2.record.Grib2Gds;
import dev.ucdm.grib.grib2.record.Grib2SectionGridDefinition;
import dev.ucdm.grib.grib2.table.Grib2Tables;
import dev.ucdm.grib.protogen.GribCollectionProto;

/**
 * Build a GribCollection object for Grib-2 files. Only from ncx files.
 * No updating, no nuthin.
 * Data file is not opened.
 *
 * @author caron
 * @since 11/9/13
 */
public class Grib2CollectionIndexReader extends GribCollectionIndexReader {
  static final int minVersion = 1; // increment this when you want to force index rebuild
  protected static final int version = 3; // increment this as needed, must be backwards compatible through minVersion

  protected Grib2Tables cust; // gets created in readIndex, after center etc is read in

  public Grib2CollectionIndexReader(GribCollection gc, GribConfig config) {
    super(gc, config);
  }

  @Override
  protected int getVersion() {
    return version;
  }

  @Override
  protected int getMinVersion() {
    return minVersion;
  }

  @Override
  protected String getMagicStart() {
    return Grib2CollectionIndexWriter.MAGIC_START;
  }

  @Override
  protected GribTables makeCustomizer() {
    this.cust = Grib2Tables.factory(gc.center, gc.subcenter, gc.master, gc.local, gc.genProcessId);
    return this.cust;
  }

  @Override
  protected String getLevelNameShort(int levelCode) {
    return cust.getLevelNameShort(levelCode);
  }

  @Override
  protected GribHorizCoordSystem importGribHorizCoordSystem(GribCollectionProto.Gds proto) {
    byte[] rawGds = proto.getGds().toByteArray();
    Grib2SectionGridDefinition gdss = new Grib2SectionGridDefinition(rawGds);
    Grib2Gds gds = gdss.getGDS();
    GdsHorizCoordSys hcs = gds.makeHorizCoordSys();

    String hcsName = makeHorizCoordSysName(hcs);

    // check for user defined group names
    String desc = null;
    if (gribConfig.gdsNamer != null) {
      desc = gribConfig.gdsNamer.get(gds.hashCode());
    }
    if (desc == null) {
      desc = hcs.makeDescription(); // default desc
    }

    return new GribHorizCoordSystem(hcs, rawGds, gds, hcsName, desc, -1);
  }

}

