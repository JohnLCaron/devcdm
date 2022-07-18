/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.common;

import org.jetbrains.annotations.Nullable;

import com.google.common.base.Preconditions;

import dev.ucdm.grib.collection.CollectionSingleFile;
import dev.ucdm.grib.collection.CollectionUpdateType;
import dev.ucdm.grib.collection.Grib1Collection;
import dev.ucdm.grib.collection.Grib1CollectionBuilder;
import dev.ucdm.grib.collection.Grib2Collection;
import dev.ucdm.grib.collection.Grib2CollectionBuilder;
import dev.ucdm.grib.protoconvert.Grib1CollectionIndexReader;
import dev.ucdm.grib.protoconvert.Grib1CollectionIndexWriter;
import dev.ucdm.grib.protoconvert.Grib2CollectionIndexReader;
import dev.ucdm.grib.collection.GribCollection;
import dev.ucdm.grib.collection.MCollection;
import dev.ucdm.grib.collection.MFile;
import dev.ucdm.grib.collection.MFileOS;
import dev.ucdm.grib.common.util.GribIndexCache;
import dev.ucdm.grib.grib1.record.Grib1RecordScanner;
import dev.ucdm.grib.grib2.record.Grib2RecordScanner;
import dev.ucdm.grib.protoconvert.Grib2CollectionIndexWriter;
import dev.ucdm.grib.protoconvert.GribCollectionIndexWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.ucdm.core.io.RandomAccessFile;
import dev.ucdm.core.util.StringUtil2;
import java.io.File;
import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.util.Formatter;


/** Logic for creating / managing ncx4 indices. */
public class GribCollectionIndex {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GribCollectionIndex.class);

  public enum Type {
    GRIB1, GRIB2, Partition1, Partition2, none
  }

  public static final String NCX_SUFFIX = ".ncx4";

  private static final Logger classLogger = LoggerFactory.getLogger(GribCollectionIndex.class);

  // raf is a data file or an ncx file
  @Nullable
  public static GribCollection openGribCollectionFromRaf(
          RandomAccessFile raf, CollectionUpdateType updateType, GribConfig config, Formatter errlog) throws IOException {

    GribCollection result = null;

    // check if its a plain ole GRIB1/2 data file
    boolean isGrib1 = false;
    boolean isGrib2 = Grib2RecordScanner.isValidFile(raf);
    if (!isGrib2) {
      isGrib1 = Grib1RecordScanner.isValidFile(raf);
    }

    if (isGrib1 || isGrib2) {
      result = openGribCollectionFromDataFile(isGrib1, raf, updateType, config, errlog);
      // TODO close the data file, the ncx raf file is managed by gribCollection ??
      // raf.close();
    } else {
      // check if its a collection dataset
      if (getType(raf) == Type.none) {
        return null;
      }
      result = openNcxIndex(raf.getLocation(), config, false);
      // TODO close the data file, the ncx raf file is managed by gribCollection ??
      // raf.close();
    }

    return result;
  }

  // raf is a data fileGribGridDataset
  public static GribCollection openGribCollectionFromDataFile(boolean isGrib1, RandomAccessFile dataRaf,
      CollectionUpdateType updateType, GribConfig config, Formatter errlog)
      throws IOException {

    File dataFile = new File(dataRaf.getLocation());
    MFile mfile = new MFileOS(dataFile);
    MCollection dcm = new CollectionSingleFile(mfile).setAuxInfo(GribConfig.AUX_CONFIG, config);
    return readOrCreateCollectionFromIndex(isGrib1, dcm, updateType, config, errlog);
  }

  @Nullable
  public static GribCollection readOrCreateCollectionFromIndex(
          boolean isGrib1, MCollection dcm, CollectionUpdateType force, GribConfig config, Formatter errlog) throws IOException {

    String idxPath = dcm.getIndexFilename(NCX_SUFFIX);
    // look to see if the file is in some special cache (eg when cant write to data directory)
    File idxFile = GribIndexCache.getExistingFileOrCache(idxPath);
    boolean idxFileExists = idxFile != null;

    GribCollection gribCollection = null;
    if (idxFileExists && force != CollectionUpdateType.always) { // always create a new index
      // look to see if the index file is older than the collection
      boolean isOlder = idxFile.lastModified() < dcm.getLastModified();

      if (force != CollectionUpdateType.nocheck && !isOlder) {
        // try to read it
        gribCollection = openNcxIndex(dcm.getIndexFilename(NCX_SUFFIX), config, false);
      }
    }

    if (gribCollection == null) {
      // may not exist, overwrite if it does.
      File idxFile2 = GribIndexCache.getFileOrCache(idxPath);
      if (idxFile2 == null) {
        errlog.format("Failed to find a place to write the index file for '%s'", idxPath);
        return null;
      }

      if (!createNcxIndex(isGrib1, dcm, errlog)) {
        logger.warn("  Index writing failed on {} errlog = '{}'", idxFile2, errlog);

      } else {
        // read it back in
        gribCollection = openNcxIndex(dcm.getIndexFilename(NCX_SUFFIX), config, false);
        logger.debug("  Index written: {}", idxPath);
      }
    } else {
      logger.debug("  Index read: {}", idxPath);
    }

    return gribCollection;
  }

  // create ncx file
  private static boolean createNcxIndex(boolean isGrib1, MCollection dcm, Formatter errlog) throws IOException {
    if (isGrib1) {
      Grib1CollectionBuilder builder = new Grib1CollectionBuilder(dcm.getCollectionName(), dcm, logger);
      if (!builder.createIndex(errlog)) {
        return false;
      }
    } else {
      Grib2CollectionBuilder builder = new Grib2CollectionBuilder(dcm.getCollectionName(), dcm, logger);
      if (!builder.createIndex(errlog)) {
        return false;
      }
    }
    return true;
  }

  // open GribCollection from an existing ncx file. return null on failure
  @Nullable
  public static GribCollection openNcxIndex(String indexFilename, GribConfig config, boolean useCache) throws IOException {

    File indexFileInCache = useCache ? GribIndexCache.getExistingFileOrCache(indexFilename) : new File(indexFilename);
    if (indexFileInCache == null)
      return null;
    String indexFilenameInCache = indexFileInCache.getPath();
    String name = makeNameFromIndexFilename(indexFilename);

    GribCollection result = null;
    try (RandomAccessFile raf = new RandomAccessFile(indexFilenameInCache, "r")) {
      GribCollectionIndex.Type collectionType = getType(raf);
      if (collectionType == Type.none) {
        return null;
      }

      boolean isGrib1 = (collectionType == Type.GRIB1) || (collectionType == Type.Partition1);
      if (isGrib1) {
        result = new Grib1Collection(name, null, config);
        Grib1CollectionIndexReader reader = new Grib1CollectionIndexReader(result, config);
        reader.readIndex(raf);

      } else {
        result = new Grib2Collection(name, null, config);
        Grib2CollectionIndexReader reader = new Grib2CollectionIndexReader(result, config);
        reader.readIndex(raf);
      }
    }

    return result;
  }

  public static String makeNameFromIndexFilename(String idxPathname) {
    idxPathname = StringUtil2.replace(idxPathname, '\\', "/");
    int pos = idxPathname.lastIndexOf('/');
    String idxFilename = (pos < 0) ? idxPathname : idxPathname.substring(pos + 1);
    Preconditions.checkArgument(idxFilename.endsWith(NCX_SUFFIX), idxFilename);
    return idxFilename.substring(0, idxFilename.length() - NCX_SUFFIX.length());
  }

  /** Find out what kind of index this is */
  public static GribCollectionIndex.Type getType(RandomAccessFile raf) throws IOException {
    // they all have the same number of bytes
    raf.seek(0);
    String magic = raf.readString(Grib2CollectionIndexWriter.MAGIC_START.getBytes(StandardCharsets.UTF_8).length);
    //System.out.printf("%s GribCollectionIndex has magic '%s'%n", raf.getLocation(), magic);

    return switch (magic) {
      case Grib2CollectionIndexWriter.MAGIC_START -> Type.GRIB2;
      case Grib1CollectionIndexWriter.MAGIC_START -> Type.GRIB1;
      case GribCollectionIndexWriter.PARTITION2_START -> Type.Partition2;
      case GribCollectionIndexWriter.PARTITION1_START -> Type.Partition1;
      default -> Type.none;
    };
  }

}
