/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.common;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import dev.ucdm.array.Immutable;

import dev.ucdm.grib.collection.CollectionType;
import dev.ucdm.grib.collection.GribCollection;
import dev.ucdm.grib.collection.VariableIndex;
import dev.ucdm.grib.common.util.SectionIterable;
import dev.ucdm.grib.coord.CoordinateTime2D;
import dev.ucdm.grib.coord.TimeCoordIntvDateValue;
import dev.ucdm.grib.grib1.iosp.Grib1Parameter;
import dev.ucdm.grib.grib1.record.Grib1ParamTime;
import dev.ucdm.grib.grib1.record.Grib1Record;
import dev.ucdm.grib.grib1.record.Grib1SectionProductDefinition;
import dev.ucdm.grib.grib1.table.Grib1Customizer;
import dev.ucdm.grib.grib2.record.Grib2Record;
import dev.ucdm.grib.grib2.record.Grib2RecordScanner;
import dev.ucdm.grib.grib2.table.Grib2Tables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.ucdm.array.Array;
import dev.ucdm.array.ArrayType;
import dev.ucdm.array.Arrays;
import dev.ucdm.array.InvalidRangeException;
import dev.ucdm.array.RangeIterator;
import dev.ucdm.grid.api.GridSubset;
import dev.ucdm.core.io.RandomAccessFile;

/** Matches the variable coordinate index to GribRecords */
@Immutable
public abstract class GribArrayReader {
  private static final Logger logger = LoggerFactory.getLogger(GribArrayReader.class);

  public static GribArrayReader factory(GribCollection gribCollection, VariableIndex vindex) {
    if (gribCollection.isGrib1)
      return new Grib1ArrayReader(gribCollection, vindex);
    else
      return new Grib2ArrayReader(gribCollection, vindex);
  }

  protected abstract float[] readData(RandomAccessFile rafData, GribReaderRecord dr) throws IOException;

  protected abstract void show(RandomAccessFile rafData, long dataPos) throws IOException;

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  public static GribCollection.ReadRecord currentDataRecord;
  public static GribDataValidator validator;
  public static String currentDataRafFilename;
  private static final boolean show = false; // debug

  protected final GribCollection gribCollection;
  private final VariableIndex vindex;
  private final List<GribReaderRecord> records = new ArrayList<>();

  protected GribArrayReader(GribCollection gribCollection, VariableIndex vindex) {
    this.gribCollection = gribCollection;
    this.vindex = vindex;
  }

  /**
   * Read the section of data described by want
   * 
   * @param want which data do you want?
   * @return data as an Array
   */
  public Array<?> readData(SectionIterable want) throws IOException, InvalidRangeException {
    if (vindex.vpartition == null)
      return readDataFromCollection(vindex, want);
    else
      return readDataFromPartition(vindex, want);
  }

  /*
   * SectionIterable iterates over the source indexes, corresponding to vindex's SparseArray.
   * IOSP: works because variable coordinate corresponds 1-1 to Grib Coordinate.
   * GribCoverage: must translate coordinates to Grib Coordinate index.
   * SectionIterable.next(int[] index) is not used here.
   */
  private Array<?> readDataFromCollection(VariableIndex vindex, SectionIterable want)
      throws IOException {
    // first time, read records and keep in memory
    vindex.readRecords(gribCollection);

    int rank = want.getRank();
    int sectionLen = rank - 2; // all but x, y
    SectionIterable sectionWanted = want.subSection(0, sectionLen);
    // assert sectionLen == vindex.getRank(); TODO true or false ??

    // collect all the records that need to be read
    int resultIndex = 0;
    for (int sourceIndex : sectionWanted) {
      // addRecord(sourceIndex, count++);
      GribCollection.ReadRecord record = vindex.getRecordAt(sourceIndex);
      if (record != null)
        records.add(new GribReaderRecord(resultIndex, record, vindex.group.getGdsHorizCoordSys()));
      resultIndex++;
    }

    // sort by file and position, then read
    DataReceiverIF dataReceiver = new DataReceiver(want.getShape(), want.getRange(rank - 2), want.getRange(rank - 1));
    read(dataReceiver);
    return dataReceiver.getArray();
  }

  /*
   * Iterates using SectionIterable.next(int[] index).
   * The work of translating that down into partition heirarchy and finally to a GC is all in
   * VariableIndexPartitioned.getDataRecord(int[] index)
   * want.getShape() indicates the result Array shape.
   */
  private Array<?> readDataFromPartition(VariableIndex vi, SectionIterable want) throws IOException {

    int rank = want.getRank();
    SectionIterable sectionWanted = want.subSection(0, rank - 2); // all but x, y
    SectionIterable.SectionIterator iterWanted = sectionWanted.getIterator(); // iterator over wanted indices
                                                                              // in vindexP
    int[] indexWanted = new int[rank - 2]; // place to put the iterator result
    int[] useIndex = indexWanted;

    // collect all the records that need to be read
    int resultPos = 0;
    while (iterWanted.hasNext()) {
      iterWanted.next(indexWanted); // returns the vindexP index in indexWanted array

      // TODO for MRUTP, must munge the index here (not in vindexP.getDataRecord, because its recursive
      if (vi.group.ds.gctype == CollectionType.MRUTP) {
        // find the partition from getRuntimeIdxFromMrutpTimeIndex
        CoordinateTime2D time2D = (CoordinateTime2D) vi.getCoordinateTime();
        Preconditions.checkNotNull(time2D);
        int[] timeIndices = time2D.getTimeIndicesFromMrutp(indexWanted[0]);

        int[] indexReallyWanted = new int[indexWanted.length + 1];
        indexReallyWanted[0] = timeIndices[0];
        indexReallyWanted[1] = timeIndices[1];
        System.arraycopy(indexWanted, 1, indexReallyWanted, 2, indexWanted.length - 1);
        useIndex = indexReallyWanted;
      }

      PartitionedReaderRecord record = gribCollection.partitions.getPartitionedReaderRecord(vi, useIndex);
      if (record == null) {
        resultPos++; // can just skip, since result is prefilled with NaNs
        continue;
      }
      record.setResultIndex(resultPos);
      records.add(record);
      resultPos++;
    }

    // sort by file and position, then read
    DataReceiverIF dataReceiver =
        new DataReceiver(want.getShape(), want.getRange(rank - 2), want.getRange(rank - 1));
    readPartitioned(dataReceiver);
    return dataReceiver.getArray();
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Read all of the data records that have been added.
   * The full (x,y) record is read, the reciever will subset the (x, y) as needed.
   * 
   * @param dataReceiver send data here.
   */
  private void read(DataReceiverIF dataReceiver) throws IOException {
    Collections.sort(records);

    int currFile = -1;
    RandomAccessFile rafData = null;
    try {
      for (GribReaderRecord dr : records) {
        if (dr.record.fileno() != currFile) {
          if (rafData != null)
            rafData.close();
          rafData = gribCollection.getDataRaf(dr.record.fileno());
          currFile = dr.record.fileno();
        }

        if (dr.record.pos() == GribCollection.MISSING_RECORD) {
          continue;
        }

        if (GribArrayReader.validator != null && dr.validation != null && rafData != null) {
          GribArrayReader.validator.validate(gribCollection.cust, rafData, dr.record.pos() + dr.record.drsOffset(),
              dr.validation);

        } else if (show && rafData != null) { // for validation
          show(dr.validation);
          show(rafData, dr.record.pos() + dr.record.drsOffset());
        }

        float[] data = readData(rafData, dr);
        GdsHorizCoordSys hcs = vindex.group.getGdsHorizCoordSys();
        dataReceiver.addData(data, dr.resultIndex, hcs.nx);
      }

    } finally {
      if (rafData != null)
        rafData.close(); // make sure its closed even on exception
    }
  }

  private void show(GridSubset validation) {
    if (validation == null)
      return;
    System.out.printf("Coords wanted%n %s", validation);
  }

  private void readPartitioned(DataReceiverIF dataReceiver) throws IOException {
    Collections.sort(records);

    PartitionedReaderRecord lastRecord = null;
    RandomAccessFile rafData = null;
    try {

      for (GribReaderRecord dr : records) {
        PartitionedReaderRecord drp = (PartitionedReaderRecord) dr;

        if ((rafData == null) || !drp.usesSameFile(lastRecord)) {
          if (rafData != null) {
            rafData.close();
          }
          rafData = drp.usePartition.getDataRaf(drp.partno, dr.record.fileno());
        }
        lastRecord = drp;

        if (dr.record.pos() == GribCollection.MISSING_RECORD)
          continue;

        if (GribArrayReader.validator != null && dr.validation != null) {
          GribArrayReader.validator.validate(gribCollection.cust, rafData, dr.record.pos() + dr.record.drsOffset(),
              dr.validation);
        } else if (show) { // for validation
          show(dr.validation);
          show(rafData, dr.record.pos() + dr.record.drsOffset());
        }

        float[] data = readData(rafData, dr);
        GdsHorizCoordSys hcs = dr.hcs;
        dataReceiver.addData(data, dr.resultIndex, hcs.nx);
      }

    } finally {
      if (rafData != null)
        rafData.close(); // make sure its closed even on exception
    }
  }

  public interface DataReceiverIF {
    void addData(float[] data, int resultIndex, int nx);

    void setDataToZero(); // only used when debugging with gbx/ncx only, to fake the data

    Array<?> getArray();
  }

  public static class DataReceiver implements DataReceiverIF {
    private final RangeIterator yRange;
    private final RangeIterator xRange;
    private final int horizSize;
    private final float[] dataArray;
    private final int[] shape;

    DataReceiver(int[] shape, RangeIterator yRange, RangeIterator xRange) {
      this.shape = shape;
      this.yRange = yRange;
      this.xRange = xRange;
      this.horizSize = yRange.length() * xRange.length();

      long len = Arrays.computeSize(shape);
      if (len > 100 * 1000 * 1000 * 4) { // TODO make configurable
        logger.debug("Len greater that 100MB shape={}%n{}", java.util.Arrays.toString(shape),
            Throwables.getStackTraceAsString(new Throwable()));
        throw new IllegalArgumentException("RequestTooLarge: Len greater that 100M ");
      }
      this.dataArray = new float[(int) len];
      java.util.Arrays.fill(this.dataArray, Float.NaN); // prefill primitive array
    }

    @Override
    public void addData(float[] data, int resultIndex, int nx) {
      int start = resultIndex * horizSize;
      int count = 0;
      for (int y : yRange) {
        for (int x : xRange) {
          int dataIdx = y * nx + x;
          // dataArray.setFloat(start + count, data[dataIdx]);
          this.dataArray[start + count] = data[dataIdx];
          count++;
        }
      }
    }

    // optimization
    @Override
    public void setDataToZero() {
      java.util.Arrays.fill(this.dataArray, 0.0f);
    }

    @Override
    public Array<?> getArray() {
      return Arrays.factory(ArrayType.FLOAT, shape, dataArray);
    }
  }

  /////////////////////////////////////////////////////////

  private static class Grib2ArrayReader extends GribArrayReader {
    private final Grib2Tables cust;

    Grib2ArrayReader(GribCollection gribCollection, VariableIndex vindex) {
      super(gribCollection, vindex);
      this.cust = (Grib2Tables) gribCollection.cust;
    }

    @Override
    protected float[] readData(RandomAccessFile rafData, GribReaderRecord dr) throws IOException {
      GdsHorizCoordSys hcs = dr.hcs;
      long dataPos = dr.record.pos() + dr.record.drsOffset();
      long bmsPos = (dr.record.bmsOffset() > 0) ? dr.record.pos() + dr.record.bmsOffset() : 0;
      return Grib2Record.readData(rafData, dataPos, bmsPos, hcs.gdsNumberPoints, hcs.getScanMode(), hcs.nxRaw,
          hcs.nyRaw, hcs.nptsInLine);
    }

    @Override
    protected void show(RandomAccessFile rafData, long pos) throws IOException {
      Grib2Record gr = Grib2RecordScanner.findRecordByDrspos(rafData, pos);
      if (gr != null) {
        Formatter f = new Formatter();
        f.format("File=%s%n", rafData.getLocation());
        f.format("  Parameter=%s%n", cust.getVariableName(gr));
        f.format("  ReferenceDate=%s%n", gr.getReferenceDate());
        f.format("  ForecastDate=%s%n", cust.getForecastDate(gr));
        TimeCoordIntvDateValue tinv = cust.getForecastTimeInterval(gr);
        if (tinv != null)
          f.format("  TimeInterval=%s%n", tinv);
        f.format("  ");
        gr.getPDS().show(f);
        System.out.printf("%nGrib2Record.readData at drsPos %d = %s%n", pos, f);
      }
    }
  }

  private static class Grib1ArrayReader extends GribArrayReader {
    private final Grib1Customizer cust;

    Grib1ArrayReader(GribCollection gribCollection, VariableIndex vindex) {
      super(gribCollection, vindex);
      this.cust = (Grib1Customizer) gribCollection.cust;
    }

    @Override
    protected float[] readData(RandomAccessFile rafData, GribReaderRecord dr) throws IOException {
      return Grib1Record.readData(rafData, dr.record.pos());
    }

    @Override
    protected void show(RandomAccessFile rafData, long dataPos) throws IOException {
      rafData.seek(dataPos);
      Grib1Record gr = new Grib1Record(rafData);
      Formatter f = new Formatter();
      f.format("File=%s%n", rafData.getLocation());
      Grib1SectionProductDefinition pds = gr.getPDSsection();
      Grib1Parameter param =
          cust.getParameter(pds.getCenter(), pds.getSubCenter(), pds.getTableVersion(), pds.getParameterNumber());
      f.format("  Parameter=%s%n", param);
      f.format("  ReferenceDate=%s%n", gr.getReferenceDate());
      Grib1ParamTime ptime = gr.getParamTime(cust);
      f.format("  ForecastTime=%d%n", ptime.forecastTime());
      if (ptime.isInterval()) {
        int[] tinv = ptime.getInterval();
        f.format("  TimeInterval=(%d,%d)%n", tinv[0], tinv[1]);
      }
      f.format("%n");
      gr.getPDSsection().showPds(cust, f);
      System.out.printf("%nGrib1Record.readData at drsPos %d = %s%n", dataPos, f);
    }
  }

}
