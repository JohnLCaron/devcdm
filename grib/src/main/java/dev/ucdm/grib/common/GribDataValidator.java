package dev.ucdm.grib.common;

import dev.ucdm.grid.api.GridSubset;
import dev.ucdm.core.io.RandomAccessFile;

import java.io.IOException;

/** internal class for debugging. */
public interface GribDataValidator {
  void validate(GribTables cust, RandomAccessFile rafData, long pos, GridSubset coords) throws IOException;
}
