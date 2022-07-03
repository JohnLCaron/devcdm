package dev.cdm.core.util;

import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import dev.cdm.core.api.Attribute;
import dev.cdm.core.api.CdmFile;
import dev.cdm.core.api.CdmFiles;
import dev.cdm.core.api.Dimension;
import dev.cdm.core.api.Group;
import dev.cdm.core.api.Structure;
import dev.cdm.core.api.Variable;
import org.jetbrains.annotations.Nullable;

// static helper classes for tree traversal and full names
public class CdmFullNames {
  public record DimensionWithGroup(Dimension dim, Group group) {}

  private final Group root;
  private final BiMap<String, Group> groups = HashBiMap.create();
  private final BiMap<String, Variable> variables = HashBiMap.create();
  private final BiMap<String, DimensionWithGroup> dimensions = HashBiMap.create();

  public CdmFullNames(CdmFile cdmFile) {
    root = cdmFile.getRootGroup();
    processGroup(root);
  }

  private void processGroup(Group group) {
    String fullName = fullName(group);
    groups.put(fullName, group);
    group.getDimensions().forEach(d -> {
      String dimName = fullName.isEmpty() ? d.getShortName() : fullName + "/" + d.getShortName();
      dimensions.put(dimName, new DimensionWithGroup(d, group));
    });

    group.getVariables().forEach(this::processVariable);
    group.getGroups().forEach(this::processGroup);
  }

  private String fullName(Group node) {
    String nodeName = EscapeStrings.backslashEscape(node.getShortName(), CdmFiles.reservedFullName);
    StringBuilder sbuff = new StringBuilder();
    appendParentName(sbuff, node.getParentGroup());
    sbuff.append(nodeName);
    return sbuff.toString();
  }

  private void appendParentName(StringBuilder sbuff, Group node) {
    if (node == null || node.getShortName().isEmpty()) { // common case?
      return;
    }
    appendParentName(sbuff, node.getParentGroup());
    sbuff.append(EscapeStrings.backslashEscape(node.getShortName(), CdmFiles.reservedFullName));
    sbuff.append("/");
  }

  private void processVariable(Variable variable) {
    variables.put(fullName(variable), variable);
    if (variable instanceof Structure structure) {
      structure.getVariables().forEach(this::processVariable);
    }
  }

  private String fullName(Variable variable) {
    String nodeName = EscapeStrings.backslashEscape(variable.getShortName(), CdmFiles.reservedFullName);
    StringBuilder sbuff = new StringBuilder();
    if (!variable.getParentGroup().isRoot()) {
      sbuff.append(groups.inverse().get(variable.getParentGroup()));
      sbuff.append("/");
    }
    appendParentName(sbuff, variable.getParentStructure());
    sbuff.append(nodeName);
    return sbuff.toString();
  }

  private void appendParentName(StringBuilder sbuff, Variable node) {
    if (node == null) {
      return;
    }
    appendParentName(sbuff, node.getParentStructure());
    sbuff.append(EscapeStrings.backslashEscape(node.getShortName(), CdmFiles.reservedFullName));
    sbuff.append(".");
  }

  /////////////////////////////////////////////////////////////////////////////////

  @Nullable
  public Group findGroup(String fullName) {
    return groups.get(fullName);
  }

  public String makeFullName(Group group) {
    return groups.inverse().get(group);
  }

  @Nullable
  public Variable findVariable(String fullName) {
    return variables.get(fullName);
  }

  public String makeFullName(Variable group) {
    return variables.inverse().get(group);
  }

  /**
   * Finds a Dimension with the specified full name. It may be nested in multiple groups. An embedded "/" is interpreted
   * as a group separator. A leading slash indicates the root group. That slash may be omitted, but the {@code fullName}
   * will be treated as if it were there. In other words, the first name token in {@code fullName} is treated as the
   * short name of a Group or Dimension, relative to the root group.
   *
   * @param fullName Dimension full name, e.g. "/group/subgroup/dim".
   * @return the Dimension and Group or {@code null} if it wasn't found.
   */
  @Nullable
  public DimensionWithGroup findDimension(String fullName) {
    return dimensions.get(fullName);
  }

  public String makeFullName(Dimension dimension, Group group) {
    return dimensions.inverse().get(new DimensionWithGroup(dimension, group));
  }

  public String makeFullName(@Nullable Group group, @Nullable Variable var, Attribute att) {
    StringBuilder sbuff = new StringBuilder();
    String groupName = groups.inverse().get(group);
    if (groupName != null && !groupName.isEmpty()) {
      sbuff.append(groupName);
      sbuff.append("/");
    }
    String varName = variables.inverse().get(var);
    if (varName != null && !varName.isEmpty()) {
      sbuff.append(varName);
    }
    sbuff.append("@");
    sbuff.append(att.getName());
    return sbuff.toString();
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
   *                        "/group/subgroup/@attName", "/group/subgroup/var@attName", or "/group/subgroup/struct.member@attName"
   * @return Attribute or null if not found.
   */
  @Nullable
  public Attribute findAttribute(String fullNameEscaped) {
    if (Strings.isNullOrEmpty(fullNameEscaped)) {
      return null;
    }

    int posAtt = fullNameEscaped.indexOf('@');
    if (posAtt < 0) {
      return root.findAttribute(fullNameEscaped);
    }
    if (posAtt == 0) {
      return root.findAttribute(fullNameEscaped.substring(1));
    }
    if (posAtt == fullNameEscaped.length() - 1) {
      return null;
    }

    String path = fullNameEscaped.substring(0, posAtt);
    String attName = fullNameEscaped.substring(posAtt + 1);
    Variable v = findVariable(path);
    if (v == null) {
      return null;
    }
    return v.findAttribute(attName);
  }

}
