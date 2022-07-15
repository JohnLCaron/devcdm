/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.core.api;

import org.junit.jupiter.api.Test;
import dev.ucdm.array.ArrayType;
import dev.ucdm.array.Indent;

import java.util.Formatter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link dev.ucdm.core.api.CdmFile} */
public class TestCdmFile {

  @Test
  public void testBuilder() {
    Attribute att = new Attribute("attName", "value");
    Group.Builder nested =
        Group.builder().setName("child").addAttribute(att).addDimension(new Dimension("dimNameChild", 42))
            .addVariable(Variable.builder().setName("varNameChild").setArrayType(ArrayType.STRING));
    Variable.Builder<?> vb = Variable.builder().setName("varName").setArrayType(ArrayType.STRING);
    Dimension dim = new Dimension("dimName", 42);
    Group.Builder root =
        Group.builder().setName("").addAttribute(att).addDimension(dim).addGroup(nested).addVariable(vb);

    CdmFile.Builder<?> builder =
        CdmFile.builder().setId("Hid").setLocation("location").setRootGroup(root).setTitle("title");

    CdmFile ncfile = builder.build();
    assertThat(ncfile.getId()).isEqualTo("Hid");
    assertThat(ncfile.getLocation()).isEqualTo("location");
    assertThat(ncfile.getTitle()).isEqualTo("title");
    assertThat(ncfile.getVariables()).hasSize(2);
    assertThat(ncfile.getDimensions()).hasSize(2);

    Group group = ncfile.getRootGroup();
    assertThat(group.getCdmFile()).isEqualTo(ncfile);
    assertThat(group.getShortName()).isEqualTo("");
    assertThat(group.isRoot()).isTrue();
    assertThat(group.attributes()).isNotEmpty();
    assertThat(group.attributes()).hasSize(1);
    assertThat(group.findAttribute("attName")).isEqualTo(att);
    assertThat(group.findAttributeString("attName", null)).isEqualTo("value");

    assertThat(group.getDimensions()).isNotEmpty();
    assertThat(group.getDimensions()).hasSize(1);
    assertThat(group.findDimension("dimName").isPresent()).isTrue();
    assertThat(group.findDimension("dimName").get()).isEqualTo(dim);

    assertThat(group.getGroups()).isNotEmpty();
    assertThat(group.getGroups()).hasSize(1);
    Group child = group.findGroupLocal("child");
    assertThat(child.getParentGroup()).isEqualTo(group);

    assertThat(group.getVariables()).isNotEmpty();
    assertThat(group.getVariables()).hasSize(1);
    Variable v = group.findVariableLocal("varName");
    assertThat(v.getParentGroup()).isEqualTo(group);
    assertThat(v.getCdmFile()).isEqualTo(ncfile);
  }

  @Test
  public void testCDL() {
    Attribute att = new Attribute("attName", "value");
    Dimension dim = new Dimension("dimName", 42);
    Group.Builder nested = Group.builder().setName("child");
    Variable.Builder<?> vb = Variable.builder().setName("varName").setArrayType(ArrayType.STRING);
    Group.Builder groupb =
        Group.builder().setName("").addAttribute(att).addDimension(dim).addGroup(nested).addVariable(vb);

    CdmFile.Builder<?> builder =
        CdmFile.builder().setId("Hid").setLocation("location.nc").setRootGroup(groupb).setTitle("title");

    CdmFile ncfile = builder.build();
    assertThat(ncfile.toString()).isEqualTo(String.format("netcdf location.nc {%n" + "  dimensions:%n"
        + "    dimName = 42;%n" + "  variables:%n" + "    string varName;%n" + "%n" + "  group: child {%n" + "  }%n"
        + "%n" + "  // global attributes:%n" + "  :attName = \"value\";%n" + "}%n"));

    Formatter f = new Formatter();
    ncfile.writeCDL(f, new Indent(2), true);
    assertThat(f.toString()).isEqualTo(String.format("netcdf location {%n" + "  dimensions:%n" + "    dimName = 42;%n"
        + "  variables:%n" + "    string varName;%n" + "%n" + "  group: child {%n" + "  }%n" + "%n"
        + "  // global attributes:%n" + "  string :attName = \"value\";%n" + "}%n"));

    assertThat(ncfile.getDetailInfo()).isNotNull();
  }

  @Test
  public void testFindAttribute() {
    Variable.Builder<?> vb = Variable.builder().setName("varName").setArrayType(ArrayType.STRING)
        .addAttribute(new Attribute("attName", "nestedGroupVariable"));
    Group.Builder nested =
        Group.builder().setName("child").addAttribute(new Attribute("attName", "nestedGroup")).addVariable(vb);

    Group.Builder rootb =
        Group.builder().setName("").addAttribute(new Attribute("attName", "rootGroup")).addGroup(nested);

    CdmFile.Builder<?> builder =
        CdmFile.builder().setId("Hid").setLocation("location").setRootGroup(rootb).setTitle("title");

    CdmFile ncfile = builder.build();

    Attribute att = ncfile.findAttribute("@attName");
    assertThat(att).isNotNull();
    assertThat(att.getStringValue()).isEqualTo("rootGroup");

    att = ncfile.findAttribute("/@attName");
    assertThat(att).isNotNull();
    assertThat(att.getStringValue()).isEqualTo("rootGroup");

    att = ncfile.findAttribute("attName");
    assertThat(att).isNotNull();
    assertThat(att.getStringValue()).isEqualTo("rootGroup");

    att = ncfile.findAttribute("/child/@attName");
    assertThat(att).isNotNull();
    assertThat(att.getStringValue()).isEqualTo("nestedGroup");

    att = ncfile.findAttribute("child/@attName");
    assertThat(att).isNotNull();
    assertThat(att.getStringValue()).isEqualTo("nestedGroup");

    att = ncfile.findAttribute("/child/varName@attName");
    assertThat(att).isNotNull();
    assertThat(att.getStringValue()).isEqualTo("nestedGroupVariable");

    att = ncfile.findAttribute("child/varName@attName");
    assertThat(att).isNotNull();
    assertThat(att.getStringValue()).isEqualTo("nestedGroupVariable");
  }

  @Test
  public void testFindGlobals() {
    Dimension dim = new Dimension("dimName", 42);
    Variable.Builder<?> vb = Variable.builder().setName("varName").setArrayType(ArrayType.STRING)
        .addAttribute(new Attribute("attName", "nestedGroupVariable"));
    Group.Builder nested = Group.builder().setName("child").addAttribute(new Attribute("attNameNested", "nestedGroup"))
        .addVariable(vb).addDimension(dim);

    Group.Builder rootb = Group.builder().setName("").addAttribute(new Attribute("attName", "rootGroup"))
        .addDimension(new Dimension("dimNameRoot", 2)).addGroup(nested);

    CdmFile.Builder<?> builder =
        CdmFile.builder().setId("Hid").setLocation("location").setRootGroup(rootb).setTitle("title");

    CdmFile ncfile = builder.build();

    Group gfound = ncfile.findGroup("/child");
    assertThat(gfound).isNotNull();
    Group g = ncfile.getRootGroup().findGroupLocal("child");
    assertThat(g).isNotNull();
    assertThat(gfound).isEqualTo(g);

    Variable v = ncfile.findVariable("/child/varName");
    assertThat(v).isNotNull();
    assertThat(v).isEqualTo(g.findVariableLocal("varName"));

    Dimension dfound = ncfile.findDimension("/child/dimName");
    assertThat(dfound).isNotNull();
    assertThat(dfound).isEqualTo(dim);

    List<Dimension> allDims = ncfile.getDimensions();
    assertThat(allDims).hasSize(2);

    assertThat(ncfile.hasUnlimitedDimension()).isFalse();
    assertThat(ncfile.getUnlimitedDimension()).isNull();
  }

  @Test
  public void testFindAttributeStructure() {
    Variable.Builder<?> mem1 = Variable.builder().setName("mem1").setArrayType(ArrayType.STRING)
        .addAttribute(new Attribute("attName", "mem1"));
    Variable.Builder<?> mem2 = Variable.builder().setName("mem2").setArrayType(ArrayType.STRING)
        .addAttribute(new Attribute("attName", "mem2"));

    Structure.Builder<?> vb = Structure.builder().setName("varName").setArrayType(ArrayType.STRING)
        .addAttribute(new Attribute("attName", "nestedGroupVariable"))
        .addMemberVariable("memm", ArrayType.STRING, "")
        .addMemberVariable(mem1).addMemberVariable(mem2);

    Group.Builder nested =
        Group.builder().setName("child").addAttribute(new Attribute("attName", "nestedGroup")).addVariable(vb);

    Group.Builder rootb =
        Group.builder().setName("").addAttribute(new Attribute("attName", "rootGroup")).addGroup(nested);

    CdmFile.Builder<?> builder =
        CdmFile.builder().setId("Hid").setLocation("location").setRootGroup(rootb).setTitle("title");

    CdmFile ncfile = builder.build();

    Attribute att = ncfile.findAttribute("/child/varName.mem1@attName");
    assertThat(att).isNotNull();
    assertThat(att.getStringValue()).isEqualTo("mem1");

    att = ncfile.findAttribute("child/varName.mem2@attName");
    assertThat(att).isNotNull();
    assertThat(att.getStringValue()).isEqualTo("mem2");

    att = ncfile.findAttribute("child/varName.memm@attName");
    assertThat(att).isNull();

    att = ncfile.findAttribute("child/varName.mem1@attName1");
    assertThat(att).isNull();
  }
}
