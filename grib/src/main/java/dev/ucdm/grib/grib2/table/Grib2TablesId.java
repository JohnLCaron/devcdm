package dev.ucdm.grib.grib2.table;

import dev.ucdm.array.Immutable;

@Immutable
public record Grib2TablesId(int center, int subCenter, int masterVersion, int localVersion, int genProcessId) {
  public enum Type {
    wmo, cfsr, gempak, gsd, kma, ncep, ndfd, mrms, nwsDev, eccodes
  }

  boolean match(Grib2TablesId id) {
    if (id.center != center)
      return false; // must match center
    if (subCenter != -1 && id.subCenter != subCenter)
      return false;
    if (masterVersion != -1 && id.masterVersion != masterVersion)
      return false;
    if (localVersion != -1 && id.localVersion != localVersion)
      return false;
    return genProcessId == -1 || id.genProcessId == genProcessId;
  }
}
