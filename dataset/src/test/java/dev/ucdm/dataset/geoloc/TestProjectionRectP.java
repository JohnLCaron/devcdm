/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.dataset.geoloc;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link ProjectionRect} */
public class TestProjectionRectP {

  private double x1, x2, y1, y2;
  private ProjectionRect projectionRect;

  public static Stream<Arguments> params() {
    return Stream.of(
            Arguments.of(-1, -1, 1, 1),
            Arguments.of(1, 1, -1, -1),
            Arguments.of(-1, 1, 1, -1),
            Arguments.of(1, -1, -1, 1));
  }

  @ParameterizedTest
  @MethodSource("params")
  public void testProjectionRectP(double x1, double y1, double x2, double y2) {
    this.x1 = x1;
    this.x2 = x2;
    this.y1 = y1;
    this.y2 = y2;
    this.projectionRect = new ProjectionRect(x1, y1, x2, y2);

    testGetX();
    testGetY();
    testGetWidth1();
    testGetHeight1();
    testGetWidth2();
    testGetHeight2();
    testGetLowerLeftPoint();
    testGetUpperRightPoint();
    testSetX();
    testSetY();
    testSetWidth();
    testSetHeight();
    testContainsPoint();
    testContainsRect();
  }

  void testGetX() {
    // getX() should give the x value for the upper left corner
    double getX = projectionRect.getMinX();
    double getMinX = projectionRect.getMinX();
    double getMaxX = projectionRect.getMaxX();

    assertThat(getX).isEqualTo(getMinX);
    assertThat(getX).isNotEqualTo(getMaxX);
  }

  void testGetY() {
    // getX() should give the y value for the upper left corner
    double getY = projectionRect.getMinY();
    double getMinY = projectionRect.getMinY();
    double getMaxY = projectionRect.getMaxY();

    assertThat(getY).isNotEqualTo(getMaxY);
    assertThat(getY).isEqualTo(getMinY);
  }

  void testGetWidth1() {
    // getX() should give the y value for the upper left corner
    double minX = projectionRect.getMinX();
    double maxX = projectionRect.getMaxX();
    double testWidth = maxX - minX;
    double width = projectionRect.getWidth();
    assertThat(testWidth).isEqualTo(width);
  }

  void testGetHeight1() {
    // getX() should give the y value for the upper left corner
    double minY = projectionRect.getMinY();
    double maxY = projectionRect.getMaxY();
    double testHeight = maxY - minY;
    double height = projectionRect.getHeight();
    assertThat(testHeight).isEqualTo(height);
  }

  void testGetWidth2() {
    // getX() should give the y value for the upper left corner
    double minX = projectionRect.getMinX();
    double maxX = projectionRect.getMaxX();
    double testWidth = maxX - minX;
    double width = projectionRect.getWidth();
    assertThat(testWidth).isEqualTo(width);

  }

  void testGetHeight2() {
    // getX() should give the y value for the upper left corner
    double minY = projectionRect.getMinY();
    double maxY = projectionRect.getMaxY();
    double testHeight = maxY - minY;
    double height = projectionRect.getHeight();
    assertThat(testHeight).isEqualTo(height);
  }

  void testGetLowerLeftPoint() {
    ProjectionPoint getllp = projectionRect.getLowerLeftPoint();
    double llx = projectionRect.getMinX();
    double lly = projectionRect.getMinY();
    double urx = projectionRect.getMaxX();
    double ury = projectionRect.getMaxY();

    assertThat(llx).isEqualTo(getllp.getX());
    assertThat(lly).isEqualTo(getllp.getY());
    assertThat(urx).isNotEqualTo(getllp.getX());
    assertThat(ury).isNotEqualTo(getllp.getY());
  }

  void testGetUpperRightPoint() {
    ProjectionPoint geturp = projectionRect.getUpperRightPoint();

    double llx = projectionRect.getMinX();
    double lly = projectionRect.getMinY();
    double urx = projectionRect.getMaxX();
    double ury = projectionRect.getMaxY();

    assertThat(urx).isEqualTo(geturp.getX());
    assertThat(ury).isEqualTo(geturp.getY());
    assertThat(llx).isNotEqualTo(geturp.getX());
    assertThat(lly).isNotEqualTo(geturp.getY());
  }

  void testSetX() {
    double x = projectionRect.getMinX();
    double x2 = x * x + 1d;
    ProjectionRect test = projectionRect.toBuilder().setX(x2).build();

    assertThat(x2).isEqualTo(test.getMinX());
    assertThat(x).isNotEqualTo(x2);
  }

  void testSetY() {
    double y = projectionRect.getMinY();
    double y2 = y * y + 1d;
    ProjectionRect test = projectionRect.toBuilder().setY(y2).build();

    assertThat(y2).isEqualTo(test.getMinY());
    assertThat(y).isNotEqualTo(y2);
  }

  void testSetWidth() {
    double width = projectionRect.getWidth();
    double width2 = width + 10d;
    ProjectionRect test = projectionRect.toBuilder().setWidth(width2).build();

    assertThat(width2).isEqualTo(test.getWidth());
    assertThat(width).isNotEqualTo(width2);
  }

  void testSetHeight() {
    double height = projectionRect.getHeight();
    double height2 = height + 10d;
    ProjectionRect test = projectionRect.toBuilder().setHeight(height2).build();

    assertThat(height2).isEqualTo(test.getHeight());
    assertThat(height).isNotEqualTo(height2);
  }

  void testContainsPoint() {
    // contains the center point? -> YES
    assertThat(
            projectionRect.contains(ProjectionPoint.create(projectionRect.getCenterX(), projectionRect.getCenterY())));
    // contains a point outside the rectangle? -> NO
    assertThat(!projectionRect.contains(ProjectionPoint.create((projectionRect.getMinX() - projectionRect.getWidth()),
            (projectionRect.getMinY() - projectionRect.getHeight()))));
    assertThat(!projectionRect.contains(ProjectionPoint.create((projectionRect.getMaxX() + projectionRect.getWidth()),
            (projectionRect.getMaxY() + projectionRect.getHeight()))));
    // contains a point on the rectangle border -> YES
    assertThat(projectionRect.contains(projectionRect.getMinPoint()));
  }

  private ProjectionRect scaleShiftRect(double scaleFactor, double deltaX, double deltaY) {
    // quick and dirty method to scale and shift a rectangle, based on projectionRect
    double centerX = projectionRect.getCenterX();
    double centerY = projectionRect.getCenterY();
    double width = projectionRect.getWidth();
    double height = projectionRect.getHeight();

    double testMinX = (centerX + deltaX) - scaleFactor * (width / 2);
    double testMinY = (centerY + deltaY) - scaleFactor * (height / 2);

    return new ProjectionRect(ProjectionPoint.create(testMinX, testMinY), scaleFactor * width, scaleFactor * height);
  }

  void testContainsRect() {
    // contains a bigger rect? -> NO
    assertThat(!projectionRect.contains(scaleShiftRect(2.0, 0, 0)));
    // contains a smaller rect? -> YES
    assertThat(projectionRect.contains(scaleShiftRect(0.5, 0, 0)));
    // contains the same rect? -> YES
    assertThat(projectionRect.contains(scaleShiftRect(1.0, 0, 0)));

    // contains a bigger rect, offset by 0.1? -> NO
    assertThat(!projectionRect.contains(scaleShiftRect(2.0, 0.1, 0.1)));
    // contains a smaller rect, offset by 0.1? -> YES
    assertThat(projectionRect.contains(scaleShiftRect(0.5, 0.1, 0.1)));
    // contain the same rect, offset by 0.1? -> NO
    assertThat(!projectionRect.contains(scaleShiftRect(1.0, 0.1, 0.1)));
  }

}
