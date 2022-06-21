/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.core.write;

import com.google.common.base.Preconditions;
import dev.cdm.array.Array;
import dev.cdm.array.ArrayType;
import dev.cdm.array.Arrays;
import dev.cdm.array.Index;
import dev.cdm.array.InvalidRangeException;
import dev.cdm.array.Section;
import dev.cdm.array.StructureData;
import dev.cdm.core.api.Attribute;
import dev.cdm.core.api.Dimension;
import dev.cdm.core.api.Dimensions;
import dev.cdm.core.api.Group;
import dev.cdm.core.api.CdmFile;
import dev.cdm.core.api.CdmFiles;
import dev.cdm.core.api.Structure;
import dev.cdm.core.api.Variable;
import dev.cdm.core.netcdf3.N3iosp;
import dev.cdm.core.netcdf3.N3iospWriter;
import dev.cdm.core.iosp.IOServiceProvider;
import dev.cdm.core.io.RandomAccessFile;

import dev.cdm.core.netcdf3.UnlimitedDimension;
import org.jetbrains.annotations.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static dev.cdm.core.api.CdmFile.IOSP_MESSAGE_GET_NETCDF_FILE_FORMAT;

/**
 * Creates new Netcdf 3 format files. Writes data to new or existing files.
 * 
 * <pre>
 * Netcdf3FormatWriter.Builder writerb = Netcdf3FormatWriter.createNewNetcdf3(testFile.getPath());
 * writerb.addDimension(Dimension.builder().setName("vdim").setIsUnlimited(true).build());
 * writerb.addVariable("v", DataType.BYTE, "vdim");
 * try (Netcdf3FormatWriter writer = writerb.build()) {
 *   writer.config().forVariable("v").withArray(dataArray).write();
 * }
 * </pre>
 */
public class Netcdf3FormatWriter implements Closeable {

  /**
   * Create a new Netcdf3 file.
   *
   * @param location name of new file to open; if it exists, will overwrite it.
   */
  public static Builder<?> createNewNetcdf3(String location) {
    return builder().setFormat(NetcdfFileFormat.NETCDF3).setLocation(location);
  }

  /**
   * Open an existing Netcdf 3 format file for writing data.
   * Cannot add new objects, you can only read/write data to existing Variables.
   *
   * @param location name of existing NetCDF 3 file to open.
   * @return existing Builder that can be modified
   */
  public static Builder<?> openExisting(String location) throws IOException {
    try (CdmFile ncfile = CdmFiles.open(location)) {
      IOServiceProvider iosp = (IOServiceProvider) ncfile.sendIospMessage(CdmFile.IOSP_MESSAGE_GET_IOSP);
      Preconditions.checkArgument(iosp instanceof N3iosp, "Can only modify Netcdf-3 files");
      Group.Builder root = ncfile.getRootGroup().toBuilder();
      NetcdfFileFormat format = (NetcdfFileFormat) iosp.sendIospMessage(IOSP_MESSAGE_GET_NETCDF_FILE_FORMAT);
      if (format != null && !format.isNetdf3format() && !format.isNetdf4format()) {
        throw new IllegalArgumentException(String.format("%s is not a netcdf-3 file (%s)", location, format));
      }
      return builder().setRootGroup(root).setLocation(location).setIosp(iosp).setFormat(format).setIsExisting();
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /** The output file's root group. */
  public Group getRootGroup() {
    return ncout.getRootGroup();
  }

  /** The output file's format. */
  public NetcdfFileFormat getFormat() {
    return this.format;
  }

  /** Find the named Variable in the output file. */
  @Nullable
  public Variable findVariable(String fullNameEscaped) {
    return ncout.findVariable(fullNameEscaped);
  }

  /** Find the named Dimension in the output file. */
  @Nullable
  public Dimension findDimension(String dimName) {
    return ncout.findDimension(dimName);
  }

  /** Find the named global attribute in the output file. */
  @Nullable
  public Attribute findGlobalAttribute(String attName) {
    return getRootGroup().findAttribute(attName);
  }

  /**
   * An estimate of the size of the file when written to disk. Ignores compression for netcdf4.
   * 
   * @return estimated file size in bytes.
   */
  public long calcSize() {
    return calcSize(getRootGroup());
  }

  // Note that we have enough info to try to estimate effects of compression, if its a Netcdf4 file.
  private long calcSize(Group group) {
    long totalSizeOfVars = 0;
    for (Variable var : group.getVariables()) {
      totalSizeOfVars += Dimensions.getSize(var.getDimensions()) * var.getElementSize();
    }
    for (Group nested : group.getGroups()) {
      totalSizeOfVars += calcSize(nested);
    }
    return totalSizeOfVars;
  }

  ////////////////////////////////////////////
  // use these calls to write data to the file

  public void write(String varName, Index origin, Array<?> values)
      throws IOException, InvalidRangeException {
    Variable v = findVariable(varName);
    Preconditions.checkNotNull(v);
    write(v, origin, values);
  }

  /**
   * Write to Variable with an Array.
   * 
   * @param v variable to write to
   * @param origin offset within the variable to start writing.
   * @param values write this array; must have compatible type and shape with Variable
   */
  public void write(Variable v, Index origin, Array<?> values) throws IOException, InvalidRangeException {
    spiw.writeData(v, new Section(origin.getCurrentIndex(), values.getShape()), values);
  }

  /**
   * Write String value to a CHAR Variable.
   * Truncated or zero extended as needed to fit into last dimension of v.
   * Note that origin is not incremeted as in previous versions.
   * 
   * <pre>
   * Index index = Index.ofRank(v.getRank());
   * writer.writeStringData(v, index, "This is the first string.");
   * writer.writeStringData(v, index.incr(0), "Shorty");
   * writer.writeStringData(v, index.incr(0), "This is too long so it will get truncated");
   * </pre>
   * 
   * @param v write to this variable, must be of type CHAR.
   * @param origin offset within the variable to start writing. The innermost dimension must be 0.
   * @param data The String to write.
   */
  public void writeStringData(Variable v, Index origin, String data) throws IOException, InvalidRangeException {
    Preconditions.checkArgument(v.getArrayType() == ArrayType.CHAR);
    Preconditions.checkArgument(v.getRank() > 0);
    int[] shape = v.getShape();
    // all but the last shape is 1
    for (int i = 0; i < shape.length - 1; i++) {
      shape[i] = 1;
    }
    int last = shape[shape.length - 1];

    // previously we truncated chars to bytes.
    // here we are going to use UTF encoded bytes, just as if we were real programmers.
    byte[] bb = data.getBytes(StandardCharsets.UTF_8);
    if (bb.length != last) {
      byte[] storage = new byte[last];
      System.arraycopy(bb, 0, storage, 0, Math.min(bb.length, last));
      bb = storage;
    }

    Array<?> barray = Arrays.factory(ArrayType.CHAR, shape, bb);
    write(v, origin, barray);
  }

  public void writeStringData(Variable v, Index origin, Array<String> data) throws IOException, InvalidRangeException {
    Preconditions.checkArgument(v.getArrayType() == ArrayType.CHAR);
    Preconditions.checkArgument(v.getRank() > 0);
    Preconditions.checkArgument(v.getRank() == data.getRank() + 1);

    int[] shape = v.getShape();
    int[] dataShape = data.getShape();
    for (int i = 0; i < dataShape.length; i++) {
      shape[i] = dataShape[i];
    }
    int last = shape[shape.length - 1];

    // write all at once by copying bytes into a single array
    byte[] storage = new byte[(int) Arrays.computeSize(shape)];
    int destPos = 0;
    for (String sdata : data) {
      byte[] sb = sdata.getBytes(StandardCharsets.UTF_8);
      System.arraycopy(sb, 0, storage, destPos, Math.min(sb.length, last));
      destPos += last;
    }
    Array<?> barray = Arrays.factory(ArrayType.CHAR, shape, storage);
    write(v, origin, barray);
  }

  /**
   * Write StructureData along the unlimited dimension.
   * 
   * @return the recnum where it was written.
   */
  // TODO does this work?
  public int appendStructureData(Structure s, StructureData sdata) throws IOException, InvalidRangeException {
    return spiw.appendStructureData(s, sdata);
  }

  /**
   * Update the value of an existing attribute. Attribute is found by name, which must match exactly.
   * You cannot make an attribute longer, or change the number of values.
   * For strings: truncate if longer, zero fill if shorter. Strings are padded to 4 byte boundaries, ok to use padding
   * if it exists.
   * For numerics: must have same number of values.
   * <p/>
   * This is really a netcdf-3 writing only, in particular supporting point feature writing.
   * netcdf-4 attributes can be changed without rewriting.
   *
   * @param v2 variable, or null for global attribute
   * @param att replace with this value
   * @throws IOException if I/O error
   */
  public void updateAttribute(Variable v2, Attribute att) throws IOException {
    spiw.updateAttribute(v2, att);
  }

  /** Close the file. */
  @Override
  public synchronized void close() throws IOException {
    if (!isClosed) {
      spiw.close();
      isClosed = true;
    }
  }

  /** Abort writing to this file. The file is closed. */
  public void abort() throws IOException {
    if (!isClosed) {
      spiw.close();
      isClosed = true;
    }
  }

  ////////////////////////////////////////////////////////////////////////////////
  final String location;
  final boolean fill;
  final int extraHeaderBytes;
  final long preallocateSize;
  final boolean isExisting;

  final NetcdfFileFormat format;
  final CdmFile ncout;
  final N3iospWriter spiw;

  private boolean isClosed = false;

  Netcdf3FormatWriter(Builder<?> builder) throws IOException {
    this.location = builder.location;
    this.fill = builder.fill;
    this.extraHeaderBytes = builder.extraHeaderBytes;
    this.preallocateSize = builder.preallocateSize;
    this.isExisting = builder.isExisting;

    if (isExisting) {
      // read existing file to get the format
      try (RandomAccessFile existingRaf = new RandomAccessFile(location, "r")) {
        this.format = NetcdfFileFormat.findNetcdfFormatType(existingRaf);
      }

      N3iospWriter spi = new N3iospWriter(builder.iosp);
      try {
        // builder.iosp has the metadata of the file to be created.
        // NC3 doesnt allow additions, NC4 should. So this is messed up here.
        // NC3 can only write to existing variables and extend along the record dimension.
        spi.openForWriting(location, builder.rootGroup, null);
        spi.setFill(fill);
      } catch (Throwable t) {
        spi.close();
        throw t;
      }
      this.ncout = spi.getOutputFile();
      this.spiw = spi;

    } else { // create file
      this.format = builder.format;
      N3iospWriter spi = new N3iospWriter();
      try {
        // builder.rootGroup has the metadata of the file to be created.
        this.ncout =
            spi.create(location, builder.rootGroup, extraHeaderBytes, preallocateSize, this.format.isLargeFile());
        spi.setFill(fill);
      } catch (Throwable t) {
        spi.close();
        throw t;
      }
      this.spiw = spi;
    }

  }

  /////////////////////////////////////////////////////////////////////////////
  /** Obtain a mutable Builder to create or modify the file metadata. */
  public static Builder<?> builder() {
    return new Builder2();
  }

  private static class Builder2 extends Builder<Builder2> {
    @Override
    protected Builder2 self() {
      return this;
    }
  }

  public static abstract class Builder<T extends Builder<T>> {
    String location;
    boolean fill = true;
    int extraHeaderBytes;
    long preallocateSize;
    boolean isExisting;

    public NetcdfFileFormat format = NetcdfFileFormat.NETCDF3;
    IOServiceProvider iosp; // existing only
    Group.Builder rootGroup = Group.builder().setName("");

    protected abstract T self();

    public Builder<?> setIosp(IOServiceProvider iosp) {
      this.iosp = iosp;
      return this;
    }

    /** The file locatipn */
    public T setLocation(String location) {
      this.location = location;
      return self();
    }

    public T setIsExisting() {
      this.isExisting = true;
      return self();
    }

    /**
     * Set the fill flag. Only used by netcdf-3.
     * If true, the data is first written with fill values.
     * Default is fill = true, to follow the C library.
     * Set false if you expect to write all data values, which makes writing faster.
     * Set true if you want to be sure that unwritten data values are set to the fill value.
     */
    public T setFill(boolean fill) {
      this.fill = fill;
      return self();
    }

    /** Set the format version. Only needed when its a new file. Default is NetcdfFileFormat.NETCDF3 */
    public T setFormat(NetcdfFileFormat format) {
      this.format = format;
      return self();
    }

    /**
     * Set extra bytes to reserve in the header. Only used by netcdf-3.
     * This can prevent rewriting the entire file on redefine.
     *
     * @param extraHeaderBytes # bytes extra for the header
     */
    public T setExtraHeader(int extraHeaderBytes) {
      this.extraHeaderBytes = extraHeaderBytes;
      return self();
    }

    /** Preallocate the file size, for efficiency. Only used by netcdf-3. */
    public T setPreallocateSize(long preallocateSize) {
      this.preallocateSize = preallocateSize;
      return self();
    }

    /** Add a global attribute */
    public T addAttribute(Attribute att) {
      rootGroup.addAttribute(att);
      return self();
    }

    /** Add a dimension to the root group. */
    public Dimension addDimension(String dimName, int length) {
      return addDimension(new Dimension(dimName, length));
    }

    /** Add a dimension to the root group. */
    public Dimension addDimension(Dimension dim) {
      Dimension useDim = dim;
      if (dim.isUnlimited() && !(dim instanceof UnlimitedDimension)) {
        useDim = new UnlimitedDimension(dim.getShortName(), dim.getLength());
      }
      rootGroup.addDimension(useDim);
      return useDim;
    }

    /** Add an unlimited dimension to the root group. */
    public Dimension addUnlimitedDimension(String dimName) {
      return addDimension(new UnlimitedDimension(dimName, 0));
    }

    /** Get the root group */
    public Group.Builder getRootGroup() {
      return rootGroup;
    }

    /** Set the root group. This allows metadata to be modified externally. */
    public T setRootGroup(Group.Builder rootGroup) {
      this.rootGroup = rootGroup;
      return self();
    }

    /** Add a Variable to the root group. */
    public Variable.Builder<?> addVariable(String shortName, ArrayType dataType, String dimString) {
      Variable.Builder<?> vb = Variable.builder().setName(shortName).setArrayType(dataType)
          .setParentGroupBuilder(rootGroup).setDimensionsByName(dimString);
      rootGroup.addVariable(vb);
      return vb;
    }

    /** Add a Variable to the root group. */
    public Variable.Builder<?> addVariable(String shortName, ArrayType dataType, List<Dimension> dims) {
      Variable.Builder<?> vb = Variable.builder().setName(shortName).setArrayType(dataType)
          .setParentGroupBuilder(rootGroup).setDimensions(dims);
      rootGroup.addVariable(vb);
      return vb;
    }

    /** Add a Structure to the root group. */
    public Structure.Builder<?> addStructure(String shortName, String dimString) {
      Structure.Builder<?> vb =
          Structure.builder().setName(shortName).setParentGroupBuilder(rootGroup).setDimensionsByName(dimString);
      rootGroup.addVariable(vb);
      return vb;
    }

    /*
     * TODO doesnt work yet
     * public Optional<Variable.Builder<?>> renameVariable(String oldName, String newName) {
     * Optional<Variable.Builder<?>> vbOpt = getRootGroup().findVariableLocal(oldName);
     * vbOpt.ifPresent(vb -> {
     * rootGroup.removeVariable(oldName);
     * vb.setName(newName);
     * rootGroup.addVariable(vb);
     * });
     * return vbOpt;
     * }
     */

    /** Once this is called, do not use the Builder again. */
    public Netcdf3FormatWriter build() throws IOException {
      return new Netcdf3FormatWriter(this);
    }
  }

  /** Obtain a WriteConfig to configure data writing. */
  public WriteConfig config() {
    return new WriteConfig();
  }

  /** Fluid API for writing. */
  public class WriteConfig {
    Variable v;
    String varName;
    Index origin;
    Object primArray;
    Array<?> values;
    int[] shape;
    String stringValue;

    /** Write to this Variable. Set Variable or Variable name. */
    public WriteConfig forVariable(Variable v) {
      this.v = v;
      return this;
    }

    /** Write to this named Variable. Set Variable or Variable name. */
    public WriteConfig forVariable(String varName) {
      this.varName = varName;
      return this;
    }

    /**
     * The starting element, ie write(int[] origin, int[] shape).
     * If not set, origin of 0 is assumed.
     */
    public WriteConfig withOrigin(Index origin) {
      this.origin = origin;
      return this;
    }

    /** The starting element as an int[]. */
    public WriteConfig withOrigin(int... origin) {
      this.origin = Index.of(origin);
      return this;
    }

    /** The values to write. ArrayType must match the Variable. */
    public WriteConfig withArray(Array<?> values) {
      this.values = values;
      this.shape = values.getShape();
      return this;
    }

    /**
     * The values to write as a 1D java primitive array, eg float[].
     * 
     * @see Arrays#factory(ArrayType type, int[] shape, Object primArray)
     */
    public WriteConfig withPrimitiveArray(Object primArray) {
      this.primArray = primArray;
      return this;
    }

    /**
     * Shape of primitive array to write, ie write(int[] origin, int[] shape).
     * Only needed if you use withPrimitiveArray, otherwise the Array values' shape is used.
     * Use v.getShape() if shape is not set, and Array values is not set.
     */
    public WriteConfig withShape(int... shape) {
      this.shape = shape;
      return this;
    }

    /**
     * For writing a String into a CHAR Variable.
     */
    public WriteConfig withString(String sval) {
      this.stringValue = sval;
      return this;
    }

    /**
     * Do the write to the file, after constructing the WriteConfig.
     * 
     * @throws IllegalArgumentException when not configured correctly.
     */
    public void write() throws IOException, InvalidRangeException, IllegalArgumentException {
      if (this.v == null && this.varName == null) {
        throw new IllegalArgumentException("Must set Variable");
      }
      if (v == null) {
        this.v = findVariable(this.varName);
        if (this.v == null) {
          throw new IllegalArgumentException("Unknown Variable " + this.varName);
        }
      }
      if (this.origin == null) {
        this.origin = Index.ofRank(v.getRank());
      }

      if (stringValue != null) {
        Netcdf3FormatWriter.this.writeStringData(this.v, this.origin, stringValue);
        return;
      }

      if (this.values == null) {
        if (this.primArray == null) {
          throw new IllegalArgumentException("Must set Array or primitive array");
        }
        if (this.shape == null) {
          this.shape = v.getShape();
        }
        this.values = Arrays.factory(v.getArrayType(), this.shape, this.primArray);
      }
      Netcdf3FormatWriter.this.write(this.v, this.origin, this.values);
    }
  }
}
