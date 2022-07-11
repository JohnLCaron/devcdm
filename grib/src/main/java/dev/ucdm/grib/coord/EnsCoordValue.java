package dev.ucdm.grib.coord;

import java.util.Formatter;
import javax.annotation.Nonnull;
import dev.cdm.array.Immutable;

@Immutable
public class EnsCoordValue implements Comparable<EnsCoordValue> {
  private final int code; // pds.getPerturbationType()
  private final int ensMember; // pds.getPerturbationNumber()

  public EnsCoordValue(int code, int ensMember) {
    this.code = code;
    this.ensMember = ensMember;
  }

  public int getCode() {
    return code;
  }

  public int getEnsMember() {
    return ensMember;
  }

  @Override
  public int compareTo(@Nonnull EnsCoordValue o) {
    int r = Integer.compare(code, o.code);
    if (r != 0)
      return r;
    return Integer.compare(ensMember, o.ensMember);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EnsCoordValue that = (EnsCoordValue) o;
    return code == that.code && ensMember == that.ensMember;
  }

  @Override
  public int hashCode() {
    int result = 17;
    result += 31 * ensMember;
    result += 31 * code;
    return result;
  }

  public String toString() {
    try (Formatter out = new Formatter()) {
      out.format("(%d %d)", code, ensMember);
      return out.toString();
    }
  }
}
