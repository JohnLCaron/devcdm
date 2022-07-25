package dev.ucdm.grib.coord;

public record EnsCoordValue(int code, int ensMember) implements Comparable<EnsCoordValue> {

  @Override
  public int compareTo(EnsCoordValue o) {
    int r = Integer.compare(code, o.code);
    if (r != 0) {
      return r;
    }
    return Integer.compare(ensMember, o.ensMember);
  }

  public String toString() {
    return String.format("(%d %d)", code, ensMember);
  }
}
