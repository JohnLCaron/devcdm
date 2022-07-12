/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.gcdm;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import dev.ucdm.array.ArrayType;
import dev.ucdm.array.ArrayVlen;
import dev.ucdm.array.Arrays;
import dev.ucdm.array.StructureData;
import dev.ucdm.array.StructureDataArray;
import dev.ucdm.array.StructureDataStorageBB;
import dev.ucdm.array.StructureMembers;
import dev.ucdm.array.StructureMembers.Member;
import dev.ucdm.array.StructureMembers.MemberBuilder;
import dev.ucdm.gcdm.protogen.GcdmProto;
import dev.ucdm.gcdm.protogen.GcdmProto.StructureDataProto;
import dev.ucdm.gcdm.protogen.GcdmProto.StructureMemberProto;
import dev.ucdm.array.Array;
import dev.ucdm.array.InvalidRangeException;
import dev.ucdm.array.Range;
import dev.ucdm.array.Section;
import dev.ucdm.core.api.*;

/** Convert between Gcdm Protos and Netcdf objects, using dev.cdm.array.Array for data. */
public class GcdmConverter {
  private static final boolean debugSize = false;

  public static GcdmProto.ArrayType convertArrayType(ArrayType dtype) {
    switch (dtype) {
      case CHAR:
        return GcdmProto.ArrayType.ARRAY_TYPE_CHAR;
      case BYTE:
        return GcdmProto.ArrayType.ARRAY_TYPE_BYTE;
      case SHORT:
        return GcdmProto.ArrayType.ARRAY_TYPE_SHORT;
      case INT:
        return GcdmProto.ArrayType.ARRAY_TYPE_INT;
      case LONG:
        return GcdmProto.ArrayType.ARRAY_TYPE_LONG;
      case FLOAT:
        return GcdmProto.ArrayType.ARRAY_TYPE_FLOAT;
      case DOUBLE:
        return GcdmProto.ArrayType.ARRAY_TYPE_DOUBLE;
      case STRING:
        return GcdmProto.ArrayType.ARRAY_TYPE_STRING;
      case STRUCTURE:
        return GcdmProto.ArrayType.ARRAY_TYPE_STRUCTURE;
      case SEQUENCE:
        return GcdmProto.ArrayType.ARRAY_TYPE_SEQUENCE;
      case ENUM1:
        return GcdmProto.ArrayType.ARRAY_TYPE_ENUM1;
      case ENUM2:
        return GcdmProto.ArrayType.ARRAY_TYPE_ENUM2;
      case ENUM4:
        return GcdmProto.ArrayType.ARRAY_TYPE_ENUM4;
      case OPAQUE:
        return GcdmProto.ArrayType.ARRAY_TYPE_OPAQUE;
      case UBYTE:
        return GcdmProto.ArrayType.ARRAY_TYPE_UBYTE;
      case USHORT:
        return GcdmProto.ArrayType.ARRAY_TYPE_USHORT;
      case UINT:
        return GcdmProto.ArrayType.ARRAY_TYPE_UINT;
      case ULONG:
        return GcdmProto.ArrayType.ARRAY_TYPE_ULONG;
    }
    throw new IllegalStateException("illegal data type " + dtype);
  }

  public static ArrayType convertArrayType(GcdmProto.ArrayType dtype) {
    switch (dtype) {
      case ARRAY_TYPE_CHAR:
        return ArrayType.CHAR;
      case ARRAY_TYPE_BYTE:
        return ArrayType.BYTE;
      case ARRAY_TYPE_SHORT:
        return ArrayType.SHORT;
      case ARRAY_TYPE_INT:
        return ArrayType.INT;
      case ARRAY_TYPE_LONG:
        return ArrayType.LONG;
      case ARRAY_TYPE_FLOAT:
        return ArrayType.FLOAT;
      case ARRAY_TYPE_DOUBLE:
        return ArrayType.DOUBLE;
      case ARRAY_TYPE_STRING:
        return ArrayType.STRING;
      case ARRAY_TYPE_STRUCTURE:
        return ArrayType.STRUCTURE;
      case ARRAY_TYPE_SEQUENCE:
        return ArrayType.SEQUENCE;
      case ARRAY_TYPE_ENUM1:
        return ArrayType.ENUM1;
      case ARRAY_TYPE_ENUM2:
        return ArrayType.ENUM2;
      case ARRAY_TYPE_ENUM4:
        return ArrayType.ENUM4;
      case ARRAY_TYPE_OPAQUE:
        return ArrayType.OPAQUE;
      case ARRAY_TYPE_UBYTE:
        return ArrayType.UBYTE;
      case ARRAY_TYPE_USHORT:
        return ArrayType.USHORT;
      case ARRAY_TYPE_UINT:
        return ArrayType.UINT;
      case ARRAY_TYPE_ULONG:
        return ArrayType.ULONG;
    }
    throw new IllegalStateException("illegal data type " + dtype);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////

  public static List<GcdmProto.Attribute> encodeAttributes(AttributeContainer atts) {
    List<GcdmProto.Attribute> result = new ArrayList<>();
    for (Attribute att : atts) {
      result.add(encodeAtt(att).build());
    }
    return result;
  }

  public static GcdmProto.Group.Builder encodeGroup(Group g, int sizeToCache) throws IOException {
    GcdmProto.Group.Builder groupBuilder = GcdmProto.Group.newBuilder();
    groupBuilder.setName(g.getShortName());

    for (Dimension dim : g.getDimensions())
      groupBuilder.addDims(encodeDim(dim));

    for (Attribute att : g.attributes())
      groupBuilder.addAtts(encodeAtt(att));

    for (EnumTypedef enumType : g.getEnumTypedefs())
      groupBuilder.addEnumTypes(encodeEnumTypedef(enumType));

    for (Variable var : g.getVariables()) {
      if (var instanceof Structure)
        groupBuilder.addStructs(encodeStructure((Structure) var));
      else
        groupBuilder.addVars(encodeVar(var, sizeToCache));
    }

    for (Group ng : g.getGroups())
      groupBuilder.addGroups(encodeGroup(ng, sizeToCache));

    return groupBuilder;
  }

  public static GcdmProto.Error encodeErrorMessage(String message) {
    GcdmProto.Error.Builder builder = GcdmProto.Error.newBuilder();
    builder.setMessage(message);
    return builder.build();
  }

  public static GcdmProto.Section encodeSection(Section section) {
    GcdmProto.Section.Builder sbuilder = GcdmProto.Section.newBuilder();
    for (Range r : section.getRanges()) {
      GcdmProto.Range.Builder rbuilder = GcdmProto.Range.newBuilder();
      rbuilder.setStart(r.first());
      rbuilder.setSize(r.length());
      rbuilder.setStride(r.stride());
      sbuilder.addRanges(rbuilder);
    }
    return sbuilder.build();
  }

  private static GcdmProto.Attribute.Builder encodeAtt(Attribute att) {
    GcdmProto.Attribute.Builder attBuilder = GcdmProto.Attribute.newBuilder();
    attBuilder.setName(att.getShortName());
    attBuilder.setArrayType(convertArrayType(att.getArrayType()));
    attBuilder.setLength(att.getLength());

    // values
    if (att.getLength() > 0) {
      if (att.isString()) {
        GcdmProto.Data.Builder datab = GcdmProto.Data.newBuilder();
        for (int i = 0; i < att.getLength(); i++) {
          datab.addSdata(att.getStringValue(i));
        }
        datab.setArrayType(convertArrayType(att.getArrayType()));
        encodeShape(datab, new int[] {att.getLength()});
        attBuilder.setData(datab);
      } else {
        attBuilder.setData(encodePrimitiveData(att.getArrayType(), att.getArrayValues()));
      }
    }
    return attBuilder;
  }

  private static GcdmProto.Dimension.Builder encodeDim(Dimension dim) {
    GcdmProto.Dimension.Builder dimBuilder = GcdmProto.Dimension.newBuilder();
    if (dim.getShortName() != null)
      dimBuilder.setName(dim.getShortName());
    if (!dim.isVariableLength())
      dimBuilder.setLength(dim.getLength());
    dimBuilder.setIsPrivate(!dim.isShared());
    dimBuilder.setIsVlen(dim.isVariableLength());
    dimBuilder.setIsUnlimited(dim.isUnlimited());
    return dimBuilder;
  }

  private static GcdmProto.EnumTypedef.Builder encodeEnumTypedef(EnumTypedef enumType) {
    GcdmProto.EnumTypedef.Builder builder = GcdmProto.EnumTypedef.newBuilder();

    builder.setName(enumType.getShortName());
    builder.setBaseType(convertArrayType(enumType.getBaseArrayType()));
    Map<Integer, String> map = enumType.getMap();
    GcdmProto.EnumTypedef.EnumType.Builder b2 = GcdmProto.EnumTypedef.EnumType.newBuilder();
    for (int code : map.keySet()) {
      b2.clear();
      b2.setCode(code);
      b2.setValue(map.get(code));
      builder.addMaps(b2);
    }
    return builder;
  }

  private static GcdmProto.Structure.Builder encodeStructure(Structure s) throws IOException {
    GcdmProto.Structure.Builder builder = GcdmProto.Structure.newBuilder();
    builder.setName(s.getShortName());
    builder.setArrayType(convertArrayType(s.getArrayType()));

    for (Dimension dim : s.getDimensions())
      builder.addShapes(encodeDim(dim));

    for (Attribute att : s.attributes())
      builder.addAtts(encodeAtt(att));

    for (Variable v : s.getVariables()) {
      if (v instanceof Structure)
        builder.addStructs(GcdmConverter.encodeStructure((Structure) v));
      else
        builder.addVars(encodeVar(v, -1));
    }

    return builder;
  }


  private static GcdmProto.Variable.Builder encodeVar(Variable var, int sizeToCache) throws IOException {
    GcdmProto.Variable.Builder builder = GcdmProto.Variable.newBuilder();
    builder.setShortName(var.getShortName());
    builder.setArrayType(convertArrayType(var.getArrayType()));
    if (var.getArrayType().isEnum()) {
      EnumTypedef enumType = var.getEnumTypedef();
      if (enumType != null)
        builder.setEnumType(enumType.getShortName());
    }

    for (Dimension dim : var.getDimensions()) {
      builder.addShapes(encodeDim(dim));
    }

    for (Attribute att : var.attributes()) {
      builder.addAtts(encodeAtt(att));
    }

    // put small amounts of data in header "immediate mode"
    if (var.isCaching() && var.getArrayType().isNumeric()) {
      if (var.isCoordinateVariable() || var.getSize() * var.getElementSize() < sizeToCache) {
        Array<?> data = var.readArray();
        builder.setData(encodeData(var.getArrayType(), data));
      }
    }
    return builder;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////

  public static GcdmProto.Data encodeData(ArrayType dataType, Array<?> data) {
    GcdmProto.Data result;
    if (dataType == ArrayType.OPAQUE) {
      result = encodePrimitiveData(dataType, data);
    } else if (data.isVlen()) {
      result = encodeVlenData(dataType, (ArrayVlen) data);
    } else if (data instanceof StructureDataArray) {
      result = encodeStructureDataArray(dataType, (StructureDataArray) data);
    } else {
      result = encodePrimitiveData(dataType, data);
    }
    if (debugSize && data.length() > 10000) {
      long expected = data.length() * data.getArrayType().getSize();
      byte[] b = result.toByteArray();
      float ratio = ((float) b.length) / expected;
      System.out.printf(" GcdmProto.Data nelems = %d type=%s expected size =%d actual = %d ratio = %f%n",
          data.length(), data.getArrayType(), expected, b.length, ratio);
    }
    return result;
  }

  private static void encodeShape(GcdmProto.Data.Builder data, int[] shape) {
    for (int j : shape) {
      data.addShapes(j);
    }
  }

  private static GcdmProto.Data encodePrimitiveData(ArrayType dataType, Array<?> data) {
    GcdmProto.Data.Builder builder = GcdmProto.Data.newBuilder();
    builder.setArrayType(convertArrayType(dataType));
    encodeShape(builder, data.getShape());

    switch (dataType) {
      case OPAQUE: {
        ArrayVlen<Byte> vlen = (ArrayVlen) data;
        int nelems = (int) Arrays.computeSize(data.getShape());
        for (int i = 0; i < nelems; i++) {
          builder.addBdata(encodeByteArray(vlen.get(i)));
        }
        break;
      }

      case ENUM1:
      case CHAR:
      case UBYTE:
      case BYTE: {
        builder.addBdata(encodeByteArray((Array<Byte>) data));
        break;
      }
      case SHORT:
      case ENUM2:
      case USHORT: {
        // USHORT Idata ratio < 1; Uidata ratio ~2
        Array<Short> idata = (Array<Short>) data;
        idata.forEach(val -> builder.addIdata(val));
        break;
      }
      case INT: {
        Array<Integer> idata = (Array<Integer>) data;
        idata.forEach(val -> builder.addIdata(val));
        break;
      }
      case ENUM4:
      case UINT: {
        Array<Integer> idata = (Array<Integer>) data;
        idata.forEach(val -> builder.addUidata(val));
        break;
      }
      case LONG: {
        Array<Long> ldata = (Array<Long>) data;
        ldata.forEach(val -> builder.addLdata(val));
        break;
      }
      case ULONG: {
        Array<Long> ldata = (Array<Long>) data;
        ldata.forEach(val -> builder.addUldata(val));
        break;
      }
      case FLOAT: {
        Array<Float> fdata = (Array<Float>) data;
        fdata.forEach(val -> builder.addFdata(val));
        break;
      }
      case DOUBLE: {
        Array<Double> ddata = (Array<Double>) data;
        ddata.forEach(val -> builder.addDdata(val));
        break;
      }
      case STRING: {
        Array<String> sdata = (Array<String>) data;
        sdata.forEach(val -> builder.addSdata(val));
        break;
      }
      default:
        throw new IllegalStateException("Unkown datatype " + dataType);
    }
    return builder.build();
  }

  /** Convert the Array into a ByteString. */
  private static ByteString encodeByteArray(Array<Byte> data) {
    byte[] raw = new byte[(int) data.length()];
    int idx = 0;
    for (byte bval : data) {
      raw[idx++] = bval;
    }
    return ByteString.copyFrom(raw);
  }

  private static GcdmProto.Data encodeStructureDataArray(ArrayType dataType, StructureDataArray arrayStructure) {
    GcdmProto.Data.Builder builder = GcdmProto.Data.newBuilder();
    builder.setArrayType(convertArrayType(dataType));
    encodeShape(builder, arrayStructure.getShape());
    builder.setMembers(encodeStructureMembers(arrayStructure.getStructureMembers()));

    // row oriented
    int count = 0; // useful in the debugger
    for (StructureData sdata : arrayStructure) {
      builder.addRows(encodeStructureData(sdata));
      count++;
    }
    return builder.build();
  }

  private static GcdmProto.StructureDataProto encodeStructureData(StructureData structData) {
    GcdmProto.StructureDataProto.Builder builder = GcdmProto.StructureDataProto.newBuilder();
    int count = 0; // useful in the debugger
    for (Member member : structData.getStructureMembers()) {
      Array<?> data = structData.getMemberData(member);
      builder.addMemberData(encodeData(member.getArrayType(), data));
      count++;
    }
    return builder.build();
  }

  private static GcdmProto.StructureMembersProto encodeStructureMembers(StructureMembers members) {
    GcdmProto.StructureMembersProto.Builder builder = GcdmProto.StructureMembersProto.newBuilder();
    builder.setName(members.getName());
    for (Member member : members.getMembers()) {
      StructureMemberProto.Builder smBuilder = StructureMemberProto.newBuilder().setName(member.getName())
          .setArrayType(convertArrayType(member.getArrayType())).addAllShapes(Ints.asList(member.getShape()));
      if (member.getStructureMembers() != null) {
        smBuilder.setMembers(encodeStructureMembers(member.getStructureMembers()));
      }
      builder.addMembers(smBuilder);
    }
    return builder.build();
  }

  private static GcdmProto.Data encodeVlenData(ArrayType dataType, ArrayVlen<?> vlenarray) {
    GcdmProto.Data.Builder builder = GcdmProto.Data.newBuilder();
    builder.setArrayType(convertArrayType(dataType));
    encodeShape(builder, vlenarray.getShape());
    for (Array<?> one : vlenarray) {
      builder.addVlen(encodeData(vlenarray.getArrayType(), one));
    }
    return builder.build();
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public static AttributeContainer decodeAttributes(String name, List<GcdmProto.Attribute> atts) {
    AttributeContainerMutable builder = new AttributeContainerMutable(name);
    for (GcdmProto.Attribute att : atts) {
      builder.addAttribute(decodeAtt(att));
    }
    return builder.toImmutable();
  }

  public static void decodeGroup(GcdmProto.Group proto, Group.Builder g) {
    for (GcdmProto.Dimension dim : proto.getDimsList())
      g.addDimension(GcdmConverter.decodeDim(dim)); // always added to group? what if private ??

    for (GcdmProto.Attribute att : proto.getAttsList())
      g.addAttribute(GcdmConverter.decodeAtt(att));

    for (GcdmProto.EnumTypedef enumType : proto.getEnumTypesList())
      g.addEnumTypedef(GcdmConverter.decodeEnumTypedef(enumType));

    for (GcdmProto.Variable var : proto.getVarsList())
      g.addVariable(GcdmConverter.decodeVar(var));

    for (GcdmProto.Structure s : proto.getStructsList())
      g.addVariable(GcdmConverter.decodeStructure(s));

    for (GcdmProto.Group gp : proto.getGroupsList()) {
      Group.Builder ng = Group.builder().setName(gp.getName());
      g.addGroup(ng);
      decodeGroup(gp, ng);
    }
  }

  private static Attribute decodeAtt(GcdmProto.Attribute attp) {
    ArrayType dtUse = convertArrayType(attp.getArrayType());
    int len = attp.getLength();
    if (len == 0) { // deal with empty attribute
      return Attribute.builder(attp.getName()).setArrayType(dtUse).build();
    }

    Array<?> attData = decodePrimitiveData(attp.getData());
    return Attribute.fromArray(attp.getName(), attData);
  }

  private static Dimension decodeDim(GcdmProto.Dimension dim) {
    String name = (dim.getName().isEmpty() ? null : dim.getName());
    int dimLen = dim.getIsVlen() ? -1 : (int) dim.getLength();
    return Dimension.builder().setName(name).setIsShared(!dim.getIsPrivate()).setIsUnlimited(dim.getIsUnlimited())
        .setIsVariableLength(dim.getIsVlen()).setLength(dimLen).build();
  }

  private static EnumTypedef decodeEnumTypedef(GcdmProto.EnumTypedef enumType) {
    List<GcdmProto.EnumTypedef.EnumType> list = enumType.getMapsList();
    Map<Integer, String> map = new HashMap<>(2 * list.size());
    for (GcdmProto.EnumTypedef.EnumType et : list) {
      map.put(et.getCode(), et.getValue());
    }
    ArrayType basetype = convertArrayType(enumType.getBaseType());
    return new EnumTypedef(enumType.getName(), map, basetype);
  }

  private static Structure.Builder<?> decodeStructure(GcdmProto.Structure s) {
    Structure.Builder<?> ncvar =
        (s.getArrayType() == GcdmProto.ArrayType.ARRAY_TYPE_SEQUENCE) ? Sequence.builder() : Structure.builder();

    ncvar.setName(s.getName()).setArrayType(convertArrayType(s.getArrayType()));

    List<Dimension> dims = new ArrayList<>(6);
    for (GcdmProto.Dimension dim : s.getShapesList()) {
      dims.add(decodeDim(dim));
    }
    ncvar.addDimensions(dims);

    for (GcdmProto.Attribute att : s.getAttsList()) {
      ncvar.addAttribute(decodeAtt(att));
    }

    for (GcdmProto.Variable vp : s.getVarsList()) {
      ncvar.addMemberVariable(decodeVar(vp));
    }

    for (GcdmProto.Structure sp : s.getStructsList()) {
      ncvar.addMemberVariable(decodeStructure(sp));
    }

    return ncvar;
  }

  public static Section decodeSection(GcdmProto.Section proto) {
    Section.Builder section = Section.builder();

    for (GcdmProto.Range pr : proto.getRangesList()) {
      try {
        long stride = pr.getStride();
        if (stride == 0) {
          stride = 1;
        }
        if (pr.getSize() == 0) {
          section.appendRange(Range.EMPTY); // used for scalars TODO really used ??
        } else {
          // this.last = first + (this.length-1) * stride;
          section.appendRange((int) pr.getStart(), (int) (pr.getStart() + (pr.getSize() - 1) * stride), (int) stride);
        }

      } catch (InvalidRangeException e) {
        throw new RuntimeException("Bad Section in Gcdm", e);
      }
    }
    return section.build();
  }

  public static dev.ucdm.array.Section decodeSection(GcdmProto.Variable var) {
    dev.ucdm.array.Section.Builder section = dev.ucdm.array.Section.builder();
    for (GcdmProto.Dimension dim : var.getShapesList()) {
      section.appendRange((int) dim.getLength());
    }
    return section.build();
  }

  private static int[] decodeShape(GcdmProto.Data data) {
    int[] shape = new int[data.getShapesCount()];
    for (int i = 0; i < shape.length; i++) {
      shape[i] = data.getShapes(i);
    }
    return shape;
  }


  private static Variable.Builder<?> decodeVar(GcdmProto.Variable var) {
    ArrayType varType = convertArrayType(var.getArrayType());
    Variable.Builder<?> ncvar = Variable.builder().setName(var.getShortName()).setArrayType(varType);

    if (varType.isEnum()) {
      ncvar.setEnumTypeName(var.getEnumType());
    }

    // The Dimensions are stored redunantly in the Variable.
    // If shared, they must also exist in a parent Group. However, we dont yet have the Groups wired together,
    // so that has to wait until build().
    List<Dimension> dims = new ArrayList<>(6);
    Section.Builder section = Section.builder();
    for (GcdmProto.Dimension dim : var.getShapesList()) {
      dims.add(decodeDim(dim));
      section.appendRange((int) dim.getLength());
    }
    ncvar.addDimensions(dims);

    for (GcdmProto.Attribute att : var.getAttsList())
      ncvar.addAttribute(decodeAtt(att));

    if (var.hasData()) {
      Array<?> data = decodePrimitiveData(var.getData());
      ncvar.setSourceData(data);
    }

    return ncvar;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////

  public static <T> Array<T> decodeData(GcdmProto.Data data) {
    if (data.getVlenCount() > 0) {
      return (Array<T>) decodeVlenData(data);
    } else if (data.hasMembers()) {
      return (Array<T>) decodeStructureDataArray(data);
    } else {
      return decodePrimitiveData(data);
    }
  }

  private static <T> Array<T> decodePrimitiveData(GcdmProto.Data data) {
    ArrayType dataType = convertArrayType(data.getArrayType());
    int[] shape = decodeShape(data);
    switch (dataType) {
      case OPAQUE: {
        byte[][] ragged = new byte[data.getBdataCount()][];
        int countOuter = 0;
        for (ByteString nestedBytes : data.getBdataList()) {
          ragged[countOuter++] = nestedBytes.toByteArray();
        }
        return (Array<T>) ArrayVlen.factory(ArrayType.OPAQUE, shape, ragged);
      }

      case CHAR:
      case ENUM1:
      case UBYTE:
      case BYTE: {
        byte[] array = data.getBdata(0).toByteArray();
        return Arrays.factory(dataType, shape, array);
      }
      case SHORT:
      case ENUM2:
      case USHORT: {
        int i = 0;
        short[] array = new short[data.getIdataCount()];
        for (int val : data.getIdataList()) {
          array[i++] = (short) val;
        }
        return Arrays.factory(dataType, shape, array);
      }
      case INT: {
        int i = 0;
        int[] array = new int[data.getIdataCount()];
        for (int val : data.getIdataList()) {
          array[i++] = val;
        }
        return Arrays.factory(dataType, shape, array);
      }
      case ENUM4:
      case UINT: {
        int i = 0;
        int[] array = new int[data.getUidataCount()];
        for (int val : data.getUidataList()) {
          array[i++] = val;
        }
        return Arrays.factory(dataType, shape, array);
      }
      case LONG: {
        int i = 0;
        long[] array = new long[data.getLdataCount()];
        for (long val : data.getLdataList()) {
          array[i++] = val;
        }
        return Arrays.factory(dataType, shape, array);
      }
      case ULONG: {
        int i = 0;
        long[] array = new long[data.getUldataCount()];
        for (long val : data.getUldataList()) {
          array[i++] = val;
        }
        return Arrays.factory(dataType, shape, array);
      }
      case FLOAT: {
        int i = 0;
        float[] array = new float[data.getFdataCount()];
        for (float val : data.getFdataList()) {
          array[i++] = val;
        }
        return Arrays.factory(dataType, shape, array);
      }
      case DOUBLE: {
        int i = 0;
        double[] array = new double[data.getDdataCount()];
        for (double val : data.getDdataList()) {
          array[i++] = val;
        }
        return Arrays.factory(dataType, shape, array);
      }
      case STRING: {
        int i = 0;
        String[] array = new String[data.getSdataCount()];
        for (String val : data.getSdataList()) {
          array[i++] = val;
        }
        return Arrays.factory(dataType, shape, array);
      }
      default:
        throw new IllegalStateException("Unknown datatype " + dataType);
    }
  }

  private static StructureDataArray decodeStructureDataArray(GcdmProto.Data arrayStructureProto) {
    int nrows = arrayStructureProto.getRowsCount();
    int[] shape = decodeShape(arrayStructureProto);

    // ok to have nrows = 0
    Preconditions.checkArgument(Arrays.computeSize(shape) == nrows);

    StructureMembers members = decodeStructureMembers(arrayStructureProto.getMembers()).build();
    ByteBuffer bbuffer = ByteBuffer.allocate(nrows * members.getStorageSizeBytes());
    StructureDataStorageBB storage = new StructureDataStorageBB(members, bbuffer, nrows);
    int row = 0;
    for (GcdmProto.StructureDataProto structProto : arrayStructureProto.getRowsList()) {
      decodeStructureData(structProto, members, storage, bbuffer, row);
      row++;
    }
    return new StructureDataArray(members, shape, storage);
  }

  private static StructureMembers.Builder decodeStructureMembers(GcdmProto.StructureMembersProto membersProto) {
    StructureMembers.Builder membersb = StructureMembers.builder();
    membersb.setName(membersProto.getName());
    for (StructureMemberProto memberProto : membersProto.getMembersList()) {
      MemberBuilder mb = StructureMembers.memberBuilder();
      mb.setName(memberProto.getName());
      mb.setArrayType(convertArrayType(memberProto.getArrayType()));
      mb.setShape(Ints.toArray(memberProto.getShapesList()));
      if (memberProto.hasMembers()) {
        mb.setStructureMembers(decodeStructureMembers(memberProto.getMembers()));
      }
      membersb.addMember(mb);
    }
    membersb.setStandardOffsets();
    return membersb;
  }

  private static void decodeStructureData(GcdmProto.StructureDataProto structDataProto, StructureMembers members,
      StructureDataStorageBB storage, ByteBuffer bbuffer, int rowidx) {
    for (int i = 0; i < structDataProto.getMemberDataCount(); i++) {
      GcdmProto.Data data = structDataProto.getMemberData(i);
      Member member = members.getMember(i);
      int computed = members.getStorageSizeBytes() * rowidx + member.getOffset();
      bbuffer.position(computed);
      decodeNestedData(member, data, storage, bbuffer);
    }
  }

  private static void decodeNestedData(Member member, GcdmProto.Data data, StructureDataStorageBB storage,
      ByteBuffer bb) {
    if (member.isVlen()) {
      if (data.getVlenCount() > 0) {
        ArrayVlen<?> vlen = decodeVlenData(data);
        int index = storage.putOnHeap(vlen);
        bb.putInt(index);
      } else { // because its row oriented, data often stored directly in Data
        Array<?> vlen = decodeData(data);
        int index = storage.putOnHeap(vlen);
        bb.putInt(index);
      }
      return;
    }

    ArrayType dataType = convertArrayType(data.getArrayType());
    switch (dataType) {
      case CHAR:
      case ENUM1:
      case UBYTE:
      case BYTE: {
        ByteString bs = data.getBdata(0);
        for (byte val : bs) {
          bb.put(val);
        }
        return;
      }
      case OPAQUE: {
        for (int i = 0; i < data.getBdataCount(); i++) {
          ByteString bs = data.getBdata(i);
          // TODO cant count on these being the same size
        }
      }
      case SHORT: {
        for (int val : data.getIdataList()) {
          bb.putShort((short) val);
        }
        return;
      }
      case INT: {
        for (int val : data.getIdataList()) {
          bb.putInt(val);
        }
        return;
      }
      case ENUM2:
      case USHORT: {
        for (int val : data.getIdataList()) {
          bb.putShort((short) val);
        }
        return;
      }
      case ENUM4:
      case UINT: {
        for (int val : data.getUidataList()) {
          bb.putInt(val);
        }
        return;
      }
      case LONG: {
        for (long val : data.getLdataList()) {
          bb.putLong(val);
        }
        return;
      }
      case ULONG: {
        for (long val : data.getUldataList()) {
          bb.putLong(val);
        }
        return;
      }
      case FLOAT: {
        for (float val : data.getFdataList()) {
          bb.putFloat(val);
        }
        return;
      }
      case DOUBLE: {
        for (double val : data.getDdataList()) {
          bb.putDouble(val);
        }
        return;
      }
      case STRING: {
        String[] vals = new String[data.getSdataCount()];
        int idx = 0;
        for (String val : data.getSdataList()) {
          vals[idx++] = val;
        }
        int index = storage.putOnHeap(vals);
        bb.putInt(index);
        return;
      }
      case SEQUENCE: {
        StructureDataArray seqData = decodeStructureDataArray(data);
        int index = storage.putOnHeap(seqData);
        bb.putInt(index);
        return;
      }
      case STRUCTURE: {
        Preconditions.checkArgument(member.getStructureMembers() != null);
        decodeNestedStructureDataArray(member.getStructureMembers(), data.getRowsList(), storage, bb);
        return;
      }
      default:
        throw new IllegalStateException("Unkown datatype " + dataType);
    }
  }

  private static void decodeNestedStructureDataArray(StructureMembers members, List<StructureDataProto> rows,
      StructureDataStorageBB storage, ByteBuffer bbuffer) {
    int offset = bbuffer.position();
    int rowidx = 0;
    for (GcdmProto.StructureDataProto structProto : rows) {
      int memberIdx = 0;
      for (Member nestedMember : members) {
        int computed = offset + members.getStorageSizeBytes() * rowidx + nestedMember.getOffset();
        bbuffer.position(computed);
        decodeNestedData(nestedMember, structProto.getMemberData(memberIdx), storage, bbuffer);
        memberIdx++;
      }
      rowidx++;
    }
  }

  private static <T> ArrayVlen<T> decodeVlenData(GcdmProto.Data data) {
    Preconditions.checkArgument(data.getVlenCount() > 0);
    int[] shape = decodeShape(data);
    int length = (int) Arrays.computeSize(shape);
    Preconditions.checkArgument(length == data.getVlenCount());

    ArrayType dataType = convertArrayType(data.getArrayType());
    ArrayVlen<T> result = ArrayVlen.factory(dataType, shape);

    for (int index = 0; index < length; index++) {
      GcdmProto.Data inner = data.getVlen(index);
      result.set(index, decodePrimitiveData(inner));
    }
    return result;
  }
}
