package dev.cdm.dataset.api;

import dev.cdm.dataset.unit.UdunitFormat;
import org.jetbrains.annotations.Nullable;
import tech.units.indriya.unit.Units;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.quantity.Length;

import static javax.measure.MetricPrefix.KILO;
import static tech.units.indriya.unit.Units.METRE;
import static tech.units.indriya.unit.Units.PASCAL;

public record SimpleUnit<T extends Quantity<T>> (Unit<T> unit) {
  static UdunitFormat udunitFormat = new UdunitFormat();

  public static final SimpleUnit<Length> kmUnit = new SimpleUnit(KILO(METRE));
  public static final SimpleUnit<Length> pressureUnit = new SimpleUnit(PASCAL);
  public static final SimpleUnit<Length> geopotentialHeight = new SimpleUnit(Units.METRE_PER_SECOND.multiply(Units.METRE_PER_SECOND).multiply(9.80665));


  public static <T extends Quantity<T>> SimpleUnit<T> factoryWithExceptions(String geoCoordinateUnits) {
    return (SimpleUnit<T>) (new SimpleUnit(udunitFormat.parse(geoCoordinateUnits)));
  }

  @Nullable
  public static <T extends Quantity<T>> SimpleUnit<T> factory(@Nullable String units) {
    if (units == null) {
      return null;
    }
    try {
      return (SimpleUnit<T>) (new SimpleUnit(udunitFormat.parse(units)));
    } catch (Throwable t) {
      return null;
    }
  }

  public static <T extends Quantity<T>> double getConversionFactor(String unit1s, String unit2s) {
    Unit<T> unit1 = (Unit<T>) udunitFormat.parse(unit1s);
    Unit<T> unit2 = (Unit<T>) udunitFormat.parse(unit2s);
    UnitConverter scale2 = unit1.getConverterTo(unit2);
    return scale2.convert(1.0);
  }

  public static boolean isCompatible(String unit1s, String unit2s) {
    Unit<?> unit1 = udunitFormat.parse(unit1s);
    Unit<?> unit2 = udunitFormat.parse(unit2s);
    if (unit1 == null || unit2 == null) {
      return false;
    }
    return unit1.isCompatible(unit2);
  }

  //////////////////////////////////////////////////////////////////////

  public double convertTo(double value, SimpleUnit<T> outputUnit) throws IllegalArgumentException {
    UnitConverter scale2 = unit.getConverterTo(outputUnit.unit);
    return scale2.convert(value);
  }

  public boolean isCompatible(String other) {
    Unit<?> otherUnit = udunitFormat.parse(other);
    if (otherUnit == null) {
      return false;
    }
    return this.unit.isCompatible(otherUnit);
  }

  public boolean isCompatible(@Nullable SimpleUnit other) {
    if (other == null || this.unit == null) {
      return false;
    }
    return this.unit.isCompatible(other.unit);
  }

}
