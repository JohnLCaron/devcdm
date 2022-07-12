/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.dataset.api;

import com.google.common.collect.ImmutableSet;
import dev.ucdm.core.api.*;
import dev.ucdm.dataset.internal.EnhanceScaleMissingUnsigned;

import org.jetbrains.annotations.Nullable;
import dev.ucdm.array.Immutable;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.Set;

/**
 * <p>
 * An "enhanced" CdmFile, adding standard attribute parsing such as scale and offset.
 * Support for Coordinate Systems is in CdmDatasetCS.
 * </p>
 *
 * <p>
 * Be sure to close the dataset when done.
 * Using statics in {@code CdmDatasets}, best practice is to use try-with-resource:
 * </p>
 * 
 * <pre>
 * try (CdmDataset ncd = CdmDatasets.openDataset(fileName)) {
 *   ...
 * }
 * </pre>
 *
 * <p>
 * By default @code CdmDataset} is opened with all enhancements turned on. The default "enhance
 * mode" can be set through setDefaultEnhanceMode(). One can also explicitly set the enhancements
 * you want in the dataset factory methods. The enhancements are:
 * </p>
 *
 * <ul>
 * <li>ConvertEnums: convert enum values to their corresponding Strings. If you want to do this manually,
 * you can call Variable.lookupEnumString().</li>
 * <li>ConvertUnsigned: reinterpret the bit patterns of any negative values as unsigned.</li>
 * <li>ApplyScaleOffset: process scale/offset attributes, and automatically convert the data.</li>
 * <li>ConvertMissing: replace missing data with NaNs, for efficiency.</li>
 * </ul>
 *
 * <p>
 * Automatic scale/offset processing has some overhead that you may not want to incur up-front. If so, open the
 * CdmDataset without {@code ApplyScaleOffset}. The VariableDS data type is not promoted and the data is not
 * converted on a read, but you can call the convertScaleOffset() routines to do the conversion later.
 * </p>
 */
@Immutable
public class CdmDataset extends CdmFile {
  public static final String IOSP_MESSAGE_GET_REFERENCED_FILE = "REFERENCED_FILE";
  public static final String IOSP_MESSAGE_GET_COORDS_HELPER = "COORDS_HELPER";

  /** Possible enhancements for a CdmDataset */
  public enum Enhance {
    /** Convert enums to Strings. */
    ConvertEnums,
    /**
     * Convert unsigned values to signed values.
     * For {@link dev.ucdm.core.constants.CDM#UNSIGNED} variables, reinterpret the bit patterns of any
     * negative values as unsigned. The result will be positive values that must be stored in a
     * {@link EnhanceScaleMissingUnsigned#nextLarger larger data type}.
     */
    ConvertUnsigned,
    /** Apply scale and offset to values, promoting the data type if needed. */
    ApplyScaleOffset,
    /**
     * Replace {@link EnhanceScaleMissingUnsigned#isMissing missing} data with NaNs, for efficiency. Note that if the
     * enhanced data type is not {@code FLOAT} or {@code DOUBLE}, this has no effect.
     */
    ConvertMissing,
  }

  private static final Set<Enhance> EnhanceAll = Collections.unmodifiableSet(EnumSet.of(Enhance.ConvertEnums,
      Enhance.ConvertUnsigned, Enhance.ApplyScaleOffset, Enhance.ConvertMissing));
  private static final Set<Enhance> EnhanceNone = Collections.unmodifiableSet(EnumSet.noneOf(Enhance.class));
  private static Set<Enhance> defaultEnhanceMode = EnhanceAll;

  /** The set of all enhancements. */
  public static Set<Enhance> getEnhanceAll() {
    return EnhanceAll;
  }

  /** The set of no enhancements. */
  public static Set<Enhance> getEnhanceNone() {
    return EnhanceNone;
  }

  /** The set of default enhancements. */
  public static Set<Enhance> getDefaultEnhanceMode() {
    return defaultEnhanceMode;
  }

  /**
   * Set the default set of Enhancements to do for all subsequent dataset opens and acquires.
   * 
   * @param mode the default set of Enhancements for open and acquire factory methods
   */
  public static void setDefaultEnhanceMode(Set<Enhance> mode) {
    defaultEnhanceMode = Collections.unmodifiableSet(mode);
  }

  public static final boolean fillValueIsMissing = true;
  public static final boolean invalidDataIsMissing = true;
  public static final boolean missingDataIsMissing = true;

  ////////////////////////////////////////////////////////////////////////////////////

  /**
   * Get conventions used to analyse coordinate systems.
   *
   * @return conventions used to analyse coordinate systems
   */
  public String getConventionBuilder() {
    return convUsed;
  }

  /**
   * Get the current state of dataset enhancement.
   *
   * @return the current state of dataset enhancement.
   */
  public Set<Enhance> getEnhanceMode() {
    return enhanceMode;
  }

  @Override
  public Object sendIospMessage(Object message) {
    if (message == IOSP_MESSAGE_GET_IOSP) {
      return (orgFile == null) ? null : orgFile.sendIospMessage(message);
    }
    if (message == IOSP_MESSAGE_GET_REFERENCED_FILE) {
      return orgFile;
    }
    return super.sendIospMessage(message);
  }

  /**
   * Close all resources (files, sockets, etc) associated with this dataset.
   * If the underlying file was acquired, it will be released, otherwise closed.
   */
  @Override
  public synchronized void close() throws IOException {
    if (!wasClosed && orgFile != null) {
      orgFile.close();
    }
    wasClosed = true;
  }

  private boolean wasClosed = false;

  @Override
  public long getLastModified() {
    return (orgFile != null) ? orgFile.getLastModified() : 0;
  }

  ////////////////////////////////////////////////////////////////////
  // debugging

  /** Show debug / underlying implementation details */
  @Override
  public void getDetailInfo(Formatter f) {
    f.format("CdmDataset location= %s%n", getLocation());
    f.format("  title= %s%n", getTitle());
    f.format("  id= %s%n", getId());
    f.format("  fileType= %s%n", getCdmFileTypeId());
    f.format("  fileDesc= %s%n", getCdmFileTypeDescription());

    f.format("  class= %s%n", getClass().getName());

    if (orgFile == null) {
      f.format("  has no referenced CdmFile%n");
    } else {
      f.format("%nReferenced File:%n");
      f.format("%s", orgFile.getDetailInfo());
    }
  }

  @Override
  @Nullable
  public String getCdmFileTypeId() {
    String inner = null;
    if (orgFile != null) {
      inner = orgFile.getCdmFileTypeId();
    }
    if (this.fileTypeId == null) {
      return inner;
    }
    if (inner == null) {
      return this.fileTypeId;
    }
    return (inner.startsWith(this.fileTypeId)) ? inner : this.fileTypeId + "/" + inner;
  }

  @Override
  public String getCdmFileTypeDescription() {
    if (orgFile != null)
      return orgFile.getCdmFileTypeDescription();
    return "N/A";
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  private final @Nullable CdmFile orgFile; // can be null in NcML
  private final @Nullable String convUsed;
  private final ImmutableSet<Enhance> enhanceMode; // enhancement mode for this specific dataset
  private final @Nullable String fileTypeId;

  protected CdmDataset(Builder<?> builder) {
    super(builder);
    this.orgFile = builder.orgFile;
    this.fileTypeId = builder.fileTypeId;
    this.convUsed = builder.convUsed;
    this.enhanceMode = ImmutableSet.copyOf(builder.getEnhanceMode());
  }

  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  private Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> b) {
    b.setOrgFile(this.orgFile).setConventionUsed(this.convUsed).setEnhanceMode(this.enhanceMode)
        .setFileTypeId(this.fileTypeId);

    return (Builder<?>) super.addLocalFieldsToBuilder(b);
  }

  /** Get Builder for CdmDataset. */
  public static Builder<?> builder() {
    return new Builder2();
  }

  private static class Builder2 extends Builder<Builder2> {
    @Override
    protected Builder2 self() {
      return this;
    }
  }

  public static abstract class Builder<T extends Builder<T>> extends CdmFile.Builder<T> {
    @Nullable
    public CdmFile orgFile;
    private String convUsed;
    private Set<Enhance> enhanceMode = EnumSet.noneOf(Enhance.class);
    private String fileTypeId;
    private boolean built;

    protected abstract T self();

    public T setOrgFile(CdmFile orgFile) {
      this.orgFile = orgFile;
      return self();
    }

    public T setFileTypeId(String fileTypeId) {
      this.fileTypeId = fileTypeId;
      return self();
    }

    public T setConventionUsed(String convUsed) {
      this.convUsed = convUsed;
      return self();
    }

    public T setEnhanceMode(Set<Enhance> enhanceMode) {
      this.enhanceMode = enhanceMode;
      return self();
    }

    public Set<Enhance> getEnhanceMode() {
      return this.enhanceMode;
    }

    public void addEnhanceModes(Set<Enhance> addEnhanceModes) {
      ImmutableSet.Builder<Enhance> result = new ImmutableSet.Builder<>();
      result.addAll(this.enhanceMode);
      result.addAll(addEnhanceModes);
      this.enhanceMode = result.build();
    }

    /** Copy metadata from orgFile. Do not copy the coordinates, etc */
    public T copyFrom(CdmFile orgFile) {
      setLocation(orgFile.getLocation());
      setId(orgFile.getId());
      setTitle(orgFile.getTitle());

      Group.Builder root = Group.builder().setName("");
      convertGroup(root, orgFile.getRootGroup());
      setRootGroup(root);

      return self();
    }

    protected void convertGroup(Group.Builder g, Group from) {
      g.setName(from.getShortName());

      g.addEnumTypedefs(from.getEnumTypedefs()); // copy

      for (Dimension d : from.getDimensions()) {
        g.addDimension(d);
      }

      g.addAttributes(from.attributes()); // copy

      for (Variable v : from.getVariables()) {
        g.addVariable(convertVariable(g, v)); // convert
      }

      for (Group nested : from.getGroups()) {
        Group.Builder nnested = Group.builder();
        g.addGroup(nnested);
        convertGroup(nnested, nested); // convert
      }
    }

    private Variable.Builder<?> convertVariable(Group.Builder g, Variable v) {
      Variable.Builder<?> newVar;
      if (v instanceof Sequence) {
        newVar = SequenceDS.builder().copyFrom((Sequence) v);
      } else if (v instanceof Structure) {
        newVar = StructureDS.builder().copyFrom((Structure) v);
      } else {
        newVar = VariableDS.builder().copyFrom(v);
      }
      newVar.setParentGroupBuilder(g);
      return newVar;
    }

    public CdmDataset build() {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new CdmDataset(this);
    }
  }

}
