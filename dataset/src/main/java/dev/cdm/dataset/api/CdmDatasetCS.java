package dev.cdm.dataset.api;

import dev.cdm.core.api.CdmFile;
import dev.cdm.core.api.Dimension;
import dev.cdm.core.api.Group;
import dev.cdm.core.constants.AxisType;
import dev.cdm.core.constants._Coordinate;
import dev.cdm.dataset.coordsysbuild.CoordsHelperBuilder;

import java.util.List;

/** A CdmDataset with Coordinate Systems */
public class CdmDatasetCS extends CdmDataset {

  /**
   * Get the list of all CoordinateSystem objects used by this dataset.
   *
   * @return list of type CoordinateSystem; may be empty, not null.
   */
  public List<CoordinateSystem> getCoordinateSystems() {
    return coords.getCoordinateSystems();
  }

  public List<CoordinateSystem> makeCoordinateSystemsFor(VariableDS vds) {
    return coords.makeCoordinateSystemsFor(vds);
  }

  public List<CoordinateTransform> getCoordinateTransforms() {
    return coords.getCoordinateTransforms();
  }

  /**
   * Get the list of all CoordinateAxis used by this dataset.
   *
   * @return list of type CoordinateAxis; may be empty, not null.
   */
  public List<CoordinateAxis> getCoordinateAxes() {
    return coords.getCoordinateAxes();
  }

  /**
   * Retrieve the CoordinateAxis with the specified Axis Type.
   *
   * @param type axis type
   * @return the first CoordinateAxis that has that type, or null if not found
   */
  public CoordinateAxis findCoordinateAxis(AxisType type) {
    if (type == null)
      return null;
    for (CoordinateAxis v : coords.getCoordinateAxes()) {
      if (type == v.getAxisType())
        return v;
    }
    return null;
  }

  /**
   * Retrieve the CoordinateAxis with the specified fullName.
   *
   * @param fullName full escaped name of the coordinate axis
   * @return the CoordinateAxis, or null if not found
   */
  public CoordinateAxis findCoordinateAxis(String fullName) {
    if (fullName == null)
      return null;
    for (CoordinateAxis v : coords.getCoordinateAxes()) {
      if (fullName.equals(v.getFullName()))
        return v;
    }
    return null;
  }

  /**
   * Retrieve the CoordinateSystem with the specified name.
   *
   * @param name String which identifies the desired CoordinateSystem
   * @return the CoordinateSystem, or null if not found
   */
  public CoordinateSystem findCoordinateSystem(String name) {
    if (name == null)
      return null;
    for (CoordinateSystem v : coords.getCoordinateSystems()) {
      if (name.equals(v.getName()))
        return v;
    }
    return null;
  }


  /** Return true if axis is 1D with a unique dimension. */
  public boolean isIndependentCoordinate(CoordinateAxis axis) {
    if (axis.isCoordinateVariable()) {
      return true;
    }
    if (axis.getRank() != 1) {
      return false;
    }
    if (axis.attributes().hasAttribute(_Coordinate.AliasForDimension)) {
      return true;
    }
    Dimension dim = axis.getDimension(0);
    for (CoordinateAxis other : getCoordinateAxes()) {
      if (other == axis) {
        continue;
      }
      for (Dimension odim : other.getDimensions()) {
        if (dim.equals(odim)) {
          return false;
        }
      }
    }
    return true;
  }
  @Override
  public Object sendIospMessage(Object message) {
    if (message == IOSP_MESSAGE_GET_COORDS_HELPER) {
      return coords;
    }
    return super.sendIospMessage(message);
  }

  public String getConventionName() {
    return coords.getConventionName();
  }


  ////////////////////////////////////////////////////////////////////////////////////////////
  private final Coordinates coords;

  private CdmDatasetCS(CdmDatasetCS.Builder<?> builder) {
    super(builder);
    if (builder.coords != null) {
      this.coords = builder.coords.build(this);
    } else {
      // there are no coordinates
      this.coords = new CoordsHelperBuilder("NoCoords").build(this);
    }
  }

  public CdmDatasetCS.Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  private CdmDatasetCS.Builder<?> addLocalFieldsToBuilder(Builder<? extends CdmDataset.Builder<?>> b) {
    if (b.coords == null) {
      b.coords = new CoordsHelperBuilder(coords.getConventionName());
    }
    this.coords.getCoordinateAxes().forEach(axis -> b.coords.addCoordinateAxis(axis.toBuilder()));
    this.coords.getCoordinateSystems().forEach(sys -> b.coords.addCoordinateSystem(sys.toBuilder()));
    this.coords.getCoordinateTransforms().forEach(ct -> b.coords.addCoordinateTransform(ct));

    return (CdmDatasetCS.Builder<?>) super.addLocalFieldsToBuilder(b);
  }

  public static Builder<?> builder() {
    return new Builder2();
  }

  private static class Builder2 extends Builder<Builder2> {
    @Override
    protected Builder2 self() {
      return this;
    }
  }

  public static abstract class Builder<T extends Builder<T>> extends CdmDataset.Builder<T> {
    public CoordsHelperBuilder coords;
    private boolean built;

    protected abstract T self();

    public T setCoordsHelper(CoordsHelperBuilder coords) {
      this.coords = coords;
      return self();
    }

    /**
     * Add a CoordinateAxis to the dataset coordinates and to the list of variables.
     * Replaces any existing Variable and CoordinateAxis with the same name.
     */
    public void replaceCoordinateAxis(Group.Builder group, CoordinateAxis.Builder<?> axis) {
      if (axis == null)
        return;
      if (coords != null) {
        coords.replaceCoordinateAxis(axis);
      }
      group.replaceVariable(axis);
      axis.setParentGroupBuilder(group);
    }

    /**
     * Copy metadata from orgFile. Do not copy the coordinates, etc
     */
    public T copyFrom(CdmFile orgFile) {
      setLocation(orgFile.getLocation());
      setId(orgFile.getId());
      setTitle(orgFile.getTitle());

      Group.Builder root = Group.builder().setName("");
      convertGroup(root, orgFile.getRootGroup());
      setRootGroup(root);

      return self();
    }

    public CdmDatasetCS build() {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new CdmDatasetCS(this);
    }
  }
}
