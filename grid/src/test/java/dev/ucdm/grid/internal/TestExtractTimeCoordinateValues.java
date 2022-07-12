package dev.ucdm.grid.internal;

import org.junit.Test;
import dev.ucdm.array.Array;
import dev.ucdm.array.ArrayType;
import dev.ucdm.array.Arrays;
import dev.ucdm.core.api.Attribute;
import dev.ucdm.core.api.Dimension;
import dev.ucdm.core.api.Group;
import dev.ucdm.core.calendar.CalendarDate;
import dev.ucdm.core.calendar.CalendarDateUnit;
import dev.ucdm.core.constants.AxisType;
import dev.ucdm.dataset.api.CoordinateAxis;
import dev.ucdm.dataset.api.CdmDataset;
import dev.ucdm.dataset.api.VariableDS;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

/** Test {@link ExtractTimeCoordinateValues} */
public class TestExtractTimeCoordinateValues {

  @Test
  public void testNumericValue() {
    String units = "days since 2001-01-01 00:00";
    Group.Builder parent = Group.builder().addDimension(Dimension.builder("dim1", 7).setIsUnlimited(true).build())
        .addDimension(new Dimension("dim2", 27));

    VariableDS.Builder<?> vdsBuilder = VariableDS.builder().setName("name").setArrayType(ArrayType.FLOAT)
        .setUnits(units).setDesc("desc").setEnhanceMode(CdmDataset.getEnhanceAll()).setAutoGen(1, 2)
        .addAttribute(new Attribute("missing_value", 0.0f)).setParentGroupBuilder(parent).setDimensionsByName("dim1");
    parent.addVariable(vdsBuilder);

    CoordinateAxis.Builder<?> builder = CoordinateAxis.fromVariableDS(vdsBuilder).setAxisType(AxisType.Time);
    CoordinateAxis axis = builder.build(parent.build());
    ExtractTimeCoordinateValues extract = new ExtractTimeCoordinateValues(axis);

    assertThat(extract.cdates).isNull();
  }

  @Test
  public void testStringValue() {
    int n = 11;
    String units = "days since 2020-02-01 00:00";
    Group.Builder parent = Group.builder().addDimension(Dimension.builder("dim1", n).setIsUnlimited(true).build())
        .addDimension(new Dimension("dim2", 27));

    int count = 1;
    String[] strings = new String[n];
    for (int i = 0; i < n; i++) {
      strings[i] = CalendarDate.of(2020, 2, count++, 0, 0, 0).toString();
    }

    Array<String> values = Arrays.factory(ArrayType.STRING, new int[] {n}, strings);

    VariableDS.Builder<?> vdsBuilder =
        VariableDS.builder().setName("name").setArrayType(ArrayType.STRING).setUnits(units).setDesc("desc")
            .setEnhanceMode(CdmDataset.getEnhanceAll()).addAttribute(new Attribute("missing_value", 0.0f))
            .setParentGroupBuilder(parent).setDimensionsByName("dim1").setSourceData(values);
    parent.addVariable(vdsBuilder);

    CoordinateAxis.Builder<?> builder = CoordinateAxis.fromVariableDS(vdsBuilder).setAxisType(AxisType.Time);
    CoordinateAxis axis = builder.build(parent.build());
    ExtractTimeCoordinateValues extract = new ExtractTimeCoordinateValues(axis);

    assertThat(extract.cdates).hasSize(n);
    System.out.printf("extract.cdates = %s%n", extract.cdates);

    CalendarDateUnit dateUnit = CalendarDateUnit.fromAttributes(axis.attributes(), null).orElseThrow();
    count = 0;
    for (CalendarDate cd : extract.cdates) {
      assertThat(cd).isEqualTo(dateUnit.makeCalendarDate(count++));
    }
  }

  @Test
  public void testStringValueBad() {
    int n = 11;
    String units = "days since 2020-02-01 00:00";
    Group.Builder parent = Group.builder().addDimension(Dimension.builder("dim1", n).setIsUnlimited(true).build())
        .addDimension(new Dimension("dim2", 27));

    int count = 1;
    String[] strings = new String[n];
    for (int i = 0; i < n; i++) {
      strings[i] = CalendarDate.of(2020, 2, count++, 0, 0, 0).toString();
    }
    strings[10] = "badenoff";

    Array<String> values = Arrays.factory(ArrayType.STRING, new int[] {n}, strings);

    VariableDS.Builder<?> vdsBuilder =
        VariableDS.builder().setName("name").setArrayType(ArrayType.STRING).setUnits(units).setDesc("desc")
            .setEnhanceMode(CdmDataset.getEnhanceAll()).addAttribute(new Attribute("missing_value", 0.0f))
            .setParentGroupBuilder(parent).setDimensionsByName("dim1").setSourceData(values);
    parent.addVariable(vdsBuilder);

    CoordinateAxis.Builder<?> builder = CoordinateAxis.fromVariableDS(vdsBuilder).setAxisType(AxisType.Time);
    CoordinateAxis axis = builder.build(parent.build());

    assertThrows(IllegalArgumentException.class, () -> new ExtractTimeCoordinateValues(axis)).getMessage()
        .contains("badenoff");
  }

  @Test
  public void testCharValue() {
    int ndates = 11;
    int nchars = 20;
    String units = "days since 2020-02-01 00:00";
    Group.Builder parent = Group.builder().addDimension(Dimension.builder("dim1", ndates).setIsUnlimited(true).build())
        .addDimension(new Dimension("dim2", 27));

    int count = 1;
    byte[] chars = new byte[ndates * nchars];
    for (int i = 0; i < ndates; i++) {
      String date = CalendarDate.of(2020, 2, count++, 0, 0, 0).toString();
      int pos = i * nchars;
      for (int c = 0; c < date.length(); c++) {
        chars[pos++] = (byte) date.charAt(c);
      }
    }

    Array<String> values = Arrays.factory(ArrayType.CHAR, new int[] {ndates, nchars}, chars);

    VariableDS.Builder<?> vdsBuilder =
        VariableDS.builder().setName("name").setArrayType(ArrayType.CHAR).setUnits(units).setDesc("desc")
            .setEnhanceMode(CdmDataset.getEnhanceAll()).addAttribute(new Attribute("missing_value", 0.0f))
            .setParentGroupBuilder(parent).setDimensionsByName("dim1 " + nchars).setSourceData(values);
    parent.addVariable(vdsBuilder);

    CoordinateAxis.Builder<?> builder = CoordinateAxis.fromVariableDS(vdsBuilder).setAxisType(AxisType.Time);
    CoordinateAxis axis = builder.build(parent.build());
    ExtractTimeCoordinateValues extract = new ExtractTimeCoordinateValues(axis);

    assertThat(extract.cdates).hasSize(ndates);
    System.out.printf("extract.cdates = %s%n", extract.cdates);

    CalendarDateUnit dateUnit = CalendarDateUnit.fromAttributes(axis.attributes(), null).orElseThrow();
    count = 0;
    for (CalendarDate cd : extract.cdates) {
      assertThat(cd).isEqualTo(dateUnit.makeCalendarDate(count++));
    }
  }


}
