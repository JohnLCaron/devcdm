/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.protoconvert;

import com.google.protobuf.ByteString;

import dev.ucdm.grib.grib1.record.Grib1Record;
import dev.ucdm.grib.grib1.record.Grib1RecordScanner;
import dev.ucdm.grib.grib1.record.Grib1SectionBinaryData;
import dev.ucdm.grib.grib1.record.Grib1SectionBitMap;
import dev.ucdm.grib.grib1.record.Grib1SectionGridDefinition;
import dev.cdm.core.io.RandomAccessFile;
import dev.ucdm.grib.grib1.record.Grib1SectionIndicator;
import dev.ucdm.grib.grib1.record.Grib1SectionProductDefinition;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Convert Grib2 indexes (gbx9) to/from proto.
 * The generated proto code is in dev.ucdm.grib.protogen.Grib2IndexProto.
 */
public class Grib1IndexProto {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib1IndexProto.class);

  public static final String MAGIC_START = "Grib1Index";
  private static final int version = 5; // index must be this version, or else rewrite.

  /**
   * Read Grib1 gbx9.
   * @param idxFile index filename to read an index from.
   * Return null if version doesnt match or file is malformed.
   */
  @Nullable
  public static Grib1Index readGrib1Index(String idxFile) {
    List<Grib1SectionGridDefinition> gdsList = new ArrayList<>();
    List<Grib1Record> records = new ArrayList<>();

    try (FileInputStream fin = new FileInputStream(idxFile)) {
      //// check header is ok
      if (!Streams.readAndTest(fin, MAGIC_START.getBytes(StandardCharsets.UTF_8))) {
        logger.info("Bad magic number of grib index, on file = {}", idxFile);
        return null;
      }

      int protoVersion = Streams.readVInt(fin);
      if (protoVersion != version) {
        if ((protoVersion == 0) || (protoVersion > version))
          throw new IOException("Grib1Index found version " + protoVersion + ", want version " + version + " on " + idxFile);
        if (logger.isDebugEnabled())
          logger.debug("Grib1Index found version " + protoVersion + ", want version " + version + " on " + idxFile);
        return null;
      }

      int size = Streams.readVInt(fin);
      if (size <= 0 || size > 100 * 1000 * 1000) { // try to catch garbage
        logger.warn("Grib1Index bad size = {} for {} ", size, idxFile);
        return null;
      }

      byte[] m = new byte[size];
      Streams.readFully(fin, m);

      var proto = dev.ucdm.grib.protogen.Grib1IndexProto.Grib1Index.parseFrom(m);
      logger.debug("{} for {}", proto.getFilename(), idxFile);

      for (dev.ucdm.grib.protogen.Grib1IndexProto.Grib1GdsSection pgds : proto.getGdsListList()) {
        Grib1SectionGridDefinition gds = importGds(pgds);
        gdsList.add(gds);
      }
      logger.debug(" read {} gds", gdsList.size());

      records = new ArrayList<>(proto.getRecordsCount());
      for (dev.ucdm.grib.protogen.Grib1IndexProto.Grib1Record precord : proto.getRecordsList()) {
        records.add(importRecord(precord, gdsList));
      }
      logger.debug(" read {} records", records.size());

    } catch (NegativeArraySizeException | IOException e) {
      logger.error("Grib1Index error on " + idxFile, e);
      return null;
    }

    return new Grib1Index(gdsList, records);
  }

  // deserialize the Grib1Record object
  private static Grib1Record importRecord(dev.ucdm.grib.protogen.Grib1IndexProto.Grib1Record p,
                                          List<Grib1SectionGridDefinition> gdsList) {
    var is = new Grib1SectionIndicator(p.getGribMessageStart(), p.getGribMessageLength());
    var pds = new Grib1SectionProductDefinition(p.getPds().toByteArray());

    Grib1SectionGridDefinition gds = pds.gdsExists() ? gdsList.get(p.getGdsIdx()) : new Grib1SectionGridDefinition(pds);
    Grib1SectionBitMap bms = pds.bmsExists() ? new Grib1SectionBitMap(p.getBmsPos()) : null;

    Grib1SectionBinaryData dataSection = new Grib1SectionBinaryData(p.getDataPos(), p.getDataLen());
    return new Grib1Record(p.getHeader().toByteArray(), is, gds, pds, bms, dataSection);
  }

  private static Grib1SectionGridDefinition importGds(dev.ucdm.grib.protogen.Grib1IndexProto.Grib1GdsSection proto) {
    ByteString bytes = proto.getGds();
    return new Grib1SectionGridDefinition(bytes.toByteArray());
  }

  ////////////////////////////////////////////////////////////////////////////////

  /**
   * Write Grib1 gbx9.
   *
   * @param dataLocation grib1 data filename to read data from.
   * @param idxFile      index File to write the index to.
   * @param errlog       error messages.
   *                     Return null if version doesnt match or file is malformed.
   */
  public static boolean writeGrib1Index(String dataLocation, File idxFile, Formatter errlog) throws IOException {
    Map<Long, Integer> gdsMap = new HashMap<>();
    ArrayList<Grib1SectionGridDefinition> gdsList = new ArrayList<>();
    ArrayList<Grib1Record> records = new ArrayList<>();

    var idxBuilder = dev.ucdm.grib.protogen.Grib1IndexProto.Grib1Index.newBuilder();
    idxBuilder.setFilename(dataLocation);

    // read from the data file
    try (RandomAccessFile gribDataFile = new RandomAccessFile(dataLocation, "r")) {
      Grib1RecordScanner scan = new Grib1RecordScanner(gribDataFile);
      while (scan.hasNext()) {
        Grib1Record r = scan.next();
        if (r == null)
          break; // done
        records.add(r);

        Grib1SectionGridDefinition gdss = r.getGDSsection();
        Integer index = gdsMap.get(gdss.calcCRC());
        if (gdss.getPredefinedGridDefinition() >= 0) // skip predefined gds - they dont have raw bytes
          index = 0;
        else if (index == null) {
          gdsList.add(gdss);
          index = gdsList.size() - 1;
          gdsMap.put(gdss.calcCRC(), index);
          idxBuilder.addGdsList(publishGdsProto(gdss));
        }
        idxBuilder.addRecords(publishRecordProto(r, index));
      }
    }
    if (records.isEmpty()) {
      errlog.format("No GRIB1 records found in " + dataLocation);
      return false;
    }

    // write to the index file TODO write to temp file and switch
    try (FileOutputStream fout = new FileOutputStream(idxFile)) {
      //// header message
      fout.write(MAGIC_START.getBytes(StandardCharsets.UTF_8));
      Streams.writeVInt(fout, version);

      dev.ucdm.grib.protogen.Grib1IndexProto.Grib1Index index = idxBuilder.build();
      byte[] b = index.toByteArray();
      Streams.writeVInt(fout, b.length); // message size
      fout.write(b); // message - all in one gulp
      logger.debug("  made gbx9 index for {} size={}", dataLocation, b.length);
    }
    return true;
  }

  private static dev.ucdm.grib.protogen.Grib1IndexProto.Grib1Record publishRecordProto(Grib1Record r, int gdsIndex) {
    dev.ucdm.grib.protogen.Grib1IndexProto.Grib1Record.Builder b = dev.ucdm.grib.protogen.Grib1IndexProto.Grib1Record.newBuilder();

    b.setHeader(ByteString.copyFrom(r.getHeader()));

    b.setGribMessageStart(r.getIs().getStartPos());
    b.setGribMessageLength(r.getIs().getMessageLength());

    b.setGdsIdx(gdsIndex);
    Grib1SectionProductDefinition pds = r.getPDSsection();
    b.setPds(ByteString.copyFrom(pds.getRawBytes()));

    if (pds.bmsExists()) {
      Grib1SectionBitMap bms = r.getBitMapSection();
      b.setBmsPos(bms.getStartingPosition());
    }

    Grib1SectionBinaryData ds = r.getDataSection();
    b.setDataPos(ds.getStartingPosition());
    b.setDataLen(ds.getLength());

    return b.build();
  }

  private static dev.ucdm.grib.protogen.Grib1IndexProto.Grib1GdsSection publishGdsProto(Grib1SectionGridDefinition gds) {
    dev.ucdm.grib.protogen.Grib1IndexProto.Grib1GdsSection.Builder b = dev.ucdm.grib.protogen.Grib1IndexProto.Grib1GdsSection.newBuilder();
    b.setGds(ByteString.copyFrom(gds.getRawBytes()));
    return b.build();
  }

}

