package dev.cdm.dataset.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tech.units.indriya.function.AbstractConverter;
import tech.units.indriya.function.AddConverter;
import tech.units.indriya.function.MultiplyConverter;
import tech.units.indriya.function.RationalNumber;
import tech.units.indriya.unit.TransformedUnit;
import tech.units.indriya.unit.Units;

import javax.measure.Unit;
import javax.measure.quantity.Temperature;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static javax.measure.MetricPrefix.MILLI;

public class TestSimpleUnit {

  public static Stream<Arguments> params() {
    return Stream.of(
            Arguments.of("percent", "%", true),
            Arguments.of("Pa", "mbar", true),
            Arguments.of("km", "km", true),
            Arguments.of("km", "m", true),
            Arguments.of("km", "s", false),
            Arguments.of("pa", "millibar", true)
    );
  }

  @ParameterizedTest
  @MethodSource("params")
  public void testBasic(String unit1, String unit2, boolean expected) {
    assertThat( SimpleUnit.isCompatible(unit1, unit2)).isEqualTo(expected);
  }

  @Test
  public void testProblem() {
    assertThat( SimpleUnit.factoryWithExceptions("mbar")).isNotNull();
    assertThat( SimpleUnit.isCompatible("Pa", "mbar")).isEqualTo(true);
    assertThat( SimpleUnit.isCompatible("pa", "mbar")).isEqualTo(true);
  }

  @Test
  public void testNames() {
    System.out.printf("%s %s%n", Units.PASCAL, Units.PASCAL.getName());

    Unit<?> bar = Units.PASCAL.multiply(100000);
    System.out.printf("%s %s%n", bar, bar.getName());

    Unit<?> millibar = MILLI(Units.PASCAL);
    System.out.printf("%s %s%n", millibar, millibar.getName());
  }

  @Test
  public void testGeopotential() {
    Unit<?> geopotentialHeight = Units.METRE_PER_SECOND.multiply(Units.METRE_PER_SECOND).multiply(9.80665);

    SimpleUnit sunit = SimpleUnit.factoryWithExceptions("gp m");
    System.out.printf("%s %s%n", sunit.unit(), sunit.unit().getName());
  }

  @Test
  public void testParseExpr() {
    SimpleUnit sunit = SimpleUnit.factoryWithExceptions("mbar");
    System.out.printf("%s %s%n", sunit.unit(), sunit.unit().getName());

    Unit<?> millibar = MILLI(sunit.unit());
    System.out.printf("%s %s%n", millibar, millibar.getName());

    SimpleUnit sunit2 = SimpleUnit.factoryWithExceptions("mPa");
    System.out.printf("%s %s%n", sunit2.unit(), sunit2.unit().getName());

    SimpleUnit sunit3 = SimpleUnit.factoryWithExceptions("mbar");
    System.out.printf("%s %s%n", sunit3.unit(), sunit3.unit().getName());
  }

  public void testF() {

    /**
     * @implSpec K = 5/9 * (F - 32) + 273.15
     * @implNote transformation composition {@code (f∘g∘h)(x)} is equivalent to {@code f(g(h(x)))},
     * so inner most transformation comes last in the sequence
     */
    AbstractConverter fahrenheitToKelvin = (AbstractConverter)
            new AddConverter(RationalNumber.of(27315, 100))
                    .concatenate(MultiplyConverter.ofRational(5, 9))
                    .concatenate(new AddConverter(-32));

    Unit<Temperature> DegreesFahrenheit =
            new TransformedUnit<>("°F", "DegreesFahrenheit", Units.KELVIN, fahrenheitToKelvin);

  }

}
