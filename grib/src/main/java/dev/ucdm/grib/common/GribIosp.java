/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.common;

import dev.cdm.array.Array;
import dev.cdm.array.Arrays;
import dev.cdm.array.InvalidRangeException;
import dev.cdm.array.Section;
import dev.cdm.core.api.Group;
import dev.cdm.core.api.Variable;
import dev.cdm.core.io.RandomAccessFile;
import dev.cdm.core.iosp.AbstractIOServiceProvider;
import dev.cdm.core.util.CancelTask;
import dev.cdm.dataset.ncml.NcmlReader;
import dev.ucdm.grib.collection.CollectionType;
import dev.ucdm.grib.collection.GribCollection;
import dev.ucdm.grib.collection.VariableIndex;
import dev.ucdm.grib.coord.Coordinate;
import dev.ucdm.grib.coord.CoordinateTime2D;
import dev.ucdm.grib.grib2.iosp.Grib2Utils;
import org.jdom2.Element;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Formatter;
import java.util.List;

/**
 * Grib Collection IOSP. Handles both collections and single GRIB files.
 * Immutable after open() is called.
 */
public abstract class GribIosp extends AbstractIOServiceProvider {
  public static int debugIndexOnlyCount; // count number of data accesses

  // store custom tables in here
  protected final GribConfig gribConfig = new GribConfig();

  public void setParamTable(Element paramTable) {
    gribConfig.paramTable = paramTable;
  }

  public void setLookupTablePath(String lookupTablePath) {
    gribConfig.lookupTablePath = lookupTablePath;
  }

  public void setParamTablePath(String paramTablePath) {
    gribConfig.paramTablePath = paramTablePath;
  }

  @Override
  @Nullable
  public Object sendIospMessage(Object special) {
    if (special instanceof String) {
      String s = (String) special;
      if (s.startsWith("gribParameterTableLookup")) {
        int pos = s.indexOf("=");
        if (pos > 0) {
          gribConfig.lookupTablePath = s.substring(pos + 1).trim();
        }

      } else if (s.startsWith("gribParameterTable")) {
        int pos = s.indexOf("=");
        if (pos > 0) {
          gribConfig.paramTablePath = s.substring(pos + 1).trim();
        }
      }
      return null;
    }

    if (special instanceof org.jdom2.Element) { // the root element will be <iospParam>
      Element root = (org.jdom2.Element) special;
      gribConfig.configFromXml(root, NcmlReader.ncmlNS);
      return null;
    }

    return super.sendIospMessage(special);
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  protected final boolean isGrib1;
  protected final org.slf4j.Logger logger;

  protected GribCollection gribCollection;
  protected GribCollection.GroupGC gHcs;
  protected CollectionType gtype; // only used if gHcs was set
  protected boolean isPartitioned = false;
  protected boolean owned; // if Iosp is owned by GribCollection; affects close() TODO get rid of this
  protected GribTables gribTable;

  public GribIosp(boolean isGrib1, org.slf4j.Logger logger) {
    this.isGrib1 = isGrib1;
    this.logger = logger;
  }

  public abstract GribTables createCustomizer() throws IOException;

  public abstract String makeVariableName(VariableIndex vindex);

  public abstract String makeVariableLongName(VariableIndex vindex);

  public abstract String makeVariableUnits(VariableIndex vindex);

  public abstract String getVerticalCoordDesc(int vc_code);

  protected abstract GribTables.Parameter getParameter(VariableIndex vindex);

  @Override
  public void build(RandomAccessFile raf, Group.Builder rootGroup, CancelTask cancelTask) throws IOException {
    setRaf(raf);

    if (gHcs != null) { // just use the one group that was set in the constructor
      this.gribCollection = gHcs.getGribCollection();
      //if (this.gribCollection instanceof PartitionCollectionImmutable) {
      //  isPartitioned = true;
      //}
      gribTable = createCustomizer();
      GribIospBuilder helper = new GribIospBuilder(this, isGrib1, logger, gribCollection, gribTable);

      helper.addGroup(rootGroup, gHcs, gtype, false);

    } else if (gribCollection == null) { // may have been set in the constructor

      this.gribCollection =
          GribCollectionIndex.openGribCollectionFromRaf(raf, CollectionUpdateType.testIndexOnly, gribConfig, logger);
      if (gribCollection == null) {
        throw new IllegalStateException("Not a GRIB data file or index file " + raf.getLocation());
      }

      // isPartitioned = (this.gribCollection instanceof PartitionCollectionImmutable);
      gribTable = createCustomizer();
      GribIospBuilder helper = new GribIospBuilder(this, isGrib1, logger, gribCollection, gribTable);

      boolean useDatasetGroup = gribCollection.getDatasets().size() > 1;
      for (GribCollection.Dataset ds : gribCollection.getDatasets()) {
        Group.Builder topGroup;
        if (useDatasetGroup) {
          topGroup = Group.builder().setName(ds.getType().toString());
          rootGroup.addGroup(topGroup);
        } else {
          topGroup = rootGroup;
        }

        List<GribCollection.GroupGC> groups = ds.getGroups();
        boolean useGroups = groups.size() > 1;
        for (GribCollection.GroupGC g : groups) {
          helper.addGroup(topGroup, g, ds.getType(), useGroups);
        }
      }
    }

    rootGroup.addAttributes(gribCollection.makeGlobalAttributes());
  }

  enum Time2DinfoType {
    off, offU, intv, intvU, bounds, boundsU, is1Dtime, isUniqueRuntime, reftime, timeAuxRef
  }

  static class Time2Dinfo {
    final Time2DinfoType which;
    final CoordinateTime2D time2D;
    final Coordinate time1D;

    Time2Dinfo(Time2DinfoType which, CoordinateTime2D time2D, Coordinate time1D) {
      this.which = which;
      this.time2D = time2D;
      this.time1D = time1D;
    }
  }

  @Nullable
  String searchCoord(Grib2Utils.LatLonCoordType type, List<VariableIndex> list) {
    if (type == null) {
      return null;
    }

    VariableIndex lat, lon;
    switch (type) {
      case U:
        lat = findParameter(list, 198);
        lon = findParameter(list, 199);
        return (lat != null && lon != null) ? makeVariableName(lat) + " " + makeVariableName(lon) : null;
      case V:
        lat = findParameter(list, 200);
        lon = findParameter(list, 201);
        return (lat != null && lon != null) ? makeVariableName(lat) + " " + makeVariableName(lon) : null;
      case P:
        lat = findParameter(list, 202);
        lon = findParameter(list, 203);
        return (lat != null && lon != null) ? makeVariableName(lat) + "  " + makeVariableName(lon) : null;
    }
    return null;
  }

  @Nullable
  private VariableIndex findParameter(List<VariableIndex> list, int p) {
    for (VariableIndex vindex : list) {
      if ((vindex.getDiscipline() == 0) && (vindex.getCategory() == 2) && (vindex.getParameter() == p)) {
        return vindex;
      }
    }
    return null;
  }

  @Override
  public void close() throws IOException {
    if (!owned && gribCollection != null) {
      gribCollection.close();
    }
    gribCollection = null;
    super.close();
  }

  @Override
  public String getDetailInfo() {
    Formatter f = new Formatter();
    f.format("%s", super.getDetailInfo());
    if (gribCollection != null) {
      f.format("GribTable %s%n%n", this.gribTable);
      gribCollection.showIndex(f);
    }
    return f.toString();
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public Array<?> readArrayData(Variable v2, Section section)
      throws IOException, InvalidRangeException {
    // see if its time2D - then generate data on the fly
    if (v2.getSPobject() instanceof Time2Dinfo) {
      Time2Dinfo info = (Time2Dinfo) v2.getSPobject();
      Array<?> data = Time2DLazyCoordinate.makeLazyCoordinateArray(v2, info, gribCollection);
      Section sectionFilled = Section.fill(section, v2.getShape());
      return Arrays.section(data, sectionFilled);
    }

    try {
      VariableIndex vindex = (VariableIndex) v2.getSPobject();
      GribArrayReader dataReader = GribArrayReader.factory(gribCollection, vindex);
      SectionIterable sectionIter = new SectionIterable(section, v2.getShape());
      return dataReader.readData(sectionIter);

    } catch (IOException ioe) {
      logger.error("Failed to readData ", ioe);
      throw ioe;
    }
  }

  ///////////////////////////////////////
  // debugging back door
  public abstract Object getLastRecordRead();

  public abstract void clearLastRecordRead();

  public abstract Object getGribCustomizer();
}
