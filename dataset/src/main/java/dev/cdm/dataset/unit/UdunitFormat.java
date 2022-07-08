/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.dataset.unit;

import dev.cdm.dataset.api.SimpleUnit;
import org.jetbrains.annotations.Nullable;
import tech.units.indriya.format.SimpleUnitFormat;
import tech.units.indriya.unit.Units;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.format.MeasurementParseException;
import javax.measure.quantity.Angle;

import javax.measure.quantity.Pressure;
import javax.measure.spi.ServiceProvider;
import java.io.IOException;
import java.text.ParsePosition;
import java.util.Map;

import static java.lang.Math.PI;

public class UdunitFormat extends SimpleUnitFormat {

  private static SimpleUnitFormat delegate = (SimpleUnitFormat) ServiceProvider.current().getFormatService().getUnitFormat("SIMPLE_ASCII");

  static {
    delegate.alias(Units.PERCENT, "percent");
    delegate.alias(Units.CELSIUS, "celsius");
    delegate.alias(Units.CELSIUS, "deg_C");
    delegate.alias(Units.CELSIUS, "degC");
    delegate.alias(Units.METRE, "meters");
    delegate.alias(Units.METRE, "meter");
    delegate.alias(Units.MOLE, "moles");

    Unit<Pressure> mbar = Units.PASCAL.multiply(100);
    delegate.alias(mbar, "mbar");
    delegate.alias(mbar, "millibar");
    delegate.alias(mbar, "hectopascals");
    delegate.alias(Units.PASCAL, "pa");

    delegate.alias(Units.HOUR, "hours");
    delegate.alias(Units.MINUTE, "minutes");
    delegate.alias(Units.SECOND, "seconds");
    delegate.alias(Units.SECOND, "sec");

    Unit<Angle> degrees = Units.RADIAN.multiply(100/PI);
    delegate.alias(degrees, "degrees_east");
    delegate.alias(degrees, "degE");
    delegate.alias(degrees, "degrees_north");
    delegate.alias(degrees, "degN");
    delegate.alias(degrees, "deg");
    delegate.alias(degrees, "degrees");
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
  @Nullable
  public Unit<?> parse(CharSequence csq) {
    if (csq.equals("gp m")) { // kludge !
      return SimpleUnit.geopotentialHeight.unit();
    }
    try {
      return delegate.parse(csq);
    } catch (Exception e) {
      // System.out.printf("** UdunitFormat cant parse '%s'%n", csq);
      return null;
    }
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

}
