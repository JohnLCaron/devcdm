package dev.ucdm.grib.grib2.table;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import dev.ucdm.grib.common.GribTables;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.grib.grib2.table.EccodesCodeTable.LATEST_VERSION;

public class TestLocalTables {

  @Test
  public void testLocalTables() throws IOException {
    List<Grib2Tables> tables = Grib2Tables.getAllRegisteredTables();
    for (Grib2Tables t : tables) {
      for (GribTables.Parameter p : t.getParameters()) {
        assertThat(p.getName()).isNotEmpty();
        assertThat(p.getId()).isNotEmpty();
      }
    }
  }

  @Test
  public void testKmaTable() {
    Grib2Tables kma = Grib2Tables.factory(40, -1, -1, -1, -1);
    assertThat(kma).isNotNull();
    assertThat(kma.getType()).isEqualTo(Grib2TablesId.Type.kma);
    assertThat(kma.getParameters()).isNotEmpty();
    for (GribTables.Parameter p : kma.getParameters())
      System.out.printf("%s%n", p);
  }

  @Test
  public void testEcmwfCodeTables() throws IOException {
    ImmutableSet<String> tableOverrides = ImmutableSet.of("4.230", "4.233", "4.192", "5.40000", "5.50002");

    Grib2Tables ecmwfTable = Grib2Tables.factory(98, -1, -1, -1, -1);
    assertThat(ecmwfTable).isNotNull();
    assertThat(ecmwfTable.getType()).isEqualTo(Grib2TablesId.Type.eccodes);

    for (String tableName : tableOverrides) {
      Iterator<String> tokens = Splitter.on('.').trimResults().omitEmptyStrings().split(tableName).iterator();

      int discipline = Integer.parseInt(tokens.next());
      int category = Integer.parseInt(tokens.next());

      EccodesCodeTable ecmwfCodeTable = EccodesCodeTable.factory(LATEST_VERSION, discipline, category);
      assertThat(ecmwfCodeTable).isNotNull();
      for (Grib2CodeTableInterface.Entry entry : ecmwfCodeTable.getEntries()) {
        assertThat(ecmwfTable.getCodeTableValue(tableName, entry.getCode())).isEqualTo(entry.getName());
      }
    }
  }
}
