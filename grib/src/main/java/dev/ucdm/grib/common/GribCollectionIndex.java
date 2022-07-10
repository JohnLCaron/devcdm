/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.common;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

import dev.ucdm.grib.collection.CollectionSingleFile;
import dev.ucdm.grib.collection.Grib1Collection;
import dev.ucdm.grib.collection.Grib2Collection;
import dev.ucdm.grib.collection.Grib2CollectionBuilder;
import dev.ucdm.grib.protoconvert.Grib1CollectionIndexReader;
import dev.ucdm.grib.protoconvert.Grib2CollectionIndexReader;
import dev.ucdm.grib.collection.GribCollection;
import dev.ucdm.grib.collection.MCollection;
import dev.ucdm.grib.collection.MFile;
import dev.ucdm.grib.collection.MFileOS;
import dev.ucdm.grib.common.util.GribIndexCache;
import dev.ucdm.grib.grib1.record.Grib1RecordScanner;
import dev.ucdm.grib.grib2.record.Grib2RecordScanner;
import dev.ucdm.grib.protoconvert.Grib1IndexProto;
import dev.ucdm.grib.protoconvert.Grib2IndexProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.cdm.core.io.RandomAccessFile;
import dev.cdm.core.util.StringUtil2;
import java.io.File;
import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.util.Formatter;


/**
 * Utilities for creating GRIB CDM index (ncx) files, both collections and partitions.
 * Can be used as a standalone program.
 * TODO review - is this working?
 */
public class GribCollectionIndex {

  public enum Type {
    GRIB1, GRIB2, Partition1, Partition2, none
  }

  public static final String NCX_SUFFIX = ".ncx4";

  private static final Logger classLogger = LoggerFactory.getLogger(GribCollectionIndex.class);


  ////////////////////////////////////////////////////////////////////////////////////
  // Used by IOSPs

  // raf is a data file or an ncx file
  @Nullable
  public static GribCollection openGribCollectionFromRaf(RandomAccessFile raf,
                                                         CollectionUpdateType updateType, GribConfig config, Logger logger) throws IOException {

    GribCollection result = null;

    // check if its a plain ole GRIB1/2 data file
    boolean isGrib1 = false;
    boolean isGrib2 = Grib2RecordScanner.isValidFile(raf);
    if (!isGrib2) {
      isGrib1 = Grib1RecordScanner.isValidFile(raf);
    }

    if (isGrib1 || isGrib2) {
      result = openGribCollectionFromDataFile(isGrib1, raf, updateType, config, null, logger);
      // close the data file, the ncx raf file is managed by gribCollection
      raf.close();
    } else {
      // see if its an ncx file
      result = openNcxIndex(isGrib1, raf.getLocation(), config, false, logger);
    }

    return result;
  }

  // raf is a data file
  public static GribCollection openGribCollectionFromDataFile(boolean isGrib1, RandomAccessFile dataRaf,
      CollectionUpdateType updateType, GribConfig config, Formatter errlog, Logger logger)
      throws IOException {

    File dataFile = new File(dataRaf.getLocation());
    MFile mfile = new MFileOS(dataFile);
    MCollection dcm = new CollectionSingleFile(mfile).setAuxInfo(GribConfig.AUX_CONFIG, config);
    return readOrCreateCollectionFromIndex(isGrib1, dcm, updateType, config, errlog, logger);
  }

  @Nullable
  public static GribCollection readOrCreateCollectionFromIndex(
          boolean isGrib1, MCollection dcm, CollectionUpdateType force, GribConfig config, Formatter errlog, Logger logger) throws IOException {

    String idxPath = dcm.getIndexFilename(NCX_SUFFIX);
    // look to see if the file is in some special cache (eg when cant write to data directory)
    File idxFile = GribIndexCache.getExistingFileOrCache(idxPath);
    boolean idxFileExists = idxFile != null;

    GribCollection index = null;
    if (idxFileExists && force != CollectionUpdateType.always) { // always create a new index
      // look to see if the index file is older than the collection
      boolean isOlder = idxFile.lastModified() < dcm.getLastModified();

      if (force == CollectionUpdateType.nocheck || isOlder) {
        // try to read it
        index = openNcxIndex(isGrib1, dcm.getIndexFilename(NCX_SUFFIX), config, false, logger);
      }
    }

    if (index == null) {
      // may not exist, overwrite if it does.
      File idxFile2 = GribIndexCache.getFileOrCache(idxPath);
      if (idxFile2 == null) {
        errlog.format("Failed to find a place to write the index file for '%s'", idxPath);
        return null;
      }

      // create the ncx
      Grib2CollectionBuilder builder = new Grib2CollectionBuilder(dcm.getCollectionName(), dcm, logger);
      if (!builder.createIndex(errlog)) {
        logger.warn("  Index writing failed on {} errlog = '{}'", idxFile2, errlog);
      } else {
        // read it back in
        index = openNcxIndex(isGrib1, dcm.getIndexFilename(NCX_SUFFIX), config, false, logger);
        logger.debug("  Index written: {}", idxPath);
      }
    } else {
      logger.debug("  Index read: {}", idxPath);
    }

    return index;
  }

  // open GribCollection from an existing ncx file. return null on failure
  @Nullable
  public static GribCollection openNcxIndex(boolean isGrib1,
          String indexFilename, GribConfig config, boolean useCache, Logger logger) throws IOException {

    File indexFileInCache = useCache ? GribIndexCache.getExistingFileOrCache(indexFilename) : new File(indexFilename);
    if (indexFileInCache == null)
      return null;
    String indexFilenameInCache = indexFileInCache.getPath();
    String name = makeNameFromIndexFilename(indexFilename);

    GribCollection result = null;
    if (isGrib1) {
      result = new Grib1Collection(name, null, config);
      Grib1CollectionIndexReader reader = new Grib1CollectionIndexReader(result, config, logger);
      try (RandomAccessFile raf = new RandomAccessFile(indexFilenameInCache, "r")) {
        reader.readIndex(raf);
      }
    } else {
      result = new Grib2Collection(name, null, config);
      Grib2CollectionIndexReader reader = new Grib2CollectionIndexReader(result, config, logger);
      try (RandomAccessFile raf = new RandomAccessFile(indexFilenameInCache, "r")) {
        reader.readIndex(raf);
      }
    }

    return result;
  }

  private static String makeNameFromIndexFilename(String idxPathname) {
    idxPathname = StringUtil2.replace(idxPathname, '\\', "/");
    int pos = idxPathname.lastIndexOf('/');
    String idxFilename = (pos < 0) ? idxPathname : idxPathname.substring(pos + 1);
    Preconditions.checkArgument(idxFilename.endsWith(NCX_SUFFIX), idxFilename);
    return idxFilename.substring(0, idxFilename.length() - NCX_SUFFIX.length());
  }

  /**
   * Find out what kind of index this is
   *
   * @param raf open RAF
   * @return GribCollectionType
   * @throws IOException on read error
   */
  public static GribCollectionIndex.Type getType(RandomAccessFile raf) throws IOException {
    String magic;

    // they all have the same number of bytes
    raf.seek(0);
    magic = raf.readString(Grib2IndexProto.MAGIC_START.getBytes(StandardCharsets.UTF_8).length);

    switch (magic) {
      case Grib2IndexProto.MAGIC_START:
        return GribCollectionIndex.Type.GRIB2;

      case Grib1IndexProto.MAGIC_START:
        return GribCollectionIndex.Type.GRIB1;

      /* case Grib2PartitionBuilder.MAGIC_START:
        return GribCollectionIndex.Type.Partition2;

      case Grib1PartitionBuilder.MAGIC_START:
        return GribCollectionIndex.Type.Partition1; */

    }
    return GribCollectionIndex.Type.none;
  }

}
