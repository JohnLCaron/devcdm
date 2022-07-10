/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.grib1.iosp;

import dev.cdm.core.http.RafHttp;
import dev.cdm.core.constants.DataFormatType;
import dev.cdm.core.io.RandomAccessFile;
import dev.ucdm.grib.collection.CollectionType;
import dev.ucdm.grib.collection.GribCollection;
import dev.ucdm.grib.collection.VariableIndex;
import dev.ucdm.grib.common.GribStatType;
import dev.ucdm.grib.common.GribTables;
import dev.ucdm.grib.common.util.GribNumbers;
import dev.ucdm.grib.common.util.GribUtils;
import dev.ucdm.grib.grib1.record.Grib1Record;
import dev.ucdm.grib.grib1.record.Grib1RecordScanner;
import dev.ucdm.grib.grib1.record.Grib1SectionProductDefinition;
import dev.ucdm.grib.grib1.tables.Grib1Customizer;
import dev.ucdm.grib.grib1.tables.Grib1ParamTables;
import dev.ucdm.grib.common.GribCollectionIndex;
import dev.ucdm.grib.common.GribConfig;
import dev.ucdm.grib.common.GribIosp;

import java.io.IOException;
import java.util.Formatter;

/**
 * Grib-1 Collection IOSP.
 * Handles both collections and single GRIB files.
 *
 * @author caron
 * @since 4/6/11
 */
public class Grib1Iosp extends GribIosp {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib1Iosp.class);

  @Override
  public String makeVariableName(VariableIndex v) {
    return makeVariableNameFromTables(gribCollection.getCenter(), gribCollection.getSubcenter(), v.getTableVersion(),
        v.getParameter(), v.getLevelType(), v.isLayer(), v.getIntvType(), v.getIntvName());
  }

  public static String makeVariableName(Grib1Customizer cust, GribConfig gribConfig,
                                        Grib1SectionProductDefinition pds) {
    return makeVariableNameFromTables(cust, gribConfig, pds.getCenter(), pds.getSubCenter(), pds.getTableVersion(),
        pds.getParameterNumber(), pds.getLevelType(), cust.isLayer(pds.getLevelType()), pds.getTimeRangeIndicator(),
        null);
  }

  private String makeVariableNameFromTables(int center, int subcenter, int version, int paramNo, int levelType,
      boolean isLayer, int intvType, String intvName) {
    return makeVariableNameFromTables(cust, gribConfig, center, subcenter, version, paramNo, levelType, isLayer,
        intvType, intvName);
  }

  private static String makeVariableNameFromTables(Grib1Customizer cust, GribConfig gribConfig,
      int center, int subcenter, int version, int paramNo, int levelType, boolean isLayer, int timeRangeIndicator,
      String intvName) {
    try (Formatter f = new Formatter()) {

      Grib1Parameter param = cust.getParameter(center, subcenter, version, paramNo); // code table 2
      if (param == null) {
        f.format("VAR%d-%d-%d-%d", center, subcenter, version, paramNo);
      } else {
        if (param.useName()) {
          f.format("%s", param.getName());
        } else {
          f.format("%s", GribUtils.makeNameFromDescription(param.getDescription()));
        }
      }

      if (gribConfig.useTableVersion) {
        f.format("_TableVersion%d", version);
      }

      if (gribConfig.useCenter) {
        f.format("_Center%d", center);
      }

      if (levelType != GribNumbers.UNDEFINED) { // satellite data doesnt have a level
        f.format("_%s", cust.getLevelNameShort(levelType)); // code table 3
        if (isLayer) {
          f.format("_layer");
        }
      }

      if (timeRangeIndicator >= 0) {
        GribStatType stat = cust.getStatType(timeRangeIndicator);
        if (stat != null) {
          if (intvName != null) {
            f.format("_%s", intvName);
          }
          f.format("_%s", stat.name());
        } else {
          if (intvName != null) {
            f.format("_%s", intvName);
          }
          // f.format("_%d", timeRangeIndicator);
        }
      }

      return f.toString();
    }
  }

  @Override
  public String makeVariableLongName(VariableIndex v) {
    return makeVariableLongName(gribCollection.getCenter(), gribCollection.getSubcenter(), v.getTableVersion(),
        v.getParameter(), v.getLevelType(), v.isLayer(), v.getIntvType(), v.getIntvName(), v.getProbabilityName());
  }


  private String makeVariableLongName(int center, int subcenter, int version, int paramNo, int levelType,
      boolean isLayer, int intvType, String intvName, String probabilityName) {
    return makeVariableLongName(cust, center, subcenter, version, paramNo, levelType, isLayer, intvType, intvName,
        probabilityName);
  }

  static String makeVariableLongName(Grib1Customizer cust, int center, int subcenter, int version, int paramNo,
      int levelType, boolean isLayer, int intvType, String intvName, String probabilityName) {
    try (Formatter f = new Formatter()) {

      boolean isProb = (probabilityName != null && !probabilityName.isEmpty());
      if (isProb) {
        f.format("Probability ");
      }

      Grib1Parameter param = cust.getParameter(center, subcenter, version, paramNo);
      if (param == null) {
        f.format("Unknown Parameter %d-%d-%d-%d", center, subcenter, version, paramNo);
      } else {
        f.format("%s", param.getDescription());
      }

      if (intvType >= 0) {
        GribStatType stat = cust.getStatType(intvType);
        if (stat != null) {
          f.format(" (%s %s)", intvName, stat.name());
        } else if (intvName != null && !intvName.isEmpty()) {
          f.format(" (%s)", intvName);
        }
      }

      if (levelType != GribNumbers.UNDEFINED) { // satellite data doesnt have a level
        f.format(" @ %s", cust.getLevelDescription(levelType));
        if (isLayer) {
          f.format(" layer");
        }
      }

      return f.toString();
    }
  }

  @Override
  public String makeVariableUnits(VariableIndex vindex) {
    return makeVariableUnits(gribCollection.getCenter(), gribCollection.getSubcenter(), vindex.getTableVersion(),
        vindex.getParameter());
  }

  private String makeVariableUnits(int center, int subcenter, int version, int paramNo) {
    Grib1Parameter param = cust.getParameter(center, subcenter, version, paramNo);
    String val = (param == null) ? "" : param.getUnit();
    return (val == null) ? "" : val;
  }

  static String makeVariableUnits(Grib1Customizer cust, GribCollection gribCollection,
      VariableIndex vindex) {
    Grib1Parameter param = cust.getParameter(gribCollection.getCenter(), gribCollection.getSubcenter(),
        vindex.getTableVersion(), vindex.getParameter());
    String val = (param == null) ? "" : param.getUnit();
    return (val == null) ? "" : val;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private Grib1Customizer cust;

  @Override
  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    if (raf instanceof RafHttp) { // only do remote if memory resident
      if (raf.length() > raf.getBufferSize())
        return false;

    } else { // wont accept remote index

      GribCollectionIndex.Type type = GribCollectionIndex.getType(raf);
      if (type == GribCollectionIndex.Type.GRIB1)
        return true;
      if (type == GribCollectionIndex.Type.Partition1)
        return true;
    }

    // check for GRIB1 data file
    return Grib1RecordScanner.isValidFile(raf);
  }

  @Override
  public String getCdmFileTypeId() {
    return DataFormatType.GRIB1.getDescription();
  }

  @Override
  public String getCdmFileTypeDescription() {
    return "GRIB1 Collection";
  }

  // public no-arg constructor for reflection
  public Grib1Iosp() {
    super(true, logger);
  }

  public Grib1Iosp(GribCollection.GroupGC gHcs, CollectionType gtype) {
    super(true, logger);
    this.gHcs = gHcs;
    this.owned = true;
    this.gtype = gtype;
  }

  public Grib1Iosp(GribCollection gc) {
    super(true, logger);
    this.gribCollection = gc;
    this.owned = true;
  }

  @Override
  public GribTables createCustomizer() throws IOException {
    // so, an iosp message must be received before the open()
    Grib1ParamTables tables =
        (gribConfig.paramTable != null) ? Grib1ParamTables.factory(gribConfig.paramTable)
            : Grib1ParamTables.factory(gribConfig.paramTablePath, gribConfig.lookupTablePath);

    cust = Grib1Customizer.factory(gribCollection.getCenter(), gribCollection.getSubcenter(),
        gribCollection.getMaster(), tables);
    return cust;
  }

  @Override
  public String getVerticalCoordDesc(int vc_code) {
    return cust.getLevelDescription(vc_code);
  }

  @Override
  protected GribTables.Parameter getParameter(VariableIndex vindex) {
    return cust.getParameter(gribCollection.getCenter(), gribCollection.getSubcenter(), gribCollection.getVersion(),
        vindex.getParameter());
  }

  //////////////////////////////////

  public Object getLastRecordRead() {
    return Grib1Record.lastRecordRead;
  }

  public void clearLastRecordRead() {
    Grib1Record.lastRecordRead = null;
  }

  public Object getGribCustomizer() {
    return cust;
  }
}
