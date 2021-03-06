/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.core.api;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import dev.ucdm.core.util.CdmFullNames;
import dev.ucdm.array.Indent;
import dev.ucdm.array.StructureData;
import dev.ucdm.core.iosp.IOServiceProvider;

import org.jetbrains.annotations.Nullable;
import dev.ucdm.array.Immutable;
import java.io.Closeable;
import java.io.IOException;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * <p>
 * Read-only scientific datasets that are accessible through the netCDF API.
 * Immutable after {@code setImmutable()} is called. Reading data is not
 * thread-safe because of the use of {@code RandomAccessFile}.
 * <p>
 * Using this class's {@code Builder} scheme to create a {@code CdmFile} object could, for
 * example, be accomplished as follows, using a try/finally block to ensure that the
 * {@code CdmFile} is closed when done.
 * 
 * <pre>
 * CdmFile ncfile = null;
 * try {
 *   ncfile = CdmFile.builder().setLocation(fileName).build();
 *   // do stuff
 * } finally {
 *   if (ncfile != null) {
 *     ncfile.close();
 *   }
 * }
 * </pre>
 * 
 * More conveniently, a {@code CdmFile} object may be created using one of the static methods
 * in {@code CdmFiles}:
 * 
 * <pre>
 * CdmFile ncfile = null;
 * try {
 *   ncfile = CdmFiles.open(fileName);
 *   // do stuff
 * } finally {
 *   if (ncfile != null) {
 *     ncfile.close();
 *   }
 * }
 * </pre>
 * 
 * Or better yet, use try-with-resources:
 * 
 * <pre>
 * try (CdmFile ncfile = CdmFiles.open(fileName)) {
 *   // do stuff
 * }
 * </pre>
 *
 * <h3>Naming</h3>
 * Each object has a name (aka "full name") that is unique within the entire netcdf file, and
 * a "short name" that is unique within the parent group.
 * These coincide for objects in the root group, and so are backwards compatible with version
 * 3 files.
 * <ol>
 * <li>Variable: group1/group2/varname
 * <li>Structure member Variable: group1/group2/varname.s1.s2
 * <li>Group Attribute: group1/group2@attName
 * <li>Variable Attribute: group1/group2/varName@attName
 * </ol>
 * </p>
 */
@Immutable
public class CdmFile implements Closeable {
  public static final String IOSP_MESSAGE_ADD_RECORD_STRUCTURE = "AddRecordStructure";
  public static final String IOSP_MESSAGE_GET_HEADER = "header";
  public static final String IOSP_MESSAGE_GET_IOSP = "IOSP";
  public static final String IOSP_MESSAGE_GET_NETCDF_FILE_FORMAT = "CdmFileFormat";
  public static final String IOSP_MESSAGE_RANDOM_ACCESS_FILE = "RandomAccessFile";

  static boolean debugSPI, debugCompress;
  static boolean debugStructureIterator;
  private static boolean showRequest;

  /**
   * Close all resources (files, sockets, etc) associated with this file. If the underlying file was acquired, it will
   * be released,
   * otherwise closed. if isClosed() already, nothing will happen
   *
   * @throws IOException if error when closing
   */
  public synchronized void close() throws IOException {
    try {
      if (null != iosp) {
        // log.warn("CdmFile.close called for ncfile="+this.hashCode()+" for iosp="+spi.hashCode());
        iosp.close();
      }
    } finally {
      iosp = null;
    }
  }

  public AttributeContainer attributes() {
    return rootGroup.attributes();
  }

  //////////////////////////////////////////////////////////////////////////////////////
  private CdmFullNames cdmFullNames = null;

  /** Helper class for dealing with full names. */
  public CdmFullNames cdmFullNames() {
    if (cdmFullNames == null) {
      cdmFullNames = new CdmFullNames(this);
    }
    return cdmFullNames;
  }


  /**
   * Find an attribute, with the specified (escaped full) name. It may be nested in multiple groups and/or structures.
   * An embedded "." is interpreted as structure.member.
   * An embedded "/" is interpreted as group/group or group/variable.
   * An embedded "@" is interpreted as variable@attribute.
   * A name without an "@" is interpreted as an attribute in the root group.
   * If the name actually has a ".", you must escape it (call CdmFiles.makeValidPathName(varname)).
   * Any other chars may also be escaped, as they are removed before testing.
   *
   * @param fullNameEscaped eg "attName", "@attName", "var@attname", "struct.member.@attName",
   *        "/group/subgroup/@attName", "/group/subgroup/var@attName", or "/group/subgroup/struct.member@attName"
   * @return Attribute or null if not found.
   */
  @Nullable
  public Attribute findAttribute(@Nullable String fullNameEscaped) {
    if (Strings.isNullOrEmpty(fullNameEscaped)) {
      return null;
    }
    return cdmFullNames().findAttribute(fullNameEscaped);
  }

  /**
   * Finds a Dimension with the specified full name. It may be nested in multiple groups. An embedded "/" is interpreted
   * as a group separator. A leading slash indicates the root group. That slash may be omitted, but the {@code fullName}
   * will be treated as if it were there. In other words, the first name token in {@code fullName} is treated as the
   * short name of a Group or Dimension, relative to the root group.
   *
   * @param fullName Dimension full name, e.g. "/group/subgroup/dim".
   * @return the Dimension or {@code null} if it wasn't found.
   */

  @Nullable
  public Dimension findDimension(@Nullable String fullName) {
    if (fullName == null || fullName.isEmpty()) {
      return null;
    }

    Group group = rootGroup;
    String dimShortName = fullName;

    // break into group/group and dim
    int pos = fullName.lastIndexOf('/');
    if (pos >= 0) {
      String groups = fullName.substring(0, pos);
      dimShortName = fullName.substring(pos + 1);

      StringTokenizer stoke = new StringTokenizer(groups, "/");
      while (stoke.hasMoreTokens()) {
        String token = CdmFiles.makeNameUnescaped(stoke.nextToken());
        group = group.findGroupLocal(token);

        if (group == null) {
          return null;
        }
      }
    }

    return group.findDimensionLocal(dimShortName);
  }

  /**
   * Find a Group, with the specified (full) name. A full name should start with a '/'. For backwards compatibility, we
   * accept full names
   * that omit the leading '/'. An embedded '/' separates subgroup names.
   *
   * @param fullName eg "/group/subgroup/wantGroup". Null or empty string returns the root group.
   * @return Group or null if not found.
   */
  @Nullable
  public Group findGroup(@Nullable String fullName) {
    if (fullName == null || fullName.isEmpty()) {
      return rootGroup;
    }
    return cdmFullNames().findGroup(fullName);
  }

  /**
   * Find a Variable, with the specified (escaped full) name. It may possibly be nested in multiple groups and/or
   * structures. An embedded "." is interpreted as structure.member. An embedded "/" is interpreted as group/variable.
   * If the name actually has a ".", you must escape it (call CdmFiles.makeValidPathName(varname)).
   * Any other chars may also be escaped, as they are removed before testing.
   *
   * @param fullName eg "/group/subgroup/name1.name2.name".
   * @return Variable or null if not found.
   */
  @Nullable
  public Variable findVariable(String fullName) {
    return cdmFullNames().findVariable(fullName);
  }

  /** Get all shared Dimensions used in this file, in all groups. Alternatively, use groups. */
  public List<Dimension> getDimensions() {
    return allDimensions;
  }

  /** Get all of the variables in the file, in all groups. Alternatively, use groups. */
  public List<Variable> getVariables() {
    return allVariables;
  }

  /**
   * Get the file type id for the underlying data source.
   *
   * @return registered id of the file type
   * @see "https://www.unidata.ucar.edu/software/netcdf-java/formats/FileTypes.html"
   */
  @Nullable
  public String getCdmFileTypeId() {
    if (iosp != null) {
      return iosp.getCdmFileTypeId();
    }
    return null;
  }

  /**
   * Get a human-readable description for this file type.
   *
   * @return description of the file type
   * @see "https://www.unidata.ucar.edu/software/netcdf-java/formats/FileTypes.html"
   */
  public String getCdmFileTypeDescription() {
    if (iosp != null) {
      return iosp.getCdmFileTypeDescription();
    }
    return "N/A";
  }

  /**
   * Get the version of this file type.
   *
   * @return version of the file type
   * @see "https://www.unidata.ucar.edu/software/netcdf-java/formats/FileTypes.html"
   */
  public String getCdmFileTypeVersion() {
    if (iosp != null) {
      return iosp.getCdmFileTypeVersion();
    }
    return "N/A";
  }


  /**
   * Get the CdmFile location. This is a URL, or a file pathname.
   *
   * @return location URL or file pathname.
   */
  public String getLocation() {
    return location;
  }

  /**
   * Get the globally unique dataset identifier, if it exists.
   *
   * @return id, or null if none.
   */
  @Nullable
  public String getId() {
    return id;
  }

  /**
   * Get the root group.
   *
   * @return root group
   */
  public Group getRootGroup() {
    return rootGroup;
  }

  /**
   * Get the human-readable title, if it exists.
   *
   * @return title, or null if none.
   */
  @Nullable
  public String getTitle() {
    return title;
  }

  /**
   * Return the unlimited (record) dimension, or null if not exist.
   * If there are multiple unlimited dimensions, it will return the first one.
   *
   * @return the unlimited Dimension, or null if none.
   */
  @Nullable
  public Dimension getUnlimitedDimension() {
    for (Dimension d : allDimensions) {
      if (d.isUnlimited()) {
        return d;
      }
    }
    return null;
  }

  /**
   * Return true if this file has one or more unlimited (record) dimension.
   *
   * @return if this file has an unlimited Dimension(s)
   */
  public boolean hasUnlimitedDimension() {
    return getUnlimitedDimension() != null;
  }

  //////////////////////////////////////////////////////////////////////////////////////
  // Service Provider calls
  // All IO eventually goes through these calls.

  protected Iterator<StructureData> getSequenceIterator(Sequence s, int bufferSize) throws IOException {
    Preconditions.checkNotNull(iosp);
    return iosp.getSequenceIterator(s, bufferSize);
  }

  /**
   * Do not call this directly, use Variable.readArray() !!
   * Ranges must be filled (no nulls)
   */
  @Nullable
  protected dev.ucdm.array.Array<?> readArrayData(Variable v, dev.ucdm.array.Section ranges)
      throws IOException, dev.ucdm.array.InvalidRangeException {
    if (iosp == null) {
      throw new IOException("iosp is null, perhaps file has been closed. Trying to read variable " + v.getFullName());
    }
    return iosp.readArrayData(v, ranges);
  }

  /**
   * Read a variable using the given section specification.
   * The result is always an array of the type of the innermost variable.
   * Its shape is the accumulation of all the shapes of its parent structures.
   *
   * @param variableSection the constraint expression.
   * @see <a href=
   *      "https://www.unidata.ucar.edu/software/netcdf-java/reference/SectionSpecification.html">SectionSpecification</a>
   */
  public dev.ucdm.array.Array<?> readSectionArray(String variableSection)
      throws IOException, dev.ucdm.array.InvalidRangeException {
    ParsedArraySectionSpec cer = ParsedArraySectionSpec.parseVariableSection(this, variableSection);
    if (cer.getChild() == null) {
      return cer.getVariable().readArray(cer.getSection());
    }
    throw new UnsupportedOperationException();
  }

  /**
   * Generic way to send a "message" to the underlying IOSP.
   * This message is sent after the file is open. To affect the creation of the file,
   * use a factory method like CdmFile.open().
   *
   * @param message iosp specific message
   * @return iosp specific return, may be null
   */
  public Object sendIospMessage(Object message) {
    if (null == message) {
      return null;
    }

    if (message == IOSP_MESSAGE_GET_IOSP) {
      return this.iosp;
    }

    if (message == IOSP_MESSAGE_ADD_RECORD_STRUCTURE) {
      Variable v = rootGroup.findVariableLocal("record");
      boolean gotit = (v instanceof Structure);
      return gotit || makeRecordStructure(); // TODO
    }

    if (iosp != null) {
      return iosp.sendIospMessage(message);
    }
    return null;
  }

  /**
   * If there is an unlimited dimension, make all variables that use it into a Structure.
   * A Variable called "record" is added.
   * You can then access these through the record structure.
   *
   * @return true if it has a Nectdf-3 record structure
   */
  private boolean makeRecordStructure() {
    Boolean didit = false;
    if ((iosp != null) && hasUnlimitedDimension()) {
      didit = (Boolean) iosp.sendIospMessage(IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
    }
    return (didit != null) && didit;
  }

  //////////////////////////////////////////////////////////////////////////////////////

  /** CDL representation of Netcdf header info, non strict */
  // TODO can we use CDLwriter?
  @Override
  public String toString() {
    Formatter f = new Formatter();
    writeCDL(f, new Indent(2, 0), false);
    return f.toString();
  }

  void writeCDL(Formatter f, Indent indent, boolean strict) {
    toStringStart(f, indent, strict);
    f.format("%s}%n", indent);
  }

  private void toStringStart(Formatter f, Indent indent, boolean strict) {
    String name = getLocation();
    if (strict) {
      if (name.endsWith(".nc")) {
        name = name.substring(0, name.length() - 3);
      }
      if (name.endsWith(".cdl")) {
        name = name.substring(0, name.length() - 4);
      }
      name = CdmFiles.makeValidCDLName(name);
    }
    f.format("%snetcdf %s {%n", indent, name);
    indent.incr();
    rootGroup.writeCDL(f, indent, strict);
    indent.decr();
  }

  ///////////////////////////////////////////////////////////////////////////////////

  public long getLastModified() {
    if (iosp != null) {
      return iosp.getLastModified();
    }
    return 0;
  }

  /** Show debug / underlying implementation details */
  public String getDetailInfo() {
    Formatter f = new Formatter();
    getDetailInfo(f);
    return f.toString();
  }

  /** Show debug / underlying implementation details */
  public void getDetailInfo(Formatter f) {
    f.format("CdmFile location= %s%n", getLocation());
    f.format("  title= %s%n", getTitle());
    f.format("  id= %s%n", getId());
    f.format("  fileType= %s%n", getCdmFileTypeId());
    f.format("  fileDesc= %s%n", getCdmFileTypeDescription());
    f.format("  fileVersion= %s%n", getCdmFileTypeVersion());

    f.format("  class= %s%n", getClass().getName());
    if (iosp == null) {
      f.format("  has no IOSP%n");
    } else {
      f.format("  iosp= %s%n%n", iosp.getClass());
      f.format("%s", iosp.getDetailInfo());
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  private final String location;
  private final String id;
  private final String title;
  private final Group rootGroup;

  @Nullable
  private IOServiceProvider iosp; // may be null when completely self contained, eg NcML

  // "global view" over all groups.
  private final ImmutableList<Variable> allVariables;
  private final ImmutableList<Dimension> allDimensions;

  protected CdmFile(Builder<?> builder) {
    this.location = builder.location;
    this.id = builder.id;
    this.title = builder.title;

    if (builder.rootGroup != null) {
      builder.rootGroup.setNcfile(this);
      this.rootGroup = builder.rootGroup.build();
    } else {
      rootGroup = Group.builder().setNcfile(this).setName("").build();
    }
    if (builder.iosp != null) {
      builder.iosp.buildFinish(this);
    }
    this.iosp = builder.iosp;

    // all global attributes, dimensions, variables
    ImmutableList.Builder<Dimension> dlist = ImmutableList.builder();
    ImmutableList.Builder<Variable> vlist = ImmutableList.builder();
    extractAll(rootGroup, dlist, vlist);
    allDimensions = dlist.build();
    allVariables = vlist.build();
  }

  private void extractAll(Group group, ImmutableList.Builder<Dimension> dlist, ImmutableList.Builder<Variable> vlist) {

    group.getDimensions().stream().filter(Dimension::isShared).forEach(dlist::add);
    vlist.addAll(group.getVariables());

    for (Group nested : group.getGroups()) {
      extractAll(nested, dlist, vlist);
    }
  }

  /** Turn into a mutable Builder. Can use toBuilder().build() to copy. */
  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the passed - in builder.
  protected Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> b) {
    return b.setLocation(this.location).setId(this.id).setTitle(this.title).setRootGroup(this.rootGroup.toBuilder())
        .setIosp(this.iosp);
  }

  /**
   * Get Builder for this class.
   * Allows subclassing.
   *
   * @see "https://community.oracle.com/blogs/emcmanus/2010/10/24/using-builder-pattern-subclasses"
   */
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
    public Group.Builder rootGroup = Group.builder().setName("");
    private String id;
    private String title;
    public String location;
    protected IOServiceProvider iosp;
    private boolean built;

    protected abstract T self();

    public T setRootGroup(Group.Builder rootGroup) {
      Preconditions.checkArgument(rootGroup.shortName.equals(""), "root group name must be empty string");
      this.rootGroup = rootGroup;
      return self();
    }

    public T setIosp(IOServiceProvider iosp) {
      this.iosp = iosp;
      return self();
    }

    public T setId(String id) {
      this.id = id;
      return self();
    }

    /** Set the dataset "human readable" title. */
    public T setTitle(String title) {
      this.title = title;
      return self();
    }

    /** Set the location, a URL or local filename. */
    public T setLocation(String location) {
      this.location = location;
      return self();
    }

    public CdmFile build() {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new CdmFile(this);
    }
  }

}
