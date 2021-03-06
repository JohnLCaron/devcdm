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
 * The Data Representation section (5) for GRIB-2 files
 *
 * @author caron
 * @since 3/29/11
 */
@Immutable
public class Grib2SectionDataRepresentation {
  private final long startingPosition;
  private final int dataPoints;
  private final int dataTemplate;
  final int length; // dont have length in index

  public Grib2SectionDataRepresentation(RandomAccessFile raf) throws IOException {
    startingPosition = raf.getFilePointer();

    // octets 1-4 (Length of DRS)
    length = GribNumbers.int4(raf);
    if (length == 0) {
      throw new IllegalArgumentException("Not a GRIB-2 Data representation section");
    }

    // octet 5
    int section = raf.read();
    if (section != 5) {
      throw new IllegalArgumentException("Not a GRIB-2 Data representation section");
    }

    // octets 6-9 number of datapoints
    dataPoints = GribNumbers.int4(raf);

    // octet 10
    int dt = GribNumbers.uint2(raf);
    dataTemplate = (dt == 40000) ? 40 : dt; // ?? NCEP bug ??

    raf.seek(startingPosition + length);
  }

  public Grib2SectionDataRepresentation(long startingPosition, int dataPoints, int dataTemplate) {
    this.startingPosition = startingPosition;
    this.dataPoints = dataPoints;
    this.dataTemplate = dataTemplate;
    this.length = 0;
  }

  /**
   * Number of data points where one or more values are specified in Section 7 when a bit map
   * is present, total number of data points when a bit map is absent.
   */
  public int getDataPoints() {
    return dataPoints;
  }

  public int getDataTemplate() {
    return dataTemplate;
  }

  public long getStartingPosition() {
    return startingPosition;
  }

  // debug
  public long readLength(RandomAccessFile raf) throws IOException {
    if (length == 0) {
      raf.seek(startingPosition);
      return GribNumbers.int4(raf);
    }
    return length;
  }

  public Grib2Drs getDrs(RandomAccessFile raf) throws IOException {
    raf.seek(startingPosition + 11);
    return Grib2Drs.factory(dataTemplate, raf);
  }

  @Override
  public String toString() {
    return "Grib2SectionDataRepresentation{" + "startingPosition=" + startingPosition + ", dataPoints=" + dataPoints
        + ", dataTemplate=" + dataTemplate + ", length=" + length + '}';
  }
}
