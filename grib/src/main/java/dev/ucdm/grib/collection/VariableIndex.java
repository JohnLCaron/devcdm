package dev.ucdm.grib.collection;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import dev.cdm.core.io.RandomAccessFile;
import dev.ucdm.grib.common.GribTables;
import dev.ucdm.grib.coord.Coordinate;
import dev.ucdm.grib.coord.CoordinateTime2D;
import dev.ucdm.grib.coord.CoordinateTimeAbstract;
import dev.ucdm.grib.coord.CoordinateTimeIntv;
import dev.ucdm.grib.coord.SparseArray;
import dev.ucdm.grib.grib1.iosp.Grib1Variable;
import dev.ucdm.grib.grib1.record.Grib1Gds;
import dev.ucdm.grib.grib1.record.Grib1ParamTime;
import dev.ucdm.grib.grib1.record.Grib1SectionProductDefinition;
import dev.ucdm.grib.grib1.table.Grib1Customizer;
import dev.ucdm.grib.grib2.iosp.Grib2Utils;
import dev.ucdm.grib.grib2.iosp.Grib2Variable;
import dev.ucdm.grib.common.GribConfig;
import dev.ucdm.grib.grib2.record.Grib2Gds;
import dev.ucdm.grib.grib2.record.Grib2Pds;
import dev.ucdm.grib.grib2.record.Grib2SectionProductDefinition;
import dev.ucdm.grib.grib2.table.Grib2Tables;
import dev.ucdm.grib.protogen.GribCollectionProto;

import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/** A mutable class for writing / reading ncx indices. */
public class VariableIndex implements Comparable<VariableIndex> {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(VariableIndex.class);

  public final GribCollection gribCollection; // belongs to this group
  public final GribCollection.GroupGC group; // belongs to this group
  public final int tableVersion; // grib1 only : can vary by variable
  public final int discipline, center, subcenter; // grib2 only
  public final byte[] rawPds; // grib1 or grib2
  public final long recordsPos; // where the records array is stored in the index. 0 means no records
  public final int recordsLen;
  public Object gribVariable; // use this to test for object equality

  List<Integer> coordIndex; // indexes into group.coords

  // derived from pds
  public final int category, parameter, levelType, intvType, ensDerivedType, probType, percentile;
  private String intvName; // eg "mixed intervals, 3 Hour, etc"
  public final String probabilityName;
  public final boolean isLayer, isEnsemble;
  public final int genProcessType;
  public final int spatialStatType;

  // variable is a partition if not null
  public Partitions.VariablePartition vpartition;

  // stats
  public int ndups, nrecords, nmissing;

  public VariableIndex(boolean isGrib1, GribCollection gribCollection, GribCollection.GroupGC g, GribTables customizer, int discipline, int center, int subcenter, byte[] rawPds,
                       List<Integer> index, long recordsPos, int recordsLen) {
    this.gribCollection = gribCollection;
    this.group = g;
    this.discipline = discipline;
    this.rawPds = rawPds;
    this.center = center;
    this.subcenter = subcenter;
    this.coordIndex = index;
    this.recordsPos = recordsPos;
    this.recordsLen = recordsLen;

    GribConfig gribConfig = gribCollection.config;
    if (isGrib1) {
      Grib1Customizer cust = (Grib1Customizer) customizer;
      Grib1SectionProductDefinition pds = new Grib1SectionProductDefinition(rawPds);

      // quantities that are stored in the pds
      this.category = 0;
      this.tableVersion = pds.getTableVersion();
      this.parameter = pds.getParameterNumber();
      this.levelType = pds.getLevelType();
      Grib1ParamTime ptime = cust.getParamTime(pds);
      if (ptime.isInterval()) {
        this.intvType = pds.getTimeRangeIndicator();
      } else {
        this.intvType = -1;
      }
      this.isLayer = cust.isLayer(pds.getLevelType());

      this.ensDerivedType = -1;
      this.probType = -1;
      this.probabilityName = null;
      this.percentile = -1;

      this.genProcessType = pds.getGenProcess(); // TODO process vs process type ??
      this.isEnsemble = pds.isEnsemble();
      this.spatialStatType = -1;

      // TODO config vs serialized config
      gribVariable = new Grib1Variable(cust, pds, (Grib1Gds) g.getGdsHash(), gribConfig.useTableVersion,
              gribConfig.intvMerge, gribConfig.useCenter);

    } else {
      Grib2Tables cust2 = (Grib2Tables) customizer;

      Grib2SectionProductDefinition pdss = new Grib2SectionProductDefinition(rawPds);
      Grib2Pds pds = pdss.getPDS();
      Preconditions.checkNotNull(pds);
      this.tableVersion = -1;

      // quantities that are stored in the pds
      this.category = pds.getParameterCategory();
      this.parameter = pds.getParameterNumber();
      this.levelType = pds.getLevelType1();
      this.intvType = pds.getStatisticalProcessType();
      this.isLayer = Grib2Utils.isLayer(pds);

      if (pds.isEnsembleDerived()) {
        Grib2Pds.PdsEnsembleDerived pdsDerived = (Grib2Pds.PdsEnsembleDerived) pds;
        ensDerivedType = pdsDerived.getDerivedForecastType(); // derived type (table 4.7)
      } else {
        this.ensDerivedType = -1;
      }

      if (pds.isProbability()) {
        Grib2Pds.PdsProbability pdsProb = (Grib2Pds.PdsProbability) pds;
        probabilityName = pdsProb.getProbabilityName();
        probType = pdsProb.getProbabilityType();
      } else {
        this.probType = -1;
        this.probabilityName = null;
      }

      if (pds.isPercentile()) {
        Grib2Pds.PdsPercentile pdsPctl = (Grib2Pds.PdsPercentile) pds;
        this.percentile = pdsPctl.getPercentileValue();
      } else {
        this.percentile = -1;
      }

      this.genProcessType = pds.getGenProcessType();
      this.isEnsemble = pds.isEnsemble();

      if (pds.isSpatialInterval()) {
        Grib2Pds.PdsSpatialInterval pdsSpatial = (Grib2Pds.PdsSpatialInterval) pds;
        this.spatialStatType = pdsSpatial.getSpatialStatisticalProcessType();
      } else {
        this.spatialStatType = -1;
      }
      // TODO config vs serialized config
      gribVariable = new Grib2Variable(cust2, discipline, center, subcenter, (Grib2Gds) g.getGdsHash(), pds,
              gribConfig.intvMerge, gribConfig.useGenType);
    }
  }

  public List<Coordinate> getCoordinates() {
    List<Coordinate> result = new ArrayList<>(coordIndex.size());
    for (int idx : coordIndex)
      result.add(group.coords.get(idx));
    return result;
  }

  @Nullable
  public Coordinate getCoordinate(Coordinate.Type want) {
    for (int idx : coordIndex)
      if (group.coords.get(idx).getType() == want)
        return group.coords.get(idx);
    return null;
  }

  // get the ith coordinate
  public Coordinate getCoordinate(int index) {
    int grpIndex = coordIndex.get(index);
    return group.coords.get(grpIndex);
  }

  public int getCoordinateIdx(Coordinate.Type want) {
    for (int idx : coordIndex)
      if (group.coords.get(idx).getType() == want)
        return idx;
    return -1;
  }

  public Iterable<Integer> getCoordinateIndex() {
    return coordIndex;
  }

  @Nullable
  public CoordinateTimeAbstract getCoordinateTime() {
    for (int idx : coordIndex) {
      if (group.coords.get(idx) instanceof CoordinateTimeAbstract) {
        return (CoordinateTimeAbstract) group.coords.get(idx);
      }
    }
    return null;
  }

  public GribCollection.GroupGC getGroup() {
    return group;
  }

  public int getTableVersion() {
    return tableVersion;
  }

  public int getDiscipline() {
    return discipline;
  }

  public int getCenter() {
    return center;
  }

  public int getSubcenter() {
    return subcenter;
  }

  public int getCategory() {
    return category;
  }

  public int getParameter() {
    return parameter;
  }

  public int getLevelType() {
    return levelType;
  }

  public int getIntvType() {
    return intvType;
  }

  public int getEnsDerivedType() {
    return ensDerivedType;
  }

  public int getProbType() {
    return probType;
  }

  public int getPercentile() {
    return percentile;
  }

  public String getIntvName() {
    return intvName;
  }

  public String getProbabilityName() {
    return probabilityName;
  }

  public boolean isLayer() {
    return isLayer;
  }

  public boolean isEnsemble() {
    return isEnsemble;
  }

  public int getGenProcessType() {
    return genProcessType;
  }

  public int getSpatialStatType() {
    return spatialStatType;
  }

  @Nullable
  public String getTimeIntvName() {
    if (intvName != null)
      return intvName;
    CoordinateTimeIntv timeiCoord = (CoordinateTimeIntv) getCoordinate(Coordinate.Type.timeIntv);
    if (timeiCoord != null) {
      intvName = timeiCoord.getTimeIntervalName();
      return intvName;
    }

    CoordinateTime2D time2DCoord = (CoordinateTime2D) getCoordinate(Coordinate.Type.time2D);
    if (time2DCoord == null || !time2DCoord.isTimeInterval())
      return null;
    intvName = time2DCoord.getTimeIntervalName();
    return intvName;
  }

  /////////////////////////////
  // read in on demand
  private SparseArray<GribCollection.ReadRecord> sparseArray; // for GC only; lazily read; same array shape as variable, minus x and y

  // read in the record information from the ncx
  public synchronized void readRecords(GribCollection gc) throws IOException {
    if (this.sparseArray != null || recordsLen == 0) {
      return;
    }

    byte[] b = new byte[recordsLen];
    try (RandomAccessFile indexRaf = new RandomAccessFile(gc.indexFilename, "r")) { // TODO cache ??
      indexRaf.seek(recordsPos);
      indexRaf.readFully(b);

      /*
       * message SparseArray {
       * repeated uint32 size = 2 [packed=true]; // multidim sizes = shape[]
       * repeated uint32 track = 3 [packed=true]; // 1-based index into record list, 0 == missing
       * repeated Record records = 4; // List<Record>
       * uint32 ndups = 5; // duplicates found when creating
       * }
       */
      GribCollectionProto.SparseArray proto = GribCollectionProto.SparseArray.parseFrom(b);

      int nsizes = proto.getSizeCount();
      int[] size = new int[nsizes];
      for (int i = 0; i < nsizes; i++)
        size[i] = proto.getSize(i);

      int ntrack = proto.getTrackCount();
      int[] track = new int[ntrack];
      for (int i = 0; i < ntrack; i++)
        track[i] = proto.getTrack(i);

      int n = proto.getRecordsCount();
      List<GribCollection.ReadRecord> records = new ArrayList<>(n);
      for (int i = 0; i < n; i++) {
        GribCollectionProto.Record pr = proto.getRecords(i);
        records.add(new GribCollection.ReadRecord(pr.getFileno(), pr.getStartPos(), pr.getBmsOffset(), pr.getDrsOffset()));
      }
      int ndups = proto.getNdups();
      this.sparseArray = new SparseArray<>(size, track, records, ndups);

    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      logger.error(" file={} recordsLen={} recordPos={}", gc.indexFilename, recordsLen, recordsPos);
      throw e;
    }
  }

  public synchronized GribCollection.ReadRecord getRecordAt(int sourceIndex) {
    return sparseArray.getContent(sourceIndex);
  }

  synchronized GribCollection.ReadRecord getRecordAt(int[] sourceIndex) {
    return sparseArray.getContent(sourceIndex);
  }

  /////////////////////////////
  public String id() {
    return discipline + "-" + category + "-" + parameter;
  }

  public int getVarid() {
    return (discipline << 16) + (category << 8) + parameter;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("tableVersion", tableVersion).add("discipline", discipline)
            .add("category", category).add("parameter", parameter).add("levelType", levelType).add("intvType", intvType)
            .add("ensDerivedType", ensDerivedType).add("probType", probType).add("intvName", intvName)
            .add("probabilityName", probabilityName).add("isLayer", isLayer).add("genProcessType", genProcessType)
            .add("cdmHash", gribVariable.hashCode()).toString();
  }

  public String toStringComplete() {
    return MoreObjects.toStringHelper(this).add("group", group).add("tableVersion", tableVersion)
            .add("discipline", discipline).add("center", center).add("subcenter", subcenter).add("recordsPos", recordsPos)
            .add("recordsLen", recordsLen).add("gribVariable", gribVariable).add("coordIndex", coordIndex)
            .add("category", category).add("parameter", parameter).add("levelType", levelType).add("intvType", intvType)
            .add("ensDerivedType", ensDerivedType).add("probType", probType).add("intvName", intvName)
            .add("probabilityName", probabilityName).add("isLayer", isLayer).add("isEnsemble", isEnsemble)
            .add("genProcessType", genProcessType).add("spatialStatType", spatialStatType).toString();
  }

  public String toStringShort() {
    try (Formatter sb = new Formatter()) {
      sb.format("Variable {%d-%d-%d", discipline, category, parameter);
      sb.format(", levelType=%d", levelType);
      sb.format(", intvType=%d", intvType);
      if (intvName != null && !intvName.isEmpty()) {
        sb.format(" intv=%s", intvName);
      }
      if (probabilityName != null && !probabilityName.isEmpty()) {
        sb.format(" prob=%s", probabilityName);
      }
      sb.format(" cdmHash=%d}", gribVariable.hashCode());
      return sb.toString();
    }
  }

  @Override
  public int compareTo(VariableIndex o) {
    int r = discipline - o.discipline; // TODO add center, subcenter, version?
    if (r != 0)
      return r;
    r = category - o.category;
    if (r != 0)
      return r;
    r = parameter - o.parameter;
    if (r != 0)
      return r;
    r = levelType - o.levelType;
    if (r != 0)
      return r;
    r = intvType - o.intvType;
    return r;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof VariableIndex))
      return false;

    VariableIndex that = (VariableIndex) o;
    return gribVariable.equals(that.gribVariable);
  }

  @Override
  public int hashCode() {
    return gribVariable.hashCode();
  }

} // VariableIndex
