/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.core.constants;

import dev.ucdm.core.api.AttributeContainerMutable;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/** Test {@link dev.ucdm.core.constants.CF} */
public class TestCF {

  @Test
  public void testFeatureConversion() {
    for (dev.ucdm.core.constants.FeatureType ft : dev.ucdm.core.constants.FeatureType.values()) {
      CF.FeatureType cff = CF.FeatureType.convert(ft);
      if (cff != null) {
        dev.ucdm.core.constants.FeatureType ft2 = CF.FeatureType.convert(cff);
        assertThat(ft).isEqualTo(ft2);
      }
    }

    for (CF.FeatureType cff : CF.FeatureType.values()) {
      dev.ucdm.core.constants.FeatureType ft = CF.FeatureType.convert(cff);
      if (ft != null) {
        CF.FeatureType cff2 = CF.FeatureType.convert(ft);
        assertThat(cff).isEqualTo(cff2);
      }
    }
  }

  @Test
  public void testGetFeatureType() {
    for (CF.FeatureType cff : CF.FeatureType.values()) {
      CF.FeatureType cff2 = CF.FeatureType.getFeatureType(cff.toString());
      assertWithMessage(cff.toString()).that(cff2).isNotNull();
      assertThat(cff).isEqualTo(cff2);
    }

    // case insensitive
    for (CF.FeatureType cff : CF.FeatureType.values()) {
      CF.FeatureType cff2 = CF.FeatureType.getFeatureType(cff.toString().toUpperCase());
      assertThat(cff2).isNotNull();
      assertThat(cff).isEqualTo(cff2);
    }
  }

  @Test
  public void testGetFeatureTypeFromGlobalAttributes() {
    assertThat(CF.FeatureType.getFromAttributes(AttributeContainerMutable.of())).isNull();

    assertThat(CF.FeatureType.getFromAttributes(AttributeContainerMutable.of("featureType", "bad"))).isNull();
    assertThat(CF.FeatureType.getFromAttributes(AttributeContainerMutable.of("featureType", "trajectory")))
            .isEqualTo(CF.FeatureType.trajectory);
    assertThat(CF.FeatureType.getFromAttributes(AttributeContainerMutable.of(CF.featureTypeAtt2, "trajectory")))
            .isEqualTo(CF.FeatureType.trajectory);
    assertThat(CF.FeatureType.getFromAttributes(AttributeContainerMutable.of(CF.featureTypeAtt3, "trajectory")))
            .isEqualTo(CF.FeatureType.trajectory);
  }

}
