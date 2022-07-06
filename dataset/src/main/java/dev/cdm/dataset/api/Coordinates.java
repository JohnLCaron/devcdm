package dev.cdm.dataset.api;

import java.util.List;

public interface Coordinates {
  String getConventionName();
  List<CoordinateAxis> getCoordinateAxes();
  List<CoordinateTransform> getCoordinateTransforms();
  List<CoordinateSystem> getCoordinateSystems();
  List<CoordinateSystem> makeCoordinateSystemsFor(VariableDS vds);
}
