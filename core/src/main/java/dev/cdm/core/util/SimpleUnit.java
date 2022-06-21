package dev.cdm.core.util;

// fake - replace with uom
public class SimpleUnit {
  public static final SimpleUnit kmUnit = new SimpleUnit();
  public static final SimpleUnit pressureUnit = new SimpleUnit();

  public static SimpleUnit factoryWithExceptions(String geoCoordinateUnits) {
    return new SimpleUnit();
  }

  public double convertTo(double value, SimpleUnit outputUnit) throws IllegalArgumentException {
    return 1.0;
  }

  public static boolean isDateUnit(String unit) {
    return false;
  }

  public static double getConversionFactor(String unit1, String unit2) {
    return 1.0;
  }

  public static boolean isCompatible(String unit1, String unit2) {
    return true;
  }

  public boolean isCompatible(String unit2) {
    return true;
  }
  }
