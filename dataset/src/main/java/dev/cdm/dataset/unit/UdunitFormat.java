/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.dataset.unit;


import tech.units.indriya.format.SimpleUnitFormat;
import tech.units.indriya.unit.Units;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.format.MeasurementParseException;
import javax.measure.quantity.AmountOfSubstance;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.ElectricCapacitance;
import javax.measure.quantity.ElectricCharge;
import javax.measure.quantity.ElectricConductance;
import javax.measure.quantity.ElectricCurrent;
import javax.measure.quantity.ElectricInductance;
import javax.measure.quantity.ElectricPotential;
import javax.measure.quantity.ElectricResistance;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Force;
import javax.measure.quantity.Frequency;
import javax.measure.quantity.Illuminance;
import javax.measure.quantity.Length;
import javax.measure.quantity.LuminousFlux;
import javax.measure.quantity.LuminousIntensity;
import javax.measure.quantity.MagneticFlux;
import javax.measure.quantity.MagneticFluxDensity;
import javax.measure.quantity.Mass;
import javax.measure.quantity.Power;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.RadiationDoseAbsorbed;
import javax.measure.quantity.RadiationDoseEffective;
import javax.measure.quantity.Radioactivity;
import javax.measure.quantity.SolidAngle;
import javax.measure.quantity.Temperature;
import javax.measure.quantity.Time;
import javax.measure.quantity.Volume;
import javax.measure.spi.ServiceProvider;
import java.io.IOException;
import java.text.ParsePosition;
import java.util.Map;

public class UdunitFormat extends SimpleUnitFormat {

  // base units
  static Unit<ElectricCurrent> ampere = Units.AMPERE;
  static Unit<LuminousIntensity> candela = Units.CANDELA;
  static Unit<Temperature> kelvin = Units.KELVIN;
  static Unit<Mass> kilogram = Units.KILOGRAM;
  static Unit<Length> meter = Units.METRE;
  static Unit<AmountOfSubstance> mole = Units.MOLE;
  static Unit<Time> second = Units.SECOND;
  static Unit<Angle> radian = Units.RADIAN;
  static Unit<SolidAngle> steradian = Units.STERADIAN;


  static Unit<Temperature> celsius = Units.CELSIUS;
  static Unit<ElectricCharge> coulomb = Units.COULOMB;
  static Unit<ElectricCapacitance> farad = Units.FARAD;
  static Unit<ElectricInductance> henry = Units.HENRY;
  static Unit<Frequency> hertz = Units.HERTZ;
  static Unit<Energy> joule = Units.JOULE;
  static Unit<Volume> liter = Units.LITRE;
  static Unit<LuminousFlux> lumen = Units.LUMEN;
  static Unit<Illuminance> lux = Units.LUX;
  static Unit<Force> newton = Units.NEWTON;
  static Unit<ElectricResistance> ohm = Units.OHM;
  static Unit<Pressure> pascal = Units.PASCAL;
  static Unit<Dimensionless> percent = Units.PERCENT;
  static Unit<ElectricConductance> siemens = Units.SIEMENS;
  static Unit<MagneticFluxDensity> tesla = Units.TESLA;
  static Unit<ElectricPotential> volt = Units.VOLT;
  static Unit<Power> watt = Units.WATT;
  static Unit<MagneticFlux> weber = Units.WEBER;

  static Unit<Radioactivity> becquerel = Units.BECQUEREL;
  static Unit<RadiationDoseAbsorbed> gray = Units.GRAY;
  static Unit<RadiationDoseEffective> sievert = Units.SIEVERT;

  static Unit<Time> minute = Units.MINUTE;
  static Unit<Time> day = Units.DAY;
  static Unit<Time> hour = Units.HOUR;

  /////////////////////////////////////

  private static SimpleUnitFormat delegate = (SimpleUnitFormat) ServiceProvider.current().getFormatService().getUnitFormat("SIMPLE_ASCII");

  static {
    delegate.alias(Units.PERCENT, "percent");
    delegate.alias(Units.PASCAL, "pa");
    delegate.alias(Units.PASCAL.multiply(100), "mbar");
    delegate.alias(Units.PASCAL.multiply(100), "millibar");
    delegate.alias(Units.CELSIUS, "degC");

    delegate.alias(Units.HOUR, "hours");
    delegate.alias(Units.MINUTE, "minutes");
    delegate.alias(Units.SECOND, "seconds");
  }

  @Override
  public Appendable format(Unit<?> unit, Appendable appendable) throws IOException {
    return delegate.format(unit, appendable);
  }

  @Override
  public Unit<? extends Quantity> parseProductUnit(CharSequence csq, ParsePosition pos) throws MeasurementParseException {
    return delegate.parseProductUnit(csq, pos);
  }

  @Override
  public Unit<? extends Quantity> parseSingleUnit(CharSequence csq, ParsePosition pos) throws MeasurementParseException {
    return delegate.parseSingleUnit(csq, pos);
  }

  @Override
  public void label(Unit<?> unit, String label) {
    delegate.label(unit, label);
  }

  @Override
  public Unit<?> parse(CharSequence csq, ParsePosition cursor) throws IllegalArgumentException {
    return delegate.parse(csq, cursor);
  }

  @Override
  public Unit<?> parse(CharSequence csq) throws MeasurementParseException {
    return delegate.parse(csq);
  }

  @Override
  protected Unit<?> parse(CharSequence csq, int index) throws IllegalArgumentException {
    return delegate.parse(csq, new ParsePosition(index));

  }

  @Override
  public void alias(Unit<?> unit, String alias) {
    delegate.alias(unit, alias);
  }

  @Override
  protected boolean isValidIdentifier(String name) {
    return false; // delegate.isValidIdentifier(name);
  }

  public Unit<?> unitFor(String name) {
    return delegate.unitFor(name);
  }

  @Override
  public Map<String, Unit<?>> getUnitMap() {
    return delegate.getUnitMap();
  }

  public  String nameFor(Unit<?> unit) {
    return delegate.nameFor(unit);
  }

      /*
    try {
      arc_degree = du("arc degree", "deg", new ScaledUnit(Math.PI / 180, radian));
      arc_minute = du("arc minute", "'", new ScaledUnit(1. / 60., arc_degree));
      arc_second = du("arc second", "\"", new ScaledUnit(1. / 60., arc_minute));

      // exact. However, from 1901 to 1964, 1 liter = 1.000028 dm3
      metric_ton = du("metric ton", "t", new ScaledUnit(1e3, kilogram));

      nautical_mile = du("nautical mile", "nmi", new ScaledUnit(1852, meter));
      knot = du("knot", "kt", nautical_mile.divideBy(hour));
      angstrom = du("angstrom", null, new ScaledUnit(1e-10, meter));
      are = du("are", "are", new ScaledUnit(10, meter).raiseTo(2));
      hectare = du("hectare", "ha", new ScaledUnit(100, are));
      barn = du("barn", "b", new ScaledUnit(1e-28, meter.raiseTo(2)));
      bar = du("bar", "bar", new ScaledUnit(1e5, pascal));
      gal = du("gal", "Gal", new ScaledUnit(1e-2, meter).divideBy(second.raiseTo(2)));

      curie = du("curie", "Ci", new ScaledUnit(3.7e10, becquerel));
      roentgen = du("roentgen", "R", new ScaledUnit(2.58e-4, coulomb.divideBy(kilogram)));
      rad = du("rad", "rd", new ScaledUnit(1e-2, gray));
      rem = du("rem", "rem", new ScaledUnit(1e-2, sievert));

    } catch (UnitException e) {
      String reason = e.getMessage();
      System.err.printf("Couldn't initialize class SI %s", reason == null ? "" : (": " + reason));
    }

    db.addAlias("litre", "liter", "l");
    db.addAlias("tonne", "metric ton");
    db.addSymbol("tne", "tonne");
*/
}
