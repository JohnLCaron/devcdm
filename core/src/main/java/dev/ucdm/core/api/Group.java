/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.core.api;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import dev.ucdm.array.ArrayType;
import dev.ucdm.core.util.EscapeStrings;
import dev.ucdm.array.Indent;

import org.jetbrains.annotations.Nullable;
import dev.ucdm.array.Immutable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;

import static dev.ucdm.core.api.CdmFiles.reservedFullName;

/**
 * A logical collection of Variables, Attributes, and Dimensions.
 * The Groups in a Dataset form a hierarchical tree, like directories on a disk.
 * A Group has a name and optionally a set of Attributes.
 * There is always at least one Group in a dataset, the root Group, whose name is the empty string.
 */
@Immutable
public class Group {

  /** The attributes contained by this Group. */
  public AttributeContainer attributes() {
    return attributes;
  }

  /**
   * Get the common parent of this and the other group.
   * Cant fail, since the root group is always a parent of any 2 groups.
   *
   * @param other the other group
   * @return common parent of this and the other group
   */
  public Group commonParent(Group other) {
    if (isParent(other)) {
      return this;
    }
    if (other.isParent(this)) {
      return other;
    }
    while (other != null && !other.isParent(this)) {
      other = other.getParentGroup();
    }
    return other;
  }

  /** Find the attribute by name, or null if not exist */
  @Nullable
  public Attribute findAttribute(String name) {
    return attributes.findAttribute(name);
  }

  /**
   * Find a String-valued Attribute by name (ignore case), return the String value of the Attribute.
   *
   * @return the attribute value, or defaultValue if not found
   */
  public String findAttributeString(String attName, String defaultValue) {
    return attributes.findAttributeString(attName, defaultValue);
  }

  /** Find a Dimension in this or a parent Group, matching on short name */
  public Optional<Dimension> findDimension(String name) {
    if (name == null) {
      return Optional.empty();
    }
    Dimension d = findDimensionLocal(name);
    if (d != null) {
      return Optional.of(d);
    }
    Group parent = getParentGroup();
    if (parent != null) {
      return parent.findDimension(name);
    }
    return Optional.empty();
  }

  /** Find a Dimension in this or a parent Group, using equals, or null if not found */
  @Nullable
  public Dimension findDimension(Dimension dim) {
    if (dim == null) {
      return null;
    }
    for (Dimension d : dimensions) {
      if (d.equals(dim)) {
        return d;
      }
    }
    Group parent = getParentGroup();
    if (parent != null) {
      return parent.findDimension(dim);
    }
    return null;
  }

  /** Find a Dimension using its (short) name, in this group only, or null if not found */
  @Nullable
  public Dimension findDimensionLocal(String shortName) {
    if (shortName == null) {
      return null;
    }
    for (Dimension d : dimensions) {
      if (shortName.equals(d.getShortName())) {
        return d;
      }
    }
    return null;
  }

  /** Find a Enumeration in this or a parent Group, using its short name. */
  @Nullable
  public EnumTypedef findEnumeration(String name) {
    if (name == null) {
      return null;
    }
    for (EnumTypedef d : enumTypedefs) {
      if (name.equals(d.getShortName())) {
        return d;
      }
    }
    Group parent = getParentGroup();
    if (parent != null) {
      return parent.findEnumeration(name);
    }
    return null;
  }

  /**
   * Retrieve the local Group with the specified (short) name. Must be contained in this Group.
   *
   * @param groupShortName short name of the local group you are looking for.
   * @return the Group, or null if not found
   */
  @Nullable
  public Group findGroupLocal(String groupShortName) {
    if (groupShortName == null) {
      return null;
    }

    for (Group group : groups) {
      if (groupShortName.equals(group.getShortName())) {
        return group;
      }
    }

    return null;
  }

  /** Retrieve the nested Group with the specified short name. May be any level of nesting. */
  public Optional<Group> findGroupNested(String groupShortName) {
    if (groupShortName == null) {
      return Optional.empty();
    }

    Group local = this.findGroupLocal(groupShortName);
    if (local != null) {
      return Optional.of(local);
    }

    for (Group nested : groups) {
      Optional<Group> result = nested.findGroupNested(groupShortName);
      if (result.isPresent()) {
        return result;
      }
    }

    return Optional.empty();
  }

  /**
   * Look in this Group and in its nested Groups for a Variable with a String valued Attribute with the given name
   * and value.
   *
   * @param attName look for an Attribuite with this name.
   * @param attValue look for an Attribuite with this value.
   * @return the first Variable that matches, or null if none match.
   */
  @Nullable
  public Variable findVariableByAttribute(String attName, String attValue) {
    Preconditions.checkNotNull(attName);
    for (Variable v : getVariables()) {
      for (Attribute att : v.attributes())
        if (attName.equals(att.getShortName()) && attValue.equals(att.getStringValue())) {
          return v;
        }
    }
    for (Group nested : getGroups()) {
      Variable v = nested.findVariableByAttribute(attName, attValue);
      if (v != null) {
        return v;
      }
    }
    return null;
  }

  /** Find the Variable with the specified (short) name in this group, or null if not found */
  @Nullable
  public Variable findVariableLocal(String varShortName) {
    if (varShortName == null) {
      return null;
    }
    for (Variable v : variables) {
      if (varShortName.equals(v.getShortName())) {
        return v;
      }
    }
    return null;
  }

  /** Find the Variable with the specified (short) name in this group or a parent group, or null if not found */
  @Nullable
  public Variable findVariableOrInParent(String varShortName) {
    if (varShortName == null) {
      return null;
    }

    Variable v = findVariableLocal(varShortName);
    Group parent = getParentGroup();
    if ((v == null) && (parent != null)) {
      v = parent.findVariableOrInParent(varShortName);
    }
    return v;
  }

  /** Get the shared Dimensions contained directly in this group. */
  public List<Dimension> getDimensions() {
    return dimensions;
  }

  /** Get the enumerations contained directly in this group. */
  public List<EnumTypedef> getEnumTypedefs() {
    return ImmutableList.copyOf(enumTypedefs);
  }

  /**
   * Get the full name of this Group.
   * Certain characters are backslash escaped (see CdmFullNames.makeFullName(Group))
   *
   * @return full name with backslash escapes
   */
  public String getFullName() {
    return CdmFiles.makeFullName(this);
  }

  /** Get the Groups contained directly in this Group. */
  public List<Group> getGroups() {
    return groups;
  }

  /** Get the CdmFile that owns this Group. */
  public CdmFile getCdmFile() {
    return ncfile;
  }

  /** Get the parent Group, or null if its the root group. */
  @Nullable
  public Group getParentGroup() {
    return this.parentGroup;
  }

  /** Get the short name of the Group. */
  public String getShortName() {
    return shortName;
  }

  /** Get the Variables contained directly in this group. */
  public List<Variable> getVariables() {
    return variables;
  }

  /**
   * Is this a parent of the other Group?
   *
   * @param other another Group
   * @return true is it is equal or a parent
   */
  public boolean isParent(Group other) {
    while ((other != this) && (other.getParentGroup() != null)) {
      other = other.getParentGroup();
    }
    return (other == this);
  }

  /** Is this the root group? */
  public boolean isRoot() {
    return getParentGroup() == null;
  }

  /**
   * Create a dimension list using dimension names. The dimension is searched for recursively in the parent groups.
   *
   * @param dimString : whitespace separated list of dimension names, or '*' for Dimension.UNKNOWN, or number for
   *        anonomous dimension. null or empty String is a scalar.
   * @return list of dimensions.
   * @throws IllegalArgumentException if cant find dimension or parse error.
   */
  public List<Dimension> makeDimensionsList(String dimString) throws IllegalArgumentException {
    return Dimensions.makeDimensionsList(this::findDimension, dimString);
  }

  //////////////////////////////////////////////////////////////////////////////////////


  void writeCDL(Formatter out, Indent indent, boolean strict) {
    boolean hasE = (!enumTypedefs.isEmpty());
    boolean hasD = (!dimensions.isEmpty());
    boolean hasV = (!variables.isEmpty());
    // boolean hasG = (groups.size() > 0);
    boolean hasA = (!Iterables.isEmpty(attributes));

    if (hasE) {
      out.format("%stypes:%n", indent);
      indent.incr();
      for (EnumTypedef e : enumTypedefs) {
        e.writeCDL(out, indent, strict);
        out.format("%n");
      }
      indent.decr();
      out.format("%n");
    }

    if (hasD) {
      out.format("%sdimensions:%n", indent);
      indent.incr();
      for (Dimension myd : dimensions) {
        myd.writeCDL(out, indent, strict);
        out.format("%n");
      }
      indent.decr();
    }

    if (hasV) {
      out.format("%svariables:%n", indent);
      indent.incr();
      for (Variable v : variables) {
        v.writeCDL(out, indent, false, strict);
        out.format("%n");
      }
      indent.decr();
    }

    for (Group g : groups) {
      String gname = strict ? CdmFiles.makeValidCDLName(g.getShortName()) : g.getShortName();
      out.format("%sgroup: %s {%n", indent, gname);
      indent.incr();
      g.writeCDL(out, indent, strict);
      indent.decr();
      out.format("%s}%n%n", indent);
    }

    if (hasA) {
      if (isRoot()) {
        out.format("%s// global attributes:%n", indent);
      } else {
        out.format("%s// group attributes:%n", indent);
      }

      for (Attribute att : attributes) {
        // String name = strict ? CdmFile.escapeNameCDL(getShortName()) : getShortName();
        out.format("%s", indent);
        att.writeCDL(out, strict, null);
        out.format(";");
        if (!strict && (att.getArrayType() != ArrayType.STRING)) {
          out.format(" // %s", att.getArrayType().toCdl());
        }
        out.format("%n");
      }
    }
  }

  @Override
  public String toString() {
    Formatter buf = new Formatter();
    writeCDL(buf, new Indent(2, 0), false);
    return buf.toString();
  }

  @Override
  public boolean equals(Object oo) {
    if (this == oo) {
      return true;
    }
    if (!(oo instanceof Group)) {
      return false;
    }
    Group og = (Group) oo;
    if (!getShortName().equals(og.getShortName())) {
      return false;
    }
    return !((getParentGroup() != null) && !getParentGroup().equals(og.getParentGroup()));
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 37 * result + getShortName().hashCode();
    if (getParentGroup() != null) {
      result = 37 * result + getParentGroup().hashCode();
    }
    return result;
  }

  ////////////////////////////////////////////////////////////////
  private final CdmFile ncfile;
  private final ImmutableList<Variable> variables;
  private final ImmutableList<Dimension> dimensions;
  private final ImmutableList<Group> groups;
  private final AttributeContainer attributes;
  private final ImmutableList<EnumTypedef> enumTypedefs;
  private final String shortName;
  private final Group parentGroup;

  private Group(Builder builder, @Nullable Group parent) {
    this.shortName = builder.shortName;
    this.parentGroup = parent;
    this.ncfile = builder.ncfile;

    this.dimensions = ImmutableList.copyOf(builder.dimensions);
    this.enumTypedefs = ImmutableList.copyOf(builder.enumTypedefs);

    // only the root group build() should be called, the rest get called recursively
    this.groups = builder.gbuilders.stream().map(g -> g.setNcfile(this.ncfile).build(this))
        .collect(ImmutableList.toImmutableList());

    builder.vbuilders.forEach(vb -> {
      // dont override ncfile if its been set.
      if (vb.ncfile == null) {
        vb.setNcfile(this.ncfile);
      }
    });
    ImmutableList.Builder<Variable> vlistb = ImmutableList.builder();
    for (Variable.Builder<?> vb : builder.vbuilders) {
      Variable var = vb.build(this);
      vlistb.add(var);
    }
    this.variables = vlistb.build();
    this.attributes = builder.attributes.toImmutable();
  }

  /** Turn into a mutable Builder. Can use toBuilder().build() to copy. */
  public Builder toBuilder() {
    Builder builder = builder().setName(this.shortName).setNcfile(this.ncfile).addAttributes(this.attributes)
        .addDimensions(this.dimensions).addEnumTypedefs(this.enumTypedefs);

    this.groups.forEach(g -> builder.addGroup(g.toBuilder()));
    this.variables.forEach(v -> builder.addVariable(v.toBuilder()));

    return builder;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static Builder builder(String name) {
    return new Builder().setName(name);
  }

  public static class Builder {
    public String shortName = "";
    private final AttributeContainerMutable attributes = new AttributeContainerMutable("");
    public final ArrayList<Dimension> dimensions = new ArrayList<>();
    public final ArrayList<EnumTypedef> enumTypedefs = new ArrayList<>();
    public final ArrayList<Variable.Builder<?>> vbuilders = new ArrayList<>();
    public final ArrayList<Builder> gbuilders = new ArrayList<>();

    private @Nullable Builder parentGroup; // null for root group; ignored during build()
    private CdmFile ncfile; // set by CdmFile.build()
    private boolean built;

    public Builder setParentGroup(@Nullable Builder parentGroup) {
      this.parentGroup = parentGroup;
      return this;
    }

    public @Nullable Builder getParentGroup() {
      return this.parentGroup;
    }

    public Builder addAttribute(Attribute att) {
      Preconditions.checkNotNull(att);
      attributes.addAttribute(att);
      return this;
    }

    public Builder addAttributes(Iterable<Attribute> atts) {
      Preconditions.checkNotNull(atts);
      attributes.addAll(atts);
      return this;
    }

    public AttributeContainerMutable getAttributeContainer() {
      return attributes;
    }

    /** Add Dimension with error if it already exists */
    public Builder addDimension(Dimension dim) {
      Preconditions.checkNotNull(dim);
      findDimensionLocal(dim.getShortName()).ifPresent(d -> {
        throw new IllegalArgumentException("Dimension '" + d.getShortName() + "' already exists");
      });
      dimensions.add(dim);
      return this;
    }

    /**
     * Add Dimension if one with same name doesnt already exist.
     * 
     * @return true if it did not exist and was added.
     */
    public boolean addDimensionIfNotExists(Dimension dim) {
      Preconditions.checkNotNull(dim);
      if (findDimensionLocal(dim.getShortName()).isEmpty()) {
        dimensions.add(dim);
        return true;
      }
      return false;
    }

    /** Add Dimensions with error if any already exist */
    public Builder addDimensions(Collection<Dimension> dims) {
      Preconditions.checkNotNull(dims);
      dims.forEach(this::addDimension);
      return this;
    }

    /**
     * Replace dimension if it exists, else just add it.
     *
     * @return true if there was an existing dimension of that name
     */
    public boolean replaceDimension(Dimension dim) {
      Optional<Dimension> want = findDimensionLocal(dim.getShortName());
      want.ifPresent(dimensions::remove);
      addDimension(dim);
      return want.isPresent();
    }

    /**
     * Remove dimension, if it exists.
     *
     * @return true if there was an existing dimension of that name
     */
    public boolean removeDimension(String name) {
      Optional<Dimension> want = findDimensionLocal(name);
      want.ifPresent(dimensions::remove);
      return want.isPresent();
    }

    /** Find Dimension local to this Group */
    public Optional<Dimension> findDimensionLocal(String name) {
      return dimensions.stream().filter(d -> d.getShortName().equals(name)).findFirst();
    }

    /** Find Dimension in this Group or a parent Group */
    public Optional<Dimension> findDimension(String name) {
      if (name == null) {
        return Optional.empty();
      }
      Optional<Dimension> dopt = findDimensionLocal(name);
      if (dopt.isPresent()) {
        return dopt;
      }
      if (this.parentGroup != null) {
        return this.parentGroup.findDimension(name);
      }

      return Optional.empty();
    }

    /** Add a nested Group. */
    public Builder addGroup(Builder nested) {
      Preconditions.checkNotNull(nested);
      this.findGroupLocal(nested.shortName).ifPresent(g -> {
        throw new IllegalStateException("Nested group already exists " + nested.shortName);
      });
      this.gbuilders.add(nested);
      nested.setParentGroup(this);
      return this;
    }

    public Builder addGroups(Collection<Builder> groups) {
      Preconditions.checkNotNull(groups);
      this.gbuilders.addAll(groups);
      return this;
    }

    /**
     * Remove group, if it exists.
     *
     * @return true if there was an existing group of that name
     */
    public boolean removeGroup(String name) {
      Optional<Builder> want = findGroupLocal(name);
      want.ifPresent(v -> gbuilders.remove(v));
      return want.isPresent();
    }

    public Optional<Builder> findGroupLocal(String shortName) {
      return this.gbuilders.stream().filter(g -> g.shortName.equals(shortName)).findFirst();
    }

    /**
     * Find a subgroup of this Group, with the specified reletive name.
     * An embedded "/" separates group names.
     * Can have a leading "/" only if this is the root group.
     *
     * @param reletiveName eg "group/subgroup/wantGroup".
     * @return Group or empty if not found.
     */
    public Optional<Builder> findGroupNested(String reletiveName) {
      if (reletiveName == null || reletiveName.isEmpty()) {
        return (this.getParentGroup() == null) ? Optional.of(this) : Optional.empty();
      }

      Builder g = this;
      StringTokenizer stoke = new StringTokenizer(reletiveName, "/");
      while (stoke.hasMoreTokens()) {
        String groupName = CdmFiles.makeNameUnescaped(stoke.nextToken());
        Optional<Builder> sub = g.findGroupLocal(groupName);
        if (sub.isEmpty()) {
          return Optional.empty();
        }
        g = sub.get();
      }
      return Optional.of(g);
    }

    /** Is this group a parent of the other group ? */
    public boolean isParent(Builder other) {
      while ((other != this) && (other.parentGroup != null)) {
        other = other.parentGroup;
      }
      return (other == this);
    }

    /** Find the common parent with the other group ? */
    public Builder commonParent(Builder other) {
      Preconditions.checkNotNull(other);
      if (isParent(other)) {
        return this;
      }
      if (other.isParent(this)) {
        return other;
      }
      while (other != null && !other.isParent(this)) {
        other = other.parentGroup;
      }
      return other;
    }

    public Builder addEnumTypedef(EnumTypedef typedef) {
      Preconditions.checkNotNull(typedef);
      enumTypedefs.add(typedef);
      return this;
    }

    public Builder addEnumTypedefs(Collection<EnumTypedef> typedefs) {
      Preconditions.checkNotNull(typedefs);
      enumTypedefs.addAll(typedefs);
      return this;
    }

    /**
     * Add a EnumTypedef if it does not already exist.
     * Return new or existing.
     */
    public EnumTypedef findOrAddEnumTypedef(String name, Map<Integer, String> map) {
      Optional<EnumTypedef> opt = findEnumeration(name);
      if (opt.isPresent()) {
        return opt.get();
      } else {
        EnumTypedef enumTypedef = new EnumTypedef(name, map);
        addEnumTypedef(enumTypedef);
        return enumTypedef;
      }
    }

    public Optional<EnumTypedef> findEnumeration(String name) {
      return this.enumTypedefs.stream().filter(e -> e.getShortName().equals(name)).findFirst();
    }

    /** Add a Variable, throw error if one of the same name if it exists. */
    public Builder addVariable(Variable.Builder<?> variable) {
      Preconditions.checkNotNull(variable);
      findVariableLocal(variable.shortName).ifPresent(v -> {
        throw new IllegalArgumentException("Variable '" + v.shortName + "' already exists");
      });
      vbuilders.add(variable);
      variable.setParentGroupBuilder(this);
      return this;
    }

    /** Add Variables, throw error if one of the same name if it exists. */
    public Builder addVariables(Collection<Variable.Builder<?>> vars) {
      vars.forEach(this::addVariable);
      return this;
    }

    /**
     * Replace variable of same name, if it exists, else just add it.
     * 
     * @return true if there was an existing variable of that name
     */
    public boolean replaceVariable(Variable.Builder<?> vb) {
      Optional<Variable.Builder<?>> want = findVariableLocal(vb.shortName);
      want.ifPresent(v -> vbuilders.remove(v));
      addVariable(vb);
      return want.isPresent();
    }

    /**
     * Remove variable, if it exists.
     *
     * @return true if there was an existing variable of that name
     */
    public boolean removeVariable(String name) {
      Optional<Variable.Builder<?>> want = findVariableLocal(name);
      want.ifPresent(v -> vbuilders.remove(v));
      return want.isPresent();
    }

    public Optional<Variable.Builder<?>> findVariableLocal(String name) {
      return vbuilders.stream().filter(v -> v.shortName.equals(name)).findFirst();
    }

    /**
     * Find a Variable, with the specified reletive name. No structure members.
     * 
     * @param reletiveName eg "group/subgroup/varname".
     */
    public Optional<Variable.Builder<?>> findVariableNested(String reletiveName) {
      if (reletiveName == null || reletiveName.isEmpty()) {
        return Optional.empty();
      }

      // break into groupNames and varName
      Builder group = this;
      String varName = reletiveName;
      int pos = reletiveName.lastIndexOf('/');
      if (pos >= 0) {
        String groupNames = reletiveName.substring(0, pos);
        varName = reletiveName.substring(pos + 1);
        group = findGroupNested(groupNames).orElse(null);
      }

      return group == null ? Optional.empty() : group.findVariableLocal(varName);
    }

    /**
     * Find the Variable with the specified (short) name in this group or a parent group.
     *
     * @param varShortName short name of Variable.
     * @return the Variable or empty.
     */
    public Optional<Variable.Builder<?>> findVariableOrInParent(String varShortName) {
      if (varShortName == null)
        return Optional.empty();

      Optional<Variable.Builder<?>> vopt = findVariableLocal(varShortName);

      Builder parent = getParentGroup();
      if (vopt.isEmpty() && (parent != null)) {
        vopt = parent.findVariableOrInParent(varShortName);
      }
      return vopt;
    }

    // Generally ncfile is set in CdmFile.build()
    public Builder setNcfile(CdmFile ncfile) {
      this.ncfile = ncfile;
      return this;
    }

    public Builder setName(String shortName) {
      this.shortName = CdmFiles.makeValidCdmObjectName(shortName);
      return this;
    }

    @Deprecated
    public CdmFile getNcfile() {
      return this.ncfile;
    }

    /** Make list of dimensions by looking in this Group or parent groups */
    public List<Dimension> makeDimensionsList(String dimString) throws IllegalArgumentException {
      return Dimensions.makeDimensionsList(this::findDimension, dimString);
    }

    /**
     * Make the full name of the this group.
     * TODO In light of CF groups, we may have to start full names with '/'
     */
    public String makeFullName() {
      if (parentGroup == null) {
        return "";
      }
      StringBuilder sbuff = new StringBuilder();
      appendGroupName(sbuff, this);
      return sbuff.toString();
    }

    private void appendGroupName(StringBuilder sbuff, Builder g) {
      if (g == null || g.getParentGroup() == null) {
        return;
      }
      appendGroupName(sbuff, g.getParentGroup());
      sbuff.append(EscapeStrings.backslashEscape(g.shortName, reservedFullName));
      sbuff.append("/");
    }

    /** Remove the given dimension from this group and any subgroups */
    public void removeDimensionFromAllGroups(Builder group, Dimension remove) {
      group.dimensions.removeIf(dim -> dim.equals(remove));
      group.gbuilders.forEach(g -> removeDimensionFromAllGroups(g, remove));
    }

    /** Build the root group, with parent = null. */
    public Group build() {
      return build(null);
    }

    /** Normally this is called by CdmFile.build() */
    Group build(@Nullable Group parent) {
      if (built) {
        throw new IllegalStateException("Group was already built " + this.shortName);
      }
      built = true;
      return new Group(this, parent);
    }
  }
}
