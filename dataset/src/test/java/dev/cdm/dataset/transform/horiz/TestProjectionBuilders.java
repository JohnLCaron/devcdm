package dev.cdm.dataset.transform.horiz;

import dev.cdm.dataset.api.CoordinateTransform;
import dev.cdm.dataset.geoloc.Projection;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

public class TestProjectionBuilders {

  private static Stream<Arguments> params() {
    return ProjectionFactory.transforms.stream().map(t -> Arguments.of(Named.of(t.transName, t)));
  }

  @ParameterizedTest
  @MethodSource("params")
  public void testSubsetLon(ProjectionFactory.Transform transform) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {

    Class<? extends ProjectionBuilder> projBuilderClass = transform.transClass;
    ProjectionBuilder projBuilder = projBuilderClass.getDeclaredConstructor().newInstance();

    Class<? extends Projection> projClass = projBuilder.getProjectionClass();
    Projection proj = projClass.getDeclaredConstructor().newInstance();

    CoordinateTransform projCT = new CoordinateTransform(proj.getName(), proj.getProjectionAttributes(), true);

    assertWithMessage(projCT.toString()).that(ProjectionFactory.hasProjectionFor(projCT)).isTrue();

    Projection roundtrip = projBuilder.makeProjection(projCT.metadata(), projCT.getXYunits());
    if (!roundtrip.equals(proj)) {
      System.out.printf("HEY");
      roundtrip.equals(proj);
    }
    assertThat(roundtrip).isEqualTo(proj);
  }

}
