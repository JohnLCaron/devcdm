package dev.cdm.grib.collection;

import dev.cdm.grib.util.GribTables;
import dev.cdm.grid.api.GridSubset;
import dev.cdm.core.io.RandomAccessFile;
import java.io.IOException;

/** internal class for debugging. */
public interface GribDataValidator {
  void validate(GribTables cust, RandomAccessFile rafData, long pos, GridSubset coords) throws IOException;
}
