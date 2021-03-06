/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.grib2.iosp;

import dev.ucdm.core.constants.DataFormatType;
import dev.ucdm.core.http.RafHttp;
import dev.ucdm.core.io.RandomAccessFile;
import dev.ucdm.grib.collection.CollectionType;
import dev.ucdm.grib.collection.GribCollection;
import dev.ucdm.grib.collection.VariableIndex;
import dev.ucdm.grib.common.GribIosp;
import dev.ucdm.grib.common.GribTables;
import dev.ucdm.grib.common.util.GribNumbers;
import dev.ucdm.grib.common.util.GribUtils;
import dev.ucdm.grib.grib2.record.Grib2Record;
import dev.ucdm.grib.grib2.record.Grib2RecordScanner;
import dev.ucdm.grib.grib2.table.Grib2Tables;
import dev.ucdm.grib.common.GribCollectionIndex;

import java.io.IOException;
import java.util.Formatter;

/**
 * Grib-2 Collection IOSP.
 * Handles both collections and single GRIB files.
 *
 * @author caron
 * @since 4/6/11
 */
public class Grib2Iosp extends GribIosp {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib2Iosp.class);

  static String makeVariableNameFromTable(Grib2Tables cust, GribCollection gribCollection,
                                          VariableIndex vindex, boolean useGenType) {

    try (Formatter f = new Formatter()) {
      GribTables.Parameter param = cust.getParameter(vindex);

      if (param == null) {
        f.format("VAR%d-%d-%d_FROM_%d-%d-%d", vindex.getDiscipline(), vindex.getCategory(), vindex.getParameter(),
            gribCollection.getCenter(), gribCollection.getSubcenter(), vindex.getTableVersion());
      } else {
        f.format("%s", GribUtils.makeNameFromDescription(param.getName()));
      }

      if (vindex.getGenProcessType() == 6 || vindex.getGenProcessType() == 7) {
        f.format("_error"); // its an "error" type variable - add to name

      } else if (useGenType && vindex.getGenProcessType() >= 0) {
        String genType = cust.getGeneratingProcessTypeName(vindex.getGenProcessType());
        String s = genType.replace(" ", "_");
        f.format("_%s", s);
      }

      if (vindex.getLevelType() != GribNumbers.UNDEFINED) { // satellite data doesnt have a level
        f.format("_%s", cust.getLevelNameShort(vindex.getLevelType())); // vindex.getLevelType()); // code table 4.5
        if (vindex.isLayer()) {
          f.format("_layer");
        }
      }

      String intvName = vindex.getTimeIntvName();
      if (intvName != null && !intvName.isEmpty()) {
        f.format("_%s", intvName);
      }

      if (vindex.getIntvType() >= 0) {
        String statName = cust.getStatisticNameShort(vindex.getIntvType());
        if (statName != null) {
          f.format("_%s", statName);
        }
      }

      if (vindex.getSpatialStatType() >= 0) {
        String statName = cust.getCodeTableValue("4.10", vindex.getSpatialStatType());
        if (statName != null) {
          f.format("_%s", statName);
        }
      }

      if (vindex.getEnsDerivedType() >= 0) {
        f.format("_%s", cust.getProbabilityNameShort(vindex.getEnsDerivedType()));
      } else if (vindex.getProbabilityName() != null && !vindex.getProbabilityName().isEmpty()) {
        String s = vindex.getProbabilityName().replace(".", "p");
        f.format("_probability_%s", s);
      } else if (vindex.isEnsemble()) {
        f.format("_ens");
      }

      if (vindex.getPercentile() >= 0) {
        f.format("_Percentile%2d", vindex.getPercentile());
      }
      return f.toString();
    }
  }

  public static String makeVariableLongName(Grib2Tables cust, VariableIndex vindex, boolean useGenType) {

    try (Formatter f = new Formatter()) {
      boolean isProb = (vindex.getProbabilityName() != null && !vindex.getProbabilityName().isEmpty());
      if (isProb) {
        f.format("Probability ");
      }

      GribTables.Parameter gp = cust.getParameter(vindex);
      if (gp == null) {
        f.format("Unknown Parameter %d-%d-%d", vindex.getDiscipline(), vindex.getCategory(), vindex.getParameter());
      } else {
        f.format("%s", gp.getName());
      }

      String vintvName = vindex.getTimeIntvName();
      if (vindex.getIntvType() >= 0 && vintvName != null && !vintvName.isEmpty()) {
        String intvName = cust.getStatisticNameShort(vindex.getIntvType());
        if (intvName == null || intvName.equalsIgnoreCase("Missing")) {
          intvName = cust.getStatisticNameShort(vindex.getIntvType());
        }
        if (intvName == null) {
          f.format(" (%s)", vintvName);
        } else {
          f.format(" (%s %s)", vintvName, intvName);
        }

      } else if (vindex.getIntvType() >= 0) {
        String intvName = cust.getStatisticNameShort(vindex.getIntvType());
        f.format(" (%s)", intvName);
      }

      if (vindex.getSpatialStatType() >= 0) {
        String statName = cust.getCodeTableValue("4.10", vindex.getSpatialStatType());
        if (statName != null) {
          f.format("_%s", statName);
        }
      }

      if (vindex.getEnsDerivedType() >= 0) {
        f.format(" (%s)", cust.getCodeTableValue("4.10", vindex.getEnsDerivedType()));
      } else if (isProb) {
        f.format(" %s %s", vindex.getProbabilityName(), getVindexUnits(cust, vindex)); // add data units here
      }

      if (vindex.getGenProcessType() == 6 || vindex.getGenProcessType() == 7) {
        f.format(" error"); // its an "error" type variable - add to name

      } else if (useGenType && vindex.getGenProcessType() >= 0) {
        f.format(" %s", cust.getGeneratingProcessTypeName(vindex.getGenProcessType()));
      }

      if (vindex.getPercentile() >= 0) {
        f.format(" %d Percentile", vindex.getPercentile());
      }

      if (vindex.getLevelType() != GribNumbers.UNDEFINED) { // satellite data doesnt have a level
        f.format(" @ %s", cust.getCodeTableValue("4.5", vindex.getLevelType()));
        if (vindex.isLayer()) {
          f.format(" layer");
        }
      }
      return f.toString();
    }
  }

  @Override
  public String makeVariableName(VariableIndex vindex) {
    return makeVariableNameFromTable(cust, gribCollection, vindex, gribCollection.config.useGenType);
  }

  @Override
  public String makeVariableLongName(VariableIndex vindex) {
    return makeVariableLongName(cust, vindex, gribCollection.config.useGenType);
  }

  @Override
  public String makeVariableUnits(VariableIndex vindex) {
    return makeVariableUnits(cust, vindex);
  }

  static String makeVariableUnits(Grib2Tables tables, VariableIndex vindex) {
    if (vindex.getProbabilityName() != null && !vindex.getProbabilityName().isEmpty())
      return "%";
    return getVindexUnits(tables, vindex);
  }

  private static String getVindexUnits(Grib2Tables tables, VariableIndex vindex) {
    GribTables.Parameter gp = tables.getParameter(vindex);
    String val = (gp == null) ? "" : gp.getUnit();
    return (val == null) ? "" : val;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private Grib2Tables cust;

  // accept grib2 or ncx files
  @Override
  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    if (raf instanceof RafHttp) { // only do remote if memory resident
      if (raf.length() > raf.getBufferSize())
        return false;

    } else { // wont accept remote index

      GribCollectionIndex.Type type = GribCollectionIndex.getType(raf);
      if (type == GribCollectionIndex.Type.GRIB2)
        return true;
      if (type == GribCollectionIndex.Type.Partition2)
        return true;
    }

    // check for GRIB2 data file
    return Grib2RecordScanner.isValidFile(raf);
  }

  @Override
  public String getCdmFileTypeId() {
    return DataFormatType.GRIB2.getDescription();
  }

  @Override
  public String getCdmFileTypeDescription() {
    return "GRIB2 Collection";
  }

  // public no-arg constructor for reflection
  public Grib2Iosp() {
    super(false, logger);
  }

  Grib2Iosp(GribCollection.GroupGC gHcs, CollectionType gtype) {
    super(false, logger);
    this.gHcs = gHcs;
    this.owned = true;
    this.gtype = gtype;
  }

  public Grib2Iosp(GribCollection gc) {
    super(false, logger);
    this.gribCollection = gc;
    this.owned = true;
  }

  @Override
  public GribTables createCustomizer() {
    cust = Grib2Tables.factory(gribCollection.getCenter(), gribCollection.getSubcenter(), gribCollection.getMaster(),
        gribCollection.getLocal(), gribCollection.getGenProcessId());
    return cust;
  }

  @Override
  public String getVerticalCoordDesc(int vc_code) {
    return cust.getCodeTableValue("4.5", vc_code);
  }

  @Override
  protected GribTables.Parameter getParameter(VariableIndex vindex) {
    return cust.getParameter(vindex);
  }

  ///////////////////////////////////////
  // debugging back door

  public Object getLastRecordRead() {
    return Grib2Record.lastRecordRead;
  }

  public void clearLastRecordRead() {
    Grib2Record.lastRecordRead = null;
  }

  public Object getGribCustomizer() {
    return cust;
  }
}
