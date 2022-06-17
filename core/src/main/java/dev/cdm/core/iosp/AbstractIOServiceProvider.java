/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.core.iosp;

import dev.cdm.array.StructureData;
import dev.cdm.core.api.CdmFile;
import dev.cdm.core.api.Sequence;
import dev.cdm.core.io.RandomAccessFile;
import dev.cdm.core.util.Format;

import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Formatter;
import java.util.Iterator;

/** Abstract base class for IOSP implementations. */
public abstract class AbstractIOServiceProvider implements IOServiceProvider {
  protected RandomAccessFile raf;
  protected String location;
  protected int rafOrder = RandomAccessFile.BIG_ENDIAN;
  protected CdmFile ncfile;

  public void setRaf(RandomAccessFile raf) {
    this.raf = raf;
    this.location = raf.getLocation();
  }

  @Override
  public void buildFinish(CdmFile ncfile) {
    this.ncfile = ncfile;
  }

  @Override
  public void close() throws IOException {
    if (raf != null)
      raf.close();
    raf = null;
  }

  @Override
  public Iterator<StructureData> getSequenceIterator(Sequence s, int bufferSize) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Nullable
  public Object sendIospMessage(@Nullable Object message) {
    if (message == CdmFile.IOSP_MESSAGE_RANDOM_ACCESS_FILE) {
      return raf;
    }
    return null;
  }

  /**
   * Returns the time that the underlying file(s) was last modified, in unix time.
   */
  @Override
  public long getLastModified() {
    if (location != null) {
      File file = new File(location);
      return file.lastModified();
    } else {
      return 0;
    }
  }

  @Override
  public String toStringDebug(Object o) {
    return "";
  }

  @Override
  public String getDetailInfo() {
    if (raf == null)
      return "";
    try {
      Formatter fout = new Formatter();
      double size = raf.length() / (1000.0 * 1000.0);
      fout.format(" raf = %s%n", raf.getLocation());
      fout.format(" size= %d (%s Mb)%n%n", raf.length(), Format.dfrac(size, 3));
      return fout.toString();

    } catch (IOException e) {
      return e.getMessage();
    }
  }

  @Override
  public String getFileTypeVersion() {
    return "N/A";
  }

}
