/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.grib1.record;

import dev.ucdm.array.Immutable;
import dev.ucdm.grib.common.util.GribNumbers;
import dev.ucdm.core.io.RandomAccessFile;
import java.io.IOException;

/** The Indicator Section for GRIB-1 files */
@Immutable
public class Grib1SectionIndicator {
  static final byte[] MAGIC = {'G', 'R', 'I', 'B'};

  private final long messageLength;
  private final long startPos;
  private final int messageLengthNotFixed;
  boolean isMessageLengthFixed;

  /**
   * Read Grib2SectionIndicator from raf.
   *
   * @param raf RandomAccessFile, with pointer at start (the "GRIB")
   * @throws IOException on I/O error
   * @throws IllegalArgumentException if not a GRIB-1 record
   */
  Grib1SectionIndicator(RandomAccessFile raf) throws IOException {
    startPos = raf.getFilePointer();
    byte[] b = new byte[4];
    raf.readFully(b);
    for (int i = 0; i < b.length; i++) {
      if (b[i] != MAGIC[i]) {
        throw new IllegalArgumentException("Not a GRIB record");
      }
    }

    messageLengthNotFixed = GribNumbers.uint3(raf);
    int edition = raf.read();
    if (edition != 1) {
      throw new IllegalArgumentException("Not a GRIB-1 record");
    }
    messageLength = Grib1RecordScanner.getFixedTotalLengthEcmwfLargeGrib(raf, messageLengthNotFixed);
    if (messageLength != messageLengthNotFixed) {
      isMessageLengthFixed = true;
    }
  }

  public Grib1SectionIndicator(long startPos, long messageLength) {
    this.startPos = startPos;
    this.messageLength = messageLength;
    this.messageLengthNotFixed = (int) messageLength;
    this.isMessageLengthFixed = false;
  }

  /**
   * Get the length of this GRIB record in bytes.
   *
   * @return length in bytes of GRIB record
   */
  public long getMessageLength() {
    return messageLength;
  }

  /**
   * Starting position of the entire GRIB message: the 'G' in GRIB
   * 
   * @return the starting position of the entire GRIB message
   */
  public long getStartPos() {
    return startPos;
  }

  /**
   * Ending position of the entire GRIB message: startPos + messageLength
   * 
   * @return the ending position of the entire GRIB message
   */
  public long getEndPos() {
    return startPos + messageLength;
  }

}

