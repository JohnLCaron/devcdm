/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.core.iosp;

import dev.cdm.core.api.CdmFile;
import dev.cdm.core.api.Group;
import dev.cdm.core.api.Sequence;
import dev.cdm.core.api.Variable;
import dev.cdm.core.util.CancelTask;
import dev.cdm.core.io.RandomAccessFile;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;

/**
 * This is the service provider interface for the low-level I/O access classes (read only).
 * This is only used by service implementors.
 *
 * An implementation must have a no-argument constructor.
 *
 * The CdmFile class manages all registered IOServiceProvider classes.
 * When NetcdfFiles.open() is called:
 * <ol>
 * <li>the file is opened as a ucar.unidata.io.RandomAccessFile;</li>
 * <li>the file is handed to the isValidFile() method of each registered
 * IOServiceProvider class (until one returns true, which means it can read the file).</li>
 * <li>the open() method on the resulting IOServiceProvider class is handed the file.</li>
 */
public interface IOServiceProvider extends Closeable {

  /**
   * Check if this is a valid file for this IOServiceProvider.
   * You must make this method thread safe, ie dont keep any state.
   * 
   * @param raf RandomAccessFile
   * @return true if valid.
   * @throws IOException if read error
   */
  boolean isValidFile(RandomAccessFile raf) throws IOException;

  /**
   * Read an existing RandomAccessFile, and populate rootGroup.
   * Note that you cannot reference the CdmFile within this routine, since it hasnt been created yet.
   *
   * @param raf the file to work on, it has already passed the isValidFile() test.
   * @param rootGroup add objects to the root group.
   * @param cancelTask used to monitor user cancellation; may be null.
   * @throws IOException if read error
   */
  void build(RandomAccessFile raf, Group.Builder rootGroup, CancelTask cancelTask) throws IOException;

  /** Sometimes the builder needs access to the finished objects. This is called when ncfile is finished being built. */
  void buildFinish(CdmFile ncfile);

  /**
   * Read data from a top level Variable and return a memory resident Array. This Array has the same element type as the
   * Variable, and the requested shape.
   *
   * @param v2 a top-level Variable
   * @param section the section of data to read. There must be a Range for each Dimension in the variable, in order.
   *        Note: no nulls allowed. IOSP may not modify.
   * @return the requested data in a memory-resident Array
   */
  dev.cdm.array.Array<?> readArrayData(Variable v2, dev.cdm.array.Section section)
      throws IOException, dev.cdm.array.InvalidRangeException;

  /**
   * Get the structure iterator. Iosps with top level sequences must override.
   *
   * @param s the Structure
   * @param bufferSize the buffersize, may be -1 for default.
   * @return an iterator over the StructureData
   */
  Iterator<dev.cdm.array.StructureData> getSequenceIterator(Sequence s, int bufferSize);

  /**
   * Close the file.
   * It is the IOServiceProvider's job to close the file (even though it didnt open it),
   * and to free any other resources it has used.
   *
   * @throws IOException if read error
   */
  void close() throws IOException;

  /** Get last time the file was modified. */
  long getLastModified();

  /**
   * A way to communicate arbitrary information to and from an iosp.
   * 
   * @param message opaque message sent to the IOSP object when its opened (not when isValidFile() is called)
   * @return opaque Object, may be null.
   */
  @Nullable
  Object sendIospMessage(@Nullable Object message);

  /**
   * Debug info for this object.
   * 
   * @param o which object
   * @return debug info for this object
   */
  String toStringDebug(Object o);

  /**
   * Show debug / underlying implementation details
   * 
   * @return debug info
   */
  String getDetailInfo();

  /**
   * Get a unique id for this file type.
   * 
   * @return registered id of the file type
   * @see "https://www.unidata.ucar.edu/software/netcdf-java/formats/FileTypes.html"
   */
  String getFileTypeId();

  /**
   * Get the version of this file type.
   * 
   * @return version of the file type
   * @see "https://www.unidata.ucar.edu/software/netcdf-java/formats/FileTypes.html"
   */
  String getFileTypeVersion();

  /**
   * Get a human-readable description for this file type.
   * 
   * @return description of the file type
   * @see "https://www.unidata.ucar.edu/software/netcdf-java/formats/FileTypes.html"
   */
  String getFileTypeDescription();

}
