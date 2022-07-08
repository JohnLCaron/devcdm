/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.dataset.transform.vertical;

import dev.cdm.core.api.AttributeContainer;
import dev.cdm.core.constants.CDM;
import dev.cdm.core.constants.CF;
import dev.cdm.dataset.api.CoordinateSystem;
import dev.cdm.dataset.api.CdmDataset;
import dev.cdm.dataset.api.CoordinateTransform;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;

/** Factory for Vertical Coordinate Transforms. */
public class VerticalTransformFactory {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(VerticalTransformFactory.class);
  private static final List<Transform> transformList = new ArrayList<>();
  private static boolean userMode = false;

  private static final boolean loadWarnings = false;

  // search in the order added
  static {
    // CF
    registerTransform(CF.atmosphere_hybrid_sigma_pressure_coordinate, AtmosHybridSigmaPressure.Builder.class);
    registerTransform(CF.atmosphere_sigma_coordinate, AtmosSigma.Builder.class);
    registerTransform(CF.ocean_sigma_coordinate, OceanSigma.Builder.class);
    registerTransform(CF.ocean_s_coordinate, OceanS.Builder.class);
    registerTransform(CF.ocean_s_coordinate_g1, OceanSG1.Builder.class);
    registerTransform(CF.ocean_s_coordinate_g2, OceanSG2.Builder.class);

    // CSM
    registerTransform(CsmHybridSigmaBuilder.transform_name, CsmHybridSigmaBuilder.class);
    registerTransform("hybrid_sigma_pressure", CsmHybridSigmaBuilder.class); // alias

    // WRF
    registerTransform(WrfEta.WRF_ETA_COORDINATE, WrfEta.Builder.class);

    // Misc
    registerTransform(ExistingFieldVerticalTransform.transform_name, ExistingFieldVerticalTransform.Builder.class);

    // further calls to registerTransform are by the user
    userMode = true;
  }

  /**
   * Register a class that implements a Coordinate Transform.
   * 
   * @param transformName name of transform. This name is used in the datasets to identify the transform, eg CF names.
   * @param c class that implements CoordTransBuilderIF.
   */
  public static void registerTransform(String transformName, Class<?> c) {
    if (!(VerticalTransform.Builder.class.isAssignableFrom(c)))
      throw new IllegalArgumentException("Class " + c.getName() + " must implement VerticalTransform");

    // fail fast - check newInstance works
    try {
      c.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      throw new IllegalArgumentException("VerticalTransform failed for " + c.getName(), e);
    }

    // user stuff gets put at top
    if (userMode)
      transformList.add(0, new Transform(transformName, c));
    else
      transformList.add(new Transform(transformName, c));
  }

  private static class Transform {
    final String transName;
    final Class<?> transClass;

    Transform(String transName, Class<?> transClass) {
      this.transName = transName;
      this.transClass = transClass;
    }
  }

  public static boolean hasVerticalTransformFor(String transformName) {
    return transformList.stream().anyMatch(it -> it.transName.equals(transformName));
  }

    /**
     * Does this AttributeContainer contain metadata we can make a VerticalTransform from?
     * Return empty if not, else return transform name.
     */
  public static Optional<String> hasVerticalTransformFor(AttributeContainer ctv) {
    // standard name
    String transform_name = ctv.findAttributeString(CDM.TRANSFORM_NAME, null);
    if (null == transform_name) {
      transform_name = ctv.findAttributeString("Projection_Name", null);
    }

    // these names are from CF - dont want to have to duplicate
    if (null == transform_name) {
      transform_name = ctv.findAttributeString(CF.GRID_MAPPING_NAME, null);
    }
    if (null == transform_name) {
      transform_name = ctv.findAttributeString(CF.STANDARD_NAME, null);
    }

    // Finally check the units
    if (null == transform_name) {
      transform_name = ctv.findAttributeString(CDM.UNITS, null);
    }

    if (null == transform_name) {
      return Optional.empty();
    }

    transform_name = transform_name.trim();

    // do we have a transform registered for this ?
    for (Transform transform : transformList) {
      if (transform.transName.equalsIgnoreCase(transform_name)) {
        return Optional.of(transform_name);
      }
    }
    return Optional.empty();
  }

  /**
   * LOOK can we remove?
   * Make a CoordinateTransform object from the parameters in a Coordinate Transform Variable, using an intrinsic or
   * registered CoordTransBuilder.
   * 
   * @param ctv the Coordinate Transform Variable - container for the transform parameters
   * @param errlog pass back error information.
   * @return VerticalTransform, or empty if failure.
   */
  public static Optional<VerticalTransform> makeVerticalTransform(String transform_name, CdmDataset ds,
      CoordinateSystem csys, AttributeContainer ctv, Formatter errlog) {

    // do we have a transform registered for this ?
    Class<?> builderClass = null;
    for (Transform transform : transformList) {
      if (transform.transName.equals(transform_name)) {
        builderClass = transform.transClass;
        break;
      }
    }
    if (null == builderClass) {
      errlog.format("**Failed to find VerticalTransform name= %s from Variable= %s%n", transform_name, ctv);
      return Optional.empty();
    }

    // get an instance of that class
    Object builderObject;
    try {
      builderObject = builderClass.getDeclaredConstructor().newInstance();
    } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      log.error("Cant create new instance " + builderClass.getName(), e);
      return Optional.empty();
    }

    Optional<VerticalTransform> vt;
    if (builderObject instanceof VerticalTransform.Builder) {
      VerticalTransform.Builder vertBuilder = (VerticalTransform.Builder) builderObject;
      vt = vertBuilder.create(ds, csys, ctv, errlog);

    } else {
      errlog.format(" Failed to make Coordinate transform %s from variable %s: %s%n", transform_name, ctv.getName(),
          builderObject.getClass().getName());
      return Optional.empty();
    }

    if (vt.isEmpty()) {
      errlog.format(" Failed to make Coordinate transform %s from variable %s: %s%n", transform_name, ctv.getName(),
          builderObject.getClass().getName());
      return Optional.empty();
    }

    return vt;
  }

  /**
   * Make a CoordinateTransform object from the parameters in a Coordinate Transform Variable,
   *
   * @param vertCtv the Coordinate Transform Variable - container for the transform parameters
   * @return CoordinateTransform, or null if failure.
   */
  @Nullable
  public static VerticalTransform makeTransform(CdmDataset ds, CoordinateSystem csys, CoordinateTransform vertCtv) {

    Class<?> builderClass = null;
    for (Transform transform : transformList) {
      if (transform.transName.equals(vertCtv.name())) {
        builderClass = transform.transClass;
        break;
      }
    }
    if (null == builderClass) {
      AttributeContainer ctv = vertCtv.metadata();
        // standard name
        String transform_name = ctv.findAttributeString(CDM.TRANSFORM_NAME, null);
        if (null == transform_name) {
          transform_name = ctv.findAttributeString("Projection_Name", null);
        }

        // these names are from CF - dont want to have to duplicate
        if (null == transform_name) {
          transform_name = ctv.findAttributeString(CF.GRID_MAPPING_NAME, null);
        }
        if (null == transform_name) {
          transform_name = ctv.findAttributeString(CF.STANDARD_NAME, null);
        }

        // Finally check the units
        if (null == transform_name) {
          transform_name = ctv.findAttributeString(CDM.UNITS, null);
        }

        if (null == transform_name) {
          return null;
        }

        transform_name = transform_name.trim();

        // do we have a transform registered for this ?
        for (Transform transform : transformList) {
          if (transform.transName.equalsIgnoreCase(transform_name)) {
            builderClass = transform.transClass;
            break;
          }
        }
    }

    if (null == builderClass) {
      return null;
    }

    // get an instance of that class
    Object builderObject;
    try {
      builderObject = builderClass.getDeclaredConstructor().newInstance();
    } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      log.error("Cant create new instance " + builderClass.getName(), e);
      return null;
    }

    Formatter errlog = new Formatter();
    Optional<VerticalTransform> vt;
    if (builderObject instanceof VerticalTransform.Builder) {
      VerticalTransform.Builder vertBuilder = (VerticalTransform.Builder) builderObject;
      vt = vertBuilder.create(ds, csys, vertCtv.metadata(), errlog);
    } else {
      log.error("Not instanceof VerticalTransform.Builder: {}", builderClass.getName());
      return null;
    }

    if (vt.isEmpty()) {
      log.error(" Failed to make Coordinate transform from ctv {} {} errs = {}", vertCtv,
              builderObject.getClass().getName(), errlog);
      return null;
    }

    return vt.get();
  }

}
