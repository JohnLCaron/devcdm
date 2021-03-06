/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.grib2.record;

import dev.ucdm.core.io.RandomAccessFile;
import dev.ucdm.grib.common.util.GribNumbers;

import dev.ucdm.array.Immutable;
import java.io.IOException;

/**
 * The Data section 7 for GRIB-2 files
 *
 * @author caron
 * @since 3/29/11
 */
@Immutable
public class Grib2SectionData {

  private final long startingPosition;
  private final int msgLength;

  public Grib2SectionData(RandomAccessFile raf) throws IOException {
    startingPosition = raf.getFilePointer();

    // octets 1-4 (Length of section in bytes)
    msgLength = GribNumbers.int4(raf);

    // octet 5
    int section = raf.read();
    if (section != 7) {
      throw new IllegalStateException("Not a Grib2SectionData (section 7)");
    }

    // skip to end of the data section
    raf.seek(startingPosition + msgLength);
  }

  public Grib2SectionData(long startingPosition, int msgLength) {
    this.startingPosition = startingPosition;
    this.msgLength = msgLength;
  }

  public long getStartingPosition() {
    return startingPosition;
  }

  public long getEndingPosition() {
    return startingPosition + msgLength;
  }

  public int getMsgLength() {
    return msgLength;
  }

  @Override
  public String toString() {
    return "Grib2SectionData{" + "startingPosition=" + startingPosition + ", msgLength=" + msgLength + '}';
  }
}
