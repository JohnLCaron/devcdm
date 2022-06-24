package dev.cdm.dataset.unit;

import org.junit.jupiter.api.Test;
import tech.units.indriya.format.SimpleUnitFormat;
import tech.units.indriya.unit.Units;

import javax.measure.Unit;
import javax.measure.format.UnitFormat;
import javax.measure.spi.FormatService;
import javax.measure.spi.ServiceProvider;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class TestUnits {
  private static SimpleUnitFormat unitFormat = (SimpleUnitFormat) ServiceProvider.current().getFormatService().getUnitFormat("SIMPLE_ASCII");

  @Test
  public void showFormats() {
    FormatService formatService = ServiceProvider.current().getFormatService();

    for (FormatService.FormatType type : FormatService.FormatType.values()) {
      System.out.printf("type %s == %s%n", type, formatService.getAvailableFormatNames(type));
    }

    UnitFormat ascii = formatService.getUnitFormat("SIMPLE_ASCII");
    System.out.printf("UnitFormat %s%n", ascii.getClass().getName());
  }

  @Test
  public void showUnits() {
    SimpleUnitFormat unitFormat = (SimpleUnitFormat) ServiceProvider.current().getFormatService().getUnitFormat("SIMPLE_ASCII");
    System.out.printf("Units %s%n", unitFormat.getClass().getName());
    List<Map.Entry<String, Unit<?>>> sortedUnits = unitFormat.getUnitMap().entrySet().stream()
            .sorted(Comparator.comparing(u -> u.toString().toLowerCase()))
            .toList();
    sortedUnits.forEach(u -> System.out.printf("%s == %s == %s%n", u.getKey(), u.getValue().getName(),
            u.getValue().getSymbol()));
  }
}
