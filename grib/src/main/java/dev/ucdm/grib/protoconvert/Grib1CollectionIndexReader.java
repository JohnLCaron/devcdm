/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.protoconvert;

import dev.ucdm.grib.collection.GribCollection;
import dev.ucdm.grib.collection.GribHorizCoordSystem;
import dev.ucdm.grib.common.GdsHorizCoordSys;
import dev.ucdm.grib.common.GribConfig;
import dev.ucdm.grib.common.GribTables;
import dev.ucdm.grib.grib1.record.Grib1Gds;
import dev.ucdm.grib.grib1.record.Grib1GdsPredefined;
import dev.ucdm.grib.grib1.record.Grib1SectionGridDefinition;
import dev.ucdm.grib.grib1.table.Grib1Customizer;
import dev.ucdm.grib.grib1.table.Grib1ParamTables;
import dev.ucdm.grib.protogen.GribCollectionProto;

import java.io.IOException;

/**
 * Grib1-specific reading of ncx files.
 *
 * @author caron
 * @since 2/20/14
 */
public class Grib1CollectionIndexReader extends GribCollectionIndexReader {

  public Grib1CollectionIndexReader(GribCollection gc, GribConfig config) {
    super(gc, config);
  }

  protected Grib1Customizer cust; // gets created in readIndex, after center etc is read in

  protected int getVersion() {
    return Grib1CollectionIndexWriter.version;
  }

  protected int getMinVersion() {
    return Grib1CollectionIndexWriter.minVersion;
  }

  protected String getMagicStart() {
    return Grib1CollectionIndexWriter.MAGIC_START;
  }

  protected GribTables makeCustomizer() throws IOException {
    /* so an iosp message must be received before the open()*/
    Grib1ParamTables ptables =
        (gribConfig.paramTable != null) ? Grib1ParamTables.factory(gribConfig.paramTable)
            : Grib1ParamTables.factory(gribConfig.paramTablePath, gribConfig.lookupTablePath);
    this.cust = Grib1Customizer.factory(gc.center, gc.subcenter, gc.version, ptables);
    return cust;
  }

  protected String getLevelNameShort(int levelCode) {
    return cust.getLevelNameShort(levelCode);
  }

  @Override
  protected GribHorizCoordSystem readGds(GribCollectionProto.Gds p) {
    byte[] rawGds = null;
    Grib1Gds gds;
    int predefined = -1;
    if (p.getPredefinedGridDefinition() > 0) {
      predefined = p.getPredefinedGridDefinition();
      gds = Grib1GdsPredefined.factory(gc.center, predefined);
    } else {
      rawGds = p.getGds().toByteArray();
      Grib1SectionGridDefinition gdss = new Grib1SectionGridDefinition(rawGds);
      gds = gdss.getGDS();
    }

    GdsHorizCoordSys hcs = gds.makeHorizCoordSys();
    String hcsName = (hcs == null) ? gds.getClass().getName() : makeHorizCoordSysName(hcs);

    // check for user defined group names
    String desc = null;
    if (gribConfig.gdsNamer != null)
      desc = gribConfig.gdsNamer.get(gds.hashCode());
    if (desc == null)
      desc = (hcs == null) ? hcsName : hcs.makeDescription(); // default desc

    return new GribHorizCoordSystem(hcs, rawGds, gds, hcsName, desc, predefined);
  }

}
