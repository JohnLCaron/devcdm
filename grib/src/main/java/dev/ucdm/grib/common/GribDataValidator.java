package dev.ucdm.grib.common;

import dev.cdm.grid.api.GridSubset;
import dev.cdm.core.io.RandomAccessFile;
import dev.ucdm.grib.common.GribTables;

import java.io.IOException;

/** internal class for debugging. */
public interface GribDataValidator {
  void validate(GribTables cust, RandomAccessFile rafData, long pos, GridSubset coords) throws IOException;
}
