package dev.ucdm.dataset.internal;

import dev.ucdm.array.InvalidRangeException;
import dev.ucdm.core.constants.AxisType;
import dev.ucdm.dataset.api.CdmDatasetCS;
import dev.ucdm.dataset.api.CdmDatasets;
import dev.ucdm.dataset.api.CoordinateSystem;
import dev.ucdm.dataset.api.SimpleUnit;
import dev.ucdm.dataset.api.TestCdmDatasets;
import dev.ucdm.dataset.api.VariableDS;
import dev.ucdm.dataset.transform.vertical.AtmosHybridSigmaPressure;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

public class TestVertical {

  @Test
  public void testHybridSigmaPressure() throws Exception {
    String filename = TestCdmDatasets.gridTestDir + "transforms/HybridSigmaPressure.nc";
    open(filename, "T", AtmosHybridSigmaPressure.class, SimpleUnit.pressureUnit);
  }

  static boolean open(String filename, String gridName, Class<?> vtClass, SimpleUnit vunit)
          throws IOException, InvalidRangeException {
    System.out.printf("compare %s %s%n", filename, gridName);

    Formatter errlog = new Formatter();
    try (CdmDatasetCS ds = CdmDatasets.openDatasetWithCS(filename, true)) {
      assertThat(ds).isNotNull();

      VariableDS grid = (VariableDS) ds.findVariable(gridName);
      List<CoordinateSystem> csyss =  ds.makeCoordinateSystemsFor(grid);
      assertThat(csyss).hasSize(1);
      CoordinateSystem csys = csyss.get(0);

      assertThat(csys.findAxis(AxisType.GeoZ)).isNotNull();
    }
    return true;
  }
}
