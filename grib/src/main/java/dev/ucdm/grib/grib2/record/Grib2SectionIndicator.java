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
 * The Indicator Section for GRIB-2 files
 *
 * @author caron
 * @since 3/28/11
 */
@Immutable
public class Grib2SectionIndicator {
  private static final byte[] MAGIC = {'G', 'R', 'I', 'B'};

  private final long messageLength;
  private final int discipline;
  private final long startPos;

  /**
   * Read Grib2SectionIndicator from raf.
   *
   * @param raf RandomAccessFile, with pointer at start (the "GRIB")
   * @throws IOException on I/O error
   * @throws IllegalArgumentException if not a GRIB-2 record
   */
  public Grib2SectionIndicator(RandomAccessFile raf) throws IOException {
    startPos = raf.getFilePointer();
    byte[] b = new byte[4];
    raf.readFully(b);
    for (int i = 0; i < b.length; i++)
      if (b[i] != MAGIC[i])
        throw new IllegalArgumentException("Not a GRIB record");

    raf.skipBytes(2);
    discipline = raf.read();
    int edition = raf.read();
    if (edition != 2)
      throw new IllegalArgumentException("Not a GRIB-2 record");

    messageLength = GribNumbers.int8(raf);
  }

  public Grib2SectionIndicator(long startPos, long messageLength, int discipline) {
    this.startPos = startPos;
    this.messageLength = messageLength;
    this.discipline = discipline;
  }

  /**
   * Get the length of this GRIB record in bytes.
   *
   * @return length in bytes of GRIB record
   */
  public long getMessageLength() {
    return messageLength;
  }

  public long getStartPos() {
    return startPos;
  }

  public long getEndPos() {
    return startPos + messageLength;
  }

  /**
   * Discipline - GRIB Master Table Number.
   *
   * @return discipline number
   */
  public int getDiscipline() {
    return discipline;
  }
}
