/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.collection;

import dev.ucdm.core.calendar.CalendarDate;
import dev.ucdm.core.calendar.CalendarDateRange;
import dev.ucdm.core.calendar.CalendarPeriod;
import dev.ucdm.grib.common.GribConstants;
import dev.ucdm.grib.common.GribIndex;
import dev.ucdm.grib.common.util.GribIndexCache;
import dev.ucdm.grib.coord.*;
import dev.ucdm.grib.grib2.iosp.Grib2Utils;
import dev.ucdm.grib.grib2.iosp.Grib2Variable;
import dev.ucdm.grib.common.GribConfig;
import dev.ucdm.grib.grib2.record.Grib2Gds;
import dev.ucdm.grib.grib2.record.Grib2Pds;
import dev.ucdm.grib.grib2.record.Grib2Record;
import dev.ucdm.grib.grib2.table.Grib2Tables;
import dev.ucdm.grib.inventory.MCollection;
import dev.ucdm.grib.inventory.MFile;
import dev.ucdm.grib.protoconvert.Grib2CollectionIndexWriter;
import dev.ucdm.grib.protoconvert.Grib2Index;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Builds ncx indexes for collections of Grib2 files.
 * This is the hard logic here.
 */
public class Grib2CollectionBuilder extends GribCollectionBuilder {
  private final GribConfig gribConfig;
  private Grib2Tables cust;

  // TODO probable name could just be dcm.getCollectionName()
  public Grib2CollectionBuilder(String name, MCollection dcm, org.slf4j.Logger logger) {
    super(false, name, dcm, logger);

    this.gribConfig = (GribConfig) dcm.getAuxInfo(GribConfig.AUX_CONFIG);
  }

  /**
   * Read all records in all files.
   * Divide into groups based on GDS hash and runtime.
   * Each group has an list of all records that belong to it.
   * For each group, run rectilizer to derive the coordinates and variables.
   *
   * @param singleRuntime PartitionType = all; creates separate collection and index for each runtime. not used.
   */
  @Override
  protected List<? extends Group> makeGroups(List<MFile> allFiles, boolean singleRuntime, Formatter errlog) {
    Map<GroupAndRuntime, Grib2CollectionIndexWriter.Group> gdsMap = new HashMap<>();

    logger.debug("Grib2CollectionBuilder {}: makeGroups", name);
    GribRecordStats statsAll = new GribRecordStats(); // debugging
    // need a final object holding a mutable integer
    AtomicInteger fileno = new AtomicInteger(0);

    // place each record into its Grib2CollectionPublish.Group, based on Grib2Gds.hashCode
    dcm.iterateOverMFiles(mfile -> {
      Grib2Index index = null;

      Formatter gbxerrors = new Formatter();
      try {
        CollectionUpdateType update = GribConstants.debugGbxIndexOnly ? CollectionUpdateType.never : CollectionUpdateType.test;
        // LOOK not using the CollectionUpdateType from GribCOllectionINdex
        index = GribIndex.readOrCreateIndex2(mfile, update, gbxerrors);
        allFiles.add(mfile); // add on success

      } catch (IOException ioe) {
        logger.error("Grib2CollectionBuilder {} : reading/Creating gbx9 index for file {} failed\n{}", name, mfile.getPath(), ioe);
        return;
      }
      if (index == null) {
        logger.error("Grib2CollectionBuilder {} : reading/Creating gbx9 index for file {} failed\n{}", name, mfile.getPath(), gbxerrors);
        return;
      }

      for (Grib2Record gr : index.getRecords()) { // we are using entire Grib2Record - memory limitations
        if (this.cust == null) {
          this.cust = Grib2Tables.factory(gr);
          cust.setTimeUnitConverter(gribConfig.getTimeUnitConverter());
        }
        if (filterIntervals(gr, gribConfig.intvFilter)) {
          statsAll.filter++;
          continue; // skip
        }

        gr.setFile(fileno.get()); // each record tracks which file it belongs to
        Grib2Gds gds = gr.getGDS(); // use GDS to group records
        // allow external config to muck with gdsHash, because of error in encoding and we need exact hash matching
        int hashCode = gribConfig.convertGdsHash(gds.hashCode());
        if (0 == hashCode) {
          continue; // skip this group
        }
        CalendarDate runtimeDate = gr.getReferenceDate();
        // separate Groups for each runtime, if singleRuntime == true
        long runtime = singleRuntime ? runtimeDate.getMillisFromEpoch() : 0;
        GroupAndRuntime gar = new GroupAndRuntime(hashCode, runtime);
        Grib2CollectionIndexWriter.Group g = gdsMap.get(gar);
        if (g == null) {
          g = new Grib2CollectionIndexWriter.Group(gr.getGDSsection(), hashCode, runtimeDate);
          gdsMap.put(gar, g);
        }
        g.records.add(gr);
        g.runtimes.add(runtimeDate.getMillisFromEpoch());
      }
      fileno.incrementAndGet();
      statsAll.recordsTotal += index.getRecords().size();
    });

    if (statsAll.recordsTotal == 0) {
      logger.warn("No Grib1 records found in collection {}.", name);
      throw new IllegalStateException("No records found in dataset " + name);
    }

    // rectilyze each group independently
    List<Grib2CollectionIndexWriter.Group> groups = new ArrayList<>(gdsMap.values());
    for (Grib2CollectionIndexWriter.Group g : groups) {
      GribRecordStats stats = new GribRecordStats(); // debugging
      Grib2Rectilyser rect = new Grib2Rectilyser(g.records, g.hashCode);
      rect.make(gribConfig, stats, errlog);
      g.gribVars = rect.gribvars;
      g.coords = rect.coords;

      statsAll.add(stats);
    }

    // debugging and validation
    if (logger.isDebugEnabled())
      logger.debug(statsAll.show());

    return groups;
  }

  // Return true if this record should be discarded. Mostly a hack.
  private boolean filterIntervals(Grib2Record gr, GribConfig.GribIntvFilter intvFilter) {
    // hack a whack - filter out records with unknown time units
    int timeUnit = gr.getPDS().getTimeUnit();
    if (Grib2Utils.getCalendarPeriod(timeUnit) == null) {
      logger.info("Skip record with unknown time Unit= {}", timeUnit);
      return true;
    }

    int[] intv = cust.getForecastTimeIntervalOffset(gr);
    if (intv == null)
      return false; // not an interval
    int haveLength = intv[1] - intv[0];

    // discard zero length intervals if so configured
    if (haveLength == 0 && intvFilter != null && intvFilter.isZeroExcluded())
      return true;

    // HACK
    if (intvFilter != null && intvFilter.hasFilter()) {
      int discipline = gr.getIs().getDiscipline();
      Grib2Pds pds = gr.getPDS();
      int category = pds.getParameterCategory();
      int number = pds.getParameterNumber();
      int id = (discipline << 16) + (category << 8) + number;

      int prob = Integer.MIN_VALUE;
      if (pds.isProbability()) {
        Grib2Pds.PdsProbability pdsProb = (Grib2Pds.PdsProbability) pds;
        prob = (int) (1000 * pdsProb.getProbabilityUpperLimit());
      }

      // true means discard
      return intvFilter.filter(id, intv[0], intv[1], prob);
    }

    return false;
  }

  @Override
  protected boolean writeIndex(String name, String indexFilepath, CoordinateRuntime masterRuntime,
                               List<? extends GribCollectionBuilder.Group> groups, List<MFile> files, CalendarDateRange dateRange)
          throws IOException {
    Grib2CollectionIndexWriter writer = new Grib2CollectionIndexWriter(dcm);
    List<Grib2CollectionIndexWriter.Group> groups2 = new ArrayList<>();
    // copy to change GribCollectionBuilder.Group -> GribCollectionPublish.Group
    for (Object g : groups) {
      groups2.add((Grib2CollectionIndexWriter.Group) g);
    }
    File indexFileInCache = GribIndexCache.getFileOrCache(indexFilepath);
    return writer.writeIndex(name, indexFileInCache, masterRuntime, groups2, files, type, dateRange);
  }

  public static class VariableBag implements Comparable<VariableBag> {
    public final Grib2Record first;
    public final Grib2Variable gv;

    final List<Grib2Record> atomList = new ArrayList<>(100); // not sorted
    public CoordinateND<Grib2Record> coordND;
    public CalendarPeriod timeUnit;

    public List<Integer> coordIndex;
    public long pos;    // ncx4 file pos of GribCollectionProto.SparseArray
    public int length;  // ncx4 file length of GribCollectionProto.SparseArray

    private VariableBag(Grib2Record first, Grib2Variable gv) {
      this.first = first;
      this.gv = gv;
    }

    @Override
    public int compareTo(VariableBag o) {
      return Grib2Utils.getVariableName(first).compareTo(Grib2Utils.getVariableName(o.first));
    }
  }

  private class Grib2Rectilyser {
    private final int hashCode;
    private final List<Grib2Record> records;
    private List<VariableBag> gribvars;
    private List<Coordinate> coords;

    Grib2Rectilyser(List<Grib2Record> records, int hashCode) {
      this.records = records;
      this.hashCode = hashCode;
      /*
       * int gdsHash = gribConfig.convertGdsHash(gdsHashObject.hashCode());
       * gdsHashOverride = (gdsHash == gdsHashObject.hashCode()) ? 0 : gdsHash;
       */
    }

    /**
     * Divide records into VariableBag's, using Grib2Variable
     * Use that to create the coordinates of the Variable.
     * Use CoordinateSharer to create common coordinates across Variables where possible.
     */
    public void make(GribConfig config, GribRecordStats counter, Formatter info) {
      CalendarPeriod userTimeUnit = config.userTimeUnit;

      // assign each record to unique variable using Grib2Variable
      Map<Grib2Variable, VariableBag> vbHash = new HashMap<>(100);
      for (Grib2Record gr : records) {
        Grib2Variable gv;
        try {
          gv = new Grib2Variable(cust, gr, hashCode, gribConfig.intvMerge, gribConfig.useGenType);

        } catch (Throwable t) {
          logger.warn("Exception on record ", t);
          continue; // keep going
        }
        VariableBag bag = vbHash.computeIfAbsent(gv, g2v -> new VariableBag(gr, g2v));
        bag.atomList.add(gr);
      }
      gribvars = new ArrayList<>(vbHash.values());
      Collections.sort(gribvars); // make it deterministic by sorting

      // create coordinates for each variable
      for (VariableBag vb : gribvars) {
        Grib2Pds pdsFirst = vb.first.getPDS();
        int code = cust.convertTimeUnit(pdsFirst.getTimeUnit());
        // so can override the code in config "timeUnit"
        vb.timeUnit = userTimeUnit == null ? Grib2Utils.getCalendarPeriod(code) : userTimeUnit;
        CoordinateND.Builder<Grib2Record> coordNBuilder = new CoordinateND.Builder<>();

        boolean isTimeInterval = vb.first.getPDS().isTimeInterval();
        CoordinateTime2D.Builder2 builder2D = new CoordinateTime2D.Builder2(isTimeInterval, cust, vb.timeUnit, code);
        coordNBuilder.addBuilder(builder2D);

        if (vb.first.getPDS().isEnsemble()) {
          coordNBuilder.addBuilder(new CoordinateEns.Builder2(0));
        }

        VertCoordType vertUnit = cust.getVertUnit(pdsFirst.getLevelType1());
        if (vertUnit.isVerticalCoordinate())
          coordNBuilder.addBuilder(
                  new CoordinateVert.Builder2(pdsFirst.getLevelType1(), cust.getVertUnit(pdsFirst.getLevelType1())));

        // populate the coordinates with the inventory of data
        for (Grib2Record gr : vb.atomList) {
          coordNBuilder.addRecord(gr);
        }

        // done, build coordinates and sparse array indicating which records to use
        vb.coordND = coordNBuilder.finish(vb.atomList, info);
      }

      // make shared coordinates across variables
      CoordinateSharer<Grib2Record> sharify = new CoordinateSharer<>(config.unionRuntimeCoord, logger);
      for (VariableBag vb : gribvars) {
        sharify.addCoords(vb.coordND.getCoordinates());
      }
      sharify.finish();
      this.coords = sharify.getUnionCoords();

      int tot_used = 0;
      int tot_dups = 0;
      int total = 0;

      // redo the variables against the shared coordinates
      // Note here is where we see how good the shared coordinates are for this variable.
      // Ideally ndups = 0, and missing = product(coord sizes) - used == 0
      for (VariableBag vb : gribvars) {
        vb.coordND = sharify.reindexCoordND(vb.coordND);
        vb.coordIndex = sharify.reindex2shared(vb.coordND.getCoordinates());
        tot_used += vb.coordND.getSparseArray().countNotMissing();
        tot_dups += vb.coordND.getSparseArray().getNdups();
        total += vb.coordND.getSparseArray().getTotalSize();
      }

      counter.recordsUnique += tot_used;
      counter.dups += tot_dups;
      counter.vars += gribvars.size();
      counter.recordsTotal += total;
    }

    public void showInfo(Formatter f, Grib2Tables tables) {
      GribRecordStats all = new GribRecordStats();

      for (VariableBag vb : gribvars) {
        f.format("Variable %s (%d)%n", tables.getVariableName(vb.first), vb.gv.hashCode());
        vb.coordND.showInfo(f, all);
        f.format("%n");
      }
      f.format("%n all= %s", all.show());
    }
  }

}
