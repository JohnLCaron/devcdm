package dev.cdm.dataset.api;

import dev.cdm.dataset.unit.UdunitFormat;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.quantity.Length;

import static javax.measure.MetricPrefix.KILO;
import static tech.units.indriya.unit.Units.METRE;

public record SimpleUnit<T extends Quantity<T>> (Unit<T> unit) {
  static UdunitFormat udunitFormat = new UdunitFormat();

  public static final SimpleUnit<Length> kmUnit = new SimpleUnit(KILO(METRE));

  public static <T extends Quantity<T>> SimpleUnit<T> factoryWithExceptions(String geoCoordinateUnits) {
    return (SimpleUnit<T>) (new SimpleUnit(udunitFormat.parse(geoCoordinateUnits)));
  }

  public static <T extends Quantity<T>> double getConversionFactor(String unit1s, String unit2s) {
    System.out.printf("SimpleUnit getConversionFactor %s %s%n", unit1s, unit2s);
    Unit<T> unit1 = (Unit<T>) udunitFormat.parse(unit1s);
    Unit<T> unit2 = (Unit<T>) udunitFormat.parse(unit2s);
    UnitConverter scale2 = unit1.getConverterTo(unit2);
    return scale2.convert(1.0);
  }

  public static boolean isCompatible(String unit1s, String unit2s) {
    System.out.printf("SimpleUnit %s isCompatible %s %s%n", udunitFormat.getClass().getName(), unit1s, unit2s);
    Unit<?> unit1 = udunitFormat.parse(unit1s);
    Unit<?> unit2 = udunitFormat.parse(unit2s);
    return unit1.isCompatible(unit2);
  }

  //////////////////////////////////////////////////////////////////////

  public double convertTo(double value, SimpleUnit<T> outputUnit) throws IllegalArgumentException {
    UnitConverter scale2 = unit.getConverterTo(outputUnit.unit);
    return scale2.convert(value);
  }

}
